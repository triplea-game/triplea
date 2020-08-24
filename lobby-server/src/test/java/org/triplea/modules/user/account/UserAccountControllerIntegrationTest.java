package org.triplea.modules.user.account;

import org.junit.jupiter.api.Test;
import org.triplea.http.client.lobby.user.account.UserAccountClient;
import org.triplea.modules.http.AllowedUserRole;
import org.triplea.modules.http.ProtectedEndpointTest;

class UserAccountControllerIntegrationTest extends ProtectedEndpointTest<UserAccountClient> {

  UserAccountControllerIntegrationTest() {
    super(AllowedUserRole.PLAYER, UserAccountClient::newClient);
  }

  @Test
  void changePassword() {
    verifyEndpoint(client -> client.changePassword("password"));
  }

  @Test
  void fetchEmail() {
    verifyEndpoint(UserAccountClient::fetchEmail);
  }

  @Test
  void changeEmail() {
    verifyEndpoint(client -> client.changeEmail("email@"));
  }
}
