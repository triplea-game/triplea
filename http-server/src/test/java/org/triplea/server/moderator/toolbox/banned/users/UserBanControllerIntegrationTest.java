package org.triplea.server.moderator.toolbox.banned.users;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.IsNot.not;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.triplea.http.client.moderator.toolbox.banned.user.ToolboxUserBanClient;
import org.triplea.http.client.moderator.toolbox.banned.user.UserBanParams;
import org.triplea.server.http.AbstractDropwizardTest;

class UserBanControllerIntegrationTest extends AbstractDropwizardTest {

  private static final ToolboxUserBanClient client =
      AbstractDropwizardTest.newClient(ToolboxUserBanClient::newClient);

  @Test
  void getUserBans() {
    assertThat(client.getUserBans(), not(empty()));
  }

  // TODO: Project#12 re-enable test
  @Disabled
  @Test
  void removeUserBan() {
    client.removeUserBan("xyz");
  }

  // TODO: Project#12 re-enable test
  @Disabled
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
}
