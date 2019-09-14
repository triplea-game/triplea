package org.triplea.server.moderator.toolbox.banned.names;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.IsNot.not;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.triplea.http.client.moderator.toolbox.banned.name.ToolboxUsernameBanClient;
import org.triplea.server.http.AbstractDropwizardTest;

class UsernameBanControllerIntegrationTest extends AbstractDropwizardTest {

  private static final ToolboxUsernameBanClient client =
      AbstractDropwizardTest.newClient(ToolboxUsernameBanClient::newClient);

  // TODO: Project#12 re-enable test
  @Disabled
  @Test
  void removeBannedUsername() {
    client.removeUsernameBan("not nice");
  }

  // TODO: Project#12 re-enable test
  @Disabled
  @Test
  void addBannedUsername() {
    client.addUsernameBan("new bad name");
  }

  @Test
  void getBannedUsernames() {
    assertThat(client.getUsernameBans(), not(empty()));
  }
}
