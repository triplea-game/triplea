package games.strategy.engine.lobby.client.login;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.logging.Level;

import javax.annotation.Nullable;

import com.google.common.annotations.VisibleForTesting;

import games.strategy.engine.ClientContext;
import games.strategy.engine.framework.map.download.DownloadConfiguration;
import games.strategy.triplea.UrlConstants;
import games.strategy.triplea.settings.ClientSetting;
import games.strategy.triplea.settings.GameSetting;
import games.strategy.util.Version;
import lombok.extern.java.Log;

/**
 * Fetches the lobby server properties from the remote Source of Truth.
 */
@Log
public final class LobbyServerPropertiesFetcher {
  private final Function<String, Optional<File>> fileDownloader;
  @Nullable private LobbyServerProperties lobbyServerProperties;


  /**
   * Default constructor with default (prod) dependencies.
   * This allows us to fetch a remote file and parse it for lobby properties.
   * Those properties then tell the game client how/where to connect to the lobby.
   */
  public LobbyServerPropertiesFetcher() {
    this(url -> DownloadConfiguration.contentReader().downloadToFile(url));
  }

  @VisibleForTesting
  LobbyServerPropertiesFetcher(final Function<String, Optional<File>> fileDownloader) {
    this.fileDownloader = fileDownloader;
  }

  /**
   * Fetches LobbyServerProperties based on values read from configuration.
   * LobbyServerProperties are based on a properties file hosted on github which can be updated live.
   * This properties file tells the game client how to connect to the lobby and provides a welcome message.
   * In case Github is not available, we also have a backup hardcoded lobby host address in the client
   * configuration that we would pass back in case github is not available.
   *
   * <p>
   * The lobby server properties may be overridden by setting values for {@link ClientSetting#testLobbyHost} and
   * {@link ClientSetting#testLobbyPort} simultaneously. Setting only one or the other will cause them to be ignored.
   * </p>
   *
   * @return LobbyServerProperties as fetched and parsed from github hosted remote URL.
   *         Otherwise backup values from client config.
   */
  public Optional<LobbyServerProperties> fetchLobbyServerProperties() {
    if (lobbyServerProperties == null) {
      final Optional<LobbyServerProperties> props = fetchProperties();
      props.ifPresent(lobbyProps -> lobbyServerProperties = lobbyProps);
    }
    return Optional.ofNullable(lobbyServerProperties);
  }

  private Optional<LobbyServerProperties> fetchProperties() {
    final Optional<LobbyServerProperties> userOverride = getTestOverrideProperties();
    if (userOverride.isPresent()) {
      return userOverride;
    }

    final Optional<LobbyServerProperties> fromHostedFile = getRemoteProperties();
    if (fromHostedFile.isPresent()) {
      return fromHostedFile;
    }

    return Optional.of(LobbyServerProperties.builder()
        .host(ClientSetting.lobbyLastUsedHost.getValueOrThrow())
        .port(ClientSetting.lobbyLastUsedPort.getValueOrThrow())
        .build());
  }

  private static Optional<LobbyServerProperties> getTestOverrideProperties() {
    return getTestOverrideProperties(ClientSetting.testLobbyHost, ClientSetting.testLobbyPort);
  }

  @VisibleForTesting
  static Optional<LobbyServerProperties> getTestOverrideProperties(
      final GameSetting<String> testLobbyHostSetting,
      final GameSetting<Integer> testLobbyPortSetting) {
    if (testLobbyHostSetting.isSet() && testLobbyPortSetting.isSet()) {
      return Optional.of(
          LobbyServerProperties.builder()
              .host(testLobbyHostSetting.getValueOrThrow())
              .port(testLobbyPortSetting.getValueOrThrow())
              .build());
    }

    return Optional.empty();
  }

  private Optional<LobbyServerProperties> getRemoteProperties() {
    final String lobbyPropsUrl = UrlConstants.LOBBY_PROPS.toString();

    final Version currentVersion = ClientContext.engineVersion();

    final Optional<LobbyServerProperties> lobbyProps = downloadAndParseRemoteFile(lobbyPropsUrl, currentVersion,
        LobbyPropertyFileParser::parse);

    lobbyProps.ifPresent(props -> {
      ClientSetting.lobbyLastUsedHost.setValue(props.getHost());
      ClientSetting.lobbyLastUsedPort.setValue(props.getPort());
      ClientSetting.flush();
    });

    return lobbyProps;
  }

  /**
   * Downloads the lobby properties file from the specified URL and returns the parsed properties.
   *
   * @param lobbyPropFileUrl The target URL to scrape for a lobby properties file.
   * @param currentVersion Our current engine version. The properties file can contain
   *        multiple listings for different versions.
   * @return Parsed LobbyServerProperties object from the data we found at the remote url,
   *         or an empty optional if there were any errors downloading the properties file.
   */
  @VisibleForTesting
  Optional<LobbyServerProperties> downloadAndParseRemoteFile(
      final String lobbyPropFileUrl,
      final Version currentVersion,
      final BiFunction<String, Version, LobbyServerProperties> propertyParser) {
    return fileDownloader.apply(lobbyPropFileUrl).map(downloadFile -> {
      try {
        final String yamlContent = new String(Files.readAllBytes(downloadFile.toPath()), StandardCharsets.UTF_8);

        final LobbyServerProperties properties =
            propertyParser.apply(yamlContent, currentVersion);

        if (!downloadFile.delete()) {
          downloadFile.deleteOnExit();
        }

        return properties;
      } catch (final IOException e) {
        log.log(Level.SEVERE, "Failed loading file: " + lobbyPropFileUrl + ", please try again, if the "
            + "problem does not go away please report a bug: " + UrlConstants.GITHUB_ISSUES, e);
        return null;
      }
    });
  }
}
