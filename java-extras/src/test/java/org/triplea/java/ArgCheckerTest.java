package org.triplea.java;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ArgCheckerTest {

  @Nested
  final class CheckNotEmptyTest {
    @Test
    void notEmptyArgCases() {
      ArgChecker.checkNotEmpty("not empty");
      ArgChecker.checkNotEmpty("a");
      ArgChecker.checkNotEmpty(" - ");
    }

    @Test
    void emptyArgCases() {
      asList("", null, "  ", "\n", "\t", "  \n")
          .forEach(
              emptyArg ->
                  assertThrows(
                      IllegalArgumentException.class,
                      () -> ArgChecker.checkNotEmpty(emptyArg),
                      "expecting empty arg to trigger arg check to throw illegal arg exception: "
                          + emptyArg));
    }
  }
}
