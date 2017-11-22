package games.strategy.engine.config.client;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.Arrays;
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

  @Test
  public void useJavaFxUi_ShouldReturnTrueWhenJavaFxUiEnabled() {
    Arrays.asList("true", "TRUE")
        .forEach(value -> {
          final GameEnginePropertyReader gameEnginePropertyReader =
              newGameEnginePropertyReader(PropertyKeys.JAVAFX_UI, value);
          assertThat(gameEnginePropertyReader.useJavaFxUi(), is(true));
        });
  }

  @Test
  public void useJavaFxUi_ShouldReturnFalseWhenJavaFxUiDisabled() {
    Arrays.asList("", "false", "FALSE", "other")
        .forEach(value -> {
          final GameEnginePropertyReader gameEnginePropertyReader =
              newGameEnginePropertyReader(PropertyKeys.JAVAFX_UI, value);
          assertThat(gameEnginePropertyReader.useJavaFxUi(), is(false));
        });
  }
}
