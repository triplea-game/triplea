package games.strategy.engine.lobby.client.login;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.jupiter.api.Test;
import org.triplea.test.common.Integration;

import games.strategy.triplea.settings.AbstractClientSettingTestCase;

@Integration
class LobbyServerPropertiesFetcherIntegrationTest extends AbstractClientSettingTestCase {
  @Test
  void remoteLobbyUrlReaderWorks() {

    final LobbyServerPropertiesFetcher testObj = new LobbyServerPropertiesFetcher();

    assertThat("verify fetch to get current lobby properties",
        testObj.fetchLobbyServerProperties(), notNullValue());
  }
}
