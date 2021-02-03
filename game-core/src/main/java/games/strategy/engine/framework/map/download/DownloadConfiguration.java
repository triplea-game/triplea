package games.strategy.engine.framework.map.download;
// TODO: move to package games.strategy.engine.framework.map.download.client

import games.strategy.engine.framework.system.HttpProxy;
import lombok.experimental.UtilityClass;
import org.apache.http.impl.client.HttpClients;
import org.triplea.io.ContentDownloader;

/** Provides methods to download files via HTTP. */
@UtilityClass
public final class DownloadConfiguration {
  private static final ContentReader contentReader =
      new ContentReader(uri -> new ContentDownloader(uri, HttpProxy::addProxy));
  private static final DownloadLengthReader downloadLengthReader =
      new DownloadLengthReader(() -> HttpClients.custom().disableCookieManagement().build());

  public static ContentReader contentReader() {
    return contentReader;
  }

  public static DownloadLengthReader downloadLengthReader() {
    return downloadLengthReader;
  }
}
