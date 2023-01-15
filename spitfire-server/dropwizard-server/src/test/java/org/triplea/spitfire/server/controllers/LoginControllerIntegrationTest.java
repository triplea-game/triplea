package org.triplea.spitfire.server.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;

import java.net.URI;
import org.junit.jupiter.api.Test;
import org.triplea.http.client.lobby.login.LobbyLoginClient;
import org.triplea.http.client.lobby.login.LobbyLoginResponse;
import org.triplea.java.Sha512Hasher;
import org.triplea.spitfire.server.ControllerIntegrationTest;

class LoginControllerIntegrationTest extends ControllerIntegrationTest {
  private static final String USERNAME = "player";
  private static final String PASSWORD = Sha512Hasher.hashPasswordWithSalt("password");
  private static final String TEMP_PASSWORD = Sha512Hasher.hashPasswordWithSalt("temp-password");
  private static final String INVALID_PASSWORD = "invalid";

  private final LobbyLoginClient client;

  LoginControllerIntegrationTest(final URI localhost) {
    client = LobbyLoginClient.newClient(localhost);
  }

  @Test
  void invalidLogin() {
    final LobbyLoginResponse response = client.login(USERNAME, INVALID_PASSWORD);

    assertThat(response.getFailReason(), notNullValue());
    assertThat(response.getApiKey(), nullValue());
    assertThat(response.isPasswordChangeRequired(), is(false));
  }

  @Test
  void validLogin() {
    final LobbyLoginResponse response = client.login(USERNAME, PASSWORD);

    assertThat(response.getFailReason(), nullValue());
    assertThat(response.getApiKey(), notNullValue());
    assertThat(response.isPasswordChangeRequired(), is(false));
  }

  @Test
  void tempPasswordLogin() {
    final LobbyLoginResponse response = client.login(USERNAME, TEMP_PASSWORD);

    assertThat(response.getFailReason(), nullValue());
    assertThat(response.getApiKey(), notNullValue());
    assertThat(response.isPasswordChangeRequired(), is(true));
  }
}
