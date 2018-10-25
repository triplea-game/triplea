package games.strategy.engine.framework;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import games.strategy.triplea.settings.ClientSetting;

/**
 * Command line argument parser, parses args formatted as: "-Pkey=value".
 */
public final class ArgParser {
  static final String TRIPLEA_PROTOCOL = "triplea:";

  private ArgParser() {}

  /**
   * Move command line arguments to system properties or client settings.
   */
  public static void handleCommandLineArgs(final String... args) {
    ClientSetting.mapFolderOverride.resetValue();

    if ((args.length == 1) && args[0].startsWith(TRIPLEA_PROTOCOL)) {
      handleMapDownloadArg(args[0]);
    } else if ((args.length == 1) && !args[0].contains("=")) {
      System.setProperty(CliProperties.TRIPLEA_GAME, args[0]);
    } else {
      ArgParsingHelper.getTripleaProperties(args)
          .forEach((key, value) -> setSystemPropertyOrClientSetting((String) key, (String) value));
    }
  }

  private static void handleMapDownloadArg(final String arg) {
    final String encoding = StandardCharsets.UTF_8.displayName();
    try {
      setSystemPropertyOrClientSetting(CliProperties.TRIPLEA_MAP_DOWNLOAD,
          URLDecoder.decode(arg.substring(TRIPLEA_PROTOCOL.length()), encoding));
    } catch (final UnsupportedEncodingException e) {
      throw new AssertionError(encoding + " is not a supported encoding!", e);
    }
  }

  private static void setSystemPropertyOrClientSetting(final String key, final String value) {
    if (CliProperties.MAP_FOLDER.equals(key)) {
      ClientSetting.mapFolderOverride.saveAndFlush(new File(value));
    } else {
      System.setProperty(key, value);
    }
  }
}
