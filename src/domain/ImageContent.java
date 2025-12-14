package domain;

/**
 * 이미지 메시지 내용을 담는 클래스
 * 원본 이미지 URL과 썸네일 URL을 관리
 */
public class ImageContent implements MessageContent {
  private final String imageUrl; // 원본 이미지 URL
  private final String thumbnailUrl; // 썸네일 URL (선택적)

  /** 생성자: 이미지 URL과 썸네일 URL 설정 */
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