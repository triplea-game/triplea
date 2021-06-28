package games.strategy.engine.framework.map.download;

import org.triplea.http.client.maps.listing.MapDownloadListing;

interface DownloadListener {
  void downloadStarted(MapDownloadListing download);

  void downloadUpdated(MapDownloadListing download, long bytesReceived);

  void downloadComplete(MapDownloadListing download);
}
