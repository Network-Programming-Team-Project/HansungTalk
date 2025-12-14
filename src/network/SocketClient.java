package network;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import util.ClientLogger;
import util.SoundPlayer;
import util.NotificationManager;

/**
 * 클라이언트 소켓 통신을 담당하는 클래스
 * 서버와의 연결, 메시지 송수신, 하트비트 등을 관리
 */
public class SocketClient {
  private String host; // 서버 호스트 주소
  private int port; // 서버 포트 번호
  private Socket socket; // 소켓 연결
  private BufferedReader reader; // 서버로부터 데이터 수신
  private PrintWriter writer; // 서버로 데이터 전송
  private final Object writerLock = new Object(); // 소켓 쓰기 동기화 락
  private volatile boolean running = false; // 연결 상태 플래그
  private MessageListener messageListener; // 메시지 수신 리스너
  private String username; // 현재 사용자 이름
  private Thread heartbeatThread; // 하트비트 스레드 (연결 유지)

  public String getUsername() {
    return username;
  }

  /** 메시지 수신 리스너 인터페이스 */
  public interface MessageListener {
    void onMessageReceived(String message); // 텍스트 메시지 수신

    void onImageReceived(String sender, javax.swing.ImageIcon image); // 이미지 수신

    void onEmojiReceived(String sender, String emojiName); // 이모티콘 수신

    void onGameInviteReceived(String sender, String gameType); // 게임 초대 수신
  }

  /** 생성자: 서버 주소와 포트 설정 */
  public SocketClient(String host, int port) {
    this.host = host;
    this.port = port;
  }

  public void setMessageListener(MessageListener listener) {
    this.messageListener = listener;
  }

  private UserListListener userListListener;

  public interface UserListListener {
    void onUserListUpdated(String[] users);

    void onUserJoined(String username);

    void onUserLeft(String username);

    void onChatListUpdate(String roomId, String lastMessage);
  }

  public void setUserListListener(UserListListener listener) {
    this.userListListener = listener;
  }

  // 안읽은 메시지 수 리스너
  private UnreadListener unreadListener;
  private java.util.Map<String, Integer> cachedUnreadCounts = new java.util.concurrent.ConcurrentHashMap<>();

  public interface UnreadListener {
    void onUnreadCountUpdated(String roomId, int count);

    void onTotalUnreadUpdated(int totalCount);
  }

  public void setUnreadListener(UnreadListener listener) {
    this.unreadListener = listener;
  }

  public java.util.Map<String, Integer> getCachedUnreadCounts() {
    return cachedUnreadCounts;
  }

  public int getTotalUnreadCount() {
    int total = 0;
    for (int count : cachedUnreadCounts.values()) {
      total += count;
    }
    return total;
  }

  // Profile listener for receiving profile data
  private ProfileListener profileListener;
  private java.util.Map<String, java.util.Map<String, Integer>> cachedProfiles = new java.util.concurrent.ConcurrentHashMap<>();

  public interface ProfileListener {
    void onProfileReceived(String username, java.util.Map<String, Integer> scores);
  }

  public void setProfileListener(ProfileListener listener) {
    this.profileListener = listener;
  }

  public java.util.Map<String, Integer> getCachedProfile(String username) {
    return cachedProfiles.get(username);
  }

