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
import static games.strategy.triplea.delegate.GameDataTestUtil.battleDelegate;
import static games.strategy.triplea.delegate.GameDataTestUtil.bidPlaceDelegate;
import static games.strategy.triplea.delegate.GameDataTestUtil.bomber;
import static games.strategy.triplea.delegate.GameDataTestUtil.british;
import static games.strategy.triplea.delegate.GameDataTestUtil.carrier;
import static games.strategy.triplea.delegate.GameDataTestUtil.chinese;
import static games.strategy.triplea.delegate.GameDataTestUtil.destroyer;
import static games.strategy.triplea.delegate.GameDataTestUtil.factory;
import static games.strategy.triplea.delegate.GameDataTestUtil.fighter;
import static games.strategy.triplea.delegate.GameDataTestUtil.germans;
import static games.strategy.triplea.delegate.GameDataTestUtil.givePlayerRadar;
import static games.strategy.triplea.delegate.GameDataTestUtil.infantry;
import static games.strategy.triplea.delegate.GameDataTestUtil.italians;
import static games.strategy.triplea.delegate.GameDataTestUtil.load;
import static games.strategy.triplea.delegate.GameDataTestUtil.makeGameLowLuck;
import static games.strategy.triplea.delegate.GameDataTestUtil.move;
import static games.strategy.triplea.delegate.GameDataTestUtil.moveDelegate;
import static games.strategy.triplea.delegate.GameDataTestUtil.placeDelegate;
import static games.strategy.triplea.delegate.GameDataTestUtil.purchaseDelegate;
import static games.strategy.triplea.delegate.GameDataTestUtil.removeFrom;
import static games.strategy.triplea.delegate.GameDataTestUtil.russians;
import static games.strategy.triplea.delegate.GameDataTestUtil.submarine;
import static games.strategy.triplea.delegate.GameDataTestUtil.techDelegate;
import static games.strategy.triplea.delegate.GameDataTestUtil.territory;
import static games.strategy.triplea.delegate.GameDataTestUtil.transport;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Before;
import org.junit.Test;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.data.Change;
import games.strategy.engine.data.ChangeFactory;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParseException;
import games.strategy.engine.data.ITestDelegateBridge;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.RepairRule;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.TechnologyFrontier;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.random.ScriptedRandomSource;
import games.strategy.net.GUID;
import games.strategy.triplea.Constants;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.attachments.TechAttachment;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.IBattle.BattleType;
import games.strategy.triplea.delegate.dataObjects.CasualtyDetails;
import games.strategy.triplea.delegate.dataObjects.CasualtyList;
import games.strategy.triplea.delegate.dataObjects.MoveValidationResult;
import games.strategy.triplea.delegate.dataObjects.PlaceableUnits;
import games.strategy.triplea.delegate.dataObjects.TechResults;
import games.strategy.triplea.player.ITripleAPlayer;
import games.strategy.triplea.ui.display.DummyTripleADisplay;
import games.strategy.triplea.util.DummyTripleAPlayer;
import games.strategy.triplea.xml.LoadGameUtil;
import games.strategy.util.IntegerMap;
import games.strategy.util.Match;

public class WW2V3_41_Test {
  private GameData m_data;

  @Before
  public void setUp() throws Exception {
    m_data = LoadGameUtil.loadTestGame("ww2v3_1941_test.xml");
  }

  private ITestDelegateBridge getDelegateBridge(final PlayerID player) {
    return GameDataTestUtil.getDelegateBridge(player, m_data);
  }

  public static String fight(final BattleDelegate battle, final Territory territory) {
    for (final Entry<BattleType, Collection<Territory>> entry : battle.getBattles().getBattles().entrySet()) {
      if (!entry.getKey().isBombingRun() && entry.getValue().contains(territory)) {
        return battle.fightBattle(territory, false, entry.getKey());
      }
    }
    throw new IllegalStateException(
        "Could not find battle in: " + territory.getName());
  }

  @Test
  public void testAACasualtiesLowLuckMixedRadar() {
    // moved from BattleCalculatorTest because "revised" does not have "radar"
    final PlayerID british = GameDataTestUtil.british(m_data);
    final ITestDelegateBridge m_bridge = getDelegateBridge(british);
    makeGameLowLuck(m_data);
    // setSelectAACasualties(data, false);
    givePlayerRadar(germans(m_data));
    // 3 bombers and 3 fighters
    final Collection<Unit> planes = bomber(m_data).create(3, british(m_data));
    planes.addAll(fighter(m_data).create(3, british(m_data)));
    final Collection<Unit> defendingAA =
        territory("Germany", m_data).getUnits().getMatches(Matches.UnitIsAAforAnything);
    // don't allow rolling, 6 of each is deterministic
    m_bridge.setRandomSource(new ScriptedRandomSource(new int[] {ScriptedRandomSource.ERROR}));
    final DiceRoll roll =
        DiceRoll
            .rollAA(
                Match.getMatches(planes,
                    Matches.unitIsOfTypes(
                        UnitAttachment.get(defendingAA.iterator().next().getType()).getTargetsAA(m_data))),
                defendingAA, m_bridge, territory("Germany", m_data), true);
    final Collection<Unit> casualties = BattleCalculator.getAACasualties(false, planes, planes, defendingAA,
        defendingAA, roll, m_bridge, null, null, null, territory("Germany", m_data), null, false, null).getKilled();
    assertEquals(casualties.size(), 2);
    // should be 1 fighter and 1 bomber
    assertEquals(Match.countMatches(casualties, Matches.UnitIsStrategicBomber), 1);
    assertEquals(Match.countMatches(casualties, Matches.UnitIsStrategicBomber.invert()), 1);
  }

  @Test
  public void testAACasualtiesLowLuckMixedWithRollingRadar() {
    // moved from BattleCalculatorTest because "revised" does not have "radar"
    final PlayerID british = GameDataTestUtil.british(m_data);
    final ITestDelegateBridge m_bridge = getDelegateBridge(british);
    makeGameLowLuck(m_data);
    // setSelectAACasualties(data, false);
    givePlayerRadar(germans(m_data));
    // 4 bombers and 4 fighters
    final Collection<Unit> planes = bomber(m_data).create(4, british(m_data));
    planes.addAll(fighter(m_data).create(4, british(m_data)));
    final Collection<Unit> defendingAA =
        territory("Germany", m_data).getUnits().getMatches(Matches.UnitIsAAforAnything);
    // 1 roll, a hit
    // then a dice to select the casualty
    final ScriptedRandomSource randomSource = new ScriptedRandomSource(new int[] {0, 1});
    m_bridge.setRandomSource(randomSource);
    final DiceRoll roll =
        DiceRoll
            .rollAA(
                Match.getMatches(planes,
                    Matches.unitIsOfTypes(
                        UnitAttachment.get(defendingAA.iterator().next().getType()).getTargetsAA(m_data))),
                defendingAA, m_bridge, territory("Germany", m_data), true);
    // make sure we rolled once
    assertEquals(1, randomSource.getTotalRolled());
    final Collection<Unit> casualties = BattleCalculator.getAACasualties(false, planes, planes, defendingAA,
        defendingAA, roll, m_bridge, null, null, null, territory("Germany", m_data), null, false, null).getKilled();
    assertEquals(casualties.size(), 3);
    // should be 1 fighter and 2 bombers
    assertEquals(Match.countMatches(casualties, Matches.UnitIsStrategicBomber), 2);
    assertEquals(Match.countMatches(casualties, Matches.UnitIsStrategicBomber.invert()), 1);
  }

  @Test
  public void testAACasualtiesLowLuckMixedWithRollingMissRadar() {
    // moved from BattleCalculatorTest because "revised" does not have "radar"
    final PlayerID british = GameDataTestUtil.british(m_data);
    final ITestDelegateBridge m_bridge = getDelegateBridge(british);
    makeGameLowLuck(m_data);
    // setSelectAACasualties(data, false);
    givePlayerRadar(germans(m_data));
    // 4 bombers and 4 fighters
    final Collection<Unit> planes = bomber(m_data).create(4, british(m_data));
    planes.addAll(fighter(m_data).create(4, british(m_data)));
    final Collection<Unit> defendingAA =
        territory("Germany", m_data).getUnits().getMatches(Matches.UnitIsAAforAnything);
    // 1 roll, a miss
    // then a dice to select the casualty
    final ScriptedRandomSource randomSource =
        new ScriptedRandomSource(new int[] {5, 0, 0, 0, ScriptedRandomSource.ERROR});
    m_bridge.setRandomSource(randomSource);
    final DiceRoll roll =
        DiceRoll
            .rollAA(
                Match.getMatches(planes,
                    Matches.unitIsOfTypes(
                        UnitAttachment.get(defendingAA.iterator().next().getType()).getTargetsAA(m_data))),
                defendingAA, m_bridge, territory("Germany", m_data), true);
    assertEquals(roll.getHits(), 2);
    // make sure we rolled once
    assertEquals(1, randomSource.getTotalRolled());
    final Collection<Unit> casualties = BattleCalculator.getAACasualties(false, planes, planes, defendingAA,
        defendingAA, roll, m_bridge, null, null, null, territory("Germany", m_data), null, false, null).getKilled();
    assertEquals(casualties.size(), 2);
    assertEquals(4, randomSource.getTotalRolled());
    // should be 1 fighter and 2 bombers
    assertEquals(Match.countMatches(casualties, Matches.UnitIsStrategicBomber), 1);
    assertEquals(Match.countMatches(casualties, Matches.UnitIsStrategicBomber.invert()), 1);
  }

