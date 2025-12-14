package network.dto;

/**
 * 채팅방 메시지 프로토콜
 * Format: ROOM_MSG:roomId:sender:content
 */
public class RoomTextMessage implements ProtocolMessage {
  private String roomId;
  private String sender;
  private String content;

  public RoomTextMessage(String roomId, String sender, String content) {
    this.roomId = roomId;
    this.sender = sender;
    this.content = content;
  }

  public static RoomTextMessage parse(String message) {
    String[] parts = message.split(":", 4);
    if (parts.length == 4) {
      return new RoomTextMessage(parts[1], parts[2], parts[3]);
    }
    return null;
  }

  @Override
  public String serialize() {
    return "ROOM_MSG:" + roomId + ":" + sender + ":" + content;
  }

  @Override
  public MessageType getType() {
    return MessageType.ROOM_MSG;
  }

  public String getRoomId() {
    return roomId;
  }

  public String getSender() {
    return sender;
  }

  public String getContent() {
    return content;
  }
}
