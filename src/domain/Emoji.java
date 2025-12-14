package domain;

/**
 * 이모지(이모티콘) 정보를 담는 도메인 클래스
 * 이미지 경로와 설명을 관리
 */
public class Emoji {
  private String imagePath; // 이모지 이미지 파일 경로
  private String description; // 이모지 설명

  /** 생성자: 이미지 경로와 설명 설정 */
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
