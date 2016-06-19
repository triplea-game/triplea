package games.strategy.engine.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Before;
import org.junit.Test;

import games.strategy.engine.data.DelegateList;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameMap;
import games.strategy.engine.data.GameParser;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.PlayerList;
import games.strategy.engine.data.ProductionFrontier;
import games.strategy.engine.data.RelationshipTracker;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.ResourceList;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.UnitTypeList;
import games.strategy.triplea.Constants;

public class ParserTest {
  private GameData gameData;

  @Before
  public void setUp() throws Exception {
    // get the xml file
    final URL url = this.getClass().getResource("GameExample.xml");
    // System.out.println(url);
    final InputStream input = url.openStream();
    gameData = (new GameParser()).parse(input, new AtomicReference<>(), false);
  }

  @Test
  public void testCanCreateData() {
    assertNotNull(gameData);
  }

  public void testTerritoriesCreated() {
    final GameMap map = gameData.getMap();
    final Collection<Territory> territories = map.getTerritories();
    assertEquals(territories.size(), 3);
  }

  @Test
  public void testWater() {
    final Territory atl = gameData.getMap().getTerritory("atlantic");
    assertEquals(atl.isWater(), true);
    final Territory can = gameData.getMap().getTerritory("canada");
    assertEquals(can.isWater(), false);
  }

  @Test
  public void testTerritoriesConnected() {
    final GameMap map = gameData.getMap();
    assertEquals(1, map.getDistance(map.getTerritory("canada"), map.getTerritory("us")));
  }

  @Test
  public void testResourcesAdded() {
    final ResourceList resources = gameData.getResourceList();
    assertEquals(resources.size(), 2);
  }

  @Test
  public void testUnitTypesAdded() {
    final UnitTypeList units = gameData.getUnitTypeList();
    assertEquals(units.size(), 1);
  }

  @Test
  public void testPlayersAdded() {
    final PlayerList players = gameData.getPlayerList();
    assertEquals(players.size(), 3);
  }

  @Test
  public void testAllianceMade() {
    final PlayerList players = gameData.getPlayerList();
    final PlayerID castro = players.getPlayerID("castro");
    final PlayerID chretian = players.getPlayerID("chretian");
    final RelationshipTracker alliances = gameData.getRelationshipTracker();
    assertEquals(true, alliances.isAllied(castro, chretian));
  }

  @Test
  public void testDelegatesCreated() {
    final DelegateList delegates = gameData.getDelegateList();
    assertEquals(delegates.size(), 2);
  }

  @Test
  public void testStepsCreated() {
    gameData.getSequence();
  }

  @Test
  public void testProductionFrontiersCreated() {
    assertEquals(gameData.getProductionFrontierList().size(), 2);
  }

  @Test
  public void testProductionRulesCreated() {
    assertEquals(gameData.getProductionRuleList().size(), 3);
  }

  @Test
  public void testPlayerProduction() {
    final ProductionFrontier cf = gameData.getProductionFrontierList().getProductionFrontier("canProd");
    final PlayerID can = gameData.getPlayerList().getPlayerID("chretian");
    assertEquals(can.getProductionFrontier(), cf);
  }

  @Test
  public void testAttachments() {
    TestAttachment att = (TestAttachment) gameData.getResourceList().getResource("gold")
        .getAttachment(Constants.RESOURCE_ATTACHMENT_NAME);
    assertEquals(att.getValue(), "gold");
    att = (TestAttachment) gameData.getUnitTypeList().getUnitType(Constants.UNIT_TYPE_INF)
        .getAttachment(Constants.INF_ATTACHMENT_NAME);
    assertEquals(att.getValue(), "inf");
    att = (TestAttachment) gameData.getMap().getTerritory("us").getAttachment(Constants.TERRITORY_ATTACHMENT_NAME);
    assertEquals(att.getValue(), "us of a");
    att = (TestAttachment) gameData.getPlayerList().getPlayerID("chretian")
        .getAttachment(Constants.PLAYER_ATTACHMENT_NAME);
    assertEquals(att.getValue(), "liberal");
  }

  @Test
  public void testOwnerInitialze() {
    final Territory can = gameData.getMap().getTerritory("canada");
    assertNotNull("couldnt find country", can);
    assertNotNull("owner null", can.getOwner());
    assertEquals(can.getOwner().getName(), "chretian");
    final Territory us = gameData.getMap().getTerritory("us");
    assertEquals(us.getOwner().getName(), "bush");
  }

  @Test
  public void testUnitsHeldInitialized() {
    final PlayerID bush = gameData.getPlayerList().getPlayerID("bush");
    assertEquals(bush.getUnits().getUnitCount(), 20);
  }

  @Test
  public void testUnitsPlacedInitialized() {
    final Territory terr = gameData.getMap().getTerritory("canada");
    assertEquals(terr.getUnits().getUnitCount(), 5);
  }

  @Test
  public void testResourcesGiven() {
    final PlayerID chretian = gameData.getPlayerList().getPlayerID("chretian");
    final Resource resource = gameData.getResourceList().getResource("silver");
    assertEquals(200, chretian.getResources().getQuantity(resource));
  }
}
