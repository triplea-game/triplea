package org.triplea.domain.data;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PlayerEmailValidationTest {

  @ParameterizedTest
  @ValueSource(
      strings = {
        "some@some.com",
        "some.someMore@some.com",
        "some@some.com some2@some2.com",
        "some@some.com some2@some2.co.uk",
        "some@some.com some2@some2.co.br",
        "",
        "some@some.some.some.com"
      })
  void shouldReturnTrueWhenAddressIsValid(final String validEmail) {
    assertThat(
        "'" + validEmail + "' should be valid",
        PlayerEmailValidation.isValid(validEmail),
        is(true));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "test",
        "test@",
        "test.com",
        "test@a.",
        "@a.com",
        "some@some.com,",
        "some@some.com,some",
      })
  void shouldReturnFalseWhenAddressIsInvalid(final String invalidEmail) {
    assertThat(
        "'" + invalidEmail + "' should be invalid",
        PlayerEmailValidation.isValid(invalidEmail),
        is(false));
  }
}
