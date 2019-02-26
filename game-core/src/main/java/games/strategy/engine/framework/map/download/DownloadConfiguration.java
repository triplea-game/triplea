package games.strategy.engine.framework.map.download;
// TODO: move to package games.strategy.engine.framework.map.download.client

import java.util.function.Supplier;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

/**
 * Provides methods to download files via HTTP.
 */
public final class DownloadConfiguration {

  private static final ContentReader contentReader;
  private static final DownloadLengthReader downloadLengthReader;

  static {
    final Supplier<CloseableHttpClient> httpClientSupplier =
        () -> HttpClients.custom().disableCookieManagement().build();
    contentReader = new ContentReader(httpClientSupplier);
    downloadLengthReader = new DownloadLengthReader(httpClientSupplier);
  }

  private DownloadConfiguration() {}

  public static ContentReader contentReader() {
    return contentReader;
  }

  public static DownloadLengthReader downloadLengthReader() {
    return downloadLengthReader;
  }
}