  public void start(String username) {
    this.username = username;
    ClientLogger.network("Starting client for user: " + username);
    new Thread(() -> {
      boolean connected = false;
      int attempts = 0;
      while (!connected && attempts < 10) {
        try {
          socket = new Socket(host, port);
          ClientLogger.network("Connected to server at " + host + ":" + port);

          reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
          writer = new PrintWriter(socket.getOutputStream(), true);
          running = true;
          connected = true;

          // Send Login
          writer.println("LOGIN:" + username);
          ClientLogger.network("Sent LOGIN:" + username);

          // Start Heartbeat
          startHeartbeat();

          // Listen loop
          String line;
          while (running && (line = reader.readLine()) != null) {
            ClientLogger.network("Received: " + line);
            if (line.startsWith("USER_LIST:")) {
              if (userListListener != null) {
                String[] users = line.substring(10).split(",");
                userListListener.onUserListUpdated(users);
              }
            } else if (line.startsWith("USER_JOINED:")) {
              if (userListListener != null) {
                userListListener.onUserJoined(line.substring(12));
              }
            } else if (line.startsWith("USER_LEFT:")) {
              if (userListListener != null) {
                userListListener.onUserLeft(line.substring(10));
              }
            } else if (line.startsWith("ROOM_MSG:")) {
              // Format: ROOM_MSG:roomId:sender:unreadCount:content
              if (messageListener != null) {
                String[] parts = line.split(":", 5);
                if (parts.length == 5) {
                  String sender = parts[2];
                  String unreadCount = parts[3];
                  String content = parts[4];
                  // Pass formatted message to listener: MSG:sender:unreadCount:content
                  messageListener.onMessageReceived("MSG:" + sender + ":" + unreadCount + ":" + content);

                  // 알림 및 사운드 재생 (다른 사람이 보낸 메시지일 때만)
                  if (!sender.equals(username)) {
                    SoundPlayer.playKakao();
                    NotificationManager.showMessageNotification(sender, content);
                  }
                }
              }
            } else if (line.startsWith("HISTORY:ROOM_MSG:")) {
              // Format: HISTORY:ROOM_MSG:roomId:sender:unreadCount:content
              if (messageListener != null) {
                String historyContent = line.substring(8); // Remove "HISTORY:" prefix
                String[] parts = historyContent.split(":", 5);
                if (parts.length == 5) {
                  String sender = parts[2];
                  String unreadCount = parts[3];
                  String content = parts[4];
                  final String finalMsg = "MSG:" + sender + ":" + unreadCount + ":" + content;
                  javax.swing.SwingUtilities.invokeLater(() -> messageListener.onMessageReceived(finalMsg));
                }
              }
            } else if (line.startsWith("HISTORY:ROOM_GAME_INVITE:")) {
              // Format: HISTORY:ROOM_GAME_INVITE:roomId:sender:gameType
              if (messageListener != null) {
                String historyContent = line.substring(8); // Remove "HISTORY:" prefix
                String[] parts = historyContent.split(":", 4);
                if (parts.length == 4) {
                  String sender = parts[2];
                  String gameType = parts[3];
                  javax.swing.SwingUtilities.invokeLater(() -> messageListener.onGameInviteReceived(sender, gameType));
                }
              }
            } else if (line.startsWith("ROOM_IMG:")) {
              // Format: ROOM_IMG:roomId:sender:base64
              if (messageListener != null) {
                int firstColon = line.indexOf(':');
                int secondColon = line.indexOf(':', firstColon + 1);
                int thirdColon = line.indexOf(':', secondColon + 1);
                if (thirdColon != -1) {
                  String sender = line.substring(secondColon + 1, thirdColon);
                  String base64 = line.substring(thirdColon + 1);
                  byte[] decodedBytes = java.util.Base64.getDecoder().decode(base64);
                  javax.swing.ImageIcon image = new javax.swing.ImageIcon(decodedBytes);
                  messageListener.onImageReceived(sender, image);
                }
              }
            } else if (line.startsWith("INVITATION:")) {
              // Format: INVITATION:roomId:inviter
              String[] parts = line.split(":", 3);
              if (parts.length == 3) {
                String roomId = parts[1];
                String inviter = parts[2];
                // Auto-join
                joinRoom(roomId);
                // Notify user (optional, maybe a popup or sound)
                System.out.println("Invited to room " + roomId + " by " + inviter);
              }
            } else if (line.startsWith("ROOM_EMOJI:")) {
              // Format: ROOM_EMOJI:roomId:sender:emojiName
              if (messageListener != null) {
                String[] parts = line.split(":", 4);
                if (parts.length == 4) {
                  String sender = parts[2];
                  String emojiName = parts[3];
                  messageListener.onEmojiReceived(sender, emojiName);
                }
              }
            } else if (line.startsWith("ROOM_GAME_INVITE:")) {
              // Format: ROOM_GAME_INVITE:roomId:sender:gameType
              if (messageListener != null) {
                String[] parts = line.split(":", 4);
                if (parts.length == 4) {
                  String sender = parts[2];
                  String gameType = parts[3];
                  messageListener.onGameInviteReceived(sender, gameType);
                }
              }
            } else if (line.startsWith("IMG:")) {
              if (messageListener != null) {
                int firstColon = line.indexOf(':');
                int secondColon = line.indexOf(':', firstColon + 1);
                if (secondColon != -1) {
                  String sender = line.substring(firstColon + 1, secondColon);
                  String base64 = line.substring(secondColon + 1);
                  byte[] decodedBytes = java.util.Base64.getDecoder().decode(base64);
                  javax.swing.ImageIcon image = new javax.swing.ImageIcon(decodedBytes);
                  messageListener.onImageReceived(sender, image);
                }
              }
            } else if (line.startsWith("UPDATE_CHAT_LIST:")) {
              // Format: UPDATE_CHAT_LIST:roomId:lastMessage
              if (userListListener != null) {
                String[] parts = line.split(":", 3);
                if (parts.length == 3) {
                  String roomId = parts[1];
                  String content = parts[2];
                  userListListener.onChatListUpdate(roomId, content);
                }
              }
            } else if (line.startsWith("UNREAD_UPDATE:")) {
              // Format: UNREAD_UPDATE:roomId:count
              String[] parts = line.split(":", 3);
              if (parts.length == 3) {
                String roomId = parts[1];
                int count = Integer.parseInt(parts[2]);
                cachedUnreadCounts.put(roomId, count);
                if (unreadListener != null) {
                  int total = getTotalUnreadCount();
                  javax.swing.SwingUtilities.invokeLater(() -> {
                    unreadListener.onUnreadCountUpdated(roomId, count);
                    unreadListener.onTotalUnreadUpdated(total);
                  });
                }
              }
            } else if (line.startsWith("PROFILE:")) {
              // Format: PROFILE:username:SPACE:score,BRICK:score,TYPING:score,VOLLEY:score
              String[] parts = line.split(":", 3);
              if (parts.length >= 3) {
                String targetUser = parts[1];
                String scoresData = parts[2];
                java.util.Map<String, Integer> scores = new java.util.HashMap<>();

                // Parse scores: SPACE:100,BRICK:50,TYPING:200,VOLLEY:0
                for (String scorePair : scoresData.split(",")) {
                  String[] kv = scorePair.split(":");
                  if (kv.length == 2) {
                    try {
                      scores.put(kv[0], Integer.parseInt(kv[1]));
                    } catch (NumberFormatException e) {
                      scores.put(kv[0], 0);
                    }
                  }
                }

                cachedProfiles.put(targetUser, scores);
                ClientLogger.network("Received profile for " + targetUser + ": " + scores);

                if (profileListener != null) {
                  final java.util.Map<String, Integer> finalScores = scores;
                  javax.swing.SwingUtilities
                      .invokeLater(() -> profileListener.onProfileReceived(targetUser, finalScores));
                }
              }
            } else if (line.equals("PONG")) {
              // Heartbeat response, ignore
            } else if (messageListener != null) {
              messageListener.onMessageReceived(line);
            }
          }
        } catch (Exception e) {
          System.out.println("Connection attempt " + (attempts + 1) + " failed: " + e.getMessage());
          attempts++;
          try {
            Thread.sleep(1000);
          } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
          }
        }
      }
      if (!connected) {
        System.err.println("Failed to connect to server after multiple attempts.");
        javax.swing.SwingUtilities.invokeLater(() -> {
          javax.swing.JOptionPane.showMessageDialog(null, "Failed to connect to server.", "Connection Error",
              javax.swing.JOptionPane.ERROR_MESSAGE);
          System.exit(1);
        });
      }
    }).start();

  }

  private void startHeartbeat() {
    if (heartbeatThread != null && heartbeatThread.isAlive())
      return;

    heartbeatThread = new Thread(() -> {
      while (running && socket != null && !socket.isClosed()) {
        try {
          Thread.sleep(10000); // 10 seconds
          if (writer != null) {
            writer.println("PING");
            if (writer.checkError())
              throw new Exception("Write error");
          }
        } catch (InterruptedException e) {
          break;
        } catch (Exception e) {
          System.out.println("Heartbeat failed: " + e.getMessage());
          // Let the reader loop handle disconnection
          break;
        }
      }
    });
    heartbeatThread.setDaemon(true);
    heartbeatThread.start();
  }

  public void sendMessage(String message) {
    if (writer != null) {
      writer.println("MSG:" + username + ": " + message);
    }
  }

  public void sendImage(java.io.File file) {
    try {
      byte[] fileContent = java.nio.file.Files.readAllBytes(file.toPath());
      String base64 = java.util.Base64.getEncoder().encodeToString(fileContent);
      if (writer != null) {
        writer.println("IMG:" + username + ":" + base64);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  // Room-based messaging
  public void joinRoom(String roomId) {
    synchronized (writerLock) {
      if (writer != null) {
        ClientLogger.network("Joining room: " + roomId);
        writer.println("JOIN_ROOM:" + roomId + ":" + username);
      }
    }
    // 로컬 캐시에서 안읽은 메시지 수 초기화 (락 밖에서 수행)
    cachedUnreadCounts.put(roomId, 0);
    if (unreadListener != null) {
      int total = getTotalUnreadCount();
      javax.swing.SwingUtilities.invokeLater(() -> {
        unreadListener.onUnreadCountUpdated(roomId, 0);
        unreadListener.onTotalUnreadUpdated(total);
      });
    }
  }

  public void sendRoomMessage(String roomId, String message) {
    synchronized (writerLock) {
      if (writer != null) {
        ClientLogger.network("Sending to room " + roomId + ": " + message);
        writer.println("ROOM_MSG:" + roomId + ":" + username + ":" + message);
      }
    }
  }

  public void sendRoomImage(String roomId, java.io.File file) {
    try {
      byte[] fileContent = java.nio.file.Files.readAllBytes(file.toPath());
      String base64 = java.util.Base64.getEncoder().encodeToString(fileContent);
      synchronized (writerLock) {
        if (writer != null) {
          writer.println("ROOM_IMG:" + roomId + ":" + username + ":" + base64);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void inviteUser(String roomId, String targetUser) {
    synchronized (writerLock) {
      if (writer != null) {
        writer.println("INVITE:" + roomId + ":" + targetUser);
      }
    }
  }

  public void sendRoomEmoji(String roomId, String emojiName) {
    synchronized (writerLock) {
      if (writer != null) {
        writer.println("ROOM_EMOJI:" + roomId + ":" + username + ":" + emojiName);
      }
    }
  }

  public void sendGameInvite(String roomId, String gameType) {
    synchronized (writerLock) {
      if (writer != null) {
        writer.println("ROOM_GAME_INVITE:" + roomId + ":" + username + ":" + gameType);
      }
    }
  }

  public void updateStatus(String status) {
    synchronized (writerLock) {
      if (writer != null) {
        writer.println("UPDATE_STATUS:" + status);
      }
    }
  }

  public void requestProfile(String targetUsername) {
    synchronized (writerLock) {
      if (writer != null) {
        writer.println("GET_PROFILE:" + targetUsername);
      }
    }
  }

  public void stop() {
    running = false;
    if (heartbeatThread != null)
      heartbeatThread.interrupt();
    try {
      if (socket != null && !socket.isClosed()) {
        socket.close();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
