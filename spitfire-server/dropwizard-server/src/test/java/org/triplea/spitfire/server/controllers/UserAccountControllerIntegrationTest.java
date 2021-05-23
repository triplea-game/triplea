package org.triplea.spitfire.server.controllers;

import com.github.database.rider.core.api.dataset.DataSet;
import java.net.URI;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.triplea.http.client.lobby.user.account.UserAccountClient;
import org.triplea.spitfire.server.AllowedUserRole;
import org.triplea.spitfire.server.ProtectedEndpointTest;
import org.triplea.spitfire.server.SpitfireServerTestExtension;

@SuppressWarnings("UnmatchedTest")
@Disabled
@DataSet(value = SpitfireServerTestExtension.LOBBY_USER_DATASET, useSequenceFiltering = false)
class UserAccountControllerIntegrationTest extends ProtectedEndpointTest<UserAccountClient> {

  UserAccountControllerIntegrationTest(final URI localhost) {
    super(localhost, AllowedUserRole.PLAYER, UserAccountClient::newClient);
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
