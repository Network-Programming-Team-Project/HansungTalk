package util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Simple logging utility for debugging client operations.
 */
public class ClientLogger {
  private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
  private static boolean enabled = true;

  public static void setEnabled(boolean enabled) {
    ClientLogger.enabled = enabled;
  }

  public static void log(String tag, String message) {
    if (!enabled)
      return;
    String time = LocalDateTime.now().format(formatter);
    System.out.println("[" + time + "] [" + tag + "] " + message);
  }

  public static void network(String message) {
    log("NETWORK", message);
  }

  public static void ui(String message) {
    log("UI", message);
  }

  public static void page(String message) {
    log("PAGE", message);
  }

  public static void error(String message) {
    log("ERROR", message);
  }

  public static void error(String message, Throwable t) {
    log("ERROR", message + " - " + t.getMessage());
    t.printStackTrace();
  }
}
