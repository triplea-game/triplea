package org.triplea.spitfire.server.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;

import java.net.URI;
import org.junit.jupiter.api.Test;
import org.triplea.http.client.lobby.login.CreateAccountResponse;
import org.triplea.http.client.lobby.login.LobbyLoginClient;
import org.triplea.java.Sha512Hasher;
import org.triplea.spitfire.server.ControllerIntegrationTest;

class CreateAccountControllerIntegrationTest extends ControllerIntegrationTest {
  private static final String USERNAME = "user-name";
  private static final String EMAIL = "email@email.com";
  private static final String PASSWORD = Sha512Hasher.hashPasswordWithSalt("pass");

  private static final String USERNAME_1 = "user-name_1";
  private static final String EMAIL_1 = "email1@email.com";
  private static final String PASSWORD_1 = Sha512Hasher.hashPasswordWithSalt("pass_1");

  private final LobbyLoginClient client;

  CreateAccountControllerIntegrationTest(final URI localhost) {
    this.client = LobbyLoginClient.newClient(localhost);
  }

  @Test
  void badRequests() {
    assertBadRequest(() -> client.login(null, null));
    assertBadRequest(() -> client.createAccount(null, null, null));
    assertBadRequest(() -> client.createAccount("user", "email@email.com", null));
    assertBadRequest(() -> client.createAccount("user", null, "password"));
    assertBadRequest(() -> client.createAccount(null, "email@email.com", "password"));
  }

  @Test
  void createAccountAndDoLogin() {
    final CreateAccountResponse result = client.createAccount(USERNAME, EMAIL, PASSWORD);
    assertThat(result, is(CreateAccountResponse.SUCCESS_RESPONSE));

    // verify login with the new account
    final var loginResponse = client.login(USERNAME, PASSWORD);
    assertThat(loginResponse.isSuccess(), is(true));
  }

  @Test
  void duplicateAccountCreateFails() {
    client.createAccount(USERNAME_1, EMAIL_1, PASSWORD_1);

    final CreateAccountResponse result = client.createAccount(USERNAME_1, EMAIL_1, PASSWORD_1);

    assertThat(result, is(not(CreateAccountResponse.SUCCESS_RESPONSE)));
  }
}