  @Test
  public void testDefendingTrasnportsAutoKilled() {
    final Territory sz13 = m_data.getMap().getTerritory("13 Sea Zone");
    final Territory sz12 = m_data.getMap().getTerritory("12 Sea Zone");
    final PlayerID british = GameDataTestUtil.british(m_data);
    final MoveDelegate moveDelegate = moveDelegate(m_data);
    final ITestDelegateBridge bridge = getDelegateBridge(british);
    bridge.setStepName("CombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(bridge);
    moveDelegate.start();
    final Route sz12To13 = new Route();
    sz12To13.setStart(sz12);
    sz12To13.add(sz13);
    final String error = moveDelegate.move(sz12.getUnits().getUnits(), sz12To13);
    assertEquals(error, null);
    assertEquals(sz13.getUnits().size(), 3);
    moveDelegate.end();
    // the transport was not removed automatically
    assertEquals(sz13.getUnits().size(), 3);
    final BattleDelegate bd = battleDelegate(m_data);
    assertFalse(bd.getBattleTracker().getPendingBattleSites(false).isEmpty());
  }

  @Test
  public void testUnplacedDie() {
    final PlaceDelegate del = placeDelegate(m_data);
    del.setDelegateBridgeAndPlayer(getDelegateBridge(british(m_data)));
    del.start();
    addTo(british(m_data), transport(m_data).create(1, british(m_data)), m_data);
    del.end();
    // unplaced units die
    assertEquals(1, british(m_data).getUnits().size());
  }

  @Test
  public void testPlaceEmpty() {
    final PlaceDelegate del = placeDelegate(m_data);
    del.setDelegateBridgeAndPlayer(getDelegateBridge(british(m_data)));
    del.start();
    addTo(british(m_data), transport(m_data).create(1, british(m_data)), m_data);
    final String error = del.placeUnits(Collections.<Unit>emptyList(), territory("United Kingdom", m_data));
    assertNull(error);
  }

  @Test
  public void testTechTokens() {
    // Set up the test
    final PlayerID germans = GameDataTestUtil.germans(m_data);
    final ITestDelegateBridge delegateBridge = getDelegateBridge(germans);
    delegateBridge.setStepName("germanTech");
    final TechnologyDelegate techDelegate = techDelegate(m_data);
    techDelegate.setDelegateBridgeAndPlayer(delegateBridge);
    techDelegate.start();
    final TechnologyFrontier mech = new TechnologyFrontier("", m_data);
    mech.addAdvance(TechAdvance.findAdvance(TechAdvance.TECH_PROPERTY_MECHANIZED_INFANTRY, m_data, null));
    // Add tech token
    m_data.performChange(
        ChangeFactory.changeResourcesChange(germans, m_data.getResourceList().getResource(Constants.TECH_TOKENS), 1));
    // Check to make sure it was successful
    final int initTokens = germans.getResources().getQuantity("techTokens");
    assertEquals(1, initTokens);
    // Fail the roll
    delegateBridge.setRandomSource(new ScriptedRandomSource(new int[] {3}));
    final TechResults roll = techDelegate.rollTech(1, mech, 0, null);
    // Check to make sure it failed
    assertEquals(0, roll.getHits());
    final int midTokens = germans.getResources().getQuantity("techTokens");
    assertEquals(1, midTokens);
    // Make a Successful roll
    delegateBridge.setRandomSource(new ScriptedRandomSource(new int[] {5}));
    final TechResults roll2 = techDelegate.rollTech(1, mech, 0, null);
    // Check to make sure it succeeded and all tokens were removed
    assertEquals(1, roll2.getHits());
    final int finalTokens = germans.getResources().getQuantity("techTokens");
    assertEquals(0, finalTokens);
  }

  @Test
  public void testInfantryLoadOnlyTransports() {
    final Territory gibraltar = territory("Gibraltar", m_data);
    // add a tank to gibralter
    final PlayerID british = british(m_data);
    addTo(gibraltar, infantry(m_data).create(1, british));
    final MoveDelegate moveDelegate = moveDelegate(m_data);
    final ITestDelegateBridge bridge = getDelegateBridge(british);
    bridge.setStepName("britishCombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(bridge);
    moveDelegate.start();
    bridge.setRemote(new DummyTripleAPlayer());
    final Territory sz9 = territory("9 Sea Zone", m_data);
    final Territory sz13 = territory("13 Sea Zone", m_data);
    final Route sz9ToSz13 = new Route(sz9, territory("12 Sea Zone", m_data), sz13);
    // move the transport to attack, this is suicide but valid
    move(sz9.getUnits().getMatches(Matches.UnitIsTransport), sz9ToSz13);
    // load the transport
    load(gibraltar.getUnits().getUnits(), new Route(gibraltar, sz13));
    moveDelegate.end();
    bridge.setStepName("britishBattle");
    final BattleDelegate battleDelegate = battleDelegate(m_data);
    battleDelegate.setDelegateBridgeAndPlayer(bridge);
    battleDelegate.start();
    assertTrue(battleDelegate.getBattles().isEmpty());
  }

  @Test
  public void testLoadedTransportAttackKillsLoadedUnits() {
    final PlayerID british = british(m_data);
    final MoveDelegate moveDelegate = moveDelegate(m_data);
    final ITestDelegateBridge bridge = getDelegateBridge(british);
    bridge.setStepName("britishCombatMove");
    bridge.setRemote(new DummyTripleAPlayer() {
      @Override
      public boolean selectAttackSubs(final Territory unitTerritory) {
        return true;
      }
    });
    moveDelegate.setDelegateBridgeAndPlayer(bridge);
    moveDelegate.start();
    final Territory sz9 = territory("9 Sea Zone", m_data);
    final Territory sz7 = territory("7 Sea Zone", m_data);
    final Territory uk = territory("United Kingdom", m_data);
    final Route sz9ToSz7 = new Route(sz9, territory("8 Sea Zone", m_data), sz7);
    // move the transport to attack, this is suicide but valid
    final List<Unit> transports = sz9.getUnits().getMatches(Matches.UnitIsTransport);
    move(transports, sz9ToSz7);
    // load the transport
    load(uk.getUnits().getMatches(Matches.UnitIsInfantry), new Route(uk, sz7));
    moveDelegate(m_data).end();
    final ScriptedRandomSource randomSource = new ScriptedRandomSource(new int[] {0, 1});
    bridge.setRandomSource(randomSource);
    bridge.setStepName("britishBattle");
    final BattleDelegate battleDelegate = battleDelegate(m_data);
    battleDelegate.setDelegateBridgeAndPlayer(bridge);
    battleDelegate.start();
    assertEquals(2, TransportTracker.transporting(transports.get(0)).size());
    // fight the battle
    assertValid(fight(battleDelegate, sz7));
    // make sure the infantry die with the transport
    assertTrue(sz7.getUnits().toString(), sz7.getUnits().getMatches(Matches.unitOwnedBy(british)).isEmpty());
  }

  @Test
  public void testCanRetreatIntoEmptyEnemyTerritory() {
    final Territory eastPoland = territory("East Poland", m_data);
    final Territory ukraine = territory("Ukraine", m_data);
    final Territory poland = territory("Poland", m_data);
    // remove all units from east poland
    removeFrom(eastPoland, eastPoland.getUnits().getUnits());
    final MoveDelegate moveDelegate = moveDelegate(m_data);
    final ITestDelegateBridge delegateBridge = getDelegateBridge(germans(m_data));
    delegateBridge.setStepName("CombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(delegateBridge);
    moveDelegate.start();
    final Territory bulgaria = territory("Bulgaria Romania", m_data);
    // attack from bulgraia
    move(bulgaria.getUnits().getUnits(), new Route(bulgaria, ukraine));
    // add an air attack from east poland
    move(poland.getUnits().getMatches(Matches.UnitIsAir), new Route(poland, eastPoland, ukraine));
    // we should not be able to retreat to east poland!
    // that territory is still owned by the enemy
    final MustFightBattle battle =
        (MustFightBattle) AbstractMoveDelegate.getBattleTracker(m_data).getPendingBattle(ukraine, false, null);
    assertFalse(battle.getAttackerRetreatTerritories().contains(eastPoland));
  }

  @Test
  public void testCanRetreatIntoBlitzedTerritory() {
    final Territory eastPoland = territory("East Poland", m_data);
    final Territory ukraine = territory("Ukraine", m_data);
    final Territory poland = territory("Poland", m_data);
    // remove all units from east poland
    removeFrom(eastPoland, eastPoland.getUnits().getUnits());
    final MoveDelegate moveDelegate = moveDelegate(m_data);
    final ITestDelegateBridge delegateBridge = getDelegateBridge(germans(m_data));
    delegateBridge.setStepName("CombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(delegateBridge);
    moveDelegate.start();
    final Territory bulgaria = territory("Bulgaria Romania", m_data);
    // attack from bulgraia
    move(bulgaria.getUnits().getUnits(), new Route(bulgaria, ukraine));
    // add a blitz attack
    move(poland.getUnits().getMatches(Matches.UnitCanBlitz), new Route(poland, eastPoland, ukraine));
    // we should not be able to retreat to east poland!
    // that territory was just conquered
    final MustFightBattle battle =
        (MustFightBattle) AbstractMoveDelegate.getBattleTracker(m_data).getPendingBattle(ukraine, false, null);
    assertTrue(battle.getAttackerRetreatTerritories().contains(eastPoland));
  }

  @Test
  public void testCantBlitzFactoryOrAA() {
    // Set up territories
    final Territory poland = territory("Poland", m_data);
    final Territory eastPoland = territory("East Poland", m_data);
    final Territory ukraine = territory("Ukraine", m_data);
    // remove all units from east poland
    removeFrom(eastPoland, eastPoland.getUnits().getUnits());
    // Add a russian factory
    addTo(eastPoland, factory(m_data).create(1, russians(m_data)));
    MoveDelegate moveDelegate = moveDelegate(m_data);
    ITestDelegateBridge delegateBridge = getDelegateBridge(germans(m_data));
    delegateBridge.setStepName("CombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(delegateBridge);
    moveDelegate.start();
    // add a blitz attack
    String errorResults =
        moveDelegate.move(poland.getUnits().getMatches(Matches.UnitCanBlitz), new Route(poland, eastPoland, ukraine));
    assertError(errorResults);
    /*
     * Now try with an AA
     */
    // remove all units from east poland
    removeFrom(eastPoland, eastPoland.getUnits().getUnits());
    // Add a russian factory
    addTo(eastPoland, aaGun(m_data).create(1, russians(m_data)));
    moveDelegate = moveDelegate(m_data);
    delegateBridge = getDelegateBridge(germans(m_data));
    delegateBridge.setStepName("CombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(delegateBridge);
    moveDelegate.start();
    // add a blitz attack
    errorResults =
        moveDelegate.move(poland.getUnits().getMatches(Matches.UnitCanBlitz), new Route(poland, eastPoland, ukraine));
    assertError(errorResults);
  }

  @Test
  public void testMultipleAAInTerritory() {
    // Set up territories
    final Territory poland = territory("Poland", m_data);
    // Add a russian factory
    final Territory germany = territory("Germany", m_data);
    addTo(poland, aaGun(m_data).create(1, germans(m_data)));
    MoveDelegate moveDelegate = moveDelegate(m_data);
    ITestDelegateBridge delegateBridge = getDelegateBridge(germans(m_data));
    delegateBridge.setStepName("NonCombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(delegateBridge);
    moveDelegate.start();
    final int preCount = germany.getUnits().getUnitCount();
    /*
     * Move one
     */
    String errorResults =
        moveDelegate.move(poland.getUnits().getMatches(Matches.UnitIsAAforAnything), new Route(poland, germany));
    assertValid(errorResults);
    assertEquals(germany.getUnits().getUnitCount(), preCount + 1);
    /*
     * Test unloading TRN
     */
    final Territory finland = territory("Finland", m_data);
    final Territory sz5 = territory("5 Sea Zone", m_data);
    addTo(finland, aaGun(m_data).create(1, germans(m_data)));
    moveDelegate = moveDelegate(m_data);
    delegateBridge = getDelegateBridge(germans(m_data));
    delegateBridge.setStepName("NonCombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(delegateBridge);
    moveDelegate.start();
    // load the trn
    errorResults = moveDelegate.move(finland.getUnits().getMatches(Matches.UnitIsAAforAnything),
        new Route(finland, sz5), sz5.getUnits().getMatches(Matches.UnitIsTransport));
    assertValid(errorResults);
    // unload the trn
    errorResults = moveDelegate.move(sz5.getUnits().getMatches(Matches.UnitIsAAforAnything), new Route(sz5, germany));
    assertValid(errorResults);
    assertEquals(germany.getUnits().getUnitCount(), preCount + 2);
    /*
     * Test Building one
     */
    final UnitType aaGun = GameDataTestUtil.aaGun(m_data);
    final IntegerMap<UnitType> map = new IntegerMap<>();
    map.add(aaGun, 1);
    // Set up the test
    final PlayerID germans = GameDataTestUtil.germans(m_data);
    final PlaceDelegate placeDelegate = placeDelegate(m_data);
    delegateBridge.setStepName("Place");
    delegateBridge.setPlayerID(germans);
    placeDelegate.setDelegateBridgeAndPlayer(getDelegateBridge(germans(m_data)));
    placeDelegate.start();
    addTo(germans(m_data), aaGun(m_data).create(1, germans(m_data)), m_data);
    errorResults = placeDelegate.placeUnits(getUnits(map, germans), germany);
    assertValid(errorResults);
    assertEquals(germany.getUnits().getUnitCount(), preCount + 3);
  }

  @Test
  public void testMechanizedInfantry() {
    // Set up tech
    final PlayerID germans = GameDataTestUtil.germans(m_data);
    final ITestDelegateBridge delegateBridge = getDelegateBridge(germans(m_data));
    TechTracker.addAdvance(germans, delegateBridge,
        TechAdvance.findAdvance(TechAdvance.TECH_PROPERTY_MECHANIZED_INFANTRY, m_data, germans));
    // Set up the move delegate
    final MoveDelegate moveDelegate = moveDelegate(m_data);
    delegateBridge.setStepName("CombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(delegateBridge);
    moveDelegate.start();
    // Set up the territories
    final Territory poland = territory("Poland", m_data);
    final Territory eastPoland = territory("East Poland", m_data);
    final Territory belorussia = territory("Belorussia", m_data);
    // Set up the unit types
    final UnitType infantryType = GameDataTestUtil.infantry(m_data);
    // Remove all units from east poland
    removeFrom(eastPoland, eastPoland.getUnits().getUnits());
    // Get total number of units in territories to start
    final Integer preCountIntPoland = poland.getUnits().size();
    final Integer preCountIntBelorussia = belorussia.getUnits().size();
    // Get units
    final Collection<Unit> moveUnits = poland.getUnits().getUnits(infantryType, 3);
    moveUnits.addAll(poland.getUnits().getMatches(Matches.UnitCanBlitz));
    // add a INVALID blitz attack
    final String errorResults = moveDelegate.move(moveUnits, new Route(poland, eastPoland, belorussia));
    assertError(errorResults);
    // Fix the number of units
    moveUnits.clear();
    moveUnits.addAll(poland.getUnits().getUnits(infantryType, 2));
    moveUnits.addAll(poland.getUnits().getMatches(Matches.UnitCanBlitz));
    // add a VALID blitz attack
    final String validResults = moveDelegate.move(moveUnits, new Route(poland, eastPoland, belorussia));
    assertValid(validResults);
    // Get number of units in territories after move (adjusted for movement)
    final Integer postCountIntPoland = poland.getUnits().size() + 4;
    final Integer postCountIntBelorussia = belorussia.getUnits().size() - 4;
    // Compare the number of units before and after
    assertEquals(preCountIntPoland, postCountIntPoland);
    assertEquals(preCountIntBelorussia, postCountIntBelorussia);
  }

  @Test
  public void testJetPower() {
    // Set up tech
    final PlayerID germans = GameDataTestUtil.germans(m_data);
    final ITestDelegateBridge delegateBridge = getDelegateBridge(germans(m_data));
    TechTracker.addAdvance(germans, delegateBridge,
        TechAdvance.findAdvance(TechAdvance.TECH_PROPERTY_JET_POWER, m_data, germans));
    // Set up the territories
    final Territory poland = territory("Poland", m_data);
    final Territory eastPoland = territory("East Poland", m_data);
    // Set up the unit types
    final UnitType fighterType = GameDataTestUtil.fighter(m_data);
    delegateBridge.setStepName("germanBattle");
    while (!m_data.getSequence().getStep().getName().equals("germanBattle")) {
      m_data.getSequence().next();
    }
    final Collection<TerritoryEffect> territoryEffects = TerritoryEffectHelper.getEffects(eastPoland);
    // With JET_POWER attacking fighter hits on 4 (0 base)
    final List<Unit> germanFighter = (List<Unit>) poland.getUnits().getUnits(fighterType, 1);
    delegateBridge.setRandomSource(new ScriptedRandomSource(new int[] {3}));
    final DiceRoll roll1 = DiceRoll.rollDice(germanFighter, false, germans, delegateBridge, new MockBattle(eastPoland),
        "", territoryEffects, null);
    assertEquals(1, roll1.getHits());
    // With JET_POWER defending fighter misses on 5 (0 base)
    delegateBridge.setRandomSource(new ScriptedRandomSource(new int[] {4}));
    final DiceRoll roll2 = DiceRoll.rollDice(germanFighter, true, germans, delegateBridge, new MockBattle(eastPoland),
        "", territoryEffects, null);
    assertEquals(0, roll2.getHits());
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
  public void testFactoryPlace() {
    // Set up game
    final PlayerID british = GameDataTestUtil.british(m_data);
    final ITestDelegateBridge delegateBridge = getDelegateBridge(british(m_data));
    // Set up the territories
    final Territory egypt = territory("Union of South Africa", m_data);
    // Set up the unit types
    final UnitType factoryType = GameDataTestUtil.factory(m_data);
    // Set up the move delegate
    final PlaceDelegate placeDelegate = placeDelegate(m_data);
    delegateBridge.setStepName("Place");
    delegateBridge.setPlayerID(british);
    placeDelegate.setDelegateBridgeAndPlayer(delegateBridge);
    placeDelegate.start();
    // Add the factory
    final IntegerMap<UnitType> map = new IntegerMap<>();
    map.add(factoryType, 1);
    addTo(british(m_data), factory(m_data).create(1, british(m_data)), m_data);
    // Place the factory
    final String response = placeDelegate.placeUnits(getUnits(map, british), egypt);
    assertValid(response);
    // placeUnits performPlace
    // get production and unit production values
    final TerritoryAttachment ta = TerritoryAttachment.get(egypt);
    assertEquals(ta.getUnitProduction(), ta.getProduction());
  }

  @Test
  public void testChinesePlacement() {
    /*
     * This tests that Chinese can place units in any territory, that they can
     * place in just conquered territories, and that they can place in territories
     * with up to 3 Chinese units in them.
     */
    // Set up game
    final PlayerID chinese = GameDataTestUtil.chinese(m_data);
    final ITestDelegateBridge delegateBridge = getDelegateBridge(chinese(m_data));
    delegateBridge.setPlayerID(chinese);
    delegateBridge.setStepName("CombatMove");
    final MoveDelegate moveDelegate = moveDelegate(m_data);
    moveDelegate.setDelegateBridgeAndPlayer(delegateBridge);
    moveDelegate.start();
    // Set up the territories
    final Territory yunnan = territory("Yunnan", m_data);
    final Territory kiangsu = territory("Kiangsu", m_data);
    final Territory hupeh = territory("Hupeh", m_data);
    // Set up the unit types
    final UnitType infantryType = GameDataTestUtil.infantry(m_data);
    // Remove all units
    removeFrom(kiangsu, kiangsu.getUnits().getUnits());
    // add a VALID attack
    final Collection<Unit> moveUnits = hupeh.getUnits().getUnits();
    final String validResults = moveDelegate.move(moveUnits, new Route(hupeh, kiangsu));
    assertValid(validResults);
    /*
     * Place units in just captured territory
     */
    final PlaceDelegate placeDelegate = placeDelegate(m_data);
    delegateBridge.setStepName("Place");
    placeDelegate.setDelegateBridgeAndPlayer(delegateBridge);
    placeDelegate.start();
    // Add the infantry
    IntegerMap<UnitType> map = new IntegerMap<>();
    map.add(infantryType, 3);
    addTo(chinese(m_data), infantry(m_data).create(1, chinese(m_data)), m_data);
    // Get the number of units before placing
    int preCount = kiangsu.getUnits().getUnitCount();
    // Place the infantry
    String response = placeDelegate.placeUnits(getUnits(map, chinese), kiangsu);
    assertValid(response);
    assertEquals(preCount + 1, kiangsu.getUnits().getUnitCount());
    /*
     * Place units in a territory with up to 3 Chinese units
     */
    // Add the infantry
    map = new IntegerMap<>();
    map.add(infantryType, 3);
    addTo(chinese(m_data), infantry(m_data).create(3, chinese(m_data)), m_data);
    // Get the number of units before placing
    preCount = yunnan.getUnits().getUnitCount();
    // Place the infantry
    response = placeDelegate.placeUnits(getUnits(map, chinese), yunnan);
    assertValid(response);
    final int midCount = yunnan.getUnits().getUnitCount();
    // Make sure they were all placed
    assertEquals(preCount, midCount - 3);
    /*
     * Place units in a territory with 3 or more Chinese units
     */
    map = new IntegerMap<>();
    map.add(infantryType, 1);
    addTo(chinese(m_data), infantry(m_data).create(1, chinese(m_data)), m_data);
    response = placeDelegate.placeUnits(getUnits(map, chinese), yunnan);
    assertError(response);
    // Make sure none were placed
    final int postCount = yunnan.getUnits().getUnitCount();
    assertEquals(midCount, postCount);
  }

  @Test
  public void testPlaceInOccupiedSZ() {
    // Set up game
    final PlayerID germans = GameDataTestUtil.germans(m_data);
    final ITestDelegateBridge delegateBridge = getDelegateBridge(british(m_data));
    // Clear all units from the SZ and add an enemy unit
    final Territory sz5 = territory("5 Sea Zone", m_data);
    removeFrom(sz5, sz5.getUnits().getUnits());
    addTo(sz5, destroyer(m_data).create(1, british(m_data)));
    // Set up the unit types
    final UnitType transportType = GameDataTestUtil.transport(m_data);
    // Set up the move delegate
    final PlaceDelegate placeDelegate = placeDelegate(m_data);
    delegateBridge.setStepName("Place");
    delegateBridge.setPlayerID(germans);
    placeDelegate.setDelegateBridgeAndPlayer(delegateBridge);
    placeDelegate.start();
    // Add the transport
    final IntegerMap<UnitType> map = new IntegerMap<>();
    map.add(transportType, 1);
    addTo(germans(m_data), transport(m_data).create(1, germans(m_data)), m_data);
    // Place it
    final String response = placeDelegate.placeUnits(getUnits(map, germans), sz5);
    assertValid(response);
  }

  @Test
  public void testMoveUnitsThroughSubs() {
    final ITestDelegateBridge bridge = getDelegateBridge(british(m_data));
    bridge.setStepName("britishNonCombatMove");
    moveDelegate(m_data).setDelegateBridgeAndPlayer(bridge);
    moveDelegate(m_data).start();
    final Territory sz6 = territory("6 Sea Zone", m_data);
    final Route route = new Route(sz6, territory("7 Sea Zone", m_data), territory("8 Sea Zone", m_data));
    final String error = moveDelegate(m_data).move(sz6.getUnits().getUnits(), route);
    assertNull(error, error);
  }

  @Test
  public void testMoveUnitsThroughTransports() {
    final ITestDelegateBridge bridge = getDelegateBridge(british(m_data));
    bridge.setStepName("britishCombatMove");
    moveDelegate(m_data).setDelegateBridgeAndPlayer(bridge);
    moveDelegate(m_data).start();
    final Territory sz12 = territory("12 Sea Zone", m_data);
    final Route route = new Route(sz12, territory("13 Sea Zone", m_data), territory("14 Sea Zone", m_data));
    final String error = moveDelegate(m_data).move(sz12.getUnits().getUnits(), route);
    assertNull(error, error);
  }

  @Test
  public void testMoveUnitsThroughTransports2() {
    final ITestDelegateBridge bridge = getDelegateBridge(british(m_data));
    bridge.setStepName("britishNonCombatMove");
    moveDelegate(m_data).setDelegateBridgeAndPlayer(bridge);
    moveDelegate(m_data).start();
    final Territory sz12 = territory("12 Sea Zone", m_data);
    final Territory sz14 = territory("14 Sea Zone", m_data);
    removeFrom(sz14, sz14.getUnits().getUnits());
    final Route route = new Route(sz12, territory("13 Sea Zone", m_data), sz14);
    final String error = moveDelegate(m_data).move(sz12.getUnits().getUnits(), route);
    assertNull(error, error);
  }

  @Test
  public void testLoadThroughSubs() {
    final ITestDelegateBridge bridge = getDelegateBridge(british(m_data));
    bridge.setStepName("britishNonCombatMove");
    final MoveDelegate moveDelegate = moveDelegate(m_data);
    moveDelegate.setDelegateBridgeAndPlayer(bridge);
    moveDelegate.start();
    final Territory sz8 = territory("8 Sea Zone", m_data);
    final Territory sz7 = territory("7 Sea Zone", m_data);
    final Territory sz6 = territory("6 Sea Zone", m_data);
    final Territory uk = territory("United Kingdom", m_data);
    // add a transport
    addTo(sz8, transport(m_data).create(1, british(m_data)));
    // move the transport where to the sub is
    assertValid(moveDelegate.move(sz8.getUnits().getUnits(), new Route(sz8, sz7)));
    // load the transport
    load(uk.getUnits().getMatches(Matches.UnitIsInfantry), new Route(uk, sz7));
    // move the transport out
    assertValid(
        moveDelegate.move(sz7.getUnits().getMatches(Matches.unitOwnedBy(british(m_data))), new Route(sz7, sz6)));
  }

  @Test
  public void testAttackUndoAndAttackAgain() {
    final MoveDelegate move = moveDelegate(m_data);
    final ITestDelegateBridge bridge = getDelegateBridge(italians(m_data));
    bridge.setStepName("CombatMove");
    move.setDelegateBridgeAndPlayer(bridge);
    move.start();
    final Territory sz14 = territory("14 Sea Zone", m_data);
    final Territory sz13 = territory("13 Sea Zone", m_data);
    final Territory sz12 = territory("12 Sea Zone", m_data);
    final Route r = new Route(sz14, sz13, sz12);
    // move the battleship
    move(sz14.getUnits().getMatches(Matches.UnitHasMoreThanOneHitPointTotal), r);
    // move everything
    move(sz14.getUnits().getMatches(Matches.UnitIsNotTransport), r);
    // undo it
    move.undoMove(1);
    // move again
    move(sz14.getUnits().getMatches(Matches.UnitIsNotTransport), r);
    final MustFightBattle mfb =
        (MustFightBattle) AbstractMoveDelegate.getBattleTracker(m_data).getPendingBattle(sz12, false, null);
    // only 3 attacking units
    // the battleship and the two cruisers
    assertEquals(3, mfb.getAttackingUnits().size());
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
        Arrays.asList(attacker + SUBS_SUBMERGE, defender + SUBS_SUBMERGE, attacker + SUBS_FIRE,
            defender + SELECT_SUB_CASUALTIES, defender + SUBS_FIRE, attacker + SELECT_SUB_CASUALTIES,
            REMOVE_SNEAK_ATTACK_CASUALTIES, REMOVE_CASUALTIES, attacker + ATTACKER_WITHDRAW).toString(),
        steps.toString());
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
    // 1 sub attacks 1 sub and 1 destroyer
    // defender sneak attacks, not attacker
    addTo(from, submarine(m_data).create(1, british(m_data)));
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
    assertEquals(
        Arrays.asList(defender + SUBS_SUBMERGE, defender + SUBS_FIRE, attacker + SELECT_SUB_CASUALTIES,
            REMOVE_SNEAK_ATTACK_CASUALTIES, attacker + SUBS_FIRE, defender + SELECT_SUB_CASUALTIES, defender + FIRE,
            attacker + SELECT_CASUALTIES, REMOVE_CASUALTIES, attacker + ATTACKER_WITHDRAW).toString(),
        steps.toString());
    bridge.setRemote(new DummyTripleAPlayer());
    // defending subs sneak attack and hit
    // no chance to return fire
    final ScriptedRandomSource randomSource = new ScriptedRandomSource(0, ScriptedRandomSource.ERROR);
    bridge.setRandomSource(randomSource);
    battle.fight(bridge);
    assertEquals(1, randomSource.getTotalRolled());
    assertTrue(attacked.getUnits().getMatches(Matches.unitIsOwnedBy(british(m_data))).isEmpty());
    assertEquals(2, attacked.getUnits().size());
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
    assertEquals(
        Arrays.asList(attacker + SUBS_SUBMERGE, attacker + SUBS_FIRE, defender + SELECT_SUB_CASUALTIES,
            REMOVE_SNEAK_ATTACK_CASUALTIES, attacker + FIRE, defender + SELECT_CASUALTIES, defender + SUBS_FIRE,
            attacker + SELECT_SUB_CASUALTIES, REMOVE_CASUALTIES, attacker + ATTACKER_WITHDRAW).toString(),
        steps.toString());
    bridge.setRemote(new DummyTripleAPlayer());
    // attacking subs sneak attack and hit
    // no chance to return fire
    final ScriptedRandomSource randomSource = new ScriptedRandomSource(0, ScriptedRandomSource.ERROR);
    bridge.setRandomSource(randomSource);
    battle.fight(bridge);
    assertEquals(1, randomSource.getTotalRolled());
    assertTrue(attacked.getUnits().getMatches(Matches.unitIsOwnedBy(germans(m_data))).isEmpty());
    assertEquals(2, attacked.getUnits().size());
  }

  @Test
  public void testAttackDestroyerAndSubsAgainstSubAndDestroyer() {
    final String defender = "Germans";
    final String attacker = "British";
    final Territory attacked = territory("31 Sea Zone", m_data);
    final Territory from = territory("32 Sea Zone", m_data);
    // 1 sub and 1 destroyer attack 1 sub and 1 destroyer
    // no sneak attacks
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
    assertEquals(
        Arrays.asList(attacker + SUBS_FIRE, defender + SELECT_SUB_CASUALTIES, attacker + FIRE,
            defender + SELECT_CASUALTIES, defender + SUBS_FIRE, attacker + SELECT_SUB_CASUALTIES, defender + FIRE,
            attacker + SELECT_CASUALTIES, REMOVE_CASUALTIES, attacker + ATTACKER_WITHDRAW).toString(),
        steps.toString());
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
    // attacking subs sneak attack and hit
    // no chance to return fire
    final ScriptedRandomSource randomSource = new ScriptedRandomSource(0, 0, 0, 0, ScriptedRandomSource.ERROR);
    bridge.setRandomSource(randomSource);
    battle.fight(bridge);
    assertEquals(4, randomSource.getTotalRolled());
    assertEquals(0, attacked.getUnits().size());
  }

  @Test
  public void testLimitBombardtoNumberOfUnloaded() {
    final MoveDelegate move = moveDelegate(m_data);
    final ITestDelegateBridge bridge = getDelegateBridge(italians(m_data));
    bridge.setRemote(new DummyTripleAPlayer() {
      @Override
      public boolean selectShoreBombard(final Territory unitTerritory) {
        return true;
      }
    });
    bridge.setStepName("CombatMove");
    move.setDelegateBridgeAndPlayer(bridge);
    move.start();
    final Territory sz14 = territory("14 Sea Zone", m_data);
    final Territory sz15 = territory("15 Sea Zone", m_data);
    final Territory eg = territory("Egypt", m_data);
    final Territory li = territory("Libya", m_data);
    final Territory balkans = territory("Balkans", m_data);
    // Clear all units from the attacked terr
    removeFrom(eg, eg.getUnits().getUnits());
    // Add 2 inf
    final PlayerID british = GameDataTestUtil.british(m_data);
    addTo(eg, infantry(m_data).create(2, british));
    // load the transports
    load(balkans.getUnits().getMatches(Matches.UnitIsInfantry), new Route(balkans, sz14));
    // move the fleet
    move(sz14.getUnits().getUnits(), new Route(sz14, sz15));
    // move troops from Libya
    move(li.getUnits().getMatches(Matches.unitOwnedBy(italians(m_data))), new Route(li, eg));
    // unload the transports
    move(sz15.getUnits().getMatches(Matches.UnitIsLand), new Route(sz15, eg));
    move.end();
    // start the battle phase, this will ask the user to bombard
    battleDelegate(m_data).setDelegateBridgeAndPlayer(bridge);
    battleDelegate(m_data).start();
    final MustFightBattle mfb =
        (MustFightBattle) AbstractMoveDelegate.getBattleTracker(m_data).getPendingBattle(eg, false, null);
    // only 2 ships are allowed to bombard (there are 1 battleship and 2 cruisers that COULD bombard, but only 2 ships
    // may bombard total)
    assertEquals(2, mfb.getBombardingUnits().size());
    // Show that bombard casualties can return fire
    // Note- the 3 & 2 hits below show default behavior of bombarding at attack strength
    // 3= Battleship hitting a 4, 2=Cruiser hitting a 3, 5555=italian infantry missing on 6s, 00= british getting return
    // fire on 1.
    bridge.setRandomSource(new ScriptedRandomSource(3, 2, 5, 5, 5, 5, 0, 0));
    battleDelegate(m_data).setDelegateBridgeAndPlayer(bridge);
    battleDelegate(m_data).start();
    fight(battleDelegate(m_data), eg);
    // end result should be 2 italian infantry.
    assertEquals(2, eg.getUnits().size());
  }

  @Test
  public void testBombardStrengthVariable() {
    final MoveDelegate move = moveDelegate(m_data);
    final ITestDelegateBridge bridge = getDelegateBridge(italians(m_data));
    bridge.setRemote(new DummyTripleAPlayer() {
      @Override
      public boolean selectShoreBombard(final Territory unitTerritory) {
        return true;
      }
    });
    bridge.setStepName("CombatMove");
    move.setDelegateBridgeAndPlayer(bridge);
    move.start();
    final Territory sz14 = territory("14 Sea Zone", m_data);
    final Territory sz15 = territory("15 Sea Zone", m_data);
    final Territory eg = territory("Egypt", m_data);
    final Territory balkans = territory("Balkans", m_data);
    // Clear all units
    removeFrom(eg, eg.getUnits().getUnits());
    removeFrom(sz14, sz14.getUnits().getUnits());
    // Add 2 inf to the attacked terr
    final PlayerID british = GameDataTestUtil.british(m_data);
    addTo(eg, infantry(m_data).create(2, british));
    // create/load the destroyers and transports
    final PlayerID italians = GameDataTestUtil.italians(m_data);
    addTo(sz14, transport(m_data).create(1, italians));
    addTo(sz14, destroyer(m_data).create(2, italians));
    // load the transports
    load(balkans.getUnits().getMatches(Matches.UnitIsInfantry), new Route(balkans, sz14));
    // move the fleet
    move(sz14.getUnits().getUnits(), new Route(sz14, sz15));
    // unload the transports
    move(sz15.getUnits().getMatches(Matches.UnitIsLand), new Route(sz15, eg));
    move.end();
    // Set the tech for DDs bombard
    // ww2v3 doesn't have this tech, so this does nothing...
    // TechAttachment.get(italians).setDestroyerBombard("true");
    UnitAttachment.get(destroyer(m_data)).setCanBombard("true");
    // Set the bombard strength for the DDs
    final Collection<Unit> dds = Match.getMatches(sz15.getUnits().getUnits(), Matches.UnitIsDestroyer);
    final Iterator<Unit> ddIter = dds.iterator();
    while (ddIter.hasNext()) {
      final Unit unit = ddIter.next();
      final UnitAttachment ua = UnitAttachment.get(unit.getType());
      ua.setBombard("3");
    }
    // start the battle phase, this will ask the user to bombard
    battleDelegate(m_data).setDelegateBridgeAndPlayer(bridge);
    battleDelegate(m_data).start();
    final MustFightBattle mfb =
        (MustFightBattle) AbstractMoveDelegate.getBattleTracker(m_data).getPendingBattle(eg, false, null);
    assertNotNull(mfb);
    // Show that bombard casualties can return fire
    // destroyer bombard hit/miss on rolls of 4 & 3
    // landing inf miss
    // defending inf hit
    bridge.setRandomSource(new ScriptedRandomSource(3, 2, 6, 6, 1, 1));
    battleDelegate(m_data).setDelegateBridgeAndPlayer(bridge);
    battleDelegate(m_data).start();
    fight(battleDelegate(m_data), eg);
    // 1 defending inf remaining
    assertEquals(1, eg.getUnits().size());
  }

  @Test
  public void testAmphAttackUndoAndAttackAgainBombard() {
    final MoveDelegate move = moveDelegate(m_data);
    final ITestDelegateBridge bridge = getDelegateBridge(italians(m_data));
    bridge.setRemote(new DummyTripleAPlayer() {
      @Override
      public boolean selectShoreBombard(final Territory unitTerritory) {
        return true;
      }
    });
    bridge.setStepName("CombatMove");
    move.setDelegateBridgeAndPlayer(bridge);
    move.start();
    final Territory sz14 = territory("14 Sea Zone", m_data);
    final Territory sz15 = territory("15 Sea Zone", m_data);
    final Territory eg = territory("Egypt", m_data);
    final Territory li = territory("Libya", m_data);
    final Territory balkans = territory("Balkans", m_data);
    // load the transports
    load(balkans.getUnits().getMatches(Matches.UnitIsInfantry), new Route(balkans, sz14));
    // move the fleet
    move(sz14.getUnits().getUnits(), new Route(sz14, sz15));
    // move troops from Libya
    move(li.getUnits().getMatches(Matches.unitOwnedBy(italians(m_data))), new Route(li, eg));
    // unload the transports
    move(sz15.getUnits().getMatches(Matches.UnitIsLand), new Route(sz15, eg));
    // undo amphibious landing
    move.undoMove(move.getMovesMade().size() - 1);
    // move again
    move(sz15.getUnits().getMatches(Matches.UnitIsLand), new Route(sz15, eg));
    move.end();
    // start the battle phase, this will ask the user to bombard
    battleDelegate(m_data).setDelegateBridgeAndPlayer(bridge);
    battleDelegate(m_data).start();
    final MustFightBattle mfb =
        (MustFightBattle) AbstractMoveDelegate.getBattleTracker(m_data).getPendingBattle(eg, false, null);
    // only 2 battleships are allowed to bombard
    assertEquals(2, mfb.getBombardingUnits().size());
  }

  // TODO this test needs work kev
  @Test
  public void testAAFireWithRadar() {
    final PlayerID russians = russians(m_data);
    final PlayerID germans = germans(m_data);
    TechAttachment.get(russians).setAARadar("true");
    final MoveDelegate move = moveDelegate(m_data);
    final ITestDelegateBridge bridge = getDelegateBridge(germans);
    bridge.setStepName("CombatMove");
    final Territory poland = territory("Poland", m_data);
    final Territory russia = territory("Russia", m_data);
    // Add bomber to Poland and attack
    addTo(poland, bomber(m_data).create(1, germans));
    // The game will ask us if we want to move bomb, say yes.
    final InvocationHandler handler = new InvocationHandler() {
      @Override
      public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
        return true;
      }
    };
    final ITripleAPlayer player = (ITripleAPlayer) Proxy
        .newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class[] {ITripleAPlayer.class}, handler);
    bridge.setRemote(player);
    // Perform the combat movement
    move.setDelegateBridgeAndPlayer(bridge);
    move.start();
    move(poland.getUnits().getMatches(Matches.UnitIsStrategicBomber), m_data.getMap().getRoute(poland, russia));
    move.end();
    // start the battle phase
    battleDelegate(m_data).setDelegateBridgeAndPlayer(bridge);
    battleDelegate(m_data).start();
    // aa guns rolls 1, hits
    bridge.setRandomSource(new ScriptedRandomSource(new int[] {0, ScriptedRandomSource.ERROR}));
    final StrategicBombingRaidBattle battle =
        (StrategicBombingRaidBattle) battleDelegate(m_data).getBattleTracker().getPendingBattle(russia, true, null);
    // aa guns rolls 1, hits
    // bridge.setRandomSource(new ScriptedRandomSource( new int[] {0, 6} ));
    final int PUsBeforeRaid = russians.getResources().getQuantity(m_data.getResourceList().getResource(Constants.PUS));
    battle.fight(bridge);
    final int PUsAfterRaid = russians.getResources().getQuantity(m_data.getResourceList().getResource(Constants.PUS));
    // Changed to match StrategicBombingRaidBattle changes
    assertEquals(PUsBeforeRaid, PUsAfterRaid);
  }

  @Test
  public void testCarrierWithAlliedPlanes() {
    final Territory sz8 = territory("8 Sea Zone", m_data);
    final Territory sz1 = territory("1 Sea Zone", m_data);
    addTo(sz8, carrier(m_data).create(1, british(m_data)));
    addTo(sz8, fighter(m_data).create(1, americans(m_data)));
    final Route route = new Route(sz8, sz1);
    final ITestDelegateBridge bridge = getDelegateBridge(british(m_data));
    bridge.setStepName("CombatMove");
    moveDelegate(m_data).setDelegateBridgeAndPlayer(bridge);
    moveDelegate(m_data).start();
    move(sz8.getUnits().getUnits(), route);
    // make sure the fighter moved
    assertTrue(sz8.getUnits().getUnits().isEmpty());
    assertFalse(sz1.getUnits().getMatches(Matches.UnitIsAir).isEmpty());
  }

  @Test
  public void testAirCanLandWithAlliedFighters() {
    // germany owns madagascar, with 2 fighters in it
    // also 1 carrier, and 1 allied fighter in sz 40
    // the fighters should not be able to move from madagascar
    // to sz 40, since with the allied fighter, their is no room
    // on the carrier
    final Territory madagascar = territory("French Madagascar", m_data);
    final PlayerID germans = germans(m_data);
    madagascar.setOwner(germans);
    final Territory sz40 = territory("40 Sea Zone", m_data);
    addTo(sz40, carrier(m_data).create(1, germans));
    addTo(sz40, fighter(m_data).create(1, italians(m_data)));
    addTo(madagascar, fighter(m_data).create(2, germans));
    final Route route = m_data.getMap().getRoute(madagascar, sz40);
    final ITestDelegateBridge bridge = getDelegateBridge(germans);
    bridge.setStepName("CombatMove");
    moveDelegate(m_data).setDelegateBridgeAndPlayer(bridge);
    moveDelegate(m_data).start();
    // don't allow kamikaze
    bridge.setRemote(new DummyTripleAPlayer() {
      @Override
      public boolean confirmMoveKamikaze() {
        return false;
      }
    });
    final String error = moveDelegate(m_data).move(madagascar.getUnits().getUnits(), route);
    assertError(error);
  }

  @Test
  public void testMechInfSimple() {
    final PlayerID germans = germans(m_data);
    final Territory france = territory("France", m_data);
    final Territory germany = territory("Germany", m_data);
    final Territory poland = territory("Poland", m_data);
    TechAttachment.get(germans).setMechanizedInfantry("true");
    final ITestDelegateBridge bridge = getDelegateBridge(germans);
    bridge.setStepName("CombatMove");
    moveDelegate(m_data).setDelegateBridgeAndPlayer(bridge);
    moveDelegate(m_data).start();
    final Route r = new Route(france, germany, poland);
    final List<Unit> toMove = new ArrayList<>();
    // 1 armour and 1 infantry
    toMove.addAll(france.getUnits().getMatches(Matches.UnitCanBlitz));
    toMove.add(france.getUnits().getMatches(Matches.UnitIsInfantry).get(0));
    move(toMove, r);
  }

  @Test
  public void testMechInfUnitAlreadyMovedSimple() {
    final PlayerID germans = germans(m_data);
    final Territory france = territory("France", m_data);
    final Territory germany = territory("Germany", m_data);
    TechAttachment.get(germans).setMechanizedInfantry("true");
    final ITestDelegateBridge bridge = getDelegateBridge(germans);
    bridge.setStepName("CombatMove");
    moveDelegate(m_data).setDelegateBridgeAndPlayer(bridge);
    moveDelegate(m_data).start();
    // get rid of the infantry in france
    removeFrom(france, france.getUnits().getMatches(Matches.UnitIsInfantry));
    // move an infantry from germany to france
    move(germany.getUnits().getMatches(Matches.UnitIsInfantry).subList(0, 1), new Route(germany, france));
    // try to move all the units in france, the infantry should not be able to move
    final Route r = new Route(france, germany);
    final String error = moveDelegate(m_data).move(france.getUnits().getUnits(), r);
    assertFalse(error == null);
  }

  @Test
  public void testParatroopsWalkOnWater() {
    final PlayerID germans = germans(m_data);
    final Territory france = territory("France", m_data);
    TechAttachment.get(germans).setParatroopers("true");
    final Route r = m_data.getMap().getRoute(france, territory("7 Sea Zone", m_data));
    final Collection<Unit> paratroopers = france.getUnits().getMatches(Matches.UnitIsAirTransportable);
    assertFalse(paratroopers.isEmpty());
    final MoveValidationResult results = MoveValidator.validateMove(paratroopers, r, germans,
        Collections.<Unit>emptyList(), new HashMap<>(), false, null, m_data);
    assertFalse(results.isMoveValid());
  }

  @Test
  public void testBomberWithTankOverWaterParatroopers() {
    final PlayerID germans = germans(m_data);
    TechAttachment.get(germans).setParatroopers("true");
    final Territory sz5 = territory("5 Sea Zone", m_data);
    final Territory germany = territory("Germany", m_data);
    final Territory karelia = territory("Karelia S.S.R.", m_data);
    addTo(germany, armour(m_data).create(1, germans));
    final Route r = new Route(germany, sz5, karelia);
    final Collection<Unit> toMove = germany.getUnits().getMatches(Matches.UnitCanBlitz);
    toMove.addAll(germany.getUnits().getMatches(Matches.UnitIsStrategicBomber));
    assertEquals(2, toMove.size());
    final MoveValidationResult results = MoveValidator.validateMove(toMove, r, germans, Collections.<Unit>emptyList(),
        new HashMap<>(), false, null, m_data);
    assertFalse(results.isMoveValid());
  }

  @Test
  public void testBomberTankOverWater() {
    // can't transport a tank over water using a bomber
    final PlayerID germans = germans(m_data);
    final Territory sz5 = territory("5 Sea Zone", m_data);
    final Territory germany = territory("Germany", m_data);
    final Territory karelia = territory("Karelia S.S.R.", m_data);
    addTo(germany, armour(m_data).create(1, germans));
    final Route r = new Route(germany, sz5, karelia);
    final Collection<Unit> toMove = germany.getUnits().getMatches(Matches.UnitCanBlitz);
    toMove.addAll(germany.getUnits().getMatches(Matches.UnitIsStrategicBomber));
    assertEquals(2, toMove.size());
    final MoveValidationResult results = MoveValidator.validateMove(toMove, r, germans, Collections.<Unit>emptyList(),
        new HashMap<>(), false, null, m_data);
    assertFalse(results.isMoveValid());
  }

  @Test
  public void testMoveParatroopersAsNonPartroops() {
    // move a bomber and a paratrooper
    // one step, but as a normal movement
    final PlayerID germans = germans(m_data);
    final Territory germany = territory("Germany", m_data);
    final Territory nwe = territory("Northwestern Europe", m_data);
    final ITestDelegateBridge bridge = getDelegateBridge(germans);
    bridge.setStepName("CombatMove");
    moveDelegate(m_data).setDelegateBridgeAndPlayer(bridge);
    moveDelegate(m_data).start();
    TechAttachment.get(germans).setParatroopers("true");
    List<Unit> paratrooper = germany.getUnits().getMatches(Matches.UnitIsAirTransportable);
    paratrooper = paratrooper.subList(0, 1);
    final List<Unit> bomberAndParatroop = new ArrayList<>(paratrooper);
    bomberAndParatroop.addAll(germany.getUnits().getMatches(Matches.UnitIsAirTransport));
    // move to nwe, this is a valid move, and it not a partroop move
    move(bomberAndParatroop, new Route(germany, nwe));
  }

  @Test
  public void testCantMoveParatroopersThatMovedPreviously() {
    // make sure infantry can't be moved as paratroopers after moving
    final PlayerID germans = germans(m_data);
    final Territory germany = territory("Germany", m_data);
    final Territory nwe = territory("Northwestern Europe", m_data);
    final Territory poland = territory("Poland", m_data);
    final Territory eastPoland = territory("East Poland", m_data);
    final ITestDelegateBridge bridge = getDelegateBridge(germans);
    bridge.setStepName("CombatMove");
    moveDelegate(m_data).setDelegateBridgeAndPlayer(bridge);
    moveDelegate(m_data).start();
    TechAttachment.get(germans).setParatroopers("true");
    final List<Unit> paratrooper = nwe.getUnits().getMatches(Matches.UnitIsAirTransportable);
    move(paratrooper, new Route(nwe, germany));
    final List<Unit> bomberAndParatroop = new ArrayList<>(paratrooper);
    bomberAndParatroop.addAll(germany.getUnits().getMatches(Matches.UnitIsAirTransport));
    // move the units to east poland
    final String error = moveDelegate(m_data).move(bomberAndParatroop, new Route(germany, poland, eastPoland));
    assertError(error);
  }

  @Test
  public void testCantTransportParatroopersWithBombersThatMovedPreviously() {
    // make sure bombers can't move then pick up paratroopers
    final PlayerID germans = germans(m_data);
    final Territory germany = territory("Germany", m_data);
    final Territory bulgaria = territory("Bulgaria Romania", m_data);
    final Territory poland = territory("Poland", m_data);
    final Territory ukraine = territory("Ukraine", m_data);
    final ITestDelegateBridge bridge = getDelegateBridge(germans);
    bridge.setStepName("CombatMove");
    moveDelegate(m_data).setDelegateBridgeAndPlayer(bridge);
    moveDelegate(m_data).start();
    TechAttachment.get(germans).setParatroopers("true");
    // Move the bomber first
    final List<Unit> bomber = germany.getUnits().getMatches(Matches.UnitIsAirTransport);
    move(bomber, new Route(germany, poland));
    // Pick up a paratrooper
    final List<Unit> bomberAndParatroop = new ArrayList<>(bomber);
    bomberAndParatroop.addAll(poland.getUnits().getUnits(GameDataTestUtil.infantry(m_data), 1));
    // move them
    final String error = moveDelegate(m_data).move(bomberAndParatroop, new Route(poland, bulgaria, ukraine));
    assertError(error);
  }

  @Test
  public void testMoveOneParatrooperPerBomber() {
    // make sure only 1 paratroop per bomber can be moved
    final PlayerID germans = germans(m_data);
    final Territory germany = territory("Germany", m_data);
    // Territory nwe = territory("Northwestern Europe", m_data);
    final Territory poland = territory("Poland", m_data);
    final Territory eastPoland = territory("East Poland", m_data);
    final ITestDelegateBridge bridge = getDelegateBridge(germans);
    bridge.setStepName("CombatMove");
    moveDelegate(m_data).setDelegateBridgeAndPlayer(bridge);
    moveDelegate(m_data).start();
    TechAttachment.get(germans).setParatroopers("true");
    final List<Unit> bomberAndParatroop = new ArrayList<>();
    bomberAndParatroop.addAll(germany.getUnits().getMatches(Matches.UnitIsAirTransport));
    // add 2 infantry
    bomberAndParatroop.addAll(germany.getUnits().getUnits(GameDataTestUtil.infantry(m_data), 2));
    // move the units to east poland
    final String error = moveDelegate(m_data).move(bomberAndParatroop, new Route(germany, poland, eastPoland));
    assertError(error);
  }

  @Test
  public void testParatroopersMoveTwice() {
    // After a battle move to put a bomber + infantry (paratroop) in a first enemy
    // territory, you can make a new move (in the same battle move round) to put
    // bomber+ infantry in a more internal enemy territory.
    final PlayerID germans = germans(m_data);
    final Territory germany = territory("Germany", m_data);
    final Territory poland = territory("Poland", m_data);
    final Territory eastPoland = territory("East Poland", m_data);
    final Territory beloRussia = territory("Belorussia", m_data);
    final ITestDelegateBridge bridge = getDelegateBridge(germans);
    bridge.setStepName("CombatMove");
    moveDelegate(m_data).setDelegateBridgeAndPlayer(bridge);
    moveDelegate(m_data).start();
    TechAttachment.get(germans).setParatroopers("true");
    List<Unit> paratroopers = germany.getUnits().getMatches(Matches.UnitIsAirTransportable);
    paratroopers = paratroopers.subList(0, 1);
    final List<Unit> bomberAndParatroop = new ArrayList<>(paratroopers);
    bomberAndParatroop.addAll(germany.getUnits().getMatches(Matches.UnitIsAirTransport));
    final Route route = new Route(germany, poland, eastPoland);
    final List<Unit> airTransports = germany.getUnits().getMatches(Matches.UnitIsAirTransport);
    for (final Unit airTransport : airTransports) {
      for (final Unit unit : paratroopers) {
        final Change change = TransportTracker.loadTransportChange((TripleAUnit) airTransport, unit);
        bridge.addChange(change);
      }
    }
    // move the units to east poland
    // airTransports
    String error = moveDelegate(m_data).move(bomberAndParatroop, route);
    assertValid(error);
    // try to move them further, this should fail
    error = moveDelegate(m_data).move(bomberAndParatroop, new Route(eastPoland, beloRussia));
    assertError(error);
  }

  @Test
  public void testParatroopersFlyOverBlitzedTerritory() {
    // We should be able to blitz a territory, then fly over it with paratroops to battle.
    final PlayerID germans = germans(m_data);
    final Territory germany = territory("Germany", m_data);
    final Territory poland = territory("Poland", m_data);
    final Territory eastPoland = territory("East Poland", m_data);
    final Territory beloRussia = territory("Belorussia", m_data);
    // Clear East Poland
    removeFrom(eastPoland, eastPoland.getUnits().getUnits());
    // Set up test
    final ITestDelegateBridge bridge = getDelegateBridge(germans);
    bridge.setStepName("CombatMove");
    moveDelegate(m_data).setDelegateBridgeAndPlayer(bridge);
    moveDelegate(m_data).start();
    TechAttachment.get(germans).setParatroopers("true");
    List<Unit> paratrooper = germany.getUnits().getMatches(Matches.UnitIsAirTransportable);
    paratrooper = paratrooper.subList(0, 1);
    final List<Unit> bomberAndParatroop = new ArrayList<>(paratrooper);
    bomberAndParatroop.addAll(germany.getUnits().getMatches(Matches.UnitIsAirTransport));
    final List<Unit> tanks = poland.getUnits().getMatches(Matches.UnitCanBlitz);
    move(tanks, new Route(poland, eastPoland, beloRussia));
    final List<Unit> airTransports = Match.getMatches(bomberAndParatroop, Matches.UnitIsAirTransport);
    for (final Unit airTransport : airTransports) {
      for (final Unit unit : paratrooper) {
        final Change change = TransportTracker.loadTransportChange((TripleAUnit) airTransport, unit);
        bridge.addChange(change);
      }
    }
    // Verify paratroops can overfly blitzed territory
    final String error =
        moveDelegate(m_data).move(bomberAndParatroop, new Route(germany, poland, eastPoland, beloRussia));
    assertValid(error);
  }

  @Test
  public void testAmphibAttackWithPlanesOnlyAskRetreatOnce() {
    final PlayerID germans = germans(m_data);
    final ITestDelegateBridge bridge = getDelegateBridge(germans);
    bridge.setStepName("CombatMove");
    moveDelegate(m_data).setDelegateBridgeAndPlayer(bridge);
    moveDelegate(m_data).start();
    final Territory france = territory("France", m_data);
    final Territory egypt = territory("Egypt", m_data);
    final Territory balkans = territory("Balkans", m_data);
    final Territory libya = territory("Libya", m_data);
    final Territory germany = territory("Germany", m_data);
    final Territory sz13 = territory("13 Sea Zone", m_data);
    final Territory sz14 = territory("14 Sea Zone", m_data);
    final Territory sz15 = territory("15 Sea Zone", m_data);
    bridge.setRemote(new DummyTripleAPlayer() {
      @Override
      public Territory retreatQuery(final GUID battleID, final boolean submerge, final Territory battleSite,
          final Collection<Territory> possibleTerritories, final String message) {
        assertFalse(message.contains(BattleStepStrings.RETREAT_PLANES));
        return null;
      }
    });
    bridge.setDisplay(new DummyTripleADisplay() {
      @Override
      public void listBattleSteps(final GUID battleID, final List<String> steps) {
        for (final String s : steps) {
          assertFalse(s.contains(BattleStepStrings.PLANES_WITHDRAW));
        }
      }
    });
    // move units for amphib assault
    load(france.getUnits().getMatches(Matches.UnitIsInfantry), new Route(france, sz13));
    move(sz13.getUnits().getUnits(), new Route(sz13, sz14, sz15));
    move(sz15.getUnits().getMatches(Matches.UnitIsInfantry), new Route(sz15, egypt));
    // ground attack
    load(libya.getUnits().getMatches(Matches.UnitIsArtillery), new Route(libya, egypt));
    // air units
    move(germany.getUnits().getMatches(Matches.UnitIsStrategicBomber), new Route(germany, balkans, sz14, sz15, egypt));
    moveDelegate(m_data).end();
    bridge.setStepName("Combat");
    // cook the dice so that all miss first round,all hit second round
    bridge.setRandomSource(new ScriptedRandomSource(5, 5, 5, 5, 5, 5, 5, 5, 5, 1, 1, 1, 1, 1, 1, 1, 1, 1));
    battleDelegate(m_data).setDelegateBridgeAndPlayer(bridge);
    battleDelegate(m_data).start();
    fight(battleDelegate(m_data), egypt);
  }

  @Test
  public void testDefencelessTransportsDie() {
    final PlayerID british = british(m_data);
    final ITestDelegateBridge bridge = getDelegateBridge(british);
    bridge.setStepName("CombatMove");
    moveDelegate(m_data).setDelegateBridgeAndPlayer(bridge);
    moveDelegate(m_data).start();
    final Territory uk = territory("United Kingdom", m_data);
    final Territory sz5 = territory("5 Sea Zone", m_data);
    // remove the sub
    removeFrom(sz5, sz5.getUnits().getMatches(Matches.UnitIsSub));
    bridge.setRemote(new DummyTripleAPlayer() {
      @Override
      public Territory retreatQuery(final GUID battleID, final boolean submerge, final Territory battleSite,
          final Collection<Territory> possibleTerritories, final String message) {
        // we should not be asked to retreat
        throw new IllegalStateException("Should not be asked to retreat:" + message);
      }
    });
    move(uk.getUnits().getMatches(Matches.UnitIsAir), m_data.getMap().getRoute(uk, sz5));
    // move units for amphib assault
    moveDelegate(m_data).end();
    bridge.setStepName("Combat");
    // cook the dice so that 1 british fighters hits, and nothing else
    // this will leave 1 transport alone in the sea zone
    bridge.setRandomSource(new ScriptedRandomSource(1, 5, 5, 5, 5, 5, 5, 5, 5));
    battleDelegate(m_data).setDelegateBridgeAndPlayer(bridge);
    battleDelegate(m_data).start();
    fight(battleDelegate(m_data), sz5);
    // make sure the transports died
    assertTrue(sz5.getUnits().getMatches(Matches.unitIsOwnedBy(germans(m_data))).isEmpty());
  }

  @Test
  public void testFighterLandsWhereCarrierCanBePlaced() {
    final PlayerID germans = germans(m_data);
    // germans have 1 carrier to place
    addTo(germans, carrier(m_data).create(1, germans), m_data);
    // start the move phase
    final ITestDelegateBridge bridge = getDelegateBridge(germans);
    bridge.setStepName("CombatMove");
    moveDelegate(m_data).setDelegateBridgeAndPlayer(bridge);
    moveDelegate(m_data).start();
    bridge.setRemote(new DummyTripleAPlayer() {
      @Override
      public boolean confirmMoveHariKari() {
        return false;
      }
    });
    // the fighter should be able to move and hover in the sea zone
    // the fighter has no movement left
    final Territory neEurope = territory("Northwestern Europe", m_data);
    final Route route = new Route(neEurope, territory("Germany", m_data), territory("Poland", m_data),
        territory("Baltic States", m_data), territory("5 Sea Zone", m_data));
    // the fighter should be able to move, and hover in the sea zone until the carrier is placed
    move(neEurope.getUnits().getMatches(Matches.UnitIsAir), route);
  }

  @Test
  public void testFighterCantHoverWithNoCarrierToPlace() {
    // start the move phase
    final ITestDelegateBridge bridge = getDelegateBridge(germans(m_data));
    bridge.setStepName("CombatMove");
    moveDelegate(m_data).setDelegateBridgeAndPlayer(bridge);
    moveDelegate(m_data).start();
    bridge.setRemote(new DummyTripleAPlayer() {
      @Override
      public boolean confirmMoveHariKari() {
        return false;
      }
    });
    // the fighter should not be able to move and hover in the sea zone
    // since their are no carriers to place
    // the fighter has no movement left
    final Territory neEurope = territory("Northwestern Europe", m_data);
    final Route route = new Route(neEurope, territory("Germany", m_data), territory("Poland", m_data),
        territory("Baltic States", m_data), territory("5 Sea Zone", m_data));
    final String error = moveDelegate(m_data).move(neEurope.getUnits().getMatches(Matches.UnitIsAir), route);
    assertNotNull(error);
  }

  @Test
  public void testRepair() {
    final Territory germany = territory("Germany", m_data);
    final Unit factory = germany.getUnits().getMatches(Matches.UnitCanBeDamaged).get(0);
    final PurchaseDelegate del = purchaseDelegate(m_data);
    del.setDelegateBridgeAndPlayer(getDelegateBridge(germans(m_data)));
    del.start();
    // Set up player
    final PlayerID germans = GameDataTestUtil.germans(m_data);
    final int initPUs = germans.getResources().getQuantity("PUs");
    // damage a factory
    IntegerMap<Unit> startHits = new IntegerMap<>();
    startHits.put(factory, 1);
    m_data.performChange(ChangeFactory.bombingUnitDamage(startHits));
    assertEquals(((TripleAUnit) factory).getUnitDamage(), 1);
    RepairRule repair = germans(m_data).getRepairFrontier().getRules().get(0);
    IntegerMap<RepairRule> repairs = new IntegerMap<>();
    repairs.put(repair, 1);
    String error = del.purchaseRepair(Collections.singletonMap(
        Match.getMatches(germany.getUnits().getUnits(), Matches.UnitCanBeDamaged).iterator().next(), repairs));
    assertValid(error);
    assertEquals(((TripleAUnit) factory).getUnitDamage(), 0);
    // Find cost
    final int midPUs = germans.getResources().getQuantity("PUs");
    assertEquals(initPUs, midPUs + 1);
    /*
     * INCREASED_FACTORY_PRODUCTION repairs
     */
    // Set up INCREASED_FACTORY_PRODUCTION
    final ITestDelegateBridge delegateBridge = getDelegateBridge(germans(m_data));
    TechTracker.addAdvance(germans, delegateBridge,
        TechAdvance.findAdvance(TechAdvance.TECH_PROPERTY_INCREASED_FACTORY_PRODUCTION, m_data, germans));
    // damage a factory
    startHits = new IntegerMap<>();
    startHits.put(factory, 2);
    m_data.performChange(ChangeFactory.bombingUnitDamage(startHits));
    assertEquals(((TripleAUnit) factory).getUnitDamage(), 2);
    repair = germans(m_data).getRepairFrontier().getRules().get(0);
    repairs = new IntegerMap<>();
    repairs.put(repair, 2);
    error = del.purchaseRepair(Collections.singletonMap(
        Match.getMatches(germany.getUnits().getUnits(), Matches.UnitCanBeDamaged).iterator().next(), repairs));
    assertValid(error);
    assertEquals(((TripleAUnit) factory).getUnitDamage(), 0);
    // Find cost
    final int finalPUs = germans.getResources().getQuantity("PUs");
    assertEquals(midPUs, finalPUs + 1);
  }

  @Test
  public void testRepairMoreThanDamaged() {
    final Territory germany = territory("Germany", m_data);
    final Unit factory = germany.getUnits().getMatches(Matches.UnitCanBeDamaged).get(0);
    final PurchaseDelegate del = purchaseDelegate(m_data);
    del.setDelegateBridgeAndPlayer(getDelegateBridge(germans(m_data)));
    del.start();
    // dame a factory
    final IntegerMap<Unit> startHits = new IntegerMap<>();
    startHits.put(factory, 1);
    m_data.performChange(ChangeFactory.bombingUnitDamage(startHits));
    assertEquals(((TripleAUnit) factory).getUnitDamage(), 1);
    final RepairRule repair = germans(m_data).getRepairFrontier().getRules().get(0);
    final IntegerMap<RepairRule> repairs = new IntegerMap<>();
    // we have 1 damaged marker, but trying to repair 2
    repairs.put(repair, 2);
    final String error = del.purchaseRepair(Collections.singletonMap(
        Match.getMatches(germany.getUnits().getUnits(), Matches.UnitCanBeDamaged).iterator().next(), repairs));
    // it is no longer an error, we just math max 0 it
    assertValid(error);
    assertEquals(((TripleAUnit) factory).getUnitDamage(), 0);
  }

  @Test
  public void testOccupiedTerrOfAttachment() {
    // Set up test
    final PlayerID british = GameDataTestUtil.british(m_data);
    final ITestDelegateBridge delegateBridge = getDelegateBridge(british(m_data));
    // Set up the move delegate
    final MoveDelegate moveDelegate = moveDelegate(m_data);
    delegateBridge.setStepName("CombatMove");
    moveDelegate(m_data).setDelegateBridgeAndPlayer(delegateBridge);
    moveDelegate(m_data).start();
    // Set up the territories
    final Territory hupeh = territory("Hupeh", m_data);
    final Territory kiangsu = territory("Kiangsu", m_data);
    // Remove all units
    removeFrom(kiangsu, kiangsu.getUnits().getUnits());
    removeFrom(hupeh, hupeh.getUnits().getUnits());
    // Set up the unit types
    addTo(hupeh, infantry(m_data).create(1, british));
    // Get units
    final Collection<Unit> moveUnits = hupeh.getUnits().getUnits();
    // Get Owner prior to battle
    final String preOwner = kiangsu.getOwner().getName();
    assertEquals(preOwner, Constants.PLAYER_NAME_JAPANESE);
    // add a VALID attack
    final String validResults = moveDelegate.move(moveUnits, new Route(hupeh, kiangsu));
    assertValid(validResults);
    // Ensure owner after attack doesn't match attacker
    final String postOwner = kiangsu.getOwner().getName();
    assertNotSame(postOwner, Constants.PLAYER_NAME_BRITISH);
    // Check that original owner is now owner
    assertEquals(postOwner, Constants.PLAYER_NAME_CHINESE);
  }

  @Test
  public void testOccupiedTerrOfAttachmentWithCapital() {
    // Set up test
    final PlayerID british = GameDataTestUtil.british(m_data);
    final ITestDelegateBridge delegateBridge = getDelegateBridge(british(m_data));
    // Set up the move delegate
    final MoveDelegate moveDelegate = moveDelegate(m_data);
    delegateBridge.setStepName("CombatMove");
    moveDelegate(m_data).setDelegateBridgeAndPlayer(delegateBridge);
    moveDelegate(m_data).start();
    // Set up the territories
    final Territory hupeh = territory("Hupeh", m_data);
    final Territory kiangsu = territory("Kiangsu", m_data);
    final Territory mongolia = territory("Mongolia", m_data);
    // Remove original capital
    final TerritoryAttachment taMongolia = TerritoryAttachment.get(mongolia);
    final TerritoryAttachment taKiangsu = TerritoryAttachment.get(kiangsu);
    try {
      final String noVal = null;
      taMongolia.setCapital(noVal);
      // Set as NEW capital
      taKiangsu.setCapital(Constants.PLAYER_NAME_CHINESE);
    } catch (final GameParseException e) {
      ClientLogger.logError(e);
    }
    // Remove all units
    removeFrom(kiangsu, kiangsu.getUnits().getUnits());
    removeFrom(hupeh, hupeh.getUnits().getUnits());
    // Set up the unit types
    addTo(hupeh, infantry(m_data).create(1, british));
    // Get units
    final Collection<Unit> moveUnits = hupeh.getUnits().getUnits();
    // Get Owner prior to battle
    final String preOwner = kiangsu.getOwner().getName();
    assertEquals(preOwner, Constants.PLAYER_NAME_JAPANESE);
    // add a VALID attack
    final String validResults = moveDelegate.move(moveUnits, new Route(hupeh, kiangsu));
    assertValid(validResults);
    // Ensure owner after attack doesn't match attacker
    final String postOwner = kiangsu.getOwner().getName();
    assertNotSame(postOwner, Constants.PLAYER_NAME_BRITISH);
    // Check that original owner is now owner
    assertEquals(postOwner, Constants.PLAYER_NAME_CHINESE);
  }

  @Test
  public void testTwoStepBlitz() {
    final ITestDelegateBridge delegateBridge = getDelegateBridge(british(m_data));
    // Set up the territories
    final Territory libya = territory("Libya", m_data);
    final Territory egypt = territory("Egypt", m_data);
    final Territory morrocco = territory("Morocco Algeria", m_data);
    removeFrom(libya, libya.getUnits().getUnits());
    // Set up the move delegate
    final MoveDelegate moveDelegate = moveDelegate(m_data);
    delegateBridge.setStepName("CombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(delegateBridge);
    moveDelegate.start();
    // blitz in two steps
    final Collection<Unit> armour = egypt.getUnits().getMatches(Matches.UnitCanBlitz);
    move(armour, new Route(egypt, libya));
    assertEquals(libya.getOwner(), british(m_data));
    move(armour, new Route(libya, morrocco));
  }

  /*
   * Add Utilities here
   */
  private Collection<Unit> getUnits(final IntegerMap<UnitType> units, final PlayerID from) {
    final Iterator<UnitType> iter = units.keySet().iterator();
    final Collection<Unit> rVal = new ArrayList<>(units.totalValues());
    while (iter.hasNext()) {
      final UnitType type = iter.next();
      rVal.addAll(from.getUnits().getUnits(type, units.getInt(type)));
    }
    return rVal;
  }

  /*
   * Add assertions here
   */
  public void assertValid(final String string) {
    assertNull(string, string);
  }

  public void assertError(final String string) {
    assertNotNull(string, string);
  }
}
