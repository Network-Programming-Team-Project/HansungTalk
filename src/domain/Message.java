package domain;

/**
 * 메시지 정보를 담는 도메인 클래스
 * 발신자, 내용, 타임스탬프 등 메시지 정보를 관리
 */
public class Message {
  private String id; // 메시지 고유 ID
  private User sender; // 발신자 정보
  private MessageContent content; // 메시지 내용 (텍스트/이미지/스티커)
  private String timestamp; // 발송 시간

  /** 생성자: 모든 필드를 초기화 */
  public Message(String id, User sender, MessageContent content, String timestamp) {
    this.id = id;
    this.sender = sender;
    this.content = content;
    this.timestamp = timestamp;
  }

  public String getId() {
    return id;
  }

  public User getSender() {
    return sender;
  }

  public MessageContent getContent() {
    return content;
  }

  public void setContent(MessageContent content) {
    this.content = content;
  }

  // 편의 메서드 (타입을 쉽게 가져오기)
  public MessageType getMessageType() {
    return (content != null) ? content.getType() : null;
  }

  public String getTimestamp() {
    return timestamp;
  }
}
