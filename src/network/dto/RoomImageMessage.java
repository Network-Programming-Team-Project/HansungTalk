package network.dto;

/**
 * 채팅방 이미지 메시지 프로토콜
 * Format: ROOM_IMG:roomId:sender:base64data
 */
public class RoomImageMessage implements ProtocolMessage {
  private String roomId;
  private String sender;
  private String base64Data;

  public RoomImageMessage(String roomId, String sender, String base64Data) {
    this.roomId = roomId;
    this.sender = sender;
    this.base64Data = base64Data;
  }

  public static RoomImageMessage parse(String message) {
    String[] parts = message.split(":", 4);
    if (parts.length == 4) {
      return new RoomImageMessage(parts[1], parts[2], parts[3]);
    }
    return null;
  }

  @Override
  public String serialize() {
    return "ROOM_IMG:" + roomId + ":" + sender + ":" + base64Data;
  }

  @Override
  public MessageType getType() {
    return MessageType.ROOM_IMG;
  }

  public String getRoomId() {
    return roomId;
  }

  public String getSender() {
    return sender;
  }

  public String getBase64Data() {
    return base64Data;
  }
}
