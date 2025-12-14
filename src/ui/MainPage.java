package ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * 메인 페이지 UI 클래스
 * 친구 목록, 채팅 목록, 더보기 탭을 관리하는 메인 화면
 */
public class MainPage extends JPanel {
  private ClientApp app; // 부모 앱 참조
  private DefaultListModel<String> listModel; // 목록 데이터 모델

  /** 탭 종류 열거형 */
  private enum Tab {
    FRIENDS, // 친구 목록 탭
    CHATS, // 채팅 목록 탭
    MORE // 더보기 탭
  }

  private Tab currentTab = Tab.FRIENDS; // 현재 선택된 탭
  private JPanel contentPanel; // 메인 컨텐츠 패널
  private JLabel titleLabel; // 페이지 제목 레이블
  private JList<String> mainList; // 메인 리스트
  private SidebarButton friendsBtn; // 친구 탭 버튼
  private SidebarButton chatsBtn; // 채팅 탭 버튼
  private SidebarButton moreBtn; // 더보기 탭 버튼

  // 캐시 데이터
  private String[] cachedUsers = new String[0]; // 온라인 사용자 캐시
  private java.util.Map<String, String> cachedChats = new java.util.HashMap<>(); // 채팅방 캐시
  private int totalUnreadCount = 0; // 총 안읽은 메시지 수

