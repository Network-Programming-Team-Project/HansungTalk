package domain;

/**
 * 이모티콘(스티커) 메시지 내용을 담는 클래스
 * MessageContent 인터페이스를 구현
 */
public class EmojiContent implements MessageContent {
  private Emoji emoji; // 이모지 정보

  /** 생성자: 이모지 설정 */
  public EmojiContent(Emoji emoji) {
    this.emoji = emoji;
  }

  public Emoji getEmoji() {
    return emoji;
  }

  @Override
  public MessageType getType() {
    return MessageType.STICKER;
  }
}