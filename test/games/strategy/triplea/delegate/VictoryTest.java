package games.strategy.triplea.delegate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import games.strategy.engine.data.ChangeFactory;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.ITestDelegateBridge;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.Constants;
import games.strategy.triplea.xml.LoadGameUtil;
import games.strategy.util.IntegerMap;

/**
 * "Victory" map is just a branch/mod of Pact of Steel 2.
 * POS2 is an actual game with good gameplay that we don't want to mess with, so
 * "Victory" is more of an xml purely for testing purposes, and probably should never be played.
 */
public class VictoryTest {
  private GameData gameData;
  private PlayerID italians;
  private ITestDelegateBridge testBridge;

  private IntegerMap<Resource> italianResources;
  private PurchaseDelegate purchaseDelegate;
  private Territory britishCongo;
  private Territory kenya;
  private UnitType motorized;
  private UnitType armour;
  private Territory frenchEastAfrica;
  private Territory frenchWestAfrica;
  private Territory angloEgypt;
  private Territory libya;
  private MoveDelegate moveDelegate;

  @Before
  public void setUp() throws Exception {
    gameData = LoadGameUtil.loadTestGame("victory_test.xml");
    italians = GameDataTestUtil.italians(gameData);
    testBridge = GameDataTestUtil.getDelegateBridge(italians, gameData);
    // we need to initialize the original owner
    final InitializationDelegate initDel =
        (InitializationDelegate) gameData.getDelegateList().getDelegate("initDelegate");
    initDel.setDelegateBridgeAndPlayer(testBridge);
    initDel.start();
    initDel.end();

    italianResources = italians.getResources().getResourcesCopy();
    purchaseDelegate = (PurchaseDelegate) gameData.getDelegateList().getDelegate("purchase");
    moveDelegate = (MoveDelegate) gameData.getDelegateList().getDelegate("move");

    britishCongo = gameData.getMap().getTerritory("Belgian Congo");
    kenya = gameData.getMap().getTerritory("Kenya");
    motorized = gameData.getUnitTypeList().getUnitType(Constants.UNIT_TYPE_MOTORIZED);
    armour = GameDataTestUtil.armour(gameData);
    frenchEastAfrica = gameData.getMap().getTerritory("French Equatorial Africa");
    frenchWestAfrica = gameData.getMap().getTerritory("French West Africa");
    angloEgypt = gameData.getMap().getTerritory("Anglo Egypt");
    libya = gameData.getMap().getTerritory("Libya");

  }

