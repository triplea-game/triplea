package org.triplea.live.servers;

import games.strategy.engine.framework.system.HttpProxy;
import games.strategy.triplea.UrlConstants;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.triplea.http.client.latest.version.LatestVersionResponse;
import org.triplea.io.CloseableDownloader;
import org.triplea.io.ContentDownloader;
import org.triplea.yaml.YamlReader;

@Slf4j
@UtilityClass
public class LiveServersFetcher {

  /**
   * Fetches from remote source the latest TripleA version that has been released. This can be used
   * to determine if the current version is out of date.
   */
  public static Optional<LatestVersionResponse> latestVersion() {
    try (CloseableDownloader downloader =
        new ContentDownloader(UrlConstants.LIVE_SERVERS_URI, HttpProxy::addProxy)) {
      var inputStream = downloader.getStream();
      final Map<String, Object> yamlProps = YamlReader.readMap(inputStream);

      return Optional.of(
          LatestVersionResponse.builder()
              .latestEngineVersion((String) yamlProps.get("latest"))
              .downloadUrl(UrlConstants.DOWNLOAD_WEBSITE)
              .releaseNotesUrl(UrlConstants.RELEASE_NOTES)
              .build());
    } catch (final IOException e) {
      log.info(
          "Unable to complete engine out-of-date check. "
              + "(No internet connection or server not available?)",
          e);
      return Optional.empty();
    }
  }

  /**
   * Reads from remote source the "lobby welcome message", this is the lobby-chat message displayed
   * first to anyone joining the lobby.
   */
  public static Optional<String> getLobbyMessage() {
    try (CloseableDownloader downloader =
        new ContentDownloader(UrlConstants.LIVE_SERVERS_URI, HttpProxy::addProxy)) {
      var inputStream = downloader.getStream();
      final Map<String, Object> yamlProps = YamlReader.readMap(inputStream);
      return Optional.of((String) yamlProps.get("message"));
    } catch (IOException e) {
      log.info("Unable to fetch lobby welcome message", e);
      return Optional.empty();
    }
  }
}
