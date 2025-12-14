package network.dto;

/**
 * 네트워크 프로토콜 메시지의 기본 인터페이스
 */
public interface ProtocolMessage {
  String serialize();

  MessageType getType();

  enum MessageType {
    LOGIN,
    JOIN_ROOM,
    LEAVE_ROOM,
    ROOM_MSG,
    ROOM_IMG,
    USER_LIST,
    USER_JOINED,
    USER_LEFT,
    ROOM_LIST
  }
}
