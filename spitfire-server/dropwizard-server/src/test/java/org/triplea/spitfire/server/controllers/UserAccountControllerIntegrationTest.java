package org.triplea.spitfire.server.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;

import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.triplea.http.client.lobby.user.account.UserAccountClient;
import org.triplea.spitfire.server.ControllerIntegrationTest;

class UserAccountControllerIntegrationTest extends ControllerIntegrationTest {
  private final URI localhost;
  private final UserAccountClient client;

  UserAccountControllerIntegrationTest(final URI localhost) {
    this.localhost = localhost;
    client = UserAccountClient.newClient(localhost, ControllerIntegrationTest.PLAYER);
  }

  @SuppressWarnings("unchecked")
  @Test
  void mustBeAuthorized() {
    assertNotAuthorized(
        List.of(ControllerIntegrationTest.ANONYMOUS),
        apiKey -> UserAccountClient.newClient(localhost, apiKey),
        UserAccountClient::fetchEmail,
        client -> client.changeEmail("new-email"),
        client -> client.changePassword("new-password"));
  }

  @Test
  void changePassword() {
    client.changePassword("password");
  }

  @Test
  void fetchEmail() {
    assertThat(client.fetchEmail(), notNullValue());
  }

  @Test
  void changeEmail() {
    assertThat(client.fetchEmail(), is(not("email@email-test.com")));

    client.changeEmail("email@email-test.com");

    assertThat(client.fetchEmail().getUserEmail(), is("email@email-test.com"));
  }
}
