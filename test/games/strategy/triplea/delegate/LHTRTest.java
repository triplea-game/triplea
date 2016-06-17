package games.strategy.triplea.delegate;

import static games.strategy.triplea.delegate.GameDataTestUtil.addTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.ChangeFactory;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.ITestDelegateBridge;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.random.IRandomSource;
import games.strategy.engine.random.ScriptedRandomSource;
import games.strategy.triplea.Constants;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.player.ITripleAPlayer;
import games.strategy.triplea.xml.LoadGameUtil;

public class LHTRTest {
  private GameData m_data;

  @Before
  public void setUp() throws Exception {
    m_data = LoadGameUtil.loadTestGame("lhtr_test.xml");
  }

  private ITestDelegateBridge getDelegateBridge(final PlayerID player) {
    return GameDataTestUtil.getDelegateBridge(player, m_data);
  }

  @Test
  public void testFightersCanLandOnNewPlacedCarrier() {
    final MoveDelegate delegate = (MoveDelegate) m_data.getDelegateList().getDelegate("move");
    delegate.initialize("MoveDelegate", "MoveDelegate");
    final PlayerID germans = GameDataTestUtil.germans(m_data);
    final ITestDelegateBridge bridge = getDelegateBridge(germans);
    bridge.setStepName("germanNonCombatMove");
    delegate.setDelegateBridgeAndPlayer(bridge);
    delegate.start();
    final Territory baltic = m_data.getMap().getTerritory("5 Sea Zone");
    final Territory easternEurope = m_data.getMap().getTerritory("Eastern Europe");
    final UnitType carrirType = GameDataTestUtil.carrier(m_data);
    // move a fighter to the baltic
    final Route route = new Route();
    route.setStart(easternEurope);
    route.add(baltic);
    final UnitType fighterType = GameDataTestUtil.fighter(m_data);
    delegate.move(easternEurope.getUnits().getMatches(Matches.unitIsOfType(fighterType)), route);
    // add a carrier to be produced in germany
    final TripleAUnit carrier = new TripleAUnit(carrirType, germans, m_data);
    m_data.performChange(ChangeFactory.addUnits(germans, Collections.singleton((Unit) carrier)));
    // end the move phase
    delegate.end();
    // make sure the fighter is still there
    // in lhtr fighters can hover, and carriers placed beneath them
    assertTrue(baltic.getUnits().someMatch(Matches.unitIsOfType(fighterType)));
  }

  @Test
  public void testFightersDestroyedWhenNoPendingCarriers() {
    final MoveDelegate delegate = (MoveDelegate) m_data.getDelegateList().getDelegate("move");
    delegate.initialize("MoveDelegate", "MoveDelegate");
    final PlayerID germans = GameDataTestUtil.germans(m_data);
    final ITestDelegateBridge bridge = getDelegateBridge(germans);
    bridge.setStepName("germanNonCombatMove");
    delegate.setDelegateBridgeAndPlayer(bridge);
    delegate.start();
    final Territory baltic = m_data.getMap().getTerritory("5 Sea Zone");
    final Territory easternEurope = m_data.getMap().getTerritory("Eastern Europe");
    // move a fighter to the baltic
    final Route route = new Route();
    route.setStart(easternEurope);
    route.add(baltic);
    final UnitType fighterType = GameDataTestUtil.fighter(m_data);
    delegate.move(easternEurope.getUnits().getMatches(Matches.unitIsOfType(fighterType)), route);
    // end the move phase
    delegate.end();
    // there is no pending carrier to be placed
    // the fighter cannot hover
    assertFalse(baltic.getUnits().someMatch(Matches.unitIsOfType(fighterType)));
  }

