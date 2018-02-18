package games.strategy.engine.config.client;

import com.google.common.annotations.VisibleForTesting;

import games.strategy.engine.framework.map.download.DownloadUtils;

/**
 * Humble object pattern. Wraps file downloading.
 */
@FunctionalInterface
@VisibleForTesting
interface LobbyLocationFileDownloader {

  DownloadUtils.FileDownloadResult download(final String url);

  LobbyLocationFileDownloader defaultDownloader = DownloadUtils::downloadToFile;
}
