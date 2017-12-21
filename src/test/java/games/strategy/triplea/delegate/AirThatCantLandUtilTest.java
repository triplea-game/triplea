package games.strategy.triplea.delegate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.Map.Entry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.ITestDelegateBridge;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.engine.random.ScriptedRandomSource;
import games.strategy.triplea.delegate.IBattle.BattleType;
import games.strategy.triplea.player.ITripleAPlayer;
import games.strategy.triplea.xml.TestMapGameData;

public class AirThatCantLandUtilTest {
  private GameData gameData;
  private PlayerID americansPlayer;
  private UnitType fighterType;

  @BeforeEach
  public void setUp() throws Exception {
    gameData = TestMapGameData.REVISED.getGameData();
    americansPlayer = GameDataTestUtil.americans(gameData);
    fighterType = GameDataTestUtil.fighter(gameData);
  }

  private ITestDelegateBridge getDelegateBridge(final PlayerID player) {
    return GameDataTestUtil.getDelegateBridge(player, gameData);
  }

  private static String fight(final BattleDelegate battle, final Territory territory, final boolean bombing) {
    for (final Entry<BattleType, Collection<Territory>> entry : battle.getBattles().getBattles().entrySet()) {
      if (entry.getKey().isBombingRun() == bombing) {
        if (entry.getValue().contains(territory)) {
          return battle.fightBattle(territory, bombing, entry.getKey());
        }
      }
    }
    throw new IllegalStateException(
        "Could not find " + (bombing ? "bombing" : "normal") + " battle in: " + territory.getName());
  }

  @Test
  public void testSimple() {
    final PlayerID player = americansPlayer;
    // everything can land
    final ITestDelegateBridge bridge = getDelegateBridge(player);
    final AirThatCantLandUtil util = new AirThatCantLandUtil(bridge);
    assertTrue(util.getTerritoriesWhereAirCantLand(player).isEmpty());
  }

  @Test
  public void testCantLandEnemyTerritory() {
    final PlayerID player = americansPlayer;
    final ITestDelegateBridge bridge = getDelegateBridge(player);
    final Territory balkans = gameData.getMap().getTerritory("Balkans");
    final Change addAir = ChangeFactory.addUnits(balkans, fighterType.create(2, player));
    gameData.performChange(addAir);
    final AirThatCantLandUtil airThatCantLandUtil = new AirThatCantLandUtil(bridge);
    final Collection<Territory> cantLand = airThatCantLandUtil.getTerritoriesWhereAirCantLand(player);
    assertEquals(1, cantLand.size());
    assertEquals(balkans, cantLand.iterator().next());
    airThatCantLandUtil.removeAirThatCantLand(player, false);
    // jsut the original german fighter
    assertEquals(1, balkans.getUnits().getMatches(Matches.unitIsAir()).size());
  }

  @Test
  public void testCantLandWater() {
    final PlayerID player = americansPlayer;
    final ITestDelegateBridge bridge = getDelegateBridge(player);
    final Territory sz55 = gameData.getMap().getTerritory("55 Sea Zone");
    final Change addAir = ChangeFactory.addUnits(sz55, fighterType.create(2, player));
    gameData.performChange(addAir);
    final AirThatCantLandUtil airThatCantLandUtil = new AirThatCantLandUtil(bridge);
    final Collection<Territory> cantLand = airThatCantLandUtil.getTerritoriesWhereAirCantLand(player);
    assertEquals(1, cantLand.size());
    assertEquals(sz55, cantLand.iterator().next());
    airThatCantLandUtil.removeAirThatCantLand(player, false);
    assertEquals(0, sz55.getUnits().getMatches(Matches.unitIsAir()).size());
  }

  @Test
  public void testSpareNextToFactory() {
    final PlayerID player = americansPlayer;
    final ITestDelegateBridge bridge = getDelegateBridge(player);
    final Territory sz55 = gameData.getMap().getTerritory("55 Sea Zone");
    final Change addAir = ChangeFactory.addUnits(sz55, fighterType.create(2, player));
    gameData.performChange(addAir);
    final AirThatCantLandUtil airThatCantLandUtil = new AirThatCantLandUtil(bridge);
    airThatCantLandUtil.removeAirThatCantLand(player, true);
    assertEquals(2, sz55.getUnits().getMatches(Matches.unitIsAir()).size());
  }

