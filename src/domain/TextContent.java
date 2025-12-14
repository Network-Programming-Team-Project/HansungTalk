package domain;

/**
 * 텍스트 메시지 내용을 담는 클래스
 * MessageContent 인터페이스를 구현
 */
public class TextContent implements MessageContent {
  private final String text; // 텍스트 내용

  /** 생성자: 텍스트 내용 설정 */
  public TextContent(String text) {
    this.text = text;
  }

  public String getText() {
    return text;
  }

  @Override
  public MessageType getType() {
    return MessageType.TEXT;
  }
}