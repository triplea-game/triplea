package games.strategy.engine.framework.map.listing;

import feign.FeignException;
import games.strategy.engine.framework.map.download.DownloadConfiguration;
import games.strategy.engine.framework.map.download.DownloadFileDescription;
import games.strategy.engine.framework.map.download.DownloadFileParser;
import games.strategy.triplea.settings.ClientSetting;
import java.util.ArrayList;
import java.util.List;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.triplea.config.product.ProductVersionReader;
import org.triplea.domain.data.SystemIdLoader;
import org.triplea.http.client.lib.ClientIdentifiers;
import org.triplea.http.client.maps.listing.MapDownloadItem;
import org.triplea.http.client.maps.listing.MapsClient;

@UtilityClass
@Slf4j
public class MapListingFetcher {

  /**
   * Parses a file at the given URL into a List of {@link DownloadFileDescription}s. If an error
   * occurs this will return an empty list.
   */
  private static List<DownloadFileDescription> download(final String url) {
    return DownloadConfiguration.contentReader()
        .download(url, DownloadFileParser::parse)
        .orElseGet(List::of);
  }

  private static List<MapDownloadItem> getMapDownloadListFromGithub() {
    String MAP_DOWNLOAD_LIST =
        "https://raw.githubusercontent.com/triplea-game/triplea/master/triplea_maps.yaml";
    var result = new ArrayList<MapDownloadItem>();
    for (var entry : download(MAP_DOWNLOAD_LIST)) {
      result.add(
          MapDownloadItem.builder()
              .mapName(entry.getMapName())
              .downloadUrl(entry.getUrl())
              .description(entry.getDescription())
              .previewImageUrl(entry.getImg())
              .downloadSizeInBytes(-1L)
              .lastCommitDateEpochMilli(0L)
              .mapTags(List.of())
              .build());
    }
    return result;
  }

  /** Fetches the full listing of maps that are available for download. */
  public static List<MapDownloadItem> getMapDownloadList() {
    if (true) {
      return getMapDownloadListFromGithub();
    }

    final var serverUri = ClientSetting.lobbyUri.getValueOrThrow();
    try {
      return MapsClient.newClient(
              serverUri,
              ClientIdentifiers.builder()
                  .applicationVersion(ProductVersionReader.getCurrentVersion().toMajorMinorString())
                  .systemId(SystemIdLoader.load().getValue())
                  .build())
          .fetchMapListing();
    } catch (FeignException e) {
      log.warn(
          "Failed to download the list of available maps from TripleA servers.\n"
              + "You can download the needed maps manually into your TripleA maps subfolder from <a href='https://github.com/triplea-maps/'>https://github.com/triplea-maps/</a>.",
          e);
      return List.of();
    }
  }
}
