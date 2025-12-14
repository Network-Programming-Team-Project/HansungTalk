package domain;

/**
 * 사용자 정보를 담는 도메인 클래스
 * 카카오톡 사용자의 기본 정보(ID, 이름, 상태메시지, 프로필 이모지)를 관리
 */
public class User {
  private String id; // 사용자 고유 ID
  private String name; // 사용자 이름 (표시명)
  private String statusMessage; // 상태 메시지
  private Emoji profileEmoji; // 프로필 이모지

  /** 생성자: 모든 필드를 초기화 */
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
