package games.strategy.engine.lobby.client.login;

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
