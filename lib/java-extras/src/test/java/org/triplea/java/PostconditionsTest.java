package org.triplea.java;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@SuppressWarnings("InnerClassMayBeStatic")
class PostconditionsTest {

  private static final String MESSAGE = "additional context and informational message";

  @Nested
  final class AssertState {
    @Test
    void positiveCase() {
      Postconditions.assertState(true);
    }

    @Test
    void assertionFails() {
      assertThrows(AssertionError.class, () -> Postconditions.assertState(false));
    }

    @Test
    void assertionFailsWithMessage() {
      final Throwable thrown =
          assertThrows(AssertionError.class, () -> Postconditions.assertState(false, MESSAGE));
      assertThat(thrown.getMessage(), StringContains.containsString(MESSAGE));
    }
  }

  @Nested
  final class AssertNotNull {
    @Test
    void positiveCase() {
      Postconditions.assertNotNull(new Object(), "no exception expected");
    }

    @Test
    void assertFailsWithMessage() {
      final Throwable thrown =
          assertThrows(AssertionError.class, () -> Postconditions.assertNotNull(null, MESSAGE));

      assertThat(thrown.getMessage(), StringContains.containsString(MESSAGE));
    }
  }
}
