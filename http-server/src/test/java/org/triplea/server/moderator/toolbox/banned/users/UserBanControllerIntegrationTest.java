package org.triplea.server.moderator.toolbox.banned.users;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.IsNot.not;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.triplea.http.client.HttpInteractionException;
import org.triplea.http.client.moderator.toolbox.banned.user.ToolboxUserBanClient;
import org.triplea.http.client.moderator.toolbox.banned.user.UserBanParams;
import org.triplea.server.http.AbstractDropwizardTest;

class UserBanControllerIntegrationTest extends AbstractDropwizardTest {

  private static final ToolboxUserBanClient client =
      AbstractDropwizardTest.newClient(ToolboxUserBanClient::newClient);

  private static final ToolboxUserBanClient clientWithBadKey =
      AbstractDropwizardTest.newClientWithInvalidCreds(ToolboxUserBanClient::newClient);

  @Test
  void getUserBans() {
    assertThat(client.getUserBans(), not(empty()));
  }

  @Test
  void getUserBansNotAuthorized() {
    assertThrows(HttpInteractionException.class, clientWithBadKey::getUserBans);
  }

  @Test
  void removeUserBan() {
    client.removeUserBan("xyz");
  }

  @Test
  void removeUserBanNotAuthorized() {
    // ban id exists, but because keys are bad we'll get an exception"
    assertThrows(HttpInteractionException.class, () -> clientWithBadKey.removeUserBan("123"));
  }

  @Test
  void banUser() {
    client.banUser(
        UserBanParams.builder()
            .hoursToBan(10)
            .hashedMac("$1$AA$AA7qDBliIofq8jOm4nMBB/")
            .ip("2.2.2.2")
            .username("name")
            .build());
  }

  @Test
  void banUserNotAuthorized() {
    assertThrows(
        HttpInteractionException.class,
        () ->
            clientWithBadKey.banUser(
                UserBanParams.builder()
                    .hoursToBan(10)
                    .hashedMac("$1$AA$AA7qDBliIofq8jOm4nMCC/")
                    .ip("2.2.2.3")
                    .username("name")
                    .build()));
  }
}