  @Test
  public void testCantLandCarrier() {
    // 1 carrier in the region, but three fighters, make sure we cant land
    final PlayerID player = americansPlayer;
    final ITestDelegateBridge bridge = getDelegateBridge(player);
    final Territory sz52 = gameData.getMap().getTerritory("52 Sea Zone");
    final Change addAir = ChangeFactory.addUnits(sz52, fighterType.create(2, player));
    gameData.performChange(addAir);
    final AirThatCantLandUtil airThatCantLandUtil = new AirThatCantLandUtil(bridge);
    final Collection<Territory> cantLand = airThatCantLandUtil.getTerritoriesWhereAirCantLand(player);
    assertEquals(1, cantLand.size());
    assertEquals(sz52, cantLand.iterator().next());
    airThatCantLandUtil.removeAirThatCantLand(player, false);
    // just the original american fighter, plus one that can land on the carrier
    assertEquals(2, sz52.getUnits().getMatches(Matches.unitIsAir()).size());
  }

  @Test
  public void testCanLandNeighborCarrier() {
    final PlayerID japanese = GameDataTestUtil.japanese(gameData);
    final PlayerID americans = GameDataTestUtil.americans(gameData);
    final ITestDelegateBridge bridge = getDelegateBridge(japanese);
    // we need to initialize the original owner
    final InitializationDelegate initDel =
        (InitializationDelegate) gameData.getDelegateList().getDelegate("initDelegate");
    initDel.setDelegateBridgeAndPlayer(bridge);
    initDel.start();
    initDel.end();
    // Get necessary sea zones and unit types for this test
    final Territory sz44 = gameData.getMap().getTerritory("44 Sea Zone");
    final Territory sz45 = gameData.getMap().getTerritory("45 Sea Zone");
    final Territory sz52 = gameData.getMap().getTerritory("52 Sea Zone");
    final UnitType subType = GameDataTestUtil.submarine(gameData);
    final UnitType carrierType = GameDataTestUtil.carrier(gameData);
    final UnitType fighterType = GameDataTestUtil.fighter(gameData);
    // Add units for the test
    gameData.performChange(ChangeFactory.addUnits(sz45, subType.create(1, japanese)));
    gameData.performChange(ChangeFactory.addUnits(sz44, carrierType.create(1, americans)));
    gameData.performChange(ChangeFactory.addUnits(sz44, fighterType.create(1, americans)));
    // Get total number of defending units before the battle
    final int preCountSz52 = sz52.getUnits().size();
    final int preCountAirSz44 = sz44.getUnits().getMatches(Matches.unitIsAir()).size();
    // now move to attack
    final MoveDelegate moveDelegate = (MoveDelegate) gameData.getDelegateList().getDelegate("move");
    bridge.setStepName("CombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(bridge);
    moveDelegate.start();
    moveDelegate.move(sz45.getUnits().getUnits(), gameData.getMap().getRoute(sz45, sz44));
    moveDelegate.end();
    // fight the battle
    final BattleDelegate battle = (BattleDelegate) gameData.getDelegateList().getDelegate("battle");
    battle.setDelegateBridgeAndPlayer(bridge);
    bridge.setRandomSource(new ScriptedRandomSource(new int[] {0, 0, 0}));
    bridge.setRemote(getDummyPlayer());
    battle.start();
    battle.end();
    // Get the total number of units that should be left after the planes retreat
    final int expectedCountSz52 = sz52.getUnits().size();
    final int postCountInt = preCountSz52 + preCountAirSz44;
    // Compare the expected count with the actual number of units in landing zone
    assertEquals(expectedCountSz52, postCountInt);
  }

  @Test
  public void testCanLandMultiNeighborCarriers() {
    final PlayerID japanese = GameDataTestUtil.japanese(gameData);
    final PlayerID americans = GameDataTestUtil.americans(gameData);
    final ITestDelegateBridge bridge = getDelegateBridge(japanese);
    // we need to initialize the original owner
    final InitializationDelegate initDel =
        (InitializationDelegate) gameData.getDelegateList().getDelegate("initDelegate");
    initDel.setDelegateBridgeAndPlayer(bridge);
    initDel.start();
    initDel.end();
    // Get necessary sea zones and unit types for this test
    final Territory sz43 = gameData.getMap().getTerritory("43 Sea Zone");
    final Territory sz44 = gameData.getMap().getTerritory("44 Sea Zone");
    final Territory sz45 = gameData.getMap().getTerritory("45 Sea Zone");
    final Territory sz52 = gameData.getMap().getTerritory("52 Sea Zone");
    final UnitType subType = GameDataTestUtil.submarine(gameData);
    final UnitType carrierType = GameDataTestUtil.carrier(gameData);
    final UnitType fighterType = GameDataTestUtil.fighter(gameData);
    // Add units for the test
    gameData.performChange(ChangeFactory.addUnits(sz45, subType.create(1, japanese)));
    gameData.performChange(ChangeFactory.addUnits(sz44, carrierType.create(1, americans)));
    gameData.performChange(ChangeFactory.addUnits(sz44, fighterType.create(3, americans)));
    gameData.performChange(ChangeFactory.addUnits(sz43, carrierType.create(1, americans)));
    // Get total number of defending units before the battle
    final int preCountSz52 = sz52.getUnits().size();
    final int preCountSz43 = sz43.getUnits().size();
    // now move to attack
    final MoveDelegate moveDelegate = (MoveDelegate) gameData.getDelegateList().getDelegate("move");
    bridge.setStepName("CombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(bridge);
    moveDelegate.start();
    moveDelegate.move(sz45.getUnits().getUnits(), gameData.getMap().getRoute(sz45, sz44));
    moveDelegate.end();
    // fight the battle
    final BattleDelegate battle = (BattleDelegate) gameData.getDelegateList().getDelegate("battle");
    battle.setDelegateBridgeAndPlayer(bridge);
    bridge.setRandomSource(new ScriptedRandomSource(new int[] {0, 0, 0}));
    bridge.setRemote(getDummyPlayer());
    battle.start();
    battle.end();
    // Get the total number of units that should be left after the planes retreat
    final int expectedCountSz52 = sz52.getUnits().size();
    final int expectedCountSz43 = sz43.getUnits().size();
    final int postCountSz52 = preCountSz52 + 1;
    final int postCountSz43 = preCountSz43 + 2;
    // Compare the expected count with the actual number of units in landing zone
    assertEquals(expectedCountSz52, postCountSz52);
    assertEquals(expectedCountSz43, postCountSz43);
  }

  @Test
  public void testCanLandNeighborLandV2() {
    final PlayerID japanese = GameDataTestUtil.japanese(gameData);
    final PlayerID americans = GameDataTestUtil.americans(gameData);
    final ITestDelegateBridge bridge = getDelegateBridge(japanese);
    // we need to initialize the original owner
    final InitializationDelegate initDel =
        (InitializationDelegate) gameData.getDelegateList().getDelegate("initDelegate");
    initDel.setDelegateBridgeAndPlayer(bridge);
    initDel.start();
    initDel.end();
    // Get necessary sea zones and unit types for this test
    final Territory sz9 = gameData.getMap().getTerritory("9 Sea Zone");
    final Territory eastCanada = gameData.getMap().getTerritory("Eastern Canada");
    final Territory sz11 = gameData.getMap().getTerritory("11 Sea Zone");
    final UnitType subType = GameDataTestUtil.submarine(gameData);
    final UnitType carrierType = GameDataTestUtil.carrier(gameData);
    final UnitType fighterType = GameDataTestUtil.fighter(gameData);
    // Add units for the test
    gameData.performChange(ChangeFactory.addUnits(sz11, subType.create(1, japanese)));
    gameData.performChange(ChangeFactory.addUnits(sz9, carrierType.create(1, americans)));
    gameData.performChange(ChangeFactory.addUnits(sz9, fighterType.create(1, americans)));
    // Get total number of defending units before the battle
    final int preCountCanada = eastCanada.getUnits().size();
    final int preCountAirSz9 = sz9.getUnits().getMatches(Matches.unitIsAir()).size();
    // now move to attack
    final MoveDelegate moveDelegate = (MoveDelegate) gameData.getDelegateList().getDelegate("move");
    bridge.setStepName("CombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(bridge);
    moveDelegate.start();
    moveDelegate.move(sz11.getUnits().getUnits(), gameData.getMap().getRoute(sz11, sz9));
    moveDelegate.end();
    // fight the battle
    final BattleDelegate battle = (BattleDelegate) gameData.getDelegateList().getDelegate("battle");
    battle.setDelegateBridgeAndPlayer(bridge);
    bridge.setRandomSource(new ScriptedRandomSource(new int[] {0,}));
    bridge.setRemote(getDummyPlayer());
    battle.start();
    battle.end();
    // Get the total number of units that should be left after the planes retreat
    final int expectedCountCanada = eastCanada.getUnits().size();
    final int postCountInt = preCountCanada + preCountAirSz9;
    // Compare the expected count with the actual number of units in landing zone
    assertEquals(expectedCountCanada, postCountInt);
  }

  @Test
  public void testCanLandNeighborLandWithRetreatedBattleV2() {
    final PlayerID japanese = GameDataTestUtil.japanese(gameData);
    final PlayerID americans = GameDataTestUtil.americans(gameData);
    final ITestDelegateBridge bridge = getDelegateBridge(japanese);
    // Get necessary sea zones and unit types for this test
    final Territory sz9 = gameData.getMap().getTerritory("9 Sea Zone");
    final Territory eastCanada = gameData.getMap().getTerritory("Eastern Canada");
    final Territory sz11 = gameData.getMap().getTerritory("11 Sea Zone");
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
        (InitializationDelegate) gameData.getDelegateList().getDelegate("initDelegate");
    initDel.setDelegateBridgeAndPlayer(bridge);
    initDel.start();
    initDel.end();
    // Get total number of defending units before the battle
    final int preCountCanada = eastCanada.getUnits().size();
    final int preCountAirSz9 = sz9.getUnits().getMatches(Matches.unitIsAir()).size();
    // now move to attack
    final MoveDelegate moveDelegate = (MoveDelegate) gameData.getDelegateList().getDelegate("move");
    bridge.setStepName("CombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(bridge);
    moveDelegate.start();
    moveDelegate.move(sz11.getUnits().getUnits(), gameData.getMap().getRoute(sz11, sz9));
    moveDelegate.move(sz9.getUnits().getUnits(infantryType, 1), gameData.getMap().getRoute(sz9, eastCanada));
    moveDelegate.end();
    // fight the battle
    final BattleDelegate battle = (BattleDelegate) gameData.getDelegateList().getDelegate("battle");
    battle.setDelegateBridgeAndPlayer(bridge);
    battle.start();
    bridge.setRandomSource(new ScriptedRandomSource(new int[] {0, 0, 0}));
    bridge.setRemote(getDummyPlayer());
    fight(battle, sz9, false);
    battle.end();
    // Get the total number of units that should be left after the planes retreat
    final int expectedCountCanada = eastCanada.getUnits().size();
    final int postCountInt = preCountCanada + preCountAirSz9;
    // Compare the expected count with the actual number of units in landing zone
    assertEquals(expectedCountCanada, postCountInt);
  }

  /**
   * @deprecated Use a mock object instead.
   */
  @Deprecated
  private static ITripleAPlayer getDummyPlayer() {
    return (ITripleAPlayer) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
        new Class<?>[] {ITripleAPlayer.class}, (p, m, a) -> null);
  }
}
