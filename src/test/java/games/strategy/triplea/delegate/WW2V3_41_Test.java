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
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.contains;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.data.Change;
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
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.engine.random.ScriptedRandomSource;
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
import games.strategy.triplea.delegate.remote.IAbstractPlaceDelegate;
import games.strategy.triplea.player.ITripleAPlayer;
import games.strategy.triplea.ui.display.ITripleADisplay;
import games.strategy.triplea.xml.TestMapGameData;
import games.strategy.util.IntegerMap;
import games.strategy.util.Match;

public class WW2V3_41_Test {
  private GameData gameData;
  private final ITripleAPlayer dummyPlayer = mock(ITripleAPlayer.class);

  @Before
  public void setUp() throws Exception {
    when(dummyPlayer.selectCasualties(any(), any(), anyInt(), any(), any(), any(), any(), any(), any(),
        anyBoolean(), any(), any(), any(), any(), anyBoolean())).thenAnswer(new Answer<CasualtyDetails>() {
          @Override
          public CasualtyDetails answer(final InvocationOnMock invocation) throws Throwable {
            final CasualtyList defaultCasualties = invocation.getArgument(11);
            if (defaultCasualties != null) {
              return new CasualtyDetails(defaultCasualties.getKilled(), defaultCasualties.getDamaged(), true);
            }
            return null;
          }
        });
    gameData = TestMapGameData.WW2V3_1941.getGameData();
  }

  private ITestDelegateBridge getDelegateBridge(final PlayerID player) {
    return GameDataTestUtil.getDelegateBridge(player, gameData);
  }

  public static String fight(final BattleDelegate battle, final Territory territory) {
    for (final Entry<BattleType, Collection<Territory>> entry : battle.getBattles().getBattles().entrySet()) {
      if (!entry.getKey().isBombingRun() && entry.getValue().contains(territory)) {
        return battle.fightBattle(territory, false, entry.getKey());
      }
    }
    throw new IllegalStateException("Could not find battle in: " + territory.getName());
  }

  @Test
  public void testAACasualtiesLowLuckMixedRadar() {
    // moved from BattleCalculatorTest because "revised" does not have "radar"
    final PlayerID british = GameDataTestUtil.british(gameData);
    final ITestDelegateBridge m_bridge = getDelegateBridge(british);
    makeGameLowLuck(gameData);
    // setSelectAACasualties(data, false);
    givePlayerRadar(germans(gameData));
    // 3 bombers and 3 fighters
    final Collection<Unit> planes = bomber(gameData).create(3, british(gameData));
    planes.addAll(fighter(gameData).create(3, british(gameData)));
    final Collection<Unit> defendingAA =
        territory("Germany", gameData).getUnits().getMatches(Matches.UnitIsAAforAnything);
    // don't allow rolling, 6 of each is deterministic
    m_bridge.setRandomSource(new ScriptedRandomSource(new int[] {ScriptedRandomSource.ERROR}));
    final DiceRoll roll =
        DiceRoll
            .rollAA(
                Match.getMatches(planes,
                    Matches.unitIsOfTypes(
                        UnitAttachment.get(defendingAA.iterator().next().getType()).getTargetsAA(gameData))),
                defendingAA, m_bridge, territory("Germany", gameData), true);
    final Collection<Unit> casualties = BattleCalculator.getAACasualties(false, planes, planes, defendingAA,
        defendingAA, roll, m_bridge, null, null, null, territory("Germany", gameData), null, false, null).getKilled();
    assertEquals(casualties.size(), 2);
    // should be 1 fighter and 1 bomber
    assertEquals(Match.countMatches(casualties, Matches.UnitIsStrategicBomber), 1);
    assertEquals(Match.countMatches(casualties, Matches.UnitIsStrategicBomber.invert()), 1);
  }

  @Test
  public void testAACasualtiesLowLuckMixedWithRollingRadar() {
    // moved from BattleCalculatorTest because "revised" does not have "radar"
    final PlayerID british = GameDataTestUtil.british(gameData);
    final ITestDelegateBridge m_bridge = getDelegateBridge(british);
    makeGameLowLuck(gameData);
    // setSelectAACasualties(data, false);
    givePlayerRadar(germans(gameData));
    // 4 bombers and 4 fighters
    final Collection<Unit> planes = bomber(gameData).create(4, british(gameData));
    planes.addAll(fighter(gameData).create(4, british(gameData)));
    final Collection<Unit> defendingAA =
        territory("Germany", gameData).getUnits().getMatches(Matches.UnitIsAAforAnything);
    // 1 roll, a hit
    // then a dice to select the casualty
    final ScriptedRandomSource randomSource = new ScriptedRandomSource(new int[] {0, 1});
    m_bridge.setRandomSource(randomSource);
    final DiceRoll roll =
        DiceRoll
            .rollAA(
                Match.getMatches(planes,
                    Matches.unitIsOfTypes(
                        UnitAttachment.get(defendingAA.iterator().next().getType()).getTargetsAA(gameData))),
                defendingAA, m_bridge, territory("Germany", gameData), true);
    // make sure we rolled once
    assertEquals(1, randomSource.getTotalRolled());
    final Collection<Unit> casualties = BattleCalculator.getAACasualties(false, planes, planes, defendingAA,
        defendingAA, roll, m_bridge, null, null, null, territory("Germany", gameData), null, false, null).getKilled();
    assertEquals(casualties.size(), 3);
    // should be 1 fighter and 2 bombers
    assertEquals(Match.countMatches(casualties, Matches.UnitIsStrategicBomber), 2);
    assertEquals(Match.countMatches(casualties, Matches.UnitIsStrategicBomber.invert()), 1);
  }

