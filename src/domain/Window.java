package domain;

enum WindowType {
  Client,
  Server
}

enum PageType {
  Login,
  Main,
  Chat,
  Settings
}

public class Window {
  private WindowType windowType;
  private PageType pageType;
  private String title;

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
