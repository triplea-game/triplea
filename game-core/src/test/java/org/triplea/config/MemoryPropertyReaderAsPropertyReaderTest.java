package org.triplea.config;

import java.util.Map;
import org.triplea.test.common.ExtendedUnitTest;

@ExtendedUnitTest
final class MemoryPropertyReaderAsPropertyReaderTest extends AbstractPropertyReaderTestCase {
  @Override
  protected PropertyReader newPropertyReader(final Map<String, String> properties) {
    final MemoryPropertyReader memoryPropertyReader = new MemoryPropertyReader();
    properties.forEach(memoryPropertyReader::setProperty);
    return memoryPropertyReader;
  }
}
