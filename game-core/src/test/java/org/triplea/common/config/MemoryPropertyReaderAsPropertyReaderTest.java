package org.triplea.common.config;

import java.util.Map;

public final class MemoryPropertyReaderAsPropertyReaderTest extends AbstractPropertyReaderTestCase {
  @Override
  protected PropertyReader createPropertyReader(final Map<String, String> properties) {
    return new MemoryPropertyReader(properties);
  }
}
