package network.dto;

/**
 * 채팅방 입장 프로토콜
 * Format: JOIN_ROOM:roomId:username
 */
public class JoinRoomMessage implements ProtocolMessage {
  private String roomId;
  private String username;

  public JoinRoomMessage(String roomId, String username) {
    this.roomId = roomId;
    this.username = username;
  }

  public static JoinRoomMessage parse(String message) {
    String[] parts = message.split(":", 3);
    if (parts.length == 3) {
      return new JoinRoomMessage(parts[1], parts[2]);
    }
    return null;
  }

  @Override
  public String serialize() {
    return "JOIN_ROOM:" + roomId + ":" + username;
  }

  @Override
  public MessageType getType() {
    return MessageType.JOIN_ROOM;
  }

  public String getRoomId() {
    return roomId;
  }

  public String getUsername() {
    return username;
  }
}
