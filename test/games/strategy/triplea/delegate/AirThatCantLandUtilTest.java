package games.strategy.triplea.delegate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.Map.Entry;

import org.junit.Before;
import org.junit.Test;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.ChangeFactory;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.ITestDelegateBridge;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.random.ScriptedRandomSource;
import games.strategy.triplea.delegate.IBattle.BattleType;
import games.strategy.triplea.player.ITripleAPlayer;
import games.strategy.triplea.xml.LoadGameUtil;

public class AirThatCantLandUtilTest {
  private GameData m_data;
  private PlayerID m_americans;
  private UnitType m_fighter;

  @Before
  public void setUp() throws Exception {
    m_data = LoadGameUtil.loadTestGame("revised_test.xml");
    m_americans = GameDataTestUtil.americans(m_data);
    m_fighter = GameDataTestUtil.fighter(m_data);
  }

  private ITestDelegateBridge getDelegateBridge(final PlayerID player) {
    return GameDataTestUtil.getDelegateBridge(player, m_data);
  }

  public static String fight(final BattleDelegate battle, final Territory territory, final boolean bombing) {
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
    final PlayerID player = m_americans;
    // everything can land
    final ITestDelegateBridge bridge = getDelegateBridge(player);
    final AirThatCantLandUtil util = new AirThatCantLandUtil(bridge);
    assertTrue(util.getTerritoriesWhereAirCantLand(player).isEmpty());
  }

  @Test
  public void testCantLandEnemyTerritory() {
    final PlayerID player = m_americans;
    final ITestDelegateBridge bridge = getDelegateBridge(player);
    final Territory balkans = m_data.getMap().getTerritory("Balkans");
    final Change addAir = ChangeFactory.addUnits(balkans, m_fighter.create(2, player));
    m_data.performChange(addAir);
    final AirThatCantLandUtil airThatCantLandUtil = new AirThatCantLandUtil(bridge);
    final Collection<Territory> cantLand = airThatCantLandUtil.getTerritoriesWhereAirCantLand(player);
    assertEquals(1, cantLand.size());
    assertEquals(balkans, cantLand.iterator().next());
    airThatCantLandUtil.removeAirThatCantLand(player, false);
    // jsut the original german fighter
    assertEquals(1, balkans.getUnits().getMatches(Matches.UnitIsAir).size());
  }

  @Test
  public void testCantLandWater() {
    final PlayerID player = m_americans;
    final ITestDelegateBridge bridge = getDelegateBridge(player);
    final Territory sz_55 = m_data.getMap().getTerritory("55 Sea Zone");
    final Change addAir = ChangeFactory.addUnits(sz_55, m_fighter.create(2, player));
    m_data.performChange(addAir);
    final AirThatCantLandUtil airThatCantLandUtil = new AirThatCantLandUtil(bridge);
    final Collection<Territory> cantLand = airThatCantLandUtil.getTerritoriesWhereAirCantLand(player);
    assertEquals(1, cantLand.size());
    assertEquals(sz_55, cantLand.iterator().next());
    airThatCantLandUtil.removeAirThatCantLand(player, false);
    assertEquals(0, sz_55.getUnits().getMatches(Matches.UnitIsAir).size());
  }

  @Test
  public void testSpareNextToFactory() {
    final PlayerID player = m_americans;
    final ITestDelegateBridge bridge = getDelegateBridge(player);
    final Territory sz_55 = m_data.getMap().getTerritory("55 Sea Zone");
    final Change addAir = ChangeFactory.addUnits(sz_55, m_fighter.create(2, player));
    m_data.performChange(addAir);
    final AirThatCantLandUtil airThatCantLandUtil = new AirThatCantLandUtil(bridge);
    airThatCantLandUtil.removeAirThatCantLand(player, true);
    assertEquals(2, sz_55.getUnits().getMatches(Matches.UnitIsAir).size());
  }

