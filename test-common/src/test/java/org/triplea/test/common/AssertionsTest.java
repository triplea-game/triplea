package org.triplea.test.common;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.regex.Pattern;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

final class AssertionsTest {
  @Nested
  final class AssertNotThrowsTest {
    @Test
    void shouldPassWhenExecutableDoesNotThrow() {
      try {
        Assertions.assertNotThrows(() -> {
        });
      } catch (final AssertionFailedError e) {
        fail("expected assertion success but was failure", e);
      }
    }

    @Test
    void shouldFailWhenExecutableThrowsExceptionWithCanonicalName() {
      final Throwable t = assertThrows(AssertionFailedError.class, () -> {
        Assertions.assertNotThrows(() -> {
          throw new RuntimeException();
        });
      });
      assertThat(t.getMessage(), is("Expected no exception to be thrown, but java.lang.RuntimeException was thrown"));
    }

    @Test
    void shouldFailWhenExecutableThrowsExceptionWithoutCanonicalName() {
      final Throwable t = assertThrows(AssertionFailedError.class, () -> {
        Assertions.assertNotThrows(() -> {
          // Anonymous classes have no canonical name
          throw new RuntimeException() {
            private static final long serialVersionUID = 1L;
          };
        });
      });
      assertThat(
          t.getMessage(),
          matchesPattern("^Expected no exception to be thrown, but "
              + Pattern.quote(AssertNotThrowsTest.class.getName())
              + "\\$\\d+ was thrown$"));
    }
  }
}
