package games.strategy.engine.framework.map.download;

import org.triplea.http.client.maps.listing.MapDownloadItem;

interface DownloadListener {
  void downloadStarted(MapDownloadItem download);

  void downloadUpdated(MapDownloadItem download, long bytesReceived);

  void downloadComplete(MapDownloadItem download);
}
