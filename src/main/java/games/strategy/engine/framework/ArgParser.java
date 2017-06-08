package games.strategy.engine.framework;

import java.util.Arrays;

import games.strategy.triplea.settings.SystemPreferenceKey;
import games.strategy.triplea.settings.SystemPreferences;

public class ArgParser {
  /**
   * Move command line arguments to System.properties
   * @return Return true if all args were valid and accepted, false otherwise.
   */
  public static boolean handleCommandLineArgs(
      final String[] args, final String[] availableProperties) {
    if (args.length == 1 && !args[0].contains("=")) {
      // assume a default single arg, convert the format so we can process as normally.
      args[0] = GameRunner.TRIPLEA_GAME_PROPERTY + "=" + args[0];
    }

    for (final String arg : args) {
      String key;
      final int indexOf = arg.indexOf('=');
      if (indexOf > 0) {
        key = arg.substring(0, indexOf);
      } else {
        throw new IllegalArgumentException("Argument " + arg + " doesn't match pattern 'key=value'");
      }
      if (!setSystemProperty(key, getValue(arg), availableProperties)) {
        System.out.println("Unrecognized: " + arg + ", available: " + Arrays.asList(availableProperties));
        return false;
      }
    }
    return true;
  }

  private static boolean setSystemProperty(String key, String value, String[] availableProperties) {
    for (final String property : availableProperties) {
      if (key.equals(property)) {
        if (property.equals(GameRunner.MAP_FOLDER)) {
          SystemPreferences.put(SystemPreferenceKey.MAP_FOLDER_OVERRIDE, value);
        } else {
          System.getProperties().setProperty(property, value);
        }
        System.out.println(property + ":" + value);
        return true;
      }
    }
    return false;
  }

  private static String getValue(final String arg) {
    final int index = arg.indexOf('=');
    if (index == -1) {
      return "";
    }
    return arg.substring(index + 1);
  }
}