  @Test
  public void testAACasualtiesLowLuckMixedWithRollingMissRadar() {
    // moved from BattleCalculatorTest because "revised" does not have "radar"
    final PlayerID british = GameDataTestUtil.british(gameData);
    final ITestDelegateBridge m_bridge = getDelegateBridge(british);
    makeGameLowLuck(gameData);
    // setSelectAACasualties(data, false);
    givePlayerRadar(germans(gameData));
    // 4 bombers and 4 fighters
    final Collection<Unit> planes = bomber(gameData).create(4, british(gameData));
    planes.addAll(fighter(gameData).create(4, british(gameData)));
    final Collection<Unit> defendingAA =
        territory("Germany", gameData).getUnits().getMatches(Matches.UnitIsAAforAnything);
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
                        UnitAttachment.get(defendingAA.iterator().next().getType()).getTargetsAA(gameData))),
                defendingAA, m_bridge, territory("Germany", gameData), true);
    assertEquals(roll.getHits(), 2);
    // make sure we rolled once
    assertEquals(1, randomSource.getTotalRolled());
    final Collection<Unit> casualties = BattleCalculator.getAACasualties(false, planes, planes, defendingAA,
        defendingAA, roll, m_bridge, null, null, null, territory("Germany", gameData), null, false, null).getKilled();
    assertEquals(casualties.size(), 2);
    assertEquals(4, randomSource.getTotalRolled());
    // should be 1 fighter and 2 bombers
    assertEquals(Match.countMatches(casualties, Matches.UnitIsStrategicBomber), 1);
    assertEquals(Match.countMatches(casualties, Matches.UnitIsStrategicBomber.invert()), 1);
  }

  @Test
  public void testDefendingTrasnportsAutoKilled() {
    final Territory sz13 = gameData.getMap().getTerritory("13 Sea Zone");
    final Territory sz12 = gameData.getMap().getTerritory("12 Sea Zone");
    final PlayerID british = GameDataTestUtil.british(gameData);
    final MoveDelegate moveDelegate = moveDelegate(gameData);
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
    final BattleDelegate bd = battleDelegate(gameData);
    assertFalse(bd.getBattleTracker().getPendingBattleSites(false).isEmpty());
  }

  @Test
  public void testUnplacedDie() {
    final PlaceDelegate del = placeDelegate(gameData);
    del.setDelegateBridgeAndPlayer(getDelegateBridge(british(gameData)));
    del.start();
    addTo(british(gameData), transport(gameData).create(1, british(gameData)), gameData);
    del.end();
    // unplaced units die
    assertEquals(1, british(gameData).getUnits().size());
  }

  @Test
  public void testPlaceEmpty() {
    final PlaceDelegate del = placeDelegate(gameData);
    del.setDelegateBridgeAndPlayer(getDelegateBridge(british(gameData)));
    del.start();
    addTo(british(gameData), transport(gameData).create(1, british(gameData)), gameData);
    final String error = del.placeUnits(Collections.<Unit>emptyList(), territory("United Kingdom", gameData),
      IAbstractPlaceDelegate.BidMode.NOT_BID);
    assertNull(error);
  }

  @Test
  public void testTechTokens() {
    // Set up the test
    final PlayerID germans = GameDataTestUtil.germans(gameData);
    final ITestDelegateBridge delegateBridge = getDelegateBridge(germans);
    delegateBridge.setStepName("germanTech");
    final TechnologyDelegate techDelegate = techDelegate(gameData);
    techDelegate.setDelegateBridgeAndPlayer(delegateBridge);
    techDelegate.start();
    final TechnologyFrontier mech = new TechnologyFrontier("", gameData);
    mech.addAdvance(TechAdvance.findAdvance(TechAdvance.TECH_PROPERTY_MECHANIZED_INFANTRY, gameData, null));
    // Add tech token
    gameData.performChange(
        ChangeFactory.changeResourcesChange(germans, gameData.getResourceList().getResource(Constants.TECH_TOKENS), 1));
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
    final Territory gibraltar = territory("Gibraltar", gameData);
    // add a tank to gibralter
    final PlayerID british = british(gameData);
    addTo(gibraltar, infantry(gameData).create(1, british));
    final MoveDelegate moveDelegate = moveDelegate(gameData);
    final ITestDelegateBridge bridge = getDelegateBridge(british);
    bridge.setStepName("britishCombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(bridge);
    moveDelegate.start();
    bridge.setRemote(dummyPlayer);
    final Territory sz9 = territory("9 Sea Zone", gameData);
    final Territory sz13 = territory("13 Sea Zone", gameData);
    final Route sz9ToSz13 = new Route(sz9, territory("12 Sea Zone", gameData), sz13);
    // move the transport to attack, this is suicide but valid
    move(sz9.getUnits().getMatches(Matches.UnitIsTransport), sz9ToSz13);
    // load the transport
    load(gibraltar.getUnits().getUnits(), new Route(gibraltar, sz13));
    moveDelegate.end();
    bridge.setStepName("britishBattle");
    final BattleDelegate battleDelegate = battleDelegate(gameData);
    battleDelegate.setDelegateBridgeAndPlayer(bridge);
    battleDelegate.start();
    assertTrue(battleDelegate.getBattles().isEmpty());
  }

  @Test
  public void testLoadedTransportAttackKillsLoadedUnits() {
    final PlayerID british = british(gameData);
    final MoveDelegate moveDelegate = moveDelegate(gameData);
    final ITestDelegateBridge bridge = getDelegateBridge(british);
    bridge.setStepName("britishCombatMove");
    when(dummyPlayer.selectAttackSubs(any())).thenReturn(true);
    bridge.setRemote(dummyPlayer);
    moveDelegate.setDelegateBridgeAndPlayer(bridge);
    moveDelegate.start();
    final Territory sz9 = territory("9 Sea Zone", gameData);
    final Territory sz7 = territory("7 Sea Zone", gameData);
    final Territory uk = territory("United Kingdom", gameData);
    final Route sz9ToSz7 = new Route(sz9, territory("8 Sea Zone", gameData), sz7);
    // move the transport to attack, this is suicide but valid
    final List<Unit> transports = sz9.getUnits().getMatches(Matches.UnitIsTransport);
    move(transports, sz9ToSz7);
    // load the transport
    load(uk.getUnits().getMatches(Matches.UnitIsInfantry), new Route(uk, sz7));
    moveDelegate(gameData).end();
    final ScriptedRandomSource randomSource = new ScriptedRandomSource(new int[] {0, 1});
    bridge.setRandomSource(randomSource);
    bridge.setStepName("britishBattle");
    final BattleDelegate battleDelegate = battleDelegate(gameData);
    battleDelegate.setDelegateBridgeAndPlayer(bridge);
    assertEquals(2, TransportTracker.transporting(transports.get(0)).size());
    battleDelegate.start();
    // battle already fought
    // make sure the infantry die with the transport
    assertTrue(sz7.getUnits().toString(), sz7.getUnits().getMatches(Matches.unitOwnedBy(british)).isEmpty());
  }

  @Test
  public void testCanRetreatIntoEmptyEnemyTerritory() {
    final Territory eastPoland = territory("East Poland", gameData);
    final Territory ukraine = territory("Ukraine", gameData);
    final Territory poland = territory("Poland", gameData);
    // remove all units from east poland
    removeFrom(eastPoland, eastPoland.getUnits().getUnits());
    final MoveDelegate moveDelegate = moveDelegate(gameData);
    final ITestDelegateBridge delegateBridge = getDelegateBridge(germans(gameData));
    delegateBridge.setStepName("CombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(delegateBridge);
    moveDelegate.start();
    final Territory bulgaria = territory("Bulgaria Romania", gameData);
    // attack from bulgraia
    move(bulgaria.getUnits().getUnits(), new Route(bulgaria, ukraine));
    // add an air attack from east poland
    move(poland.getUnits().getMatches(Matches.UnitIsAir), new Route(poland, eastPoland, ukraine));
    // we should not be able to retreat to east poland!
    // that territory is still owned by the enemy
    final MustFightBattle battle =
        (MustFightBattle) AbstractMoveDelegate.getBattleTracker(gameData).getPendingBattle(ukraine, false, null);
    assertFalse(battle.getAttackerRetreatTerritories().contains(eastPoland));
  }

  @Test
  public void testCanRetreatIntoBlitzedTerritory() {
    final Territory eastPoland = territory("East Poland", gameData);
    final Territory ukraine = territory("Ukraine", gameData);
    final Territory poland = territory("Poland", gameData);
    // remove all units from east poland
    removeFrom(eastPoland, eastPoland.getUnits().getUnits());
    final MoveDelegate moveDelegate = moveDelegate(gameData);
    final ITestDelegateBridge delegateBridge = getDelegateBridge(germans(gameData));
    delegateBridge.setStepName("CombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(delegateBridge);
    moveDelegate.start();
    final Territory bulgaria = territory("Bulgaria Romania", gameData);
    // attack from bulgraia
    move(bulgaria.getUnits().getUnits(), new Route(bulgaria, ukraine));
    // add a blitz attack
    move(poland.getUnits().getMatches(Matches.UnitCanBlitz), new Route(poland, eastPoland, ukraine));
    // we should not be able to retreat to east poland!
    // that territory was just conquered
    final MustFightBattle battle =
        (MustFightBattle) AbstractMoveDelegate.getBattleTracker(gameData).getPendingBattle(ukraine, false, null);
    assertTrue(battle.getAttackerRetreatTerritories().contains(eastPoland));
  }

  @Test
  public void testCantBlitzFactoryOrAA() {
    // Set up territories
    final Territory poland = territory("Poland", gameData);
    final Territory eastPoland = territory("East Poland", gameData);
    final Territory ukraine = territory("Ukraine", gameData);
    // remove all units from east poland
    removeFrom(eastPoland, eastPoland.getUnits().getUnits());
    // Add a russian factory
    addTo(eastPoland, factory(gameData).create(1, russians(gameData)));
    MoveDelegate moveDelegate = moveDelegate(gameData);
    ITestDelegateBridge delegateBridge = getDelegateBridge(germans(gameData));
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
    addTo(eastPoland, aaGun(gameData).create(1, russians(gameData)));
    moveDelegate = moveDelegate(gameData);
    delegateBridge = getDelegateBridge(germans(gameData));
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
    final Territory poland = territory("Poland", gameData);
    // Add a russian factory
    final Territory germany = territory("Germany", gameData);
    addTo(poland, aaGun(gameData).create(1, germans(gameData)));
    MoveDelegate moveDelegate = moveDelegate(gameData);
    ITestDelegateBridge delegateBridge = getDelegateBridge(germans(gameData));
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
    final Territory finland = territory("Finland", gameData);
    final Territory sz5 = territory("5 Sea Zone", gameData);
    addTo(finland, aaGun(gameData).create(1, germans(gameData)));
    moveDelegate = moveDelegate(gameData);
    delegateBridge = getDelegateBridge(germans(gameData));
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
    final UnitType aaGun = GameDataTestUtil.aaGun(gameData);
    final IntegerMap<UnitType> map = new IntegerMap<>();
    map.add(aaGun, 1);
    // Set up the test
    final PlayerID germans = GameDataTestUtil.germans(gameData);
    final PlaceDelegate placeDelegate = placeDelegate(gameData);
    delegateBridge.setStepName("Place");
    delegateBridge.setPlayerID(germans);
    placeDelegate.setDelegateBridgeAndPlayer(getDelegateBridge(germans(gameData)));
    placeDelegate.start();
    addTo(germans(gameData), aaGun(gameData).create(1, germans(gameData)), gameData);
    errorResults = placeDelegate.placeUnits(getUnits(map, germans), germany, IAbstractPlaceDelegate.BidMode.NOT_BID);
    assertValid(errorResults);
    assertEquals(germany.getUnits().getUnitCount(), preCount + 3);
  }

  @Test
  public void testMechanizedInfantry() {
    // Set up tech
    final PlayerID germans = GameDataTestUtil.germans(gameData);
    final ITestDelegateBridge delegateBridge = getDelegateBridge(germans(gameData));
    TechTracker.addAdvance(germans, delegateBridge,
        TechAdvance.findAdvance(TechAdvance.TECH_PROPERTY_MECHANIZED_INFANTRY, gameData, germans));
    // Set up the move delegate
    final MoveDelegate moveDelegate = moveDelegate(gameData);
    delegateBridge.setStepName("CombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(delegateBridge);
    moveDelegate.start();
    // Set up the territories
    final Territory poland = territory("Poland", gameData);
    final Territory eastPoland = territory("East Poland", gameData);
    final Territory belorussia = territory("Belorussia", gameData);
    // Set up the unit types
    final UnitType infantryType = GameDataTestUtil.infantry(gameData);
    // Remove all units from east poland
    removeFrom(eastPoland, eastPoland.getUnits().getUnits());
    // Get total number of units in territories to start
    final int preCountIntPoland = poland.getUnits().size();
    final int preCountIntBelorussia = belorussia.getUnits().size();
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
    final int postCountIntPoland = poland.getUnits().size() + 4;
    final int postCountIntBelorussia = belorussia.getUnits().size() - 4;
    // Compare the number of units before and after
    assertEquals(preCountIntPoland, postCountIntPoland);
    assertEquals(preCountIntBelorussia, postCountIntBelorussia);
  }

  @Test
  public void testJetPower() {
    // Set up tech
    final PlayerID germans = GameDataTestUtil.germans(gameData);
    final ITestDelegateBridge delegateBridge = getDelegateBridge(germans(gameData));
    TechTracker.addAdvance(germans, delegateBridge,
        TechAdvance.findAdvance(TechAdvance.TECH_PROPERTY_JET_POWER, gameData, germans));
    // Set up the territories
    final Territory poland = territory("Poland", gameData);
    final Territory eastPoland = territory("East Poland", gameData);
    // Set up the unit types
    final UnitType fighterType = GameDataTestUtil.fighter(gameData);
    delegateBridge.setStepName("germanBattle");
    while (!gameData.getSequence().getStep().getName().equals("germanBattle")) {
      gameData.getSequence().next();
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
    final ITestDelegateBridge bridge = getDelegateBridge(british(gameData));
    bridge.setStepName("BidPlace");
    bidPlaceDelegate(gameData).setDelegateBridgeAndPlayer(bridge);
    bidPlaceDelegate(gameData).start();
    // create 20 british infantry
    addTo(british(gameData), infantry(gameData).create(20, british(gameData)), gameData);
    final Territory uk = territory("United Kingdom", gameData);
    final Collection<Unit> units = british(gameData).getUnits().getUnits();
    final PlaceableUnits placeable = bidPlaceDelegate(gameData).getPlaceableUnits(units, uk);
    assertEquals(20, placeable.getMaxUnits());
    assertNull(placeable.getErrorMessage());
    final String error = bidPlaceDelegate(gameData).placeUnits(units, uk);
    assertNull(error);
  }

  @Test
  public void testFactoryPlace() {
    // Set up game
    final PlayerID british = GameDataTestUtil.british(gameData);
    final ITestDelegateBridge delegateBridge = getDelegateBridge(british(gameData));
    // Set up the territories
    final Territory egypt = territory("Union of South Africa", gameData);
    // Set up the unit types
    final UnitType factoryType = GameDataTestUtil.factory(gameData);
    // Set up the move delegate
    final PlaceDelegate placeDelegate = placeDelegate(gameData);
    delegateBridge.setStepName("Place");
    delegateBridge.setPlayerID(british);
    placeDelegate.setDelegateBridgeAndPlayer(delegateBridge);
    placeDelegate.start();
    // Add the factory
    final IntegerMap<UnitType> map = new IntegerMap<>();
    map.add(factoryType, 1);
    addTo(british(gameData), factory(gameData).create(1, british(gameData)), gameData);
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
    final PlayerID chinese = GameDataTestUtil.chinese(gameData);
    final ITestDelegateBridge delegateBridge = getDelegateBridge(chinese(gameData));
    delegateBridge.setPlayerID(chinese);
    delegateBridge.setStepName("CombatMove");
    final MoveDelegate moveDelegate = moveDelegate(gameData);
    moveDelegate.setDelegateBridgeAndPlayer(delegateBridge);
    moveDelegate.start();
    // Set up the territories
    final Territory yunnan = territory("Yunnan", gameData);
    final Territory kiangsu = territory("Kiangsu", gameData);
    final Territory hupeh = territory("Hupeh", gameData);
    // Set up the unit types
    final UnitType infantryType = GameDataTestUtil.infantry(gameData);
    // Remove all units
    removeFrom(kiangsu, kiangsu.getUnits().getUnits());
    // add a VALID attack
    final Collection<Unit> moveUnits = hupeh.getUnits().getUnits();
    final String validResults = moveDelegate.move(moveUnits, new Route(hupeh, kiangsu));
    assertValid(validResults);
    /*
     * Place units in just captured territory
     */
    final PlaceDelegate placeDelegate = placeDelegate(gameData);
    delegateBridge.setStepName("Place");
    placeDelegate.setDelegateBridgeAndPlayer(delegateBridge);
    placeDelegate.start();
    // Add the infantry
    IntegerMap<UnitType> map = new IntegerMap<>();
    map.add(infantryType, 3);
    addTo(chinese(gameData), infantry(gameData).create(1, chinese(gameData)), gameData);
    // Get the number of units before placing
    int preCount = kiangsu.getUnits().getUnitCount();
    // Place the infantry
    String response = placeDelegate.placeUnits(getUnits(map, chinese), kiangsu, IAbstractPlaceDelegate.BidMode.NOT_BID);
    assertValid(response);
    assertEquals(preCount + 1, kiangsu.getUnits().getUnitCount());
    /*
     * Place units in a territory with up to 3 Chinese units
     */
    // Add the infantry
    map = new IntegerMap<>();
    map.add(infantryType, 3);
    addTo(chinese(gameData), infantry(gameData).create(3, chinese(gameData)), gameData);
    // Get the number of units before placing
    preCount = yunnan.getUnits().getUnitCount();
    // Place the infantry
    response = placeDelegate.placeUnits(getUnits(map, chinese), yunnan, IAbstractPlaceDelegate.BidMode.NOT_BID);
    assertValid(response);
    final int midCount = yunnan.getUnits().getUnitCount();
    // Make sure they were all placed
    assertEquals(preCount, midCount - 3);
    /*
     * Place units in a territory with 3 or more Chinese units
     */
    map = new IntegerMap<>();
    map.add(infantryType, 1);
    addTo(chinese(gameData), infantry(gameData).create(1, chinese(gameData)), gameData);
    response = placeDelegate.placeUnits(getUnits(map, chinese), yunnan, IAbstractPlaceDelegate.BidMode.NOT_BID);
    assertError(response);
    // Make sure none were placed
    final int postCount = yunnan.getUnits().getUnitCount();
    assertEquals(midCount, postCount);
  }

  @Test
  public void testPlaceInOccupiedSZ() {
    // Set up game
    final PlayerID germans = GameDataTestUtil.germans(gameData);
    final ITestDelegateBridge delegateBridge = getDelegateBridge(british(gameData));
    // Clear all units from the SZ and add an enemy unit
    final Territory sz5 = territory("5 Sea Zone", gameData);
    removeFrom(sz5, sz5.getUnits().getUnits());
    addTo(sz5, destroyer(gameData).create(1, british(gameData)));
    // Set up the unit types
    final UnitType transportType = GameDataTestUtil.transport(gameData);
    // Set up the move delegate
    final PlaceDelegate placeDelegate = placeDelegate(gameData);
    delegateBridge.setStepName("Place");
    delegateBridge.setPlayerID(germans);
    placeDelegate.setDelegateBridgeAndPlayer(delegateBridge);
    placeDelegate.start();
    // Add the transport
    final IntegerMap<UnitType> map = new IntegerMap<>();
    map.add(transportType, 1);
    addTo(germans(gameData), transport(gameData).create(1, germans(gameData)), gameData);
    // Place it
    final String response = placeDelegate.placeUnits(getUnits(map, germans), sz5);
    assertValid(response);
  }

  @Test
  public void testMoveUnitsThroughSubs() {
    final ITestDelegateBridge bridge = getDelegateBridge(british(gameData));
    bridge.setStepName("britishNonCombatMove");
    moveDelegate(gameData).setDelegateBridgeAndPlayer(bridge);
    moveDelegate(gameData).start();
    final Territory sz6 = territory("6 Sea Zone", gameData);
    final Route route = new Route(sz6, territory("7 Sea Zone", gameData), territory("8 Sea Zone", gameData));
    final String error = moveDelegate(gameData).move(sz6.getUnits().getUnits(), route);
    assertNull(error, error);
  }

  @Test
  public void testMoveUnitsThroughTransports() {
    final ITestDelegateBridge bridge = getDelegateBridge(british(gameData));
    bridge.setStepName("britishCombatMove");
    moveDelegate(gameData).setDelegateBridgeAndPlayer(bridge);
    moveDelegate(gameData).start();
    final Territory sz12 = territory("12 Sea Zone", gameData);
    final Route route = new Route(sz12, territory("13 Sea Zone", gameData), territory("14 Sea Zone", gameData));
    final String error = moveDelegate(gameData).move(sz12.getUnits().getUnits(), route);
    assertNull(error, error);
  }

  @Test
  public void testMoveUnitsThroughTransports2() {
    final ITestDelegateBridge bridge = getDelegateBridge(british(gameData));
    bridge.setStepName("britishNonCombatMove");
    moveDelegate(gameData).setDelegateBridgeAndPlayer(bridge);
    moveDelegate(gameData).start();
    final Territory sz12 = territory("12 Sea Zone", gameData);
    final Territory sz14 = territory("14 Sea Zone", gameData);
    removeFrom(sz14, sz14.getUnits().getUnits());
    final Route route = new Route(sz12, territory("13 Sea Zone", gameData), sz14);
    final String error = moveDelegate(gameData).move(sz12.getUnits().getUnits(), route);
    assertNull(error, error);
  }

  @Test
  public void testLoadThroughSubs() {
    final ITestDelegateBridge bridge = getDelegateBridge(british(gameData));
    bridge.setStepName("britishNonCombatMove");
    final MoveDelegate moveDelegate = moveDelegate(gameData);
    moveDelegate.setDelegateBridgeAndPlayer(bridge);
    moveDelegate.start();
    final Territory sz8 = territory("8 Sea Zone", gameData);
    final Territory sz7 = territory("7 Sea Zone", gameData);
    final Territory sz6 = territory("6 Sea Zone", gameData);
    final Territory uk = territory("United Kingdom", gameData);
    // add a transport
    addTo(sz8, transport(gameData).create(1, british(gameData)));
    // move the transport where to the sub is
    assertValid(moveDelegate.move(sz8.getUnits().getUnits(), new Route(sz8, sz7)));
    // load the transport
    load(uk.getUnits().getMatches(Matches.UnitIsInfantry), new Route(uk, sz7));
    // move the transport out
    assertValid(
        moveDelegate.move(sz7.getUnits().getMatches(Matches.unitOwnedBy(british(gameData))), new Route(sz7, sz6)));
  }

  @Test
  public void testAttackUndoAndAttackAgain() {
    final MoveDelegate move = moveDelegate(gameData);
    final ITestDelegateBridge bridge = getDelegateBridge(italians(gameData));
    bridge.setStepName("CombatMove");
    move.setDelegateBridgeAndPlayer(bridge);
    move.start();
    final Territory sz14 = territory("14 Sea Zone", gameData);
    final Territory sz13 = territory("13 Sea Zone", gameData);
    final Territory sz12 = territory("12 Sea Zone", gameData);
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
        (MustFightBattle) AbstractMoveDelegate.getBattleTracker(gameData).getPendingBattle(sz12, false, null);
    // only 3 attacking units
    // the battleship and the two cruisers
    assertEquals(3, mfb.getAttackingUnits().size());
  }

  @Test
  public void testAttackSubsOnSubs() {
    final String defender = "Germans";
    final String attacker = "British";
    final Territory attacked = territory("31 Sea Zone", gameData);
    final Territory from = territory("32 Sea Zone", gameData);
    // 1 sub attacks 1 sub
    addTo(from, submarine(gameData).create(1, british(gameData)));
    addTo(attacked, submarine(gameData).create(1, germans(gameData)));
    final ITestDelegateBridge bridge = getDelegateBridge(british(gameData));
    bridge.setStepName("CombatMove");
    moveDelegate(gameData).setDelegateBridgeAndPlayer(bridge);
    moveDelegate(gameData).start();
    move(from.getUnits().getUnits(), new Route(from, attacked));
    moveDelegate(gameData).end();
    final MustFightBattle battle =
        (MustFightBattle) AbstractMoveDelegate.getBattleTracker(gameData).getPendingBattle(attacked, false, null);
    final List<String> steps = battle.determineStepStrings(true, bridge);
    assertEquals(
        Arrays.asList(attacker + SUBS_SUBMERGE, defender + SUBS_SUBMERGE, attacker + SUBS_FIRE,
            defender + SELECT_SUB_CASUALTIES, defender + SUBS_FIRE, attacker + SELECT_SUB_CASUALTIES,
            REMOVE_SNEAK_ATTACK_CASUALTIES, REMOVE_CASUALTIES, attacker + ATTACKER_WITHDRAW).toString(),
        steps.toString());
    bridge.setRemote(dummyPlayer);
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
    final Territory attacked = territory("31 Sea Zone", gameData);
    final Territory from = territory("32 Sea Zone", gameData);
    // 1 sub attacks 1 sub and 1 destroyer
    // defender sneak attacks, not attacker
    addTo(from, submarine(gameData).create(1, british(gameData)));
    addTo(attacked, submarine(gameData).create(1, germans(gameData)));
    addTo(attacked, destroyer(gameData).create(1, germans(gameData)));
    final ITestDelegateBridge bridge = getDelegateBridge(british(gameData));
    bridge.setStepName("CombatMove");
    moveDelegate(gameData).setDelegateBridgeAndPlayer(bridge);
    moveDelegate(gameData).start();
    move(from.getUnits().getUnits(), new Route(from, attacked));
    moveDelegate(gameData).end();
    final MustFightBattle battle =
        (MustFightBattle) AbstractMoveDelegate.getBattleTracker(gameData).getPendingBattle(attacked, false, null);
    final List<String> steps = battle.determineStepStrings(true, bridge);
    assertEquals(
        Arrays.asList(defender + SUBS_SUBMERGE, defender + SUBS_FIRE, attacker + SELECT_SUB_CASUALTIES,
            REMOVE_SNEAK_ATTACK_CASUALTIES, attacker + SUBS_FIRE, defender + SELECT_SUB_CASUALTIES, defender + FIRE,
            attacker + SELECT_CASUALTIES, REMOVE_CASUALTIES, attacker + ATTACKER_WITHDRAW).toString(),
        steps.toString());
    bridge.setRemote(dummyPlayer);
    // defending subs sneak attack and hit
    // no chance to return fire
    final ScriptedRandomSource randomSource = new ScriptedRandomSource(0, ScriptedRandomSource.ERROR);
    bridge.setRandomSource(randomSource);
    battle.fight(bridge);
    assertEquals(1, randomSource.getTotalRolled());
    assertTrue(attacked.getUnits().getMatches(Matches.unitIsOwnedBy(british(gameData))).isEmpty());
    assertEquals(2, attacked.getUnits().size());
  }

  @Test
  public void testAttackDestroyerAndSubsAgainstSub() {
    final String defender = "Germans";
    final String attacker = "British";
    final Territory attacked = territory("31 Sea Zone", gameData);
    final Territory from = territory("32 Sea Zone", gameData);
    // 1 sub and 1 destroyer attack 1 sub
    // defender sneak attacks, not attacker
    addTo(from, submarine(gameData).create(1, british(gameData)));
    addTo(from, destroyer(gameData).create(1, british(gameData)));
    addTo(attacked, submarine(gameData).create(1, germans(gameData)));
    final ITestDelegateBridge bridge = getDelegateBridge(british(gameData));
    bridge.setStepName("CombatMove");
    moveDelegate(gameData).setDelegateBridgeAndPlayer(bridge);
    moveDelegate(gameData).start();
    move(from.getUnits().getUnits(), new Route(from, attacked));
    moveDelegate(gameData).end();
    final MustFightBattle battle =
        (MustFightBattle) AbstractMoveDelegate.getBattleTracker(gameData).getPendingBattle(attacked, false, null);
    final List<String> steps = battle.determineStepStrings(true, bridge);
    assertEquals(
        Arrays.asList(attacker + SUBS_SUBMERGE, attacker + SUBS_FIRE, defender + SELECT_SUB_CASUALTIES,
            REMOVE_SNEAK_ATTACK_CASUALTIES, attacker + FIRE, defender + SELECT_CASUALTIES, defender + SUBS_FIRE,
            attacker + SELECT_SUB_CASUALTIES, REMOVE_CASUALTIES, attacker + ATTACKER_WITHDRAW).toString(),
        steps.toString());
    bridge.setRemote(dummyPlayer);
    // attacking subs sneak attack and hit
    // no chance to return fire
    final ScriptedRandomSource randomSource = new ScriptedRandomSource(0, ScriptedRandomSource.ERROR);
    bridge.setRandomSource(randomSource);
    battle.fight(bridge);
    assertEquals(1, randomSource.getTotalRolled());
    assertTrue(attacked.getUnits().getMatches(Matches.unitIsOwnedBy(germans(gameData))).isEmpty());
    assertEquals(2, attacked.getUnits().size());
  }

  @Test
  public void testAttackDestroyerAndSubsAgainstSubAndDestroyer() {
    final String defender = "Germans";
    final String attacker = "British";
    final Territory attacked = territory("31 Sea Zone", gameData);
    final Territory from = territory("32 Sea Zone", gameData);
    // 1 sub and 1 destroyer attack 1 sub and 1 destroyer
    // no sneak attacks
    addTo(from, submarine(gameData).create(1, british(gameData)));
    addTo(from, destroyer(gameData).create(1, british(gameData)));
    addTo(attacked, submarine(gameData).create(1, germans(gameData)));
    addTo(attacked, destroyer(gameData).create(1, germans(gameData)));
    final ITestDelegateBridge bridge = getDelegateBridge(british(gameData));
    bridge.setStepName("CombatMove");
    moveDelegate(gameData).setDelegateBridgeAndPlayer(bridge);
    moveDelegate(gameData).start();
    move(from.getUnits().getUnits(), new Route(from, attacked));
    moveDelegate(gameData).end();
    final MustFightBattle battle =
        (MustFightBattle) AbstractMoveDelegate.getBattleTracker(gameData).getPendingBattle(attacked, false, null);
    final List<String> steps = battle.determineStepStrings(true, bridge);
    assertEquals(
        Arrays.asList(attacker + SUBS_FIRE, defender + SELECT_SUB_CASUALTIES, attacker + FIRE,
            defender + SELECT_CASUALTIES, defender + SUBS_FIRE, attacker + SELECT_SUB_CASUALTIES, defender + FIRE,
            attacker + SELECT_CASUALTIES, REMOVE_CASUALTIES, attacker + ATTACKER_WITHDRAW).toString(),
        steps.toString());
    when(dummyPlayer.selectCasualties(any(), any(), anyInt(), any(), any(), any(), any(), any(), any(), anyBoolean(),
        any(),
        any(), any(), any(), anyBoolean())).thenAnswer(new Answer<CasualtyDetails>() {
          @Override
          public CasualtyDetails answer(final InvocationOnMock invocation) {
            final Collection<Unit> selectFrom = invocation.getArgument(0);
            return new CasualtyDetails(Arrays.asList(selectFrom.iterator().next()), new ArrayList<>(), false);
          }
        });
    bridge.setRemote(dummyPlayer);
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
    final MoveDelegate move = moveDelegate(gameData);
    final ITestDelegateBridge bridge = getDelegateBridge(italians(gameData));

    when(dummyPlayer.selectShoreBombard(any())).thenReturn(true);
    bridge.setRemote(dummyPlayer);
    bridge.setStepName("CombatMove");
    move.setDelegateBridgeAndPlayer(bridge);
    move.start();
    final Territory sz14 = territory("14 Sea Zone", gameData);
    final Territory sz15 = territory("15 Sea Zone", gameData);
    final Territory eg = territory("Egypt", gameData);
    final Territory li = territory("Libya", gameData);
    final Territory balkans = territory("Balkans", gameData);
    // Clear all units from the attacked terr
    removeFrom(eg, eg.getUnits().getUnits());
    // Add 2 inf
    final PlayerID british = GameDataTestUtil.british(gameData);
    addTo(eg, infantry(gameData).create(2, british));
    // load the transports
    load(balkans.getUnits().getMatches(Matches.UnitIsInfantry), new Route(balkans, sz14));
    // move the fleet
    move(sz14.getUnits().getUnits(), new Route(sz14, sz15));
    // move troops from Libya
    move(li.getUnits().getMatches(Matches.unitOwnedBy(italians(gameData))), new Route(li, eg));
    // unload the transports
    move(sz15.getUnits().getMatches(Matches.UnitIsLand), new Route(sz15, eg));
    move.end();
    // start the battle phase, this will ask the user to bombard
    battleDelegate(gameData).setDelegateBridgeAndPlayer(bridge);
    BattleDelegate.doInitialize(battleDelegate(gameData).getBattleTracker(), bridge);
    battleDelegate(gameData).addBombardmentSources();
    final MustFightBattle mfb =
        (MustFightBattle) AbstractMoveDelegate.getBattleTracker(gameData).getPendingBattle(eg, false, null);
    // only 2 ships are allowed to bombard (there are 1 battleship and 2 cruisers that COULD bombard, but only 2 ships
    // may bombard total)
    assertEquals(2, mfb.getBombardingUnits().size());
    // Show that bombard casualties can return fire
    // Note- the 3 & 2 hits below show default behavior of bombarding at attack strength
    // 2= Battleship hitting a 3, 2=Cruiser hitting a 3, 15=British infantry hitting once
    bridge.setRandomSource(new ScriptedRandomSource(2, 2, 1, 5, 5, 5, 5, 5));
    battleDelegate(gameData).setDelegateBridgeAndPlayer(bridge);
    battleDelegate(gameData).start();
    // end result should be 2 italian infantry.
    assertEquals(3, eg.getUnits().size());
  }

  @Test
  public void testBombardStrengthVariable() {
    final MoveDelegate move = moveDelegate(gameData);
    final ITestDelegateBridge bridge = getDelegateBridge(italians(gameData));
    when(dummyPlayer.selectShoreBombard(any())).thenReturn(true);
    bridge.setRemote(dummyPlayer);
    bridge.setStepName("CombatMove");
    move.setDelegateBridgeAndPlayer(bridge);
    move.start();
    final Territory sz14 = territory("14 Sea Zone", gameData);
    final Territory sz15 = territory("15 Sea Zone", gameData);
    final Territory eg = territory("Egypt", gameData);
    final Territory balkans = territory("Balkans", gameData);
    // Clear all units
    removeFrom(eg, eg.getUnits().getUnits());
    removeFrom(sz14, sz14.getUnits().getUnits());
    // Add 2 inf to the attacked terr
    final PlayerID british = GameDataTestUtil.british(gameData);
    addTo(eg, infantry(gameData).create(2, british));
    // create/load the destroyers and transports
    final PlayerID italians = GameDataTestUtil.italians(gameData);
    addTo(sz14, transport(gameData).create(1, italians));
    addTo(sz14, destroyer(gameData).create(2, italians));
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
    UnitAttachment.get(destroyer(gameData)).setCanBombard("true");
    // Set the bombard strength for the DDs
    final Collection<Unit> dds = Match.getMatches(sz15.getUnits().getUnits(), Matches.UnitIsDestroyer);
    final Iterator<Unit> ddIter = dds.iterator();
    while (ddIter.hasNext()) {
      final Unit unit = ddIter.next();
      final UnitAttachment ua = UnitAttachment.get(unit.getType());
      ua.setBombard("3");
    }
    // start the battle phase, this will ask the user to bombard
    battleDelegate(gameData).setDelegateBridgeAndPlayer(bridge);
    BattleDelegate.doInitialize(battleDelegate(gameData).getBattleTracker(), bridge);
    battleDelegate(gameData).addBombardmentSources();
    final MustFightBattle mfb =
        (MustFightBattle) AbstractMoveDelegate.getBattleTracker(gameData).getPendingBattle(eg, false, null);
    assertNotNull(mfb);
    // Show that bombard casualties can return fire
    // destroyer bombard hit/miss on rolls of 4 & 3
    // landing inf miss
    // defending inf hit
    bridge.setRandomSource(new ScriptedRandomSource(3, 2, 6, 6, 1, 1));
    battleDelegate(gameData).setDelegateBridgeAndPlayer(bridge);
    BattleDelegate.doInitialize(battleDelegate(gameData).getBattleTracker(), bridge);
    battleDelegate(gameData).addBombardmentSources();
    fight(battleDelegate(gameData), eg);
    // 1 defending inf remaining
    assertEquals(1, eg.getUnits().size());
  }

  @Test
  public void testAmphAttackUndoAndAttackAgainBombard() {
    final MoveDelegate move = moveDelegate(gameData);
    final ITestDelegateBridge bridge = getDelegateBridge(italians(gameData));
    when(dummyPlayer.selectShoreBombard(any())).thenReturn(true);
    bridge.setRemote(dummyPlayer);
    bridge.setStepName("CombatMove");
    move.setDelegateBridgeAndPlayer(bridge);
    move.start();
    final Territory sz14 = territory("14 Sea Zone", gameData);
    final Territory sz15 = territory("15 Sea Zone", gameData);
    final Territory eg = territory("Egypt", gameData);
    final Territory li = territory("Libya", gameData);
    final Territory balkans = territory("Balkans", gameData);
    // load the transports
    load(balkans.getUnits().getMatches(Matches.UnitIsInfantry), new Route(balkans, sz14));
    // move the fleet
    move(sz14.getUnits().getUnits(), new Route(sz14, sz15));
    // move troops from Libya
    move(li.getUnits().getMatches(Matches.unitOwnedBy(italians(gameData))), new Route(li, eg));
    // unload the transports
    move(sz15.getUnits().getMatches(Matches.UnitIsLand), new Route(sz15, eg));
    // undo amphibious landing
    move.undoMove(move.getMovesMade().size() - 1);
    // move again
    move(sz15.getUnits().getMatches(Matches.UnitIsLand), new Route(sz15, eg));
    move.end();
    // start the battle phase, this will ask the user to bombard
    battleDelegate(gameData).setDelegateBridgeAndPlayer(bridge);
    BattleDelegate.doInitialize(battleDelegate(gameData).getBattleTracker(), bridge);
    battleDelegate(gameData).addBombardmentSources();
    final MustFightBattle mfb =
        (MustFightBattle) AbstractMoveDelegate.getBattleTracker(gameData).getPendingBattle(eg, false, null);
    // only 2 battleships are allowed to bombard
    assertEquals(2, mfb.getBombardingUnits().size());
  }

  @Test
  public void testCarrierWithAlliedPlanes() {
    final Territory sz8 = territory("8 Sea Zone", gameData);
    final Territory sz1 = territory("1 Sea Zone", gameData);
    addTo(sz8, carrier(gameData).create(1, british(gameData)));
    addTo(sz8, fighter(gameData).create(1, americans(gameData)));
    final Route route = new Route(sz8, sz1);
    final ITestDelegateBridge bridge = getDelegateBridge(british(gameData));
    bridge.setStepName("CombatMove");
    moveDelegate(gameData).setDelegateBridgeAndPlayer(bridge);
    moveDelegate(gameData).start();
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
    final Territory madagascar = territory("French Madagascar", gameData);
    final PlayerID germans = germans(gameData);
    madagascar.setOwner(germans);
    final Territory sz40 = territory("40 Sea Zone", gameData);
    addTo(sz40, carrier(gameData).create(1, germans));
    addTo(sz40, fighter(gameData).create(1, italians(gameData)));
    addTo(madagascar, fighter(gameData).create(2, germans));
    final Route route = gameData.getMap().getRoute(madagascar, sz40);
    final ITestDelegateBridge bridge = getDelegateBridge(germans);
    bridge.setStepName("CombatMove");
    moveDelegate(gameData).setDelegateBridgeAndPlayer(bridge);
    moveDelegate(gameData).start();
    // don't allow kamikaze
    bridge.setRemote(dummyPlayer);
    final String error = moveDelegate(gameData).move(madagascar.getUnits().getUnits(), route);
    assertError(error);
  }

  @Test
  public void testMechInfSimple() {
    final PlayerID germans = germans(gameData);
    final Territory france = territory("France", gameData);
    final Territory germany = territory("Germany", gameData);
    final Territory poland = territory("Poland", gameData);
    TechAttachment.get(germans).setMechanizedInfantry("true");
    final ITestDelegateBridge bridge = getDelegateBridge(germans);
    bridge.setStepName("CombatMove");
    moveDelegate(gameData).setDelegateBridgeAndPlayer(bridge);
    moveDelegate(gameData).start();
    final Route r = new Route(france, germany, poland);
    final List<Unit> toMove = new ArrayList<>();
    // 1 armour and 1 infantry
    toMove.addAll(france.getUnits().getMatches(Matches.UnitCanBlitz));
    toMove.add(france.getUnits().getMatches(Matches.UnitIsInfantry).get(0));
    move(toMove, r);
  }

  @Test
  public void testMechInfUnitAlreadyMovedSimple() {
    final PlayerID germans = germans(gameData);
    final Territory france = territory("France", gameData);
    final Territory germany = territory("Germany", gameData);
    TechAttachment.get(germans).setMechanizedInfantry("true");
    final ITestDelegateBridge bridge = getDelegateBridge(germans);
    bridge.setStepName("CombatMove");
    moveDelegate(gameData).setDelegateBridgeAndPlayer(bridge);
    moveDelegate(gameData).start();
    // get rid of the infantry in france
    removeFrom(france, france.getUnits().getMatches(Matches.UnitIsInfantry));
    // move an infantry from germany to france
    move(germany.getUnits().getMatches(Matches.UnitIsInfantry).subList(0, 1), new Route(germany, france));
    // try to move all the units in france, the infantry should not be able to move
    final Route r = new Route(france, germany);
    final String error = moveDelegate(gameData).move(france.getUnits().getUnits(), r);
    assertFalse(error == null);
  }

  @Test
  public void testParatroopsWalkOnWater() {
    final PlayerID germans = germans(gameData);
    final Territory france = territory("France", gameData);
    TechAttachment.get(germans).setParatroopers("true");
    final Route r = gameData.getMap().getRoute(france, territory("7 Sea Zone", gameData));
    final Collection<Unit> paratroopers = france.getUnits().getMatches(Matches.UnitIsAirTransportable);
    assertFalse(paratroopers.isEmpty());
    final MoveValidationResult results = MoveValidator.validateMove(paratroopers, r, germans,
        Collections.<Unit>emptyList(), new HashMap<>(), false, null, gameData);
    assertFalse(results.isMoveValid());
  }

  @Test
  public void testBomberWithTankOverWaterParatroopers() {
    final PlayerID germans = germans(gameData);
    TechAttachment.get(germans).setParatroopers("true");
    final Territory sz5 = territory("5 Sea Zone", gameData);
    final Territory germany = territory("Germany", gameData);
    final Territory karelia = territory("Karelia S.S.R.", gameData);
    addTo(germany, armour(gameData).create(1, germans));
    final Route r = new Route(germany, sz5, karelia);
    final Collection<Unit> toMove = germany.getUnits().getMatches(Matches.UnitCanBlitz);
    toMove.addAll(germany.getUnits().getMatches(Matches.UnitIsStrategicBomber));
    assertEquals(2, toMove.size());
    final MoveValidationResult results = MoveValidator.validateMove(toMove, r, germans, Collections.<Unit>emptyList(),
        new HashMap<>(), false, null, gameData);
    assertFalse(results.isMoveValid());
  }

  @Test
  public void testBomberTankOverWater() {
    // can't transport a tank over water using a bomber
    final PlayerID germans = germans(gameData);
    final Territory sz5 = territory("5 Sea Zone", gameData);
    final Territory germany = territory("Germany", gameData);
    final Territory karelia = territory("Karelia S.S.R.", gameData);
    addTo(germany, armour(gameData).create(1, germans));
    final Route r = new Route(germany, sz5, karelia);
    final Collection<Unit> toMove = germany.getUnits().getMatches(Matches.UnitCanBlitz);
    toMove.addAll(germany.getUnits().getMatches(Matches.UnitIsStrategicBomber));
    assertEquals(2, toMove.size());
    final MoveValidationResult results = MoveValidator.validateMove(toMove, r, germans, Collections.<Unit>emptyList(),
        new HashMap<>(), false, null, gameData);
    assertFalse(results.isMoveValid());
  }

  @Test
  public void testMoveParatroopersAsNonPartroops() {
    // move a bomber and a paratrooper
    // one step, but as a normal movement
    final PlayerID germans = germans(gameData);
    final Territory germany = territory("Germany", gameData);
    final Territory nwe = territory("Northwestern Europe", gameData);
    final ITestDelegateBridge bridge = getDelegateBridge(germans);
    bridge.setStepName("CombatMove");
    moveDelegate(gameData).setDelegateBridgeAndPlayer(bridge);
    moveDelegate(gameData).start();
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
    final PlayerID germans = germans(gameData);
    final Territory germany = territory("Germany", gameData);
    final Territory nwe = territory("Northwestern Europe", gameData);
    final Territory poland = territory("Poland", gameData);
    final Territory eastPoland = territory("East Poland", gameData);
    final ITestDelegateBridge bridge = getDelegateBridge(germans);
    bridge.setStepName("CombatMove");
    moveDelegate(gameData).setDelegateBridgeAndPlayer(bridge);
    moveDelegate(gameData).start();
    TechAttachment.get(germans).setParatroopers("true");
    final List<Unit> paratrooper = nwe.getUnits().getMatches(Matches.UnitIsAirTransportable);
    move(paratrooper, new Route(nwe, germany));
    final List<Unit> bomberAndParatroop = new ArrayList<>(paratrooper);
    bomberAndParatroop.addAll(germany.getUnits().getMatches(Matches.UnitIsAirTransport));
    // move the units to east poland
    final String error = moveDelegate(gameData).move(bomberAndParatroop, new Route(germany, poland, eastPoland));
    assertError(error);
  }

  @Test
  public void testCantTransportParatroopersWithBombersThatMovedPreviously() {
    // make sure bombers can't move then pick up paratroopers
    final PlayerID germans = germans(gameData);
    final Territory germany = territory("Germany", gameData);
    final Territory bulgaria = territory("Bulgaria Romania", gameData);
    final Territory poland = territory("Poland", gameData);
    final Territory ukraine = territory("Ukraine", gameData);
    final ITestDelegateBridge bridge = getDelegateBridge(germans);
    bridge.setStepName("CombatMove");
    moveDelegate(gameData).setDelegateBridgeAndPlayer(bridge);
    moveDelegate(gameData).start();
    TechAttachment.get(germans).setParatroopers("true");
    // Move the bomber first
    final List<Unit> bomber = germany.getUnits().getMatches(Matches.UnitIsAirTransport);
    move(bomber, new Route(germany, poland));
    // Pick up a paratrooper
    final List<Unit> bomberAndParatroop = new ArrayList<>(bomber);
    bomberAndParatroop.addAll(poland.getUnits().getUnits(GameDataTestUtil.infantry(gameData), 1));
    // move them
    final String error = moveDelegate(gameData).move(bomberAndParatroop, new Route(poland, bulgaria, ukraine));
    assertError(error);
  }

  @Test
  public void testMoveOneParatrooperPerBomber() {
    // make sure only 1 paratroop per bomber can be moved
    final PlayerID germans = germans(gameData);
    final Territory germany = territory("Germany", gameData);
    // Territory nwe = territory("Northwestern Europe", gameData);
    final Territory poland = territory("Poland", gameData);
    final Territory eastPoland = territory("East Poland", gameData);
    final ITestDelegateBridge bridge = getDelegateBridge(germans);
    bridge.setStepName("CombatMove");
    moveDelegate(gameData).setDelegateBridgeAndPlayer(bridge);
    moveDelegate(gameData).start();
    TechAttachment.get(germans).setParatroopers("true");
    final List<Unit> bomberAndParatroop = new ArrayList<>();
    bomberAndParatroop.addAll(germany.getUnits().getMatches(Matches.UnitIsAirTransport));
    // add 2 infantry
    bomberAndParatroop.addAll(germany.getUnits().getUnits(GameDataTestUtil.infantry(gameData), 2));
    // move the units to east poland
    final String error = moveDelegate(gameData).move(bomberAndParatroop, new Route(germany, poland, eastPoland));
    assertError(error);
  }

  @Test
  public void testParatroopersMoveTwice() {
    // After a battle move to put a bomber + infantry (paratroop) in a first enemy
    // territory, you can make a new move (in the same battle move round) to put
    // bomber+ infantry in a more internal enemy territory.
    final PlayerID germans = germans(gameData);
    final Territory germany = territory("Germany", gameData);
    final Territory poland = territory("Poland", gameData);
    final Territory eastPoland = territory("East Poland", gameData);
    final Territory beloRussia = territory("Belorussia", gameData);
    final ITestDelegateBridge bridge = getDelegateBridge(germans);
    bridge.setStepName("CombatMove");
    moveDelegate(gameData).setDelegateBridgeAndPlayer(bridge);
    moveDelegate(gameData).start();
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
    String error = moveDelegate(gameData).move(bomberAndParatroop, route);
    assertValid(error);
    // try to move them further, this should fail
    error = moveDelegate(gameData).move(bomberAndParatroop, new Route(eastPoland, beloRussia));
    assertError(error);
  }

  @Test
  public void testParatroopersFlyOverBlitzedTerritory() {
    // We should be able to blitz a territory, then fly over it with paratroops to battle.
    final PlayerID germans = germans(gameData);
    final Territory germany = territory("Germany", gameData);
    final Territory poland = territory("Poland", gameData);
    final Territory eastPoland = territory("East Poland", gameData);
    final Territory beloRussia = territory("Belorussia", gameData);
    // Clear East Poland
    removeFrom(eastPoland, eastPoland.getUnits().getUnits());
    // Set up test
    final ITestDelegateBridge bridge = getDelegateBridge(germans);
    bridge.setStepName("CombatMove");
    moveDelegate(gameData).setDelegateBridgeAndPlayer(bridge);
    moveDelegate(gameData).start();
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
        moveDelegate(gameData).move(bomberAndParatroop, new Route(germany, poland, eastPoland, beloRussia));
    assertValid(error);
  }

  @Test
  public void testAmphibAttackWithPlanesOnlyAskRetreatOnce() {
    final PlayerID germans = germans(gameData);
    final ITestDelegateBridge bridge = getDelegateBridge(germans);
    bridge.setStepName("CombatMove");
    moveDelegate(gameData).setDelegateBridgeAndPlayer(bridge);
    moveDelegate(gameData).start();
    final Territory france = territory("France", gameData);
    final Territory egypt = territory("Egypt", gameData);
    final Territory balkans = territory("Balkans", gameData);
    final Territory libya = territory("Libya", gameData);
    final Territory germany = territory("Germany", gameData);
    final Territory sz13 = territory("13 Sea Zone", gameData);
    final Territory sz14 = territory("14 Sea Zone", gameData);
    final Territory sz15 = territory("15 Sea Zone", gameData);
    ;
    when(dummyPlayer.retreatQuery(any(), anyBoolean(), any(),
        any(), contains(BattleStepStrings.RETREAT_PLANES))).thenThrow(
            new AssertionError("The Message is not allowed to contain the BattleStepStrings.RETREAT_PLANES constant"));
    bridge.setRemote(dummyPlayer);
    final ITripleADisplay dummyDisplay = mock(ITripleADisplay.class);
    doThrow(new AssertionError(
        "None of the Battle steps is allow to contain the BattleStepStrings.PLANES_WITHDRAW constant"))
            .when(dummyDisplay).listBattleSteps(any(), argThat(list -> {
              for (final String string : list) {
                if (string.contains(BattleStepStrings.PLANES_WITHDRAW)) {
                  return true;
                }
              }
              return false;
            }));
    bridge.setDisplay(dummyDisplay);
    // move units for amphib assault
    load(france.getUnits().getMatches(Matches.UnitIsInfantry), new Route(france, sz13));
    move(sz13.getUnits().getUnits(), new Route(sz13, sz14, sz15));
    move(sz15.getUnits().getMatches(Matches.UnitIsInfantry), new Route(sz15, egypt));
    // ground attack
    load(libya.getUnits().getMatches(Matches.UnitIsArtillery), new Route(libya, egypt));
    // air units
    move(germany.getUnits().getMatches(Matches.UnitIsStrategicBomber), new Route(germany, balkans, sz14, sz15, egypt));
    moveDelegate(gameData).end();
    bridge.setStepName("Combat");
    // cook the dice so that all miss first round,all hit second round
    bridge.setRandomSource(new ScriptedRandomSource(5, 5, 5, 5, 5, 5, 5, 5, 5, 1, 1, 1, 1, 1, 1, 1, 1, 1));
    battleDelegate(gameData).setDelegateBridgeAndPlayer(bridge);
    battleDelegate(gameData).start();
  }

  @Test
  public void testDefencelessTransportsDie() {
    final PlayerID british = british(gameData);
    final ITestDelegateBridge bridge = getDelegateBridge(british);
    bridge.setStepName("CombatMove");
    moveDelegate(gameData).setDelegateBridgeAndPlayer(bridge);
    moveDelegate(gameData).start();
    final Territory uk = territory("United Kingdom", gameData);
    final Territory sz5 = territory("5 Sea Zone", gameData);
    // remove the sub
    removeFrom(sz5, sz5.getUnits().getMatches(Matches.UnitIsSub));

    when(dummyPlayer.retreatQuery(any(), anyBoolean(), any(),
        any(), any())).thenAnswer(new Answer<Territory>() {
          @Override
          public Territory answer(final InvocationOnMock invocation) throws Throwable {
            throw new IllegalStateException(
                "Should not be asked to retreat:" + invocation.getArgument(4));
          }
        });
    bridge.setRemote(dummyPlayer);
    move(uk.getUnits().getMatches(Matches.UnitIsAir), gameData.getMap().getRoute(uk, sz5));
    // move units for amphib assault
    moveDelegate(gameData).end();
    bridge.setStepName("Combat");
    // cook the dice so that 1 british fighters hits, and nothing else
    // this will leave 1 transport alone in the sea zone
    bridge.setRandomSource(new ScriptedRandomSource(1, 5, 5, 5, 5, 5, 5, 5, 5));
    battleDelegate(gameData).setDelegateBridgeAndPlayer(bridge);
    battleDelegate(gameData).start();
    // make sure the transports died
    assertTrue(sz5.getUnits().getMatches(Matches.unitIsOwnedBy(germans(gameData))).isEmpty());
  }

  @Test
  public void testFighterLandsWhereCarrierCanBePlaced() {
    final PlayerID germans = germans(gameData);
    // germans have 1 carrier to place
    addTo(germans, carrier(gameData).create(1, germans), gameData);
    // start the move phase
    final ITestDelegateBridge bridge = getDelegateBridge(germans);
    bridge.setStepName("CombatMove");
    moveDelegate(gameData).setDelegateBridgeAndPlayer(bridge);
    moveDelegate(gameData).start();
    bridge.setRemote(dummyPlayer);
    // the fighter should be able to move and hover in the sea zone
    // the fighter has no movement left
    final Territory neEurope = territory("Northwestern Europe", gameData);
    final Route route = new Route(neEurope, territory("Germany", gameData), territory("Poland", gameData),
        territory("Baltic States", gameData), territory("5 Sea Zone", gameData));
    // the fighter should be able to move, and hover in the sea zone until the carrier is placed
    move(neEurope.getUnits().getMatches(Matches.UnitIsAir), route);
  }

  @Test
  public void testFighterCantHoverWithNoCarrierToPlace() {
    // start the move phase
    final ITestDelegateBridge bridge = getDelegateBridge(germans(gameData));
    bridge.setStepName("CombatMove");
    moveDelegate(gameData).setDelegateBridgeAndPlayer(bridge);
    moveDelegate(gameData).start();
    bridge.setRemote(dummyPlayer);
    // the fighter should not be able to move and hover in the sea zone
    // since their are no carriers to place
    // the fighter has no movement left
    final Territory neEurope = territory("Northwestern Europe", gameData);
    final Route route = new Route(neEurope, territory("Germany", gameData), territory("Poland", gameData),
        territory("Baltic States", gameData), territory("5 Sea Zone", gameData));
    final String error = moveDelegate(gameData).move(neEurope.getUnits().getMatches(Matches.UnitIsAir), route);
    assertNotNull(error);
  }

  @Test
  public void testRepair() {
    final Territory germany = territory("Germany", gameData);
    final Unit factory = germany.getUnits().getMatches(Matches.UnitCanBeDamaged).get(0);
    final PurchaseDelegate del = purchaseDelegate(gameData);
    del.setDelegateBridgeAndPlayer(getDelegateBridge(germans(gameData)));
    del.start();
    // Set up player
    final PlayerID germans = GameDataTestUtil.germans(gameData);
    final int initPUs = germans.getResources().getQuantity("PUs");
    // damage a factory
    IntegerMap<Unit> startHits = new IntegerMap<>();
    startHits.put(factory, 1);
    gameData.performChange(ChangeFactory.bombingUnitDamage(startHits));
    assertEquals(((TripleAUnit) factory).getUnitDamage(), 1);
    RepairRule repair = germans(gameData).getRepairFrontier().getRules().get(0);
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
    final ITestDelegateBridge delegateBridge = getDelegateBridge(germans(gameData));
    TechTracker.addAdvance(germans, delegateBridge,
        TechAdvance.findAdvance(TechAdvance.TECH_PROPERTY_INCREASED_FACTORY_PRODUCTION, gameData, germans));
    // damage a factory
    startHits = new IntegerMap<>();
    startHits.put(factory, 2);
    gameData.performChange(ChangeFactory.bombingUnitDamage(startHits));
    assertEquals(((TripleAUnit) factory).getUnitDamage(), 2);
    repair = germans(gameData).getRepairFrontier().getRules().get(0);
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
    final Territory germany = territory("Germany", gameData);
    final Unit factory = germany.getUnits().getMatches(Matches.UnitCanBeDamaged).get(0);
    final PurchaseDelegate del = purchaseDelegate(gameData);
    del.setDelegateBridgeAndPlayer(getDelegateBridge(germans(gameData)));
    del.start();
    // dame a factory
    final IntegerMap<Unit> startHits = new IntegerMap<>();
    startHits.put(factory, 1);
    gameData.performChange(ChangeFactory.bombingUnitDamage(startHits));
    assertEquals(((TripleAUnit) factory).getUnitDamage(), 1);
    final RepairRule repair = germans(gameData).getRepairFrontier().getRules().get(0);
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
    final PlayerID british = GameDataTestUtil.british(gameData);
    final ITestDelegateBridge delegateBridge = getDelegateBridge(british(gameData));
    // Set up the move delegate
    final MoveDelegate moveDelegate = moveDelegate(gameData);
    delegateBridge.setStepName("CombatMove");
    moveDelegate(gameData).setDelegateBridgeAndPlayer(delegateBridge);
    moveDelegate(gameData).start();
    // Set up the territories
    final Territory hupeh = territory("Hupeh", gameData);
    final Territory kiangsu = territory("Kiangsu", gameData);
    // Remove all units
    removeFrom(kiangsu, kiangsu.getUnits().getUnits());
    removeFrom(hupeh, hupeh.getUnits().getUnits());
    // Set up the unit types
    addTo(hupeh, infantry(gameData).create(1, british));
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
    final PlayerID british = GameDataTestUtil.british(gameData);
    final ITestDelegateBridge delegateBridge = getDelegateBridge(british(gameData));
    // Set up the move delegate
    final MoveDelegate moveDelegate = moveDelegate(gameData);
    delegateBridge.setStepName("CombatMove");
    moveDelegate(gameData).setDelegateBridgeAndPlayer(delegateBridge);
    moveDelegate(gameData).start();
    // Set up the territories
    final Territory hupeh = territory("Hupeh", gameData);
    final Territory kiangsu = territory("Kiangsu", gameData);
    final Territory mongolia = territory("Mongolia", gameData);
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
    addTo(hupeh, infantry(gameData).create(1, british));
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
    final ITestDelegateBridge delegateBridge = getDelegateBridge(british(gameData));
    // Set up the territories
    final Territory libya = territory("Libya", gameData);
    final Territory egypt = territory("Egypt", gameData);
    final Territory morrocco = territory("Morocco Algeria", gameData);
    removeFrom(libya, libya.getUnits().getUnits());
    // Set up the move delegate
    final MoveDelegate moveDelegate = moveDelegate(gameData);
    delegateBridge.setStepName("CombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(delegateBridge);
    moveDelegate.start();
    // blitz in two steps
    final Collection<Unit> armour = egypt.getUnits().getMatches(Matches.UnitCanBlitz);
    move(armour, new Route(egypt, libya));
    assertEquals(libya.getOwner(), british(gameData));
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
