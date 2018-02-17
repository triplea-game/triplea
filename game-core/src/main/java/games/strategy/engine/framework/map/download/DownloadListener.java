package games.strategy.engine.framework.map.download;

interface DownloadListener {
  void downloadStarted(DownloadFileDescription download);

  void downloadUpdated(DownloadFileDescription download, long bytesReceived);

  void downloadStopped(DownloadFileDescription download);
}
