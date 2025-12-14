package ui;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class UserSelectDialog extends JDialog {
  private JList<String> userList;
  private DefaultListModel<String> listModel;
  private List<String> selectedUsers = new ArrayList<>();
  private boolean confirmed = false;

  public UserSelectDialog(Frame owner, String[] allUsers, String myName) {
    super(owner, "대화상대 선택", true);
    setLayout(new BorderLayout());
    setSize(300, 400);
    setLocationRelativeTo(owner);

    // Header
    JLabel titleLabel = new JLabel("대화상대 선택");
    titleLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
    titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    add(titleLabel, BorderLayout.NORTH);

    // List
    listModel = new DefaultListModel<>();
    for (String user : allUsers) {
      if (!user.equals(myName)) {
        listModel.addElement(user);
      }
    }

    userList = new JList<>(listModel);
    userList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    userList.setCellRenderer(new DefaultListCellRenderer() {
      @Override
      public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
          boolean cellHasFocus) {
        JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        label.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        return label;
      }
    });

    add(new JScrollPane(userList), BorderLayout.CENTER);

    // Buttons
    JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    JButton okButton = new JButton("확인");
    JButton cancelButton = new JButton("취소");

    okButton.addActionListener(e -> {
      selectedUsers = userList.getSelectedValuesList();
      if (!selectedUsers.isEmpty()) {
        confirmed = true;
        dispose();
      } else {
        JOptionPane.showMessageDialog(this, "대화상대를 선택해주세요.");
      }
    });

    cancelButton.addActionListener(e -> dispose());

    buttonPanel.add(cancelButton);
    buttonPanel.add(okButton);
    add(buttonPanel, BorderLayout.SOUTH);
  }

  public boolean isConfirmed() {
    return confirmed;
  }

  public List<String> getSelectedUsers() {
    return selectedUsers;
  }
}
