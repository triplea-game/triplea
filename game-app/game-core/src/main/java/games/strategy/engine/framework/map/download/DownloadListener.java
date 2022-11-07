package games.strategy.engine.framework.map.download;

import org.triplea.http.client.maps.listing.MapDownloadItem;

interface DownloadListener {
  void downloadUpdated(MapDownloadItem download, long bytesReceived);

  void downloadComplete(MapDownloadItem download);
}
