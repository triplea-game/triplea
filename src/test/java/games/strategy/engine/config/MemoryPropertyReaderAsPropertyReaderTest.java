package games.strategy.engine.config;

import java.util.Map;

public final class MemoryPropertyReaderAsPropertyReaderTest extends AbstractPropertyReaderTestCase {
  @Override
  protected PropertyReader createPropertyReader(final Map<String, String> properties) {
    return new MemoryPropertyReader(properties);
  }
}
