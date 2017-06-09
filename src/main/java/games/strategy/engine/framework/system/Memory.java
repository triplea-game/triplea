package games.strategy.engine.framework.system;

import java.util.Properties;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.framework.GameRunner;

public class Memory {
  public static final String TRIPLEA_MEMORY_SET = "triplea.memory.set";
  private static final String TRIPLEA_MEMORY_XMX = "triplea.memory.Xmx";
  private static final String TRIPLEA_MEMORY_USE_DEFAULT = "triplea.memory.useDefault";



  public static void checkForMemoryXMX() {
    final String memSetString = System.getProperty(TRIPLEA_MEMORY_SET, "false");
    final boolean memSet = Boolean.parseBoolean(memSetString);
    // if we have already set the memory, then return.
    // (example: we used process runner to create a new triplea with a specific memory)
    if (memSet) {
      return;
    }
    final Properties systemIni = GameRunner.getSystemIni();
    if (useDefaultMaxMemory(systemIni)) {
      return;
    }
    long xmx = getMaxMemoryFromSystemIniFileInMB(systemIni);
    // if xmx less than zero, return (because it means we do not want to change it)
    if (xmx <= 0) {
      return;
    }
    final int mb = 1024 * 1024;
    xmx = xmx * mb;
    final long currentMaxMemory = Runtime.getRuntime().maxMemory();
    System.out.println("Current max memory: " + currentMaxMemory + ";  and new xmx should be: " + xmx);
    final long diff = Math.abs(currentMaxMemory - xmx);
    // Runtime.maxMemory is never accurate, and is usually off by 5% to 15%,
    // so if our difference is less than 22% we should just ignore the difference
    if (diff <= xmx * 0.22) {
      return;
    }
    // the difference is significant enough that we should re-run triplea with a larger number
    GameRunner.startNewTripleA(xmx);
    // must exit now
    System.exit(0);
  }

  public static boolean useDefaultMaxMemory(final Properties systemIni) {
    final String useDefaultMaxMemoryString = systemIni.getProperty(TRIPLEA_MEMORY_USE_DEFAULT, "true");
    return Boolean.parseBoolean(useDefaultMaxMemoryString);
  }

  public static long getMaxMemoryInBytes() {
    final Properties systemIni = GameRunner.getSystemIni();
    final String useDefaultMaxMemoryString = systemIni.getProperty(TRIPLEA_MEMORY_USE_DEFAULT, "true");
    final boolean useDefaultMaxMemory = Boolean.parseBoolean(useDefaultMaxMemoryString);
    final String maxMemoryString = systemIni.getProperty(TRIPLEA_MEMORY_XMX, "").trim();
    // for whatever reason, .maxMemory() returns a value about 12% smaller than the real Xmx value.
    // Just something to be aware of.
    long max = Runtime.getRuntime().maxMemory();
    if (!useDefaultMaxMemory && maxMemoryString.length() > 0) {
      try {
        final int maxMemorySet = Integer.parseInt(maxMemoryString);
        // it is in MB
        max = 1024 * 1024 * ((long) maxMemorySet);
      } catch (final NumberFormatException e) {
        ClientLogger.logQuietly(e);
      }
    }
    return max;
  }

  public static int getMaxMemoryFromSystemIniFileInMB(final Properties systemIni) {
    final String maxMemoryString = systemIni.getProperty(TRIPLEA_MEMORY_XMX, "").trim();
    int maxMemorySet = -1;
    if (!maxMemoryString.isEmpty()) {
      try {
        maxMemorySet = Integer.parseInt(maxMemoryString);
      } catch (final NumberFormatException e) {
        ClientLogger.logQuietly(e);
      }
    }
    return maxMemorySet;
  }

  public static Properties setMaxMemoryInMB(final int maxMemoryInMb) {
    ClientLogger.logQuietly("Setting max memory for TripleA to: " + maxMemoryInMb + "m");
    final Properties prop = new Properties();
    prop.put(TRIPLEA_MEMORY_USE_DEFAULT, "false");
    prop.put(TRIPLEA_MEMORY_XMX, "" + maxMemoryInMb);
    return prop;
  }

  public static void clearMaxMemory() {
    final Properties prop = new Properties();
    prop.put(TRIPLEA_MEMORY_USE_DEFAULT, "true");
    prop.put(TRIPLEA_MEMORY_XMX, "");
    GameRunner.writeSystemIni(prop);
  }

}
