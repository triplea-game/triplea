package org.triplea.server.user.account;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.StringStartsWith.startsWith;

import at.favre.lib.crypto.bcrypt.BCrypt;
import at.favre.lib.crypto.bcrypt.LongPasswordStrategies;
import org.junit.jupiter.api.Test;

class PasswordBCrypterTest {

  @Test
  void bcrypt() {
    final String cryptedResult = new PasswordBCrypter().apply("password");

    assertThat("Simple check to ensure we can invoke the library", cryptedResult, notNullValue());

    assertThat(
        " Bcrypt hashes have a specific starting sequence", cryptedResult, startsWith("$2a$"));

    assertThat(" Bcrypt hashes have a specific length", cryptedResult.length(), is(60));
  }

  @Test
  void bcryptHashAndPasswordVerification() {
    final String crypted = new PasswordBCrypter().apply("password");

    final boolean result =
        BCrypt.verifyer(BCrypt.Version.VERSION_2A, LongPasswordStrategies.none())
            .verify(
                PasswordBCrypter.hashPasswordWithSalt("password").toCharArray(),
                crypted.toCharArray())
            .verified;

    assertThat(
        "Verify BCrypt to match a plaintext password against a crypted password", result, is(true));
  }
}
