package network;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import util.ClientLogger;

public class SocketClient {
  private String host;
  private int port;
  private Socket socket;
  private BufferedReader reader;
  private PrintWriter writer;
  private volatile boolean running = false;
  private MessageListener messageListener;
  private String username;
  private Thread heartbeatThread;

  public String getUsername() {
    return username;
  }

  public interface MessageListener {
    void onMessageReceived(String message);

    void onImageReceived(String sender, javax.swing.ImageIcon image);

    void onEmojiReceived(String sender, String emojiName);

    void onGameInviteReceived(String sender, String gameType);
  }

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
    if (writer != null) {
      ClientLogger.network("Joining room: " + roomId);
      writer.println("JOIN_ROOM:" + roomId + ":" + username);
    }
  }

  public void sendRoomMessage(String roomId, String message) {
    if (writer != null) {
      ClientLogger.network("Sending to room " + roomId + ": " + message);
      writer.println("ROOM_MSG:" + roomId + ":" + username + ":" + message);
    }
  }

  public void sendRoomImage(String roomId, java.io.File file) {
    try {
      byte[] fileContent = java.nio.file.Files.readAllBytes(file.toPath());
      String base64 = java.util.Base64.getEncoder().encodeToString(fileContent);
      if (writer != null) {
        writer.println("ROOM_IMG:" + roomId + ":" + username + ":" + base64);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void inviteUser(String roomId, String targetUser) {
    if (writer != null) {
      writer.println("INVITE:" + roomId + ":" + targetUser);
    }
  }

  public void sendRoomEmoji(String roomId, String emojiName) {
    if (writer != null) {
      writer.println("ROOM_EMOJI:" + roomId + ":" + username + ":" + emojiName);
    }
  }

  public void sendGameInvite(String roomId, String gameType) {
    if (writer != null) {
      writer.println("ROOM_GAME_INVITE:" + roomId + ":" + username + ":" + gameType);
    }
  }

  public void updateStatus(String status) {
    if (writer != null) {
      writer.println("UPDATE_STATUS:" + status);
    }
  }

  public void requestProfile(String targetUsername) {
    if (writer != null) {
      writer.println("GET_PROFILE:" + targetUsername);
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
