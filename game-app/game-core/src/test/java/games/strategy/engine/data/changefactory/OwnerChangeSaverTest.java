package games.strategy.engine.data.changefactory;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.serializer.GameDataOracle;
import games.strategy.triplea.xml.TestMapGameData;
import org.junit.jupiter.api.Test;

/**
 * Validates the {@code Change} text-contract approach (reflective access to package-private, {@code
 * private final}-field Change subclasses) on a real {@code OwnerChange} built through {@link
 * ChangeFactory}.
 */
class OwnerChangeSaverTest {

  private final GameData gameData = TestMapGameData.REVISED.getGameData();

  @Test
  void saverRoundTripsChange() {
    final Territory territory = gameData.getMap().getTerritories().get(0);
    final GamePlayer player = gameData.getPlayerList().getPlayers().get(0);
    final OwnerChange change = (OwnerChange) ChangeFactory.changeOwner(territory, player);

    GameDataOracle.assertStateRoundTrips(new OwnerChangeSaver(), change, gameData);
  }
}
