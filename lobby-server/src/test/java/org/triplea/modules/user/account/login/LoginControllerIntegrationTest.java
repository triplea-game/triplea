package org.triplea.modules.user.account.login;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;

import org.junit.jupiter.api.Test;
import org.triplea.http.client.lobby.login.LobbyLoginClient;
import org.triplea.http.client.lobby.login.LobbyLoginResponse;
import org.triplea.java.Sha512Hasher;
import org.triplea.modules.http.BasicEndpointTest;

class LoginControllerIntegrationTest extends BasicEndpointTest<LobbyLoginClient> {

  private static final String USERNAME = "player";

  private static final String PASSWORD = Sha512Hasher.hashPasswordWithSalt("password");
  private static final String TEMP_PASSWORD = Sha512Hasher.hashPasswordWithSalt("temp-password");
  private static final String INVALID_PASSWORD = "invalid";

  LoginControllerIntegrationTest() {
    super(LobbyLoginClient::newClient);
  }

  @Test
  void invalidLogin() {
    final LobbyLoginResponse response =
        verifyEndpointReturningObject(client -> client.login(USERNAME, INVALID_PASSWORD));

    assertThat(response.getFailReason(), notNullValue());
    assertThat(response.getApiKey(), nullValue());
    assertThat(response.isPasswordChangeRequired(), is(false));
  }

  @Test
  void validLogin() {
    final LobbyLoginResponse response =
        verifyEndpointReturningObject(client -> client.login(USERNAME, PASSWORD));

    assertThat(response.getFailReason(), nullValue());
    assertThat(response.getApiKey(), notNullValue());
    assertThat(response.isPasswordChangeRequired(), is(false));
  }

  @Test
  void tempPasswordLogin() {
    final LobbyLoginResponse response =
        verifyEndpointReturningObject(client -> client.login(USERNAME, TEMP_PASSWORD));

    assertThat(response.getFailReason(), nullValue());
    assertThat(response.getApiKey(), notNullValue());
    assertThat(response.isPasswordChangeRequired(), is(true));
  }
}
