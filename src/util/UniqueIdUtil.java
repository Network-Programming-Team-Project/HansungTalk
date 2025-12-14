package util;

public class UniqueIdUtil {
  private static long currentId = 0;

  public static synchronized String generateUniqueId() {
    currentId++;
    return "ID_" + currentId;
  }
}
