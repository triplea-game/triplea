package org.triplea.modules.forgot.password;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PasswordGeneratorTest {

  @Test
  void verifyPasswordLength() {
    assertThat(
        new PasswordGenerator().generatePassword().length(), is(PasswordGenerator.PASSWORD_LENGTH));
  }

  /**
   * Generate a password, check we do not have it in a set, add the password to the set and repeat
   * many times.
   */
  @Test
  void verifyPasswordChanges() {
    final var passwordGenerator = new PasswordGenerator();

    final Set<String> strings = new HashSet<>();
    for (int i = 0; i < 1000; i++) {
      final String generated = passwordGenerator.generatePassword();
      assertThat(
          "If this test fails, DO NOT IGNORE IT! Instead increase the length of the generated "
              + "temporary password",
          strings.contains(generated),
          is(false));
      strings.add(generated);
    }
  }
}
