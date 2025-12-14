package ui;

import javax.swing.*;
import java.awt.*;
import java.util.Map;
import java.util.HashMap;
import java.util.function.Consumer;

/**
 * Profile popup that shows user info, game achievements, and 1:1 chat button.
 */
public class UserProfilePopup extends JDialog {
  private String username;
  private String statusMessage;
  private Map<String, Integer> gameScores; // gameType -> best score
  private Consumer<String> onChatClicked;

  public UserProfilePopup(Frame owner, String username, String statusMessage,
      Map<String, Integer> gameScores, Consumer<String> onChatClicked) {
    super(owner, "프로필", true);
    this.username = username;
    this.statusMessage = statusMessage != null ? statusMessage : "";
    this.gameScores = gameScores != null ? gameScores : new HashMap<>();
    this.onChatClicked = onChatClicked;

    initUI();
    setSize(320, 450);
    setLocationRelativeTo(owner);
  }

  private void initUI() {
    setLayout(new BorderLayout());
    getContentPane().setBackground(Color.WHITE);

    // Main panel with padding
    JPanel mainPanel = new JPanel();
    mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
    mainPanel.setBackground(Color.WHITE);
    mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));

    // Profile icon
    JPanel iconPanel = new JPanel() {
      @Override
      protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int size = 80;
        int x = (getWidth() - size) / 2;
        int y = 0;

        // Background circle
        g2.setColor(KakaoColors.PROFILE_PLACEHOLDER);
        g2.fillRoundRect(x, y, size, size, 24, 24);

        // Avatar silhouette
        g2.setColor(Color.WHITE);
        g2.fillOval(x + 22, y + 12, 36, 36);
        g2.fillArc(x + 14, y + 44, 52, 40, 0, 180);

        g2.dispose();
      }
    };
    iconPanel.setOpaque(false);
    iconPanel.setPreferredSize(new Dimension(100, 90));
    iconPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
    iconPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

    // Username
    JLabel nameLabel = new JLabel(username);
    nameLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
    nameLabel.setForeground(KakaoColors.TEXT_PRIMARY);
    nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

    // Status message
    JLabel statusLabel = new JLabel(statusMessage.isEmpty() ? "상태 메시지 없음" : statusMessage);
    statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
    statusLabel.setForeground(KakaoColors.TEXT_SECONDARY);
    statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

    mainPanel.add(iconPanel);
    mainPanel.add(Box.createVerticalStrut(10));
    mainPanel.add(nameLabel);
    mainPanel.add(Box.createVerticalStrut(5));
    mainPanel.add(statusLabel);
    mainPanel.add(Box.createVerticalStrut(20));

    // Divider
    JSeparator sep = new JSeparator();
    sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
    mainPanel.add(sep);
    mainPanel.add(Box.createVerticalStrut(15));

    // Game achievements section
    JLabel achievementsTitle = new JLabel("🎮 게임 성과");
    achievementsTitle.setFont(new Font("SansSerif", Font.BOLD, 14));
    achievementsTitle.setForeground(KakaoColors.TEXT_PRIMARY);
    achievementsTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
    mainPanel.add(achievementsTitle);
    mainPanel.add(Box.createVerticalStrut(10));

    // Game scores
    String[] games = { "SPACE", "BRICK", "TYPING", "VOLLEY" };
    String[] gameNames = { "슈팅 게임", "벽돌깨기", "타이핑 게임", "배구 게임" };
    String[] gameIcons = { "🚀", "🧱", "⌨️", "🏐" };

    for (int i = 0; i < games.length; i++) {
      JPanel gameRow = createGameScoreRow(gameIcons[i], gameNames[i], gameScores.getOrDefault(games[i], 0));
      gameRow.setAlignmentX(Component.LEFT_ALIGNMENT);
      mainPanel.add(gameRow);
      mainPanel.add(Box.createVerticalStrut(8));
    }

    mainPanel.add(Box.createVerticalGlue());

    add(mainPanel, BorderLayout.CENTER);

    // Bottom buttons
    JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
    buttonPanel.setBackground(new Color(248, 248, 248));
    buttonPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(230, 230, 230)));

    JButton chatButton = new JButton("1:1 채팅");
    chatButton.setFont(new Font("SansSerif", Font.BOLD, 14));
    chatButton.setBackground(KakaoColors.KAKAO_YELLOW);
    chatButton.setForeground(KakaoColors.KAKAO_BROWN);
    chatButton.setPreferredSize(new Dimension(120, 36));
    chatButton.setBorderPainted(false);
    chatButton.setFocusPainted(false);
    chatButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    chatButton.addActionListener(e -> {
      dispose();
      if (onChatClicked != null) {
        onChatClicked.accept(username);
      }
    });

    JButton closeButton = new JButton("닫기");
    closeButton.setFont(new Font("SansSerif", Font.PLAIN, 14));
    closeButton.setPreferredSize(new Dimension(80, 36));
    closeButton.addActionListener(e -> dispose());

    buttonPanel.add(chatButton);
    buttonPanel.add(closeButton);

    add(buttonPanel, BorderLayout.SOUTH);
  }

  private JPanel createGameScoreRow(String icon, String gameName, int score) {
    JPanel row = new JPanel(new BorderLayout(10, 0));
    row.setOpaque(false);
    row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));

    JLabel iconLabel = new JLabel(icon + " " + gameName);
    iconLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
    iconLabel.setForeground(KakaoColors.TEXT_PRIMARY);

    JLabel scoreLabel = new JLabel(score > 0 ? String.valueOf(score) + " pts" : "-");
    scoreLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
    scoreLabel.setForeground(score > 0 ? new Color(76, 175, 80) : KakaoColors.TEXT_TERTIARY);

    row.add(iconLabel, BorderLayout.WEST);
    row.add(scoreLabel, BorderLayout.EAST);

    return row;
  }
}
