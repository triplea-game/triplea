package games.strategy.engine.data.changefactory;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.serializer.GameDataOracle;
import games.strategy.triplea.xml.TestMapGameData;
import org.junit.jupiter.api.Test;

class ProductionFrontierChangeSaverTest {

  private final GameData gameData = TestMapGameData.REVISED.getGameData();

  @Test
  void saverRoundTripsChange() {
    final GamePlayer player = gameData.getPlayerList().getPlayers().get(0);
    // package-private ctor (startFrontierName, endFrontierName, playerName).
    final ProductionFrontierChange change =
        new ProductionFrontierChange("frontierA", "frontierB", player.getName());

    GameDataOracle.assertStateRoundTrips(new ProductionFrontierChangeSaver(), change, gameData);
  }
}
