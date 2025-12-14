package domain;

public class Emoji {
  private String imagePath;
  private String description;

  public Emoji(String imagePath, String description) {
    this.imagePath = imagePath;
    this.description = description;
  }

  public String getImagePath() {
    return imagePath;
  }

  public String getDescription() {
    return description;
  }
}