  @Test
  public void testCantLandCarrier() {
    // 1 carrier in the region, but three fighters, make sure we cant land
    final PlayerID player = m_americans;
    final ITestDelegateBridge bridge = getDelegateBridge(player);
    final Territory sz_52 = m_data.getMap().getTerritory("52 Sea Zone");
    final Change addAir = ChangeFactory.addUnits(sz_52, m_fighter.create(2, player));
    m_data.performChange(addAir);
    final AirThatCantLandUtil airThatCantLandUtil = new AirThatCantLandUtil(bridge);
    final Collection<Territory> cantLand = airThatCantLandUtil.getTerritoriesWhereAirCantLand(player);
    assertEquals(1, cantLand.size());
    assertEquals(sz_52, cantLand.iterator().next());
    airThatCantLandUtil.removeAirThatCantLand(player, false);
    // just the original american fighter, plus one that can land on the carrier
    assertEquals(2, sz_52.getUnits().getMatches(Matches.UnitIsAir).size());
  }

  @Test
  public void testCanLandNeighborCarrier() {
    final PlayerID japanese = GameDataTestUtil.japanese(m_data);
    final PlayerID americans = GameDataTestUtil.americans(m_data);
    final ITestDelegateBridge bridge = getDelegateBridge(japanese);
    // we need to initialize the original owner
    final InitializationDelegate initDel =
        (InitializationDelegate) m_data.getDelegateList().getDelegate("initDelegate");
    initDel.setDelegateBridgeAndPlayer(bridge);
    initDel.start();
    initDel.end();
    // Get necessary sea zones and unit types for this test
    final Territory sz_44 = m_data.getMap().getTerritory("44 Sea Zone");
    final Territory sz_45 = m_data.getMap().getTerritory("45 Sea Zone");
    final Territory sz_52 = m_data.getMap().getTerritory("52 Sea Zone");
    final UnitType subType = GameDataTestUtil.submarine(m_data);
    final UnitType carrierType = GameDataTestUtil.carrier(m_data);
    final UnitType fighterType = GameDataTestUtil.fighter(m_data);
    // Add units for the test
    m_data.performChange(ChangeFactory.addUnits(sz_45, subType.create(1, japanese)));
    m_data.performChange(ChangeFactory.addUnits(sz_44, carrierType.create(1, americans)));
    m_data.performChange(ChangeFactory.addUnits(sz_44, fighterType.create(1, americans)));
    // Get total number of defending units before the battle
    final Integer preCountSz_52 = sz_52.getUnits().size();
    final Integer preCountAirSz_44 = sz_44.getUnits().getMatches(Matches.UnitIsAir).size();
    // now move to attack
    final MoveDelegate moveDelegate = (MoveDelegate) m_data.getDelegateList().getDelegate("move");
    bridge.setStepName("CombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(bridge);
    moveDelegate.start();
    moveDelegate.move(sz_45.getUnits().getUnits(), m_data.getMap().getRoute(sz_45, sz_44));
    moveDelegate.end();
    // fight the battle
    final BattleDelegate battle = (BattleDelegate) m_data.getDelegateList().getDelegate("battle");
    battle.setDelegateBridgeAndPlayer(bridge);
    battle.start();
    bridge.setRandomSource(new ScriptedRandomSource(new int[] {0, 0, 0}));
    bridge.setRemote(getDummyPlayer());
    fight(battle, sz_44, false);
    battle.end();
    // Get the total number of units that should be left after the planes retreat
    final Integer expectedCountSz_52 = sz_52.getUnits().size();
    final Integer postCountInt = preCountSz_52 + preCountAirSz_44;
    // Compare the expected count with the actual number of units in landing zone
    assertEquals(expectedCountSz_52, postCountInt);
  }

  @Test
  public void testCanLandMultiNeighborCarriers() {
    final PlayerID japanese = GameDataTestUtil.japanese(m_data);
    final PlayerID americans = GameDataTestUtil.americans(m_data);
    final ITestDelegateBridge bridge = getDelegateBridge(japanese);
    // we need to initialize the original owner
    final InitializationDelegate initDel =
        (InitializationDelegate) m_data.getDelegateList().getDelegate("initDelegate");
    initDel.setDelegateBridgeAndPlayer(bridge);
    initDel.start();
    initDel.end();
    // Get necessary sea zones and unit types for this test
    final Territory sz_43 = m_data.getMap().getTerritory("43 Sea Zone");
    final Territory sz_44 = m_data.getMap().getTerritory("44 Sea Zone");
    final Territory sz_45 = m_data.getMap().getTerritory("45 Sea Zone");
    final Territory sz_52 = m_data.getMap().getTerritory("52 Sea Zone");
    final UnitType subType = GameDataTestUtil.submarine(m_data);
    final UnitType carrierType = GameDataTestUtil.carrier(m_data);
    final UnitType fighterType = GameDataTestUtil.fighter(m_data);
    // Add units for the test
    m_data.performChange(ChangeFactory.addUnits(sz_45, subType.create(1, japanese)));
    m_data.performChange(ChangeFactory.addUnits(sz_44, carrierType.create(1, americans)));
    m_data.performChange(ChangeFactory.addUnits(sz_44, fighterType.create(3, americans)));
    m_data.performChange(ChangeFactory.addUnits(sz_43, carrierType.create(1, americans)));
    // Get total number of defending units before the battle
    final Integer preCountSz_52 = sz_52.getUnits().size();
    final Integer preCountSz_43 = sz_43.getUnits().size();
    // now move to attack
    final MoveDelegate moveDelegate = (MoveDelegate) m_data.getDelegateList().getDelegate("move");
    bridge.setStepName("CombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(bridge);
    moveDelegate.start();
    moveDelegate.move(sz_45.getUnits().getUnits(), m_data.getMap().getRoute(sz_45, sz_44));
    moveDelegate.end();
    // fight the battle
    final BattleDelegate battle = (BattleDelegate) m_data.getDelegateList().getDelegate("battle");
    battle.setDelegateBridgeAndPlayer(bridge);
    battle.start();
    bridge.setRandomSource(new ScriptedRandomSource(new int[] {0, 0, 0}));
    bridge.setRemote(getDummyPlayer());
    fight(battle, sz_44, false);
    battle.end();
    // Get the total number of units that should be left after the planes retreat
    final Integer expectedCountSz_52 = sz_52.getUnits().size();
    final Integer expectedCountSz_43 = sz_43.getUnits().size();
    final Integer postCountSz_52 = preCountSz_52 + 1;
    final Integer postCountSz_43 = preCountSz_43 + 2;
    // Compare the expected count with the actual number of units in landing zone
    assertEquals(expectedCountSz_52, postCountSz_52);
    assertEquals(expectedCountSz_43, postCountSz_43);
  }

  @Test
  public void testCanLandNeighborLandV2() {
    final PlayerID japanese = GameDataTestUtil.japanese(m_data);
    final PlayerID americans = GameDataTestUtil.americans(m_data);
    final ITestDelegateBridge bridge = getDelegateBridge(japanese);
    // we need to initialize the original owner
    final InitializationDelegate initDel =
        (InitializationDelegate) m_data.getDelegateList().getDelegate("initDelegate");
    initDel.setDelegateBridgeAndPlayer(bridge);
    initDel.start();
    initDel.end();
    // Get necessary sea zones and unit types for this test
    final Territory sz_9 = m_data.getMap().getTerritory("9 Sea Zone");
    final Territory eastCanada = m_data.getMap().getTerritory("Eastern Canada");
    final Territory sz_11 = m_data.getMap().getTerritory("11 Sea Zone");
    final UnitType subType = GameDataTestUtil.submarine(m_data);
    final UnitType carrierType = GameDataTestUtil.carrier(m_data);
    final UnitType fighterType = GameDataTestUtil.fighter(m_data);
    // Add units for the test
    m_data.performChange(ChangeFactory.addUnits(sz_11, subType.create(1, japanese)));
    m_data.performChange(ChangeFactory.addUnits(sz_9, carrierType.create(1, americans)));
    m_data.performChange(ChangeFactory.addUnits(sz_9, fighterType.create(1, americans)));
    // Get total number of defending units before the battle
    final Integer preCountCanada = eastCanada.getUnits().size();
    final Integer preCountAirSz_9 = sz_9.getUnits().getMatches(Matches.UnitIsAir).size();
    // now move to attack
    final MoveDelegate moveDelegate = (MoveDelegate) m_data.getDelegateList().getDelegate("move");
    bridge.setStepName("CombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(bridge);
    moveDelegate.start();
    moveDelegate.move(sz_11.getUnits().getUnits(), m_data.getMap().getRoute(sz_11, sz_9));
    moveDelegate.end();
    // fight the battle
    final BattleDelegate battle = (BattleDelegate) m_data.getDelegateList().getDelegate("battle");
    battle.setDelegateBridgeAndPlayer(bridge);
    battle.start();
    bridge.setRandomSource(new ScriptedRandomSource(new int[] {0,}));
    bridge.setRemote(getDummyPlayer());
    fight(battle, sz_9, false);
    battle.end();
    // Get the total number of units that should be left after the planes retreat
    final Integer expectedCountCanada = eastCanada.getUnits().size();
    final Integer postCountInt = preCountCanada + preCountAirSz_9;
    // Compare the expected count with the actual number of units in landing zone
    assertEquals(expectedCountCanada, postCountInt);
  }

  @Test
  public void testCanLandNeighborLandWithRetreatedBattleV2() {
    final PlayerID japanese = GameDataTestUtil.japanese(m_data);
    final PlayerID americans = GameDataTestUtil.americans(m_data);
    final ITestDelegateBridge bridge = getDelegateBridge(japanese);
    // Get necessary sea zones and unit types for this test
    final Territory sz_9 = m_data.getMap().getTerritory("9 Sea Zone");
    final Territory eastCanada = m_data.getMap().getTerritory("Eastern Canada");
    final Territory sz_11 = m_data.getMap().getTerritory("11 Sea Zone");
    final UnitType subType = GameDataTestUtil.submarine(m_data);
    final UnitType carrierType = GameDataTestUtil.carrier(m_data);
    final UnitType fighterType = GameDataTestUtil.fighter(m_data);
    final UnitType transportType = GameDataTestUtil.transport(m_data);
    final UnitType infantryType = GameDataTestUtil.infantry(m_data);
    // Add units for the test
    m_data.performChange(ChangeFactory.addUnits(sz_11, subType.create(1, japanese)));
    m_data.performChange(ChangeFactory.addUnits(sz_11, transportType.create(1, japanese)));
    m_data.performChange(ChangeFactory.addUnits(sz_11, infantryType.create(1, japanese)));
    m_data.performChange(ChangeFactory.addUnits(sz_9, carrierType.create(1, americans)));
    m_data.performChange(ChangeFactory.addUnits(sz_9, fighterType.create(2, americans)));
    // we need to initialize the original owner
    final InitializationDelegate initDel =
        (InitializationDelegate) m_data.getDelegateList().getDelegate("initDelegate");
    initDel.setDelegateBridgeAndPlayer(bridge);
    initDel.start();
    initDel.end();
    // Get total number of defending units before the battle
    final Integer preCountCanada = eastCanada.getUnits().size();
    final Integer preCountAirSz_9 = sz_9.getUnits().getMatches(Matches.UnitIsAir).size();
    // now move to attack
    final MoveDelegate moveDelegate = (MoveDelegate) m_data.getDelegateList().getDelegate("move");
    bridge.setStepName("CombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(bridge);
    moveDelegate.start();
    moveDelegate.move(sz_11.getUnits().getUnits(), m_data.getMap().getRoute(sz_11, sz_9));
    moveDelegate.move(sz_9.getUnits().getUnits(infantryType, 1), m_data.getMap().getRoute(sz_9, eastCanada));
    moveDelegate.end();
    // fight the battle
    final BattleDelegate battle = (BattleDelegate) m_data.getDelegateList().getDelegate("battle");
    battle.setDelegateBridgeAndPlayer(bridge);
    battle.start();
    bridge.setRandomSource(new ScriptedRandomSource(new int[] {0, 0, 0}));
    bridge.setRemote(getDummyPlayer());
    fight(battle, sz_9, false);
    battle.end();
    // Get the total number of units that should be left after the planes retreat
    final Integer expectedCountCanada = eastCanada.getUnits().size();
    final Integer postCountInt = preCountCanada + preCountAirSz_9;
    // Compare the expected count with the actual number of units in landing zone
    assertEquals(expectedCountCanada, postCountInt);
  }

  /** @deprecated Use a mock object instead */
  @Deprecated
  private static ITripleAPlayer getDummyPlayer() {
    final InvocationHandler handler = new InvocationHandler() {
      @Override
      public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
        return null;
      }
    };
    return (ITripleAPlayer) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
        new Class[] {ITripleAPlayer.class}, handler);
  }
}
