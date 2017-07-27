package games.strategy.engine.config.client;

import games.strategy.engine.framework.map.download.DownloadUtils;

/**
 * Humble object pattern. Wraps file downloading.
 */
@FunctionalInterface
public interface LobbyLocationFileDownloader {

  DownloadUtils.FileDownloadResult download(final String url);

  LobbyLocationFileDownloader defaultDownloader = DownloadUtils::downloadToFile;
}
