package games.strategy.engine.lobby.client.login;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.function.BiFunction;

import javax.annotation.Nullable;

import org.triplea.java.function.ThrowingFunction;
import org.triplea.util.Version;

import com.google.common.annotations.VisibleForTesting;

import games.strategy.engine.ClientContext;
import games.strategy.triplea.UrlConstants;
import games.strategy.triplea.settings.ClientSetting;
import games.strategy.triplea.settings.GameSetting;

/**
 * Fetches the lobby server properties from the remote Source of Truth.
 */
public final class LobbyServerPropertiesFetcher {

  public static final int TEST_LOBBY_DEFAULT_PORT = 3304;
  public static final int TEST_LOBBY_DEFAULT_HTTPS_PORT = 8080;

  private final BiFunction<String, ThrowingFunction<InputStream, LobbyServerProperties, IOException>,
      Optional<LobbyServerProperties>> fileDownloader;
  @Nullable
  private LobbyServerProperties lobbyServerProperties;


  LobbyServerPropertiesFetcher(
      final BiFunction<String, ThrowingFunction<InputStream, LobbyServerProperties, IOException>,
          Optional<LobbyServerProperties>> fileDownloader) {
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
    return getLastUsedProperties();
  }

  private static Optional<LobbyServerProperties> getTestOverrideProperties() {
    return getTestOverrideProperties(
        ClientSetting.testLobbyHost,
        ClientSetting.testLobbyPort,
        ClientSetting.testLobbyHttpsPort);
  }



  @VisibleForTesting
  static Optional<LobbyServerProperties> getTestOverrideProperties(
      final GameSetting<String> testLobbyHostSetting,
      final GameSetting<Integer> testLobbyPortSetting,
      final GameSetting<Integer> testLobbyHttpsPort) {
    if (testLobbyHostSetting.isSet()) {
      return Optional.of(
          LobbyServerProperties.builder()
              .host(testLobbyHostSetting.getValueOrThrow())
              .port(testLobbyPortSetting.getValue().orElse(TEST_LOBBY_DEFAULT_PORT))
              .httpsPort(testLobbyHttpsPort.getValue().orElse(TEST_LOBBY_DEFAULT_HTTPS_PORT))
              .build());
    }

    return Optional.empty();
  }

  private Optional<LobbyServerProperties> getRemoteProperties() {
    final Optional<LobbyServerProperties> lobbyProps = downloadAndParseRemoteFile(
        UrlConstants.LOBBY_PROPS,
        ClientContext.engineVersion(),
        LobbyPropertyFileParser::parse);
    lobbyProps.ifPresent(props -> {
      ClientSetting.lobbyLastUsedHost.setValue(props.getHost());
      ClientSetting.lobbyLastUsedPort.setValue(props.getPort());
      ClientSetting.lobbyLastUsedHttpsPort.setValue(props.getHttpsPort());
      ClientSetting.flush();
    });
    return lobbyProps;
  }

  private static Optional<LobbyServerProperties> getLastUsedProperties() {
    if (ClientSetting.lobbyLastUsedHost.isSet()) {
      return Optional.of(LobbyServerProperties.builder()
          .host(ClientSetting.lobbyLastUsedHost.getValueOrThrow())
          .port(ClientSetting.lobbyLastUsedPort.getValue().orElse(TEST_LOBBY_DEFAULT_PORT))
          .httpsPort(ClientSetting.lobbyLastUsedHttpsPort.getValue().orElse(TEST_LOBBY_DEFAULT_HTTPS_PORT))
          .build());
    }

    return Optional.empty();
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
      final BiFunction<InputStream, Version, LobbyServerProperties> propertyParser) {
    return fileDownloader.apply(lobbyPropFileUrl, inputStream -> propertyParser.apply(inputStream, currentVersion));
  }
}
