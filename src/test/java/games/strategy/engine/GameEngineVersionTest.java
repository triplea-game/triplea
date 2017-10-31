package games.strategy.engine;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import games.strategy.util.Tuple;
import games.strategy.util.Version;

public final class GameEngineVersionTest {
  @Test
  public void isCompatibleWithEngineVersion_ShouldReturnTrueWhenOtherVersionIsCompatible() {
    Arrays.asList(
        Tuple.of(new Version(1, 2, 3, 4), "equal versions should be compatible"),
        Tuple.of(new Version(1, 2, 3, 0), "smaller micro version should be compatible"),
        Tuple.of(new Version(1, 2, 3, 9), "larger micro version should be compatible"))
        .forEach(t -> assertTrue(
            GameEngineVersion.of(new Version(1, 2, 3, 4)).isCompatibleWithEngineVersion(t.getFirst()),
            t.getSecond()));
  }

  @Test
  public void isCompatibleWithEngineVersion_ShouldReturnFalseWhenOtherVersionIsNotCompatible() {
    Arrays.asList(
        Tuple.of(new Version(1, 2, 0, 4), "smaller point version should not be compatible"),
        Tuple.of(new Version(1, 2, 9, 4), "larger point version should not be compatible"),
        Tuple.of(new Version(1, 0, 3, 4), "smaller minor version should not be compatible"),
        Tuple.of(new Version(1, 9, 3, 4), "larger minor version should not be compatible"),
        Tuple.of(new Version(0, 2, 3, 4), "smaller major version should not be compatible"),
        Tuple.of(new Version(9, 2, 3, 4), "larger major version should not be compatible"))
        .forEach(t -> assertFalse(
            GameEngineVersion.of(new Version(1, 2, 3, 4)).isCompatibleWithEngineVersion(t.getFirst()),
            t.getSecond()));
  }

  @Test
  public void isCompatibleWithMapMinimumEngineVersion_ShouldReturnTrueWhenOtherVersionIsCompatible() {
    Arrays.asList(
        Tuple.of(new Version(1, 2, 3, 4), "equal versions should be compatible"),
        Tuple.of(new Version(1, 2, 3, 0), "smaller micro version should be compatible"),
        Tuple.of(new Version(1, 2, 3, 9), "larger micro version should be compatible"),
        Tuple.of(new Version(1, 2, 0, 4), "smaller point version should be compatible"),
        Tuple.of(new Version(1, 0, 3, 4), "smaller minor version should be compatible"),
        Tuple.of(new Version(0, 2, 3, 4), "smaller major version should be compatible"))
        .forEach(t -> assertTrue(
            GameEngineVersion.of(new Version(1, 2, 3, 4)).isCompatibleWithMapMinimumEngineVersion(t.getFirst()),
            t.getSecond()));
  }

  @Test
  public void isCompatibleWithMapMinimumEngineVersion_ShouldReturnFalseWhenOtherVersionIsNotCompatible() {
    Arrays.asList(
        Tuple.of(new Version(1, 2, 9, 4), "larger point version should not be compatible"),
        Tuple.of(new Version(1, 9, 3, 4), "larger minor version should not be compatible"),
        Tuple.of(new Version(9, 2, 3, 4), "larger major version should not be compatible"))
        .forEach(t -> assertFalse(
            GameEngineVersion.of(new Version(1, 2, 3, 4)).isCompatibleWithMapMinimumEngineVersion(t.getFirst()),
            t.getSecond()));
  }
}
