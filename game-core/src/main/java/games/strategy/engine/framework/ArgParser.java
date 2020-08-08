package games.strategy.engine.framework;

import games.strategy.triplea.settings.ClientSetting;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

/** Command line argument parser, parses args formatted as: "-Pkey=value". */
public final class ArgParser {
  public static final String TRIPLEA_PROTOCOL = "triplea:";

  private ArgParser() {}

  /** Move command line arguments to system properties or client settings. */
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
    setSystemPropertyOrClientSetting(
        CliProperties.TRIPLEA_MAP_DOWNLOAD,
        URLDecoder.decode(arg.substring(TRIPLEA_PROTOCOL.length()), StandardCharsets.UTF_8));
  }

  private static void setSystemPropertyOrClientSetting(final String key, final String value) {
    if (CliProperties.MAP_FOLDER.equals(key)) {
      ClientSetting.mapFolderOverride.setValueAndFlush(Paths.get(value));
    } else {
      System.setProperty(key, value);
    }
  }
}
