package games.strategy.triplea.delegate;

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
import static games.strategy.triplea.delegate.MockDelegateBridge.advanceToStep;
import static games.strategy.triplea.delegate.MockDelegateBridge.newDelegateBridge;
import static games.strategy.triplea.delegate.MockDelegateBridge.thenGetRandomShouldHaveBeenCalled;
import static games.strategy.triplea.delegate.MockDelegateBridge.whenGetRandom;
import static games.strategy.triplea.delegate.MockDelegateBridge.withValues;
import static games.strategy.triplea.delegate.battle.BattleStepStrings.ATTACKER_WITHDRAW;
import static games.strategy.triplea.delegate.battle.BattleStepStrings.REMOVE_CASUALTIES;
import static games.strategy.triplea.delegate.battle.BattleStepStrings.REMOVE_SNEAK_ATTACK_CASUALTIES;
import static games.strategy.triplea.delegate.battle.BattleStepStrings.SUBS_SUBMERGE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.MoveDescription;
import games.strategy.engine.data.RepairRule;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.TechnologyFrontier;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.engine.data.gameparser.GameParseException;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.Constants;
import games.strategy.triplea.Properties;
import games.strategy.triplea.attachments.TechAttachment;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.battle.BattleDelegate;
import games.strategy.triplea.delegate.battle.BattleState;
import games.strategy.triplea.delegate.battle.IBattle.BattleType;
import games.strategy.triplea.delegate.battle.MustFightBattle;
import games.strategy.triplea.delegate.battle.casualty.AaCasualtySelector;
import games.strategy.triplea.delegate.battle.steps.BattleStepsTest;
import games.strategy.triplea.delegate.data.CasualtyDetails;
import games.strategy.triplea.delegate.data.MoveValidationResult;
import games.strategy.triplea.delegate.data.PlaceableUnits;
import games.strategy.triplea.delegate.data.TechResults;
import games.strategy.triplea.delegate.dice.RollDiceFactory;
import games.strategy.triplea.delegate.move.validation.MoveValidator;
import games.strategy.triplea.delegate.power.calculator.CombatValueBuilder;
import games.strategy.triplea.delegate.remote.IAbstractPlaceDelegate;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import org.triplea.java.collections.CollectionUtils;
import org.triplea.java.collections.IntegerMap;

class WW2V3Year41Test {
  private final GameData gameData = TestMapGameData.WW2V3_1941.getGameData();

  private static void givenRemotePlayerWillSelectAttackSubs(final IDelegateBridge delegateBridge) {
    when(delegateBridge.getRemotePlayer().selectAttackSubs(any())).thenReturn(true);
  }

  private static void givenRemotePlayerWillSelectCasualtiesPer(
      final IDelegateBridge delegateBridge, final Answer<?> answer) {
    when(delegateBridge
            .getRemotePlayer()
            .selectCasualties(
                any(),
                any(),
                anyInt(),
                any(),
                any(),
                any(),
                any(),
                any(),
                anyBoolean(),
                any(),
                any(),
                any(),
                any(),
                anyBoolean()))
        .thenAnswer(answer);
  }

  private static void givenRemotePlayerWillSelectShoreBombard(
      final IDelegateBridge delegateBridge) {
    when(delegateBridge.getRemotePlayer().selectShoreBombard(any())).thenReturn(true);
  }

  private static void thenRemotePlayerShouldNotBeAskedToRetreat(
      final IDelegateBridge delegateBridge) {
    verify(delegateBridge.getRemotePlayer(), never())
        .retreatQuery(any(), anyBoolean(), any(), any(), any());
  }

  private static void fight(final BattleDelegate battle, final Territory territory) {
    for (final Entry<BattleType, Collection<Territory>> entry :
        battle.getBattles().getBattles().entrySet()) {
      if (!entry.getKey().isBombingRun() && entry.getValue().contains(territory)) {
        battle.fightBattle(territory, false, entry.getKey());
        return;
      }
    }
    throw new IllegalStateException("Could not find battle in: " + territory.getName());
  }

  @Test
  void testAaCasualtiesLowLuckMixedRadar() {
    // moved from BattleCalculatorTest because "revised" does not have "radar"
    final GamePlayer british = british(gameData);
    final IDelegateBridge bridge = newDelegateBridge(british);
    makeGameLowLuck(gameData);
    // setSelectAACasualties(data, false);
    givePlayerRadar(germans(gameData));
    // 3 bombers and 3 fighters
    final Collection<Unit> planes = bomber(gameData).create(3, british(gameData));
    planes.addAll(fighter(gameData).create(3, british(gameData)));
    final Collection<Unit> defendingAa =
        territory("Germany", gameData)
            .getUnitCollection()
            .getMatches(Matches.unitIsAaForAnything());
    // don't allow rolling, 6 of each is deterministic
    final DiceRoll roll =
        RollDiceFactory.rollAaDice(
            CollectionUtils.getMatches(
                planes,
                Matches.unitIsOfTypes(
                    UnitAttachment.get(defendingAa.iterator().next().getType())
                        .getTargetsAa(gameData.getUnitTypeList()))),
            defendingAa,
            bridge,
            territory("Germany", gameData),
            CombatValueBuilder.aaCombatValue()
                .enemyUnits(planes)
                .friendlyUnits(territory("Germany", gameData).getUnits())
                .side(BattleState.Side.DEFENSE)
                .supportAttachments(bridge.getData().getUnitTypeList().getSupportAaRules())
                .build());
    final Collection<Unit> casualties =
        AaCasualtySelector.getAaCasualties(
                planes,
                defendingAa,
                CombatValueBuilder.mainCombatValue()
                    .enemyUnits(defendingAa)
                    .friendlyUnits(planes)
                    .side(BattleState.Side.OFFENSE)
                    .gameSequence(gameData.getSequence())
                    .supportAttachments(gameData.getUnitTypeList().getSupportRules())
                    .lhtrHeavyBombers(Properties.getLhtrHeavyBombers(gameData.getProperties()))
                    .gameDiceSides(gameData.getDiceSides())
                    .territoryEffects(List.of())
                    .build(),
                CombatValueBuilder.aaCombatValue()
                    .enemyUnits(planes)
                    .friendlyUnits(defendingAa)
                    .side(BattleState.Side.DEFENSE)
                    .supportAttachments(gameData.getUnitTypeList().getSupportAaRules())
                    .build(),
                "",
                roll,
                bridge,
                null,
                null,
                territory("Germany", gameData))
            .getKilled();
    assertEquals(2, casualties.size());
    // should be 1 fighter and 1 bomber
    assertEquals(1, CollectionUtils.countMatches(casualties, Matches.unitIsStrategicBomber()));
    assertEquals(
        1, CollectionUtils.countMatches(casualties, Matches.unitIsStrategicBomber().negate()));
    thenGetRandomShouldHaveBeenCalled(bridge, never());
  }

  @Test
  void testAaCasualtiesLowLuckMixedWithRollingRadar() {
    // moved from BattleCalculatorTest because "revised" does not have "radar"
    final GamePlayer british = british(gameData);
    final IDelegateBridge bridge = newDelegateBridge(british);
    makeGameLowLuck(gameData);
    // setSelectAACasualties(data, false);
    givePlayerRadar(germans(gameData));
    // 4 bombers and 4 fighters
    final Collection<Unit> planes = bomber(gameData).create(4, british(gameData));
    planes.addAll(fighter(gameData).create(4, british(gameData)));
    final Collection<Unit> defendingAa =
        territory("Germany", gameData)
            .getUnitCollection()
            .getMatches(Matches.unitIsAaForAnything());
    // 1 roll, a hit
    // then a dice to select the casualty
    whenGetRandom(bridge).thenAnswer(withValues(0)).thenAnswer(withValues(1));
    final DiceRoll roll =
        RollDiceFactory.rollAaDice(
            CollectionUtils.getMatches(
                planes,
                Matches.unitIsOfTypes(
                    UnitAttachment.get(defendingAa.iterator().next().getType())
                        .getTargetsAa(gameData.getUnitTypeList()))),
            defendingAa,
            bridge,
            territory("Germany", gameData),
            CombatValueBuilder.aaCombatValue()
                .enemyUnits(planes)
                .friendlyUnits(territory("Germany", gameData).getUnits())
                .side(BattleState.Side.DEFENSE)
                .supportAttachments(bridge.getData().getUnitTypeList().getSupportAaRules())
                .build());
    // make sure we rolled once
    thenGetRandomShouldHaveBeenCalled(bridge, times(1));
    final Collection<Unit> casualties =
        AaCasualtySelector.getAaCasualties(
                planes,
                defendingAa,
                CombatValueBuilder.mainCombatValue()
                    .enemyUnits(defendingAa)
                    .friendlyUnits(planes)
                    .side(BattleState.Side.OFFENSE)
                    .gameSequence(gameData.getSequence())
                    .supportAttachments(gameData.getUnitTypeList().getSupportRules())
                    .lhtrHeavyBombers(Properties.getLhtrHeavyBombers(gameData.getProperties()))
                    .gameDiceSides(gameData.getDiceSides())
                    .territoryEffects(List.of())
                    .build(),
                CombatValueBuilder.aaCombatValue()
                    .enemyUnits(planes)
                    .friendlyUnits(defendingAa)
                    .side(BattleState.Side.DEFENSE)
                    .supportAttachments(gameData.getUnitTypeList().getSupportAaRules())
                    .build(),
                "",
                roll,
                bridge,
                null,
                null,
                territory("Germany", gameData))
            .getKilled();
    assertEquals(3, casualties.size());
    // should be 1 fighter and 2 bombers
    assertEquals(2, CollectionUtils.countMatches(casualties, Matches.unitIsStrategicBomber()));
    assertEquals(
        1, CollectionUtils.countMatches(casualties, Matches.unitIsStrategicBomber().negate()));
  }

