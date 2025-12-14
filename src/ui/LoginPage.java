package ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * 로그인 페이지 UI 클래스
 * 사용자 이름 입력 및 로그인 처리
 */
public class LoginPage extends JPanel {
  private ClientApp app; // 부모 앱 참조
  private JTextField usernameField; // 사용자 이름 입력 필드

  /** 생성자: UI 초기화 */
  public LoginPage(ClientApp app) {
    this.app = app;
    setLayout(new GridBagLayout());
    setBackground(new Color(254, 229, 0)); // Kakao Yellow

    GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(10, 10, 10, 10);
    gbc.fill = GridBagConstraints.HORIZONTAL;

    // Logo / Title
    JLabel titleLabel = new JLabel("KakaoTalk");
    titleLabel.setFont(new Font("SansSerif", Font.BOLD, 40));
    titleLabel.setForeground(new Color(60, 30, 30)); // Kakao Brown
    titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.gridwidth = 2;
    gbc.insets = new Insets(0, 0, 80, 0); // Bottom padding
    add(titleLabel, gbc);

    // Reset insets
    gbc.insets = new Insets(5, 40, 5, 40);
    gbc.gridwidth = 2;

    // Username Input
    usernameField = new JTextField(20);
    usernameField.setPreferredSize(new Dimension(200, 45));
    usernameField.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(new Color(230, 230, 230), 1),
        BorderFactory.createEmptyBorder(5, 10, 5, 10)));
    usernameField.setFont(new Font("SansSerif", Font.PLAIN, 14));
    gbc.gridy = 1;
    add(usernameField, gbc);

    // Login Button
    JButton loginButton = new JButton("Log In");
    loginButton.setPreferredSize(new Dimension(200, 45));
    loginButton.setBackground(new Color(60, 30, 30)); // Kakao Brown
    loginButton.setForeground(Color.WHITE);
    loginButton.setFont(new Font("SansSerif", Font.BOLD, 14));
    loginButton.setFocusPainted(false);
    loginButton.setBorderPainted(false);
    loginButton.setOpaque(true);
    loginButton.addActionListener(this::performLogin);
    gbc.gridy = 2;
    gbc.insets = new Insets(20, 40, 5, 40);
    add(loginButton, gbc);
  }

  private void performLogin(ActionEvent e) {
    String username = usernameField.getText().trim();
    if (!username.isEmpty()) {
      app.getSocketClient().start(username);
      app.showPage("Main");
    } else {
      JOptionPane.showMessageDialog(this, "Please enter a username.", "Login Error", JOptionPane.ERROR_MESSAGE);
    }
  }
}
