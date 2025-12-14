package domain;

public class EmojiContent implements MessageContent {
  private Emoji emoji;

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