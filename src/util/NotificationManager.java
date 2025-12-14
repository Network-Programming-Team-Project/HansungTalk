package util;

import java.awt.*;
import java.awt.TrayIcon.MessageType;

/**
 * 데스크탑 푸시 알림 관리 클래스
 * SystemTray를 사용하여 데스크탑 알림을 표시
 */
public class NotificationManager {
  private static TrayIcon trayIcon;
  private static boolean initialized = false;
  private static boolean notificationsEnabled = true;

  /**
   * 알림 활성화/비활성화 설정
   */
  public static void setNotificationsEnabled(boolean enabled) {
    notificationsEnabled = enabled;
  }

  /**
   * 시스템 트레이 초기화
   */
  public static synchronized void initialize() {
    if (initialized)
      return;

    if (!SystemTray.isSupported()) {
      ClientLogger.network("System tray is not supported on this platform");
      return;
    }

    try {
      SystemTray tray = SystemTray.getSystemTray();

      // 트레이 아이콘 이미지 생성 (간단한 노란색 원)
      Image image = createTrayImage();

      trayIcon = new TrayIcon(image, "KakaoTalk");
      trayIcon.setImageAutoSize(true);

      tray.add(trayIcon);
      initialized = true;
      ClientLogger.network("NotificationManager initialized successfully");
    } catch (Exception e) {
      ClientLogger.network("Failed to initialize NotificationManager: " + e.getMessage());
    }
  }

  /**
   * 트레이 아이콘용 간단한 이미지 생성
   */
  private static Image createTrayImage() {
    int size = 16;
    java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(
        size, size, java.awt.image.BufferedImage.TYPE_INT_ARGB);
    Graphics2D g2 = image.createGraphics();
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    // 카카오 노란색 원
    g2.setColor(new Color(254, 229, 0));
    g2.fillOval(0, 0, size, size);

    // 간단한 말풍선 모양
    g2.setColor(new Color(60, 30, 30));
    g2.fillOval(4, 4, 8, 6);
    int[] xPoints = { 6, 10, 5 };
    int[] yPoints = { 9, 9, 12 };
    g2.fillPolygon(xPoints, yPoints, 3);

    g2.dispose();
    return image;
  }

  /**
   * 데스크탑 알림 표시
   * 
   * @param title   알림 제목
   * @param message 알림 내용
   */
  public static void showNotification(String title, String message) {
    if (!notificationsEnabled)
      return;

    if (!initialized) {
      initialize();
    }

    if (trayIcon != null) {
      try {
        trayIcon.displayMessage(title, message, MessageType.INFO);
      } catch (Exception e) {
        ClientLogger.network("Failed to show notification: " + e.getMessage());
      }
    }
  }

  /**
   * 새 메시지 알림 표시 (편의 메소드)
   * 
   * @param sender  발신자 이름
   * @param preview 메시지 미리보기
   */
  public static void showMessageNotification(String sender, String preview) {
    // 미리보기 길이 제한
    if (preview.length() > 50) {
      preview = preview.substring(0, 47) + "...";
    }
    showNotification(sender, preview);
  }

  /**
   * 리소스 정리
   */
  public static void cleanup() {
    if (trayIcon != null && SystemTray.isSupported()) {
      try {
        SystemTray.getSystemTray().remove(trayIcon);
      } catch (Exception e) {
        // ignore
      }
    }
    initialized = false;
  }
}
