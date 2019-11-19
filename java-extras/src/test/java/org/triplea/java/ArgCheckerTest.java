package org.triplea.java;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ArgCheckerTest {

  @ParameterizedTest
  @ValueSource(strings = {"not empty", "a", " - "})
  void notEmptyArgCases(final String notEmpty) {
    ArgChecker.checkNotEmpty(notEmpty);
  }

  @Test
  void nullArgCase() {
    assertThrows(NullPointerException.class, () -> ArgChecker.checkNotEmpty(null));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "  ", "\n", "\t", "  \n"})
  void emptyArgCases(final String emptyArg) {
    assertThrows(IllegalArgumentException.class, () -> ArgChecker.checkNotEmpty(emptyArg));
  }
}
