package org.triplea.config;

import java.util.Map;

final class MemoryPropertyReaderAsPropertyReaderTest extends AbstractPropertyReaderTestCase {
  @Override
  protected PropertyReader newPropertyReader(final Map<String, String> properties) {
    return new MemoryPropertyReader(properties);
  }
}
