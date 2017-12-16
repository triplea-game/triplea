package games.strategy.engine.data;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import games.strategy.triplea.Constants;
import games.strategy.triplea.xml.TestMapGameData;

public class AllianceTrackerTest {
  private GameData gameData;

  @BeforeEach
  public void setUp() throws Exception {
    gameData = TestMapGameData.TEST.getGameData();
  }

  @Test
  public void testAddAlliance() {
    final PlayerID bush = gameData.getPlayerList().getPlayerId("bush");
    final PlayerID castro = gameData.getPlayerList().getPlayerId("castro");
    final AllianceTracker allianceTracker = gameData.getAllianceTracker();
    final RelationshipTracker relationshipTracker = gameData.getRelationshipTracker();
    assertFalse(relationshipTracker.isAllied(bush, castro));
    // the alliance tracker now only keeps track of GUI elements like the stats panel alliance TUV totals, and does not
    // affect gameplay
    allianceTracker.addToAlliance(bush, "natp");
    // the relationship tracker is the one that keeps track of actual relationships between players, affecting gameplay.
    // Note that changing
    // the relationship between bush and castro, does not change the relationship between bush and chretian
    relationshipTracker.setRelationship(bush, castro,
        gameData.getRelationshipTypeList().getRelationshipType(Constants.RELATIONSHIP_TYPE_DEFAULT_ALLIED));
    assertTrue(relationshipTracker.isAllied(bush, castro));
  }

  // TODO create test suite for Alliance/Relationships/Politics
}
