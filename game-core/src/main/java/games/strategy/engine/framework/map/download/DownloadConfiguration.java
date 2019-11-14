package games.strategy.engine.framework.map.download;
// TODO: move to package games.strategy.engine.framework.map.download.client

import lombok.experimental.UtilityClass;
import org.apache.http.impl.client.HttpClients;

/** Provides methods to download files via HTTP. */
@UtilityClass
public final class DownloadConfiguration {
  private static final ContentReader contentReader = new ContentReader(ContentDownloader::new);
  private static final DownloadLengthReader downloadLengthReader =
      new DownloadLengthReader(() -> HttpClients.custom().disableCookieManagement().build());

  public static ContentReader contentReader() {
    return contentReader;
  }

  public static DownloadLengthReader downloadLengthReader() {
    return downloadLengthReader;
  }
}
