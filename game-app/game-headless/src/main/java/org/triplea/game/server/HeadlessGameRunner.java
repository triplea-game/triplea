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
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.triplea.config.product.ProductVersionReader;
import org.triplea.domain.data.SystemIdLoader;
import org.triplea.http.client.LobbyHttpClientConfig;
import org.triplea.util.ExitStatus;

/** Runs a headless game server. */
@Slf4j
public final class HeadlessGameRunner {
  private HeadlessGameRunner() {}

  private static final String BOT_COMMENT_ENV = "BOT_COMMENT";
  private static final String BOT_NAME_ENV = "BOT_NAME";
  private static final String BOT_PORT_ENV = "BOT_PORT";
  private static final String BOT_LOBBY_URI_ENV = "BOT_LOBBY_URI";

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

    System.setProperty(GameRunner.TRIPLEA_HEADLESS, "true");
    System.setProperty(TRIPLEA_SERVER, "true");

    if (!setSystemProperty(
        LOBBY_GAME_COMMENTS,
        BOT_COMMENT_ENV, //
        v -> v.length() >= 0 && v.length() <= 14)) {
      log.error(
          "Invalid value for lobby game comments, length must be between 0 and 14 characters");
      ExitStatus.FAILURE.exit();
    }

    if (!setSystemProperty(
        TRIPLEA_NAME,
        BOT_NAME_ENV, //
        v ->
            v.length() >= 7
                && v.length() <= 21
                && v.startsWith(GameRunner.BOT_GAME_HOST_NAME_PREFIX))) {
      log.error(
          "Invalid value for bot name, must start with the word BOT and be length at least 7, under 22, value: {}");
      ExitStatus.FAILURE.exit();
    }

    if (!setSystemProperty(
        TRIPLEA_PORT,
        BOT_PORT_ENV, //
        v -> !isInvalidPortNumber(v))) {
      log.error("Invalid value for bot port, must be a number, value found: {}");
      ExitStatus.FAILURE.exit();
    }

    if (!setSystemProperty(
        LOBBY_URI,
        BOT_LOBBY_URI_ENV, //
        v -> URI.create(v).isAbsolute())) {
      log.error("Invalid lobby URI");
      ExitStatus.FAILURE.exit();
    }

    LobbyHttpClientConfig.setConfig(
        LobbyHttpClientConfig.builder()
            .systemId(SystemIdLoader.load().getValue())
            .clientVersion(
                ProductVersionReader.getCurrentVersion().getMajor()
                    + "."
                    + ProductVersionReader.getCurrentVersion().getMinor())
            .build());

    ClientSetting.initialize();

    Path mapsFolder = Path.of(System.getenv("MAPS_FOLDER"));
    ClientSetting.mapFolderOverride.setValue(Path.of(System.getenv("MAPS_FOLDER")));
    log.info(
        "Using map folder: " + ClientSetting.mapFolderOverride.getValueOrThrow().toAbsolutePath());
    if (!Files.isDirectory(mapsFolder)) {
      throw new RuntimeException(
          "Check env variable: MAPS_FOLDER, value found: "
              + System.getenv("MAPS_FOLDER")
              + ", is not a directory");
    }

    ZippedMapsExtractor.builder()
        .downloadedMapsFolder(ClientSetting.mapFolderOverride.getValueOrThrow())
        .progressIndicator(
            unzipTask -> {
              log.info("Unzipping map files");
              unzipTask.run();
            })
        .build()
        .unzipMapFiles();

    try {
      HeadlessGameServer.runHeadlessGameServer();
    } catch (final Exception e) {
      log.error("Failed to run game server", e);
      ExitStatus.FAILURE.exit();
    }
  }

  private static boolean setSystemProperty(
      String systemPropName, String envVarFallbackName, Predicate<String> validator) {
    String value = System.getProperty(systemPropName, System.getenv(envVarFallbackName));
    if (value == null) {
      log.warn("Set env variable: {}", envVarFallbackName);
      return false;
    }

    if (validator.test(value)) {
      System.setProperty(systemPropName, value);
      return true;
    } else {
      if (value == null) {
        log.warn("Set env variable: {}", envVarFallbackName);
      } else if (System.getProperty(systemPropName) == null) {
        log.warn("Invalid env variable: {}, env variable name: {}", value, envVarFallbackName);
      } else {
        log.warn(
            "Invalid system property: {}, system property name: {}", value, envVarFallbackName);
      }
      return false;
    }
  }

  private static boolean isInvalidPortNumber(final String testValue) {
    try {
      return Integer.parseInt(testValue) <= 0;
    } catch (final NumberFormatException e) {
      return true;
    }
  }
}
