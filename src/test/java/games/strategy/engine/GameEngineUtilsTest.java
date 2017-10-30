package games.strategy.engine;

import static games.strategy.engine.GameEngineUtils.isEngineCompatibleWithEngine;
import static games.strategy.engine.GameEngineUtils.isEngineCompatibleWithMap;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import games.strategy.util.Version;

public final class GameEngineUtilsTest {
  @Test
  public void testIsEngineCompatibleWithEngine() {
    assertTrue(isEngineCompatibleWithEngine(new Version(1, 9), new Version(1, 9)));
    assertTrue(isEngineCompatibleWithEngine(new Version(1, 9), new Version(1, 9, 0, 1)));
    assertTrue(isEngineCompatibleWithEngine(new Version(1, 9, 0, 1), new Version(1, 9, 0, 2)));
    assertTrue(isEngineCompatibleWithEngine(new Version(1, 9, 1), new Version(1, 9, 1, 3)));
    assertTrue(isEngineCompatibleWithEngine(new Version(1, 9, 1, 2), new Version(1, 9, 1)));

    assertFalse(isEngineCompatibleWithEngine(new Version(2, 9), new Version(1, 9)));
    assertFalse(isEngineCompatibleWithEngine(new Version(1, 10), new Version(1, 9)));
    assertFalse(isEngineCompatibleWithEngine(new Version(2, 10), new Version(1, 9)));
    assertFalse(isEngineCompatibleWithEngine(new Version(1, 9, 1, 9), new Version(2, 0, 1, 9)));
    assertFalse(isEngineCompatibleWithEngine(new Version(1, 9, 0), new Version(1, 9, 1)));
  }

  @Test
  public void testIsEngineCompatibleWithMap() {
    assertTrue(isEngineCompatibleWithMap(new Version(1, 9, 0, 0), new Version(1, 9, 0, 0)));
    assertTrue(isEngineCompatibleWithMap(new Version(1, 9, 0, 1), new Version(1, 9, 0, 0)));
    assertTrue(isEngineCompatibleWithMap(new Version(1, 9, 0, 0), new Version(1, 9, 0, 1)));
    assertTrue(isEngineCompatibleWithMap(new Version(1, 9, 0, 0), new Version(1, 8, 9, 9)));

    assertFalse(isEngineCompatibleWithMap(new Version(1, 9, 0, 0), new Version(1, 9, 1, 0)));
  }
}
