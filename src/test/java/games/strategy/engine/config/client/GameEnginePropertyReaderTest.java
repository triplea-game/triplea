package games.strategy.engine.config.client;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import games.strategy.engine.config.MemoryPropertyReader;
import games.strategy.engine.config.client.GameEnginePropertyReader.PropertyKeys;
import games.strategy.util.Version;

public class GameEnginePropertyReaderTest {
  private static GameEnginePropertyReader newGameEnginePropertyReader(final String key, final String value) {
    return new GameEnginePropertyReader(new MemoryPropertyReader(Collections.singletonMap(key, value)));
  }

  @Test
  public void engineVersion() {
    final GameEnginePropertyReader gameEnginePropertyReader =
        newGameEnginePropertyReader(PropertyKeys.ENGINE_VERSION, "1.0.1.3");

    assertThat(gameEnginePropertyReader.getEngineVersion(), is(new Version(1, 0, 1, 3)));
  }
}
