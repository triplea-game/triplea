package org.triplea.game.server;

import static games.strategy.engine.framework.CliProperties.LOBBY_GAME_COMMENTS;
import static games.strategy.engine.framework.CliProperties.LOBBY_URI;
import static games.strategy.engine.framework.CliProperties.TRIPLEA_GAME;
import static games.strategy.engine.framework.CliProperties.TRIPLEA_NAME;
import static games.strategy.engine.framework.CliProperties.TRIPLEA_PORT;
import static games.strategy.engine.framework.CliProperties.TRIPLEA_SERVER;

import games.strategy.engine.ClientFileSystemHelper;
import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.framework.I18nResourceBundle;
import games.strategy.engine.framework.map.file.system.loader.ZippedMapsExtractor;
import games.strategy.triplea.settings.ClientSetting;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.triplea.config.product.ProductVersionReader;
import org.triplea.domain.data.SystemIdLoader;
import org.triplea.http.client.LobbyHttpClientConfig;
import org.triplea.util.ExitStatus;

/** Runs a headless game server. */
@Slf4j
public final class HeadlessGameRunner {
  private HeadlessGameRunner() {}

  /**
   * Starts a new headless game server. This method will return before the headless game server
   * exits. The headless game server runs until the process is killed or the headless game server is
   * shut down via administrative command.
   */
  public static void main(final String[] args) {
    final Locale defaultLocale = Locale.getDefault();
    if (!I18nResourceBundle.getMapSupportedLocales().contains(defaultLocale)) {
      Locale.setDefault(Locale.US);
    }
    ClientSetting.initialize();
    System.setProperty(LOBBY_GAME_COMMENTS, GameRunner.BOT_GAME_HOST_COMMENT);
    System.setProperty(GameRunner.TRIPLEA_HEADLESS, "true");
    System.setProperty(TRIPLEA_SERVER, "true");

    LobbyHttpClientConfig.setConfig(
        LobbyHttpClientConfig.builder()
            .systemId(SystemIdLoader.load().getValue())
            .clientVersion(
                ProductVersionReader.getCurrentVersion().getMajor()
                    + "."
                    + ProductVersionReader.getCurrentVersion().getMinor())
            .build());

    Path mapsFolder = Path.of(System.getenv("MAPS_FOLDER"));
    if (!Files.isDirectory(mapsFolder)) {
      throw new RuntimeException(
          "Check env variable: MAPS_FOLDER, value found: "
              + System.getenv("MAPS_FOLDER")
              + ", is not a directory");
    }
    ClientSetting.mapFolderOverride.setValue(Path.of(System.getenv("MAPS_FOLDER")));

    handleHeadlessGameServerArgs();
    ZippedMapsExtractor.builder()
        .downloadedMapsFolder(ClientSetting.mapFolderOverride.getValueOrThrow())
        .progressIndicator(
            unzipTask -> {
              log.info("Unzipping map files");
              unzipTask.run();
            })
        .build()
        .unzipMapFiles();

    log.info(
        "Using map folder: " + ClientSetting.mapFolderOverride.getValueOrThrow().toAbsolutePath());
    try {
      HeadlessGameServer.runHeadlessGameServer();
    } catch (final Exception e) {
      log.error("Failed to run game server", e);
      ExitStatus.FAILURE.exit();
    }
  }

  private static void handleHeadlessGameServerArgs() {
    boolean printUsage = false;

    if (!ClientSetting.mapFolderOverride.isSet()) {
      ClientSetting.mapFolderOverride.setValue(ClientFileSystemHelper.getUserMapsFolder());
    }

    final String playerName = System.getProperty(TRIPLEA_NAME, "");
    if ((playerName.length() < 7) || !playerName.startsWith(GameRunner.BOT_GAME_HOST_NAME_PREFIX)) {
      log.warn(
          "Invalid or missing argument: "
              + TRIPLEA_NAME
              + " must at least 7 characters long "
              + "and start with "
              + GameRunner.BOT_GAME_HOST_NAME_PREFIX);
      printUsage = true;
    }

    if (isInvalidPortNumber(System.getProperty(TRIPLEA_PORT, "0"))) {
      log.warn("Invalid or missing argument: " + TRIPLEA_PORT + " must be greater than zero");
      printUsage = true;
    }

    if (System.getProperty(LOBBY_URI, "").isEmpty()) {
      log.warn("Invalid or missing argument: " + LOBBY_URI + " must be set");
      printUsage = true;
    }

    if (printUsage) {
      usage();
      ExitStatus.FAILURE.exit();
    }
  }

  private static boolean isInvalidPortNumber(final String testValue) {
    try {
      return Integer.parseInt(testValue) <= 0;
    } catch (final NumberFormatException e) {
      return true;
    }
  }

  private static void usage() {
    // TODO replace this method with the generated usage of commons-cli
    log.info(
        "\nUsage and Valid Arguments:\n"
            + "   "
            + TRIPLEA_GAME
            + "=<FILE_NAME>\n"
            + "   "
            + TRIPLEA_PORT
            + "=<PORT>\n"
            + "   "
            + TRIPLEA_NAME
            + "=<PLAYER_NAME>\n"
            + "   "
            + LOBBY_URI
            + "=<LOBBY_URI>\n"
            + "\n");
  }
}
