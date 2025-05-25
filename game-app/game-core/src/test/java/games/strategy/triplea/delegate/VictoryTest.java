package games.strategy.triplea.delegate;

import static games.strategy.triplea.delegate.GameDataTestUtil.territory;
import static games.strategy.triplea.delegate.GameDataTestUtil.unitType;
import static games.strategy.triplea.delegate.MockDelegateBridge.advanceToStep;
import static games.strategy.triplea.delegate.MockDelegateBridge.newDelegateBridge;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.Constants;
import games.strategy.triplea.delegate.move.validation.MoveValidator;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.triplea.java.collections.CollectionUtils;
import org.triplea.java.collections.IntegerMap;

/**
 * "Victory" map is just a branch/mod of Pact of Steel 2. POS2 is an actual game with good gameplay
 * that we don't want to mess with, so "Victory" is more of an xml purely for testing purposes, and
 * probably should never be played.
 */
class VictoryTest {
  private final GameData gameData = TestMapGameData.VICTORY_TEST.getGameData();
  private final GamePlayer italians = GameDataTestUtil.italians(gameData);
  private final GamePlayer germans = GameDataTestUtil.germans(gameData);
  private final GamePlayer british = GameDataTestUtil.british(gameData);
  private final UnitType motorized = unitType(Constants.UNIT_TYPE_MOTORIZED, gameData);
  private final UnitType armour = GameDataTestUtil.armour(gameData);
  private final UnitType infantry = GameDataTestUtil.infantry(gameData);
  private final UnitType fighter = GameDataTestUtil.fighter(gameData);
  private final UnitType carrier = GameDataTestUtil.carrier(gameData);
  private final Territory belgianCongo = territory("Belgian Congo", gameData);
  private final Territory frenchEquatorialAfrica = territory("French Equatorial Africa", gameData);
  private final Territory frenchWestAfrica = territory("French West Africa", gameData);
  private final Territory angloEgypt = territory("Anglo Egypt", gameData);
  private final Territory kenya = territory("Kenya", gameData);
  private final Territory libya = territory("Libya", gameData);
  private final Territory transJordan = territory("Trans-Jordan", gameData);
  private final Territory sz29 = territory("29 Sea Zone", gameData);
  private final Territory sz30 = territory("30 Sea Zone", gameData);
  private IDelegateBridge testBridge;
  private IntegerMap<Resource> italianResources;
  private MoveDelegate moveDelegate;
  private PurchaseDelegate purchaseDelegate;

  @BeforeEach
  void setUp() {
    testBridge = newDelegateBridge(italians);
    // we need to initialize the original owner
    final InitializationDelegate initDel =
        (InitializationDelegate) gameData.getDelegate("initDelegate");
    initDel.setDelegateBridgeAndPlayer(testBridge);
    initDel.start();
    initDel.end();

    italianResources = italians.getResources().getResourcesCopy();
    purchaseDelegate = (PurchaseDelegate) gameData.getDelegate("purchase");
    moveDelegate = (MoveDelegate) gameData.getDelegate("move");
  }

