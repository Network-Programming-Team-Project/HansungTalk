package ui;

import network.SocketClient;
import util.ClientLogger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;

/**
 * 채팅 페이지 UI 클래스
 * 1:1 또는 그룹 채팅 화면을 담당하며 메시지 표시 및 송수신 처리
 */
public class ChatPage extends JPanel implements SocketClient.MessageListener {
  private ClientApp app; // 부모 앱 참조
  private DefaultListModel<ChatMessage> listModel; // 메시지 목록 모델
  private JList<ChatMessage> messageList; // 메시지 표시 리스트
  private JTextField inputField; // 메시지 입력 필드
  private String otherUsername; // 상대방 또는 채팅방 이름
  private String roomId; // 채팅방 고유 ID

  /** 생성자: 1:1 채팅용 (상대방 이름으로 roomId 자동 생성) */
  public ChatPage(ClientApp app, String otherUsername) {
    ClientLogger.ui("ChatPage constructor called with otherUsername: " + otherUsername);
    this.app = app;
    this.otherUsername = otherUsername;
    ClientLogger.ui("Generating roomId...");
    this.roomId = generateRoomId(app.getSocketClient().getUsername(), otherUsername);
    ClientLogger.ui("Generated roomId: " + roomId);

    initUI(otherUsername);
    ClientLogger.ui("ChatPage initUI completed");
  }

  public ChatPage(ClientApp app, String roomName, String roomId) {
    ClientLogger.ui("ChatPage constructor called with roomName: " + roomName + ", roomId: " + roomId);
    this.app = app;
    this.otherUsername = roomName; // Use room name as display name
    this.roomId = roomId;

    initUI(roomName);
    ClientLogger.ui("ChatPage initUI completed");
  }

