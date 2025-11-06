package games.strategy.engine.data;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import games.strategy.triplea.Constants;
import games.strategy.triplea.xml.TestMapGameData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class AllianceTrackerTest {
  private final GameData gameData = TestMapGameData.TEST.getGameData();

  @Test
  void testAddAlliance() {
    final String allianceName = "natp";
    final GamePlayer bush = gameData.getPlayerList().getPlayerId("bush");
    final GamePlayer castro = gameData.getPlayerList().getPlayerId("castro");
    final GamePlayer chretian = gameData.getPlayerList().getPlayerId("chretian");
    final AllianceTracker allianceTracker = gameData.getAllianceTracker();
    final RelationshipTracker relationshipTracker = gameData.getRelationshipTracker();
    // Validate that the expected setup for the alliance is what we expect from the data
    assertFalse(relationshipTracker.isAllied(bush, castro));
    assertFalse(relationshipTracker.isAllied(bush, chretian));
    assertTrue(relationshipTracker.isAllied(castro, chretian));

    assertTrue(allianceTracker.getAllies(bush).isEmpty());
    assertTrue(allianceTracker.getAllies(castro).contains(chretian));
    assertTrue(allianceTracker.getAllies(chretian).contains(castro));

    // the alliance tracker now only keeps track of GUI elements like the stats panel alliance TUV
    // totals, and does not
    // affect gameplay
    allianceTracker.addToAlliance(bush, allianceName);
    assertFalse(relationshipTracker.isAllied(bush, castro));
    assertFalse(relationshipTracker.isAllied(bush, chretian));
    assertTrue(relationshipTracker.isAllied(castro, chretian));
    assertTrue(allianceTracker.getAllies(bush).contains(castro));
    assertTrue(allianceTracker.getAllies(bush).contains(chretian));
    // the relationship tracker is the one that keeps track of actual relationships between players,
    // affecting gameplay.
    // Note that changing
    // the relationship between bush and castro, does not change the relationship between bush and
    // chretian
    relationshipTracker.setRelationship(
        bush,
        castro,
        gameData
            .getRelationshipTypeList()
            .getRelationshipType(Constants.RELATIONSHIP_TYPE_DEFAULT_ALLIED));
    assertTrue(relationshipTracker.isAllied(bush, castro));
    assertFalse(relationshipTracker.isAllied(bush, chretian));
    assertTrue(relationshipTracker.isAllied(castro, chretian));
  }

  @Nested
  class GetPlayersInAlliance {
    @Test
    @DisplayName(
        "Get Players In Alliance Should Differentiate Alliance Names That Are "
            + "Substrings Of Other Alliance Names")
    void differentiatsAllianceNamesThatAreSubstrings() {
      final GamePlayer player1 = new GamePlayer("Player1", gameData);
      final GamePlayer player2 = new GamePlayer("Player2", gameData);
      final GamePlayer player3 = new GamePlayer("Player3", gameData);
      final GamePlayer player4 = new GamePlayer("Player4", gameData);
      final String alliance1Name = "Alliance";
      final String alliance2Name = "Anti" + alliance1Name;
      final AllianceTracker allianceTracker =
          new AllianceTracker(
              ImmutableMultimap.<GamePlayer, String>builder()
                  .put(player1, alliance1Name)
                  .put(player2, alliance1Name)
                  .put(player3, alliance2Name)
                  .put(player4, alliance2Name)
                  .build());

      assertThat(
          allianceTracker.getPlayersInAlliance(alliance1Name),
          is(ImmutableSet.of(player1, player2)));
      assertThat(
          allianceTracker.getPlayersInAlliance(alliance2Name),
          is(ImmutableSet.of(player3, player4)));
    }
  }
}
