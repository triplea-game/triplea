package games.strategy.engine.framework;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.google.common.base.Joiner;

import games.strategy.triplea.settings.ClientSetting;

/**
 * Command line argument parser for the various TripleA applications.
 */
public final class ArgParser {
  private static final String TRIPLEA_PROTOCOL = "triplea:";
  private static final String TRIPLEA_PROPERTY_PREFIX = "P";
  private final Set<String> availableProperties;

  public ArgParser(final Set<String> availableProperties) {
    this.availableProperties = availableProperties;
  }

  /**
   * Move command line arguments to system properties or client settings.
   *
   * @return Return true if all args were valid and accepted, false otherwise.
   */
  public boolean handleCommandLineArgs(final String[] args) {
    resetTransientClientSettings();
    final Options options = getOptions();
    final CommandLineParser parser = new DefaultParser();
    try {
      final CommandLine cli = parser.parse(options, args);
      if (!cli.getOptionProperties(TRIPLEA_PROPERTY_PREFIX).entrySet().stream().allMatch(
          entry -> setSystemPropertyOrClientSetting((String) entry.getKey(), (String) entry.getValue()))) {
        return false;
      }

      // Parse remaining options
      parseRemaining(cli.getArgs());

    } catch (final ParseException e) {
      throw new IllegalArgumentException(e);
    }
    ClientSetting.flush();
    return true;
  }

  private void parseRemaining(final String[] remainingArgs) {
    if (remainingArgs.length == 1) {
      if (remainingArgs[0].startsWith(TRIPLEA_PROTOCOL)) {
        final String encoding = StandardCharsets.UTF_8.displayName();
        try {
          setSystemPropertyOrClientSetting(CliProperties.TRIPLEA_MAP_DOWNLOAD,
              URLDecoder.decode(remainingArgs[0].substring(TRIPLEA_PROTOCOL.length()), encoding));
        } catch (final UnsupportedEncodingException e) {
          throw new AssertionError(encoding + " is not a supported encoding!", e);
        }
      } else {
        setSystemPropertyOrClientSetting(CliProperties.TRIPLEA_GAME, remainingArgs[0]);
      }
    } else if (remainingArgs.length > 1) {
      throw new IllegalArgumentException("Too much arguments: " + Arrays.toString(remainingArgs));
    }
  }

  private Options getOptions() {
    final Options options = new Options();
    options.addOption(Option.builder(TRIPLEA_PROPERTY_PREFIX)
        .argName("key=value")
        .hasArgs()
        .numberOfArgs(2)
        .valueSeparator()
        .desc("Assign the given value to the given property key.\nValid keys are : "
            + Joiner.on(",\n").join(availableProperties))
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

  private boolean setSystemPropertyOrClientSetting(
      final String key,
      final String value) {
    if (!availableProperties.contains(key)) {
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
    if (CliProperties.MAP_FOLDER.equals(key)) {
      ClientSetting.MAP_FOLDER_OVERRIDE.save(value);
      return true;
    }
    return false;
  }


  /**
   * A collection of all CLI related constants.
   */
  public static final class CliProperties {
    private CliProperties() {}

    // argument options below:
    public static final String GAME_HOST_CONSOLE = "triplea.game.host.console";
    public static final String TRIPLEA_GAME = "triplea.game";
    public static final String TRIPLEA_MAP_DOWNLOAD = "triplea.map.download";
    public static final String TRIPLEA_SERVER = "triplea.server";
    public static final String TRIPLEA_CLIENT = "triplea.client";
    public static final String TRIPLEA_HOST = "triplea.host";
    public static final String TRIPLEA_PORT = "triplea.port";
    public static final String TRIPLEA_NAME = "triplea.name";
    public static final String SERVER_PASSWORD = "triplea.server.password";
    public static final String TRIPLEA_STARTED = "triplea.started";
    public static final String LOBBY_HOST = "triplea.lobby.host";
    public static final String LOBBY_PORT = "triplea.lobby.port";
    public static final String LOBBY_GAME_COMMENTS = "triplea.lobby.game.comments";
    public static final String LOBBY_GAME_HOSTED_BY = "triplea.lobby.game.hostedBy";
    public static final String LOBBY_GAME_SUPPORT_EMAIL = "triplea.lobby.game.supportEmail";
    public static final String LOBBY_GAME_SUPPORT_PASSWORD = "triplea.lobby.game.supportPassword";
    public static final String LOBBY_GAME_RECONNECTION = "triplea.lobby.game.reconnection";
    public static final String DO_NOT_CHECK_FOR_UPDATES = "triplea.doNotCheckForUpdates";

    public static final String TRIPLEA_SERVER_START_GAME_SYNC_WAIT_TIME = "triplea.server.startGameSyncWaitTime";
    public static final String TRIPLEA_SERVER_OBSERVER_JOIN_WAIT_TIME = "triplea.server.observerJoinWaitTime";

    public static final String MAP_FOLDER = "mapFolder";
  }
}