  @Test
  void testNoBlitzThroughMountain() {
    gameData.performChange(ChangeFactory.addUnits(libya, armour.create(1, italians)));
    advanceToStep(testBridge, "CombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(testBridge);
    moveDelegate.start();
    final String error =
        moveDelegate.move(
            libya.getUnits(),
            gameData.getMap().getRouteOrElseThrow(libya, belgianCongo, it -> true));
    moveDelegate.end();
    assertEquals(MoveValidator.NOT_ALL_UNITS_CAN_BLITZ, error);
  }

  @Test
  void testBlitzNormal() {
    gameData.performChange(ChangeFactory.addUnits(frenchWestAfrica, armour.create(1, italians)));
    advanceToStep(testBridge, "CombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(testBridge);
    moveDelegate.start();
    final String error =
        moveDelegate.move(
            frenchWestAfrica.getUnits(),
            gameData.getMap().getRouteOrElseThrow(frenchWestAfrica, belgianCongo, it -> true));
    moveDelegate.end();
    assertNull(error);
  }

  @Test
  void testNoBlitzWithStopThroughMountain() {
    gameData.performChange(ChangeFactory.addUnits(libya, armour.create(1, italians)));
    advanceToStep(testBridge, "CombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(testBridge);
    moveDelegate.start();
    String error = moveDelegate.move(libya.getUnits(), new Route(libya, angloEgypt));
    // first step is legal
    assertNull(error);
    // second step isn't legal because we lost blitz even though we took the mountain
    error = moveDelegate.move(angloEgypt.getUnits(), new Route(angloEgypt, belgianCongo));
    moveDelegate.end();
    assertEquals(MoveValidator.NOT_ALL_UNITS_CAN_BLITZ, error);
  }

  @Test
  void testBlitzWithStop() {
    gameData.performChange(ChangeFactory.addUnits(frenchWestAfrica, armour.create(1, italians)));
    advanceToStep(testBridge, "CombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(testBridge);
    moveDelegate.start();
    String error =
        moveDelegate.move(
            frenchWestAfrica.getUnits(), new Route(frenchWestAfrica, frenchEquatorialAfrica));
    assertNull(error);
    error =
        moveDelegate.move(
            frenchEquatorialAfrica.getUnits(), new Route(frenchEquatorialAfrica, belgianCongo));
    moveDelegate.end();
    assertNull(error);
  }

  @Test
  void testMotorizedThroughMountain() {
    gameData.performChange(ChangeFactory.addUnits(libya, motorized.create(1, italians)));
    advanceToStep(testBridge, "CombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(testBridge);
    moveDelegate.start();
    final String error =
        moveDelegate.move(
            libya.getUnits(),
            gameData.getMap().getRouteOrElseThrow(libya, belgianCongo, it -> true));
    moveDelegate.end();
    assertEquals(MoveValidator.NOT_ALL_UNITS_CAN_BLITZ, error);
  }

  @Test
  void testMotorizedNoBlitzBlitzedTerritory() {
    gameData.performChange(ChangeFactory.changeOwner(frenchEquatorialAfrica, italians));
    gameData.performChange(
        ChangeFactory.addUnits(frenchEquatorialAfrica, armour.create(1, italians)));
    gameData.performChange(ChangeFactory.changeOwner(kenya, italians));
    gameData.performChange(ChangeFactory.addUnits(kenya, motorized.create(1, italians)));
    advanceToStep(testBridge, "CombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(testBridge);
    moveDelegate.start();
    String error =
        moveDelegate.move(
            frenchEquatorialAfrica.getUnits(), new Route(frenchEquatorialAfrica, belgianCongo));
    assertNull(error);
    error = moveDelegate.move(kenya.getUnits(), new Route(kenya, belgianCongo));
    assertNull(error);
    error =
        moveDelegate.move(belgianCongo.getUnits(), new Route(belgianCongo, frenchEquatorialAfrica));
    assertEquals(MoveValidator.NOT_ALL_UNITS_CAN_BLITZ, error);
    moveDelegate.end();
  }

  /**
   * These tests verify move validation concerning attacks between angloEgypt to transJordan, with
   * angloEgypt set up as contested (owned by enemy or with enemy units, depending on the test).
   */
  @Nested
  final class AttackingFromContestedTerritory {
    final List<Unit> infantryUnit = infantry.create(1, italians);
    final List<Unit> tankUnit = armour.create(1, italians);
    final List<Unit> tankAndInfantry = List.of(tankUnit.get(0), infantryUnit.get(0));
    final Route route = gameData.getMap().getRouteOrElseThrow(angloEgypt, transJordan, it -> true);

    @BeforeEach
    void setUp() {
      // Ensure that both territories we're testing are owned by an enemy.
      assertThat(angloEgypt.getOwner().isAtWar(italians), is(true));
      assertThat(transJordan.getOwner().isAtWar(italians), is(true));
      // Add some enemy units to both of them.
      gameData.performChange(ChangeFactory.addUnits(angloEgypt, armour.create(1, british)));
      gameData.performChange(ChangeFactory.addUnits(transJordan, armour.create(1, british)));
      // Ensure that infantry can't blitz and tank can, since we want to test both cases.
      assertThat(Matches.unitCanBlitz().test(infantryUnit.get(0)), is(false));
      assertThat(Matches.unitCanBlitz().test(tankUnit.get(0)), is(true));
      // Add them to angloEgypt.
      gameData.performChange(ChangeFactory.addUnits(angloEgypt, tankAndInfantry));

      advanceToStep(testBridge, "CombatMove");
      moveDelegate.setDelegateBridgeAndPlayer(testBridge);
      moveDelegate.start();
    }

    /** Attack with tank and infantry from a battle in angloEgypt to a battle in transJordan. */
    @Test
    void testAttackWithBothUnits() {
      assertThat(
          moveDelegate.move(tankAndInfantry, route),
          is(MoveValidator.CANNOT_BLITZ_OUT_OF_BATTLE_FURTHER_INTO_ENEMY_TERRITORY));
      // If CAN_ATTACK_FROM_CONTESTED_TERRITORIES is true, the attack is valid.
      gameData.getProperties().set(Constants.ALL_UNITS_CAN_ATTACK_FROM_CONTESTED_TERRITORIES, true);
      assertThat(moveDelegate.move(tankAndInfantry, route), wasPerformedSuccessfully());
    }

    /** Attack with infantry, then tank from a battle in angloEgypt to a battle in transJordan. */
    @Test
    void testAttackWithInfantryThenTank() {
      assertThat(
          moveDelegate.move(infantryUnit, route),
          is(MoveValidator.CANNOT_BLITZ_OUT_OF_BATTLE_FURTHER_INTO_ENEMY_TERRITORY));
      assertThat(
          moveDelegate.move(tankUnit, route),
          is(MoveValidator.CANNOT_BLITZ_OUT_OF_BATTLE_FURTHER_INTO_ENEMY_TERRITORY));
      // If CAN_ATTACK_FROM_CONTESTED_TERRITORIES is true, the attack is valid.
      gameData.getProperties().set(Constants.ALL_UNITS_CAN_ATTACK_FROM_CONTESTED_TERRITORIES, true);
      assertThat(moveDelegate.move(infantryUnit, route), wasPerformedSuccessfully());
      assertThat(moveDelegate.move(tankUnit, route), wasPerformedSuccessfully());
    }

    /** Attack with tank, then infantry from a battle in angloEgypt to a battle in transJordan. */
    @Test
    void testAttackWithTankThenInfantry() {
      assertThat(
          moveDelegate.move(tankUnit, route),
          is(MoveValidator.CANNOT_BLITZ_OUT_OF_BATTLE_FURTHER_INTO_ENEMY_TERRITORY));
      assertThat(
          moveDelegate.move(infantryUnit, route),
          is(MoveValidator.CANNOT_BLITZ_OUT_OF_BATTLE_FURTHER_INTO_ENEMY_TERRITORY));
      // If CAN_ATTACK_FROM_CONTESTED_TERRITORIES is true, the attack is valid.
      gameData.getProperties().set(Constants.ALL_UNITS_CAN_ATTACK_FROM_CONTESTED_TERRITORIES, true);
      assertThat(moveDelegate.move(tankUnit, route), wasPerformedSuccessfully());
      assertThat(moveDelegate.move(infantryUnit, route), wasPerformedSuccessfully());
    }

    /** Attack from a battle in angloEgypt to an empty enemy-owned transJordan. */
    @Test
    void testAttackToEmptyEnemyTerritory() {
      removeEnemyUnits(italians, transJordan);
      assertThat(transJordan.getOwner().isAtWar(italians), is(true));

      assertThat(
          moveDelegate.move(tankAndInfantry, route),
          is(MoveValidator.CANNOT_BLITZ_OUT_OF_BATTLE_FURTHER_INTO_ENEMY_TERRITORY));
      assertThat(
          moveDelegate.move(infantryUnit, route),
          is(MoveValidator.CANNOT_BLITZ_OUT_OF_BATTLE_FURTHER_INTO_ENEMY_TERRITORY));
      assertThat(
          moveDelegate.move(tankUnit, route),
          is(MoveValidator.CANNOT_BLITZ_OUT_OF_BATTLE_FURTHER_INTO_ENEMY_TERRITORY));
      // If CAN_ATTACK_FROM_CONTESTED_TERRITORIES is true, the attack is valid.
      gameData.getProperties().set(Constants.ALL_UNITS_CAN_ATTACK_FROM_CONTESTED_TERRITORIES, true);
      assertThat(moveDelegate.move(tankAndInfantry, route), wasPerformedSuccessfully());
    }

    /** Attack from an empty enemy-owned angloEgypt to a battle in transJordan. */
    @Test
    void testAttackFromContestedTerritoryWithNoEnemies() {
      removeEnemyUnits(italians, angloEgypt);
      assertThat(angloEgypt.getOwner().isAtWar(italians), is(true));

      assertThat(
          moveDelegate.move(tankAndInfantry, route),
          is(MoveValidator.NOT_ALL_UNITS_CAN_BLITZ_OUT_OF_EMPTY_ENEMY_TERRITORY));
      assertThat(
          moveDelegate.move(infantryUnit, route),
          is(MoveValidator.NOT_ALL_UNITS_CAN_BLITZ_OUT_OF_EMPTY_ENEMY_TERRITORY));
      assertThat(moveDelegate.move(tankUnit, route), wasPerformedSuccessfully());
    }

    /** Attack from an empty enemy-owned angloEgypt to a battle in transJordan. */
    @Test
    void testAttackFromContestedTerritoryWithNoEnemiesWithPropertySet() {
      removeEnemyUnits(italians, angloEgypt);
      assertThat(angloEgypt.getOwner().isAtWar(italians), is(true));

      gameData.getProperties().set(Constants.ALL_UNITS_CAN_ATTACK_FROM_CONTESTED_TERRITORIES, true);
      assertThat(moveDelegate.move(tankAndInfantry, route), wasPerformedSuccessfully());
    }

    /** Attack from an empty enemy-owned angloEgypt to an empty enemy-owned transJordan. */
    @Test
    void testAttackFromContestedTerritoryWithNoEnemiesToEmptyEnemyTerritory() {
      removeEnemyUnits(italians, angloEgypt);
      assertThat(angloEgypt.getOwner().isAtWar(italians), is(true));
      removeEnemyUnits(italians, transJordan);
      assertThat(transJordan.getOwner().isAtWar(italians), is(true));

      assertThat(
          moveDelegate.move(tankAndInfantry, route),
          is(MoveValidator.NOT_ALL_UNITS_CAN_BLITZ_OUT_OF_EMPTY_ENEMY_TERRITORY));
      assertThat(
          moveDelegate.move(infantryUnit, route),
          is(MoveValidator.NOT_ALL_UNITS_CAN_BLITZ_OUT_OF_EMPTY_ENEMY_TERRITORY));
      assertThat(moveDelegate.move(tankUnit, route), wasPerformedSuccessfully());
    }

    /** Attack from an empty enemy-owned angloEgypt to an empty enemy-owned transJordan. */
    @Test
    void testAttackFromContestedTerritoryWithNoEnemiesToEmptyEnemyTerritoryWithPropertySet() {
      removeEnemyUnits(italians, angloEgypt);
      assertThat(angloEgypt.getOwner().isAtWar(italians), is(true));
      removeEnemyUnits(italians, transJordan);
      assertThat(transJordan.getOwner().isAtWar(italians), is(true));

      gameData.getProperties().set(Constants.ALL_UNITS_CAN_ATTACK_FROM_CONTESTED_TERRITORIES, true);
      assertThat(moveDelegate.move(tankAndInfantry, route), wasPerformedSuccessfully());
    }

    /** Attack from friendly angloEgypt with enemy units to a battle in transJordan. */
    @Test
    void testAttackFromOwnContestedTerritory() {
      // Make angloEgypt owned by italians, but with enemy units.
      angloEgypt.setOwner(italians);
      assertThat(Matches.territoryHasEnemyUnits(italians).test(angloEgypt), is(true));

      assertThat(
          moveDelegate.move(tankAndInfantry, route),
          is(MoveValidator.CANNOT_BLITZ_OUT_OF_BATTLE_INTO_ENEMY_TERRITORY));
      assertThat(
          moveDelegate.move(tankUnit, route),
          is(MoveValidator.CANNOT_BLITZ_OUT_OF_BATTLE_INTO_ENEMY_TERRITORY));
      assertThat(
          moveDelegate.move(infantryUnit, route),
          is(MoveValidator.CANNOT_BLITZ_OUT_OF_BATTLE_INTO_ENEMY_TERRITORY));
      // If CAN_ATTACK_FROM_CONTESTED_TERRITORIES is true, the attack is valid.
      gameData.getProperties().set(Constants.ALL_UNITS_CAN_ATTACK_FROM_CONTESTED_TERRITORIES, true);
      assertThat(moveDelegate.move(tankAndInfantry, route), wasPerformedSuccessfully());
    }

    /** Attack from a battle in angloEgypt to friendly transJordan with enemy units. */
    @Test
    void testAttackToOwnContestedTerritory() {
      // Make transJordan owned by italians, but with enemy units.
      transJordan.setOwner(italians);
      assertThat(Matches.territoryHasEnemyUnits(italians).test(transJordan), is(true));

      assertThat(moveDelegate.move(tankAndInfantry, route), wasPerformedSuccessfully());
    }

    /** Attack from a battle in angloEgypt to friendly transJordan with enemy units. */
    @Test
    void testAttackToOwnContestedTerritoryWithPropertySet() {
      // Make transJordan owned by italians, but with enemy units.
      transJordan.setOwner(italians);
      assertThat(Matches.territoryHasEnemyUnits(italians).test(transJordan), is(true));

      gameData.getProperties().set(Constants.ALL_UNITS_CAN_ATTACK_FROM_CONTESTED_TERRITORIES, true);
      assertThat(moveDelegate.move(tankAndInfantry, route), wasPerformedSuccessfully());
    }

    private void removeEnemyUnits(GamePlayer player, Territory t) {
      Predicate<Unit> isEnemy = Matches.unitIsEnemyOf(player);
      Collection<Unit> enemyUnits = CollectionUtils.getMatches(t.getUnits(), isEnemy);
      gameData.performChange(ChangeFactory.removeUnits(t, enemyUnits));
      assertThat(CollectionUtils.countMatches(t.getUnits(), isEnemy), is(0));
    }

    Matcher<Object> wasPerformedSuccessfully() {
      return is(nullValue());
    }
  }

  @Test
  void testFuelCostAndFuelFlatCost() {
    gameData.performChange(ChangeFactory.changeOwner(kenya, italians));
    gameData.performChange(ChangeFactory.changeOwner(belgianCongo, italians));
    gameData.performChange(ChangeFactory.changeOwner(frenchEquatorialAfrica, italians));
    advanceToStep(testBridge, "CombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(testBridge);
    moveDelegate.start();
    final int fuelAmount = italians.getResources().getQuantity("Fuel");
    final int puAmount = italians.getResources().getQuantity("PUs");
    final int oreAmount = italians.getResources().getQuantity("Ore");

    gameData.performChange(ChangeFactory.addUnits(kenya, motorized.create(1, italians)));
    moveDelegate.move(kenya.getUnits(), new Route(kenya, belgianCongo));
    assertEquals(fuelAmount - 2, italians.getResources().getQuantity("Fuel"));
    assertEquals(puAmount - 1, italians.getResources().getQuantity("PUs"));
    assertEquals(oreAmount - 2, italians.getResources().getQuantity("Ore"));

    gameData.performChange(ChangeFactory.addUnits(kenya, armour.create(1, italians)));
    moveDelegate.move(kenya.getUnits(), new Route(kenya, belgianCongo));
    assertEquals(fuelAmount - 2, italians.getResources().getQuantity("Fuel"));
    assertEquals(puAmount - 1, italians.getResources().getQuantity("PUs"));
    assertEquals(oreAmount - 2, italians.getResources().getQuantity("Ore"));

    moveDelegate.move(belgianCongo.getUnits(), new Route(belgianCongo, frenchEquatorialAfrica));
    assertEquals(fuelAmount - 3, italians.getResources().getQuantity("Fuel"));
    assertEquals(puAmount - 2, italians.getResources().getQuantity("PUs"));
    assertEquals(oreAmount - 2, italians.getResources().getQuantity("Ore"));

    gameData.performChange(ChangeFactory.addUnits(kenya, motorized.create(5, italians)));
    moveDelegate.move(kenya.getUnits(), new Route(kenya, belgianCongo));
    assertEquals(fuelAmount - 13, italians.getResources().getQuantity("Fuel"));
    assertEquals(puAmount - 7, italians.getResources().getQuantity("PUs"));
    assertEquals(oreAmount - 12, italians.getResources().getQuantity("Ore"));

    gameData.performChange(ChangeFactory.addUnits(kenya, motorized.create(50, italians)));
    final String error = moveDelegate.move(kenya.getUnits(), new Route(kenya, belgianCongo));
    assertTrue(error.startsWith("Not enough resources to perform this move"));
    moveDelegate.end();
  }

  @Test
  void testFuelForCarriers() {
    advanceToStep(testBridge, "CombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(testBridge);
    moveDelegate.start();
    final int fuelAmount = italians.getResources().getQuantity("Fuel");

    // Combat move where air is always charged fuel
    gameData.performChange(ChangeFactory.addUnits(sz29, carrier.create(1, italians)));
    gameData.performChange(ChangeFactory.addUnits(sz29, fighter.create(2, italians)));
    moveDelegate.move(sz29.getUnits(), new Route(sz29, sz30));
    assertEquals(fuelAmount - 7, italians.getResources().getQuantity("Fuel"));

    // Rest of the cases use non-combat move
    moveDelegate.end();
    advanceToStep(testBridge, "NonCombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(testBridge);
    moveDelegate.start();

    // Non-combat move where air isn't charged fuel
    gameData.performChange(ChangeFactory.addUnits(sz29, carrier.create(1, italians)));
    gameData.performChange(ChangeFactory.addUnits(sz29, fighter.create(2, italians)));
    moveDelegate.move(sz29.getUnits(), new Route(sz29, sz30));
    assertEquals(fuelAmount - 8, italians.getResources().getQuantity("Fuel"));
    gameData.performChange(ChangeFactory.removeUnits(sz30, sz30.getUnitCollection()));

    // Move onto carrier, move with carrier, move off carrier
    gameData.performChange(ChangeFactory.addUnits(sz29, carrier.create(1, italians)));
    gameData.performChange(ChangeFactory.addUnits(sz29, fighter.create(1, italians)));
    gameData.performChange(ChangeFactory.addUnits(sz30, fighter.create(1, italians)));
    moveDelegate.move(sz30.getUnits(), new Route(sz30, sz29));
    assertEquals(fuelAmount - 11, italians.getResources().getQuantity("Fuel"));
    moveDelegate.move(sz29.getUnits(), new Route(sz29, sz30));
    assertEquals(fuelAmount - 12, italians.getResources().getQuantity("Fuel"));
    moveDelegate.move(
        sz30.getUnitCollection().getMatches(Matches.unitIsAir()), new Route(sz30, sz29));
    assertEquals(fuelAmount - 16, italians.getResources().getQuantity("Fuel"));
    gameData.performChange(ChangeFactory.removeUnits(sz29, sz29.getUnitCollection()));
    gameData.performChange(ChangeFactory.removeUnits(sz30, sz30.getUnitCollection()));

    // Too many fighters for carrier
    gameData.performChange(ChangeFactory.addUnits(sz29, carrier.create(1, italians)));
    gameData.performChange(ChangeFactory.addUnits(sz29, fighter.create(3, italians)));
    moveDelegate.move(sz29.getUnits(), new Route(sz29, sz30));
    assertEquals(fuelAmount - 20, italians.getResources().getQuantity("Fuel"));

    // Allied and owned fighters
    gameData.performChange(ChangeFactory.addUnits(sz29, carrier.create(2, italians)));
    gameData.performChange(ChangeFactory.addUnits(sz29, fighter.create(2, italians)));
    gameData.performChange(ChangeFactory.addUnits(sz29, fighter.create(3, germans)));
    moveDelegate.move(sz29.getUnits(), new Route(sz29, sz30));
    assertEquals(fuelAmount - 25, italians.getResources().getQuantity("Fuel"));
  }

  @Test
  void testMultipleResourcesToPurchase() {
    advanceToStep(testBridge, "italianPurchase");
    purchaseDelegate.setDelegateBridgeAndPlayer(testBridge);
    purchaseDelegate.start();
    final IntegerMap<ProductionRule> purchaseList = new IntegerMap<>();
    final ProductionRule armourtest =
        gameData.getProductionRuleList().getProductionRule("buyArmourtest");
    assertNotNull(armourtest);
    italianResources.subtract(armourtest.getCosts());
    purchaseList.add(armourtest, 1);
    final String error = purchaseDelegate.purchase(purchaseList);
    assertNull(error);
    assertEquals(italianResources, italians.getResources().getResourcesCopy());
  }

  @Test
  void testNotEnoughMultipleResourcesToPurchase() {
    advanceToStep(testBridge, "italianPurchase");
    purchaseDelegate.setDelegateBridgeAndPlayer(testBridge);
    purchaseDelegate.start();
    final IntegerMap<ProductionRule> purchaseList = new IntegerMap<>();
    final ProductionRule armourtest =
        gameData.getProductionRuleList().getProductionRule("buyArmourtest2");
    assertNotNull(armourtest);
    italianResources.subtract(armourtest.getCosts());
    purchaseList.add(armourtest, 1);
    final String error = purchaseDelegate.purchase(purchaseList);
    assertEquals(PurchaseDelegate.NOT_ENOUGH_RESOURCES, error);
  }

  @Test
  void testPuOnlyResourcesToPurchase() {
    advanceToStep(testBridge, "italianPurchase");
    purchaseDelegate.setDelegateBridgeAndPlayer(testBridge);
    purchaseDelegate.start();
    final IntegerMap<ProductionRule> purchaseList = new IntegerMap<>();
    final ProductionRule buyArmour =
        gameData.getProductionRuleList().getProductionRule("buyArmour");
    assertNotNull(buyArmour);
    italianResources.subtract(buyArmour.getCosts());
    purchaseList.add(buyArmour, 1);
    final String error = purchaseDelegate.purchase(purchaseList);
    assertNull(error);
    assertEquals(italianResources, italians.getResources().getResourcesCopy());
  }

  @Test
  void testNoPuResourcesToPurchase() {
    advanceToStep(testBridge, "italianPurchase");
    purchaseDelegate.setDelegateBridgeAndPlayer(testBridge);
    purchaseDelegate.start();
    final IntegerMap<ProductionRule> purchaseList = new IntegerMap<>();
    final ProductionRule buyArmour =
        gameData.getProductionRuleList().getProductionRule("buyArmourtest3");
    assertNotNull(buyArmour);
    italianResources.subtract(buyArmour.getCosts());
    purchaseList.add(buyArmour, 1);
    final String error = purchaseDelegate.purchase(purchaseList);
    assertNull(error);
    assertEquals(italianResources, italians.getResources().getResourcesCopy());
  }
}
