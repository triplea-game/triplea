package org.triplea.server.moderator.toolbox.moderators;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.triplea.http.client.HttpInteractionException;
import org.triplea.http.client.moderator.toolbox.moderator.management.ToolboxModeratorManagementClient;
import org.triplea.server.http.AbstractDropwizardTest;

class ModeratorsControllerIntegrationTest extends AbstractDropwizardTest {
  private static final ToolboxModeratorManagementClient client =
      AbstractDropwizardTest.newClient(ToolboxModeratorManagementClient::newClient);

  private static final ToolboxModeratorManagementClient clientWithBadKey =
      AbstractDropwizardTest.newClientWithInvalidCreds(ToolboxModeratorManagementClient::newClient);

  @Test
  void checkUserExists() {}

  @Test
  void checkUserExistsNotAuthorized() {
    assertThrows(
        HttpInteractionException.class, () -> clientWithBadKey.checkUserExists("any username"));
  }

  @Test
  void getModerators() {}

  @Test
  void getModeratorsNotAuthorized() {
    assertThrows(HttpInteractionException.class, clientWithBadKey::fetchModeratorList);
  }

  @Test
  void isSuperMod() {
    assertThat(client.isCurrentUserSuperMod(), is(true));
  }

  @Test
  void isSuperModNotAuthorized() {
    assertThrows(HttpInteractionException.class, clientWithBadKey::isCurrentUserSuperMod);
  }

  @Test
  void generateSingleUseKey() {
    client.generateSingleUseKey("mod2");
  }

  @Test
  void generateSingleUseKeyNotAuthorized() {
    // user name exists in DB
    assertThrows(
        HttpInteractionException.class, () -> clientWithBadKey.generateSingleUseKey("test"));
  }

  @Test
  void removeMod() {
    client.removeMod("mod");
  }

  @Test
  void removeModNotAuthorized() {
    assertThrows(
        HttpInteractionException.class, () -> clientWithBadKey.generateSingleUseKey("mod2"));
  }

  @Test
  void setSuperMod() {
    client.addSuperMod("mod3");
  }

  @Test
  void setSuperModNotAuthorized() {
    assertThrows(HttpInteractionException.class, () -> clientWithBadKey.addSuperMod("mod2"));
  }
}
