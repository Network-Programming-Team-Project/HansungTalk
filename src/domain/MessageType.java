package domain;

/**
 * 메시지 타입을 정의하는 열거형
 * 메시지 내용물의 종류를 구분하는 데 사용
 */
public enum MessageType {
  TEXT, // 텍스트 메시지
  IMAGE, // 이미지 메시지
  STICKER // 스티커(이모티콘) 메시지
}