  /** 생성자: 메인 페이지 UI 초기화 */
  public MainPage(ClientApp app) {
    this.app = app;
    setLayout(new BorderLayout());
    setBackground(Color.WHITE);

    // Sidebar
    JPanel sidebar = new JPanel();
    sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
    sidebar.setPreferredSize(new Dimension(66, getHeight()));
    sidebar.setBackground(KakaoColors.SIDEBAR_BG);
    sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, KakaoColors.DIVIDER));

    sidebar.add(Box.createVerticalStrut(30));
    friendsBtn = new SidebarButton(Tab.FRIENDS);
    chatsBtn = new SidebarButton(Tab.CHATS);
    moreBtn = new SidebarButton(Tab.MORE);

    sidebar.add(friendsBtn);
    sidebar.add(Box.createVerticalStrut(20));
    sidebar.add(chatsBtn);
    // Remove MORE tab as requested
    // sidebar.add(Box.createVerticalStrut(20));
    // sidebar.add(moreBtn);

    add(sidebar, BorderLayout.WEST);

    // Main Content Area
    contentPanel = new JPanel(new BorderLayout());
    contentPanel.setBackground(Color.WHITE);

    // Header
    JPanel header = new JPanel(new BorderLayout());
    header.setBackground(Color.WHITE);
    header.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createMatteBorder(0, 0, 1, 0, KakaoColors.DIVIDER),
        BorderFactory.createEmptyBorder(15, 20, 15, 20)));

    titleLabel = new JLabel("Friends");
    titleLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
    titleLabel.setForeground(KakaoColors.TEXT_PRIMARY);
    header.add(titleLabel, BorderLayout.WEST);

    JPanel headerIcons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
    headerIcons.setBackground(Color.WHITE);

    // Removed search icon as requested
    // JLabel searchIcon = new JLabel("🔍");
    // searchIcon.setFont(new Font("SansSerif", Font.PLAIN, 18));
    JLabel addIcon = new JLabel("➕");
    addIcon.setFont(new Font("SansSerif", Font.PLAIN, 18));
    addIcon.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    addIcon.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        openUserSelectDialog();
      }
    });

    // headerIcons.add(searchIcon);
    headerIcons.add(addIcon);
    header.add(headerIcons, BorderLayout.EAST);

    contentPanel.add(header, BorderLayout.NORTH);

    // Scrollable content
    JPanel scrollContent = new JPanel(new BorderLayout());
    scrollContent.setBackground(Color.WHITE);

    // Profile Header (appears only in Friends tab)
    JPanel profileHeader = createProfileHeader();
    scrollContent.add(profileHeader, BorderLayout.NORTH);

    // List
    listModel = new DefaultListModel<>();
    mainList = new JList<>(listModel);
    mainList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    mainList.setCellRenderer(new MainListCellRenderer());
    mainList.setFixedCellHeight(-1); // Variable heights
    mainList.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
    mainList.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() == 2) {
          int index = mainList.locationToIndex(e.getPoint());
          util.ClientLogger.ui("Double-click at index: " + index);
          if (index >= 0) {
            String selected = listModel.getElementAt(index);
            util.ClientLogger.ui("Selected item: " + selected);

            if (currentTab == Tab.CHATS) {
              // Open the chat room
              String roomId = selected;
              util.ClientLogger.ui("Opening chat room: " + roomId);

              // Generate a friendly room name
              String roomName = roomId;
              if (!roomId.startsWith("group_") && roomId.contains("_")) {
                // 1:1 chat - show the other user's name
                String[] users = roomId.split("_");
                if (users.length == 2) {
                  String myName = app.getSocketClient().getUsername();
                  roomName = users[0].equals(myName) ? users[1] : users[0];
                }
              }
              app.showGroupChat(roomId, roomName);
            } else if (currentTab == Tab.FRIENDS) {
              // Parse username - extract from "name|status" or "name (Me)|status" format
              String[] parts = selected.split("\\|");
              String nameWithSuffix = parts[0].trim();
              String statusMsg = parts.length > 1 ? parts[1] : "";
              String targetUser = nameWithSuffix.replace(" (Me)", "");
              boolean isSelf = nameWithSuffix.endsWith(" (Me)");
              util.ClientLogger.ui("Target user: " + targetUser + ", is self: " + isSelf);

              // Request profile from server
              final String finalTargetUser = targetUser;
              final String finalStatusMsg = statusMsg;
              final boolean finalIsSelf = isSelf;

              // Check if we have cached profile
              java.util.Map<String, Integer> cachedScores = app.getSocketClient().getCachedProfile(targetUser);

              if (cachedScores != null) {
                // Use cached scores directly
                showProfilePopup(finalTargetUser, finalStatusMsg, cachedScores, finalIsSelf);
              } else {
                // Request in background, then show popup on EDT
                new Thread(() -> {
                  // Request profile (network call - must be off EDT)
                  app.getSocketClient().requestProfile(finalTargetUser);

                  // Wait a bit for response to arrive
                  try {
                    Thread.sleep(200);
                  } catch (Exception ex) {
                  }

                  // Show popup on EDT with whatever we got
                  javax.swing.SwingUtilities.invokeLater(() -> {
                    java.util.Map<String, Integer> scores = app.getSocketClient().getCachedProfile(finalTargetUser);
                    if (scores == null)
                      scores = new java.util.HashMap<>();
                    showProfilePopup(finalTargetUser, finalStatusMsg, scores, finalIsSelf);
                  });
                }).start();
              }
            }

          }
        }
      }
    });

    scrollContent.add(mainList, BorderLayout.CENTER);

    JScrollPane scrollPane = new JScrollPane(scrollContent);
    scrollPane.setBorder(null);
    scrollPane.getVerticalScrollBar().setUnitIncrement(16);
    contentPanel.add(scrollPane, BorderLayout.CENTER);

    add(contentPanel, BorderLayout.CENTER);

    // Initial Load
    switchTab(Tab.FRIENDS);

    // Listen for user updates
    if (app.getSocketClient() != null) {
      app.getSocketClient().setUserListListener(new network.SocketClient.UserListListener() {
        @Override
        public void onUserListUpdated(String[] users) {
          SwingUtilities.invokeLater(() -> {
            cachedUsers = users;
            if (currentTab == Tab.FRIENDS) {
              updateFriendList();
            }
          });
        }

        @Override
        public void onUserJoined(String username) {
          // Trigger full update usually via onUserListUpdated
        }

        @Override
        public void onUserLeft(String username) {
        }

        @Override
        public void onChatListUpdate(String roomId, String lastMessage) {
          SwingUtilities.invokeLater(() -> {
            cachedChats.put(roomId, lastMessage);
            if (currentTab == Tab.CHATS) {
              if (!listModel.contains(roomId)) {
                listModel.add(0, roomId); // Add to top
              } else {
                // Move to top? simplified: just repaint or leave it
                listModel.removeElement(roomId);
                listModel.add(0, roomId);
              }
              mainList.repaint();
            }
          });
        }
      });

      // Listen for unread count updates
      app.getSocketClient().setUnreadListener(new network.SocketClient.UnreadListener() {
        @Override
        public void onUnreadCountUpdated(String roomId, int count) {
          SwingUtilities.invokeLater(() -> {
            if (currentTab == Tab.CHATS) {
              mainList.repaint();
            }
          });
        }

        @Override
        public void onTotalUnreadUpdated(int total) {
          SwingUtilities.invokeLater(() -> {
            totalUnreadCount = total;
            chatsBtn.repaint(); // 채팅 버튼 재렌더링
          });
        }
      });
    }
  }

  private JPanel createProfileHeader() {
    JPanel header = new JPanel(new BorderLayout(12, 0));
    header.setBackground(Color.WHITE);
    header.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(235, 235, 235)),
        BorderFactory.createEmptyBorder(20, 20, 20, 20)));

    // Large profile icon
    JPanel profileIcon = new JPanel() {
      @Override
      protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(KakaoColors.PROFILE_PLACEHOLDER);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 24, 24);

        g2.setColor(Color.WHITE);
        g2.fillOval(20, 14, 32, 32);
        g2.fillArc(14, 40, 44, 32, 0, 180);

        g2.dispose();
      }
    };
    profileIcon.setOpaque(false);
    profileIcon.setPreferredSize(new Dimension(72, 72));

    // Name and status
    JPanel textPanel = new JPanel();
    textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
    textPanel.setOpaque(false);

    JLabel nameLabel = new JLabel(app.getSocketClient() != null ? app.getSocketClient().getUsername() : "User");
    nameLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
    nameLabel.setForeground(KakaoColors.TEXT_PRIMARY);
    nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

    JLabel statusLabel = new JLabel("상태 메시지를 입력하세요");
    statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
    statusLabel.setForeground(KakaoColors.TEXT_SECONDARY);
    statusLabel.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
    statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

    textPanel.add(nameLabel);

    // Status Message Input/Display
    JPanel statusPanel = new JPanel(new BorderLayout());
    statusPanel.setOpaque(false);

    JTextField statusField = new JTextField();
    statusField.setBorder(null);
    statusField.setOpaque(false);
    statusField.setText("상태 메시지를 입력하세요");
    statusField.setForeground(KakaoColors.TEXT_SECONDARY);
    statusField.setFont(new Font("SansSerif", Font.PLAIN, 13));

    // Allow editing on click or focus
    statusField.addActionListener(e -> {
      String newStatus = statusField.getText();
      if (app.getSocketClient() != null) {
        app.getSocketClient().updateStatus(newStatus);
        statusField.setFocusable(false);
        statusField.setFocusable(true); // Reset focus state
        JOptionPane.showMessageDialog(this, "상태 메시지가 변경되었습니다.");
      }
    });

    statusPanel.add(statusField, BorderLayout.CENTER);
    statusPanel.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
    textPanel.add(statusPanel);

    // textPanel.add(statusLabel); // Removed old static label

    header.add(profileIcon, BorderLayout.WEST);
    header.add(textPanel, BorderLayout.CENTER);

    return header;
  }

  private void updateFriendList() {
    SwingUtilities.invokeLater(() -> {
      listModel.clear();
      // Add "Me" first
      String me = app.getSocketClient().getUsername();
      // Find my status from cachedUsers if possible? Or locally store it?
      // Simplified: Just show name for Me, or try to find my entry in list

      String myEntry = me + " (Me)";
      // Check if cachedUsers has me with status
      for (String userStr : cachedUsers) {
        String[] parts = userStr.split("\\|");
        if (parts[0].equals(me)) {
          if (parts.length > 1 && !parts[1].isEmpty()) {
            myEntry = me + " (Me)|" + parts[1];
          }
          break;
        }
      }

      if (me != null) {
        listModel.addElement(myEntry);
      }

      for (String userStr : cachedUsers) {
        String[] parts = userStr.split("\\|");
        String username = parts[0];
        if (me == null || !username.equals(me)) {
          // We keep the full string "name|status" in the model
          listModel.addElement(userStr);
        }
      }
    });
  }

  private void switchTab(Tab tab) {
    currentTab = tab;
    friendsBtn.repaint();
    chatsBtn.repaint();
    moreBtn.repaint();

    listModel.clear();
    if (tab == Tab.FRIENDS) {
      titleLabel.setText("Friends");
      updateFriendList();
    } else if (tab == Tab.CHATS) {
      titleLabel.setText("Chats");
      for (String roomId : cachedChats.keySet()) {
        listModel.addElement(roomId);
      }
    } else {
      titleLabel.setText("More");
      listModel.addElement("Settings");
      listModel.addElement("Profile");
    }
  }

  private class SidebarButton extends JComponent {
    private Tab tab;

    public SidebarButton(Tab tab) {
      this.tab = tab;
      setPreferredSize(new Dimension(66, 50));
      setMaximumSize(new Dimension(66, 50));
      setAlignmentX(Component.CENTER_ALIGNMENT); // Ensure centering in BoxLayout
      setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
      addMouseListener(new MouseAdapter() {
        @Override
        public void mousePressed(MouseEvent e) {
          switchTab(tab);
        }

        @Override
        public void mouseEntered(MouseEvent e) {
          isHovered = true;
          repaint();
        }

        @Override
        public void mouseExited(MouseEvent e) {
          isHovered = false;
          repaint();
        }
      });
    }

    private boolean isHovered = false;

    @Override
    protected void paintComponent(Graphics g) {
      super.paintComponent(g);
      Graphics2D g2 = (Graphics2D) g;
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

      boolean isSelected = (currentTab == tab);
      if (isSelected) {
        g2.setColor(Color.BLACK);
      } else if (isHovered) {
        g2.setColor(Color.GRAY);
      } else {
        g2.setColor(Color.LIGHT_GRAY);
      }

      int cx = getWidth() / 2;
      int cy = getHeight() / 2;

      if (tab == Tab.FRIENDS) {
        // Draw User Icon (Reduced size)
        g2.fillOval(cx - 9, cy - 11, 18, 18); // Head
        g2.fillArc(cx - 12, cy + 3, 24, 16, 0, 180); // Body
      } else if (tab == Tab.CHATS) {
        // Draw Chat Bubble (Reduced size)
        g2.fillRoundRect(cx - 11, cy - 9, 22, 16, 8, 8);
        int[] xPoints = { cx - 4, cx + 4, cx - 7 };
        int[] yPoints = { cy + 7, cy + 7, cy + 12 };
        g2.fillPolygon(xPoints, yPoints, 3);

        // Draw unread badge if there are unread messages
        if (totalUnreadCount > 0) {
          g2.setColor(new Color(255, 70, 70));
          int badgeSize = 14; // Slightly smaller badge
          int badgeX = cx + 8;
          int badgeY = cy - 14;
          g2.fillOval(badgeX, badgeY, badgeSize, badgeSize);

          g2.setColor(Color.WHITE);
          g2.setFont(new Font("SansSerif", Font.BOLD, 9));
          String countStr = totalUnreadCount > 99 ? "99+" : String.valueOf(totalUnreadCount);
          FontMetrics fm = g2.getFontMetrics();
          int textX = badgeX + (badgeSize - fm.stringWidth(countStr)) / 2;
          int textY = badgeY + (badgeSize + fm.getAscent() - fm.getDescent()) / 2;
          g2.drawString(countStr, textX, textY);
        }
      } else {
        // Draw Dots
        g2.fillOval(cx - 12, cy - 2, 4, 4);
        g2.fillOval(cx - 2, cy - 2, 4, 4);
        g2.fillOval(cx + 8, cy - 2, 4, 4);
      }
    }
  }

  private void openUserSelectDialog() {
    if (cachedUsers == null || cachedUsers.length == 0) {
      JOptionPane.showMessageDialog(this, "접속 중인 친구가 없습니다.");
      return;
    }

    String myName = app.getSocketClient().getUsername();
    UserSelectDialog dialog = new UserSelectDialog(app, cachedUsers, myName);
    dialog.setVisible(true);

    if (dialog.isConfirmed()) {
      java.util.List<String> selectedUsers = dialog.getSelectedUsers();

      // Prompt for Room Name
      String defaultName = String.join(", ", selectedUsers);
      if (defaultName.length() > 20)
        defaultName = defaultName.substring(0, 20) + "...";

      String roomName = (String) JOptionPane.showInputDialog(
          this,
          "채팅방 이름을 입력하세요:",
          "채팅방 생성",
          JOptionPane.PLAIN_MESSAGE,
          null,
          null,
          defaultName);

      if (roomName != null && !roomName.trim().isEmpty()) {
        // Create group chat ID
        // Room ID: group_timestamp_creator
        String roomId = "group_" + System.currentTimeMillis() + "_" + myName;

        // 1. Open chat page FIRST (This ensures we join the room and set listeners)
        app.showGroupChat(roomId, roomName);

        // 2. Invite users (in background/separate thread to avoid any blocking on EDT?
        // Writer is usually fast, but safe to do after showing UI)
        new Thread(() -> {
          try {
            // Small delay to ensure join is processed if needed, though usually not
            // strictly necessary
            // if server handles out of order. But here we just want to ensure UI is up.
            Thread.sleep(100);
            for (String user : selectedUsers) {
              app.getSocketClient().inviteUser(roomId, user);
            }
          } catch (Exception ex) {
            ex.printStackTrace();
          }
        }).start();
      }
    }
  }

  private class MainListCellRenderer extends JPanel implements ListCellRenderer<String> {

    public MainListCellRenderer() {
      setOpaque(true);
      setBackground(Color.WHITE);
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends String> list, String value, int index,
        boolean isSelected, boolean cellHasFocus) {

      removeAll();
      setLayout(new BorderLayout(12, 0));
      setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

      // Profile icon
      JPanel iconPanel = new JPanel() {
        @Override
        protected void paintComponent(Graphics g) {
          super.paintComponent(g);
          Graphics2D g2 = (Graphics2D) g.create();
          g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

          // Squircle background
          g2.setColor(KakaoColors.PROFILE_PLACEHOLDER);
          g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);

          // Simple avatar icon
          g2.setColor(Color.WHITE);
          g2.fillOval(13, 8, 24, 24);
          g2.fillArc(9, 28, 32, 24, 0, 180);

          g2.dispose();
        }
      };
      iconPanel.setOpaque(false);
      iconPanel.setPreferredSize(new Dimension(50, 50));

      // Text panel
      JPanel textPanel = new JPanel();
      textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
      textPanel.setOpaque(false);

      JLabel nameLabel = new JLabel(value);

      String[] info = parseUserEntry(value);
      String displayName = info[0];
      String statusMsg = info[1];

      // For Chats tab, convert roomId to friendly name
      if (currentTab == Tab.CHATS) {
        String roomId = value;
        if (!roomId.startsWith("group_") && roomId.contains("_")) {
          // 1:1 chat - show the other user's name
          String[] users = roomId.split("_");
          if (users.length == 2) {
            String myName = app.getSocketClient().getUsername();
            displayName = users[0].equals(myName) ? users[1] : users[0];
          }
        }
      }

      nameLabel.setText(displayName);
      nameLabel.setFont(new Font("SansSerif", Font.PLAIN, 16));
      nameLabel.setForeground(KakaoColors.TEXT_PRIMARY);
      nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

      textPanel.add(nameLabel);

      // Add status message for friends tab
      if (currentTab == Tab.FRIENDS) {
        if (!statusMsg.isEmpty()) {
          textPanel.add(Box.createVerticalStrut(2));
          JLabel subLabel = new JLabel(statusMsg);
          subLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
          subLabel.setForeground(KakaoColors.TEXT_SECONDARY);
          subLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
          textPanel.add(subLabel);
        }
      } else if (currentTab == Tab.CHATS) {
        textPanel.add(Box.createVerticalStrut(2));
        String[] chatInfo = parseUserEntry(value); // This might need adjustment if CHATS tab uses different model
        // If it's a room name, we don't parse it.
        String roomId = value;
        String lastMsg = cachedChats.get(roomId);
        if (lastMsg == null)
          lastMsg = "";

        JLabel subLabel = new JLabel(lastMsg);
        subLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        subLabel.setForeground(KakaoColors.TEXT_SECONDARY);
        subLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        textPanel.add(subLabel);

        // Unread badge for this chat room
        java.util.Map<String, Integer> unreadMap = app.getSocketClient().getCachedUnreadCounts();
        int unreadCount = unreadMap.getOrDefault(roomId, 0);
        if (unreadCount > 0) {
          JLabel badgeLabel = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
              super.paintComponent(g);
              Graphics2D g2 = (Graphics2D) g.create();
              g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

              g2.setColor(new Color(255, 70, 70));
              // Use fixed size for circle
              g2.fillOval(0, 0, 22, 22);

              g2.setColor(Color.WHITE);
              g2.setFont(new Font("SansSerif", Font.BOLD, 10));
              String countStr = unreadCount > 99 ? "99+" : String.valueOf(unreadCount);
              FontMetrics fm = g2.getFontMetrics();
              int textX = (22 - fm.stringWidth(countStr)) / 2;
              int textY = (22 + fm.getAscent() - fm.getDescent()) / 2;
              g2.drawString(countStr, textX, textY);

              g2.dispose();
            }
          };
          badgeLabel.setPreferredSize(new Dimension(22, 22));
          // Wrap in a panel to prevent layout from stretching it
          JPanel badgeContainer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
          badgeContainer.setOpaque(false);
          badgeContainer.add(badgeLabel);
          add(badgeContainer, BorderLayout.EAST);
        }
      } else if (currentTab == Tab.MORE) {
        textPanel.add(Box.createVerticalStrut(2));
        JLabel subLabel = new JLabel("설명");
        subLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        subLabel.setForeground(KakaoColors.TEXT_TERTIARY);
        subLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        textPanel.add(subLabel);
      }

      add(iconPanel, BorderLayout.WEST);
      add(textPanel, BorderLayout.CENTER);

      if (isSelected) {
        setBackground(KakaoColors.SELECTED_BG);
      } else {
        setBackground(Color.WHITE);
      }

      return this;
    }
  }

  // Helper to extract display name and status
  private String[] parseUserEntry(String value) {
    String[] parts = value.split("\\|");
    String name = parts[0];
    String status = (parts.length > 1) ? parts[1] : "";
    return new String[] { name, status };
  }

  private void showProfilePopup(String targetUser, String statusMsg, java.util.Map<String, Integer> scores,
      boolean isSelf) {
    UserProfilePopup popup = new UserProfilePopup(
        (java.awt.Frame) SwingUtilities.getWindowAncestor(MainPage.this),
        targetUser,
        statusMsg,
        scores,
        isSelf,
        username -> {
          util.ClientLogger.ui("Starting chat with: " + username);
          app.showChatWith(username);
        });
    popup.setVisible(true);
  }

}
