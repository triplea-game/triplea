package org.triplea.io;

import java.io.Closeable;
import java.io.InputStream;

public interface CloseableDownloader extends Closeable {
  InputStream getStream();
}
