package me.villagers654;

public class OSChecker {
  public enum OS {
    WINDOWS,
    LINUX,
    MAC,
    UNKNOWN
  }

  public static OS getOperatingSystem() {
    String osName = System.getProperty("os.name").toLowerCase();

    if (osName.contains("win")) {
      return OS.WINDOWS;
    } else if (osName.contains("nix") || osName.contains("nux") || osName.contains("aix")) {
      return OS.LINUX;
    } else if (osName.contains("mac")) {
      return OS.MAC;
    } else {
      return OS.UNKNOWN;
    }
  }
}
