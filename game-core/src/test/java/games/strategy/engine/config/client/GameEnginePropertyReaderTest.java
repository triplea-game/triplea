package games.strategy.engine.config.client;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import games.strategy.engine.config.MemoryPropertyReader;
import games.strategy.engine.config.client.GameEnginePropertyReader.PropertyKeys;
import games.strategy.util.Version;

public final class GameEnginePropertyReaderTest {
  private final MemoryPropertyReader memoryPropertyReader = new MemoryPropertyReader();
  private final GameEnginePropertyReader gameEnginePropertyReader = new GameEnginePropertyReader(memoryPropertyReader);

  @Test
  public void engineVersion() {
    memoryPropertyReader.setProperty(PropertyKeys.ENGINE_VERSION, "1.0.1.3");

    assertThat(gameEnginePropertyReader.getEngineVersion(), is(new Version(1, 0, 1, 3)));
  }
}
