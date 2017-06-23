package games.strategy.engine.config.client.remote;

import java.io.IOException;

import com.google.common.annotations.VisibleForTesting;

import games.strategy.engine.framework.map.download.DownloadUtils;
import games.strategy.engine.lobby.client.login.LobbyServerProperties;
import games.strategy.util.Version;

public class LobbyServerPropertiesFetcher {

  private final UrlRedirectResolver urlRedirectResolver;
  private final LobbyPropertyFileParser lobbyPropertyFileParser;
  private final FileDownloader fileDownloader;


  /**
   * Default constructor with default (prod) dependencies.
   * This allows us to fetch a remote file and parse it for lobby properties.
   * Those properties then tell the game client how/where to connect to the lobby.
   */
  public LobbyServerPropertiesFetcher() {
    this(
        new UrlRedirectResolver(),
        new LobbyPropertyFileParser(),
        FileDownloader.defaultDownloader);
  }

  @VisibleForTesting
  LobbyServerPropertiesFetcher(
      final UrlRedirectResolver urlRedirectResolver,
      final LobbyPropertyFileParser lobbyPropertyFileParser,
      final FileDownloader fileDownloader) {
    this.urlRedirectResolver = urlRedirectResolver;
    this.lobbyPropertyFileParser = lobbyPropertyFileParser;
    this.fileDownloader = fileDownloader;
  }

  /**
   *
   * @param lobbyPropFileUrl The taret URL to scrape for a lobby properties file.
   * @param currentVersion Our current engine version. The properties file can contain
   *                       multiple listings for different versions.
   * @return Parsed LobbyServerProperties object from the data we found at the remote
   *     url.
   * @throws IOException Thrown if there is a failure doing the remote network fetching
   *     or IO problem once we downloaded the remote file to a temp file and are then
   *     reading it..
   */
  public LobbyServerProperties downloadAndParseRemoteFile(
      final String lobbyPropFileUrl,
      final Version currentVersion) throws IOException {
    final String lobbyUrl = urlRedirectResolver.getUrlFollowingRedirects(lobbyPropFileUrl);

    final DownloadUtils.FileDownloadResult fileDownloadResult = fileDownloader.download(lobbyUrl);

    if (!fileDownloadResult.wasSuccess) {
      throw new IOException("Failed to download: " + lobbyPropFileUrl);
    }
    final LobbyServerProperties properties =
        lobbyPropertyFileParser.parse(fileDownloadResult.downloadedFile, currentVersion);

    // delete file after it has been used. If there there was an IOException, the 'deleteOnExit' should
    // kick in and delete the file. (hence there is no try/catch/finally block here)
    fileDownloadResult.downloadedFile.delete();
    return properties;
  }

}