  @Test
  void testAaCasualtiesLowLuckMixedWithRollingMissRadar() {
    // moved from BattleCalculatorTest because "revised" does not have "radar"
    final GamePlayer british = GameDataTestUtil.british(gameData);
    final IDelegateBridge bridge = newDelegateBridge(british);
    makeGameLowLuck(gameData);
    // setSelectAACasualties(data, false);
    givePlayerRadar(germans(gameData));
    // 4 bombers and 4 fighters
    final Collection<Unit> planes = bomber(gameData).create(4, british(gameData));
    planes.addAll(fighter(gameData).create(4, british(gameData)));
    final Collection<Unit> defendingAa =
        territory("Germany", gameData)
            .getUnitCollection()
            .getMatches(Matches.unitIsAaForAnything());
    // 1 roll, a miss
    // then a dice to select the casualty
    whenGetRandom(bridge)
        .thenAnswer(withValues(5))
        .thenAnswer(withValues(0))
        .thenAnswer(withValues(0, 0));
    final DiceRoll roll =
        RollDiceFactory.rollAaDice(
            CollectionUtils.getMatches(
                planes,
                Matches.unitIsOfTypes(
                    UnitAttachment.get(defendingAa.iterator().next().getType())
                        .getTargetsAa(gameData.getUnitTypeList()))),
            defendingAa,
            bridge,
            territory("Germany", gameData),
            CombatValueBuilder.aaCombatValue()
                .enemyUnits(planes)
                .friendlyUnits(territory("Germany", gameData).getUnits())
                .side(BattleState.Side.DEFENSE)
                .supportAttachments(bridge.getData().getUnitTypeList().getSupportAaRules())
                .build());
    assertEquals(2, roll.getHits());
    // make sure we rolled once
    thenGetRandomShouldHaveBeenCalled(bridge, times(1));
    final Collection<Unit> casualties =
        AaCasualtySelector.getAaCasualties(
                planes,
                defendingAa,
                CombatValueBuilder.mainCombatValue()
                    .enemyUnits(defendingAa)
                    .friendlyUnits(planes)
                    .side(BattleState.Side.OFFENSE)
                    .gameSequence(gameData.getSequence())
                    .supportAttachments(gameData.getUnitTypeList().getSupportRules())
                    .lhtrHeavyBombers(Properties.getLhtrHeavyBombers(gameData.getProperties()))
                    .gameDiceSides(gameData.getDiceSides())
                    .territoryEffects(List.of())
                    .build(),
                CombatValueBuilder.aaCombatValue()
                    .enemyUnits(planes)
                    .friendlyUnits(defendingAa)
                    .side(BattleState.Side.DEFENSE)
                    .supportAttachments(gameData.getUnitTypeList().getSupportAaRules())
                    .build(),
                "",
                roll,
                bridge,
                null,
                null,
                territory("Germany", gameData))
            .getKilled();
    assertEquals(2, casualties.size());
    // random should not have been called during getAaCasualties so the number of times
    // should stay at 1
    thenGetRandomShouldHaveBeenCalled(bridge, times(1));
    // should be 1 fighter and 1 bomber
    assertEquals(1, CollectionUtils.countMatches(casualties, Matches.unitIsStrategicBomber()));
    assertEquals(
        1, CollectionUtils.countMatches(casualties, Matches.unitIsStrategicBomber().negate()));
  }

