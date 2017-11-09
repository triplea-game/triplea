package games.strategy.engine.framework;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import games.strategy.debug.ClientLogger;
import games.strategy.triplea.settings.ClientSetting;

/**
 * Command line argument parser for the various TripleA applications.
 */
public final class ArgParser {
  private static final String TRIPLEA_PROTOCOL = "triplea:";
  private static final String TRIPLEA_PROPERTY_PREFIX = "P";

  private ArgParser() {}

  /**
   * Move command line arguments to system properties or client settings.
   *
   * @return Return true if all args were valid and accepted, false otherwise.
   */
  public static boolean handleCommandLineArgs(final String[] args, final String[] availableProperties) {
    resetTransientClientSettings();
    final Options options = getOptions();
    final CommandLineParser parser = new DefaultParser();
    try {
      final CommandLine cli = parser.parse(options, args);
      if (!cli.getOptionProperties(TRIPLEA_PROPERTY_PREFIX).entrySet().stream().allMatch(
          entry -> setSystemPropertyOrClientSetting((String) entry.getKey(), (String) entry.getValue(),
              availableProperties))) {
      }

      // Parse remaining options
      parseRemaining(cli.getArgs(), availableProperties);

    } catch (final ParseException e) {
      ClientLogger.logError(e);
    }
    ClientSetting.flush();
    return true;
  }

  private static void parseRemaining(final String[] remainingArgs, final String[] availableProperties) {
    if (remainingArgs.length >= 1) {
      if (remainingArgs[0].startsWith(TRIPLEA_PROTOCOL)) {
        final String encoding = StandardCharsets.UTF_8.displayName();
        try {
          setSystemPropertyOrClientSetting(GameRunner.TRIPLEA_MAP_DOWNLOAD_PROPERTY,
              URLDecoder.decode(remainingArgs[0].substring(TRIPLEA_PROTOCOL.length()), encoding), availableProperties);
        } catch (final UnsupportedEncodingException e) {
          throw new AssertionError(encoding + " is not a supported encoding!", e);
        }
      } else {
        setSystemPropertyOrClientSetting(GameRunner.TRIPLEA_GAME_PROPERTY, remainingArgs[0], availableProperties);
      }
    }
  }

  private static Options getOptions() {
    final Options options = new Options();
    options.addOption(Option.builder(TRIPLEA_PROPERTY_PREFIX)
        .argName("property=value")
        .hasArgs()
        .numberOfArgs(2)
        .valueSeparator()
        .desc("Assign the given value to the given property key.")
        .build());
    // See https://github.com/triplea-game/triplea/pull/2574 for more information
    options.addOption(Option.builder("console").build());
    return options;
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
      System.out.println(String.format("Key %s is unknown", key));
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
}
