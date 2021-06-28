package games.strategy.engine.framework.map.download;

import java.util.List;
import lombok.AllArgsConstructor;
import org.triplea.http.client.maps.listing.MapDownloadListing;
import org.triplea.http.client.maps.listing.MapsListingClient;

/**
 * Downloads a map index file, parses it and returns a <code>List</code> of <code>
 * DownloadFileDescription</code>.
 */
@AllArgsConstructor
public class DownloadRunnable implements MapsListingClient {

  /** URL of the maps YAML file */
  private final String url;

  /** Parses a file at the given URL. If an error occurs this will return an empty list. */
  @Override
  public List<MapDownloadListing> fetchMapDownloads() {
    return DownloadConfiguration.contentReader()
        .download(url, DownloadFileParser::parse)
        .orElseGet(List::of);
  }
}
