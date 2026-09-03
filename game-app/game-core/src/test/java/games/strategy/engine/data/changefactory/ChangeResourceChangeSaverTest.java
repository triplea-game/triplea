package games.strategy.engine.data.changefactory;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.serializer.GameDataOracle;
import games.strategy.triplea.xml.TestMapGameData;
import org.junit.jupiter.api.Test;

class ChangeResourceChangeSaverTest {

  private final GameData gameData = TestMapGameData.REVISED.getGameData();

  @Test
  void saverRoundTripsChange() {
    final Resource resource = gameData.getResourceList().getResources().iterator().next();
    final GamePlayer player = gameData.getPlayerList().getPlayers().get(0);
    final ChangeResourceChange change =
        (ChangeResourceChange) ChangeFactory.changeResourcesChange(player, resource, 7);

    GameDataOracle.assertStateRoundTrips(new ChangeResourceChangeSaver(), change, gameData);
  }
}
