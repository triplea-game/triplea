package org.triplea.server.user.account.login;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;

import org.junit.jupiter.api.Test;
import org.triplea.http.client.lobby.login.CreateAccountResponse;
import org.triplea.http.client.lobby.login.LobbyLoginClient;
import org.triplea.lobby.common.login.RsaAuthenticator;
import org.triplea.server.http.BasicEndpointTest;

class CreateAccountControllerIntegrationTest extends BasicEndpointTest<LobbyLoginClient> {
  private static final String USERNAME = "user-name";
  private static final String EMAIL = "email@email.com";
  private static final String PASSWORD = RsaAuthenticator.hashPasswordWithSalt("pass");

  private static final String USERNAME_1 = "user-name_1";
  private static final String EMAIL_1 = "email1@email.com";
  private static final String PASSWORD_1 = RsaAuthenticator.hashPasswordWithSalt("pass_1");

  CreateAccountControllerIntegrationTest() {
    super(LobbyLoginClient::newClient);
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
