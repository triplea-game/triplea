package games.strategy.engine.config.client;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.Test;

public class GameEnginePropertyReaderIntegrationTest {
  @Test
  public void remoteLobbyUrlReaderWorks() {
    final GameEnginePropertyReader testObj = new GameEnginePropertyReader();

    assertThat("verify fetch to get current lobby properties",
        testObj.fetchLobbyServerProperties(), notNullValue());
    assertThat(testObj.getEngineVersion(), notNullValue());
    assertThat(testObj.getMapListingSource(), notNullValue());
  }
}
