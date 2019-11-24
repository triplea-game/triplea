package org.triplea.server.user.account.login;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;

import org.junit.jupiter.api.Test;
import org.triplea.http.client.lobby.login.LobbyLoginClient;
import org.triplea.http.client.lobby.login.LobbyLoginResponse;
import org.triplea.server.http.BasicEndpointTest;

class LoginControllerIntegrationTest extends BasicEndpointTest<LobbyLoginClient> {

  private static final String USERNAME = "player";

  // TODO: Md5-Deprecation - SHA512 hash these passwords
  private static final String PASSWORD = "password";
  private static final String TEMP_PASSWORD = "temp-password";
  private static final String INVALID_PASSWORD = "invalid";

  private static final String LEGACY_PASSWORD_USER = "legacy-password-user";
  private static final String LEGACY_PASSWORD = "legacy";

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

  @Test
  void legacyLogin() {
    LobbyLoginResponse response =
        verifyEndpointReturningObject(
            client -> client.login(LEGACY_PASSWORD_USER, LEGACY_PASSWORD));

    assertThat(response.getFailReason(), nullValue());
    assertThat(response.getApiKey(), notNullValue());

    // second login should behind the scenes do a password upgrade, transparent to the client.
    response =
        verifyEndpointReturningObject(
            client -> client.login(LEGACY_PASSWORD_USER, LEGACY_PASSWORD));
    assertThat(response.getFailReason(), nullValue());
    assertThat(response.getApiKey(), notNullValue());
  }
}
