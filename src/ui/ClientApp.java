package ui;

import network.SocketClient;
import util.ClientLogger;

import javax.swing.*;
import java.awt.*;

/**
 * 클라이언트 애플리케이션의 메인 클래스
 * 페이지 전환, 소켓 클라이언트 관리 등 전체 앱 제어
 */
public class ClientApp extends JFrame {
  private CardLayout cardLayout; // 페이지 전환용 레이아웃
  private JPanel mainPanel; // 메인 컨테이너 패널
  private SocketClient socketClient; // 서버 연결 소켓 클라이언트

  /** 생성자: 앱 초기화 */
  public ClientApp() {
    setTitle("KakaoTalk");
    setSize(380, 640); // More mobile-like ratio
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setLocationRelativeTo(null);

    socketClient = new SocketClient("localhost", 12345);

    cardLayout = new CardLayout();
    mainPanel = new JPanel(cardLayout);

    // Add Pages
    mainPanel.add(new LoginPage(this), "Login");
    mainPanel.add(new MainPage(this), "Main");
    // Initial dummy chat page (will be replaced when user clicks a friend)
    mainPanel.add(new JPanel(), "Chat");

    add(mainPanel);

    setVisible(true);
    ClientLogger.ui("ClientApp initialized");
  }

  public SocketClient getSocketClient() {
    return socketClient;
  }

  public void showPage(String page) {
    ClientLogger.page("Navigating to: " + page);
    cardLayout.show(mainPanel, page);
  }

  public void showChatWith(String otherUsername) {
    ClientLogger.page("Opening chat with: " + otherUsername);
    // Navigate to chat page with specific user
    ChatPage chatPage = new ChatPage(this, otherUsername);
    mainPanel.remove(2); // Remove old chat page
    mainPanel.add(chatPage, "Chat");
    cardLayout.show(mainPanel, "Chat");
  }

  public void showGroupChat(String roomId, String roomName) {
    ClientLogger.page("[DEBUG] showGroupChat START - roomName: " + roomName + ", roomId: " + roomId);
    ClientLogger.page("[DEBUG] Creating ChatPage...");
    ChatPage chatPage = new ChatPage(this, roomName, roomId);
    ClientLogger.page("[DEBUG] ChatPage created successfully");
    ClientLogger.page("[DEBUG] Removing old chat panel...");
    mainPanel.remove(2);
    ClientLogger.page("[DEBUG] Adding new ChatPage...");
    mainPanel.add(chatPage, "Chat");
    ClientLogger.page("[DEBUG] Showing Chat panel via CardLayout...");
    cardLayout.show(mainPanel, "Chat");
    ClientLogger.page("[DEBUG] showGroupChat END");
  }

  public static void main(String[] args) {
    SwingUtilities.invokeLater(() -> {
      new ClientApp();
    });
  }
}
