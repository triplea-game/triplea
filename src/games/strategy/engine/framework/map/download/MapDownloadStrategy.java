package games.strategy.engine.framework.map.download;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.apache.commons.io.FileUtils;

import games.strategy.debug.ClientLogger;

public class MapDownloadStrategy {

  private static final int READ_TIMEOUT_MS = 5000;
  private static final int CONNECTION_TIMEOUT_MS = 5000;

  public void download(final URL url, final File fileToDownloadTo) {
    try {
      FileUtils.copyURLToFile(url, fileToDownloadTo, CONNECTION_TIMEOUT_MS, READ_TIMEOUT_MS);
    } catch (final IOException e) {
      ClientLogger.logError("Failed to download: " + url, e);
    }
  }
}
