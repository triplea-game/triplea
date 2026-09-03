package games.strategy.engine.data.changefactory;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.serializer.GameDataOracle;
import games.strategy.triplea.xml.TestMapGameData;
import org.junit.jupiter.api.Test;

class PlayerWhoAmIChangeSaverTest {

  private final GameData gameData = TestMapGameData.REVISED.getGameData();

  @Test
  void saverRoundTripsChange() {
    final GamePlayer player = gameData.getPlayerList().getPlayers().get(0);
    // package-private ctor (startWhoAmI, endWhoAmI, playerName); round-trip needs no live perform.
    final PlayerWhoAmIChange change =
        new PlayerWhoAmIChange("Human:Foo", "Human:Bar", player.getName());

    GameDataOracle.assertStateRoundTrips(new PlayerWhoAmIChangeSaver(), change, gameData);
  }
}
