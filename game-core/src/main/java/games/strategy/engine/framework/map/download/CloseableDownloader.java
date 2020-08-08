package games.strategy.engine.framework.map.download;

import java.io.Closeable;
import java.io.InputStream;

public interface CloseableDownloader extends Closeable {
  InputStream getStream();
}
