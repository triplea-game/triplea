package games.strategy.triplea.delegate;

import static games.strategy.triplea.delegate.MockDelegateBridge.advanceToStep;
import static games.strategy.triplea.delegate.MockDelegateBridge.newDelegateBridge;
import static games.strategy.triplea.delegate.MockDelegateBridge.whenGetRandom;
import static games.strategy.triplea.delegate.MockDelegateBridge.withValues;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.delegate.battle.BattleDelegate;
import games.strategy.triplea.delegate.battle.IBattle.BattleType;
import games.strategy.triplea.delegate.remote.IBattleDelegate;
import games.strategy.triplea.settings.AbstractClientSettingTestCase;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import org.junit.jupiter.api.Test;

class AirThatCantLandUtilTest extends AbstractClientSettingTestCase {
  private final GameData gameData = TestMapGameData.REVISED.getGameData();
  private final GamePlayer americansPlayer = GameDataTestUtil.americans(gameData);
  private final UnitType fighterType = GameDataTestUtil.fighter(gameData);

  private static void fight(final IBattleDelegate battle, final Territory territory) {
    for (final Entry<BattleType, Collection<Territory>> entry :
        battle.getBattleListing().getBattlesMap().entrySet()) {
      if (!entry.getKey().isBombingRun() && entry.getValue().contains(territory)) {
        battle.fightBattle(territory, false, entry.getKey());
        return;
      }
    }
    throw new IllegalStateException("Could not find  battle in: " + territory.getName());
  }

  @Test
  void testSimple() {
    final GamePlayer player = americansPlayer;
    // everything can land
    final IDelegateBridge bridge = newDelegateBridge(player);
    final AirThatCantLandUtil util = new AirThatCantLandUtil(bridge);
    assertTrue(util.getTerritoriesWhereAirCantLand(player).isEmpty());
  }

  @Test
  void testCantLandEnemyTerritory() {
    final GamePlayer player = americansPlayer;
    final IDelegateBridge bridge = newDelegateBridge(player);
    final Territory balkans = gameData.getMap().getTerritoryOrThrow("Balkans");
    final Change addAir = ChangeFactory.addUnits(balkans, fighterType.create(2, player));
    gameData.performChange(addAir);
    final AirThatCantLandUtil airThatCantLandUtil = new AirThatCantLandUtil(bridge);
    final Collection<Territory> cantLand =
        airThatCantLandUtil.getTerritoriesWhereAirCantLand(player);
    assertEquals(1, cantLand.size());
    assertEquals(balkans, cantLand.iterator().next());
    airThatCantLandUtil.removeAirThatCantLand(player, false);
    // just the original german fighter
    assertEquals(1, balkans.getUnitCollection().getMatches(Matches.unitIsAir()).size());
  }

  @Test
  void testCantLandWater() {
    final GamePlayer player = americansPlayer;
    final IDelegateBridge bridge = newDelegateBridge(player);
    final Territory sz55 = gameData.getMap().getTerritoryOrThrow("55 Sea Zone");
    final Change addAir = ChangeFactory.addUnits(sz55, fighterType.create(2, player));
    gameData.performChange(addAir);
    final AirThatCantLandUtil airThatCantLandUtil = new AirThatCantLandUtil(bridge);
    final Collection<Territory> cantLand =
        airThatCantLandUtil.getTerritoriesWhereAirCantLand(player);
    assertEquals(1, cantLand.size());
    assertEquals(sz55, cantLand.iterator().next());
    airThatCantLandUtil.removeAirThatCantLand(player, false);
    assertEquals(0, sz55.getUnitCollection().getMatches(Matches.unitIsAir()).size());
  }

  @Test
  void testSpareNextToFactory() {
    final GamePlayer player = americansPlayer;
    final IDelegateBridge bridge = newDelegateBridge(player);
    final Territory sz55 = gameData.getMap().getTerritoryOrThrow("55 Sea Zone");
    final Change addAir = ChangeFactory.addUnits(sz55, fighterType.create(2, player));
    gameData.performChange(addAir);
    final AirThatCantLandUtil airThatCantLandUtil = new AirThatCantLandUtil(bridge);
    airThatCantLandUtil.removeAirThatCantLand(player, true);
    assertEquals(2, sz55.getUnitCollection().getMatches(Matches.unitIsAir()).size());
  }

