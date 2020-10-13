package org.triplea.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@SuppressWarnings("InnerClassMayBeStatic")
class VersionTest {
  @Test
  void shouldBeEquatableAndHashable() {
    EqualsVerifier.forClass(Version.class).verify();
  }

  @Nested
  class CompareTo {
    @Test
    void shouldThrowExceptionWhenOtherIsNull() {
      assertThrows(NullPointerException.class, () -> new Version(1, 0, 0).compareTo(null));
    }

    @Test
    void shouldReturnNegativeIntegerWhenFirstIsLessThanSecond() {
      List.of(
              Tuple.of(new Version(1, 0, 0), new Version(2, 0, 0)),
              Tuple.of(new Version(0, 1, 0), new Version(0, 2, 0)),
              Tuple.of(new Version(0, 0, 1), new Version(0, 0, 2)))
          .forEach(t -> assertThat(t.getFirst().compareTo(t.getSecond()), is(lessThan(0))));
    }

    @Test
    void shouldReturnZeroWhenFirstIsEqualToSecond() {
      List.of(
              Tuple.of(new Version(1, 0, 0), new Version(1, 0, 0)),
              Tuple.of(new Version(0, 1, 0), new Version(0, 1, 0)),
              Tuple.of(new Version(0, 0, 1), new Version(0, 0, 1)))
          .forEach(t -> assertThat(t.getFirst().compareTo(t.getSecond()), is(0)));
    }

    @Test
    void shouldReturnPositiveIntegerWhenFirstIsGreaterThanSecond() {
      List.of(
              Tuple.of(new Version(2, 0, 0), new Version(1, 0, 0)),
              Tuple.of(new Version(0, 2, 0), new Version(0, 1, 0)),
              Tuple.of(new Version(0, 0, 2), new Version(0, 0, 1)))
          .forEach(t -> assertThat(t.getFirst().compareTo(t.getSecond()), is(greaterThan(0))));
    }
  }

  @Test
  void testIsGreaterThan() {
    assertFalse(new Version(1, 0, 0).isGreaterThan(new Version(2, 0, 0)));
    assertFalse(new Version(1, 0, 0).isGreaterThan(new Version(1, 0, 0)));
    assertTrue(new Version(2, 0, 0).isGreaterThan(new Version(1, 0, 0)));
  }

  @Test
  void testToString() {
    assertEquals("1.2.3", new Version("1.2.3").toString());
    assertEquals("1.2.0", new Version("1.2.0").toString());
    assertEquals("1.2.3", new Version(1, 2, 3).toString());
    assertEquals("1.2.3", new Version("1.2.3.4").toString());
    assertEquals("1.2.3", new Version("1.2.3.4.something weird").toString());
  }

  @Test
  void testErrorOnWrongSyntax() {
    assertThrows(IllegalArgumentException.class, () -> new Version("abc12.34.56.78"));
    assertThrows(IllegalArgumentException.class, () -> new Version("abc12.34.56"));
    assertThrows(IllegalArgumentException.class, () -> new Version("abc12.34"));
    assertThrows(IllegalArgumentException.class, () -> new Version("abc12"));
    assertThrows(IllegalArgumentException.class, () -> new Version("a b c.12"));
    assertThrows(IllegalArgumentException.class, () -> new Version("a.b.c.12.34"));
    assertThrows(IllegalArgumentException.class, () -> new Version("a:b:c.12.34.56"));
    assertThrows(IllegalArgumentException.class, () -> new Version("a;b;c.12.34.56.78"));
    assertThrows(IllegalArgumentException.class, () -> new Version("1.2.3 wrong syntax"));
  }

  @Nested
  final class IsCompatibleWithMapMinimumEngineVersionTest {
    @Test
    void shouldReturnTrueWhenOtherVersionIsCompatible() {
      List.of(
              Tuple.of(new Version(1, 2, 3), "equal versions should be compatible"),
              Tuple.of(new Version(0, 9, 0), "smaller major version should be compatible"),
              Tuple.of(new Version(1, 2, 0), "smaller point version should be compatible"),
              Tuple.of(new Version(1, 2, 9), "larger point version should be compatible"),
              Tuple.of(new Version(1, 0, 3), "smaller minor version should be compatible"),
              Tuple.of(new Version(0, 2, 3), "smaller major version should be compatible"))
          .forEach(
              t ->
                  assertTrue(
                      new Version(1, 2, 3).isCompatibleWithMapMinimumEngineVersion(t.getFirst()),
                      t.getSecond()));
    }

    @Test
    void shouldReturnFalseWhenOtherVersionIsNotCompatible() {
      List.of(
              Tuple.of(new Version(1, 9, 3), "larger minor version should not be compatible"),
              Tuple.of(new Version(9, 2, 3), "larger major version should not be compatible"))
          .forEach(
              t ->
                  assertFalse(
                      new Version(1, 2, 3).isCompatibleWithMapMinimumEngineVersion(t.getFirst()),
                      t.getSecond()));
    }
  }
}
