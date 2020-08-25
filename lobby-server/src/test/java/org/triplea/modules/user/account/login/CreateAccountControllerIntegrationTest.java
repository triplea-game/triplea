package org.triplea.modules.user.account.login;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;

import java.net.URI;
import org.junit.jupiter.api.Test;
import org.triplea.http.client.lobby.login.CreateAccountResponse;
import org.triplea.http.client.lobby.login.LobbyLoginClient;
import org.triplea.java.Sha512Hasher;
import org.triplea.modules.http.BasicEndpointTest;

class CreateAccountControllerIntegrationTest extends BasicEndpointTest<LobbyLoginClient> {
  private static final String USERNAME = "user-name";
  private static final String EMAIL = "email@email.com";
  private static final String PASSWORD = Sha512Hasher.hashPasswordWithSalt("pass");

  private static final String USERNAME_1 = "user-name_1";
  private static final String EMAIL_1 = "email1@email.com";
  private static final String PASSWORD_1 = Sha512Hasher.hashPasswordWithSalt("pass_1");

  CreateAccountControllerIntegrationTest(final URI localhost) {
    super(localhost, LobbyLoginClient::newClient);
  }

  @Test
  void createAccount() {
    final CreateAccountResponse result =
        verifyEndpointReturningObject(client -> client.createAccount(USERNAME, EMAIL, PASSWORD));

    assertThat(result, is(CreateAccountResponse.SUCCESS_RESPONSE));
  }

  @Test
  void duplicateAccountCreateFails() {
    verifyEndpointReturningObject(client -> client.createAccount(USERNAME_1, EMAIL_1, PASSWORD_1));

    final CreateAccountResponse result =
        verifyEndpointReturningObject(
            client -> client.createAccount(USERNAME_1, EMAIL_1, PASSWORD_1));

    assertThat(result, is(not(CreateAccountResponse.SUCCESS_RESPONSE)));
  }
}
