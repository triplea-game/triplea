package org.triplea.server.user.account;

import org.junit.jupiter.api.Test;
import org.triplea.http.client.lobby.user.account.UserAccountClient;
import org.triplea.server.http.AuthenticatedEndpointTest;

class UserAccountControllerIntegrationTest extends AuthenticatedEndpointTest<UserAccountClient> {

  UserAccountControllerIntegrationTest() {
    super(UserAccountClient::newClient);
  }

  @Test
  void changePassword() {
    verifyEndpointReturningVoid(client -> client.changePassword("password"));
  }

  @Test
  void fetchEmail() {
    verifyEndpointReturningObject(UserAccountClient::fetchEmail);
  }

  @Test
  void changeEmail() {
    verifyEndpointReturningVoid(client -> client.changeEmail("email@"));
  }
}
