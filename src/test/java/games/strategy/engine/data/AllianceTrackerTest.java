package games.strategy.engine.data;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import games.strategy.triplea.Constants;
import games.strategy.triplea.xml.TestMapGameData;

public class AllianceTrackerTest {
  private GameData gameData;

  @Before
  public void setUp() throws Exception {
    gameData = TestMapGameData.TEST.getGameData();
  }

  @Test
  public void testAddAlliance() throws Exception {
    final PlayerID bush = gameData.getPlayerList().getPlayerID("bush");
    final PlayerID castro = gameData.getPlayerList().getPlayerID("castro");
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
