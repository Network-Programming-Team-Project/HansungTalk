package domain;

public class Message {
  private String id;
  private User sender;
  private MessageContent content;
  private String timestamp;

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
