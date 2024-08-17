package games.strategy.engine.framework.system;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Locale;
import java.util.Properties;

/**
 * Wrapper class around System.getProperties(), use this class to set/get System properties. Prefer
 * to use system props only for command line usage. TripleA code base has made pretty extensive use
 * of System props to pass values, which is not a best practice. Converting those usages to this
 * wrapper interface will make different parts of the code that use system properties much easier to
 * manage.
 */
public final class SystemProperties {
  private SystemProperties() {}

  public static Properties all() {
    return System.getProperties();
  }

  public static String getJavaClassPath() {
    return checkNotNull(System.getProperty("java.class.path"));
  }

  public static String getJavaHome() {
    return checkNotNull(System.getProperty("java.home"));
  }

  /** Returns current java version with build number. EG: {@code "1.8.0_181-b13"} */
  public static String getJavaVersion() {
    return checkNotNull(System.getProperty("java.version"));
  }

  public static String getOperatingSystem() {
    return checkNotNull(System.getProperty("os.name"));
  }

  public static String getUserDir() {
    return checkNotNull(System.getProperty("user.dir"));
  }

  public static String getUserHome() {
    return checkNotNull(System.getProperty("user.home"));
  }

  public static String getUserName() {
    return checkNotNull(System.getProperty("user.name"));
  }

  public static boolean isMac() {
    return getOperatingSystem().toLowerCase(Locale.ROOT).contains("mac");
  }

  public static boolean isWindows() {
    return getOperatingSystem().toLowerCase(Locale.ROOT).contains("windows");
  }

  public static void setJavaNetUseSystemProxies(final String value) {
    System.setProperty("java.net.useSystemProxies", checkNotNull(value));
  }
}
