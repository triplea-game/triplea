package org.triplea.test.common.assertions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Optional;

import org.junit.jupiter.api.Test;

class OptionalsTest {

  @Test
  void isMissing() {
    assertThat(
        Optional.empty(),
        Optionals.isMissing());
  }

  @Test
  void isMissingThrowingCase() {
    assertThrows(
        AssertionError.class,
        () -> assertThat(
            Optional.of("value"),
            Optionals.isMissing()));
  }

  @Test
  void isPresent() {
    assertThat(
        Optional.of("value"),
        Optionals.isPresent());
  }

  @Test
  void isPresentThrowingCase() {
    assertThrows(
        AssertionError.class,
        () -> assertThat(
            Optional.empty(),
            Optionals.isPresent()));
  }
}