  private void initUI(String title) {
    setLayout(new BorderLayout());

    // Header
    JPanel header = new JPanel(new BorderLayout());
    header.setBackground(KakaoColors.CHAT_BACKGROUND);
    header.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(170, 190, 210)),
        BorderFactory.createEmptyBorder(12, 15, 12, 15)));

    JButton backButton = new JButton("<");
    backButton.setBorderPainted(false);
    backButton.setContentAreaFilled(false);
    backButton.setFont(new Font("SansSerif", Font.PLAIN, 24));
    backButton.setForeground(KakaoColors.TEXT_PRIMARY);
    backButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    backButton.addActionListener(e -> app.showPage("Main"));

    JPanel titlePanel = new JPanel(new BorderLayout());
    titlePanel.setOpaque(false);

    JLabel titleLabel = new JLabel(title);
    titleLabel.setFont(new Font("SansSerif", Font.BOLD, 17));
    titleLabel.setForeground(KakaoColors.TEXT_PRIMARY);

    // 참여자 수 - roomId에서 계산
    int memberCountNum = 2; // 1:1 채팅 기본값
    if (roomId != null && roomId.startsWith("group_")) {
      // 그룹 채팅의 경우 서버 동기화 필요
      memberCountNum = 2;
    }
    JLabel memberCount = new JLabel(String.valueOf(memberCountNum));
    memberCount.setFont(new Font("SansSerif", Font.PLAIN, 13));
    memberCount.setForeground(KakaoColors.TEXT_SECONDARY);
    memberCount.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));

    titlePanel.add(titleLabel, BorderLayout.WEST);
    titlePanel.add(memberCount, BorderLayout.CENTER);

    header.add(backButton, BorderLayout.WEST);
    header.add(titlePanel, BorderLayout.CENTER);

    // Right side icons removed per user request

    add(header, BorderLayout.NORTH);

    // Message List
    listModel = new DefaultListModel<>();
    messageList = new JList<>(listModel);
    messageList.setCellRenderer(new ChatBubbleRenderer());
    messageList.setBackground(KakaoColors.CHAT_BACKGROUND);
    messageList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    messageList.setFixedCellHeight(-1); // 가변 높이 허용

    // 게임 초대 카드 클릭 리스너 (CellRenderer의 버튼은 이벤트를 받지 못함)
    messageList.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        int index = messageList.locationToIndex(e.getPoint());
        if (index >= 0) {
          ChatMessage msg = listModel.getElementAt(index);
          if (msg.isGameInvite) {
            ClientLogger.ui("Game invite clicked: " + msg.content);
            launchGame(msg.content);
          }
        }
      }
    });

    JScrollPane scrollPane = new JScrollPane(messageList);
    scrollPane.setBorder(null);
    scrollPane.getVerticalScrollBar().setUnitIncrement(16);
    add(scrollPane, BorderLayout.CENTER);

    // 입력 영역
    JPanel inputPanel = new JPanel(new BorderLayout(8, 0));
    inputPanel.setBackground(Color.WHITE);
    inputPanel.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createMatteBorder(1, 0, 0, 0, KakaoColors.DIVIDER),
        BorderFactory.createEmptyBorder(8, 12, 8, 12)));

    JButton plusButton = new JButton("사진");
    plusButton.setBorderPainted(false);
    plusButton.setContentAreaFilled(false);
    plusButton.setFont(new Font("SansSerif", Font.PLAIN, 13));
    plusButton.setForeground(KakaoColors.TEXT_SECONDARY);
    plusButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    plusButton.setPreferredSize(new Dimension(50, 35));
    plusButton.addActionListener(this::sendImageAction);

    JButton emojiButton = new JButton("이모티콘");
    emojiButton.setBorderPainted(false);
    emojiButton.setContentAreaFilled(false);
    emojiButton.setFont(new Font("SansSerif", Font.PLAIN, 13));
    emojiButton.setForeground(KakaoColors.TEXT_SECONDARY);
    emojiButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    emojiButton.setPreferredSize(new Dimension(60, 35));
    emojiButton.addActionListener(e -> showEmoticonPopup(emojiButton));

    JButton gameButton = new JButton("게임");
    gameButton.setBorderPainted(false);
    gameButton.setContentAreaFilled(false);
    gameButton.setFont(new Font("SansSerif", Font.PLAIN, 13));
    gameButton.setForeground(KakaoColors.TEXT_SECONDARY);
    gameButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    gameButton.setPreferredSize(new Dimension(50, 35));
    gameButton.addActionListener(e -> showGameSelectPopup(gameButton));

    // 버튼 호버 효과 추가
    addHoverEffect(plusButton);
    addHoverEffect(emojiButton);
    addHoverEffect(gameButton);

    // 입력 필드 래퍼
    JPanel inputWrapper = new JPanel(new BorderLayout());
    inputWrapper.setBackground(new Color(245, 245, 245));
    inputWrapper.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

    inputField = new JTextField();
    inputField.setBorder(null);
    inputField.setFont(new Font("SansSerif", Font.PLAIN, 14));
    inputField.setBackground(new Color(245, 245, 245));
    inputField.addActionListener(this::sendMessage);

    inputWrapper.add(inputField, BorderLayout.CENTER);

    JButton sendButton = new JButton("전송");
    sendButton.setBackground(KakaoColors.KAKAO_YELLOW);
    sendButton.setForeground(KakaoColors.KAKAO_BROWN);
    sendButton.setFont(new Font("SansSerif", Font.BOLD, 13));
    sendButton.setBorderPainted(false);
    sendButton.setFocusPainted(false);
    sendButton.setOpaque(true);
    sendButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    sendButton.setPreferredSize(new Dimension(55, 35));
    sendButton.addActionListener(this::sendMessage);

    // 전송 버튼 호버 효과
    sendButton.addMouseListener(new MouseAdapter() {
      public void mouseEntered(MouseEvent e) {
        sendButton.setBackground(new Color(240, 215, 0)); // 약간 어두운 노란색
      }

      public void mouseExited(MouseEvent e) {
        sendButton.setBackground(KakaoColors.KAKAO_YELLOW);
      }
    });

    JPanel leftButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
    leftButtons.setBackground(Color.WHITE);
    leftButtons.add(plusButton);
    leftButtons.add(emojiButton);
    leftButtons.add(gameButton);

    inputPanel.add(leftButtons, BorderLayout.WEST);
    inputPanel.add(inputWrapper, BorderLayout.CENTER);
    inputPanel.add(sendButton, BorderLayout.EAST);

    add(inputPanel, BorderLayout.SOUTH);

    // 메시지 리스너 설정 (room join 전에 설정 - 히스토리가 즉시 전송됨)
    if (app.getSocketClient() != null) {
      app.getSocketClient().setMessageListener(this);

      // UI 문제 방지를 위해 백그라운드 스레드에서 네트워크 작업 수행
      new Thread(() -> {
        app.getSocketClient().joinRoom(roomId);
      }).start();
    }
  }

  /** 두 사용자 이름에서 방 ID 생성 (알파벳 순으로 정렬) */
  private String generateRoomId(String user1, String user2) {
    if (user1.compareTo(user2) < 0) {
      return user1 + "_" + user2;
    } else {
      return user2 + "_" + user1;
    }
  }

  /** 메시지 전송 처리 */
  private void sendMessage(ActionEvent e) {
    String text = inputField.getText().trim();
    if (!text.isEmpty() && app.getSocketClient() != null) {
      app.getSocketClient().sendRoomMessage(roomId, text);

      // 내 메시지 목록에 추가
      ChatMessage msg = new ChatMessage(app.getSocketClient().getUsername(), text, true);
      listModel.addElement(msg);
      inputField.setText("");

      // 하단으로 스크롤
      messageList.ensureIndexIsVisible(listModel.getSize() - 1);
    }
  }

  /** 이미지 전송 처리 */
  private void sendImageAction(ActionEvent e) {
    JFileChooser fileChooser = new JFileChooser();
    int result = fileChooser.showOpenDialog(this);
    if (result == JFileChooser.APPROVE_OPTION) {
      File selectedFile = fileChooser.getSelectedFile();
      if (app.getSocketClient() != null) {
        app.getSocketClient().sendRoomImage(roomId, selectedFile);

        // 즉시 표시 (스케일링 최적화)
        new Thread(() -> {
          try {
            ImageIcon icon = new ImageIcon(selectedFile.getAbsolutePath());
            Image img = icon.getImage();
            // EDT 외부에서 스케일링
            Image resized = img.getScaledInstance(200, -1, Image.SCALE_SMOOTH);
            ImageIcon resizedIcon = new ImageIcon(resized);

            SwingUtilities.invokeLater(() -> {
              ChatMessage msg = new ChatMessage(app.getSocketClient().getUsername(), resizedIcon, true);
              listModel.addElement(msg);
              messageList.ensureIndexIsVisible(listModel.getSize() - 1);
            });
          } catch (Exception ex) {
            ex.printStackTrace();
          }
        }).start();
      }
    }
  }

  /** 하단으로 스크롤 */
  private void scrollToBottom() {
    SwingUtilities.invokeLater(() -> {
      int lastIndex = listModel.getSize() - 1;
      if (lastIndex >= 0) {
        messageList.ensureIndexIsVisible(lastIndex);
      }
    });
  }

  @Override
  public void onMessageReceived(String message) {
    if (message.startsWith("MSG:")) {
      int firstColon = message.indexOf(':');
      int secondColon = message.indexOf(':', firstColon + 1);
      int thirdColon = message.indexOf(':', secondColon + 1);

      if (thirdColon != -1) {
        String sender = message.substring(firstColon + 1, secondColon);
        String unreadCountStr = message.substring(secondColon + 1, thirdColon);
        String content = message.substring(thirdColon + 1).trim();

        int unread = 0;
        try {
          unread = Integer.parseInt(unreadCountStr);
        } catch (NumberFormatException e) {
        }

        int finalUnread = unread;
        SwingUtilities.invokeLater(() -> {
          ChatMessage msg = new ChatMessage(sender, content, false);
          msg.unreadCount = finalUnread;
          listModel.addElement(msg);
          scrollToBottom();
        });
      }
    } else if (message.startsWith("GAME_RESULT:")) {
      // 형식: GAME_RESULT:roomId:GAME_SYSTEM:scoreMsg
      String[] parts = message.split(":", 4);
      if (parts.length == 4) {
        String sender = parts[2];
        String content = parts[3];
        // 시스템 메시지로 표시
        SwingUtilities.invokeLater(() -> {
          listModel.addElement(new ChatMessage(sender, "🎮 " + content, false));
          scrollToBottom();
        });
      }
    }
  }

  @Override
  public void onImageReceived(String sender, ImageIcon image) {
    new Thread(() -> {
      // EDT 외부에서 리사이즈
      Image img = image.getImage();
      Image newImg = img.getScaledInstance(200, -1, Image.SCALE_SMOOTH);
      ImageIcon resizedIcon = new ImageIcon(newImg);

      SwingUtilities.invokeLater(() -> {
        listModel.addElement(new ChatMessage(sender, resizedIcon, false));
        scrollToBottom();
      });
    }).start();
  }

  /** 이모티콘 팝업 표시 */
  private void showEmoticonPopup(Component invoker) {
    EmoticonPopup popup = new EmoticonPopup(emojiName -> {
      if (app.getSocketClient() != null) {
        app.getSocketClient().sendRoomEmoji(roomId, emojiName);

        // 로컬 목록에 추가 (EDT 블로킹 방지를 위해 비동기 이미지 로딩)
        new Thread(() -> {
          ImageIcon icon = new ImageIcon("src/assets/emoticons/" + emojiName + ".png");
          Image img = icon.getImage().getScaledInstance(100, 100, Image.SCALE_SMOOTH);
          ImageIcon scaledIcon = new ImageIcon(img);
          SwingUtilities.invokeLater(() -> {
            ChatMessage msg = new ChatMessage(app.getSocketClient().getUsername(), scaledIcon, true);
            listModel.addElement(msg);
            scrollToBottom();
          });
        }).start();
      }
    });
    popup.show(invoker, 0, -200);
  }

  /** 게임 선택 팝업 표시 */
  private void showGameSelectPopup(Component invoker) {
    GameSelectPopup popup = new GameSelectPopup(gameType -> {
      if (app.getSocketClient() != null) {
        app.getSocketClient().sendGameInvite(roomId, gameType);

        // 로컬 목록에 추가
        ChatMessage msg = new ChatMessage(app.getSocketClient().getUsername(), gameType, true, true);
        listModel.addElement(msg);
        scrollToBottom();
      }
    });
    popup.show(invoker, 0, -220);
  }

  /** 이모지 수신 콜백 */
  @Override
  public void onEmojiReceived(String sender, String emojiName) {
    // EDT 블로킹 방지를 위해 이미지 로딩 및 스케일링
    new Thread(() -> {
      ImageIcon icon = new ImageIcon("src/assets/emoticons/" + emojiName + ".png");
      Image img = icon.getImage().getScaledInstance(100, 100, Image.SCALE_SMOOTH);
      ImageIcon scaledIcon = new ImageIcon(img);
      SwingUtilities.invokeLater(() -> {
        listModel.addElement(new ChatMessage(sender, scaledIcon, false));
        scrollToBottom();
      });
    }).start();
  }

  @Override
  public void onGameInviteReceived(String sender, String gameType) {
    SwingUtilities.invokeLater(() -> {
      listModel.addElement(new ChatMessage(sender, gameType, false, true));
      scrollToBottom();
    });
  }

  @Override
  public void addNotify() {
    super.addNotify();
    if (app.getSocketClient() != null) {
      app.getSocketClient().setMessageListener(this);
    }
  }

  /** 메시지 데이터 클래스 */
  private static class ChatMessage {
    String sender; // 발신자
    String content; // 메시지 내용
    ImageIcon image; // 이미지 (이미지 메시지인 경우)
    boolean isMine; // 내 메시지 여부
    boolean isImage; // 이미지 메시지 여부
    boolean isGameInvite; // 게임 초대 여부
    int unreadCount = 0; // 안읽은 수
    long timestamp; // 타임스탬프

    public ChatMessage(String sender, String content, boolean isMine) {
      this.sender = sender;
      this.content = content;
      this.isMine = isMine;
      this.isImage = false;
      this.isGameInvite = false;
      this.timestamp = System.currentTimeMillis();
    }

    /** 이미지 메시지 생성자 */
    public ChatMessage(String sender, ImageIcon image, boolean isMine) {
      this.sender = sender;
      this.image = image;
      this.isMine = isMine;
      this.isImage = true;
      this.isGameInvite = false;
      this.timestamp = System.currentTimeMillis();
    }

    /** 게임 초대 메시지 생성자 */
    public ChatMessage(String sender, String gameType, boolean isMine, boolean isGameInvite) {
      this.sender = sender;
      this.content = gameType; // 게임 타입 코드 저장
      this.isMine = isMine;
      this.isImage = false;
      this.isGameInvite = true;
      this.timestamp = System.currentTimeMillis();
    }
  }

  private final java.text.SimpleDateFormat timeFormat = new java.text.SimpleDateFormat("HH:mm");

  /** 채팅 버블 커스텀 렌더러 */
  private class ChatBubbleRenderer extends JPanel implements ListCellRenderer<ChatMessage> {

    public ChatBubbleRenderer() {
      setOpaque(true);
      setBackground(KakaoColors.CHAT_BACKGROUND);
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends ChatMessage> list, ChatMessage value, int index,
        boolean isSelected, boolean cellHasFocus) {

      removeAll();
      setLayout(new BorderLayout());
      setBorder(BorderFactory.createEmptyBorder(4, 12, 4, 12));

      if (value.isMine) {
        // My message (right-aligned, yellow)
        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.X_AXIS));
        container.setOpaque(false);
        container.add(Box.createHorizontalGlue());

        // Timestamp (left of bubble)
        JLabel timeLabel = new JLabel(timeFormat.format(new java.util.Date(value.timestamp)));
        timeLabel.setFont(new Font("SansSerif", Font.PLAIN, 10));
        timeLabel.setForeground(new Color(130, 130, 130));
        timeLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 2, 6));
        timeLabel.setAlignmentY(Component.BOTTOM_ALIGNMENT);

        // Bubble
        JPanel bubble = createBubble(value, true);
        bubble.setAlignmentY(Component.BOTTOM_ALIGNMENT);

        // Unread Count (Between bubble and time)
        if (value.unreadCount > 0) {
          JLabel unreadLabel = new JLabel(String.valueOf(value.unreadCount));
          unreadLabel.setFont(new Font("SansSerif", Font.BOLD, 10));
          unreadLabel.setForeground(new Color(255, 200, 0)); // Yellow/Orange
          unreadLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 2, 4));
          unreadLabel.setAlignmentY(Component.BOTTOM_ALIGNMENT);
          container.add(unreadLabel);
        }

        container.add(timeLabel);
        container.add(bubble);
        add(container, BorderLayout.EAST);

      } else {
        // Other's message (left-aligned, white)
        JPanel container = new JPanel(new BorderLayout(8, 4));
        container.setOpaque(false);

        // Profile icon
        JPanel profileIcon = createProfileIcon();

        // Content panel
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);

        // Name
        JLabel nameLabel = new JLabel(value.sender);
        nameLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        nameLabel.setForeground(KakaoColors.TEXT_PRIMARY);
        nameLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
        nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Bubble + Time container
        JPanel bubbleTimePanel = new JPanel();
        bubbleTimePanel.setLayout(new BoxLayout(bubbleTimePanel, BoxLayout.X_AXIS));
        bubbleTimePanel.setOpaque(false);
        bubbleTimePanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel bubble = createBubble(value, false);
        bubble.setAlignmentY(Component.BOTTOM_ALIGNMENT);

        JLabel timeLabel = new JLabel(timeFormat.format(new java.util.Date(value.timestamp)));
        timeLabel.setFont(new Font("SansSerif", Font.PLAIN, 10));
        timeLabel.setForeground(new Color(130, 130, 130));
        timeLabel.setBorder(BorderFactory.createEmptyBorder(0, 6, 2, 0));
        timeLabel.setAlignmentY(Component.BOTTOM_ALIGNMENT);

        bubbleTimePanel.add(bubble);

        // Unread Count for others (next to bubble)
        if (value.unreadCount > 0) {
          JLabel unreadLabel = new JLabel(String.valueOf(value.unreadCount));
          unreadLabel.setFont(new Font("SansSerif", Font.BOLD, 10));
          unreadLabel.setForeground(new Color(255, 200, 0)); // Yellow/Orange
          unreadLabel.setBorder(BorderFactory.createEmptyBorder(0, 4, 2, 0));
          unreadLabel.setAlignmentY(Component.BOTTOM_ALIGNMENT);
          bubbleTimePanel.add(unreadLabel);
        }

        bubbleTimePanel.add(timeLabel);
        bubbleTimePanel.add(Box.createHorizontalGlue());

        contentPanel.add(nameLabel);
        contentPanel.add(bubbleTimePanel);

        container.add(profileIcon, BorderLayout.WEST);
        container.add(contentPanel, BorderLayout.CENTER);
        add(container, BorderLayout.WEST);
      }

      return this;
    }

    private JPanel createBubble(ChatMessage msg, boolean isMine) {
      JPanel bubble = new JPanel(new BorderLayout()) {
        @Override
        protected void paintComponent(Graphics g) {
          Graphics2D g2 = (Graphics2D) g.create();
          g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

          // Background
          if (msg.isGameInvite) {
            g2.setColor(Color.WHITE); // Game invites are always white cards
          } else {
            g2.setColor(isMine ? KakaoColors.MY_BUBBLE : KakaoColors.OTHER_BUBBLE);
          }

          g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);

          // Subtle shadow/border
          g2.setColor(new Color(0, 0, 0, 20));
          g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);

          g2.dispose();
          super.paintComponent(g);
        }
      };

      bubble.setOpaque(false);

      if (msg.isGameInvite) {
        bubble.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Game Invite UI
        JPanel card = new JPanel(new BorderLayout(10, 5));
        card.setOpaque(false);

        String gameType = msg.content;
        String gameName = "알 수 없는 게임";
        String iconName = "space";

        switch (gameType) {
          case "SPACE":
            gameName = "슈팅 게임";
            iconName = "space";
            break;
          case "BRICK":
            gameName = "벽돌깨기";
            iconName = "brick";
            break;
          case "TYPING":
            gameName = "타이핑 게임";
            iconName = "typing";
            break;
          case "VOLLEY":
            gameName = "배구 게임";
            iconName = "volley";
            break;
        }

        // Header
        JLabel titleLabel = new JLabel("초대장이 도착했습니다!");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        titleLabel.setForeground(KakaoColors.TEXT_PRIMARY);

        // Content (Icon + Name)
        JPanel content = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        content.setOpaque(false);

        ImageIcon icon = new ImageIcon("src/assets/games/" + iconName + ".png");
        Image img = icon.getImage().getScaledInstance(50, 50, Image.SCALE_SMOOTH);
        JLabel iconLabel = new JLabel(new ImageIcon(img));

        JLabel nameLabel = new JLabel(gameName);
        nameLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));

        content.add(iconLabel);
        content.add(nameLabel);

        // Context for lambda
        final String finalGameName = gameName;

        // Button
        JButton playButton = new JButton("게임하기");
        playButton.setBackground(KakaoColors.KAKAO_YELLOW);
        playButton.setForeground(KakaoColors.KAKAO_BROWN);
        playButton.setFont(new Font("SansSerif", Font.BOLD, 12));
        playButton.setBorderPainted(false);
        playButton.setFocusPainted(false);
        playButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        playButton.addActionListener(e -> {
          launchGame(msg.content);
        });

        card.add(titleLabel, BorderLayout.NORTH);
        card.add(content, BorderLayout.CENTER);
        card.add(playButton, BorderLayout.SOUTH);

        bubble.add(card, BorderLayout.CENTER); // Add card to bubble!

      } else if (msg.isImage) {
        bubble.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        JLabel imgLabel = new JLabel(msg.image);
        bubble.add(imgLabel, BorderLayout.CENTER);
      } else {
        bubble.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        JTextArea textArea = new JTextArea(msg.content);
        textArea.setWrapStyleWord(true);
        textArea.setLineWrap(true);
        textArea.setEditable(false);
        textArea.setOpaque(false);
        textArea.setFont(new Font("SansSerif", Font.PLAIN, 14));
        textArea.setForeground(isMine ? KakaoColors.KAKAO_BROWN : KakaoColors.TEXT_PRIMARY);

        // Calculate preferred size
        int maxWidth = 250;
        textArea.setSize(maxWidth, Short.MAX_VALUE);
        Dimension d = textArea.getPreferredSize();
        textArea.setPreferredSize(new Dimension(Math.min(maxWidth, d.width), d.height));

        bubble.add(textArea, BorderLayout.CENTER);
      }

      return bubble;
    }

    private JPanel createProfileIcon() {
      JPanel icon = new JPanel() {
        @Override
        protected void paintComponent(Graphics g) {
          super.paintComponent(g);
          Graphics2D g2 = (Graphics2D) g.create();
          g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

          // Background (squircle-ish)
          g2.setColor(KakaoColors.PROFILE_PLACEHOLDER);
          g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);

          // Simple avatar icon
          g2.setColor(Color.WHITE);
          g2.fillOval(10, 6, 18, 18);
          g2.fillArc(6, 20, 26, 20, 0, 180);

          g2.dispose();
        }
      };

      icon.setOpaque(false);
      icon.setPreferredSize(new Dimension(38, 38));
      return icon;
    }
  }

  private void addHoverEffect(JButton button) {
    button.addMouseListener(new MouseAdapter() {
      public void mouseEntered(MouseEvent e) {
        button.setContentAreaFilled(true);
        button.setBackground(new Color(240, 240, 240));
      }

      public void mouseExited(MouseEvent e) {
        button.setContentAreaFilled(false);
        button.setBackground(null);
      }
    });
  }

  private void launchGame(String gameType) {
    ClientLogger.ui("launchGame called with gameType: " + gameType);

    String projectPath = "";
    String mainClass = "";

    switch (gameType) {
      case "SPACE":
        projectPath = "MINIGAMES/ShootingGame";
        mainClass = "game.ShootingGame";
        break;
      case "BRICK":
        projectPath = "MINIGAMES/SwipeBreakoutGame";
        mainClass = "client.VersusFrame";
        break;
      case "TYPING":
        projectPath = "MINIGAMES/TypingGame";
        mainClass = "client.TypingGameClient";
        break;
      case "VOLLEY":
        projectPath = "MINIGAMES/VolleyGame";
        mainClass = "client.VolleyClient";
        break;
      default:
        ClientLogger.error("Unknown game type: " + gameType);
        JOptionPane.showMessageDialog(this, "알 수 없는 게임입니다.", "오류", JOptionPane.ERROR_MESSAGE);
        return;
    }

    final String finalProjectPath = projectPath;
    final String finalMainClass = mainClass;

    new Thread(() -> {
      try {
        String userDir = System.getProperty("user.dir");
        ClientLogger.ui("user.dir: " + userDir);

        String rootDir = new java.io.File(userDir).getParent();
        ClientLogger.ui("Initial rootDir (parent of user.dir): " + rootDir);

        if (!new File(rootDir, "MINIGAMES").exists()) {
          ClientLogger.ui("MINIGAMES not found at " + rootDir + "/MINIGAMES");
          if (new File(userDir, "MINIGAMES").exists()) {
            rootDir = userDir;
            ClientLogger.ui("Found MINIGAMES at user.dir, using: " + rootDir);
          }
        }

        String gameBinPath = rootDir + "/" + finalProjectPath + "/bin";
        ClientLogger.ui("Game bin path: " + gameBinPath);

        File binDir = new File(gameBinPath);
        if (!binDir.exists()) {
          ClientLogger.error("Bin directory does not exist: " + gameBinPath);
          SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "게임 바이너리를 찾을 수 없습니다: " + gameBinPath,
              "오류", JOptionPane.ERROR_MESSAGE));
          return;
        }

        String username = app.getSocketClient().getUsername();

        ClientLogger.ui("Launching: java -cp " + gameBinPath + " " + finalMainClass + " " + username + " " + roomId);

        ProcessBuilder pb = new ProcessBuilder("java", "-cp", gameBinPath, finalMainClass, username, roomId);
        pb.directory(new File(rootDir, finalProjectPath)); // Set working directory for game assets
        pb.inheritIO();
        Process process = pb.start();
        ClientLogger.ui("Game process started: " + process.toString());

      } catch (Exception e) {
        ClientLogger.error("Failed to launch game", e);
        SwingUtilities.invokeLater(
            () -> JOptionPane.showMessageDialog(this, "게임 실행 실패: " + e.getMessage(), "오류", JOptionPane.ERROR_MESSAGE));
      }
    }).start();
  }
}
