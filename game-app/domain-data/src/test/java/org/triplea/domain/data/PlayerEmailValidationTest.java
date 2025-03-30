package org.triplea.domain.data;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PlayerEmailValidationTest {

  @ParameterizedTest
  @ValueSource(strings = {"some@some.com", "some.someMore@some.com", "some@some.some.some.com"})
  void shouldReturnTrueWhenAddressIsValid(final String validEmail) {
    assertThat(
        "'" + validEmail + "' should be valid",
        PlayerEmailValidation.isValid(validEmail),
        is(true));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "some@some.com some2@some2.com",
        "some@some.com some2@some2.co.uk",
        "some@some.com some2@some2.co.br",
        // long but valid mail
        "12345678901234567890123456789012345678901234567890123456789012345678901234567890"
            + "12345678901234567890123456789012345678901234567890123456789012345678901234567890"
            + "1234567890@some.com "
            + "12345678901234567890123456789012345678901234567890123456789012345678901234567890"
            + "12345678901234567890123456789012345678901234567890123456789012345678901234567890"
            + "@some.com"
      })
  void shouldReturnTrueWhenAddressesAreValid(final String validEmails) {
    assertThat(
        "'" + validEmails + "' should be valid",
        PlayerEmailValidation.areValid(validEmails),
        is(true));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "",
        "test",
        "test@",
        "test.com",
        "test@a.",
        "@a.com",
        "some@some.com,",
        "some@some.com,some",
        "1234567890",
        // too long mail (4x80 + 9 = 329)
        "12345678901234567890123456789012345678901234567890123456789012345678901234567890"
            + "12345678901234567890123456789012345678901234567890123456789012345678901234567890"
            + "12345678901234567890123456789012345678901234567890123456789012345678901234567890"
            + "12345678901234567890123456789012345678901234567890123456789012345678901234567890@some.com"
      })
  void shouldReturnFalseWhenAddressIsInvalid(final String invalidEmail) {
    assertThat(
        "'" + invalidEmail + "' should be invalid",
        PlayerEmailValidation.isValid(invalidEmail),
        is(false));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "some@some.com test",
        "test@ some@some.com",
      })
  void shouldReturnFalseWhenAddressesAreInvalid(final String invalidEmails) {
    assertThat(
        "'" + invalidEmails + "' should be invalid",
        PlayerEmailValidation.areValid(invalidEmails),
        is(false));
  }
}
