package org.triplea.modules.user.account.create;

import static com.github.npathai.hamcrestopt.OptionalMatchers.isEmpty;
import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresent;
import static org.hamcrest.MatcherAssert.assertThat;

import com.google.common.base.Strings;
import org.junit.jupiter.api.Test;

class PasswordValidationTest {

  private static final String VALID = Strings.repeat("a", PasswordValidation.MIN_LENGTH);
  private static final String INVALID = Strings.repeat("a", PasswordValidation.MIN_LENGTH - 1);

  private final PasswordValidation passwordValidation = new PasswordValidation();

  @Test
  void valid() {
    assertThat(passwordValidation.apply(VALID), isEmpty());
  }

  @Test
  void invalid() {
    assertThat(passwordValidation.apply(INVALID), isPresent());
  }
}
