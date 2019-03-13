package org.triplea.lobby.server.db;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.triplea.lobby.server.config.TestLobbyConfigurations;

final class BannedUsernameControllerIntegrationTest extends AbstractModeratorServiceControllerTestCase {
  private final PlayerNameBlackListDao controller =
      TestLobbyConfigurations.INTEGRATION_TEST.getDatabaseDao().getPlayerNameBlackListDao();

  @Test
  void testBanUsername() {
    assertFalse(isUsernameBanned());
    banUsername();
    assertTrue(isUsernameBanned());
  }

  private void banUsername() {
    controller.addName(user.getUsername(), moderator.getUsername());
  }

  private boolean isUsernameBanned() {
    return controller.isUsernameBanned(user.getUsername());
  }
}
