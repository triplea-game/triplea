package games.strategy.engine.data.changefactory;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.ProductionFrontier;
import games.strategy.engine.data.serializer.GameDataOracle;
import games.strategy.triplea.xml.TestMapGameData;
import org.junit.jupiter.api.Test;

class RemoveProductionRuleSaverTest {

  private final GameData gameData = TestMapGameData.REVISED.getGameData();

  @Test
  void saverRoundTripsChange() {
    final String frontierName =
        gameData.getProductionFrontierList().getProductionFrontierNames().iterator().next();
    final ProductionFrontier frontier =
        gameData.getProductionFrontierList().getProductionFrontier(frontierName);
    final RemoveProductionRule change =
        new RemoveProductionRule(frontier.getRules().get(0), frontier);

    GameDataOracle.assertStateRoundTrips(new RemoveProductionRuleSaver(), change, gameData);
  }
}
