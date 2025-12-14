package ui;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 대화 상대 선택 다이얼로그 클래스
 * 그룹 채팅 생성 시 대화 상대를 선택하는 UI 제공
 */
public class UserSelectDialog extends JDialog {
  private JList<String> userList; // 사용자 목록 UI
  private DefaultListModel<String> listModel; // 목록 데이터 모델
  private List<String> selectedUsers = new ArrayList<>(); // 선택된 사용자 목록
  private boolean confirmed = false; // 확인 버튼 클릭 여부

  /** 생성자: 다이얼로그 초기화 */
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
