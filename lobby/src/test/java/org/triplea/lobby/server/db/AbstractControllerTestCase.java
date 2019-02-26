package org.triplea.lobby.server.db;

import org.triplea.lobby.server.config.TestLobbyConfigurations;
import org.triplea.test.common.Integration;

/**
 * Superclass for fixtures that test a DAO implementation.
 */
@Integration
public abstract class AbstractControllerTestCase {
  protected final Database database = new Database(TestLobbyConfigurations.INTEGRATION_TEST);

  protected AbstractControllerTestCase() {}
}
