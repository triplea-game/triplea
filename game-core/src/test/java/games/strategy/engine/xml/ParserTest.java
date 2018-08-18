package games.strategy.engine.xml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import games.strategy.engine.data.DelegateList;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameMap;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.PlayerList;
import games.strategy.engine.data.ProductionFrontier;
import games.strategy.engine.data.RelationshipTracker;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.ResourceList;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.UnitTypeList;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.xml.TestMapGameData;

public class ParserTest {
  private GameData gameData;

  @BeforeEach
  public void setUp() throws Exception {
    gameData = TestMapGameData.GAME_EXAMPLE.getGameData();
  }

  @Test
  public void testCanCreateData() {
    assertNotNull(gameData);
  }

  @Test
  public void testTerritoriesCreated() {
    final GameMap map = gameData.getMap();
    final Collection<Territory> territories = map.getTerritories();
    assertEquals(3, territories.size());
  }

  @Test
  public void testWater() {
    final Territory atl = gameData.getMap().getTerritory("atlantic");
    assertTrue(atl.isWater());
    final Territory can = gameData.getMap().getTerritory("canada");
    assertFalse(can.isWater());
  }

  @Test
  public void testTerritoriesConnected() {
    final GameMap map = gameData.getMap();
    assertEquals(1, map.getDistance(map.getTerritory("canada"), map.getTerritory("us")));
  }

  @Test
  public void testResourcesAdded() {
    final ResourceList resources = gameData.getResourceList();
    assertEquals(2, resources.size());
  }

  @Test
  public void testUnitTypesAdded() {
    final UnitTypeList units = gameData.getUnitTypeList();
    assertEquals(1, units.size());
  }

  @Test
  public void testPlayersAdded() {
    final PlayerList players = gameData.getPlayerList();
    assertEquals(3, players.size());
  }

  @Test
  public void testAllianceMade() {
    final PlayerList players = gameData.getPlayerList();
    final PlayerID castro = players.getPlayerId("castro");
    final PlayerID chretian = players.getPlayerId("chretian");
    final RelationshipTracker alliances = gameData.getRelationshipTracker();
    assertTrue(alliances.isAllied(castro, chretian));
  }

  @Test
  public void testDelegatesCreated() {
    final DelegateList delegates = gameData.getDelegateList();
    assertEquals(2, delegates.size());
  }

  @Test
  public void testStepsCreated() {
    gameData.getSequence();
  }

  @Test
  public void testProductionFrontiersCreated() {
    assertEquals(2, gameData.getProductionFrontierList().size());
  }

  @Test
  public void testProductionRulesCreated() {
    assertEquals(3, gameData.getProductionRuleList().size());
  }

  @Test
  public void testPlayerProduction() {
    final ProductionFrontier cf = gameData.getProductionFrontierList().getProductionFrontier("canProd");
    final PlayerID can = gameData.getPlayerList().getPlayerId("chretian");
    assertEquals(cf, can.getProductionFrontier());
  }

  @Test
  public void testAttachments() {
    TestAttachment att = (TestAttachment) gameData.getResourceList().getResource("gold")
        .getAttachment(Constants.RESOURCE_ATTACHMENT_NAME);
    assertEquals("gold", att.getValue());
    final UnitAttachment ua = (UnitAttachment) gameData.getUnitTypeList().getUnitType(Constants.UNIT_TYPE_INF)
        .getAttachment(Constants.UNIT_ATTACHMENT_NAME);
    assertEquals(1, ua.getTransportCost());
    att = (TestAttachment) gameData.getMap().getTerritory("us").getAttachment(Constants.TERRITORY_ATTACHMENT_NAME);
    assertEquals("us of a", att.getValue());
    att = (TestAttachment) gameData.getPlayerList().getPlayerId("chretian")
        .getAttachment(Constants.PLAYER_ATTACHMENT_NAME);
    assertEquals("liberal", att.getValue());
  }

  @Test
  public void testOwnerInitialze() {
    final Territory can = gameData.getMap().getTerritory("canada");
    assertNotNull(can, "couldnt find country");
    assertNotNull(can.getOwner(), "owner null");
    assertEquals("chretian", can.getOwner().getName());
    final Territory us = gameData.getMap().getTerritory("us");
    assertEquals("bush", us.getOwner().getName());
  }

  @Test
  public void testUnitsHeldInitialized() {
    final PlayerID bush = gameData.getPlayerList().getPlayerId("bush");
    assertEquals(20, bush.getUnits().getUnitCount());
  }

  @Test
  public void testUnitsPlacedInitialized() {
    final Territory terr = gameData.getMap().getTerritory("canada");
    assertEquals(5, terr.getUnits().getUnitCount());
  }

  @Test
  public void testResourcesGiven() {
    final PlayerID chretian = gameData.getPlayerList().getPlayerId("chretian");
    final Resource resource = gameData.getResourceList().getResource("silver");
    assertEquals(200, chretian.getResources().getQuantity(resource));
  }
}
