package domain;

public class TextContent implements MessageContent {
  private final String text;

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