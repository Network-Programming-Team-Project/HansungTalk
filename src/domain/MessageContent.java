package domain;

// 메시지 내용물을 나타내는 최상위 인터페이스
public interface MessageContent {
  /**
   * 이 내용물의 타입을 반환합니다.
   * (JSON 직렬화/역직렬화 시 구분을 위해 필수)
   */
  MessageType getType();
}