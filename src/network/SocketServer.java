package network;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 서버 소켓 통신을 담당하는 클래스
 * 클라이언트 연결 관리, 채팅방 관리, 메시지 브로드캐스트 등을 처리
 */
public class SocketServer {
  private ServerSocket serverSocket; // 서버 소켓
  private int port; // 서버 포트 번호
  private boolean running = false; // 서버 실행 상태
  private Thread acceptThread; // 클라이언트 연결 수락 스레드
  private List<SocketClientHandler> clients = new CopyOnWriteArrayList<>(); // 연결된 클라이언트 목록
  private ServerLogListener logListener; // 로그 리스너

  // 채팅방 관리
  // activeRoomUsers: roomId -> 현재 채팅방을 보고 있는 사용자 집합
  private Map<String, Set<String>> activeRoomUsers = new ConcurrentHashMap<>();
  // roomAllMembers: roomId -> 채팅방에 속한 모든 사용자 집합 (초대됨 또는 참여함)
  private Map<String, Set<String>> roomAllMembers = new ConcurrentHashMap<>();

  // 사용자 게임 점수: username -> (gameType -> 최고 점수)
  private Map<String, Map<String, Integer>> userGameScores = new ConcurrentHashMap<>();

  // 채팅 히스토리: roomId -> 메시지 목록 (프로토콜 문자열로 저장)
  private Map<String, java.util.List<String>> roomChatHistory = new ConcurrentHashMap<>();
  private static final int MAX_HISTORY_PER_ROOM = 100; // 채팅방당 최대 히스토리 개수

  // 안읽은 메시지 수: roomId -> (username -> unreadCount)
  private Map<String, Map<String, Integer>> unreadCounts = new ConcurrentHashMap<>();

  /** 서버 로그 리스너 인터페이스 */
  public interface ServerLogListener {
    void onLog(String message);

    void onClientCountUpdated(int count);
  }

  public void setLogListener(ServerLogListener listener) {
    this.logListener = listener;
  }

  public SocketServer(int port) {
    this.port = port;
  }

  private void log(String message) {
    System.out.println(message);
    if (logListener != null) {
      logListener.onLog(message);
    }
  }

  private void updateClientCount() {
    if (logListener != null) {
      logListener.onClientCountUpdated(clients.size());
    }
  }

  public void start() throws Exception {
    serverSocket = new ServerSocket(port);
    running = true;
    log("Server started on port " + port);
    acceptThread = new Thread(() -> {
      while (running) {
        try {
          Socket clientSocket = serverSocket.accept();
          log("Client connected: " + clientSocket.getInetAddress());
          SocketClientHandler clientHandler = new SocketClientHandler(clientSocket, this);
          clients.add(clientHandler);
          updateClientCount();
          new Thread(clientHandler).start();
        } catch (Exception e) {
          if (running) {
            e.printStackTrace();
          }
        }
      }
    });
    acceptThread.start();
  }

  public void stop() throws Exception {
    running = false;
    if (serverSocket != null && !serverSocket.isClosed()) {
      serverSocket.close();
    }
    if (acceptThread != null) {
      acceptThread.join();
    }
    for (SocketClientHandler client : clients) {
      client.stop();
    }
    clients.clear();
    updateClientCount();
    log("Server stopped.");
  }

  public void broadcast(String message, SocketClientHandler sender) {
    log("Broadcasting: " + message);
    for (SocketClientHandler client : clients) {
      if (client != sender) { // Optional: don't echo back to sender
        client.sendMessage(message);
      }
    }
  }

  public void broadcastUserList() {
    StringBuilder sb = new StringBuilder("USER_LIST:");
    for (SocketClientHandler client : clients) {
      if (client.getUsername() != null) {
        String status = client.getStatusMessage();
        if (status == null)
          status = "";
        // Sanitize status to remove commas or pipes if necessary, though simple
        // replacement is safer
        status = status.replace(",", " ").replace("|", " ");
        sb.append(client.getUsername()).append("|").append(status).append(",");
      }
    }
    String userListMsg = sb.toString();
    if (userListMsg.endsWith(",")) {
      userListMsg = userListMsg.substring(0, userListMsg.length() - 1);
    }

    for (SocketClientHandler client : clients) {
      client.sendMessage(userListMsg);
    }
  }

