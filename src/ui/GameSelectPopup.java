package ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * 게임 선택 팝업 메뉴 클래스
 * 미니게임 목록을 표시하고 게임 초대 이벤트를 처리
 */
public class GameSelectPopup extends JPopupMenu {
  private Consumer<String> onSelect; // 게임 선택 시 콜백

  // 게임 이름 -> 내부 코드 매핑
  private static final Map<String, String> GAMES = new HashMap<>();

  // 이미지 캐시 (반복 로딩 방지)
  private static final Map<String, ImageIcon> imageCache = new ConcurrentHashMap<>();

  static {
    GAMES.put("슈팅 게임", "SPACE"); // 슈팅 게임
    GAMES.put("벽돌깨기", "BRICK"); // 벽돌깨기 게임
    GAMES.put("타이핑 게임", "TYPING"); // 타이핑 게임
    GAMES.put("배구 게임", "VOLLEY"); // 배구 게임
  }

  /** 생성자: 팝업 메뉴 초기화 */
  public GameSelectPopup(Consumer<String> onSelect) {
    this.onSelect = onSelect;
    setLayout(new GridLayout(2, 2, 5, 5));
    setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
    setBackground(Color.WHITE);
    setPreferredSize(new Dimension(220, 220));

    // GridLayout에 맞춰 순서 배치
    add(createGameButton("슈팅 게임", "space"));
    add(createGameButton("벽돌깨기", "brick"));
    add(createGameButton("타이핑 게임", "typing"));
    add(createGameButton("배구 게임", "volley"));
  }

  private JPanel createGameButton(String gameName, String iconName) {
    JPanel panel = new JPanel(new BorderLayout());
    panel.setBackground(Color.WHITE);
    panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    panel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

    JLabel iconLabel = new JLabel();
    iconLabel.setPreferredSize(new Dimension(60, 60));
    iconLabel.setHorizontalAlignment(SwingConstants.CENTER);

    // 이미지 비동기 로딩 (캐시 사용, EDT 블로킹 방지)
    if (imageCache.containsKey(iconName)) {
      iconLabel.setIcon(imageCache.get(iconName));
    } else {
      new Thread(() -> {
        String path = "src/assets/games/" + iconName + ".png";
        ImageIcon icon = new ImageIcon(path);
        Image img = icon.getImage().getScaledInstance(60, 60, Image.SCALE_SMOOTH);
        ImageIcon scaledIcon = new ImageIcon(img);
        imageCache.put(iconName, scaledIcon);
        SwingUtilities.invokeLater(() -> iconLabel.setIcon(scaledIcon));
      }).start();
    }

    JLabel textLabel = new JLabel(gameName);
    textLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
    textLabel.setHorizontalAlignment(SwingConstants.CENTER);
    textLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));

    panel.add(iconLabel, BorderLayout.CENTER);
    panel.add(textLabel, BorderLayout.SOUTH);

    panel.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        // 내부 코드 반환
        onSelect.accept(GAMES.get(gameName));
        setVisible(false);
      }

      @Override
      public void mouseEntered(MouseEvent e) {
        panel.setBackground(new Color(240, 240, 240));
      }

      @Override
      public void mouseExited(MouseEvent e) {
        panel.setBackground(Color.WHITE);
      }
    });

    return panel;
  }
}
