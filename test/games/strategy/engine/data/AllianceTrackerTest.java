package games.strategy.engine.data;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Before;
import org.junit.Test;

import games.strategy.triplea.Constants;

public class AllianceTrackerTest {
  private GameData m_data;

  @Before
  public void setUp() throws Exception {
    // get the xml file
    final URL url = this.getClass().getResource("Test.xml");
    final InputStream input = url.openStream();
    m_data = (new GameParser()).parse(input, new AtomicReference<>(), false);
  }

  @Test
  public void testAddAlliance() throws Exception {
    final PlayerID bush = m_data.getPlayerList().getPlayerID("bush");
    final PlayerID castro = m_data.getPlayerList().getPlayerID("castro");
    final AllianceTracker allianceTracker = m_data.getAllianceTracker();
    final RelationshipTracker relationshipTracker = m_data.getRelationshipTracker();
    assertFalse(relationshipTracker.isAllied(bush, castro));
    // the alliance tracker now only keeps track of GUI elements like the stats panel alliance TUV totals, and does not
    // affect gameplay
    allianceTracker.addToAlliance(bush, "natp");
    // the relationship tracker is the one that keeps track of actual relationships between players, affecting gameplay.
    // Note that changing
    // the relationship between bush and castro, does not change the relationship between bush and chretian
    relationshipTracker.setRelationship(bush, castro,
        m_data.getRelationshipTypeList().getRelationshipType(Constants.RELATIONSHIP_TYPE_DEFAULT_ALLIED));
    assertTrue(relationshipTracker.isAllied(bush, castro));
  }

  // TODO create test suite for Alliance/Relationships/Politics
}
