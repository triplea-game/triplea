package org.triplea.modules.user.account.login.authorizer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

class BCryptHashVerifierTest {

  private static final String PASSWORD = "password";
  private static final String BCRYPTED_PASSWORD =
      "$2a$10$RSkV60Ky7F7.ybGmiOEUcO/ynTyUZlLqSoXSQtliSrpFf7/WEe3QO";

  private final BCryptHashVerifier bcryptHashVerifier = new BCryptHashVerifier();

  @Test
  void incorrectPassword() {
    final boolean result = bcryptHashVerifier.test("incorrect", BCRYPTED_PASSWORD);

    assertThat(result, is(false));
  }

  @Test
  void correctPassword() {
    final boolean result = bcryptHashVerifier.test(PASSWORD, BCRYPTED_PASSWORD);

    assertThat(result, is(true));
  }
}
