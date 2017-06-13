package games.strategy.engine.framework.map.download;

interface DownloadListener {
  void downloadStarted(DownloadFileDescription download);

  void downloadUpdated(DownloadFileDescription download, long bytesReceived);

  // TODO: possibly need to pass a second enum parameter describing the "reason" the download stopped
  // (e.g. DONE, CANCELLED, etc.)
  void downloadStopped(DownloadFileDescription download);
}