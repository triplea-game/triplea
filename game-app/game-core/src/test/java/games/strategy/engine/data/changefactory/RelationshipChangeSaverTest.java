package games.strategy.engine.data.changefactory;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.RelationshipType;
import games.strategy.engine.data.serializer.GameDataOracle;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.List;
import org.junit.jupiter.api.Test;

class RelationshipChangeSaverTest {

  private final GameData gameData = TestMapGameData.REVISED.getGameData();

  @Test
  void saverRoundTripsChange() {
    final List<GamePlayer> players = gameData.getPlayerList().getPlayers();
    final RelationshipType war = gameData.getRelationshipTypeList().getDefaultWarRelationship();
    final RelationshipType allied =
        gameData.getRelationshipTypeList().getDefaultAlliedRelationship();
    final RelationshipChange change =
        (RelationshipChange)
            ChangeFactory.relationshipChange(players.get(0), players.get(1), war, allied);

    GameDataOracle.assertStateRoundTrips(new RelationshipChangeSaver(), change, gameData);
  }
}
