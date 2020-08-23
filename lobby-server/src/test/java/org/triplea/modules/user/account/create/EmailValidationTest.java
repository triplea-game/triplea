package org.triplea.modules.user.account.create;

import static com.github.npathai.hamcrestopt.OptionalMatchers.isEmpty;
import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresent;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

class EmailValidationTest {

  @Test
  void valid() {
    assertThat(new EmailValidation().apply("email@test.com"), isEmpty());
  }

  @Test
  void invalid() {
    assertThat(new EmailValidation().apply(null), isPresent());
    assertThat(new EmailValidation().apply(""), isPresent());
    assertThat(new EmailValidation().apply("invalid"), isPresent());
  }
}
