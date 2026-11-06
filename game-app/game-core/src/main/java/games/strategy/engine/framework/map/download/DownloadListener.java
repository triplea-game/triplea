package games.strategy.engine.framework.map.download;

import org.triplea.http.client.lobby.maps.listing.MapDownloadItem;

interface DownloadListener {
  void downloadUpdated(MapDownloadItem download, long bytesReceived);

  void downloadComplete(MapDownloadItem download);

  /** The download could not be completed, eg the server could not be reached. */
  void downloadFailed(MapDownloadItem download);
}
