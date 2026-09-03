package games.strategy.engine.data.changefactory;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.ProductionFrontier;
import games.strategy.engine.data.serializer.GameDataOracle;
import games.strategy.triplea.xml.TestMapGameData;
import org.junit.jupiter.api.Test;

class AddProductionRuleSaverTest {

  private final GameData gameData = TestMapGameData.REVISED.getGameData();

  @Test
  void saverRoundTripsChange() {
    final String frontierName =
        gameData.getProductionFrontierList().getProductionFrontierNames().iterator().next();
    final ProductionFrontier frontier =
        gameData.getProductionFrontierList().getProductionFrontier(frontierName);
    final AddProductionRule change = new AddProductionRule(frontier.getRules().get(0), frontier);

    GameDataOracle.assertStateRoundTrips(new AddProductionRuleSaver(), change, gameData);
  }
}
