package games.strategy.engine;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import games.strategy.util.Tuple;
import games.strategy.util.Version;

final class GameEngineVersionTest {
  @Nested
  final class IsCompatibleWithEngineVersionTest {
    @Test
    void shouldReturnTrueWhenOtherVersionIsCompatible() {
      Arrays.asList(
          Tuple.of(new Version(1, 2, 3), "equal versions should be compatible"),
          Tuple.of(new Version(1, 2, 0), "smaller point version should be compatible"),
          Tuple.of(new Version(1, 2, 9), "larger point version should be compatible"))
          .forEach(t -> assertTrue(
              GameEngineVersion.of(new Version(1, 2, 3)).isCompatibleWithEngineVersion(t.getFirst()),
              t.getSecond()));
    }

    @Test
    void shouldReturnFalseWhenOtherVersionIsNotCompatible() {
      Arrays.asList(
          Tuple.of(new Version(1, 0, 3), "smaller minor version should not be compatible"),
          Tuple.of(new Version(1, 9, 3), "larger minor version should not be compatible"),
          Tuple.of(new Version(0, 2, 3), "smaller major version should not be compatible"),
          Tuple.of(new Version(9, 2, 3), "larger major version should not be compatible"))
          .forEach(t -> assertFalse(
              GameEngineVersion.of(new Version(1, 2, 3)).isCompatibleWithEngineVersion(t.getFirst()),
              t.getSecond()));
    }
  }

  @Nested
  final class IsCompatibleWithMapMinimumEngineVersionTest {
    @Test
    void shouldReturnTrueWhenOtherVersionIsCompatible() {
      Arrays.asList(
          Tuple.of(new Version(1, 2, 3), "equal versions should be compatible"),
          Tuple.of(new Version(1, 2, 0), "smaller point version should be compatible"),
          Tuple.of(new Version(1, 2, 9), "larger point version should be compatible"),
          Tuple.of(new Version(1, 0, 3), "smaller minor version should be compatible"),
          Tuple.of(new Version(0, 2, 3), "smaller major version should be compatible"))
          .forEach(t -> assertTrue(
              GameEngineVersion.of(new Version(1, 2, 3)).isCompatibleWithMapMinimumEngineVersion(t.getFirst()),
              t.getSecond()));
    }

    @Test
    void shouldReturnFalseWhenOtherVersionIsNotCompatible() {
      Arrays.asList(
          Tuple.of(new Version(1, 9, 3), "larger minor version should not be compatible"),
          Tuple.of(new Version(9, 2, 3), "larger major version should not be compatible"))
          .forEach(t -> assertFalse(
              GameEngineVersion.of(new Version(1, 2, 3)).isCompatibleWithMapMinimumEngineVersion(t.getFirst()),
              t.getSecond()));
    }
  }
}
