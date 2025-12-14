package domain;

public class ImageContent implements MessageContent {
  private final String imageUrl;
  private final String thumbnailUrl; // 썸네일 URL (선택적)

  public ImageContent(String imageUrl, String thumbnailUrl) {
    this.imageUrl = imageUrl;
    this.thumbnailUrl = thumbnailUrl;
  }

  public String getImageUrl() {
    return imageUrl;
  }

  public String getThumbnailUrl() {
    return thumbnailUrl;
  }

  @Override
  public MessageType getType() {
    return MessageType.IMAGE;
  }
}