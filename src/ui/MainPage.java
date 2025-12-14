package ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class MainPage extends JPanel {
  private ClientApp app;

  private DefaultListModel<String> listModel;

  private enum Tab {
    FRIENDS, CHATS, MORE
  }

  private Tab currentTab = Tab.FRIENDS;
  private JPanel contentPanel;
  private JLabel titleLabel;
  private JList<String> mainList;
  private SidebarButton friendsBtn;
  private SidebarButton chatsBtn;
  private SidebarButton moreBtn;

  private String[] cachedUsers = new String[0];
  private java.util.Map<String, String> cachedChats = new java.util.HashMap<>();

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
    sidebar.add(Box.createVerticalStrut(20));
    sidebar.add(moreBtn);

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

    JLabel searchIcon = new JLabel("🔍");
    searchIcon.setFont(new Font("SansSerif", Font.PLAIN, 18));
    JLabel addIcon = new JLabel("➕");
    addIcon.setFont(new Font("SansSerif", Font.PLAIN, 18));
    addIcon.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    addIcon.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        openUserSelectDialog();
      }
    });

    headerIcons.add(searchIcon);
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
              util.ClientLogger.ui("Target user: " + targetUser + ", is self: " + nameWithSuffix.endsWith(" (Me)"));

              // Don't show profile for self
              if (!nameWithSuffix.endsWith(" (Me)")) {
                util.ClientLogger.ui("Showing profile for: " + targetUser);

                // Request profile from server
                final String finalTargetUser = targetUser;
                final String finalStatusMsg = statusMsg;

                // Check if we have cached profile, otherwise request it
                java.util.Map<String, Integer> cachedScores = app.getSocketClient().getCachedProfile(targetUser);

                if (cachedScores != null) {
                  // Use cached scores
                  showProfilePopup(finalTargetUser, finalStatusMsg, cachedScores);
                } else {
                  // Request from server and show popup with zeros, update when data arrives
                  java.util.Map<String, Integer> emptyScores = new java.util.HashMap<>();

                  // Set up listener for profile response
                  app.getSocketClient().setProfileListener((username, scores) -> {
                    if (username.equals(finalTargetUser)) {
                      util.ClientLogger.ui("Profile data received for " + username + ": " + scores);
                    }
                  });

                  // Request profile
                  app.getSocketClient().requestProfile(targetUser);

                  // Show popup (scores will initially be empty/cached)
                  showProfilePopup(finalTargetUser, finalStatusMsg,
                      app.getSocketClient().getCachedProfile(targetUser) != null
                          ? app.getSocketClient().getCachedProfile(targetUser)
                          : emptyScores);
                }
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
        // Draw User Icon
        g2.fillOval(cx - 10, cy - 12, 20, 20); // Head
        g2.fillArc(cx - 14, cy + 2, 28, 20, 0, 180); // Body
      } else if (tab == Tab.CHATS) {
        // Draw Chat Bubble
        g2.fillRoundRect(cx - 12, cy - 10, 24, 18, 8, 8);
        int[] xPoints = { cx - 5, cx + 5, cx - 8 };
        int[] yPoints = { cy + 8, cy + 8, cy + 14 };
        g2.fillPolygon(xPoints, yPoints, 3);
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
      // Create group chat
      // Room ID: group_timestamp_creator
      String roomId = "group_" + System.currentTimeMillis() + "_" + myName;

      // Invite users
      for (String user : selectedUsers) {
        app.getSocketClient().inviteUser(roomId, user);
      }

      // Open chat page
      // For group chat, we might want to pass the list of names or a group name
      String roomName = String.join(", ", selectedUsers);
      if (roomName.length() > 20)
        roomName = roomName.substring(0, 20) + "...";

      app.showGroupChat(roomId, roomName);
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

  private void showProfilePopup(String targetUser, String statusMsg, java.util.Map<String, Integer> scores) {
    UserProfilePopup popup = new UserProfilePopup(
        (java.awt.Frame) SwingUtilities.getWindowAncestor(MainPage.this),
        targetUser,
        statusMsg,
        scores,
        username -> {
          util.ClientLogger.ui("Starting chat with: " + username);
          app.showChatWith(username);
        });
    popup.setVisible(true);
  }
}
