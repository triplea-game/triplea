package org.triplea.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

class VersionTest {
  @Test void shouldBeEquatableAndHashable() {
    EqualsVerifier.forClass(Version.class).withIgnoredFields("exactVersion").verify();
  }

  @Test void compareTo_ShouldThrowExceptionWhenOtherIsNull() {
    assertThrows(NullPointerException.class, () -> new Version(1, 0).compareTo(null));
  }

  @Test void compareTo_ShouldReturnNegativeIntegerWhenFirstIsLessThanSecond() {
    Arrays.asList(
        Tuple.of(new Version(1, 0, 0), new Version(2, 0, 0)),
        Tuple.of(new Version(0, 1, 0), new Version(0, 2, 0)),
        Tuple.of(new Version(0, 0, 1), new Version(0, 0, 2)),
        Tuple.of(new Version(0, 0, 0), new Version("0.0.dev")))
        .forEach(t -> assertThat(t.getFirst().compareTo(t.getSecond()), is(lessThan(0))));
  }

  @Test void compareTo_ShouldReturnZeroWhenFirstIsEqualToSecond() {
    Arrays.asList(
        Tuple.of(new Version(1, 0, 0), new Version(1, 0, 0)),
        Tuple.of(new Version(0, 1, 0), new Version(0, 1, 0)),
        Tuple.of(new Version(0, 0, 1), new Version(0, 0, 1)),
        Tuple.of(new Version("0.0.dev"), new Version("0.0.dev")))
        .forEach(t -> assertThat(t.getFirst().compareTo(t.getSecond()), is(0)));
  }

  @Test void compareTo_ShouldReturnPositiveIntegerWhenFirstIsGreaterThanSecond() {
    Arrays.asList(
        Tuple.of(new Version(2, 0, 0), new Version(1, 0, 0)),
        Tuple.of(new Version(0, 2, 0), new Version(0, 1, 0)),
        Tuple.of(new Version(0, 0, 2), new Version(0, 0, 1)),
        Tuple.of(new Version("0.0.dev"), new Version(0, 0, 0)))
        .forEach(t -> assertThat(t.getFirst().compareTo(t.getSecond()), is(greaterThan(0))));
  }

  @Test
  void testWithPoint() {
    assertEquals(new Version(1, 2, 999), new Version(1, 2, 3).withPoint(999));
  }

  @Test
  void testIsGreaterThan() {
    assertFalse(new Version(1, 0).isGreaterThan(new Version(2, 0)));
    assertFalse(new Version(1, 0).isGreaterThan(new Version(1, 0)));
    assertTrue(new Version(2, 0).isGreaterThan(new Version(1, 0)));
  }

  @Test
  void testIsGreaterThanOrEqualTo() {
    assertFalse(new Version(1, 0).isGreaterThanOrEqualTo(new Version(2, 0)));
    assertTrue(new Version(1, 0).isGreaterThanOrEqualTo(new Version(1, 0)));
    assertTrue(new Version(2, 0).isGreaterThanOrEqualTo(new Version(1, 0)));
  }

  @Test
  void testIsLessThan() {
    assertTrue(new Version(1, 0).isLessThan(new Version(2, 0)));
    assertFalse(new Version(1, 0).isLessThan(new Version(1, 0)));
    assertFalse(new Version(2, 0).isLessThan(new Version(1, 0)));
  }

  @Test
  void testToString() {
    assertEquals("1.2.3", new Version("1.2.3").toString());
    assertEquals("1.2", new Version("1.2").toString());
    assertEquals("1.2", new Version("1.2.0").toString());
    assertEquals("1.2.dev", new Version("1.2.dev").toString());
  }

  @Test
  void testToStringFull() {
    assertEquals("1.2.dev", new Version("1.2.dev").toStringFull());
  }

  @Test
  void testGetExactVersion() {
    assertEquals("1.2.3", new Version(1, 2, 3).getExactVersion());
    assertEquals("1.2.3.4", new Version("1.2.3.4").getExactVersion());
    assertEquals("1.2.3.4.something weird", new Version("1.2.3.4.something weird").getExactVersion());
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
}