  @Test
  void testCantLandCarrier() {
    // 1 carrier in the region, but three fighters, make sure we cant land
    final GamePlayer player = americansPlayer;
    final IDelegateBridge bridge = newDelegateBridge(player);
    final Territory sz52 = gameData.getMap().getTerritoryOrThrow("52 Sea Zone");
    final Change addAir = ChangeFactory.addUnits(sz52, fighterType.create(2, player));
    gameData.performChange(addAir);
    final AirThatCantLandUtil airThatCantLandUtil = new AirThatCantLandUtil(bridge);
    final Collection<Territory> cantLand =
        airThatCantLandUtil.getTerritoriesWhereAirCantLand(player);
    assertEquals(1, cantLand.size());
    assertEquals(sz52, cantLand.iterator().next());
    airThatCantLandUtil.removeAirThatCantLand(player, false);
    // just the original american fighter, plus one that can land on the carrier
    assertEquals(2, sz52.getUnitCollection().getMatches(Matches.unitIsAir()).size());
  }

  @Test
  void testCanLandNeighborCarrier() {
    final GamePlayer japanese = GameDataTestUtil.japanese(gameData);
    final GamePlayer americans = GameDataTestUtil.americans(gameData);
    final IDelegateBridge bridge = newDelegateBridge(japanese);
    // we need to initialize the original owner
    final InitializationDelegate initDel =
        (InitializationDelegate) gameData.getDelegate("initDelegate");
    initDel.setDelegateBridgeAndPlayer(bridge);
    initDel.start();
    initDel.end();
    // Get necessary sea zones and unit types for this test
    final Territory sz44 = gameData.getMap().getTerritoryOrThrow("44 Sea Zone");
    final Territory sz45 = gameData.getMap().getTerritoryOrThrow("45 Sea Zone");
    final Territory sz52 = gameData.getMap().getTerritoryOrThrow("52 Sea Zone");
    final UnitType subType = GameDataTestUtil.submarine(gameData);
    final UnitType carrierType = GameDataTestUtil.carrier(gameData);
    final UnitType fighterType = GameDataTestUtil.fighter(gameData);
    // Add units for the test
    gameData.performChange(ChangeFactory.addUnits(sz45, subType.create(1, japanese)));
    gameData.performChange(ChangeFactory.addUnits(sz44, carrierType.create(1, americans)));
    gameData.performChange(ChangeFactory.addUnits(sz44, fighterType.create(1, americans)));
    // Get total number of defending units before the battle
    final int preCountSz52 = sz52.getUnitCollection().size();
    final int preCountAirSz44 = sz44.getUnitCollection().getMatches(Matches.unitIsAir()).size();
    // now move to attack
    final AbstractMoveDelegate moveDelegate = gameData.getMoveDelegate();
    advanceToStep(bridge, "CombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(bridge);
    moveDelegate.start();
    moveDelegate.move(sz45.getUnits(), new Route(sz45, sz44));
    moveDelegate.end();
    // fight the battle
    final BattleDelegate battle = gameData.getBattleDelegate();
    battle.setDelegateBridgeAndPlayer(bridge);
    whenGetRandom(bridge)
        .thenAnswer(withValues(0, 0))
        .thenAnswer(withValues(0))
        .thenAnswer(withValues(0))
        .thenAnswer(withValues(0));
    battle.start();
    battle.end();
    // Get the total number of units that should be left after the planes retreat
    final int expectedCountSz52 = sz52.getUnitCollection().size();
    final int postCountInt = preCountSz52 + preCountAirSz44;
    // Compare the expected count with the actual number of units in landing zone
    assertEquals(expectedCountSz52, postCountInt);
  }

  @Test
  void testCanLandMultiNeighborCarriers() {
    final GamePlayer japanese = GameDataTestUtil.japanese(gameData);
    final GamePlayer americans = GameDataTestUtil.americans(gameData);
    final IDelegateBridge bridge = newDelegateBridge(japanese);
    // we need to initialize the original owner
    final InitializationDelegate initDel =
        (InitializationDelegate) gameData.getDelegate("initDelegate");
    initDel.setDelegateBridgeAndPlayer(bridge);
    initDel.start();
    initDel.end();
    // Get necessary sea zones and unit types for this test
    final Territory sz43 = gameData.getMap().getTerritoryOrThrow("43 Sea Zone");
    final Territory sz44 = gameData.getMap().getTerritoryOrThrow("44 Sea Zone");
    final Territory sz45 = gameData.getMap().getTerritoryOrThrow("45 Sea Zone");
    final Territory sz52 = gameData.getMap().getTerritoryOrThrow("52 Sea Zone");
    final UnitType subType = GameDataTestUtil.submarine(gameData);
    final UnitType carrierType = GameDataTestUtil.carrier(gameData);
    final UnitType fighterType = GameDataTestUtil.fighter(gameData);
    // Add units for the test
    gameData.performChange(ChangeFactory.addUnits(sz45, subType.create(1, japanese)));
    gameData.performChange(ChangeFactory.addUnits(sz44, carrierType.create(1, americans)));
    gameData.performChange(ChangeFactory.addUnits(sz44, fighterType.create(3, americans)));
    gameData.performChange(ChangeFactory.addUnits(sz43, carrierType.create(1, americans)));
    // Get total number of defending units before the battle
    final int preCountSz52 = sz52.getUnitCollection().size();
    final int preCountSz43 = sz43.getUnitCollection().size();
    // now move to attack
    final AbstractMoveDelegate moveDelegate = gameData.getMoveDelegate();
    advanceToStep(bridge, "CombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(bridge);
    moveDelegate.start();
    moveDelegate.move(sz45.getUnits(), new Route(sz45, sz44));
    moveDelegate.end();
    // fight the battle
    final BattleDelegate battle = gameData.getBattleDelegate();
    battle.setDelegateBridgeAndPlayer(bridge);
    whenGetRandom(bridge).thenAnswer(withValues(0, 0)).thenAnswer(withValues(0, 0, 0));
    battle.start();
    battle.end();
    // Get the total number of units that should be left after the planes retreat
    final int expectedCountSz52 = sz52.getUnitCollection().size();
    final int expectedCountSz43 = sz43.getUnitCollection().size();
    final int postCountSz52 = preCountSz52 + 1;
    final int postCountSz43 = preCountSz43 + 2;
    // Compare the expected count with the actual number of units in landing zone
    assertEquals(expectedCountSz52, postCountSz52);
    assertEquals(expectedCountSz43, postCountSz43);
  }

  @Test
  void testCanLandNeighborLandV2() {
    final GamePlayer japanese = GameDataTestUtil.japanese(gameData);
    final GamePlayer americans = GameDataTestUtil.americans(gameData);
    final IDelegateBridge bridge = newDelegateBridge(japanese);
    // we need to initialize the original owner
    final InitializationDelegate initDel =
        (InitializationDelegate) gameData.getDelegate("initDelegate");
    initDel.setDelegateBridgeAndPlayer(bridge);
    initDel.start();
    initDel.end();
    // Get necessary sea zones and unit types for this test
    final Territory sz9 = gameData.getMap().getTerritoryOrThrow("9 Sea Zone");
    final Territory eastCanada = gameData.getMap().getTerritoryOrThrow("Eastern Canada");
    final Territory sz11 = gameData.getMap().getTerritoryOrThrow("11 Sea Zone");
    final UnitType subType = GameDataTestUtil.submarine(gameData);
    final UnitType carrierType = GameDataTestUtil.carrier(gameData);
    final UnitType fighterType = GameDataTestUtil.fighter(gameData);
    // Add units for the test
    gameData.performChange(ChangeFactory.addUnits(sz11, subType.create(1, japanese)));
    gameData.performChange(ChangeFactory.addUnits(sz9, carrierType.create(1, americans)));
    gameData.performChange(ChangeFactory.addUnits(sz9, fighterType.create(1, americans)));
    // Get total number of defending units before the battle
    final int preCountCanada = eastCanada.getUnitCollection().size();
    final int preCountAirSz9 = sz9.getUnitCollection().getMatches(Matches.unitIsAir()).size();
    // now move to attack
    final AbstractMoveDelegate moveDelegate = gameData.getMoveDelegate();
    advanceToStep(bridge, "CombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(bridge);
    moveDelegate.start();
    moveDelegate.move(sz11.getUnits(), new Route(sz11, sz9));
    moveDelegate.end();
    // fight the battle
    final BattleDelegate battle = gameData.getBattleDelegate();
    battle.setDelegateBridgeAndPlayer(bridge);
    whenGetRandom(bridge).thenAnswer(withValues(0));
    battle.start();
    battle.end();
    // Get the total number of units that should be left after the planes retreat
    final int expectedCountCanada = eastCanada.getUnitCollection().size();
    final int postCountInt = preCountCanada + preCountAirSz9;
    // Compare the expected count with the actual number of units in landing zone
    assertEquals(expectedCountCanada, postCountInt);
  }

  @Test
  void testCanLandNeighborLandWithRetreatedBattleV2() {
    final GamePlayer japanese = GameDataTestUtil.japanese(gameData);
    final GamePlayer americans = GameDataTestUtil.americans(gameData);
    final IDelegateBridge bridge = newDelegateBridge(japanese);
    // Get necessary sea zones and unit types for this test
    final Territory sz9 = gameData.getMap().getTerritoryOrThrow("9 Sea Zone");
    final Territory eastCanada = gameData.getMap().getTerritoryOrThrow("Eastern Canada");
    final Territory sz11 = gameData.getMap().getTerritoryOrThrow("11 Sea Zone");
    final UnitType subType = GameDataTestUtil.submarine(gameData);
    final UnitType carrierType = GameDataTestUtil.carrier(gameData);
    final UnitType fighterType = GameDataTestUtil.fighter(gameData);
    final UnitType transportType = GameDataTestUtil.transport(gameData);
    final UnitType infantryType = GameDataTestUtil.infantry(gameData);
    // Add units for the test
    gameData.performChange(ChangeFactory.addUnits(sz11, subType.create(1, japanese)));
    gameData.performChange(ChangeFactory.addUnits(sz11, transportType.create(1, japanese)));
    gameData.performChange(ChangeFactory.addUnits(sz11, infantryType.create(1, japanese)));
    gameData.performChange(ChangeFactory.addUnits(sz9, carrierType.create(1, americans)));
    gameData.performChange(ChangeFactory.addUnits(sz9, fighterType.create(2, americans)));
    // we need to initialize the original owner
    final InitializationDelegate initDel =
        (InitializationDelegate) gameData.getDelegate("initDelegate");
    initDel.setDelegateBridgeAndPlayer(bridge);
    initDel.start();
    initDel.end();
    // Get total number of defending units before the battle
    final int preCountCanada = eastCanada.getUnitCollection().size();
    final int preCountAirSz9 = sz9.getUnitCollection().getMatches(Matches.unitIsAir()).size();
    // now move to attack
    final AbstractMoveDelegate moveDelegate = gameData.getMoveDelegate();
    advanceToStep(bridge, "CombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(bridge);
    moveDelegate.start();
    moveDelegate.move(List.copyOf(sz11.getUnits()), new Route(sz11, sz9));
    moveDelegate.move(
        sz9.getUnitCollection().getUnits(infantryType, 1), new Route(sz9, eastCanada));
    moveDelegate.end();
    // fight the battle
    final BattleDelegate battle = gameData.getBattleDelegate();
    battle.setDelegateBridgeAndPlayer(bridge);
    battle.start();
    whenGetRandom(bridge).thenAnswer(withValues(0)).thenAnswer(withValues(0, 0));
    fight(battle, sz9);
    battle.end();
    // Get the total number of units that should be left after the planes retreat
    final int expectedCountCanada = eastCanada.getUnitCollection().size();
    final int postCountInt = preCountCanada + preCountAirSz9;
    // Compare the expected count with the actual number of units in landing zone
    assertEquals(expectedCountCanada, postCountInt);
  }
}
