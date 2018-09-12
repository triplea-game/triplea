package games.strategy.engine.config.client;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;
import org.triplea.test.common.Integration;

@Integration
final class GameEnginePropertyReaderIntegrationTest {
  private final GameEnginePropertyReader gameEnginePropertyReader = new GameEnginePropertyReader();

  @Test
  void shouldReadPropertiesFromResource() {
    assertThat(gameEnginePropertyReader.getEngineVersion().getExactVersion(), is("1.9.0.0.dev"));
  }
}
