package org.triplea.test.common;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.triplea.test.common.IsInstant.isInstant;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class IsInstantTest {

  @Test
  void sampleMatch() {
    assertThat(Instant.parse("2020-12-24T23:59:59Z"), isInstant(2020, 12, 24, 23, 59, 59));
  }

  @Test
  void verifyValuesAreZeroPadded() {
    assertThat(Instant.parse("2020-01-01T01:01:01Z"), isInstant(2020, 1, 1, 1, 1, 1));
  }

  @Test
  void verifyCanHandleZeros() {
    assertThat(Instant.parse("0000-01-01T00:00:00Z"), isInstant(0, 1, 1, 0, 0, 0));
  }

  @Test
  void verifyNegativeCaseWhereDoesNotMatch() {
    assertThrows(
        AssertionError.class,
        () -> assertThat(Instant.parse("1111-01-01T00:00:00Z"), isInstant(0, 1, 1, 0, 0, 0)));
  }
}
