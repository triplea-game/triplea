package games.strategy.triplea.delegate;

import static games.strategy.triplea.delegate.BattleStepStrings.ATTACKER_WITHDRAW;
import static games.strategy.triplea.delegate.BattleStepStrings.FIRE;
import static games.strategy.triplea.delegate.BattleStepStrings.REMOVE_CASUALTIES;
import static games.strategy.triplea.delegate.BattleStepStrings.REMOVE_SNEAK_ATTACK_CASUALTIES;
import static games.strategy.triplea.delegate.BattleStepStrings.SELECT_CASUALTIES;
import static games.strategy.triplea.delegate.BattleStepStrings.SELECT_SUB_CASUALTIES;
import static games.strategy.triplea.delegate.BattleStepStrings.SUBS_FIRE;
import static games.strategy.triplea.delegate.BattleStepStrings.SUBS_SUBMERGE;
import static games.strategy.triplea.delegate.GameDataTestUtil.aaGun;
import static games.strategy.triplea.delegate.GameDataTestUtil.addTo;
import static games.strategy.triplea.delegate.GameDataTestUtil.americans;
import static games.strategy.triplea.delegate.GameDataTestUtil.armour;
import static games.strategy.triplea.delegate.GameDataTestUtil.assertError;
import static games.strategy.triplea.delegate.GameDataTestUtil.assertMoveError;
import static games.strategy.triplea.delegate.GameDataTestUtil.assertValid;
import static games.strategy.triplea.delegate.GameDataTestUtil.battleDelegate;
import static games.strategy.triplea.delegate.GameDataTestUtil.battleship;
import static games.strategy.triplea.delegate.GameDataTestUtil.bidPlaceDelegate;
import static games.strategy.triplea.delegate.GameDataTestUtil.bomber;
import static games.strategy.triplea.delegate.GameDataTestUtil.british;
import static games.strategy.triplea.delegate.GameDataTestUtil.destroyer;
import static games.strategy.triplea.delegate.GameDataTestUtil.germans;
import static games.strategy.triplea.delegate.GameDataTestUtil.getIndex;
import static games.strategy.triplea.delegate.GameDataTestUtil.infantry;
import static games.strategy.triplea.delegate.GameDataTestUtil.japanese;
import static games.strategy.triplea.delegate.GameDataTestUtil.load;
import static games.strategy.triplea.delegate.GameDataTestUtil.makeGameLowLuck;
import static games.strategy.triplea.delegate.GameDataTestUtil.move;
import static games.strategy.triplea.delegate.GameDataTestUtil.moveDelegate;
import static games.strategy.triplea.delegate.GameDataTestUtil.placeDelegate;
import static games.strategy.triplea.delegate.GameDataTestUtil.removeFrom;
import static games.strategy.triplea.delegate.GameDataTestUtil.submarine;
import static games.strategy.triplea.delegate.GameDataTestUtil.techDelegate;
import static games.strategy.triplea.delegate.GameDataTestUtil.territory;
import static games.strategy.triplea.delegate.GameDataTestUtil.transport;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.ChangeFactory;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.ITestDelegateBridge;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.TechnologyFrontier;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.random.ScriptedRandomSource;
import games.strategy.net.GUID;
import games.strategy.triplea.Constants;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.attachments.TechAttachment;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.IBattle.BattleType;
import games.strategy.triplea.delegate.dataObjects.CasualtyDetails;
import games.strategy.triplea.delegate.dataObjects.CasualtyList;
import games.strategy.triplea.delegate.dataObjects.PlaceableUnits;
import games.strategy.triplea.delegate.dataObjects.TechResults;
import games.strategy.triplea.player.ITripleAPlayer;
import games.strategy.triplea.util.DummyTripleAPlayer;
import games.strategy.triplea.xml.LoadGameUtil;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.CompositeMatchOr;
import games.strategy.util.Match;

public class RevisedTest {
  private GameData m_data;

