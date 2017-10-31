package games.strategy.engine;

import static games.strategy.engine.GameEngineUtils.isEngineCompatibleWithEngine;
import static games.strategy.engine.GameEngineUtils.isEngineCompatibleWithMap;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import games.strategy.util.Tuple;
import games.strategy.util.Version;

public final class GameEngineUtilsTest {
  @Test
  public void isEngineCompatibleWithEngine_ShouldReturnTrueWhenOtherVersionIsCompatible() {
    Arrays.asList(
        Tuple.of(new Version(1, 2, 3, 4), "equal versions should be compatible"),
        Tuple.of(new Version(1, 2, 3, 0), "smaller micro version should be compatible"),
        Tuple.of(new Version(1, 2, 3, 9), "larger micro version should be compatible"))
        .forEach(t -> assertTrue(isEngineCompatibleWithEngine(new Version(1, 2, 3, 4), t.getFirst()), t.getSecond()));
  }

  @Test
  public void isEngineCompatibleWithEngine_ShouldReturnFalseWhenOtherVersionIsNotCompatible() {
    Arrays.asList(
        Tuple.of(new Version(1, 2, 0, 4), "smaller point version should not be compatible"),
        Tuple.of(new Version(1, 2, 9, 4), "larger point version should not be compatible"),
        Tuple.of(new Version(1, 0, 3, 4), "smaller minor version should not be compatible"),
        Tuple.of(new Version(1, 9, 3, 4), "larger minor version should not be compatible"),
        Tuple.of(new Version(0, 2, 3, 4), "smaller major version should not be compatible"),
        Tuple.of(new Version(9, 2, 3, 4), "larger major version should not be compatible"))
        .forEach(t -> assertFalse(isEngineCompatibleWithEngine(new Version(1, 2, 3, 4), t.getFirst()), t.getSecond()));
  }

  @Test
  public void testIsEngineCompatibleWithMap_ShouldReturnTrueWhenOtherVersionIsCompatible() {
    Arrays.asList(
        Tuple.of(new Version(1, 2, 3, 4), "equal versions should be compatible"),
        Tuple.of(new Version(1, 2, 3, 9), "smaller micro version should be compatible"),
        Tuple.of(new Version(1, 2, 3, 0), "larger micro version should be compatible"),
        Tuple.of(new Version(1, 2, 0, 4), "smaller point version should be compatible"),
        Tuple.of(new Version(1, 0, 3, 4), "smaller minor version should be compatible"),
        Tuple.of(new Version(0, 2, 3, 4), "smaller major version should be compatible"))
        .forEach(t -> assertTrue(isEngineCompatibleWithMap(new Version(1, 2, 3, 4), t.getFirst()), t.getSecond()));
  }

  @Test
  public void testIsEngineCompatibleWithMap_ShouldReturnFalseWhenOtherVersionIsNotCompatible() {
    Arrays.asList(
        Tuple.of(new Version(1, 2, 9, 4), "larger point version should not be compatible"),
        Tuple.of(new Version(1, 9, 3, 4), "larger minor version should not be compatible"),
        Tuple.of(new Version(9, 2, 3, 4), "larger major version should not be compatible"))
        .forEach(t -> assertFalse(isEngineCompatibleWithEngine(new Version(1, 2, 3, 4), t.getFirst()), t.getSecond()));
  }
}
