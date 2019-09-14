package org.triplea.server.moderator.toolbox.moderators;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.triplea.http.client.moderator.toolbox.moderator.management.ToolboxModeratorManagementClient;
import org.triplea.server.http.AbstractDropwizardTest;

class ModeratorsControllerIntegrationTest extends AbstractDropwizardTest {
  private static final ToolboxModeratorManagementClient client =
      AbstractDropwizardTest.newClient(ToolboxModeratorManagementClient::newClient);

  // TODO: Project#12 re-enable test
  @Disabled
  @Test
  void isSuperMod() {
    assertThat(client.isCurrentUserSuperMod(), is(true));
  }

  // TODO: Project#12 re-enable test
  @Disabled
  @Test
  void removeMod() {
    client.removeMod("mod");
  }

  // TODO: Project#12 re-enable test
  @Disabled
  @Test
  void setSuperMod() {
    client.addSuperMod("mod3");
  }
}