  // Room management methods
  public void joinRoom(String roomId, String username) {
    activeRoomUsers.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet()).add(username);
    roomAllMembers.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet()).add(username);
    log(username + " joined room: " + roomId);

    // 안읽은 메시지 수 초기화
    Map<String, Integer> roomUnread = unreadCounts.get(roomId);
    if (roomUnread != null) {
      int previousUnread = roomUnread.getOrDefault(username, 0);
      roomUnread.put(username, 0);
      if (previousUnread > 0) {
        // 해당 사용자에게 unread 초기화 알림
        broadcastUnreadUpdate(roomId, username, 0);
      }
    }

    // Send chat history to the user who just joined
    sendChatHistory(roomId, username);
  }

  public void leaveRoom(String roomId, String username) {
    Set<String> active = activeRoomUsers.get(roomId);
    if (active != null) {
      active.remove(username);
      if (active.isEmpty()) {
        activeRoomUsers.remove(roomId);
        // Note: We might want to keep roomAllMembers? For now, if no one is active,
        // keep members?
        // If everyone leaves (disconnects), we lose state anyway in this memory-only
        // server.
      }
      log(username + " left room (active): " + roomId);
    }
  }

  public void broadcastToRoom(String roomId, String message, String senderUsername) {
    // Ensure room members are set for 1:1 chats
    ensureRoomMembers(roomId);

    Set<String> members = roomAllMembers.get(roomId);
    if (members == null || members.isEmpty()) {
      log("No members found for room: " + roomId);
      return;
    }

    log("Broadcasting to room " + roomId + " (members: " + members + "): " + message);
    for (SocketClientHandler client : clients) {
      if (client.getUsername() != null &&
          members.contains(client.getUsername()) &&
          !client.getUsername().equals(senderUsername)) {
        client.sendMessage(message);
      }
    }
  }

  /**
   * 게임 결과를 방의 모든 멤버에게 전송 (발신자 제외 없이 전체 전송)
   */
  public void broadcastGameResultToRoom(String roomId, String message) {
    ensureRoomMembers(roomId);

    Set<String> members = roomAllMembers.get(roomId);
    if (members == null || members.isEmpty()) {
      log("No members found for room: " + roomId);
      return;
    }

    log("Broadcasting game result to room " + roomId + " (members: " + members + "): " + message);
    for (SocketClientHandler client : clients) {
      if (client.getUsername() != null && members.contains(client.getUsername())) {
        client.sendMessage(message);
      }
    }
  }

  public void removeClient(SocketClientHandler client) {
    clients.remove(client);
    updateClientCount();
    log("Client disconnected: " + client.getUsername());

    // Remove from all rooms
    String username = client.getUsername();
    if (username != null) {
      for (String roomId : activeRoomUsers.keySet()) {
        leaveRoom(roomId, username);
      }
      broadcast("USER_LEFT:" + username, client);
      broadcastUserList();
    }
  }

  public List<SocketClientHandler> getClients() {
    return clients;
  }

  public static class SocketClientHandler implements Runnable {
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private boolean running = false;
    private SocketServer server;
    private String username;

    public SocketClientHandler(Socket socket, SocketServer server) throws Exception {
      this.socket = socket;
      this.server = server;
      this.reader = new BufferedReader(new java.io.InputStreamReader(socket.getInputStream()));
      this.writer = new PrintWriter(socket.getOutputStream(), true);
    }

    public String getUsername() {
      return username;
    }

    public String getStatusMessage() {
      return statusMessage;
    }

    private String statusMessage = "";

    @Override
    public void run() {
      running = true;
      try {
        String line;
        while (running && (line = reader.readLine()) != null) {
          server.log("Received: " + line);

          if (line.startsWith("LOGIN:")) {
            this.username = line.substring(6);
            server.log("User logged in: " + username);
            server.broadcast("USER_JOINED:" + username, this);
            server.broadcastUserList();

          } else if (line.startsWith("JOIN_ROOM:")) {
            // Format: JOIN_ROOM:roomId:username
            String[] parts = line.split(":", 3);
            if (parts.length == 3) {
              String roomId = parts[1];
              server.joinRoom(roomId, username);
            }

          } else if (line.startsWith("ROOM_MSG:")) {
            // Format: ROOM_MSG:roomId:sender:content
            String[] parts = line.split(":", 4);
            if (parts.length == 4) {
              String roomId = parts[1];
              String content = parts[3];

              // Calculate Unread Count
              // unread = allMembers - activeMembers
              Set<String> members = server.roomAllMembers.get(roomId);
              Set<String> active = server.activeRoomUsers.get(roomId);
              int totalMembers = (members != null) ? members.size() : 0;
              int activeCount = (active != null) ? active.size() : 0;
              // Ensure we count the sender as 'active' logic if they just sent it?
              // Sender is in active if they joined.
              int unreadCount = Math.max(0, totalMembers - activeCount);

              // New Format: ROOM_MSG:roomId:sender:unreadCount:content
              String enrichedMsg = "ROOM_MSG:" + roomId + ":" + username + ":" + unreadCount + ":" + content;

              // Save message to history
              server.saveMessage(roomId, enrichedMsg);

              server.broadcastToRoom(roomId, enrichedMsg, username);

              // 안읽은 메시지 수 증가 (방에 없는 사용자들)
              server.incrementUnreadCounts(roomId, username);

              server.notifyChatListUpdate(roomId, content, username);
            }

          } else if (line.startsWith("ROOM_IMG:")) {
            // Format: ROOM_IMG:roomId:sender:base64
            String[] parts = line.split(":", 4);
            if (parts.length == 4) {
              String roomId = parts[1];
              server.broadcastToRoom(roomId, line, username);
              server.notifyChatListUpdate(roomId, "사진", username);
            }

          } else if (line.startsWith("ROOM_EMOJI:")) {
            // Format: ROOM_EMOJI:roomId:sender:emojiName
            String[] parts = line.split(":", 4);
            if (parts.length == 4) {
              String roomId = parts[1];
              // 히스토리에 저장 (채팅방 재입장 시 이모티콘 유지)
              server.saveMessage(roomId, line);
              server.broadcastToRoom(roomId, line, username);
              server.notifyChatListUpdate(roomId, "이모티콘", username);
            }

          } else if (line.startsWith("ROOM_GAME_INVITE:")) {
            // Format: ROOM_GAME_INVITE:roomId:sender:gameType
            String[] parts = line.split(":", 4);
            if (parts.length == 4) {
              String roomId = parts[1];
              // Save to history
              server.saveMessage(roomId, line);
              server.broadcastToRoom(roomId, line, username);
              server.notifyChatListUpdate(roomId, "게임 초대", username);
            }

          } else if (line.startsWith("INVITE:")) {
            // Format: INVITE:roomId:targetUser
            String[] parts = line.split(":", 3);
            if (parts.length == 3) {
              String roomId = parts[1];
              String targetUser = parts[2];
              // Find target user and send invitation
              for (SocketClientHandler client : server.getClients()) {
                if (targetUser.equals(client.getUsername())) {
                  client.sendMessage("INVITATION:" + roomId + ":" + username);
                  // Add to roomAllMembers immediately so they count as a member even before
                  // joining active
                  server.roomAllMembers.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet()).add(targetUser);
                  server.roomAllMembers.get(roomId).add(username); // Ensure inviter is also a member
                  break;
                }
              }
            }

          } else if (line.startsWith("MSG:") || line.startsWith("IMG:")) {
            // Legacy broadcast (deprecated)
            server.broadcast(line, this);

          } else if (line.startsWith("GAME_RESULT:")) {
            // Format: GAME_RESULT:roomId:gameName:scoreMsg
            String[] parts = line.split(":", 4);
            if (parts.length == 4) {
              String roomId = parts[1];
              String gameType = parts[2];
              String scoreMsg = parts[3];

              // Try to parse score from message and save (username is null for game clients)
              if (username != null) {
                server.saveGameScore(username, gameType, scoreMsg);
              } else {
                // 게임 클라이언트에서 전송 시 메시지에서 유저명 추출
                java.util.regex.Matcher m = java.util.regex.Pattern.compile("^(.+?)님").matcher(scoreMsg);
                if (m.find()) {
                  server.saveGameScore(m.group(1), gameType, scoreMsg);
                }
              }

              // 모든 방 멤버에게 게임 결과 전송 (발신자 제외 없이 전체 전송)
              server.broadcastGameResultToRoom(roomId, line);
            }
          } else if (line.startsWith("TYPING:")) {
            // Format: TYPING:roomId:username:START or TYPING:roomId:username:STOP
            String[] parts = line.split(":", 4);
            if (parts.length == 4) {
              String roomId = parts[1];
              String typingUser = parts[2];
              // 입력중 상태를 방의 다른 멤버들에게 전송
              server.broadcastToRoom(roomId, line, typingUser);
            }
          } else if (line.startsWith("GET_PROFILE:")) {
            // Format: GET_PROFILE:targetUsername
            String targetUser = line.substring(12);
            String profile = server.getProfileData(targetUser);
            sendMessage("PROFILE:" + targetUser + ":" + profile);

          } else if (line.startsWith("UPDATE_STATUS:")) {
            String newStatus = line.substring(14);
            this.statusMessage = newStatus;
            server.log(username + " updated status: " + newStatus);
            server.broadcastUserList(); // Broadcast change

          } else if (line.equals("PING")) {
            sendMessage("PONG");
          }
        }
      } catch (Exception e) {
        if (running) {
          // e.printStackTrace(); // Suppress error on disconnect
        }
      } finally {
        stop();
        server.removeClient(this);
      }
    }

    public void sendMessage(String message) {
      writer.println(message);
    }

    public void stop() {
      running = false;
      try {
        if (socket != null && !socket.isClosed()) {
          socket.close();
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  // Helper methods for SocketServer

  public void notifyChatListUpdate(String roomId, String lastMessage, String sender) {
    ensureRoomMembers(roomId);
    Set<String> members = roomAllMembers.get(roomId);
    if (members != null) {
      members.add(sender); // Ensure sender is included

      String chatListUpdate = "UPDATE_CHAT_LIST:" + roomId + ":" + lastMessage;
      for (SocketClientHandler client : clients) {
        if (client.getUsername() != null && members.contains(client.getUsername())) {
          client.sendMessage(chatListUpdate);
        }
      }
    }
  }

  private void ensureRoomMembers(String roomId) {
    // For 1:1 chat format (userA_userB), always ensure both users are added
    if (roomId.contains("_") && !roomId.startsWith("group_")) {
      String[] users = roomId.split("_");
      if (users.length == 2) {
        Set<String> members = roomAllMembers.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet());
        boolean added = false;
        if (!members.contains(users[0])) {
          members.add(users[0]);
          added = true;
        }
        if (!members.contains(users[1])) {
          members.add(users[1]);
          added = true;
        }
        if (added) {
          log("Ensured room members for " + roomId + ": " + members);
        }
      }
    }
  }

  private void saveGameScore(String username, String gameType, String scoreMsg) {
    // Try to extract numeric score from message
    // Typical format: "username님 결과 - 승리 (150 vs 120)" or "username님 결과 - 승리"
    try {
      int score = 0;

      // Try to find a number in the message
      java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\((\\d+)\\s*vs").matcher(scoreMsg);
      if (m.find()) {
        score = Integer.parseInt(m.group(1));
      } else {
        // Try to find any number in parentheses
        m = java.util.regex.Pattern.compile("\\d+").matcher(scoreMsg);
        if (m.find()) {
          score = Integer.parseInt(m.group());
        }
      }

      // Also give points for wins
      if (scoreMsg.contains("승리")) {
        score += 100; // Bonus for winning
      }

      if (score > 0) {
        Map<String, Integer> userScores = userGameScores.computeIfAbsent(username, k -> new ConcurrentHashMap<>());
        int currentBest = userScores.getOrDefault(gameType, 0);
        if (score > currentBest) {
          userScores.put(gameType, score);
          log("Updated best score for " + username + " in " + gameType + ": " + score);
        }
      }
    } catch (Exception e) {
      log("Failed to parse score from message: " + scoreMsg);
    }
  }

  private String getProfileData(String username) {
    // Return format: "SPACE:score,BRICK:score,TYPING:score,VOLLEY:score"
    Map<String, Integer> scores = userGameScores.getOrDefault(username, new ConcurrentHashMap<>());
    StringBuilder sb = new StringBuilder();
    sb.append("SPACE:").append(scores.getOrDefault("SPACE", 0));
    sb.append(",BRICK:").append(scores.getOrDefault("BRICK", 0));
    sb.append(",TYPING:").append(scores.getOrDefault("TYPING", 0));
    sb.append(",VOLLEY:").append(scores.getOrDefault("VOLLEY", 0));
    return sb.toString();
  }

  public void saveMessage(String roomId, String message) {
    java.util.List<String> history = roomChatHistory.computeIfAbsent(roomId,
        k -> java.util.Collections.synchronizedList(new java.util.ArrayList<>()));
    history.add(message);
    // Keep only last N messages
    while (history.size() > MAX_HISTORY_PER_ROOM) {
      history.remove(0);
    }
    log("Saved message to room " + roomId + " history (total: " + history.size() + ")");
  }

  private void sendChatHistory(String roomId, String username) {
    java.util.List<String> history = roomChatHistory.get(roomId);
    if (history == null || history.isEmpty()) {
      log("No chat history for room " + roomId);
      return;
    }

    // Find the client for this user and send them the history
    for (SocketClientHandler client : clients) {
      if (username.equals(client.getUsername())) {
        log("Sending " + history.size() + " history messages to " + username + " for room " + roomId);
        for (String msg : history) {
          client.sendMessage("HISTORY:" + msg);
        }
        break;
      }
    }
  }

  /**
   * 방에 없는 사용자들의 안읽은 메시지 수 증가
   */
  public void incrementUnreadCounts(String roomId, String senderUsername) {
    Set<String> members = roomAllMembers.get(roomId);
    Set<String> active = activeRoomUsers.get(roomId);
    if (members == null)
      return;

    Map<String, Integer> roomUnread = unreadCounts.computeIfAbsent(roomId, k -> new ConcurrentHashMap<>());

    for (String member : members) {
      // 발신자는 제외, 방에 활성화되어 있는 사용자도 제외
      if (member.equals(senderUsername))
        continue;
      if (active != null && active.contains(member))
        continue;

      int newCount = roomUnread.getOrDefault(member, 0) + 1;
      roomUnread.put(member, newCount);

      // 해당 사용자에게 unread 업데이트 알림
      broadcastUnreadUpdate(roomId, member, newCount);
    }
  }

  /**
   * 특정 사용자에게 안읽은 메시지 수 업데이트 전송
   */
  private void broadcastUnreadUpdate(String roomId, String username, int count) {
    // Format: UNREAD_UPDATE:roomId:count
    String message = "UNREAD_UPDATE:" + roomId + ":" + count;
    for (SocketClientHandler client : clients) {
      if (username.equals(client.getUsername())) {
        client.sendMessage(message);
        log("Sent unread update to " + username + " for room " + roomId + ": " + count);
        break;
      }
    }
  }

  /**
   * 특정 사용자의 특정 방 안읽은 메시지 수 조회
   */
  public int getUnreadCount(String roomId, String username) {
    Map<String, Integer> roomUnread = unreadCounts.get(roomId);
    if (roomUnread == null)
      return 0;
    return roomUnread.getOrDefault(username, 0);
  }
}
