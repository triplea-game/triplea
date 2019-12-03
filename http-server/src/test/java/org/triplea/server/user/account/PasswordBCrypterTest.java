package org.triplea.server.user.account;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.StringStartsWith.startsWith;

import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;

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
        BCrypt.checkpw(PasswordBCrypter.hashPasswordWithSalt("password"), crypted);

    assertThat(
        "Verify BCrypt to match a plaintext password against a crypted password", result, is(true));
  }
}
