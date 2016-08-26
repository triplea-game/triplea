package games.strategy.engine.framework.system;



/**
 * Wrapper class around System.getProperties(), use this class to set/get System properties.
 * Prefer to use system props only for command line usage. TripleA code base has made pretty extensive use
 * of System props to pass values, which is not a best practice. Converting those usages to this wrapper interface
 * will make different parts of the code that use systemn properties much easier to manage.
 */
// final + private constructor to disallow inheritance, all access to the system is static
public final class SystemProperties {

  private enum SystemPropertyKey {
  }

  private SystemProperties() {

  }

  public static boolean isWindows() {
    return System.getProperties().getProperty("os.name").toLowerCase().contains("windows");
  }

  public static boolean isMac() {
    return System.getProperties().getProperty("os.name").toLowerCase().contains("mac");
  }


}
