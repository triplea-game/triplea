package games.strategy.engine.config.client.remote;

import games.strategy.engine.framework.map.download.DownloadUtils;

/**
 * Humble object pattern. Wraps file downloading.
 */
@FunctionalInterface
public interface FileDownloader {

  DownloadUtils.FileDownloadResult download(final String url);

  FileDownloader defaultDownloader = DownloadUtils::downloadToFile;
}
