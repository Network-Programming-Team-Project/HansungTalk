package ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.function.Consumer;

public class EmoticonPopup extends JPopupMenu {
  private Consumer<String> onSelect;
  private static final String[] EMOTICONS = {
      "sangsang_happy", "sangsang_sad", "sangsang_angry",
      "sangsang_love", "sangsang_ok", "sangsang_hello"
  };

  // Image cache to avoid repeated loading
  private static final Map<String, ImageIcon> imageCache = new ConcurrentHashMap<>();

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

    // Load image asynchronously with cache to avoid EDT blocking
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