  @Test
  void testDefendingTrasnportsAutoKilled() {
    final Territory sz13 = gameData.getMap().getTerritory("13 Sea Zone");
    final Territory sz12 = gameData.getMap().getTerritory("12 Sea Zone");
    final GamePlayer british = GameDataTestUtil.british(gameData);
    final MoveDelegate moveDelegate = moveDelegate(gameData);
    final IDelegateBridge bridge = newDelegateBridge(british);
    advanceToStep(bridge, "CombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(bridge);
    moveDelegate.start();
    final Route sz12To13 = new Route(sz12, sz13);
    final String error = moveDelegate.move(sz12.getUnits(), sz12To13);
    assertNull(error);
    assertEquals(3, sz13.getUnitCollection().size());
    moveDelegate.end();
    // the transport was not removed automatically
    assertEquals(3, sz13.getUnitCollection().size());
    final BattleDelegate bd = battleDelegate(gameData);
    assertFalse(bd.getBattleTracker().getPendingBattleSites(false).isEmpty());
  }

  @Test
  void testUnplacedDie() {
    final PlaceDelegate del = placeDelegate(gameData);
    del.setDelegateBridgeAndPlayer(newDelegateBridge(british(gameData)));
    del.start();
    addTo(british(gameData), transport(gameData).create(1, british(gameData)), gameData);
    del.end();
    // unplaced units die
    assertEquals(1, british(gameData).getUnitCollection().size());
  }

  @Test
  void testPlaceEmpty() {
    final PlaceDelegate del = placeDelegate(gameData);
    del.setDelegateBridgeAndPlayer(newDelegateBridge(british(gameData)));
    del.start();
    addTo(british(gameData), transport(gameData).create(1, british(gameData)), gameData);
    final String error =
        del.placeUnits(
            List.of(),
            territory("United Kingdom", gameData),
            IAbstractPlaceDelegate.BidMode.NOT_BID);
    assertNull(error);
  }

  @Test
  void testTechTokens() {
    // Set up the test
    final GamePlayer germans = germans(gameData);
    final IDelegateBridge delegateBridge = newDelegateBridge(germans);
    advanceToStep(delegateBridge, "germanTech");
    final TechnologyDelegate techDelegate = techDelegate(gameData);
    techDelegate.setDelegateBridgeAndPlayer(delegateBridge);
    techDelegate.start();
    final TechnologyFrontier mech = new TechnologyFrontier("", gameData);
    mech.addAdvance(
        TechAdvance.findAdvance(
            TechAdvance.TECH_PROPERTY_MECHANIZED_INFANTRY, gameData.getTechnologyFrontier(), null));
    // Add tech token
    gameData.performChange(
        ChangeFactory.changeResourcesChange(
            germans, gameData.getResourceList().getResource(Constants.TECH_TOKENS), 1));
    // Check to make sure it was successful
    final int initTokens = germans.getResources().getQuantity("techTokens");
    assertEquals(1, initTokens);
    whenGetRandom(delegateBridge)
        .thenAnswer(withValues(3)) // Fail the roll
        .thenAnswer(withValues(5)); // Make a Successful roll
    // Fail the roll
    final TechResults roll = techDelegate.rollTech(1, mech, 0, null);
    // Check to make sure it failed
    assertEquals(0, roll.getHits());
    final int midTokens = germans.getResources().getQuantity("techTokens");
    assertEquals(1, midTokens);
    // Make a Successful roll
    final TechResults roll2 = techDelegate.rollTech(1, mech, 0, null);
    // Check to make sure it succeeded and all tokens were removed
    assertEquals(1, roll2.getHits());
    final int finalTokens = germans.getResources().getQuantity("techTokens");
    assertEquals(0, finalTokens);
  }

  @Test
  void testInfantryLoadOnlyTransports() {
    final Territory gibraltar = territory("Gibraltar", gameData);
    // add a tank to gibralter
    final GamePlayer british = british(gameData);
    addTo(gibraltar, infantry(gameData).create(1, british));
    final MoveDelegate moveDelegate = moveDelegate(gameData);
    final IDelegateBridge bridge = newDelegateBridge(british);
    advanceToStep(bridge, "britishCombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(bridge);
    moveDelegate.start();
    final Territory sz9 = territory("9 Sea Zone", gameData);
    final Territory sz13 = territory("13 Sea Zone", gameData);
    final Route sz9ToSz13 = new Route(sz9, territory("12 Sea Zone", gameData), sz13);
    // move the transport to attack, this is suicide but valid
    move(sz9.getUnitCollection().getMatches(Matches.unitIsTransport()), sz9ToSz13);
    // load the transport
    load(gibraltar.getUnits(), new Route(gibraltar, sz13));
    moveDelegate.end();
    advanceToStep(bridge, "britishBattle");
    final BattleDelegate battleDelegate = battleDelegate(gameData);
    battleDelegate.setDelegateBridgeAndPlayer(bridge);
    battleDelegate.start();
    assertTrue(battleDelegate.getBattles().isEmpty());
  }

  @Test
  void testLoadedTransportAttackKillsLoadedUnits() {
    final GamePlayer british = british(gameData);
    final MoveDelegate moveDelegate = moveDelegate(gameData);
    final IDelegateBridge bridge = newDelegateBridge(british);
    advanceToStep(bridge, "britishCombatMove");
    givenRemotePlayerWillSelectAttackSubs(bridge);
    moveDelegate.setDelegateBridgeAndPlayer(bridge);
    moveDelegate.start();
    final Territory sz9 = territory("9 Sea Zone", gameData);
    final Territory sz7 = territory("7 Sea Zone", gameData);
    final Territory uk = territory("United Kingdom", gameData);
    final Route sz9ToSz7 = new Route(sz9, territory("8 Sea Zone", gameData), sz7);
    // move the transport to attack, this is suicide but valid
    final List<Unit> transports = sz9.getUnitCollection().getMatches(Matches.unitIsTransport());
    move(transports, sz9ToSz7);
    // load the transport
    load(uk.getUnitCollection().getMatches(Matches.unitIsLandTransportable()), new Route(uk, sz7));
    moveDelegate(gameData).end();
    whenGetRandom(bridge).thenAnswer(withValues(0, 1));
    advanceToStep(bridge, "britishBattle");
    final BattleDelegate battleDelegate = battleDelegate(gameData);
    battleDelegate.setDelegateBridgeAndPlayer(bridge);
    assertEquals(2, transports.get(0).getTransporting().size());
    battleDelegate.start();
    // battle already fought
    // make sure the infantry die with the transport
    assertTrue(
        sz7.getUnitCollection().getMatches(Matches.unitOwnedBy(british)).isEmpty(),
        sz7.getUnitCollection().toString());
  }

  @Test
  void testCanRetreatIntoEmptyEnemyTerritory() {
    final Territory eastPoland = territory("East Poland", gameData);
    final Territory ukraine = territory("Ukraine", gameData);
    final Territory poland = territory("Poland", gameData);
    // remove all units from east poland
    removeFrom(eastPoland, eastPoland.getUnits());
    final MoveDelegate moveDelegate = moveDelegate(gameData);
    final IDelegateBridge delegateBridge = newDelegateBridge(germans(gameData));
    advanceToStep(delegateBridge, "CombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(delegateBridge);
    moveDelegate.start();
    final Territory bulgaria = territory("Bulgaria Romania", gameData);
    // attack from bulgraia
    move(bulgaria.getUnits(), new Route(bulgaria, ukraine));
    // add an air attack from east poland
    move(
        poland.getUnitCollection().getMatches(Matches.unitIsAir()),
        new Route(poland, eastPoland, ukraine));
    // we should not be able to retreat to east poland!
    // that territory is still owned by the enemy
    final MustFightBattle battle =
        (MustFightBattle) AbstractMoveDelegate.getBattleTracker(gameData).getPendingBattle(ukraine);
    assertFalse(battle.getAttackerRetreatTerritories().contains(eastPoland));
  }

  @Test
  void testCanRetreatIntoBlitzedTerritory() {
    final Territory eastPoland = territory("East Poland", gameData);
    final Territory ukraine = territory("Ukraine", gameData);
    final Territory poland = territory("Poland", gameData);
    // remove all units from east poland
    removeFrom(eastPoland, eastPoland.getUnits());
    final MoveDelegate moveDelegate = moveDelegate(gameData);
    final IDelegateBridge delegateBridge = newDelegateBridge(germans(gameData));
    advanceToStep(delegateBridge, "CombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(delegateBridge);
    moveDelegate.start();
    final Territory bulgaria = territory("Bulgaria Romania", gameData);
    // attack from bulgraia
    move(bulgaria.getUnits(), new Route(bulgaria, ukraine));
    // add a blitz attack
    move(
        poland.getUnitCollection().getMatches(Matches.unitCanBlitz()),
        new Route(poland, eastPoland, ukraine));
    // we should not be able to retreat to east poland!
    // that territory was just conquered
    final MustFightBattle battle =
        (MustFightBattle) AbstractMoveDelegate.getBattleTracker(gameData).getPendingBattle(ukraine);
    assertTrue(battle.getAttackerRetreatTerritories().contains(eastPoland));
  }

  @Test
  void testCantBlitzFactoryOrAa() {
    // Set up territories
    final Territory poland = territory("Poland", gameData);
    final Territory eastPoland = territory("East Poland", gameData);
    final Territory ukraine = territory("Ukraine", gameData);
    // remove all units from east poland
    removeFrom(eastPoland, eastPoland.getUnits());
    // Add a russian factory
    addTo(eastPoland, factory(gameData).create(1, russians(gameData)));
    MoveDelegate moveDelegate = moveDelegate(gameData);
    IDelegateBridge delegateBridge = newDelegateBridge(germans(gameData));
    advanceToStep(delegateBridge, "CombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(delegateBridge);
    moveDelegate.start();
    // add a blitz attack
    String errorResults =
        moveDelegate.move(
            poland.getUnitCollection().getMatches(Matches.unitCanBlitz()),
            new Route(poland, eastPoland, ukraine));
    assertError(errorResults);
    /*
     * Now try with an AA
     */
    // remove all units from east poland
    removeFrom(eastPoland, eastPoland.getUnits());
    // Add a russian factory
    addTo(eastPoland, aaGun(gameData).create(1, russians(gameData)));
    moveDelegate = moveDelegate(gameData);
    delegateBridge = newDelegateBridge(germans(gameData));
    advanceToStep(delegateBridge, "CombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(delegateBridge);
    moveDelegate.start();
    // add a blitz attack
    errorResults =
        moveDelegate.move(
            poland.getUnitCollection().getMatches(Matches.unitCanBlitz()),
            new Route(poland, eastPoland, ukraine));
    assertError(errorResults);
  }

  @Test
  void testMultipleAaInTerritory() {
    // Set up territories
    final Territory poland = territory("Poland", gameData);
    // Add a russian factory
    final Territory germany = territory("Germany", gameData);
    addTo(poland, aaGun(gameData).create(1, germans(gameData)));
    MoveDelegate moveDelegate = moveDelegate(gameData);
    IDelegateBridge delegateBridge = newDelegateBridge(germans(gameData));
    advanceToStep(delegateBridge, "NonCombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(delegateBridge);
    moveDelegate.start();
    final int preCount = germany.getUnitCollection().getUnitCount();
    /*
     * Move one
     */
    String errorResults =
        moveDelegate.move(
            poland.getUnitCollection().getMatches(Matches.unitIsAaForAnything()),
            new Route(poland, germany));
    assertValid(errorResults);
    assertEquals(germany.getUnitCollection().getUnitCount(), preCount + 1);
    /*
     * Test unloading TRN
     */
    final Territory finland = territory("Finland", gameData);
    final Territory sz5 = territory("5 Sea Zone", gameData);
    addTo(finland, aaGun(gameData).create(1, germans(gameData)));
    moveDelegate = moveDelegate(gameData);
    delegateBridge = newDelegateBridge(germans(gameData));
    advanceToStep(delegateBridge, "NonCombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(delegateBridge);
    moveDelegate.start();
    // load the trn
    load(
        finland.getUnitCollection().getMatches(Matches.unitIsAaForAnything()),
        new Route(finland, sz5));
    // unload the trn
    errorResults =
        moveDelegate.move(
            sz5.getUnitCollection().getMatches(Matches.unitIsAaForAnything()),
            new Route(sz5, germany));
    assertValid(errorResults);
    assertEquals(germany.getUnitCollection().getUnitCount(), preCount + 2);
    /*
     * Test Building one
     */
    final UnitType aaGun = aaGun(gameData);
    final IntegerMap<UnitType> map = new IntegerMap<>();
    map.add(aaGun, 1);
    // Set up the test
    final GamePlayer germans = germans(gameData);
    delegateBridge = newDelegateBridge(germans);
    final PlaceDelegate placeDelegate = placeDelegate(gameData);
    advanceToStep(delegateBridge, "Place");
    placeDelegate.setDelegateBridgeAndPlayer(delegateBridge);
    placeDelegate.start();
    addTo(germans, aaGun(gameData).create(1, germans), gameData);
    errorResults =
        placeDelegate.placeUnits(
            GameDataTestUtil.getUnits(map, germans),
            germany,
            IAbstractPlaceDelegate.BidMode.NOT_BID);
    assertValid(errorResults);
    assertEquals(germany.getUnitCollection().getUnitCount(), preCount + 3);
  }

  @Test
  void testMechanizedInfantry() {
    // Set up tech
    final GamePlayer germans = GameDataTestUtil.germans(gameData);
    final IDelegateBridge delegateBridge = newDelegateBridge(germans(gameData));
    TechTracker.addAdvance(
        germans,
        delegateBridge,
        TechAdvance.findAdvance(
            TechAdvance.TECH_PROPERTY_MECHANIZED_INFANTRY,
            gameData.getTechnologyFrontier(),
            germans));
    // Set up the move delegate
    final MoveDelegate moveDelegate = moveDelegate(gameData);
    advanceToStep(delegateBridge, "CombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(delegateBridge);
    moveDelegate.start();
    // Set up the territories
    final Territory poland = territory("Poland", gameData);
    final Territory eastPoland = territory("East Poland", gameData);
    final Territory belorussia = territory("Belorussia", gameData);
    // Set up the unit types
    final UnitType infantryType = infantry(gameData);
    // Remove all units from east poland
    removeFrom(eastPoland, eastPoland.getUnits());
    // Get total number of units in territories to start
    final int preCountIntPoland = poland.getUnitCollection().size();
    final int preCountIntBelorussia = belorussia.getUnitCollection().size();
    // Get units
    final Collection<Unit> moveUnits = poland.getUnitCollection().getUnits(infantryType, 3);
    moveUnits.addAll(poland.getUnitCollection().getMatches(Matches.unitCanBlitz()));
    // add a INVALID blitz attack
    final String errorResults =
        moveDelegate.move(moveUnits, new Route(poland, eastPoland, belorussia));
    assertError(errorResults);
    // Fix the number of units
    moveUnits.clear();
    moveUnits.addAll(poland.getUnitCollection().getUnits(infantryType, 2));
    moveUnits.addAll(poland.getUnitCollection().getMatches(Matches.unitCanBlitz()));
    // add a VALID blitz attack
    final String validResults =
        moveDelegate.move(moveUnits, new Route(poland, eastPoland, belorussia));
    assertValid(validResults);
    // Get number of units in territories after move (adjusted for movement)
    final int postCountIntPoland = poland.getUnitCollection().size() + 4;
    final int postCountIntBelorussia = belorussia.getUnitCollection().size() - 4;
    // Compare the number of units before and after
    assertEquals(preCountIntPoland, postCountIntPoland);
    assertEquals(preCountIntBelorussia, postCountIntBelorussia);
  }

  @Test
  void testJetPower() {
    // Set up tech
    final GamePlayer germans = GameDataTestUtil.germans(gameData);
    final IDelegateBridge delegateBridge = newDelegateBridge(germans(gameData));
    TechTracker.addAdvance(
        germans,
        delegateBridge,
        TechAdvance.findAdvance(
            TechAdvance.TECH_PROPERTY_JET_POWER, gameData.getTechnologyFrontier(), germans));
    // Set up the territories
    final Territory poland = territory("Poland", gameData);
    final Territory eastPoland = territory("East Poland", gameData);
    // Set up the unit types
    final UnitType fighterType = fighter(gameData);
    advanceToStep(delegateBridge, "germanBattle");
    while (!gameData.getSequence().getStep().getName().equals("germanBattle")) {
      gameData.getSequence().next();
    }
    final Collection<TerritoryEffect> territoryEffects =
        TerritoryEffectHelper.getEffects(eastPoland);
    final List<Unit> germanFighter =
        (List<Unit>) poland.getUnitCollection().getUnits(fighterType, 1);
    whenGetRandom(delegateBridge)
        .thenAnswer(withValues(3)) // With JET_POWER attacking fighter hits on 4 (0 base)
        .thenAnswer(withValues(4)); // With JET_POWER defending fighter misses on 5 (0 base)
    // Attacking fighter
    final DiceRoll roll1 =
        RollDiceFactory.rollBattleDice(
            germanFighter,
            germans,
            delegateBridge,
            "",
            CombatValueBuilder.mainCombatValue()
                .enemyUnits(List.of())
                .friendlyUnits(germanFighter)
                .side(BattleState.Side.OFFENSE)
                .gameSequence(delegateBridge.getData().getSequence())
                .supportAttachments(delegateBridge.getData().getUnitTypeList().getSupportRules())
                .lhtrHeavyBombers(
                    Properties.getLhtrHeavyBombers(delegateBridge.getData().getProperties()))
                .gameDiceSides(delegateBridge.getData().getDiceSides())
                .territoryEffects(territoryEffects)
                .build());
    assertEquals(1, roll1.getHits());
    // Defending fighter
    final DiceRoll roll2 =
        RollDiceFactory.rollBattleDice(
            germanFighter,
            germans,
            delegateBridge,
            "",
            CombatValueBuilder.mainCombatValue()
                .enemyUnits(List.of())
                .friendlyUnits(germanFighter)
                .side(BattleState.Side.DEFENSE)
                .gameSequence(delegateBridge.getData().getSequence())
                .supportAttachments(delegateBridge.getData().getUnitTypeList().getSupportRules())
                .lhtrHeavyBombers(
                    Properties.getLhtrHeavyBombers(delegateBridge.getData().getProperties()))
                .gameDiceSides(delegateBridge.getData().getDiceSides())
                .territoryEffects(territoryEffects)
                .build());
    assertEquals(0, roll2.getHits());
  }

  @Test
  void testBidPlace() {
    final IDelegateBridge bridge = newDelegateBridge(british(gameData));
    advanceToStep(bridge, "BidPlace");
    bidPlaceDelegate(gameData).setDelegateBridgeAndPlayer(bridge);
    bidPlaceDelegate(gameData).start();
    // create 20 british infantry
    addTo(british(gameData), infantry(gameData).create(20, british(gameData)), gameData);
    final Territory uk = territory("United Kingdom", gameData);
    final Collection<Unit> units = british(gameData).getUnits();
    final PlaceableUnits placeable = bidPlaceDelegate(gameData).getPlaceableUnits(units, uk);
    assertEquals(20, placeable.getMaxUnits());
    assertNull(placeable.getErrorMessage());
    final String error = bidPlaceDelegate(gameData).placeUnits(units, uk);
    assertNull(error);
  }

  @Test
  void testFactoryPlace() {
    // Set up game
    final GamePlayer british = GameDataTestUtil.british(gameData);
    final IDelegateBridge delegateBridge = newDelegateBridge(british(gameData));
    // Set up the territories
    final Territory egypt = territory("Union of South Africa", gameData);
    // Set up the unit types
    final UnitType factoryType = factory(gameData);
    // Set up the move delegate
    final PlaceDelegate placeDelegate = placeDelegate(gameData);
    advanceToStep(delegateBridge, "Place");
    placeDelegate.setDelegateBridgeAndPlayer(delegateBridge);
    placeDelegate.start();
    // Add the factory
    final IntegerMap<UnitType> map = new IntegerMap<>();
    map.add(factoryType, 1);
    addTo(british(gameData), factory(gameData).create(1, british(gameData)), gameData);
    // Place the factory
    final String response =
        placeDelegate.placeUnits(GameDataTestUtil.getUnits(map, british), egypt);
    assertValid(response);
    // placeUnits performPlace
    // get production and unit production values
    final TerritoryAttachment ta = TerritoryAttachment.get(egypt);
    assertEquals(ta.getUnitProduction(), ta.getProduction());
  }

  @Test
  void testChinesePlacement() {
    /*
     * This tests that Chinese can place units in any territory, that they can
     * place in just conquered territories, and that they can place in territories
     * with up to 3 Chinese units in them.
     */
    // Set up game
    final GamePlayer chinese = chinese(gameData);
    final IDelegateBridge delegateBridge = newDelegateBridge(chinese);
    advanceToStep(delegateBridge, "CombatMove");
    final MoveDelegate moveDelegate = moveDelegate(gameData);
    moveDelegate.setDelegateBridgeAndPlayer(delegateBridge);
    moveDelegate.start();
    // Set up the territories
    final Territory yunnan = territory("Yunnan", gameData);
    final Territory kiangsu = territory("Kiangsu", gameData);
    final Territory hupeh = territory("Hupeh", gameData);
    // Set up the unit types
    final UnitType infantryType = infantry(gameData);
    // Remove all units
    removeFrom(kiangsu, kiangsu.getUnits());
    // add a VALID attack
    final Collection<Unit> moveUnits = hupeh.getUnits();
    final String validResults = moveDelegate.move(moveUnits, new Route(hupeh, kiangsu));
    assertValid(validResults);
    /*
     * Place units in just captured territory
     */
    final PlaceDelegate placeDelegate = placeDelegate(gameData);
    advanceToStep(delegateBridge, "Place");
    placeDelegate.setDelegateBridgeAndPlayer(delegateBridge);
    placeDelegate.start();
    // Add the infantry
    IntegerMap<UnitType> map = new IntegerMap<>();
    map.add(infantryType, 3);
    addTo(chinese(gameData), infantry(gameData).create(1, chinese(gameData)), gameData);
    // Get the number of units before placing
    int preCount = kiangsu.getUnitCollection().getUnitCount();
    // Place the infantry
    String response =
        placeDelegate.placeUnits(
            GameDataTestUtil.getUnits(map, chinese),
            kiangsu,
            IAbstractPlaceDelegate.BidMode.NOT_BID);
    assertValid(response);
    assertEquals(preCount + 1, kiangsu.getUnitCollection().getUnitCount());
    /*
     * Place units in a territory with up to 3 Chinese units
     */
    // Add the infantry
    map = new IntegerMap<>();
    map.add(infantryType, 3);
    addTo(chinese(gameData), infantry(gameData).create(3, chinese(gameData)), gameData);
    // Get the number of units before placing
    preCount = yunnan.getUnitCollection().getUnitCount();
    // Place the infantry
    response =
        placeDelegate.placeUnits(
            GameDataTestUtil.getUnits(map, chinese),
            yunnan,
            IAbstractPlaceDelegate.BidMode.NOT_BID);
    assertValid(response);
    final int midCount = yunnan.getUnitCollection().getUnitCount();
    // Make sure they were all placed
    assertEquals(preCount, midCount - 3);
    /*
     * Place units in a territory with 3 or more Chinese units
     */
    map = new IntegerMap<>();
    map.add(infantryType, 1);
    addTo(chinese(gameData), infantry(gameData).create(1, chinese(gameData)), gameData);
    response =
        placeDelegate.placeUnits(
            GameDataTestUtil.getUnits(map, chinese),
            yunnan,
            IAbstractPlaceDelegate.BidMode.NOT_BID);
    assertError(response);
    // Make sure none were placed
    final int postCount = yunnan.getUnitCollection().getUnitCount();
    assertEquals(midCount, postCount);
  }

  @Test
  void testPlaceInOccupiedSeaZone() {
    // Set up game
    final GamePlayer germans = GameDataTestUtil.germans(gameData);
    final IDelegateBridge delegateBridge = newDelegateBridge(germans);
    // Clear all units from the SZ and add an enemy unit
    final Territory sz5 = territory("5 Sea Zone", gameData);
    removeFrom(sz5, sz5.getUnits());
    addTo(sz5, destroyer(gameData).create(1, british(gameData)));
    // Set up the unit types
    final UnitType transportType = transport(gameData);
    // Set up the move delegate
    final PlaceDelegate placeDelegate = placeDelegate(gameData);
    advanceToStep(delegateBridge, "Place");
    placeDelegate.setDelegateBridgeAndPlayer(delegateBridge);
    placeDelegate.start();
    // Add the transport
    final IntegerMap<UnitType> map = new IntegerMap<>();
    map.add(transportType, 1);
    addTo(germans, transport(gameData).create(1, germans), gameData);
    // Place it
    final String response = placeDelegate.placeUnits(GameDataTestUtil.getUnits(map, germans), sz5);
    assertValid(response);
  }

  @Test
  void testMoveUnitsThroughSubs() {
    final IDelegateBridge bridge = newDelegateBridge(british(gameData));
    advanceToStep(bridge, "britishNonCombatMove");
    moveDelegate(gameData).setDelegateBridgeAndPlayer(bridge);
    moveDelegate(gameData).start();
    final Territory sz6 = territory("6 Sea Zone", gameData);
    final Route route =
        new Route(sz6, territory("7 Sea Zone", gameData), territory("8 Sea Zone", gameData));
    final String error = moveDelegate(gameData).move(sz6.getUnits(), route);
    assertNull(error, error);
  }

  @Test
  void testMoveUnitsThroughTransports() {
    final IDelegateBridge bridge = newDelegateBridge(british(gameData));
    advanceToStep(bridge, "britishCombatMove");
    moveDelegate(gameData).setDelegateBridgeAndPlayer(bridge);
    moveDelegate(gameData).start();
    final Territory sz12 = territory("12 Sea Zone", gameData);
    final Route route =
        new Route(sz12, territory("13 Sea Zone", gameData), territory("14 Sea Zone", gameData));
    final String error = moveDelegate(gameData).move(sz12.getUnits(), route);
    assertNull(error, error);
  }

  @Test
  void testMoveUnitsThroughTransports2() {
    final IDelegateBridge bridge = newDelegateBridge(british(gameData));
    advanceToStep(bridge, "britishNonCombatMove");
    moveDelegate(gameData).setDelegateBridgeAndPlayer(bridge);
    moveDelegate(gameData).start();
    final Territory sz12 = territory("12 Sea Zone", gameData);
    final Territory sz14 = territory("14 Sea Zone", gameData);
    removeFrom(sz14, sz14.getUnits());
    final Route route = new Route(sz12, territory("13 Sea Zone", gameData), sz14);
    final String error = moveDelegate(gameData).move(sz12.getUnits(), route);
    assertNull(error, error);
  }

  @Test
  void testLoadThroughSubs() {
    final IDelegateBridge bridge = newDelegateBridge(british(gameData));
    advanceToStep(bridge, "britishNonCombatMove");
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
    assertValid(moveDelegate.move(sz8.getUnits(), new Route(sz8, sz7)));
    // load the transport
    load(uk.getUnitCollection().getMatches(Matches.unitIsLandTransportable()), new Route(uk, sz7));
    // move the transport out
    assertValid(
        moveDelegate.move(
            sz7.getUnitCollection().getMatches(Matches.unitOwnedBy(british(gameData))),
            new Route(sz7, sz6)));
  }

  @Test
  void testAttackUndoAndAttackAgain() {
    final MoveDelegate move = moveDelegate(gameData);
    final IDelegateBridge bridge = newDelegateBridge(italians(gameData));
    advanceToStep(bridge, "CombatMove");
    move.setDelegateBridgeAndPlayer(bridge);
    move.start();
    final Territory sz14 = territory("14 Sea Zone", gameData);
    final Territory sz13 = territory("13 Sea Zone", gameData);
    final Territory sz12 = territory("12 Sea Zone", gameData);
    final Route r = new Route(sz14, sz13, sz12);
    // move the battleship
    move(sz14.getUnitCollection().getMatches(Matches.unitHasMoreThanOneHitPointTotal()), r);
    // move everything
    move(sz14.getUnitCollection().getMatches(Matches.unitIsNotTransport()), r);
    // undo it
    move.undoMove(1);
    // move again
    move(sz14.getUnitCollection().getMatches(Matches.unitIsNotTransport()), r);
    final MustFightBattle mfb =
        (MustFightBattle) AbstractMoveDelegate.getBattleTracker(gameData).getPendingBattle(sz12);
    // only 3 attacking units
    // the battleship and the two cruisers
    assertEquals(3, mfb.getAttackingUnits().size());
  }

  @Test
  void testAttackUndoAndBattlesCleared() {
    final MoveDelegate move = moveDelegate(gameData);
    final IDelegateBridge bridge = newDelegateBridge(germans(gameData));
    advanceToStep(bridge, "CombatMove");
    move.setDelegateBridgeAndPlayer(bridge);
    move.start();
    final Territory libya = territory("Libya", gameData);
    final Territory egypt = territory("Egypt", gameData);
    final Territory sudan = territory("Anglo-Egypt Sudan", gameData);
    final Route r = new Route(libya, egypt, sudan);
    egypt.getUnitCollection().clear();
    // blitz tank
    move(libya.getUnitCollection().getMatches(Matches.unitCanBlitz()), r);
    // undo it
    move.undoMove(0);
    // verify both blitz battles were cleared
    assertTrue(AbstractMoveDelegate.getBattleTracker(gameData).getPendingBattleSites().isEmpty());
  }

  @Test
  void testAttackSubsOnSubs() {
    final String defender = "Germans";
    final GamePlayer defenderPlayer = germans(gameData);
    final String attacker = "British";
    final GamePlayer attackerPlayer = british(gameData);
    final Territory attacked = territory("31 Sea Zone", gameData);
    final Territory from = territory("32 Sea Zone", gameData);
    // 1 sub attacks 1 sub
    addTo(from, submarine(gameData).create(1, attackerPlayer));
    addTo(attacked, submarine(gameData).create(1, defenderPlayer));
    final IDelegateBridge bridge = newDelegateBridge(attackerPlayer);
    advanceToStep(bridge, "CombatMove");
    moveDelegate(gameData).setDelegateBridgeAndPlayer(bridge);
    moveDelegate(gameData).start();
    move(from.getUnits(), new Route(from, attacked));
    moveDelegate(gameData).end();
    final MustFightBattle battle =
        (MustFightBattle)
            AbstractMoveDelegate.getBattleTracker(gameData).getPendingBattle(attacked);
    final List<String> steps = battle.determineStepStrings();
    assertEquals(
        BattleStepsTest.mergeSteps(
                List.of(attacker + SUBS_SUBMERGE, defender + SUBS_SUBMERGE),
                BattleStepsTest.firstStrikeFightStepStrings(attackerPlayer, defenderPlayer),
                BattleStepsTest.firstStrikeFightStepStrings(defenderPlayer, attackerPlayer),
                List.of(
                    REMOVE_SNEAK_ATTACK_CASUALTIES,
                    REMOVE_CASUALTIES,
                    attacker + ATTACKER_WITHDRAW))
            .toString(),
        steps.toString());
    // fight, each sub should fire
    // and hit
    whenGetRandom(bridge).thenAnswer(withValues(0)).thenAnswer(withValues(0));
    battle.fight(bridge);
    thenGetRandomShouldHaveBeenCalled(bridge, times(2));
    assertTrue(attacked.getUnitCollection().isEmpty());
  }

  @Test
  void testAttackSubsOnDestroyer() {
    final String defender = "Germans";
    final GamePlayer defenderPlayer = germans(gameData);
    final String attacker = "British";
    final GamePlayer attackerPlayer = british(gameData);
    final Territory attacked = territory("31 Sea Zone", gameData);
    final Territory from = territory("32 Sea Zone", gameData);
    // 1 sub attacks 1 sub and 1 destroyer
    // defender sneak attacks, not attacker
    addTo(from, submarine(gameData).create(1, attackerPlayer));
    addTo(attacked, submarine(gameData).create(1, defenderPlayer));
    addTo(attacked, destroyer(gameData).create(1, defenderPlayer));
    final IDelegateBridge bridge = newDelegateBridge(attackerPlayer);
    advanceToStep(bridge, "CombatMove");
    moveDelegate(gameData).setDelegateBridgeAndPlayer(bridge);
    moveDelegate(gameData).start();
    move(from.getUnits(), new Route(from, attacked));
    moveDelegate(gameData).end();
    final MustFightBattle battle =
        (MustFightBattle)
            AbstractMoveDelegate.getBattleTracker(gameData).getPendingBattle(attacked);
    final List<String> steps = battle.determineStepStrings();
    assertEquals(
        BattleStepsTest.mergeSteps(
                List.of(attacker + SUBS_SUBMERGE),
                List.of(defender + SUBS_SUBMERGE),
                BattleStepsTest.firstStrikeFightStepStrings(defenderPlayer, attackerPlayer),
                List.of(REMOVE_SNEAK_ATTACK_CASUALTIES),
                BattleStepsTest.firstStrikeFightStepStrings(attackerPlayer, defenderPlayer),
                BattleStepsTest.generalFightStepStrings(defenderPlayer, attackerPlayer),
                List.of(REMOVE_CASUALTIES, attacker + ATTACKER_WITHDRAW))
            .toString(),
        steps.toString());
    // defending subs sneak attack and hit
    // no chance to return fire
    whenGetRandom(bridge).thenAnswer(withValues(0));
    battle.fight(bridge);
    thenGetRandomShouldHaveBeenCalled(bridge, times(1));
    assertTrue(
        attacked
            .getUnitCollection()
            .getMatches(Matches.unitIsOwnedBy(british(gameData)))
            .isEmpty());
    assertEquals(2, attacked.getUnitCollection().size());
  }

  @Test
  void testAttackDestroyerAndSubsAgainstSub() {
    final GamePlayer defenderPlayer = germans(gameData);
    final String attacker = "British";
    final String defender = "Germans";
    final GamePlayer attackerPlayer = british(gameData);
    final Territory attacked = territory("31 Sea Zone", gameData);
    final Territory from = territory("32 Sea Zone", gameData);
    // 1 sub and 1 destroyer attack 1 sub
    // defender sneak attacks, not attacker
    addTo(from, submarine(gameData).create(1, attackerPlayer));
    addTo(from, destroyer(gameData).create(1, attackerPlayer));
    addTo(attacked, submarine(gameData).create(1, defenderPlayer));
    final IDelegateBridge bridge = newDelegateBridge(attackerPlayer);
    advanceToStep(bridge, "CombatMove");
    moveDelegate(gameData).setDelegateBridgeAndPlayer(bridge);
    moveDelegate(gameData).start();
    move(from.getUnits(), new Route(from, attacked));
    moveDelegate(gameData).end();
    final MustFightBattle battle =
        (MustFightBattle)
            AbstractMoveDelegate.getBattleTracker(gameData).getPendingBattle(attacked);
    final List<String> steps = battle.determineStepStrings();
    assertEquals(
        BattleStepsTest.mergeSteps(
                List.of(attacker + SUBS_SUBMERGE),
                List.of(defender + SUBS_SUBMERGE),
                BattleStepsTest.firstStrikeFightStepStrings(attackerPlayer, defenderPlayer),
                List.of(REMOVE_SNEAK_ATTACK_CASUALTIES),
                BattleStepsTest.generalFightStepStrings(attackerPlayer, defenderPlayer),
                BattleStepsTest.firstStrikeFightStepStrings(defenderPlayer, attackerPlayer),
                List.of(REMOVE_CASUALTIES, attacker + ATTACKER_WITHDRAW))
            .toString(),
        steps.toString());
    // attacking subs sneak attack and hit
    // no chance to return fire
    whenGetRandom(bridge).thenAnswer(withValues(0));
    battle.fight(bridge);
    thenGetRandomShouldHaveBeenCalled(bridge, times(1));
    assertTrue(
        attacked
            .getUnitCollection()
            .getMatches(Matches.unitIsOwnedBy(germans(gameData)))
            .isEmpty());
    assertEquals(2, attacked.getUnitCollection().size());
  }

  @Test
  void testAttackDestroyerAndSubsAgainstSubAndDestroyer() {
    final GamePlayer defenderPlayer = germans(gameData);
    final String attacker = "British";
    final String defender = "Germans";
    final GamePlayer attackerPlayer = british(gameData);
    final Territory attacked = territory("31 Sea Zone", gameData);
    final Territory from = territory("32 Sea Zone", gameData);
    // 1 sub and 1 destroyer attack 1 sub and 1 destroyer
    // no sneak attacks
    addTo(from, submarine(gameData).create(1, attackerPlayer));
    addTo(from, destroyer(gameData).create(1, attackerPlayer));
    addTo(attacked, submarine(gameData).create(1, defenderPlayer));
    addTo(attacked, destroyer(gameData).create(1, defenderPlayer));
    final IDelegateBridge bridge = newDelegateBridge(attackerPlayer);
    advanceToStep(bridge, "CombatMove");
    moveDelegate(gameData).setDelegateBridgeAndPlayer(bridge);
    moveDelegate(gameData).start();
    move(from.getUnits(), new Route(from, attacked));
    moveDelegate(gameData).end();
    final MustFightBattle battle =
        (MustFightBattle)
            AbstractMoveDelegate.getBattleTracker(gameData).getPendingBattle(attacked);
    final List<String> steps = battle.determineStepStrings();
    assertEquals(
        BattleStepsTest.mergeSteps(
                List.of(attacker + SUBS_SUBMERGE),
                List.of(defender + SUBS_SUBMERGE),
                BattleStepsTest.firstStrikeFightStepStrings(attackerPlayer, defenderPlayer),
                BattleStepsTest.generalFightStepStrings(attackerPlayer, defenderPlayer),
                BattleStepsTest.firstStrikeFightStepStrings(defenderPlayer, attackerPlayer),
                BattleStepsTest.generalFightStepStrings(defenderPlayer, attackerPlayer),
                List.of(REMOVE_CASUALTIES, attacker + ATTACKER_WITHDRAW))
            .toString(),
        steps.toString());
    givenRemotePlayerWillSelectCasualtiesPer(
        bridge,
        invocation -> {
          final Collection<Unit> selectFrom = invocation.getArgument(0);
          return new CasualtyDetails(
              List.of(selectFrom.iterator().next()), new ArrayList<>(), false);
        });
    // attacking subs sneak attack and hit
    // no chance to return fire
    whenGetRandom(bridge)
        .thenAnswer(withValues(0))
        .thenAnswer(withValues(0))
        .thenAnswer(withValues(0))
        .thenAnswer(withValues(0));
    battle.fight(bridge);
    thenGetRandomShouldHaveBeenCalled(bridge, times(4));
    assertEquals(0, attacked.getUnitCollection().size());
  }

  @Test
  void testLimitBombardtoNumberOfUnloaded() {
    final MoveDelegate move = moveDelegate(gameData);
    final IDelegateBridge bridge = newDelegateBridge(italians(gameData));
    givenRemotePlayerWillSelectShoreBombard(bridge);
    advanceToStep(bridge, "CombatMove");
    move.setDelegateBridgeAndPlayer(bridge);
    move.start();
    final Territory sz14 = territory("14 Sea Zone", gameData);
    final Territory sz15 = territory("15 Sea Zone", gameData);
    final Territory eg = territory("Egypt", gameData);
    final Territory li = territory("Libya", gameData);
    final Territory balkans = territory("Balkans", gameData);
    // Clear all units from the attacked terr
    removeFrom(eg, eg.getUnits());
    // Add 2 inf
    final GamePlayer british = GameDataTestUtil.british(gameData);
    addTo(eg, infantry(gameData).create(2, british));
    // load the transports
    load(
        balkans.getUnitCollection().getMatches(Matches.unitIsLandTransportable()),
        new Route(balkans, sz14));
    // move the fleet
    move(sz14.getUnits(), new Route(sz14, sz15));
    // move troops from Libya
    move(
        li.getUnitCollection().getMatches(Matches.unitOwnedBy(italians(gameData))),
        new Route(li, eg));
    // unload the transports
    move(sz15.getUnitCollection().getMatches(Matches.unitIsLand()), new Route(sz15, eg));
    move.end();
    // start the battle phase, this will ask the user to bombard
    battleDelegate(gameData).setDelegateBridgeAndPlayer(bridge);
    BattleDelegate.doInitialize(battleDelegate(gameData).getBattleTracker(), bridge);
    battleDelegate(gameData).addBombardmentSources();
    final MustFightBattle mfb =
        (MustFightBattle) AbstractMoveDelegate.getBattleTracker(gameData).getPendingBattle(eg);
    // only 2 ships are allowed to bombard (there are 1 battleship and 2 cruisers that COULD
    // bombard, but only 2 ships
    // may bombard total)
    assertEquals(2, mfb.getBombardingUnits().size());
    // Show that bombard casualties can return fire
    // Note- the 3 & 2 hits below show default behavior of bombarding at attack strength
    // 2= Battleship hitting a 3, 2=Cruiser hitting a 3, 15=British infantry hitting once
    whenGetRandom(bridge).thenAnswer(withValues(2, 2)).thenAnswer(withValues(1, 5));
    battleDelegate(gameData).setDelegateBridgeAndPlayer(bridge);
    battleDelegate(gameData).start();
    // end result should be 2 italian infantry.
    assertEquals(3, eg.getUnitCollection().size());
  }

  @Test
  void testBombardStrengthVariable() {
    final MoveDelegate move = moveDelegate(gameData);
    final IDelegateBridge bridge = newDelegateBridge(italians(gameData));
    givenRemotePlayerWillSelectShoreBombard(bridge);
    advanceToStep(bridge, "CombatMove");
    move.setDelegateBridgeAndPlayer(bridge);
    move.start();
    final Territory sz14 = territory("14 Sea Zone", gameData);
    final Territory sz15 = territory("15 Sea Zone", gameData);
    final Territory eg = territory("Egypt", gameData);
    final Territory balkans = territory("Balkans", gameData);
    // Clear all units
    removeFrom(eg, eg.getUnits());
    removeFrom(sz14, sz14.getUnits());
    // Add 2 inf to the attacked terr
    final GamePlayer british = GameDataTestUtil.british(gameData);
    addTo(eg, infantry(gameData).create(2, british));
    // create/load the destroyers and transports
    final GamePlayer italians = italians(gameData);
    addTo(sz14, transport(gameData).create(1, italians));
    addTo(sz14, destroyer(gameData).create(2, italians));
    // load the transports
    load(
        balkans.getUnitCollection().getMatches(Matches.unitIsLandTransportable()),
        new Route(balkans, sz14));
    // move the fleet
    move(sz14.getUnits(), new Route(sz14, sz15));
    // unload the transports
    move(sz15.getUnitCollection().getMatches(Matches.unitIsLand()), new Route(sz15, eg));
    move.end();
    // Set the tech for DDs bombard
    // ww2v3 doesn't have this tech, so this does nothing...
    // TechAttachment.get(italians).setDestroyerBombard("true");
    UnitAttachment.get(destroyer(gameData)).setCanBombard("true");
    // Set the bombard strength for the DDs
    final Collection<Unit> dds =
        CollectionUtils.getMatches(sz15.getUnits(), Matches.unitIsDestroyer());
    for (final Unit unit : dds) {
      final UnitAttachment ua = UnitAttachment.get(unit.getType());
      ua.setBombard(3);
    }
    // start the battle phase, this will ask the user to bombard
    battleDelegate(gameData).setDelegateBridgeAndPlayer(bridge);
    BattleDelegate.doInitialize(battleDelegate(gameData).getBattleTracker(), bridge);
    battleDelegate(gameData).addBombardmentSources();
    final MustFightBattle mfb =
        (MustFightBattle) AbstractMoveDelegate.getBattleTracker(gameData).getPendingBattle(eg);
    assertNotNull(mfb);
    // Show that bombard casualties can return fire
    // destroyer bombard hit/miss on rolls of 4 & 3
    // landing inf miss
    // defending inf hit
    whenGetRandom(bridge)
        .thenAnswer(withValues(3, 2))
        .thenAnswer(withValues(6, 6))
        .thenAnswer(withValues(1, 1));
    battleDelegate(gameData).setDelegateBridgeAndPlayer(bridge);
    BattleDelegate.doInitialize(battleDelegate(gameData).getBattleTracker(), bridge);
    battleDelegate(gameData).addBombardmentSources();
    fight(battleDelegate(gameData), eg);
    // 1 defending inf remaining
    assertEquals(1, eg.getUnitCollection().size());
  }

  @Test
  void testAmphAttackUndoAndAttackAgainBombard() {
    final MoveDelegate move = moveDelegate(gameData);
    final IDelegateBridge bridge = newDelegateBridge(italians(gameData));
    givenRemotePlayerWillSelectShoreBombard(bridge);
    advanceToStep(bridge, "CombatMove");
    move.setDelegateBridgeAndPlayer(bridge);
    move.start();
    final Territory sz14 = territory("14 Sea Zone", gameData);
    final Territory sz15 = territory("15 Sea Zone", gameData);
    final Territory eg = territory("Egypt", gameData);
    final Territory li = territory("Libya", gameData);
    final Territory balkans = territory("Balkans", gameData);
    // load the transports
    load(
        balkans.getUnitCollection().getMatches(Matches.unitIsLandTransportable()),
        new Route(balkans, sz14));
    // move the fleet
    move(sz14.getUnits(), new Route(sz14, sz15));
    // move troops from Libya
    move(
        li.getUnitCollection().getMatches(Matches.unitOwnedBy(italians(gameData))),
        new Route(li, eg));
    // unload the transports
    move(sz15.getUnitCollection().getMatches(Matches.unitIsLand()), new Route(sz15, eg));
    // undo amphibious landing
    move.undoMove(move.getMovesMade().size() - 1);
    // move again
    move(sz15.getUnitCollection().getMatches(Matches.unitIsLand()), new Route(sz15, eg));
    move.end();
    // start the battle phase, this will ask the user to bombard
    battleDelegate(gameData).setDelegateBridgeAndPlayer(bridge);
    BattleDelegate.doInitialize(battleDelegate(gameData).getBattleTracker(), bridge);
    battleDelegate(gameData).addBombardmentSources();
    final MustFightBattle mfb =
        (MustFightBattle) AbstractMoveDelegate.getBattleTracker(gameData).getPendingBattle(eg);
    // only 2 battleships are allowed to bombard
    assertEquals(2, mfb.getBombardingUnits().size());
  }

  @Test
  void testCarrierWithAlliedPlanes() {
    final Territory sz8 = territory("8 Sea Zone", gameData);
    final Territory sz1 = territory("1 Sea Zone", gameData);
    addTo(sz8, carrier(gameData).create(1, british(gameData)));
    addTo(sz8, fighter(gameData).create(1, americans(gameData)));
    final Route route = new Route(sz8, sz1);
    final IDelegateBridge bridge = newDelegateBridge(british(gameData));
    advanceToStep(bridge, "CombatMove");
    moveDelegate(gameData).setDelegateBridgeAndPlayer(bridge);
    moveDelegate(gameData).start();
    move(sz8.getUnits(), route);
    // make sure the fighter moved
    assertTrue(sz8.getUnits().isEmpty());
    assertFalse(sz1.getUnitCollection().getMatches(Matches.unitIsAir()).isEmpty());
  }

  @Test
  void testAirCanLandWithAlliedFighters() {
    // germany owns madagascar, with 2 fighters in it
    // also 1 carrier, and 1 allied fighter in sz 40
    // the fighters should not be able to move from madagascar
    // to sz 40, since with the allied fighter, there is no room on the carrier
    final Territory madagascar = territory("French Madagascar", gameData);
    final GamePlayer germans = germans(gameData);
    madagascar.setOwner(germans);
    final Territory sz40 = territory("40 Sea Zone", gameData);
    addTo(sz40, carrier(gameData).create(1, germans));
    addTo(sz40, fighter(gameData).create(1, italians(gameData)));
    addTo(madagascar, fighter(gameData).create(2, germans));
    final Route route = gameData.getMap().getRoute(madagascar, sz40, it -> true);
    final IDelegateBridge bridge = newDelegateBridge(germans);
    advanceToStep(bridge, "CombatMove");
    moveDelegate(gameData).setDelegateBridgeAndPlayer(bridge);
    moveDelegate(gameData).start();
    // don't allow kamikaze
    final String error = moveDelegate(gameData).move(madagascar.getUnits(), route);
    assertError(error);
  }

  @Test
  void testMechInfSimple() {
    final GamePlayer germans = germans(gameData);
    final Territory france = territory("France", gameData);
    final Territory germany = territory("Germany", gameData);
    final Territory poland = territory("Poland", gameData);
    TechAttachment.get(germans).setMechanizedInfantry("true");
    final IDelegateBridge bridge = newDelegateBridge(germans);
    advanceToStep(bridge, "CombatMove");
    moveDelegate(gameData).setDelegateBridgeAndPlayer(bridge);
    moveDelegate(gameData).start();
    final Route r = new Route(france, germany, poland);
    // 1 armour and 1 infantry
    final List<Unit> toMove =
        new ArrayList<>(france.getUnitCollection().getMatches(Matches.unitCanBlitz()));
    toMove.add(france.getUnitCollection().getMatches(Matches.unitIsLandTransportable()).get(0));
    move(toMove, r);
  }

  @Test
  void testMechInfUnitAlreadyMovedSimple() {
    final GamePlayer germans = germans(gameData);
    final Territory france = territory("France", gameData);
    final Territory germany = territory("Germany", gameData);
    TechAttachment.get(germans).setMechanizedInfantry("true");
    final IDelegateBridge bridge = newDelegateBridge(germans);
    advanceToStep(bridge, "CombatMove");
    moveDelegate(gameData).setDelegateBridgeAndPlayer(bridge);
    moveDelegate(gameData).start();
    // get rid of the infantry in france
    removeFrom(france, france.getUnitCollection().getMatches(Matches.unitIsLandTransportable()));
    // move an infantry from germany to france
    move(
        germany.getUnitCollection().getMatches(Matches.unitIsLandTransportable()).subList(0, 1),
        new Route(germany, france));
    // try to move all the units in france, the infantry should not be able to move
    final Route r = new Route(france, germany);
    final String error = moveDelegate(gameData).move(france.getUnits(), r);
    assertNotNull(error);
  }

  @Test
  void testParatroopsWalkOnWater() {
    final GamePlayer germans = germans(gameData);
    final Territory france = territory("France", gameData);
    TechAttachment.get(germans).setParatroopers("true");
    final Route r = new Route(france, territory("7 Sea Zone", gameData));
    final Collection<Unit> paratroopers =
        france.getUnitCollection().getMatches(Matches.unitIsAirTransportable());
    assertFalse(paratroopers.isEmpty());
    final MoveValidationResult results =
        new MoveValidator(gameData)
            .validateMove(new MoveDescription(paratroopers, r), germans, false, null);
    assertFalse(results.isMoveValid());
  }

  @Test
  void testBomberWithTankOverWaterParatroopers() {
    final GamePlayer germans = germans(gameData);
    final IDelegateBridge bridge = newDelegateBridge(germans);
    advanceToStep(bridge, "CombatMove");
    moveDelegate(gameData).setDelegateBridgeAndPlayer(bridge);
    moveDelegate(gameData).start();
    TechAttachment.get(germans).setParatroopers("true");
    final Territory sz5 = territory("5 Sea Zone", gameData);
    final Territory germany = territory("Germany", gameData);
    final Territory karelia = territory("Karelia S.S.R.", gameData);
    addTo(germany, armour(gameData).create(1, germans));
    final Route r = new Route(germany, sz5, karelia);
    final Collection<Unit> toMove = germany.getUnitCollection().getMatches(Matches.unitCanBlitz());
    toMove.addAll(germany.getUnitCollection().getMatches(Matches.unitIsStrategicBomber()));
    assertEquals(2, toMove.size());
    final MoveValidationResult results =
        new MoveValidator(gameData)
            .validateMove(new MoveDescription(toMove, r), germans, false, null);
    assertFalse(results.isMoveValid());
  }

  @Test
  void testBomberTankOverWater() {
    // can't transport a tank over water using a bomber
    final GamePlayer germans = germans(gameData);
    final IDelegateBridge bridge = newDelegateBridge(germans);
    advanceToStep(bridge, "CombatMove");
    moveDelegate(gameData).setDelegateBridgeAndPlayer(bridge);
    moveDelegate(gameData).start();
    final Territory sz5 = territory("5 Sea Zone", gameData);
    final Territory germany = territory("Germany", gameData);
    final Territory karelia = territory("Karelia S.S.R.", gameData);
    addTo(germany, armour(gameData).create(1, germans));
    final Route r = new Route(germany, sz5, karelia);
    final Collection<Unit> toMove = germany.getUnitCollection().getMatches(Matches.unitCanBlitz());
    toMove.addAll(germany.getUnitCollection().getMatches(Matches.unitIsStrategicBomber()));
    assertEquals(2, toMove.size());
    final MoveValidationResult results =
        new MoveValidator(gameData)
            .validateMove(new MoveDescription(toMove, r), germans, false, null);
    assertFalse(results.isMoveValid());
  }

  @Test
  void testMoveParatroopersAsNonPartroops() {
    // move a bomber and a paratrooper
    // one step, but as a normal movement
    final GamePlayer germans = germans(gameData);
    final Territory germany = territory("Germany", gameData);
    final Territory nwe = territory("Northwestern Europe", gameData);
    final IDelegateBridge bridge = newDelegateBridge(germans);
    advanceToStep(bridge, "CombatMove");
    moveDelegate(gameData).setDelegateBridgeAndPlayer(bridge);
    moveDelegate(gameData).start();
    TechAttachment.get(germans).setParatroopers("true");
    List<Unit> paratrooper =
        germany.getUnitCollection().getMatches(Matches.unitIsAirTransportable());
    paratrooper = paratrooper.subList(0, 1);
    final List<Unit> bomberAndParatroop = new ArrayList<>(paratrooper);
    bomberAndParatroop.addAll(germany.getUnitCollection().getMatches(Matches.unitIsAirTransport()));
    // move to nwe, this is a valid move, and it not a partroop move
    move(bomberAndParatroop, new Route(germany, nwe));
  }

  @Test
  void testCantMoveParatroopersThatMovedPreviously() {
    // make sure infantry can't be moved as paratroopers after moving
    final GamePlayer germans = germans(gameData);
    final Territory germany = territory("Germany", gameData);
    final Territory nwe = territory("Northwestern Europe", gameData);
    final Territory poland = territory("Poland", gameData);
    final Territory eastPoland = territory("East Poland", gameData);
    final IDelegateBridge bridge = newDelegateBridge(germans);
    advanceToStep(bridge, "CombatMove");
    moveDelegate(gameData).setDelegateBridgeAndPlayer(bridge);
    moveDelegate(gameData).start();
    TechAttachment.get(germans).setParatroopers("true");
    final List<Unit> paratrooper =
        nwe.getUnitCollection().getMatches(Matches.unitIsAirTransportable());
    move(paratrooper, new Route(nwe, germany));
    final List<Unit> bomberAndParatroop = new ArrayList<>(paratrooper);
    bomberAndParatroop.addAll(germany.getUnitCollection().getMatches(Matches.unitIsAirTransport()));
    // move the units to east poland
    final String error =
        moveDelegate(gameData).move(bomberAndParatroop, new Route(germany, poland, eastPoland));
    assertError(error);
  }

  @Test
  void testCantTransportParatroopersWithBombersThatMovedPreviously() {
    // make sure bombers can't move then pick up paratroopers
    final GamePlayer germans = germans(gameData);
    final Territory germany = territory("Germany", gameData);
    final Territory bulgaria = territory("Bulgaria Romania", gameData);
    final Territory poland = territory("Poland", gameData);
    final Territory ukraine = territory("Ukraine", gameData);
    final IDelegateBridge bridge = newDelegateBridge(germans);
    advanceToStep(bridge, "CombatMove");
    moveDelegate(gameData).setDelegateBridgeAndPlayer(bridge);
    moveDelegate(gameData).start();
    TechAttachment.get(germans).setParatroopers("true");
    // Move the bomber first
    final List<Unit> bomber = germany.getUnitCollection().getMatches(Matches.unitIsAirTransport());
    move(bomber, new Route(germany, poland));
    // Pick up a paratrooper
    final List<Unit> bomberAndParatroop = new ArrayList<>(bomber);
    bomberAndParatroop.addAll(poland.getUnitCollection().getUnits(infantry(gameData), 1));
    // move them
    final String error =
        moveDelegate(gameData).move(bomberAndParatroop, new Route(poland, bulgaria, ukraine));
    assertError(error);
  }

  @Test
  void testMoveOneParatrooperPerBomber() {
    // make sure only 1 paratroop per bomber can be moved
    final GamePlayer germans = germans(gameData);
    final Territory germany = territory("Germany", gameData);
    // Territory nwe = territory("Northwestern Europe", gameData);
    final Territory poland = territory("Poland", gameData);
    final Territory eastPoland = territory("East Poland", gameData);
    final IDelegateBridge bridge = newDelegateBridge(germans);
    advanceToStep(bridge, "CombatMove");
    moveDelegate(gameData).setDelegateBridgeAndPlayer(bridge);
    moveDelegate(gameData).start();
    TechAttachment.get(germans).setParatroopers("true");
    final List<Unit> bomberAndParatroop = new ArrayList<>();
    bomberAndParatroop.addAll(germany.getUnitCollection().getMatches(Matches.unitIsAirTransport()));
    // add 2 infantry
    bomberAndParatroop.addAll(germany.getUnitCollection().getUnits(infantry(gameData), 2));
    // move the units to east poland
    final String error =
        moveDelegate(gameData).move(bomberAndParatroop, new Route(germany, poland, eastPoland));
    assertError(error);
  }

  @Test
  void testParatroopersMoveTwice() {
    // After a battle move to put a bomber + infantry (paratroop) in a first enemy
    // territory, you can make a new move (in the same battle move round) to put
    // bomber+ infantry in a more internal enemy territory.
    final GamePlayer germans = germans(gameData);
    final Territory germany = territory("Germany", gameData);
    final Territory poland = territory("Poland", gameData);
    final Territory eastPoland = territory("East Poland", gameData);
    final Territory beloRussia = territory("Belorussia", gameData);
    final IDelegateBridge bridge = newDelegateBridge(germans);
    advanceToStep(bridge, "CombatMove");
    moveDelegate(gameData).setDelegateBridgeAndPlayer(bridge);
    moveDelegate(gameData).start();
    TechAttachment.get(germans).setParatroopers("true");
    List<Unit> paratroopers =
        germany.getUnitCollection().getMatches(Matches.unitIsAirTransportable());
    paratroopers = paratroopers.subList(0, 1);
    final List<Unit> bomberAndParatroop = new ArrayList<>(paratroopers);
    bomberAndParatroop.addAll(germany.getUnitCollection().getMatches(Matches.unitIsAirTransport()));
    final Route route = new Route(germany, poland, eastPoland);
    final List<Unit> airTransports =
        germany.getUnitCollection().getMatches(Matches.unitIsAirTransport());
    for (final Unit airTransport : airTransports) {
      for (final Unit unit : paratroopers) {
        final Change change = TransportTracker.loadTransportChange(airTransport, unit);
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
  void testParatroopersFlyOverBlitzedTerritory() {
    // We should be able to blitz a territory, then fly over it with paratroops to battle.
    final GamePlayer germans = germans(gameData);
    final Territory germany = territory("Germany", gameData);
    final Territory poland = territory("Poland", gameData);
    final Territory eastPoland = territory("East Poland", gameData);
    final Territory beloRussia = territory("Belorussia", gameData);
    // Clear East Poland
    removeFrom(eastPoland, eastPoland.getUnits());
    // Set up test
    final IDelegateBridge bridge = newDelegateBridge(germans);
    advanceToStep(bridge, "CombatMove");
    moveDelegate(gameData).setDelegateBridgeAndPlayer(bridge);
    moveDelegate(gameData).start();
    TechAttachment.get(germans).setParatroopers("true");
    List<Unit> paratrooper =
        germany.getUnitCollection().getMatches(Matches.unitIsAirTransportable());
    paratrooper = paratrooper.subList(0, 1);
    final List<Unit> bomberAndParatroop = new ArrayList<>(paratrooper);
    bomberAndParatroop.addAll(germany.getUnitCollection().getMatches(Matches.unitIsAirTransport()));
    final List<Unit> tanks = poland.getUnitCollection().getMatches(Matches.unitCanBlitz());
    move(tanks, new Route(poland, eastPoland, beloRussia));
    final List<Unit> airTransports =
        CollectionUtils.getMatches(bomberAndParatroop, Matches.unitIsAirTransport());
    for (final Unit airTransport : airTransports) {
      for (final Unit unit : paratrooper) {
        final Change change = TransportTracker.loadTransportChange(airTransport, unit);
        bridge.addChange(change);
      }
    }
    // Verify paratroops can overfly blitzed territory
    final String error =
        moveDelegate(gameData)
            .move(bomberAndParatroop, new Route(germany, poland, eastPoland, beloRussia));
    assertValid(error);
  }

  @Test
  void testDefencelessTransportsDie() {
    final GamePlayer british = british(gameData);
    final IDelegateBridge bridge = newDelegateBridge(british);
    advanceToStep(bridge, "CombatMove");
    moveDelegate(gameData).setDelegateBridgeAndPlayer(bridge);
    moveDelegate(gameData).start();
    final Territory uk = territory("United Kingdom", gameData);
    final Territory sz5 = territory("5 Sea Zone", gameData);
    // remove the sub
    removeFrom(sz5, sz5.getUnitCollection().getMatches(Matches.unitCanEvade()));

    move(
        uk.getUnitCollection().getMatches(Matches.unitIsAir()),
        gameData.getMap().getRoute(uk, sz5, it -> true));
    // move units for amphib assault
    moveDelegate(gameData).end();
    advanceToStep(bridge, "Combat");
    // cook the dice so that 1 british fighters hits, and nothing else
    // this will leave 1 transport alone in the sea zone
    whenGetRandom(bridge).thenAnswer(withValues(1, 5, 5)).thenAnswer(withValues(5));
    battleDelegate(gameData).setDelegateBridgeAndPlayer(bridge);
    battleDelegate(gameData).start();
    // make sure the transports died
    assertTrue(
        sz5.getUnitCollection().getMatches(Matches.unitIsOwnedBy(germans(gameData))).isEmpty());
    thenRemotePlayerShouldNotBeAskedToRetreat(bridge);
  }

  @Test
  void testFighterLandsWhereCarrierCanBePlaced() {
    final GamePlayer germans = germans(gameData);
    // germans have 1 carrier to place
    addTo(germans, carrier(gameData).create(1, germans), gameData);
    // start the move phase
    final IDelegateBridge bridge = newDelegateBridge(germans);
    advanceToStep(bridge, "CombatMove");
    moveDelegate(gameData).setDelegateBridgeAndPlayer(bridge);
    moveDelegate(gameData).start();
    // the fighter should be able to move and hover in the sea zone
    // the fighter has no movement left
    final Territory neEurope = territory("Northwestern Europe", gameData);
    final Route route =
        new Route(
            neEurope,
            territory("Germany", gameData),
            territory("Poland", gameData),
            territory("Baltic States", gameData),
            territory("5 Sea Zone", gameData));
    // the fighter should be able to move, and hover in the sea zone until the carrier is placed
    move(neEurope.getUnitCollection().getMatches(Matches.unitIsAir()), route);
  }

  @Test
  void testFighterCantHoverWithNoCarrierToPlace() {
    // start the move phase
    final IDelegateBridge bridge = newDelegateBridge(germans(gameData));
    advanceToStep(bridge, "CombatMove");
    moveDelegate(gameData).setDelegateBridgeAndPlayer(bridge);
    moveDelegate(gameData).start();
    // the fighter should not be able to move and hover in the sea zone
    // since their are no carriers to place
    // the fighter has no movement left
    final Territory neEurope = territory("Northwestern Europe", gameData);
    final Route route =
        new Route(
            neEurope,
            territory("Germany", gameData),
            territory("Poland", gameData),
            territory("Baltic States", gameData),
            territory("5 Sea Zone", gameData));
    final String error =
        moveDelegate(gameData)
            .move(neEurope.getUnitCollection().getMatches(Matches.unitIsAir()), route);
    assertNotNull(error);
  }

  @Test
  void testRepair() {
    final Territory germany = territory("Germany", gameData);
    final Unit factory = germany.getUnitCollection().getMatches(Matches.unitCanBeDamaged()).get(0);
    final PurchaseDelegate del = purchaseDelegate(gameData);
    del.setDelegateBridgeAndPlayer(newDelegateBridge(germans(gameData)));
    del.start();
    // Set up player
    final GamePlayer germans = GameDataTestUtil.germans(gameData);
    final int initPUs = germans.getResources().getQuantity("PUs");
    // damage a factory
    IntegerMap<Unit> startHits = new IntegerMap<>();
    startHits.put(factory, 1);
    gameData.performChange(ChangeFactory.bombingUnitDamage(startHits, List.of(germany)));
    assertEquals(1, factory.getUnitDamage());
    RepairRule repair = germans(gameData).getRepairFrontier().getRules().get(0);
    IntegerMap<RepairRule> repairs = new IntegerMap<>();
    repairs.put(repair, 1);
    String error =
        del.purchaseRepair(
            Map.of(
                CollectionUtils.getMatches(germany.getUnits(), Matches.unitCanBeDamaged())
                    .iterator()
                    .next(),
                repairs));
    assertValid(error);
    assertEquals(0, factory.getUnitDamage());
    // Find cost
    final int midPUs = germans.getResources().getQuantity("PUs");
    assertEquals(initPUs, midPUs + 1);
    /*
     * INCREASED_FACTORY_PRODUCTION repairs
     */
    // Set up INCREASED_FACTORY_PRODUCTION
    final IDelegateBridge delegateBridge = newDelegateBridge(germans(gameData));
    TechTracker.addAdvance(
        germans,
        delegateBridge,
        TechAdvance.findAdvance(
            TechAdvance.TECH_PROPERTY_INCREASED_FACTORY_PRODUCTION,
            gameData.getTechnologyFrontier(),
            germans));
    // damage a factory
    startHits = new IntegerMap<>();
    startHits.put(factory, 2);
    gameData.performChange(ChangeFactory.bombingUnitDamage(startHits, List.of(germany)));
    assertEquals(2, factory.getUnitDamage());
    repair = germans(gameData).getRepairFrontier().getRules().get(0);
    repairs = new IntegerMap<>();
    repairs.put(repair, 2);
    error =
        del.purchaseRepair(
            Map.of(
                CollectionUtils.getMatches(germany.getUnits(), Matches.unitCanBeDamaged())
                    .iterator()
                    .next(),
                repairs));
    assertValid(error);
    assertEquals(0, factory.getUnitDamage());
    // Find cost
    final int finalPUs = germans.getResources().getQuantity("PUs");
    assertEquals(midPUs, finalPUs + 1);
  }

  @Test
  void testRepairMoreThanDamaged() {
    final Territory germany = territory("Germany", gameData);
    final Unit factory = germany.getUnitCollection().getMatches(Matches.unitCanBeDamaged()).get(0);
    final PurchaseDelegate del = purchaseDelegate(gameData);
    del.setDelegateBridgeAndPlayer(newDelegateBridge(germans(gameData)));
    del.start();
    // dame a factory
    final IntegerMap<Unit> startHits = new IntegerMap<>();
    startHits.put(factory, 1);
    gameData.performChange(ChangeFactory.bombingUnitDamage(startHits, List.of(germany)));
    assertEquals(1, factory.getUnitDamage());
    final RepairRule repair = germans(gameData).getRepairFrontier().getRules().get(0);
    final IntegerMap<RepairRule> repairs = new IntegerMap<>();
    // we have 1 damaged marker, but trying to repair 2
    repairs.put(repair, 2);
    final String error =
        del.purchaseRepair(
            Map.of(
                CollectionUtils.getMatches(germany.getUnits(), Matches.unitCanBeDamaged())
                    .iterator()
                    .next(),
                repairs));
    // it is no longer an error, we just math max 0 it
    assertValid(error);
    assertEquals(0, factory.getUnitDamage());
  }

  @Test
  void testOccupiedTerrOfAttachment() {
    // Set up test
    final GamePlayer british = GameDataTestUtil.british(gameData);
    final IDelegateBridge delegateBridge = newDelegateBridge(british(gameData));
    // Set up the move delegate
    final MoveDelegate moveDelegate = moveDelegate(gameData);
    advanceToStep(delegateBridge, "CombatMove");
    moveDelegate(gameData).setDelegateBridgeAndPlayer(delegateBridge);
    moveDelegate(gameData).start();
    // Set up the territories
    final Territory hupeh = territory("Hupeh", gameData);
    final Territory kiangsu = territory("Kiangsu", gameData);
    // Remove all units
    removeFrom(kiangsu, kiangsu.getUnits());
    removeFrom(hupeh, hupeh.getUnits());
    // Set up the unit types
    addTo(hupeh, infantry(gameData).create(1, british));
    // Get units
    final Collection<Unit> moveUnits = hupeh.getUnits();
    // Get Owner prior to battle
    final String preOwner = kiangsu.getOwner().getName();
    assertEquals(Constants.PLAYER_NAME_JAPANESE, preOwner);
    // add a VALID attack
    final String validResults = moveDelegate.move(moveUnits, new Route(hupeh, kiangsu));
    assertValid(validResults);
    // Ensure owner after attack doesn't match attacker
    final String postOwner = kiangsu.getOwner().getName();
    assertNotSame(Constants.PLAYER_NAME_BRITISH, postOwner);
    // Check that original owner is now owner
    assertEquals(Constants.PLAYER_NAME_CHINESE, postOwner);
  }

  @Test
  void testOccupiedTerrOfAttachmentWithCapital() throws GameParseException {
    // Set up test
    final GamePlayer british = GameDataTestUtil.british(gameData);
    final IDelegateBridge delegateBridge = newDelegateBridge(british(gameData));
    // Set up the move delegate
    final MoveDelegate moveDelegate = moveDelegate(gameData);
    advanceToStep(delegateBridge, "CombatMove");
    moveDelegate(gameData).setDelegateBridgeAndPlayer(delegateBridge);
    moveDelegate(gameData).start();
    // Set up the territories
    final Territory hupeh = territory("Hupeh", gameData);
    final Territory kiangsu = territory("Kiangsu", gameData);
    final Territory mongolia = territory("Mongolia", gameData);
    // Remove original capital
    final TerritoryAttachment taMongolia = TerritoryAttachment.get(mongolia);
    final TerritoryAttachment taKiangsu = TerritoryAttachment.get(kiangsu);
    taMongolia.setCapital(null);
    // Set as NEW capital
    taKiangsu.setCapital(Constants.PLAYER_NAME_CHINESE);
    // Remove all units
    removeFrom(kiangsu, kiangsu.getUnits());
    removeFrom(hupeh, hupeh.getUnits());
    // Set up the unit types
    addTo(hupeh, infantry(gameData).create(1, british));
    // Get units
    final Collection<Unit> moveUnits = hupeh.getUnits();
    // Get Owner prior to battle
    final String preOwner = kiangsu.getOwner().getName();
    assertEquals(Constants.PLAYER_NAME_JAPANESE, preOwner);
    // add a VALID attack
    final String validResults = moveDelegate.move(moveUnits, new Route(hupeh, kiangsu));
    assertValid(validResults);
    // Ensure owner after attack doesn't match attacker
    final String postOwner = kiangsu.getOwner().getName();
    assertNotSame(Constants.PLAYER_NAME_BRITISH, postOwner);
    // Check that original owner is now owner
    assertEquals(Constants.PLAYER_NAME_CHINESE, postOwner);
  }

  @Test
  void testTwoStepBlitz() {
    final IDelegateBridge delegateBridge = newDelegateBridge(british(gameData));
    // Set up the territories
    final Territory libya = territory("Libya", gameData);
    final Territory egypt = territory("Egypt", gameData);
    final Territory morrocco = territory("Morocco Algeria", gameData);
    removeFrom(libya, libya.getUnits());
    // Set up the move delegate
    final MoveDelegate moveDelegate = moveDelegate(gameData);
    advanceToStep(delegateBridge, "CombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(delegateBridge);
    moveDelegate.start();
    // blitz in two steps
    final Collection<Unit> armour = egypt.getUnitCollection().getMatches(Matches.unitCanBlitz());
    move(armour, new Route(egypt, libya));
    assertEquals(libya.getOwner(), british(gameData));
    move(armour, new Route(libya, morrocco));
  }

  void assertValid(final String string) {
    assertNull(string, string);
  }

  void assertError(final String string) {
    assertNotNull(string, string);
  }
}
