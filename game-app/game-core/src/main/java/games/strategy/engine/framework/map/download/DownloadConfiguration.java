package games.strategy.engine.framework.map.download;

import games.strategy.engine.framework.system.HttpProxy;
import lombok.experimental.UtilityClass;
import org.triplea.io.ContentDownloader;

/** Provides methods to download files via HTTP. */
@UtilityClass
public final class DownloadConfiguration {
  private static final ContentReader contentReader =
      new ContentReader(uri -> new ContentDownloader(uri, HttpProxy::addProxy));

  public static ContentReader contentReader() {
    return contentReader;
  }
}
