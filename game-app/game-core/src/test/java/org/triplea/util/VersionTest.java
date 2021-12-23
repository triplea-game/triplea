package org.triplea.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@SuppressWarnings("InnerClassMayBeStatic")
class VersionTest {
  @Nested
  class PartParsing {
    @Test
    void simpleMajorMinor() {
      final Version version = new Version("1.2");
      assertThat(version.getMajor(), is(1));
      assertThat(version.getMinor(), is(2));
      assertThat(version.getBuildNumber(), is(""));
      assertThat(version.toString(), is("1.2"));
    }

    @Test
    void integerBuildNumber() {
      final Version version = new Version("1.2.3");
      assertThat(version.getMajor(), is(1));
      assertThat(version.getMinor(), is(2));
      assertThat(version.getBuildNumber(), is("3"));
      assertThat(version.toString(), is("1.2.3"));
    }

    @Test
    void stringBuildNumber() {
      final Version version = new Version("1.2.3@xyz");
      assertThat(version.getMajor(), is(1));
      assertThat(version.getMinor(), is(2));
      assertThat(version.getBuildNumber(), is("3@xyz"));
      assertThat(version.toString(), is("1.2.3@xyz"));
    }
  }

  @Nested
  class CompareTo {
    @Test
    void shouldThrowExceptionWhenOtherIsNull() {
      assertThrows(NullPointerException.class, () -> new Version("1.0.0").compareTo(null));
    }

    @Test
    void shouldReturnNegativeIntegerWhenFirstIsLessThanSecond() {
      assertThat(new Version("1.0.0").compareTo(new Version("2.0.0")), is(lessThan(0)));
      assertThat(new Version("0.1.0").compareTo(new Version("0.2.0")), is(lessThan(0)));
    }

    @Test
    void shouldReturnZeroWhenFirstIsEqualToSecond() {
      assertThat(new Version("1.0.0").compareTo(new Version("1.0.0")), is(0));
      assertThat(new Version("0.1.0").compareTo(new Version("0.1.0")), is(0));
      assertThat(new Version("0.0.1").compareTo(new Version("0.0.1")), is(0));
      assertThat(
          "Build number, 3rd digit and beyond is not significant",
          new Version("0.0.2").compareTo(new Version("0.0.1")),
          is(0));
    }

    @Test
    void shouldReturnPositiveIntegerWhenFirstIsGreaterThanSecond() {
      assertThat(new Version("2.0.0").compareTo(new Version("1.0.0")), is(greaterThan(0)));
      assertThat(new Version("0.2.0").compareTo(new Version("0.1.0")), is(greaterThan(0)));
    }
  }

  @Test
  void testIsGreaterThan() {
    assertFalse(new Version("1.0.0").isGreaterThan(new Version("2.0.0")));
    assertFalse(new Version("1.0.0").isGreaterThan(new Version("1.0.0")));
    assertTrue(new Version("2.0.0").isGreaterThan(new Version("1.0.0")));
  }

  @Test
  void testToString() {
    assertEquals("1.2.3", new Version("1.2.3").toString());
    assertEquals("1.2.0", new Version("1.2.0").toString());
    assertEquals("1.2.3", new Version("1.2.3").toString());
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
  }

  @ParameterizedTest
  @ValueSource(strings = {"1.2.3", "0.9.0", "1.2.0", "1.2.9", "1.0.3", "0.2.3"})
  void compatibleWithMapShouldReturnTrueWhenOtherVersionIsCompatible(final String versionValue) {
    assertThat(
        new Version("1.2.3").isCompatibleWithMapMinimumEngineVersion(new Version(versionValue)),
        is(true));
  }

  @ParameterizedTest
  @ValueSource(strings = {"1.9.3", "9.2.3"})
  void compatibleWithMapShouldReturnFalseWhenOtherVersionIsNotCompatible(
      final String versionValue) {
    assertThat(
        new Version("1.2.3").isCompatibleWithMapMinimumEngineVersion(new Version(versionValue)),
        is(false));
  }
}