  @Test
  public void testAAGunsDontFireNonCombat() {
    final MoveDelegate delegate = (MoveDelegate) m_data.getDelegateList().getDelegate("move");
    delegate.initialize("MoveDelegate", "MoveDelegate");
    final PlayerID germans = GameDataTestUtil.germans(m_data);
    final ITestDelegateBridge bridge = getDelegateBridge(germans);
    bridge.setStepName("germanNonCombatMove");
    delegate.setDelegateBridgeAndPlayer(bridge);
    delegate.start();
    // if we try to move aa, then the game will ask us if we want to move
    // fail if we are called
    final InvocationHandler handler = new InvocationHandler() {
      @Override
      public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
        fail("method called:" + method);
        // never reached
        return null;
      }
    };
    final ITripleAPlayer player = (ITripleAPlayer) Proxy
        .newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class[] {ITripleAPlayer.class}, handler);
    bridge.setRemote(player);
    // move 1 fighter over the aa gun in caucus
    final Route route = new Route();
    route.setStart(m_data.getMap().getTerritory("Ukraine S.S.R."));
    route.add(m_data.getMap().getTerritory("Caucasus"));
    route.add(m_data.getMap().getTerritory("West Russia"));
    final List<Unit> fighter = route.getStart().getUnits().getMatches(Matches.UnitIsAir);
    delegate.move(fighter, route);
  }

  @Test
  public void testSubDefenseBonus() {
    final UnitType sub = GameDataTestUtil.submarine(m_data);
    final UnitAttachment attachment = UnitAttachment.get(sub);
    final PlayerID japanese = GameDataTestUtil.japanese(m_data);
    // before the advance, subs defend and attack at 2
    assertEquals(2, attachment.getDefense(japanese));
    assertEquals(2, attachment.getAttack(japanese));
    final ITestDelegateBridge bridge = getDelegateBridge(japanese);
    TechTracker.addAdvance(japanese, bridge,
        TechAdvance.findAdvance(TechAdvance.TECH_PROPERTY_SUPER_SUBS, m_data, japanese));
    // after tech advance, this is now 3
    assertEquals(3, attachment.getDefense(japanese));
    assertEquals(3, attachment.getAttack(japanese));
    // make sure this only changes for the player with the tech
    final PlayerID americans = GameDataTestUtil.americans(m_data);
    assertEquals(2, attachment.getDefense(americans));
    assertEquals(2, attachment.getAttack(americans));
  }

  @Test
  public void testLHTRBombingRaid() {
    final Territory germany = m_data.getMap().getTerritory("Germany");
    final Territory uk = m_data.getMap().getTerritory("United Kingdom");
    final PlayerID germans = GameDataTestUtil.germans(m_data);
    final PlayerID british = GameDataTestUtil.british(m_data);
    final BattleTracker tracker = new BattleTracker();
    final StrategicBombingRaidBattle battle = new StrategicBombingRaidBattle(germany, m_data, british, tracker);
    battle.addAttackChange(m_data.getMap().getRoute(uk, germany),
        uk.getUnits().getMatches(Matches.UnitIsStrategicBomber), null);
    addTo(germany, uk.getUnits().getMatches(Matches.UnitIsStrategicBomber));
    tracker.getBattleRecords(m_data).addBattle(british, battle.getBattleID(), germany, battle.getBattleType(), m_data);
    final ITestDelegateBridge bridge = getDelegateBridge(british);
    TechTracker.addAdvance(british, bridge,
        TechAdvance.findAdvance(TechAdvance.TECH_PROPERTY_HEAVY_BOMBER, m_data, british));
    // aa guns rolls 3, misses, bomber rolls 2 dice at 3 and 4
    bridge.setRandomSource(new ScriptedRandomSource(new int[] {2, 2, 3}));
    // if we try to move aa, then the game will ask us if we want to move
    // fail if we are called
    final InvocationHandler handler = new InvocationHandler() {
      @Override
      public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
        return null;
      }
    };
    final ITripleAPlayer player = (ITripleAPlayer) Proxy
        .newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class[] {ITripleAPlayer.class}, handler);
    bridge.setRemote(player);
    final int PUsBeforeRaid = germans.getResources().getQuantity(m_data.getResourceList().getResource(Constants.PUS));
    battle.fight(bridge);
    final int PUsAfterRaid = germans.getResources().getQuantity(m_data.getResourceList().getResource(Constants.PUS));
    // targets dice is 4, so damage is 1 + 4 = 5
    // Changed to match StrategicBombingRaidBattle changes
    assertEquals(PUsBeforeRaid - 5, PUsAfterRaid);
  }

  @Test
  public void testLHTRBombingRaid2Bombers() {
    final Territory germany = m_data.getMap().getTerritory("Germany");
    final Territory uk = m_data.getMap().getTerritory("United Kingdom");
    final PlayerID germans = GameDataTestUtil.germans(m_data);
    final PlayerID british = GameDataTestUtil.british(m_data);
    // add a unit
    final Unit bomber = GameDataTestUtil.bomber(m_data).create(british);
    final Change change = ChangeFactory.addUnits(uk, Collections.singleton(bomber));
    m_data.performChange(change);
    final BattleTracker tracker = new BattleTracker();
    final StrategicBombingRaidBattle battle = new StrategicBombingRaidBattle(germany, m_data, british, tracker);
    battle.addAttackChange(m_data.getMap().getRoute(uk, germany),
        uk.getUnits().getMatches(Matches.UnitIsStrategicBomber), null);
    addTo(germany, uk.getUnits().getMatches(Matches.UnitIsStrategicBomber));
    tracker.getBattleRecords(m_data).addBattle(british, battle.getBattleID(), germany, battle.getBattleType(), m_data);
    final ITestDelegateBridge bridge = getDelegateBridge(british);
    TechTracker.addAdvance(british, bridge,
        TechAdvance.findAdvance(TechAdvance.TECH_PROPERTY_HEAVY_BOMBER, m_data, british));
    // aa guns rolls 3,3 both miss, bomber 1 rolls 2 dice at 3,4 and bomber 2 rolls dice at 1,2
    bridge.setRandomSource(new ScriptedRandomSource(new int[] {3, 3, 2, 3, 0, 1}));
    // if we try to move aa, then the game will ask us if we want to move
    // fail if we are called
    final InvocationHandler handler = new InvocationHandler() {
      @Override
      public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
        return null;
      }
    };
    final ITripleAPlayer player = (ITripleAPlayer) Proxy
        .newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class[] {ITripleAPlayer.class}, handler);
    bridge.setRemote(player);
    final int PUsBeforeRaid = germans.getResources().getQuantity(m_data.getResourceList().getResource(Constants.PUS));
    battle.fight(bridge);
    final int PUsAfterRaid = germans.getResources().getQuantity(m_data.getResourceList().getResource(Constants.PUS));
    // targets dice is 4, so damage is 1 + 4 = 5
    // bomber 2 hits at 2, so damage is 3, for a total of 8
    // Changed to match StrategicBombingRaidBattle changes
    assertEquals(PUsBeforeRaid - 8, PUsAfterRaid);
  }
}


/**
 * a random source that throws when asked for random
 * usefule for testing
 */
class ThrowingRandomSource implements IRandomSource {
  @Override
  public int getRandom(final int max, final String annotation) {
    throw new IllegalStateException("not allowed");
  }

  @Override
  public int[] getRandom(final int max, final int count, final String annotation) {
    throw new IllegalStateException("not allowed");
  }
}