  @Test
  public void testNoBlitzThroughMountain() {
    gameData.performChange(ChangeFactory.addUnits(libya, armour.create(1, italians)));
    testBridge.setStepName("CombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(testBridge);
    moveDelegate.start();
    final String error =
        moveDelegate.move(libya.getUnits().getUnits(), gameData.getMap().getRoute(libya, britishCongo));
    moveDelegate.end();
    assertTrue(error.equals(MoveValidator.NOT_ALL_UNITS_CAN_BLITZ));
  }

  @Test
  public void testBlitzNormal() {
    gameData.performChange(ChangeFactory.addUnits(frenchWestAfrica, armour.create(1, italians)));
    testBridge.setStepName("CombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(testBridge);
    moveDelegate.start();
    final String error =
        moveDelegate.move(frenchWestAfrica.getUnits().getUnits(),
            gameData.getMap().getRoute(frenchWestAfrica, britishCongo));
    moveDelegate.end();
    assertEquals(error, null);
  }

  @Test
  public void testNoBlitzWithStopThroughMountain() {
    gameData.performChange(ChangeFactory.addUnits(libya, armour.create(1, italians)));
    testBridge.setStepName("CombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(testBridge);
    moveDelegate.start();
    String error = moveDelegate.move(libya.getUnits().getUnits(), gameData.getMap().getRoute(libya, angloEgypt));
    // first step is legal
    assertEquals(error, null);
    // second step isn't legal because we lost blitz even though we took the mountain
    error = moveDelegate.move(angloEgypt.getUnits().getUnits(), gameData.getMap().getRoute(angloEgypt, britishCongo));
    moveDelegate.end();
    assertTrue(error.equals(MoveValidator.NOT_ALL_UNITS_CAN_BLITZ));
  }

  @Test
  public void testBlitzWithStop() {
    gameData.performChange(ChangeFactory.addUnits(frenchWestAfrica, armour.create(1, italians)));
    testBridge.setStepName("CombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(testBridge);
    moveDelegate.start();
    String error = moveDelegate.move(frenchWestAfrica.getUnits().getUnits(),
        gameData.getMap().getRoute(frenchWestAfrica, frenchEastAfrica));
    assertEquals(error, null);
    error = moveDelegate.move(frenchEastAfrica.getUnits().getUnits(),
        gameData.getMap().getRoute(frenchEastAfrica, britishCongo));
    moveDelegate.end();
    assertEquals(error, null);
  }


  @Test
  public void testMotorizedThroughMountain() {
    gameData.performChange(ChangeFactory.addUnits(libya, motorized.create(1, italians)));
    testBridge.setStepName("CombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(testBridge);
    moveDelegate.start();
    final String error =
        moveDelegate.move(libya.getUnits().getUnits(), gameData.getMap().getRoute(libya, britishCongo));
    moveDelegate.end();
    assertTrue(error.equals(MoveValidator.NOT_ALL_UNITS_CAN_BLITZ));
  }

  @Test
  public void testMotorizedNoBlitzBlitzedTerritory() {
    gameData.performChange(ChangeFactory.changeOwner(frenchEastAfrica, italians));
    gameData.performChange(ChangeFactory.addUnits(frenchEastAfrica, armour.create(1, italians)));
    gameData.performChange(ChangeFactory.changeOwner(kenya, italians));
    gameData.performChange(ChangeFactory.addUnits(kenya, motorized.create(1, italians)));
    testBridge.setStepName("CombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(testBridge);
    moveDelegate.start();
    String error = moveDelegate.move(frenchEastAfrica.getUnits().getUnits(),
        gameData.getMap().getRoute(frenchEastAfrica, britishCongo));
    assertEquals(null, error);
    error = moveDelegate.move(kenya.getUnits().getUnits(), gameData.getMap().getRoute(kenya, britishCongo));
    assertEquals(null, error);
    error = moveDelegate.move(britishCongo.getUnits().getUnits(),
        gameData.getMap().getRoute(britishCongo, frenchEastAfrica));
    assertEquals(MoveValidator.NOT_ALL_UNITS_CAN_BLITZ, error);
    moveDelegate.end();
  }

  @Test
  public void testFuelUseMotorized() {
    gameData.performChange(ChangeFactory.changeOwner(kenya, italians));
    gameData.performChange(ChangeFactory.addUnits(kenya, motorized.create(1, italians)));
    testBridge.setStepName("CombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(testBridge);
    moveDelegate.start();
    final int fuelAmount = italians.getResources().getQuantity("Fuel");
    final int puAmount = italians.getResources().getQuantity("PUs");
    moveDelegate.move(kenya.getUnits().getUnits(), gameData.getMap().getRoute(kenya, britishCongo));
    assertEquals(fuelAmount - 1, italians.getResources().getQuantity("Fuel"));
    assertEquals(puAmount - 1, italians.getResources().getQuantity("PUs"));
    gameData.performChange(ChangeFactory.addUnits(kenya, armour.create(1, italians)));
    moveDelegate.move(kenya.getUnits().getUnits(), gameData.getMap().getRoute(kenya, britishCongo));
    assertEquals(fuelAmount - 1, italians.getResources().getQuantity("Fuel"));
    assertEquals(puAmount - 1, italians.getResources().getQuantity("PUs"));
    gameData.performChange(ChangeFactory.addUnits(kenya, motorized.create(5, italians)));
    moveDelegate.move(kenya.getUnits().getUnits(), gameData.getMap().getRoute(kenya, britishCongo));
    assertEquals(fuelAmount - 6, italians.getResources().getQuantity("Fuel"));
    assertEquals(puAmount - 6, italians.getResources().getQuantity("PUs"));
    gameData.performChange(ChangeFactory.addUnits(kenya, motorized.create(50, italians)));
    final String error =
        moveDelegate.move(kenya.getUnits().getUnits(), gameData.getMap().getRoute(kenya, britishCongo));
    assertTrue(error.startsWith("Not enough resources to perform this move"));
    moveDelegate.end();
  }

  @Test
  public void testMultipleResourcesToPurchase() {
    testBridge.setStepName("italianPurchase");
    purchaseDelegate.setDelegateBridgeAndPlayer(testBridge);
    purchaseDelegate.start();
    final IntegerMap<ProductionRule> purchaseList = new IntegerMap<>();
    final ProductionRule armourtest = gameData.getProductionRuleList().getProductionRule("buyArmourtest");
    assertNotNull(armourtest);
    italianResources.subtract(armourtest.getCosts());
    purchaseList.add(armourtest, 1);
    final String error = purchaseDelegate.purchase(purchaseList);
    assertEquals(null, error);
    assertEquals(italianResources, italians.getResources().getResourcesCopy());
  }

  @Test
  public void testNotEnoughMultipleResourcesToPurchase() {
    testBridge.setStepName("italianPurchase");
    purchaseDelegate.setDelegateBridgeAndPlayer(testBridge);
    purchaseDelegate.start();
    final IntegerMap<ProductionRule> purchaseList = new IntegerMap<>();
    final ProductionRule armourtest = gameData.getProductionRuleList().getProductionRule("buyArmourtest2");
    assertNotNull(armourtest);
    italianResources.subtract(armourtest.getCosts());
    purchaseList.add(armourtest, 1);
    final String error = purchaseDelegate.purchase(purchaseList);
    assertEquals(PurchaseDelegate.NOT_ENOUGH_RESOURCES, error);
  }

  @Test
  public void testPUOnlyResourcesToPurchase() {
    testBridge.setStepName("italianPurchase");
    purchaseDelegate.setDelegateBridgeAndPlayer(testBridge);
    purchaseDelegate.start();
    final IntegerMap<ProductionRule> purchaseList = new IntegerMap<>();
    final ProductionRule buyArmour = gameData.getProductionRuleList().getProductionRule("buyArmour");
    assertNotNull(buyArmour);
    italianResources.subtract(buyArmour.getCosts());
    purchaseList.add(buyArmour, 1);
    final String error = purchaseDelegate.purchase(purchaseList);
    assertEquals(null, error);
    assertEquals(italianResources, italians.getResources().getResourcesCopy());
  }

  @Test
  public void testNoPUResourcesToPurchase() {
    testBridge.setStepName("italianPurchase");
    purchaseDelegate.setDelegateBridgeAndPlayer(testBridge);
    purchaseDelegate.start();
    final IntegerMap<ProductionRule> purchaseList = new IntegerMap<>();
    final ProductionRule buyArmour = gameData.getProductionRuleList().getProductionRule("buyArmourtest3");
    assertNotNull(buyArmour);
    italianResources.subtract(buyArmour.getCosts());
    purchaseList.add(buyArmour, 1);
    final String error = purchaseDelegate.purchase(purchaseList);
    assertEquals(null, error);
    assertEquals(italianResources, italians.getResources().getResourcesCopy());
  }
}
