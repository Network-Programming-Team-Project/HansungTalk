package domain;

public class User {
  private String id;
  private String name;
  private String statusMessage;
  private Emoji profileEmoji;

  public User(String id, String name, String statusMessage, Emoji profileEmoji) {
    this.id = id;
    this.name = name;
    this.statusMessage = statusMessage;
    this.profileEmoji = profileEmoji;
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getStatusMessage() {
    return statusMessage;
  }

  public void setStatusMessage(String statusMessage) {
    this.statusMessage = statusMessage;
  }

  public Emoji getProfileEmoji() {
    return profileEmoji;
  }

  public void setProfileEmoji(Emoji profileEmoji) {
    this.profileEmoji = profileEmoji;
  }
}