  @Before
  public void setUp() throws Exception {
    m_data = LoadGameUtil.loadTestGame("revised_test.xml");
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
  public void testMoveBadRoute() {
    final PlayerID british = GameDataTestUtil.british(m_data);
    final Territory sz1 = m_data.getMap().getTerritory("1 Sea Zone");
    final Territory sz11 = m_data.getMap().getTerritory("11 Sea Zone");
    final Territory sz9 = m_data.getMap().getTerritory("9 Sea Zone");
    final ITestDelegateBridge bridge = getDelegateBridge(british);
    bridge.setStepName("NonCombatMove");
    moveDelegate(m_data).setDelegateBridgeAndPlayer(bridge);
    moveDelegate(m_data).start();
    final String error = moveDelegate(m_data).move(sz1.getUnits().getUnits(), new Route(sz1, sz11, sz9));
    assertTrue(error != null);
  }

  @Test
  public void testAlliedNeighbors() {
    final PlayerID americans = americans(m_data);
    final Territory centralUs = territory("Central United States", m_data);
    final Set<Territory> enemyNeighbors =
        m_data.getMap().getNeighbors(centralUs, Matches.isTerritoryEnemy(americans, m_data));
    assertTrue(enemyNeighbors.isEmpty());
  }

  @Test
  public void testSubAdvance() {
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
    assertEquals(2, attachment.getDefense(japanese));
    assertEquals(3, attachment.getAttack(japanese));
  }

  @Test
  public void testMoveThroughSubmergedSubs() {
    final PlayerID british = GameDataTestUtil.british(m_data);
    final Territory sz1 = m_data.getMap().getTerritory("1 Sea Zone");
    final Territory sz7 = m_data.getMap().getTerritory("7 Sea Zone");
    final Territory sz8 = m_data.getMap().getTerritory("8 Sea Zone");
    final TripleAUnit sub = (TripleAUnit) sz8.getUnits().iterator().next();
    sub.setSubmerged(true);
    // now move to attack it
    final MoveDelegate moveDelegate = (MoveDelegate) m_data.getDelegateList().getDelegate("move");
    final ITestDelegateBridge bridge = getDelegateBridge(british);
    bridge.setStepName("NonCombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(bridge);
    moveDelegate.start();
    // the transport can enter sz 8
    // since the sub is submerged
    final Route m1 = new Route(sz1, sz8);
    assertNull(moveDelegate.move(sz1.getUnits().getUnits(), m1));
    // the transport can now leave sz8
    final Route m2 = new Route(sz8, sz7);
    final String error = moveDelegate.move(sz8.getUnits().getMatches(Matches.unitIsOwnedBy(british)), m2);
    assertNull(error, error);
  }

  @Test
  public void testRetreatBug() {
    final PlayerID russians = GameDataTestUtil.russians(m_data);
    final PlayerID americans = GameDataTestUtil.americans(m_data);
    final ITestDelegateBridge bridge = getDelegateBridge(russians);
    // we need to initialize the original owner
    final InitializationDelegate initDel =
        (InitializationDelegate) m_data.getDelegateList().getDelegate("initDelegate");
    initDel.setDelegateBridgeAndPlayer(bridge);
    initDel.start();
    initDel.end();
    // make sinkian japanese owned, put one infantry in it
    final Territory sinkiang = m_data.getMap().getTerritory("Sinkiang");
    m_data.performChange(ChangeFactory.removeUnits(sinkiang, sinkiang.getUnits().getUnits()));
    final PlayerID japanese = GameDataTestUtil.japanese(m_data);
    sinkiang.setOwner(japanese);
    final UnitType infantryType = GameDataTestUtil.infantry(m_data);
    m_data.performChange(ChangeFactory.addUnits(sinkiang, infantryType.create(1, japanese)));
    // now move to attack it
    final MoveDelegate moveDelegate = (MoveDelegate) m_data.getDelegateList().getDelegate("move");
    bridge.setStepName("CombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(bridge);
    moveDelegate.start();
    final Territory novo = m_data.getMap().getTerritory("Novosibirsk");
    moveDelegate.move(novo.getUnits().getUnits(), m_data.getMap().getRoute(novo, sinkiang));
    moveDelegate.end();
    final BattleDelegate battle = (BattleDelegate) m_data.getDelegateList().getDelegate("battle");
    battle.setDelegateBridgeAndPlayer(bridge);
    battle.start();
    // fight the battle
    bridge.setRandomSource(new ScriptedRandomSource(new int[] {0, 0, 0}));
    bridge.setRemote(getDummyPlayer());
    fight(battle, sinkiang, false);
    battle.end();
    assertEquals(sinkiang.getOwner(), americans);
    assertTrue(battle.getBattleTracker().wasConquered(sinkiang));
    bridge.setStepName("NonCombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(bridge);
    moveDelegate.start();
    final Territory russia = m_data.getMap().getTerritory("Russia");
    // move two tanks from russia, then undo
    final Route r = new Route();
    r.setStart(russia);
    r.add(novo);
    r.add(sinkiang);
    assertNull(moveDelegate.move(russia.getUnits().getMatches(Matches.UnitCanBlitz), r));
    moveDelegate.undoMove(0);
    assertTrue(battle.getBattleTracker().wasConquered(sinkiang));
    // now move the planes into the territory
    assertNull(moveDelegate.move(russia.getUnits().getMatches(Matches.UnitIsAir), r));
    // make sure they can't land, they can't because the territory was conquered
    assertEquals(1, moveDelegate.getTerritoriesWhereAirCantLand().size());
  }

  @Test
  public void testContinuedBattles() {
    final PlayerID russians = GameDataTestUtil.russians(m_data);
    final PlayerID germans = GameDataTestUtil.germans(m_data);
    final ITestDelegateBridge bridge = getDelegateBridge(germans);
    final MoveDelegate moveDelegate = (MoveDelegate) m_data.getDelegateList().getDelegate("move");
    bridge.setStepName("CombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(bridge);
    moveDelegate.start();
    // set up battle
    final Territory germany = m_data.getMap().getTerritory("Germany");
    final Territory karelia = m_data.getMap().getTerritory("Karelia S.S.R.");
    final Territory sz5 = m_data.getMap().getTerritory("5 Sea Zone");
    m_data.performChange(ChangeFactory.removeUnits(sz5, sz5.getUnits().getUnits()));
    final UnitType infantryType = GameDataTestUtil.infantry(m_data);
    final UnitType subType = GameDataTestUtil.submarine(m_data);
    final UnitType trnType = GameDataTestUtil.transport(m_data);
    m_data.performChange(ChangeFactory.addUnits(sz5, subType.create(1, germans)));
    m_data.performChange(ChangeFactory.addUnits(sz5, trnType.create(1, germans)));
    m_data.performChange(ChangeFactory.addUnits(sz5, subType.create(1, russians)));
    // submerge the russian sub
    final TripleAUnit sub =
        (TripleAUnit) Match.getMatches(sz5.getUnits().getUnits(), Matches.unitIsOwnedBy(russians)).iterator().next();
    sub.setSubmerged(true);
    // now move an infantry through the sz
    String results =
        moveDelegate.move(Match.getNMatches(germany.getUnits().getUnits(), 1, Matches.unitIsOfType(infantryType)),
            m_data.getMap().getRoute(germany, sz5),
            Match.getMatches(sz5.getUnits().getUnits(), Matches.unitIsOfType(trnType)));
    assertNull(results);
    results = moveDelegate.move(Match.getNMatches(sz5.getUnits().getUnits(), 1, Matches.unitIsOfType(infantryType)),
        m_data.getMap().getRoute(sz5, karelia));
    assertNull(results);
    moveDelegate.end();
    final BattleDelegate battle = (BattleDelegate) m_data.getDelegateList().getDelegate("battle");
    battle.setDelegateBridgeAndPlayer(bridge);
    battle.start();
    final BattleTracker tracker = AbstractMoveDelegate.getBattleTracker(m_data);
    // The battle should NOT be empty
    assertTrue(tracker.hasPendingBattle(sz5, false));
    assertFalse(tracker.getPendingBattle(sz5, false, null).isEmpty());
    battle.end();
  }

  @Test
  public void testLoadAlliedTransports() {
    final PlayerID british = british(m_data);
    final PlayerID americans = americans(m_data);
    final Territory uk = territory("United Kingdom", m_data);
    final ITestDelegateBridge bridge = getDelegateBridge(british);
    bridge.setStepName("CombatMove");
    moveDelegate(m_data).setDelegateBridgeAndPlayer(bridge);
    moveDelegate(m_data).start();
    // create 2 us infantry
    addTo(uk, infantry(m_data).create(2, americans));
    // try to load them on the british players turn
    final Territory sz2 = territory("2 Sea Zone", m_data);
    final String error = moveDelegate(m_data).move(uk.getUnits().getMatches(Matches.unitIsOwnedBy(americans)),
        new Route(uk, sz2), sz2.getUnits().getMatches(Matches.UnitIsTransport));
    // should not be able to load on british turn, only on american turn
    assertFalse(error == null);
  }

  @Test
  public void testBidPlace() {
    final ITestDelegateBridge bridge = getDelegateBridge(british(m_data));
    bridge.setStepName("BidPlace");
    bidPlaceDelegate(m_data).setDelegateBridgeAndPlayer(bridge);
    bidPlaceDelegate(m_data).start();
    // create 20 british infantry
    addTo(british(m_data), infantry(m_data).create(20, british(m_data)), m_data);
    final Territory uk = territory("United Kingdom", m_data);
    final Collection<Unit> units = british(m_data).getUnits().getUnits();
    final PlaceableUnits placeable = bidPlaceDelegate(m_data).getPlaceableUnits(units, uk);
    assertEquals(20, placeable.getMaxUnits());
    assertNull(placeable.getErrorMessage());
    final String error = bidPlaceDelegate(m_data).placeUnits(units, uk);
    assertNull(error);
  }

  @Test
  public void testBombingRaid() {
    final MoveDelegate moveDelegate = (MoveDelegate) m_data.getDelegateList().getDelegate("move");
    final ITestDelegateBridge bridge = getDelegateBridge(british(m_data));
    bridge.setStepName("CombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(bridge);
    moveDelegate.start();
    bridge.setRemote(new DummyTripleAPlayer() {
      @Override
      public boolean shouldBomberBomb(final Territory territory) {
        return true;
      }
    });
    final Territory uk = territory("United Kingdom", m_data);
    final Territory germany = territory("Germany", m_data);
    final Route route = new Route(uk, territory("6 Sea Zone", m_data), territory("5 Sea Zone", m_data), germany);
    final String error = moveDelegate.move(uk.getUnits().getMatches(Matches.UnitIsStrategicBomber), route);
    assertNull(error);
    final BattleTracker tracker = AbstractMoveDelegate.getBattleTracker(m_data);
    // there should be a bombing battle
    assertTrue(tracker.hasPendingBattle(germany, true));
    // there should not be a normal battle
    assertFalse(tracker.hasPendingBattle(germany, false));
    // start the battle phase, this should not add a new battle
    moveDelegate.end();
    battleDelegate(m_data).setDelegateBridgeAndPlayer(bridge);
    battleDelegate(m_data).start();
    // there should be a bombing battle
    assertTrue(tracker.hasPendingBattle(germany, true));
    // there should not be a normal battle
    assertFalse(tracker.hasPendingBattle(germany, false));
  }

  @Test
  public void testOverFlyBombersDies() {
    final PlayerID british = british(m_data);
    final MoveDelegate moveDelegate = (MoveDelegate) m_data.getDelegateList().getDelegate("move");
    final ITestDelegateBridge bridge = getDelegateBridge(british);
    bridge.setStepName("CombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(bridge);
    moveDelegate.start();
    bridge.setRemote(new DummyTripleAPlayer() {
      @Override
      public boolean confirmMoveInFaceOfAA(final Collection<Territory> aaFiringTerritories) {
        return true;
      }
    });
    bridge.setRandomSource(new ScriptedRandomSource(0));
    final Territory uk = territory("United Kingdom", m_data);
    final Territory we = territory("Western Europe", m_data);
    final Territory se = territory("Southern Europe", m_data);
    final Route route = new Route(uk, territory("7 Sea Zone", m_data), we, se);
    move(uk.getUnits().getMatches(Matches.UnitIsStrategicBomber), route);
    // the aa gun should have fired. the bomber no longer exists
    assertTrue(se.getUnits().getMatches(Matches.UnitIsStrategicBomber).isEmpty());
    assertTrue(we.getUnits().getMatches(Matches.UnitIsStrategicBomber).isEmpty());
    assertTrue(uk.getUnits().getMatches(Matches.UnitIsStrategicBomber).isEmpty());
  }

  @Test
  public void testMultipleOverFlyBombersDies() {
    final PlayerID british = british(m_data);
    final MoveDelegate moveDelegate = (MoveDelegate) m_data.getDelegateList().getDelegate("move");
    final ITestDelegateBridge bridge = getDelegateBridge(british);
    bridge.setStepName("CombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(bridge);
    moveDelegate.start();
    bridge.setRemote(new DummyTripleAPlayer() {
      @Override
      public boolean confirmMoveInFaceOfAA(final Collection<Territory> aaFiringTerritories) {
        return true;
      }
    });
    bridge.setRandomSource(new ScriptedRandomSource(0, 4));
    final Territory uk = territory("United Kingdom", m_data);
    final Territory sz7 = territory("7 Sea Zone", m_data);
    final Territory we = territory("Western Europe", m_data);
    final Territory se = territory("Southern Europe", m_data);
    final Territory balk = territory("Balkans", m_data);
    addTo(uk, bomber(m_data).create(1, british));
    final Route route = new Route(uk, sz7, we, se, balk);
    move(uk.getUnits().getMatches(Matches.UnitIsStrategicBomber), route);
    // the aa gun should have fired (one hit, one miss in each territory overflown). the bombers no longer exists
    assertTrue(uk.getUnits().getMatches(Matches.UnitIsStrategicBomber).isEmpty());
    assertTrue(we.getUnits().getMatches(Matches.UnitIsStrategicBomber).isEmpty());
    assertTrue(se.getUnits().getMatches(Matches.UnitIsStrategicBomber).isEmpty());
    assertTrue(balk.getUnits().getMatches(Matches.UnitIsStrategicBomber).isEmpty());
  }

  @Test
  public void testOverFlyBombersJoiningBattleDie() {
    // a bomber flies over aa to join a battle, gets hit,
    // it should not appear in the battle
    final PlayerID british = british(m_data);
    final MoveDelegate moveDelegate = (MoveDelegate) m_data.getDelegateList().getDelegate("move");
    final ITestDelegateBridge bridge = getDelegateBridge(british);
    bridge.setStepName("CombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(bridge);
    moveDelegate.start();
    bridge.setRemote(new DummyTripleAPlayer() {
      @Override
      public boolean confirmMoveInFaceOfAA(final Collection<Territory> aaFiringTerritories) {
        return true;
      }
    });
    bridge.setRandomSource(new ScriptedRandomSource(0));
    final Territory uk = territory("United Kingdom", m_data);
    final Territory we = territory("Western Europe", m_data);
    final Territory se = territory("Southern Europe", m_data);
    final Territory sz14 = territory("14 Sea Zone", m_data);
    final Territory sz15 = territory("15 Sea Zone", m_data);
    final Territory egypt = territory("Anglo Egypt", m_data);
    // start a battle in se
    removeFrom(sz14, sz14.getUnits().getUnits());
    addTo(sz15, transport(m_data).create(1, british));
    load(egypt.getUnits().getMatches(Matches.UnitIsInfantry), new Route(egypt, sz15));
    move(sz15.getUnits().getUnits(), new Route(sz15, sz14));
    move(sz14.getUnits().getMatches(Matches.UnitIsInfantry), new Route(sz14, se));
    final Route route = new Route(uk, territory("7 Sea Zone", m_data), we, se);
    move(uk.getUnits().getMatches(Matches.UnitIsStrategicBomber), route);
    // the aa gun should have fired and hit
    assertTrue(se.getUnits().getMatches(Matches.UnitIsStrategicBomber).isEmpty());
    assertTrue(we.getUnits().getMatches(Matches.UnitIsStrategicBomber).isEmpty());
    assertTrue(uk.getUnits().getMatches(Matches.UnitIsStrategicBomber).isEmpty());
  }

  @Test
  public void testTransportAttack() {
    final Territory sz14 = m_data.getMap().getTerritory("14 Sea Zone");
    final Territory sz13 = m_data.getMap().getTerritory("13 Sea Zone");
    final PlayerID germans = GameDataTestUtil.germans(m_data);
    final MoveDelegate moveDelegate = (MoveDelegate) m_data.getDelegateList().getDelegate("move");
    final ITestDelegateBridge bridge = getDelegateBridge(germans);
    bridge.setStepName("CombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(bridge);
    moveDelegate.start();
    final Route sz14To13 = new Route();
    sz14To13.setStart(sz14);
    sz14To13.add(sz13);
    final List<Unit> transports = sz14.getUnits().getMatches(Matches.UnitIsTransport);
    assertEquals(1, transports.size());
    final String error = moveDelegate.move(transports, sz14To13);
    assertNull(error, error);
  }

  @Test
  public void testLoadUndo() {
    final Territory sz5 = m_data.getMap().getTerritory("5 Sea Zone");
    final Territory eastEurope = m_data.getMap().getTerritory("Eastern Europe");
    final UnitType infantryType = GameDataTestUtil.infantry(m_data);
    final PlayerID germans = GameDataTestUtil.germans(m_data);
    final MoveDelegate moveDelegate = (MoveDelegate) m_data.getDelegateList().getDelegate("move");
    final ITestDelegateBridge bridge = getDelegateBridge(germans);
    bridge.setStepName("CombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(bridge);
    moveDelegate.start();
    final Route eeToSz5 = new Route();
    eeToSz5.setStart(eastEurope);
    eeToSz5.add(sz5);
    // load the transport in the baltic
    final List<Unit> infantry = eastEurope.getUnits().getMatches(Matches.unitIsOfType(infantryType));
    assertEquals(2, infantry.size());
    final TripleAUnit transport = (TripleAUnit) sz5.getUnits().getMatches(Matches.UnitIsTransport).get(0);
    final String error = moveDelegate.move(infantry, eeToSz5, Collections.<Unit>singletonList(transport));
    assertNull(error, error);
    // make sure the transport was loaded
    assertTrue(moveDelegate.getMovesMade().get(0).wasTransportLoaded(transport));
    // make sure it was laoded
    assertTrue(transport.getTransporting().containsAll(infantry));
    assertTrue(((TripleAUnit) infantry.get(0)).getWasLoadedThisTurn());
    // udo the move
    moveDelegate.undoMove(0);
    // make sure that loaded is not set
    assertTrue(transport.getTransporting().isEmpty());
    assertFalse(((TripleAUnit) infantry.get(0)).getWasLoadedThisTurn());
  }

  @Test
  public void testLoadDependencies() {
    final Territory sz5 = m_data.getMap().getTerritory("5 Sea Zone");
    final Territory eastEurope = m_data.getMap().getTerritory("Eastern Europe");
    final Territory norway = m_data.getMap().getTerritory("Norway");
    final UnitType infantryType = GameDataTestUtil.infantry(m_data);
    final PlayerID germans = GameDataTestUtil.germans(m_data);
    final MoveDelegate moveDelegate = (MoveDelegate) m_data.getDelegateList().getDelegate("move");
    final ITestDelegateBridge bridge = getDelegateBridge(germans);
    bridge.setStepName("CombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(bridge);
    moveDelegate.start();
    final Route eeToSz5 = new Route();
    eeToSz5.setStart(eastEurope);
    eeToSz5.add(sz5);
    // load the transport in the baltic
    final List<Unit> infantry = eastEurope.getUnits().getMatches(Matches.unitIsOfType(infantryType));
    assertEquals(2, infantry.size());
    final TripleAUnit transport = (TripleAUnit) sz5.getUnits().getMatches(Matches.UnitIsTransport).get(0);
    // load the transport
    String error = moveDelegate.move(infantry, eeToSz5, Collections.<Unit>singletonList(transport));
    assertNull(error, error);
    final Route sz5ToNorway = new Route();
    sz5ToNorway.setStart(sz5);
    sz5ToNorway.add(norway);
    // move the infantry in two steps
    error = moveDelegate.move(infantry.subList(0, 1), sz5ToNorway);
    assertNull(error);
    error = moveDelegate.move(infantry.subList(1, 2), sz5ToNorway);
    assertNull(error);
    assertEquals(3, moveDelegate.getMovesMade().size());
    // the load
    final UndoableMove move1 = moveDelegate.getMovesMade().get(0);
    // the first unload
    // AbstractUndoableMove move2 = moveDelegate.getMovesMade().get(0);
    // the second unload must be done first
    assertFalse(move1.getcanUndo());
    error = moveDelegate.undoMove(2);
    assertNull(error);
    // the second unload must be done first
    assertFalse(move1.getcanUndo());
    error = moveDelegate.undoMove(1);
    assertNull(error);
    // we can now be undone
    assertTrue(move1.getcanUndo());
  }

  @Test
  public void testLoadUndoInWrongOrder() {
    final Territory sz5 = m_data.getMap().getTerritory("5 Sea Zone");
    final Territory eastEurope = m_data.getMap().getTerritory("Eastern Europe");
    final UnitType infantryType = GameDataTestUtil.infantry(m_data);
    final PlayerID germans = GameDataTestUtil.germans(m_data);
    final MoveDelegate moveDelegate = (MoveDelegate) m_data.getDelegateList().getDelegate("move");
    final ITestDelegateBridge bridge = getDelegateBridge(germans);
    bridge.setStepName("CombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(bridge);
    moveDelegate.start();
    final Route eeToSz5 = new Route();
    eeToSz5.setStart(eastEurope);
    eeToSz5.add(sz5);
    // load the transport in the baltic
    final List<Unit> infantry = eastEurope.getUnits().getMatches(Matches.unitIsOfType(infantryType));
    assertEquals(2, infantry.size());
    final TripleAUnit transport = (TripleAUnit) sz5.getUnits().getMatches(Matches.UnitIsTransport).get(0);
    // load the transports
    // in two moves
    String error = moveDelegate.move(infantry.subList(0, 1), eeToSz5, Collections.<Unit>singletonList(transport));
    assertNull(error, error);
    error = moveDelegate.move(infantry.subList(1, 2), eeToSz5, Collections.<Unit>singletonList(transport));
    assertNull(error, error);
    // make sure the transport was loaded
    assertTrue(moveDelegate.getMovesMade().get(0).wasTransportLoaded(transport));
    assertTrue(moveDelegate.getMovesMade().get(1).wasTransportLoaded(transport));
    // udo the moves in reverse order
    moveDelegate.undoMove(0);
    moveDelegate.undoMove(0);
    // make sure that loaded is not set
    assertTrue(transport.getTransporting().isEmpty());
    assertFalse(((TripleAUnit) infantry.get(0)).getWasLoadedThisTurn());
  }

  @Test
  public void testLoadUnloadAlliedTransport() {
    // you cant load and unload an allied transport the same turn
    final UnitType infantryType = GameDataTestUtil.infantry(m_data);
    final Territory eastEurope = m_data.getMap().getTerritory("Eastern Europe");
    // add japanese infantry to eastern europe
    final PlayerID japanese = GameDataTestUtil.japanese(m_data);
    final Change change = ChangeFactory.addUnits(eastEurope, infantryType.create(1, japanese));
    m_data.performChange(change);
    final Territory sz5 = m_data.getMap().getTerritory("5 Sea Zone");
    final MoveDelegate moveDelegate = (MoveDelegate) m_data.getDelegateList().getDelegate("move");
    final ITestDelegateBridge bridge = getDelegateBridge(japanese);
    bridge.setStepName("CombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(bridge);
    moveDelegate.start();
    final Route eeToSz5 = new Route();
    eeToSz5.setStart(eastEurope);
    eeToSz5.add(sz5);
    // load the transport in the baltic
    final List<Unit> infantry = eastEurope.getUnits()
        .getMatches(new CompositeMatchAnd<>(Matches.unitIsOfType(infantryType), Matches.unitIsOwnedBy(japanese)));
    assertEquals(1, infantry.size());
    final TripleAUnit transport = (TripleAUnit) sz5.getUnits().getMatches(Matches.UnitIsTransport).get(0);
    String error = moveDelegate.move(infantry, eeToSz5, Collections.<Unit>singletonList(transport));
    assertNull(error, error);
    // try to unload
    final Route sz5ToEee = new Route();
    sz5ToEee.setStart(sz5);
    sz5ToEee.add(eastEurope);
    error = moveDelegate.move(infantry, sz5ToEee);
    assertEquals(MoveValidator.CANNOT_LOAD_AND_UNLOAD_AN_ALLIED_TRANSPORT_IN_THE_SAME_ROUND, error);
  }

  @Test
  public void testUnloadMultipleTerritories() {
    // in revised a transport may only unload to 1 territory.
    final Territory sz5 = m_data.getMap().getTerritory("5 Sea Zone");
    final Territory eastEurope = m_data.getMap().getTerritory("Eastern Europe");
    final UnitType infantryType = GameDataTestUtil.infantry(m_data);
    final PlayerID germans = GameDataTestUtil.germans(m_data);
    final MoveDelegate moveDelegate = (MoveDelegate) m_data.getDelegateList().getDelegate("move");
    final ITestDelegateBridge bridge = getDelegateBridge(germans);
    bridge.setStepName("CombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(bridge);
    moveDelegate.start();
    final Route eeToSz5 = new Route();
    eeToSz5.setStart(eastEurope);
    eeToSz5.add(sz5);
    // load the transport in the baltic
    final List<Unit> infantry = eastEurope.getUnits().getMatches(Matches.unitIsOfType(infantryType));
    assertEquals(2, infantry.size());
    final TripleAUnit transport = (TripleAUnit) sz5.getUnits().getMatches(Matches.UnitIsTransport).get(0);
    String error = moveDelegate.move(infantry, eeToSz5, Collections.<Unit>singletonList(transport));
    assertNull(error, error);
    // unload one infantry to Norway
    final Territory norway = m_data.getMap().getTerritory("Norway");
    final Route sz5ToNorway = new Route();
    sz5ToNorway.setStart(sz5);
    sz5ToNorway.add(norway);
    error = moveDelegate.move(infantry.subList(0, 1), sz5ToNorway);
    assertNull(error, error);
    // make sure the transport was unloaded
    assertTrue(moveDelegate.getMovesMade().get(1).wasTransportUnloaded(transport));
    // try to unload the other infantry somewhere else, an error occurs
    final Route sz5ToEE = new Route();
    sz5ToEE.setStart(sz5);
    sz5ToEE.add(eastEurope);
    error = moveDelegate.move(infantry.subList(1, 2), sz5ToEE);
    assertNotNull(error, error);
    assertTrue(error.startsWith(MoveValidator.TRANSPORT_HAS_ALREADY_UNLOADED_UNITS_TO));
    // end the round
    moveDelegate.end();
    bridge.setStepName("NonCombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(bridge);
    moveDelegate.start();
    moveDelegate.end();
    bridge.setStepName("CombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(bridge);
    moveDelegate.start();
    // a new round, the move should work
    moveDelegate.setDelegateBridgeAndPlayer(bridge);
    moveDelegate.start();
    error = moveDelegate.move(infantry.subList(1, 2), sz5ToEE);
    assertNull(error);
  }

  @Test
  public void testUnloadInPreviousPhase() {
    // a transport may not unload in both combat and non combat
    final Territory sz5 = m_data.getMap().getTerritory("5 Sea Zone");
    final Territory eastEurope = m_data.getMap().getTerritory("Eastern Europe");
    final UnitType infantryType = GameDataTestUtil.infantry(m_data);
    final PlayerID germans = GameDataTestUtil.germans(m_data);
    final MoveDelegate moveDelegate = (MoveDelegate) m_data.getDelegateList().getDelegate("move");
    final ITestDelegateBridge bridge = getDelegateBridge(germans);
    bridge.setStepName("CombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(bridge);
    moveDelegate.start();
    final Route eeToSz5 = new Route();
    eeToSz5.setStart(eastEurope);
    eeToSz5.add(sz5);
    // load the transport in the baltic
    final List<Unit> infantry = eastEurope.getUnits().getMatches(Matches.unitIsOfType(infantryType));
    assertEquals(2, infantry.size());
    final TripleAUnit transport = (TripleAUnit) sz5.getUnits().getMatches(Matches.UnitIsTransport).get(0);
    String error = moveDelegate.move(infantry, eeToSz5, Collections.<Unit>singletonList(transport));
    assertNull(error, error);
    // unload one infantry to Norway
    final Territory norway = m_data.getMap().getTerritory("Norway");
    final Route sz5ToNorway = new Route();
    sz5ToNorway.setStart(sz5);
    sz5ToNorway.add(norway);
    error = moveDelegate.move(infantry.subList(0, 1), sz5ToNorway);
    assertNull(error, error);
    assertTrue(((TripleAUnit) infantry.get(0)).getWasUnloadedInCombatPhase());
    // start non combat
    moveDelegate.end();
    bridge.setStepName("germanNonCombatMove");
    // the transport tracker relies on the step name
    while (!m_data.getSequence().getStep().getName().equals("germanNonCombatMove")) {
      m_data.getSequence().next();
    }
    moveDelegate.setDelegateBridgeAndPlayer(bridge);
    moveDelegate.start();
    // try to unload the other infantry somewhere else, an error occurs
    error = moveDelegate.move(infantry.subList(1, 2), sz5ToNorway);
    assertNotNull(error, error);
    assertTrue(error.startsWith(MoveValidator.TRANSPORT_HAS_ALREADY_UNLOADED_UNITS_IN_A_PREVIOUS_PHASE));
  }

  @Test
  public void testSubAttackTransportNonCombat() {
    final Territory sz1 = territory("1 Sea Zone", m_data);
    final Territory sz8 = territory("8 Sea Zone", m_data);
    final PlayerID germans = germans(m_data);
    // german sub tries to attack a transport in non combat
    // should be an error
    final MoveDelegate moveDelegate = (MoveDelegate) m_data.getDelegateList().getDelegate("move");
    final ITestDelegateBridge bridge = getDelegateBridge(germans);
    bridge.setStepName("NonCombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(bridge);
    moveDelegate.start();
    final String error = moveDelegate(m_data).move(sz8.getUnits().getUnits(), new Route(sz8, sz1));
    assertError(error);
  }

  @Test
  public void testSubAttackNonCombat() {
    final Territory sz2 = territory("2 Sea Zone", m_data);
    final Territory sz8 = territory("8 Sea Zone", m_data);
    final PlayerID germans = germans(m_data);
    // german sub tries to attack a transport in non combat
    // should be an error
    final MoveDelegate moveDelegate = (MoveDelegate) m_data.getDelegateList().getDelegate("move");
    final ITestDelegateBridge bridge = getDelegateBridge(germans);
    bridge.setStepName("NonCombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(bridge);
    moveDelegate.start();
    final String error = moveDelegate(m_data).move(sz8.getUnits().getUnits(), new Route(sz8, sz2));
    assertError(error);
  }

  @Test
  public void testTransportAttackSubNonCombat() {
    final Territory sz1 = territory("1 Sea Zone", m_data);
    final Territory sz8 = territory("8 Sea Zone", m_data);
    final PlayerID british = british(m_data);
    // german sub tries to attack a transport in non combat
    // should be an error
    final MoveDelegate moveDelegate = (MoveDelegate) m_data.getDelegateList().getDelegate("move");
    final ITestDelegateBridge bridge = getDelegateBridge(british);
    bridge.setStepName("NonCombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(bridge);
    moveDelegate.start();
    final String error = moveDelegate(m_data).move(sz8.getUnits().getUnits(), new Route(sz1, sz8));
    assertError(error);
  }

  @Test
  public void testMoveSubAwayFromSubmergedSubsInBattleZone() {
    final Territory sz45 = m_data.getMap().getTerritory("45 Sea Zone");
    final Territory sz50 = m_data.getMap().getTerritory("50 Sea Zone");
    final PlayerID british = GameDataTestUtil.british(m_data);
    final PlayerID japanese = GameDataTestUtil.japanese(m_data);
    // put 1 british sub in sz 45, this simulates a submerged enemy sub
    final UnitType sub = GameDataTestUtil.submarine(m_data);
    final Change c = ChangeFactory.addUnits(sz45, sub.create(1, british));
    m_data.performChange(c);
    // new move delegate
    final MoveDelegate moveDelegate = (MoveDelegate) m_data.getDelegateList().getDelegate("move");
    final ITestDelegateBridge bridge = getDelegateBridge(japanese);
    bridge.setStepName("CombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(bridge);
    moveDelegate.start();
    // move a fighter into the sea zone, this will cause a battle
    final Route sz50To45 = new Route();
    sz50To45.setStart(sz50);
    sz50To45.add(sz45);
    String error = moveDelegate.move(sz50.getUnits().getMatches(Matches.UnitIsAir), sz50To45);
    assertNull(error);
    assertEquals(1, AbstractMoveDelegate.getBattleTracker(m_data).getPendingBattleSites(false).size());
    // we should be able to move the sub out of the sz
    final Route sz45To50 = new Route();
    sz45To50.setStart(sz45);
    sz45To50.add(sz50);
    final List<Unit> japSub =
        sz45.getUnits().getMatches(new CompositeMatchAnd<>(Matches.UnitIsSub, Matches.unitIsOwnedBy(japanese)));
    error = moveDelegate.move(japSub, sz45To50);
    // make sure no error
    assertNull(error);
    // make sure the battle is still there
    assertEquals(1, AbstractMoveDelegate.getBattleTracker(m_data).getPendingBattleSites(false).size());
    // we should be able to undo the move of the sub
    error = moveDelegate.undoMove(1);
    assertNull(error);
    // undo the move of the fighter, should be no battles now
    error = moveDelegate.undoMove(0);
    assertNull(error);
    assertEquals(0, AbstractMoveDelegate.getBattleTracker(m_data).getPendingBattleSites(false).size());
  }

  @Test
  public void testAAOwnership() {
    // Set up players
    // PlayerID british = GameDataTestUtil.british(m_data);
    final PlayerID japanese = GameDataTestUtil.japanese(m_data);
    // PlayerID americans = GameDataTestUtil.americans(m_data);
    // Set up the territories
    final Territory india = territory("India", m_data);
    final Territory fic = territory("French Indochina", m_data);
    final Territory china = territory("China", m_data);
    final Territory kwang = territory("Kwantung", m_data);
    // Preset units in FIC
    final UnitType infType = GameDataTestUtil.infantry(m_data);
    // UnitType aaType = GameDataTestUtil.aaGun(m_data);
    removeFrom(fic, fic.getUnits().getUnits());
    addTo(fic, aaGun(m_data).create(1, japanese));
    addTo(fic, infantry(m_data).create(1, japanese));
    assertEquals(2, fic.getUnits().getUnitCount());
    // Get attacking units
    final Collection<Unit> britishUnits = india.getUnits().getUnits(infType, 1);
    final Collection<Unit> japaneseUnits = kwang.getUnits().getUnits(infType, 1);
    final Collection<Unit> americanUnits = china.getUnits().getUnits(infType, 1);
    // Get Owner prior to battle
    assertTrue(fic.getUnits().allMatch(Matches.unitIsOwnedBy(japanese(m_data))));
    final String preOwner = fic.getOwner().getName();
    assertEquals(preOwner, Constants.PLAYER_NAME_JAPANESE);
    // Set up the move delegate
    ITestDelegateBridge delegateBridge = getDelegateBridge(british(m_data));
    final MoveDelegate moveDelegate = moveDelegate(m_data);
    delegateBridge.setStepName("CombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(delegateBridge);
    moveDelegate.start();
    /*
     * add a VALID BRITISH attack
     */
    String validResults = moveDelegate.move(britishUnits, new Route(india, fic));
    assertValid(validResults);
    moveDelegate(m_data).end();
    // Set up battle
    MustFightBattle battle =
        (MustFightBattle) AbstractMoveDelegate.getBattleTracker(m_data).getPendingBattle(fic, false, null);
    delegateBridge.setRemote(new DummyTripleAPlayer());
    // fight
    ScriptedRandomSource randomSource = new ScriptedRandomSource(0, 5);
    delegateBridge.setRandomSource(randomSource);
    battle.fight(delegateBridge);
    // Get Owner after to battle
    assertTrue(fic.getUnits().allMatch(Matches.unitIsOwnedBy(british(m_data))));
    final String postOwner = fic.getOwner().getName();
    assertEquals(postOwner, Constants.PLAYER_NAME_BRITISH);
    /*
     * add a VALID JAPANESE attack
     */
    // Set up battle
    delegateBridge = getDelegateBridge(japanese(m_data));
    delegateBridge.setStepName("CombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(delegateBridge);
    moveDelegate.start();
    // Move to battle
    validResults = moveDelegate.move(japaneseUnits, new Route(kwang, fic));
    assertValid(validResults);
    moveDelegate(m_data).end();
    battle = (MustFightBattle) AbstractMoveDelegate.getBattleTracker(m_data).getPendingBattle(fic, false, null);
    delegateBridge.setRemote(new DummyTripleAPlayer());
    // fight
    randomSource = new ScriptedRandomSource(0, 5);
    delegateBridge.setRandomSource(randomSource);
    battle.fight(delegateBridge);
    // Get Owner after to battle
    assertTrue(fic.getUnits().allMatch(Matches.unitIsOwnedBy(japanese(m_data))));
    final String midOwner = fic.getOwner().getName();
    assertEquals(midOwner, Constants.PLAYER_NAME_JAPANESE);
    /*
     * add a VALID AMERICAN attack
     */
    // Set up battle
    delegateBridge = getDelegateBridge(americans(m_data));
    delegateBridge.setStepName("CombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(delegateBridge);
    moveDelegate.start();
    // Move to battle
    validResults = moveDelegate.move(americanUnits, new Route(china, fic));
    assertValid(validResults);
    moveDelegate(m_data).end();
    battle = (MustFightBattle) AbstractMoveDelegate.getBattleTracker(m_data).getPendingBattle(fic, false, null);
    delegateBridge.setRemote(new DummyTripleAPlayer());
    // fight
    randomSource = new ScriptedRandomSource(0, 5);
    delegateBridge.setRandomSource(randomSource);
    battle.fight(delegateBridge);
    // Get Owner after to battle
    assertTrue(fic.getUnits().allMatch(Matches.unitIsOwnedBy(americans(m_data))));
    final String endOwner = fic.getOwner().getName();
    assertEquals(endOwner, Constants.PLAYER_NAME_AMERICANS);
  }

  @Test
  public void testStratBombCasualties() {
    final Territory germany = m_data.getMap().getTerritory("Germany");
    final Territory uk = m_data.getMap().getTerritory("United Kingdom");
    final PlayerID germans = GameDataTestUtil.germans(m_data);
    final PlayerID british = GameDataTestUtil.british(m_data);
    final BattleTracker tracker = new BattleTracker();
    final StrategicBombingRaidBattle battle = new StrategicBombingRaidBattle(germany, m_data, british, tracker);
    final List<Unit> bombers = uk.getUnits().getMatches(Matches.UnitIsStrategicBomber);
    addTo(germany, bombers);
    battle.addAttackChange(m_data.getMap().getRoute(uk, germany), bombers, null);
    tracker.getBattleRecords(m_data).addBattle(british, battle.getBattleID(), germany, battle.getBattleType(), m_data);
    final ITestDelegateBridge bridge = getDelegateBridge(british);
    bridge.setRemote(getDummyPlayer());
    // aa guns rolls 0 and hits
    bridge.setRandomSource(new ScriptedRandomSource(new int[] {0, ScriptedRandomSource.ERROR}));
    // int PUsBeforeRaid = germans.getResources().getQuantity(m_data.getResourceList().getResource(Constants.PUS));
    final int pusBeforeRaid = germans.getResources().getQuantity(m_data.getResourceList().getResource(Constants.PUS));
    battle.fight(bridge);
    // int PUsAfterRaid = germans.getResources().getQuantity(m_data.getResourceList().getResource(Constants.PUS));
    final int pusAfterRaid = germans.getResources().getQuantity(m_data.getResourceList().getResource(Constants.PUS));
    assertEquals(pusBeforeRaid, pusAfterRaid);
    assertEquals(0, germany.getUnits().getMatches(Matches.unitIsOwnedBy(british)).size());
  }

  @Test
  public void testStratBombCasualtiesLowLuck() {
    makeGameLowLuck(m_data);
    final Territory germany = m_data.getMap().getTerritory("Germany");
    final Territory uk = m_data.getMap().getTerritory("United Kingdom");
    final PlayerID germans = GameDataTestUtil.germans(m_data);
    final PlayerID british = GameDataTestUtil.british(m_data);
    final BattleTracker tracker = new BattleTracker();
    final StrategicBombingRaidBattle battle = new StrategicBombingRaidBattle(germany, m_data, british, tracker);
    final List<Unit> bombers = bomber(m_data).create(2, british);
    addTo(germany, bombers);
    battle.addAttackChange(m_data.getMap().getRoute(uk, germany), bombers, null);
    tracker.getBattleRecords(m_data).addBattle(british, battle.getBattleID(), germany, battle.getBattleType(), m_data);
    final ITestDelegateBridge bridge = getDelegateBridge(british);
    bridge.setRemote(getDummyPlayer());
    // should be exactly 3 rolls total. would be exactly 2 rolls if the number of units being shot at = max dice side of
    // the AA gun, because
    // the casualty selection roll would not happen in LL
    // first 0 is the AA gun rolling 1@2 and getting a 1, which is a hit
    // second 0 is the LL AA casualty selection randomly picking the first unit to die
    // third 0 is the single remaining bomber dealing 1 damage to the enemy's PUs
    bridge.setRandomSource(new ScriptedRandomSource(new int[] {0, 0, 0, ScriptedRandomSource.ERROR}));
    final int pusBeforeRaid = germans.getResources().getQuantity(m_data.getResourceList().getResource(Constants.PUS));
    battle.fight(bridge);
    final int pusAfterRaid = germans.getResources().getQuantity(m_data.getResourceList().getResource(Constants.PUS));
    assertEquals(pusBeforeRaid - 1, pusAfterRaid);
    assertEquals(1, germany.getUnits().getMatches(Matches.unitIsOwnedBy(british)).size());
  }

  @Test
  public void testStratBombCasualtiesLowLuckManyBombers() {
    makeGameLowLuck(m_data);
    final Territory germany = m_data.getMap().getTerritory("Germany");
    final Territory uk = m_data.getMap().getTerritory("United Kingdom");
    final PlayerID germans = GameDataTestUtil.germans(m_data);
    final PlayerID british = GameDataTestUtil.british(m_data);
    final BattleTracker tracker = new BattleTracker();
    final StrategicBombingRaidBattle battle = new StrategicBombingRaidBattle(germany, m_data, british, tracker);
    final List<Unit> bombers = bomber(m_data).create(7, british);
    addTo(germany, bombers);
    battle.addAttackChange(m_data.getMap().getRoute(uk, germany), bombers, null);
    tracker.getBattleRecords(m_data).addBattle(british, battle.getBattleID(), germany, battle.getBattleType(), m_data);
    final ITestDelegateBridge bridge = getDelegateBridge(british);
    bridge.setRemote(getDummyPlayer());
    // aa guns rolls 0 and hits, next 5 dice are for the bombing raid cost for the
    // surviving bombers
    bridge.setRandomSource(new ScriptedRandomSource(new int[] {0, 0, 0, 0, 0, 0, ScriptedRandomSource.ERROR}));
    final int pusBeforeRaid = germans.getResources().getQuantity(m_data.getResourceList().getResource(Constants.PUS));
    battle.fight(bridge);
    final int pusAfterRaid = germans.getResources().getQuantity(m_data.getResourceList().getResource(Constants.PUS));
    assertEquals(pusBeforeRaid - 5, pusAfterRaid);
    // 2 bombers get hit
    assertEquals(5, germany.getUnits().getMatches(Matches.unitIsOwnedBy(british)).size());
  }

  @Test
  public void testStratBombRaidWithHeavyBombers() {
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
    // aa guns rolls 3, misses, bomber rolls 2 dice at 3
    bridge.setRandomSource(new ScriptedRandomSource(new int[] {3, 2, 2}));
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
    // int PUsBeforeRaid = germans.getResources().getQuantity(m_data.getResourceList().getResource(Constants.PUS));
    final int pusBeforeRaid = germans.getResources().getQuantity(m_data.getResourceList().getResource(Constants.PUS));
    battle.fight(bridge);
    // int PUsAfterRaid = germans.getResources().getQuantity(m_data.getResourceList().getResource(Constants.PUS));
    final int pusAfterRaid = germans.getResources().getQuantity(m_data.getResourceList().getResource(Constants.PUS));
    assertEquals(pusBeforeRaid - 6, pusAfterRaid);
  }

  @Test
  public void testLandBattleNoSneakAttack() {
    final String defender = "Germans";
    final String attacker = "British";
    final Territory attacked = territory("Libya", m_data);
    final Territory from = territory("Anglo Egypt", m_data);
    final ITestDelegateBridge bridge = getDelegateBridge(british(m_data));
    bridge.setStepName("CombatMove");
    moveDelegate(m_data).setDelegateBridgeAndPlayer(bridge);
    moveDelegate(m_data).start();
    move(from.getUnits().getUnits(), new Route(from, attacked));
    moveDelegate(m_data).end();
    final MustFightBattle battle =
        (MustFightBattle) AbstractMoveDelegate.getBattleTracker(m_data).getPendingBattle(attacked, false, null);
    final List<String> steps = battle.determineStepStrings(true, bridge);
    assertEquals(Arrays.asList(attacker + FIRE, defender + SELECT_CASUALTIES, defender + FIRE,
        attacker + SELECT_CASUALTIES, REMOVE_CASUALTIES, attacker + ATTACKER_WITHDRAW).toString(), steps.toString());
  }

  @Test
  public void testSeaBattleNoSneakAttack() {
    final String defender = "Germans";
    final String attacker = "British";
    final Territory attacked = territory("31 Sea Zone", m_data);
    final Territory from = territory("32 Sea Zone", m_data);
    // 1 destroyer attacks 1 destroyer
    addTo(from, destroyer(m_data).create(1, british(m_data)));
    addTo(attacked, destroyer(m_data).create(1, germans(m_data)));
    final ITestDelegateBridge bridge = getDelegateBridge(british(m_data));
    bridge.setStepName("CombatMove");
    moveDelegate(m_data).setDelegateBridgeAndPlayer(bridge);
    moveDelegate(m_data).start();
    move(from.getUnits().getUnits(), new Route(from, attacked));
    moveDelegate(m_data).end();
    final MustFightBattle battle =
        (MustFightBattle) AbstractMoveDelegate.getBattleTracker(m_data).getPendingBattle(attacked, false, null);
    final List<String> steps = battle.determineStepStrings(true, bridge);
    assertEquals(Arrays.asList(attacker + FIRE, defender + SELECT_CASUALTIES, defender + FIRE,
        attacker + SELECT_CASUALTIES, REMOVE_CASUALTIES, attacker + ATTACKER_WITHDRAW).toString(), steps.toString());
  }

  @Test
  public void testAttackSubsOnSubs() {
    final String defender = "Germans";
    final String attacker = "British";
    final Territory attacked = territory("31 Sea Zone", m_data);
    final Territory from = territory("32 Sea Zone", m_data);
    // 1 sub attacks 1 sub
    addTo(from, submarine(m_data).create(1, british(m_data)));
    addTo(attacked, submarine(m_data).create(1, germans(m_data)));
    final ITestDelegateBridge bridge = getDelegateBridge(british(m_data));
    bridge.setStepName("CombatMove");
    moveDelegate(m_data).setDelegateBridgeAndPlayer(bridge);
    moveDelegate(m_data).start();
    move(from.getUnits().getUnits(), new Route(from, attacked));
    moveDelegate(m_data).end();
    final MustFightBattle battle =
        (MustFightBattle) AbstractMoveDelegate.getBattleTracker(m_data).getPendingBattle(attacked, false, null);
    final List<String> steps = battle.determineStepStrings(true, bridge);
    assertEquals(
        Arrays.asList(attacker + SUBS_FIRE, defender + SELECT_SUB_CASUALTIES, defender + SUBS_FIRE,
            attacker + SELECT_SUB_CASUALTIES, REMOVE_SNEAK_ATTACK_CASUALTIES, REMOVE_CASUALTIES,
            attacker + SUBS_SUBMERGE, defender + SUBS_SUBMERGE, attacker + ATTACKER_WITHDRAW).toString(),
        steps.toString());
    final List<IExecutable> execs = battle.getBattleExecutables(false);
    final int attackSubs = getIndex(execs, MustFightBattle.AttackSubs.class);
    final int defendSubs = getIndex(execs, MustFightBattle.DefendSubs.class);
    assertTrue(attackSubs < defendSubs);
    bridge.setRemote(new DummyTripleAPlayer());
    // fight, each sub should fire
    // and hit
    final ScriptedRandomSource randomSource = new ScriptedRandomSource(0, 0, ScriptedRandomSource.ERROR);
    bridge.setRandomSource(randomSource);
    battle.fight(bridge);
    assertEquals(2, randomSource.getTotalRolled());
    assertTrue(attacked.getUnits().isEmpty());
  }

  @Test
  public void testAttackSubsOnDestroyer() {
    final String defender = "Germans";
    final String attacker = "British";
    final Territory attacked = territory("31 Sea Zone", m_data);
    final Territory from = territory("32 Sea Zone", m_data);
    // 2 sub attacks 1 sub and 1 destroyer
    addTo(from, submarine(m_data).create(2, british(m_data)));
    addTo(attacked, submarine(m_data).create(1, germans(m_data)));
    addTo(attacked, destroyer(m_data).create(1, germans(m_data)));
    final ITestDelegateBridge bridge = getDelegateBridge(british(m_data));
    bridge.setStepName("CombatMove");
    moveDelegate(m_data).setDelegateBridgeAndPlayer(bridge);
    moveDelegate(m_data).start();
    move(from.getUnits().getUnits(), new Route(from, attacked));
    moveDelegate(m_data).end();
    final MustFightBattle battle =
        (MustFightBattle) AbstractMoveDelegate.getBattleTracker(m_data).getPendingBattle(attacked, false, null);
    final List<String> steps = battle.determineStepStrings(true, bridge);
    /*
     * Here are the exact errata clarifications on how REVISED rules subs work:
     * Every sub, regardless of whether it is on the attacking or defending side, fires in the Opening Fire step of
     * combat. That is the only
     * time a sub ever fires.
     * Losses caused by attacking or defending subs are removed at the end of the Opening Fire step, before normal
     * attack and defense rolls,
     * unless the enemy has a destroyer present.
     * If the enemy (attacker or defender) has a destroyer, then hits caused by your subs are not removed until the
     * Remove Casualties step
     * (step 6) of combat.
     * In other words, subs work exactly the same for the attacker and the defender. Nothing, not even a destroyer, ever
     * stops a sub from
     * rolling its die (attack or defense) in the Opening Fire step.
     * What a destroyer does do is let you keep your units that were sunk by enemy subs on the battle board until step
     * 6, allowing them to
     * fire back before going to the scrap heap.
     */
    assertEquals(Arrays.asList(attacker + SUBS_FIRE, defender + SELECT_SUB_CASUALTIES, defender + SUBS_FIRE,
        attacker + SELECT_SUB_CASUALTIES, REMOVE_SNEAK_ATTACK_CASUALTIES, defender + FIRE, attacker + SELECT_CASUALTIES,
        REMOVE_CASUALTIES, attacker + SUBS_SUBMERGE, defender + SUBS_SUBMERGE, attacker + ATTACKER_WITHDRAW).toString(),
        steps.toString());
    final List<IExecutable> execs = battle.getBattleExecutables(false);
    final int attackSubs = getIndex(execs, MustFightBattle.AttackSubs.class);
    final int defendSubs = getIndex(execs, MustFightBattle.DefendSubs.class);
    assertTrue(attackSubs < defendSubs);
    bridge.setRemote(new DummyTripleAPlayer());
    // attacking subs fires, defending destroyer and sub still gets to fire
    // attacking subs still gets to fire even if defending sub hits
    final ScriptedRandomSource randomSource = new ScriptedRandomSource(0, 2, 0, 0, ScriptedRandomSource.ERROR);
    bridge.setRandomSource(randomSource);
    battle.fight(bridge);
    assertEquals(4, randomSource.getTotalRolled());
    assertTrue(attacked.getUnits().getMatches(Matches.unitIsOwnedBy(british(m_data))).isEmpty());
    assertEquals(1, attacked.getUnits().size());
  }

  @Test
  public void testAttackSubsAndBBOnDestroyerAndSubs() {
    final String defender = "Germans";
    final String attacker = "British";
    final Territory attacked = territory("31 Sea Zone", m_data);
    final Territory from = territory("32 Sea Zone", m_data);
    // 1 sub and 1 BB (two hp) attacks 3 subs and 1 destroyer
    addTo(from, submarine(m_data).create(1, british(m_data)));
    addTo(from, battleship(m_data).create(1, british(m_data)));
    addTo(attacked, submarine(m_data).create(3, germans(m_data)));
    addTo(attacked, destroyer(m_data).create(1, germans(m_data)));
    final ITestDelegateBridge bridge = getDelegateBridge(british(m_data));
    bridge.setStepName("CombatMove");
    moveDelegate(m_data).setDelegateBridgeAndPlayer(bridge);
    moveDelegate(m_data).start();
    move(from.getUnits().getUnits(), new Route(from, attacked));
    moveDelegate(m_data).end();
    final MustFightBattle battle =
        (MustFightBattle) AbstractMoveDelegate.getBattleTracker(m_data).getPendingBattle(attacked, false, null);
    final List<String> steps = battle.determineStepStrings(true, bridge);
    /*
     * Here are the exact errata clarifications on how REVISED rules subs work:
     * Every sub, regardless of whether it is on the attacking or defending side, fires in the Opening Fire step of
     * combat. That is the only
     * time a sub ever fires.
     * Losses caused by attacking or defending subs are removed at the end of the Opening Fire step, before normal
     * attack and defense rolls,
     * unless the enemy has a destroyer present.
     * If the enemy (attacker or defender) has a destroyer, then hits caused by your subs are not removed until the
     * Remove Casualties step
     * (step 6) of combat.
     * In other words, subs work exactly the same for the attacker and the defender. Nothing, not even a destroyer, ever
     * stops a sub from
     * rolling its die (attack or defense) in the Opening Fire step.
     * What a destroyer does do is let you keep your units that were sunk by enemy subs on the battle board until step
     * 6, allowing them to
     * fire back before going to the scrap heap.
     */
    assertEquals(
        Arrays.asList(attacker + SUBS_FIRE, defender + SELECT_SUB_CASUALTIES, defender + SUBS_FIRE,
            attacker + SELECT_SUB_CASUALTIES, REMOVE_SNEAK_ATTACK_CASUALTIES, attacker + FIRE,
            defender + SELECT_CASUALTIES, defender + FIRE, attacker + SELECT_CASUALTIES, REMOVE_CASUALTIES,
            attacker + SUBS_SUBMERGE, defender + SUBS_SUBMERGE, attacker + ATTACKER_WITHDRAW).toString(),
        steps.toString());
    final List<IExecutable> execs = battle.getBattleExecutables(false);
    final int attackSubs = getIndex(execs, MustFightBattle.AttackSubs.class);
    final int defendSubs = getIndex(execs, MustFightBattle.DefendSubs.class);
    assertTrue(attackSubs < defendSubs);
    bridge.setRemote(new DummyTripleAPlayer());
    // attacking subs fires, defending destroyer and sub still gets to fire
    // attacking subs still gets to fire even if defending sub hits
    // battleship will not get to fire since it is killed by defending sub's sneak attack
    final ScriptedRandomSource randomSource = new ScriptedRandomSource(0, 0, 0, 0, ScriptedRandomSource.ERROR);
    bridge.setRandomSource(randomSource);
    battle.fight(bridge);
    assertEquals(4, randomSource.getTotalRolled());
    assertTrue(attacked.getUnits().getMatches(Matches.unitIsOwnedBy(british(m_data))).isEmpty());
    assertEquals(3, attacked.getUnits().size());
  }

  @Test
  public void testAttackDestroyerAndSubsAgainstSub() {
    final String defender = "Germans";
    final String attacker = "British";
    final Territory attacked = territory("31 Sea Zone", m_data);
    final Territory from = territory("32 Sea Zone", m_data);
    // 1 sub and 1 destroyer attack 1 sub
    // defender sneak attacks, not attacker
    addTo(from, submarine(m_data).create(1, british(m_data)));
    addTo(from, destroyer(m_data).create(1, british(m_data)));
    addTo(attacked, submarine(m_data).create(1, germans(m_data)));
    final ITestDelegateBridge bridge = getDelegateBridge(british(m_data));
    bridge.setStepName("CombatMove");
    moveDelegate(m_data).setDelegateBridgeAndPlayer(bridge);
    moveDelegate(m_data).start();
    move(from.getUnits().getUnits(), new Route(from, attacked));
    moveDelegate(m_data).end();
    final MustFightBattle battle =
        (MustFightBattle) AbstractMoveDelegate.getBattleTracker(m_data).getPendingBattle(attacked, false, null);
    final List<String> steps = battle.determineStepStrings(true, bridge);
    assertEquals(Arrays.asList(attacker + SUBS_FIRE, defender + SELECT_SUB_CASUALTIES, defender + SUBS_FIRE,
        attacker + SELECT_SUB_CASUALTIES, REMOVE_SNEAK_ATTACK_CASUALTIES, attacker + FIRE, defender + SELECT_CASUALTIES,
        REMOVE_CASUALTIES, attacker + SUBS_SUBMERGE, defender + SUBS_SUBMERGE, attacker + ATTACKER_WITHDRAW).toString(),
        steps.toString());
    final List<IExecutable> execs = battle.getBattleExecutables(false);
    final int attackSubs = getIndex(execs, MustFightBattle.AttackSubs.class);
    final int defendSubs = getIndex(execs, MustFightBattle.DefendSubs.class);
    assertTrue(attackSubs < defendSubs);
    bridge.setRemote(new DummyTripleAPlayer());
    // attacking sub hits with sneak attack, but defending sub gets to return fire because it is a sub and this is
    // revised rules
    final ScriptedRandomSource randomSource = new ScriptedRandomSource(0, 0, ScriptedRandomSource.ERROR);
    bridge.setRandomSource(randomSource);
    battle.fight(bridge);
    assertEquals(2, randomSource.getTotalRolled());
    assertTrue(attacked.getUnits().getMatches(Matches.unitIsOwnedBy(germans(m_data))).isEmpty());
    assertEquals(1, attacked.getUnits().size());
  }

  @Test
  public void testAttackSubsAndDestroyerOnBBAndSubs() {
    final String defender = "Germans";
    final String attacker = "British";
    final Territory attacked = territory("31 Sea Zone", m_data);
    final Territory from = territory("32 Sea Zone", m_data);
    // 1 sub and 1 BB (two hp) attacks 3 subs and 1 destroyer
    addTo(from, submarine(m_data).create(3, british(m_data)));
    addTo(from, destroyer(m_data).create(1, british(m_data)));
    addTo(attacked, submarine(m_data).create(1, germans(m_data)));
    addTo(attacked, battleship(m_data).create(1, germans(m_data)));
    final ITestDelegateBridge bridge = getDelegateBridge(british(m_data));
    bridge.setStepName("CombatMove");
    moveDelegate(m_data).setDelegateBridgeAndPlayer(bridge);
    moveDelegate(m_data).start();
    move(from.getUnits().getUnits(), new Route(from, attacked));
    moveDelegate(m_data).end();
    final MustFightBattle battle =
        (MustFightBattle) AbstractMoveDelegate.getBattleTracker(m_data).getPendingBattle(attacked, false, null);
    final List<String> steps = battle.determineStepStrings(true, bridge);
    /*
     * Here are the exact errata clarifications on how REVISED rules subs work:
     * Every sub, regardless of whether it is on the attacking or defending side, fires in the Opening Fire step of
     * combat. That is the only
     * time a sub ever fires.
     * Losses caused by attacking or defending subs are removed at the end of the Opening Fire step, before normal
     * attack and defense rolls,
     * unless the enemy has a destroyer present.
     * If the enemy (attacker or defender) has a destroyer, then hits caused by your subs are not removed until the
     * Remove Casualties step
     * (step 6) of combat.
     * In other words, subs work exactly the same for the attacker and the defender. Nothing, not even a destroyer, ever
     * stops a sub from
     * rolling its die (attack or defense) in the Opening Fire step.
     * What a destroyer does do is let you keep your units that were sunk by enemy subs on the battle board until step
     * 6, allowing them to
     * fire back before going to the scrap heap.
     */
    assertEquals(
        Arrays.asList(attacker + SUBS_FIRE, defender + SELECT_SUB_CASUALTIES, defender + SUBS_FIRE,
            attacker + SELECT_SUB_CASUALTIES, REMOVE_SNEAK_ATTACK_CASUALTIES, attacker + FIRE,
            defender + SELECT_CASUALTIES, defender + FIRE, attacker + SELECT_CASUALTIES, REMOVE_CASUALTIES,
            attacker + SUBS_SUBMERGE, defender + SUBS_SUBMERGE, attacker + ATTACKER_WITHDRAW).toString(),
        steps.toString());
    final List<IExecutable> execs = battle.getBattleExecutables(false);
    final int attackSubs = getIndex(execs, MustFightBattle.AttackSubs.class);
    final int defendSubs = getIndex(execs, MustFightBattle.DefendSubs.class);
    assertTrue(attackSubs < defendSubs);
    bridge.setRemote(new DummyTripleAPlayer());
    // attacking subs fires, defending destroyer and sub still gets to fire
    // attacking subs still gets to fire even if defending sub hits
    // battleship will not get to fire since it is killed by defending sub's sneak attack
    final ScriptedRandomSource randomSource = new ScriptedRandomSource(0, 0, 0, 0, ScriptedRandomSource.ERROR);
    bridge.setRandomSource(randomSource);
    battle.fight(bridge);
    assertEquals(4, randomSource.getTotalRolled());
    assertTrue(attacked.getUnits().getMatches(Matches.unitIsOwnedBy(germans(m_data))).isEmpty());
    assertEquals(3, attacked.getUnits().size());
  }

  @Test
  public void testAttackDestroyerAndSubsAgainstSubAndDestroyer() {
    final String defender = "Germans";
    final String attacker = "British";
    final Territory attacked = territory("31 Sea Zone", m_data);
    final Territory from = territory("32 Sea Zone", m_data);
    // 1 sub and 1 destroyer attack 1 sub and 1 destroyer
    // defender sneak attacks, not attacker
    addTo(from, submarine(m_data).create(1, british(m_data)));
    addTo(from, destroyer(m_data).create(1, british(m_data)));
    addTo(attacked, submarine(m_data).create(1, germans(m_data)));
    addTo(attacked, destroyer(m_data).create(1, germans(m_data)));
    final ITestDelegateBridge bridge = getDelegateBridge(british(m_data));
    bridge.setStepName("CombatMove");
    moveDelegate(m_data).setDelegateBridgeAndPlayer(bridge);
    moveDelegate(m_data).start();
    move(from.getUnits().getUnits(), new Route(from, attacked));
    moveDelegate(m_data).end();
    final MustFightBattle battle =
        (MustFightBattle) AbstractMoveDelegate.getBattleTracker(m_data).getPendingBattle(attacked, false, null);
    final List<String> steps = battle.determineStepStrings(true, bridge);
    assertEquals(Arrays.asList(attacker + SUBS_FIRE, defender + SELECT_SUB_CASUALTIES, defender + SUBS_FIRE,
        attacker + SELECT_SUB_CASUALTIES, attacker + FIRE, defender + SELECT_CASUALTIES, defender + FIRE,
        attacker + SELECT_CASUALTIES, REMOVE_CASUALTIES, attacker + SUBS_SUBMERGE, defender + SUBS_SUBMERGE,
        attacker + ATTACKER_WITHDRAW).toString(), steps.toString());
    final List<IExecutable> execs = battle.getBattleExecutables(false);
    final int attackSubs = getIndex(execs, MustFightBattle.AttackSubs.class);
    final int defendSubs = getIndex(execs, MustFightBattle.DefendSubs.class);
    assertTrue(attackSubs < defendSubs);
    bridge.setRemote(new DummyTripleAPlayer());
    bridge.setRemote(new DummyTripleAPlayer() {
      @Override
      public CasualtyDetails selectCasualties(final Collection<Unit> selectFrom,
          final Map<Unit, Collection<Unit>> dependents, final int count, final String message, final DiceRoll dice,
          final PlayerID hit, final Collection<Unit> friendlyUnits, final PlayerID enemyPlayer,
          final Collection<Unit> enemyUnits, final boolean amphibious, final Collection<Unit> amphibiousLandAttackers,
          final CasualtyList defaultCasualties, final GUID battleID, final Territory battlesite,
          final boolean allowMultipleHitsPerUnit) {
        return new CasualtyDetails(Arrays.asList(selectFrom.iterator().next()), Collections.<Unit>emptyList(), false);
      }
    });
    final ScriptedRandomSource randomSource = new ScriptedRandomSource(0, 0, 0, 0, ScriptedRandomSource.ERROR);
    bridge.setRandomSource(randomSource);
    battle.fight(bridge);
    assertEquals(4, randomSource.getTotalRolled());
    assertEquals(0, attacked.getUnits().size());
  }

  @Test
  public void testUnplacedDie() {
    final PlaceDelegate del = placeDelegate(m_data);
    del.setDelegateBridgeAndPlayer(getDelegateBridge(british(m_data)));
    del.start();
    addTo(british(m_data), transport(m_data).create(1, british(m_data)), m_data);
    del.end();
    // unplaced units die
    assertTrue(british(m_data).getUnits().isEmpty());
  }

  @Test
  public void testRocketsDontFireInConquered() {
    final MoveDelegate move = moveDelegate(m_data);
    final ITestDelegateBridge bridge = getDelegateBridge(germans(m_data));
    bridge.setStepName("CombatMove");
    bridge.setRemote(new DummyTripleAPlayer() {});
    move.setDelegateBridgeAndPlayer(bridge);
    move.start();
    // remove the russians units in caucasus so we can blitz
    final Territory cauc = territory("Caucasus", m_data);
    removeFrom(cauc, cauc.getUnits().getMatches(Matches.UnitIsNotAA));
    // blitz
    final Territory wr = territory("West Russia", m_data);
    move(wr.getUnits().getMatches(Matches.UnitCanBlitz), new Route(wr, cauc));
    final Set<Territory> fire = new RocketsFireHelper().getTerritoriesWithRockets(m_data, germans(m_data));
    // germany, WE, SE, but not caucusus
    assertEquals(fire.size(), 3);
  }

  @Test
  public void testTechRolls() {
    // Set up the test
    final PlayerID germans = GameDataTestUtil.germans(m_data);
    final ITestDelegateBridge delegateBridge = getDelegateBridge(germans);
    delegateBridge.setStepName("germanTech");
    final TechnologyDelegate techDelegate = techDelegate(m_data);
    techDelegate.setDelegateBridgeAndPlayer(delegateBridge);
    techDelegate.start();
    final TechAttachment ta = TechAttachment.get(germans);
    // PlayerAttachment pa = PlayerAttachment.get(germans);
    final TechnologyFrontier rockets = new TechnologyFrontier("", m_data);
    rockets.addAdvance(TechAdvance.findAdvance(TechAdvance.TECH_PROPERTY_ROCKETS, m_data, null));
    final TechnologyFrontier jet = new TechnologyFrontier("", m_data);
    jet.addAdvance(TechAdvance.findAdvance(TechAdvance.TECH_PROPERTY_JET_POWER, m_data, null));
    // Check to make sure it was successful
    final int initPUs = germans.getResources().getQuantity("PUs");
    // Fail the roll
    delegateBridge.setRandomSource(new ScriptedRandomSource(new int[] {3, 4}));
    final TechResults roll = techDelegate.rollTech(2, rockets, 0, null);
    // Check to make sure it failed
    assertEquals(0, roll.getHits());
    final int midPUs = germans.getResources().getQuantity("PUs");
    assertEquals(initPUs - 10, midPUs);
    // Make a Successful roll
    delegateBridge.setRandomSource(new ScriptedRandomSource(new int[] {5}));
    final TechResults roll2 = techDelegate.rollTech(1, rockets, 0, null);
    // Check to make sure it succeeded
    assertEquals(1, roll2.getHits());
    final int finalPUs = germans.getResources().getQuantity("PUs");
    assertEquals(midPUs - 5, finalPUs);
    // Test the variable tech cost
    // Make a Successful roll
    ta.setTechCost("6");
    delegateBridge.setRandomSource(new ScriptedRandomSource(new int[] {5}));
    final TechResults roll3 = techDelegate.rollTech(1, jet, 0, null);
    // Check to make sure it succeeded
    assertEquals(1, roll3.getHits());
    final int VariablePUs = germans.getResources().getQuantity("PUs");
    assertEquals(finalPUs - 6, VariablePUs);
  }

  @Test
  public void testTransportsUnloadingToMultipleTerritoriesDie() {
    // two transports enter a battle, but drop off
    // their units to two allied territories before
    // the begin the battle
    // the units they drop off should die with the transports
    final PlayerID germans = germans(m_data);
    final PlayerID british = british(m_data);
    final Territory sz6 = territory("6 Sea Zone", m_data);
    final Territory sz5 = territory("5 Sea Zone", m_data);
    final Territory germany = territory("Germany", m_data);
    final Territory norway = territory("Norway", m_data);
    final Territory we = territory("Western Europe", m_data);
    final Territory uk = territory("United Kingdom", m_data);
    addTo(sz6, destroyer(m_data).create(2, british));
    addTo(sz5, transport(m_data).create(3, germans));
    addTo(germany, armour(m_data).create(3, germans));
    final ITestDelegateBridge bridge = getDelegateBridge(germans(m_data));
    bridge.setStepName("CombatMove");
    bridge.setRemote(getDummyPlayer());
    moveDelegate(m_data).setDelegateBridgeAndPlayer(bridge);
    moveDelegate(m_data).start();
    // load two transports, 1 tank each
    load(germany.getUnits().getMatches(Matches.UnitCanBlitz).subList(0, 1), new Route(germany, sz5));
    load(germany.getUnits().getMatches(Matches.UnitCanBlitz).subList(0, 1), new Route(germany, sz5));
    load(germany.getUnits().getMatches(Matches.UnitCanBlitz).subList(0, 1), new Route(germany, sz5));
    // attack sz 6
    move(sz5.getUnits().getMatches(new CompositeMatchOr<>(Matches.UnitCanBlitz, Matches.UnitIsTransport)),
        new Route(sz5, sz6));
    // unload transports, 1 each to a different country
    // this move is illegal now
    assertMoveError(sz6.getUnits().getMatches(Matches.UnitCanBlitz).subList(0, 1), new Route(sz6, norway));
    // this move is illegal now
    assertMoveError(sz6.getUnits().getMatches(Matches.UnitCanBlitz).subList(0, 1), new Route(sz6, we));
    move(sz6.getUnits().getMatches(Matches.UnitCanBlitz).subList(0, 1), new Route(sz6, uk));
    // fight the battle
    moveDelegate(m_data).end();
    final MustFightBattle battle =
        (MustFightBattle) AbstractMoveDelegate.getBattleTracker(m_data).getPendingBattle(sz6, false, null);
    // everything hits, this will kill both transports
    bridge.setRandomSource(new ScriptedRandomSource(0));
    battle.fight(bridge);
    // the armour should have died
    assertEquals(0, norway.getUnits().countMatches(Matches.UnitCanBlitz));
    assertEquals(2, we.getUnits().countMatches(Matches.UnitCanBlitz));
    assertEquals(0, uk.getUnits().countMatches(Matches.unitIsOwnedBy(germans)));
  }

  @Test
  public void testCanalMovePass() {
    final Territory sz15 = territory("15 Sea Zone", m_data);
    final Territory sz34 = territory("34 Sea Zone", m_data);
    final ITestDelegateBridge bridge = getDelegateBridge(british(m_data));
    bridge.setStepName("CombatMove");
    final MoveDelegate moveDelegate = moveDelegate(m_data);
    moveDelegate.setDelegateBridgeAndPlayer(bridge);
    moveDelegate.start();
    final String error = moveDelegate.move(sz15.getUnits().getUnits(), new Route(sz15, sz34));
    assertValid(error);
  }

  @Test
  public void testCanalMovementFail() {
    final Territory sz14 = territory("14 Sea Zone", m_data);
    final Territory sz15 = territory("15 Sea Zone", m_data);
    final Territory sz34 = territory("34 Sea Zone", m_data);
    // clear the british in sz 15
    removeFrom(sz15, sz15.getUnits().getUnits());
    final ITestDelegateBridge bridge = getDelegateBridge(germans(m_data));
    bridge.setStepName("CombatMove");
    final MoveDelegate moveDelegate = moveDelegate(m_data);
    moveDelegate.setDelegateBridgeAndPlayer(bridge);
    moveDelegate.start();
    final String error = moveDelegate.move(sz14.getUnits().getUnits(), new Route(sz14, sz15, sz34));
    assertError(error);
  }

  private ITripleAPlayer getDummyPlayer() {
    return new DummyTripleAPlayer();
  }

  public void testTransportIsTransport() {
    assertTrue(Matches.UnitIsTransport.match(transport(m_data).create(british(m_data))));
    assertFalse(Matches.UnitIsTransport.match(infantry(m_data).create(british(m_data))));
  }
}
