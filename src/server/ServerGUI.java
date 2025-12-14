package server;

import network.SocketServer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class ServerGUI extends JFrame implements SocketServer.ServerLogListener {
  private SocketServer server;
  private JTextArea logArea;
  private JButton toggleButton;
  private JLabel statusLabel;
  private JLabel clientCountLabel;
  private boolean isRunning = false;

  public ServerGUI() {
    setTitle("KakaoTalk Server");
    setSize(600, 400);
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setLocationRelativeTo(null);

    // Layout
    setLayout(new BorderLayout());

    // Header
    JPanel headerPanel = new JPanel(new BorderLayout());
    headerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

    statusLabel = new JLabel("Status: Stopped");
    statusLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
    statusLabel.setForeground(Color.RED);

    clientCountLabel = new JLabel("Clients: 0");

    headerPanel.add(statusLabel, BorderLayout.WEST);
    headerPanel.add(clientCountLabel, BorderLayout.EAST);
    add(headerPanel, BorderLayout.NORTH);

    // Log Area
    logArea = new JTextArea();
    logArea.setEditable(false);
    logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
    add(new JScrollPane(logArea), BorderLayout.CENTER);

    // Bottom Panel
    JPanel bottomPanel = new JPanel();
    toggleButton = new JButton("Start Server");
    toggleButton.addActionListener(this::toggleServer);
    bottomPanel.add(toggleButton);
    add(bottomPanel, BorderLayout.SOUTH);

    // Initialize Server
    server = new SocketServer(12345);
    server.setLogListener(this);
  }

  private void toggleServer(ActionEvent e) {
    if (!isRunning) {
      try {
        server.start();
        isRunning = true;
        toggleButton.setText("Stop Server");
        statusLabel.setText("Status: Running");
        statusLabel.setForeground(new Color(0, 128, 0));
      } catch (Exception ex) {
        logArea.append("Error starting server: " + ex.getMessage() + "\n");
        ex.printStackTrace();
      }
    } else {
      try {
        server.stop();
        isRunning = false;
        toggleButton.setText("Start Server");
        statusLabel.setText("Status: Stopped");
        statusLabel.setForeground(Color.RED);
      } catch (Exception ex) {
        logArea.append("Error stopping server: " + ex.getMessage() + "\n");
        ex.printStackTrace();
      }
    }
  }

  @Override
  public void onLog(String message) {
    SwingUtilities.invokeLater(() -> {
      logArea.append(message + "\n");
      logArea.setCaretPosition(logArea.getDocument().getLength());
    });
  }

  @Override
  public void onClientCountUpdated(int count) {
    SwingUtilities.invokeLater(() -> {
      clientCountLabel.setText("Clients: " + count);
    });
  }
}
