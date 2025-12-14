package ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 이모티콘 선택 팝업 메뉴 클래스
 * 사용 가능한 이모티콘 목록을 표시하고 선택 이벤트를 처리
 */
public class EmoticonPopup extends JPopupMenu {
  private Consumer<String> onSelect; // 이모티콘 선택 시 콜백

  // 사용 가능한 이모티콘 목록
  private static final String[] EMOTICONS = {
      "sangsang_happy", "sangsang_sad", "sangsang_angry",
      "sangsang_love", "sangsang_ok", "sangsang_hello"
  };

  // 이미지 캐시 (반복 로딩 방지)
  private static final Map<String, ImageIcon> imageCache = new ConcurrentHashMap<>();

  /** 생성자: 팝업 메뉴 초기화 */
  public EmoticonPopup(Consumer<String> onSelect) {
    this.onSelect = onSelect;
    setLayout(new GridLayout(2, 3, 5, 5));
    setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
    setBackground(Color.WHITE);

    for (String emojiName : EMOTICONS) {
      add(createEmojiButton(emojiName));
    }
  }

  private JPanel createEmojiButton(String emojiName) {
    JPanel panel = new JPanel(new BorderLayout());
    panel.setBackground(Color.WHITE);
    panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    panel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

    JLabel label = new JLabel();
    label.setPreferredSize(new Dimension(60, 60));
    label.setHorizontalAlignment(SwingConstants.CENTER);

    // 이미지 비동기 로딩 (캐시 사용, EDT 블로킹 방지)
    if (imageCache.containsKey(emojiName)) {
      label.setIcon(imageCache.get(emojiName));
    } else {
      new Thread(() -> {
        String path = "src/assets/emoticons/" + emojiName + ".png";
        ImageIcon icon = new ImageIcon(path);
        Image img = icon.getImage().getScaledInstance(60, 60, Image.SCALE_SMOOTH);
        ImageIcon scaledIcon = new ImageIcon(img);
        imageCache.put(emojiName, scaledIcon);
        SwingUtilities.invokeLater(() -> label.setIcon(scaledIcon));
      }).start();
    }

    panel.add(label, BorderLayout.CENTER);

    panel.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        onSelect.accept(emojiName);
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
