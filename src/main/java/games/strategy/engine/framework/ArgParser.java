package games.strategy.engine.framework;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import games.strategy.triplea.settings.ClientSetting;

/**
 * Command line argument parser for the various TripleA applications.
 */
public final class ArgParser {
  private static final String TRIPLEA_PROTOCOL = "triplea:";

  private ArgParser() {}

  /**
   * Move command line arguments to system properties or client settings.
   *
   * @return Return true if all args were valid and accepted, false otherwise.
   */
  public static boolean handleCommandLineArgs(final String[] args, final String[] availableProperties) {
    if (args.length == 1 && !args[0].contains("=") && !isSwitch(args[0])) {
      // assume a default single arg, convert the format so we can process as normally.
      if (args[0].startsWith(TRIPLEA_PROTOCOL)) {
        final String encoding = StandardCharsets.UTF_8.displayName();
        try {
          args[0] = GameRunner.TRIPLEA_MAP_DOWNLOAD_PROPERTY + "="
              + URLDecoder.decode(args[0].substring(TRIPLEA_PROTOCOL.length()), encoding);
        } catch (final UnsupportedEncodingException e) {
          throw new AssertionError(encoding + " is not a supported encoding!", e);
        }
      } else {
        args[0] = GameRunner.TRIPLEA_GAME_PROPERTY + "=" + args[0];
      }
    }

    resetTransientClientSettings();

    for (final String arg : args) {
      // ignore command-line switches forwarded by launchers
      if (isSwitch(arg)) {
        continue;
      }

      final String key;
      final int indexOf = arg.indexOf('=');
      if (indexOf > 0) {
        key = arg.substring(0, indexOf);
      } else {
        throw new IllegalArgumentException("Argument " + arg + " doesn't match pattern 'key=value'");
      }

      if (!setSystemPropertyOrClientSetting(key, getValue(arg), availableProperties)) {
        System.out.println("Unrecognized: " + arg + ", available: " + Arrays.asList(availableProperties));
        return false;
      }
    }

    ClientSetting.flush();

    return true;
  }

  private static boolean isSwitch(final String arg) {
    return arg.startsWith("-");
  }

  /**
   * Resets any client settings that may be set via the command line but whose value should not persist between runs.
   *
   * <p>
   * For example, if the user specified a setting via the command line during the previous run, the value of that
   * setting will be persisted. If the user does not specify the setting via the command line during the next run, the
   * previous value will still be available and used during the current run even though it should technically be
   * reported as "not set."
   * </p>
   */
  private static void resetTransientClientSettings() {
    Arrays.asList(ClientSetting.MAP_FOLDER_OVERRIDE).stream()
        .forEach(setting -> setting.save(setting.defaultValue));
  }

  private static boolean setSystemPropertyOrClientSetting(
      final String key,
      final String value,
      final String[] availableProperties) {
    if (!Arrays.stream(availableProperties).anyMatch(key::equals)) {
      return false;
    }

    if (!handleGameSetting(key, value)) {
      System.setProperty(key, value);
    }

    System.out.println(key + ":" + value);
    return true;
  }

  private static boolean handleGameSetting(final String key, final String value) {
    if (GameRunner.MAP_FOLDER.equals(key)) {
      ClientSetting.MAP_FOLDER_OVERRIDE.save(value);
      return true;
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
