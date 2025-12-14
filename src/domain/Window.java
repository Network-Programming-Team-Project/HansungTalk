package domain;

/** 윈도우 타입 열거형: 클라이언트/서버 구분 */
enum WindowType {
  Client, // 클라이언트 윈도우
  Server // 서버 윈도우
}

/** 페이지 타입 열거형: 화면 종류 구분 */
enum PageType {
  Login, // 로그인 페이지
  Main, // 메인 페이지
  Chat, // 채팅 페이지
  Settings // 설정 페이지
}

/**
 * 윈도우(창) 정보를 담는 도메인 클래스
 * 윈도우 타입, 페이지 타입, 제목을 관리
 */
public class Window {
  private WindowType windowType; // 윈도우 타입
  private PageType pageType; // 현재 페이지 타입
  private String title; // 윈도우 제목

  /** 생성자: 모든 필드 초기화 */
  public Window(WindowType windowType, PageType pageType, String title) {
    this.windowType = windowType;
    this.pageType = pageType;
    this.title = title;
  };

  public WindowType getWindowType() {
    return windowType;
  }

  public PageType getPageType() {
    return pageType;
  }

  public String getTitle() {
    return title;
  }
}
