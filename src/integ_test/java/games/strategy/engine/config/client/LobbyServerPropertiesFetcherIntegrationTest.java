package games.strategy.engine.config.client;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.jupiter.api.Test;

import games.strategy.triplea.settings.AbstractClientSettingTestCase;

public class LobbyServerPropertiesFetcherIntegrationTest extends AbstractClientSettingTestCase {
  @Test
  public void remoteLobbyUrlReaderWorks() {

    final LobbyServerPropertiesFetcher testObj = new LobbyServerPropertiesFetcher();

    assertThat("verify fetch to get current lobby properties",
        testObj.fetchLobbyServerProperties(), notNullValue());
  }
}
