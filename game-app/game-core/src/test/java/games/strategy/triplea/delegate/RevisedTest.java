package games.strategy.triplea.delegate;

import static games.strategy.triplea.delegate.GameDataTestUtil.aaGun;
import static games.strategy.triplea.delegate.GameDataTestUtil.addTo;
import static games.strategy.triplea.delegate.GameDataTestUtil.americans;
import static games.strategy.triplea.delegate.GameDataTestUtil.armour;
import static games.strategy.triplea.delegate.GameDataTestUtil.assertError;
import static games.strategy.triplea.delegate.GameDataTestUtil.assertMoveError;
import static games.strategy.triplea.delegate.GameDataTestUtil.assertValid;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.MoveDescription;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.TechnologyFrontier;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.engine.delegate.IDelegate;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attachments.TechAttachment;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.battle.BattleDelegate;
import games.strategy.triplea.delegate.battle.BattleTracker;
import games.strategy.triplea.delegate.battle.IBattle;
import games.strategy.triplea.delegate.battle.IBattle.BattleType;
import games.strategy.triplea.delegate.battle.MustFightBattle;
import games.strategy.triplea.delegate.battle.StrategicBombingRaidBattle;
import games.strategy.triplea.delegate.battle.steps.BattleStepsTest;
import games.strategy.triplea.delegate.battle.steps.fire.firststrike.DefensiveFirstStrike;
import games.strategy.triplea.delegate.battle.steps.fire.firststrike.OffensiveFirstStrike;
import games.strategy.triplea.delegate.data.CasualtyDetails;
import games.strategy.triplea.delegate.data.CasualtyList;
import games.strategy.triplea.delegate.data.PlaceableUnits;
import games.strategy.triplea.delegate.data.TechResults;
import games.strategy.triplea.delegate.move.validation.MoveValidator;
import games.strategy.triplea.settings.AbstractClientSettingTestCase;
import games.strategy.triplea.util.TransportUtils;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import org.triplea.java.collections.CollectionUtils;

class RevisedTest extends AbstractClientSettingTestCase {
  private final GameData gameData = TestMapGameData.REVISED.getGameData();

  private static void givenRemotePlayerWillSelectDefaultCasualties(
      final IDelegateBridge delegateBridge) {
    givenRemotePlayerWillSelectCasualtiesPer(
        delegateBridge,
        invocation -> {
          final CasualtyList defaultCasualties = invocation.getArgument(10);
          return new CasualtyDetails(
              defaultCasualties.getKilled(), defaultCasualties.getDamaged(), true);
        });
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

  private static void givenRemotePlayerWillConfirmMoveInFaceOfAa(
      final IDelegateBridge delegateBridge) {
    when(delegateBridge.getRemotePlayer().confirmMoveInFaceOfAa(any())).thenReturn(true);
  }

  @Test
  void testMoveBadRoute() {
    final GamePlayer british = british(gameData);
    final Territory sz1 = gameData.getMap().getTerritory("1 Sea Zone");
    final Territory sz11 = gameData.getMap().getTerritory("11 Sea Zone");
    final Territory sz9 = gameData.getMap().getTerritory("9 Sea Zone");
    final IDelegateBridge bridge = newDelegateBridge(british);
    advanceToStep(bridge, "NonCombatMove");
    moveDelegate(gameData).setDelegateBridgeAndPlayer(bridge);
    moveDelegate(gameData).start();
    final Optional<String> error =
        moveDelegate(gameData).move(sz1.getUnits(), new Route(sz1, sz11, sz9));
    assertNotNull(error);
  }

  @Test
  void testAlliedNeighbors() {
    final GamePlayer americans = americans(gameData);
    final Territory centralUs = territory("Central United States", gameData);
    final Set<Territory> enemyNeighbors =
        gameData.getMap().getNeighbors(centralUs, Matches.isTerritoryEnemy(americans));
    assertTrue(enemyNeighbors.isEmpty());
  }

  @Test
  void testSubAdvance() {
    final UnitType sub = submarine(gameData);
    final UnitAttachment attachment = sub.getUnitAttachment();
    final GamePlayer japanese = japanese(gameData);
    // before the advance, subs defend and attack at 2
    assertEquals(2, attachment.getDefense(japanese));
    assertEquals(2, attachment.getAttack(japanese));
    final IDelegateBridge bridge = newDelegateBridge(japanese);
    TechTracker.addAdvance(
        japanese,
        bridge,
        TechAdvance.findAdvance(
            TechAdvance.TECH_PROPERTY_SUPER_SUBS, gameData.getTechnologyFrontier(), japanese));
    // after tech advance, this is now 3
    assertEquals(2, attachment.getDefense(japanese));
    assertEquals(3, attachment.getAttack(japanese));
  }

  @Test
  void testMoveThroughSubmergedSubs() {
    final GamePlayer british = british(gameData);
    final Territory sz1 = gameData.getMap().getTerritory("1 Sea Zone");
    final Territory sz7 = gameData.getMap().getTerritory("7 Sea Zone");
    final Territory sz8 = gameData.getMap().getTerritory("8 Sea Zone");
    final Unit sub = sz8.getUnitCollection().iterator().next();
    sub.setSubmerged(true);
    // now move to attack it
    final MoveDelegate moveDelegate = (MoveDelegate) gameData.getDelegate("move");
    final IDelegateBridge bridge = newDelegateBridge(british);
    advanceToStep(bridge, "NonCombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(bridge);
    moveDelegate.start();
    // the transport can enter sz 8
    // since the sub is submerged
    final Route m1 = new Route(sz1, sz8);
    move(sz1.getUnits(), m1);
    // the transport can now leave sz8
    final Route m2 = new Route(sz8, sz7);
    move(sz8.getUnitCollection().getMatches(Matches.unitIsOwnedBy(british)), m2);
  }

  @Test
  void testRetreatBug() {
    final GamePlayer russians = russians(gameData);
    final GamePlayer americans = americans(gameData);
    final IDelegateBridge bridge = newDelegateBridge(russians);
    // we need to initialize the original owner
    final InitializationDelegate initDel =
        (InitializationDelegate) gameData.getDelegate("initDelegate");
    initDel.setDelegateBridgeAndPlayer(bridge);
    initDel.start();
    initDel.end();
    // make sinkian japanese owned, put one infantry in it
    final Territory sinkiang = gameData.getMap().getTerritory("Sinkiang");
    gameData.performChange(ChangeFactory.removeUnits(sinkiang, sinkiang.getUnits()));
    final GamePlayer japanese = japanese(gameData);
    sinkiang.setOwner(japanese);
    final UnitType infantryType = infantry(gameData);
    gameData.performChange(ChangeFactory.addUnits(sinkiang, infantryType.create(1, japanese)));
    // now move to attack it
    final MoveDelegate moveDelegate = (MoveDelegate) gameData.getDelegate("move");
    advanceToStep(bridge, "CombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(bridge);
    moveDelegate.start();
    final Territory novo = gameData.getMap().getTerritory("Novosibirsk");
    move(novo.getUnits(), new Route(novo, sinkiang));
    moveDelegate.end();
    final BattleDelegate battle = (BattleDelegate) gameData.getDelegate("battle");
    battle.setDelegateBridgeAndPlayer(bridge);
    whenGetRandom(bridge).thenAnswer(withValues(0, 0)).thenAnswer(withValues(0));
    battle.start(); // fights battle
    battle.end();
    assertEquals(sinkiang.getOwner(), americans);
    assertTrue(battle.getBattleTracker().wasConquered(sinkiang));
    advanceToStep(bridge, "NonCombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(bridge);
    moveDelegate.start();
    final Territory russia = gameData.getMap().getTerritory("Russia");
    // move two tanks from russia, then undo
    final Route r = new Route(russia, novo, sinkiang);
    move(russia.getUnitCollection().getMatches(Matches.unitCanBlitz()), r);
    moveDelegate.undoMove(0);
    assertTrue(battle.getBattleTracker().wasConquered(sinkiang));
    // now move the planes into the territory
    move(russia.getUnitCollection().getMatches(Matches.unitIsAir()), r);
    // make sure they can't land, they can't because the territory was conquered
    assertEquals(1, moveDelegate.getTerritoriesWhereAirCantLand().size());
  }

  @Test
  void testContinuedBattles() {
    final GamePlayer russians = russians(gameData);
    final GamePlayer germans = germans(gameData);
    final IDelegateBridge bridge = newDelegateBridge(germans);
    final MoveDelegate moveDelegate = (MoveDelegate) gameData.getDelegate("move");
    advanceToStep(bridge, "CombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(bridge);
    moveDelegate.start();
    // set up battle
    final Territory germany = gameData.getMap().getTerritory("Germany");
    final Territory karelia = gameData.getMap().getTerritory("Karelia S.S.R.");
    final Territory sz5 = gameData.getMap().getTerritory("5 Sea Zone");
    gameData.performChange(ChangeFactory.removeUnits(sz5, sz5.getUnits()));
    final UnitType infantryType = infantry(gameData);
    final UnitType subType = submarine(gameData);
    final UnitType trnType = transport(gameData);
    gameData.performChange(ChangeFactory.addUnits(sz5, subType.create(1, germans)));
    gameData.performChange(ChangeFactory.addUnits(sz5, trnType.create(1, germans)));
    gameData.performChange(ChangeFactory.addUnits(sz5, subType.create(1, russians)));
    // submerge the russian sub
    final Unit sub =
        CollectionUtils.getMatches(sz5.getUnits(), Matches.unitIsOwnedBy(russians))
            .iterator()
            .next();
    sub.setSubmerged(true);
    // now move an infantry through the sz
    load(
        CollectionUtils.getNMatches(germany.getUnits(), 1, Matches.unitIsOfType(infantryType)),
        new Route(germany, sz5));
    move(
        CollectionUtils.getNMatches(sz5.getUnits(), 1, Matches.unitIsOfType(infantryType)),
        new Route(sz5, karelia));
    moveDelegate.end();
    final IDelegate battle = gameData.getDelegate("battle");
    battle.setDelegateBridgeAndPlayer(bridge);
    battle.start();
    final BattleTracker tracker = AbstractMoveDelegate.getBattleTracker(gameData);
    // The battle should NOT be empty
    assertTrue(tracker.hasPendingNonBombingBattle(sz5));
    assertFalse(tracker.getPendingBattle(sz5, BattleType.NORMAL).isEmpty());
    battle.end();
  }

  @Test
  void testLoadAlliedTransports() {
    final GamePlayer british = british(gameData);
    final GamePlayer americans = americans(gameData);
    final Territory uk = territory("United Kingdom", gameData);
    final IDelegateBridge bridge = newDelegateBridge(british);
    advanceToStep(bridge, "CombatMove");
    moveDelegate(gameData).setDelegateBridgeAndPlayer(bridge);
    moveDelegate(gameData).start();
    // create 2 us infantry
    addTo(uk, infantry(gameData).create(2, americans));
    // try to load them on the british players turn
    final Territory sz2 = territory("2 Sea Zone", gameData);
    final Collection<Unit> units =
        uk.getUnitCollection().getMatches(Matches.unitIsOwnedBy(americans));
    final List<Unit> transports = sz2.getUnitCollection().getMatches(Matches.unitIsSeaTransport());
    final Map<Unit, Unit> unitsToTransports =
        TransportUtils.mapTransports(new Route(uk, sz2), units, transports);
    final Optional<String> error =
        moveDelegate(gameData)
            .performMove(new MoveDescription(units, new Route(uk, sz2), unitsToTransports));
    // should not be able to load on british turn, only on american turn
    assertNotNull(error);
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
    assertTrue(bidPlaceDelegate(gameData).placeUnits(units, uk).isEmpty());
  }

  @Test
  void testOverFlyBombersDies() {
    final GamePlayer british = british(gameData);
    final MoveDelegate moveDelegate = (MoveDelegate) gameData.getDelegate("move");
    final IDelegateBridge bridge = newDelegateBridge(british);
    advanceToStep(bridge, "CombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(bridge);
    moveDelegate.start();
    givenRemotePlayerWillConfirmMoveInFaceOfAa(bridge);
    whenGetRandom(bridge).thenAnswer(withValues(0));
    final Territory uk = territory("United Kingdom", gameData);
    final Territory we = territory("Western Europe", gameData);
    final Territory se = territory("Southern Europe", gameData);
    final Route route = new Route(uk, territory("7 Sea Zone", gameData), we, se);
    move(uk.getUnitCollection().getMatches(Matches.unitIsStrategicBomber()), route);
    // the aa gun should have fired. the bomber no longer exists
    assertTrue(se.getUnitCollection().getMatches(Matches.unitIsStrategicBomber()).isEmpty());
    assertTrue(we.getUnitCollection().getMatches(Matches.unitIsStrategicBomber()).isEmpty());
    assertTrue(uk.getUnitCollection().getMatches(Matches.unitIsStrategicBomber()).isEmpty());
  }

  @Test
  void testMultipleOverFlyBombersDies() {
    final GamePlayer british = british(gameData);
    final MoveDelegate moveDelegate = (MoveDelegate) gameData.getDelegate("move");
    final IDelegateBridge bridge = newDelegateBridge(british);
    advanceToStep(bridge, "CombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(bridge);
    moveDelegate.start();
    givenRemotePlayerWillConfirmMoveInFaceOfAa(bridge);
    whenGetRandom(bridge).thenAnswer(withValues(0, 4)).thenAnswer(withValues(0));
    final Territory uk = territory("United Kingdom", gameData);
    final Territory sz7 = territory("7 Sea Zone", gameData);
    final Territory we = territory("Western Europe", gameData);
    final Territory se = territory("Southern Europe", gameData);
    final Territory balk = territory("Balkans", gameData);
    addTo(uk, bomber(gameData).create(1, british));
    final Route route = new Route(uk, sz7, we, se, balk);
    move(uk.getUnitCollection().getMatches(Matches.unitIsStrategicBomber()), route);
    // the aa gun should have fired (one hit, one miss in each territory overflown). the bombers no
    // longer exists
    assertTrue(uk.getUnitCollection().getMatches(Matches.unitIsStrategicBomber()).isEmpty());
    assertTrue(we.getUnitCollection().getMatches(Matches.unitIsStrategicBomber()).isEmpty());
    assertTrue(se.getUnitCollection().getMatches(Matches.unitIsStrategicBomber()).isEmpty());
    assertTrue(balk.getUnitCollection().getMatches(Matches.unitIsStrategicBomber()).isEmpty());
  }

  @Test
  void testOverFlyBombersJoiningBattleDie() {
    // a bomber flies over aa to join a battle, gets hit,
    // it should not appear in the battle
    final GamePlayer british = british(gameData);
    final MoveDelegate moveDelegate = (MoveDelegate) gameData.getDelegate("move");
    final IDelegateBridge bridge = newDelegateBridge(british);
    advanceToStep(bridge, "CombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(bridge);
    moveDelegate.start();
    givenRemotePlayerWillConfirmMoveInFaceOfAa(bridge);
    whenGetRandom(bridge).thenAnswer(withValues(0));
    final Territory uk = territory("United Kingdom", gameData);
    final Territory we = territory("Western Europe", gameData);
    final Territory se = territory("Southern Europe", gameData);
    final Route route = new Route(uk, territory("7 Sea Zone", gameData), we, se);
    move(uk.getUnitCollection().getMatches(Matches.unitIsStrategicBomber()), route);
    // the aa gun should have fired and hit
    assertTrue(se.getUnitCollection().getMatches(Matches.unitIsStrategicBomber()).isEmpty());
    assertTrue(we.getUnitCollection().getMatches(Matches.unitIsStrategicBomber()).isEmpty());
    assertTrue(uk.getUnitCollection().getMatches(Matches.unitIsStrategicBomber()).isEmpty());
  }

  @Test
  void testTransportAttack() {
    final Territory sz14 = gameData.getMap().getTerritory("14 Sea Zone");
    final Territory sz13 = gameData.getMap().getTerritory("13 Sea Zone");
    final GamePlayer germans = germans(gameData);
    final MoveDelegate moveDelegate = (MoveDelegate) gameData.getDelegate("move");
    final IDelegateBridge bridge = newDelegateBridge(germans);
    advanceToStep(bridge, "CombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(bridge);
    moveDelegate.start();
    final Route sz14To13 = new Route(sz14, sz13);
    final List<Unit> transports = sz14.getUnitCollection().getMatches(Matches.unitIsSeaTransport());
    assertEquals(1, transports.size());
    move(transports, sz14To13);
  }

  @Test
  void testLoadUndo() {
    final Territory sz5 = gameData.getMap().getTerritory("5 Sea Zone");
    final Territory eastEurope = gameData.getMap().getTerritory("Eastern Europe");
    final UnitType infantryType = infantry(gameData);
    final GamePlayer germans = germans(gameData);
    final MoveDelegate moveDelegate = (MoveDelegate) gameData.getDelegate("move");
    final IDelegateBridge bridge = newDelegateBridge(germans);
    advanceToStep(bridge, "CombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(bridge);
    moveDelegate.start();
    final Route eeToSz5 = new Route(eastEurope, sz5);
    // load the transport in the baltic
    final List<Unit> infantry =
        eastEurope.getUnitCollection().getMatches(Matches.unitIsOfType(infantryType));
    assertEquals(2, infantry.size());
    final Unit transport = sz5.getUnitCollection().getMatches(Matches.unitIsSeaTransport()).get(0);
    final Optional<String> error =
        moveDelegate.performMove(
            new MoveDescription(
                infantry, eeToSz5, Map.of(infantry.get(0), transport, infantry.get(1), transport)));
    assertValid(error);
    // make sure the transport was loaded
    assertTrue(moveDelegate.getMovesMade().get(0).wasTransportLoaded(transport));
    // make sure it was laoded
    assertTrue(transport.getTransporting().containsAll(infantry));
    assertTrue(infantry.get(0).getWasLoadedThisTurn());
    // udo the move
    moveDelegate.undoMove(0);
    // make sure that loaded is not set
    assertTrue(transport.getTransporting().isEmpty());
    assertFalse(infantry.get(0).getWasLoadedThisTurn());
  }

  @Test
  void testLoadDependencies() {
    final Territory sz5 = gameData.getMap().getTerritory("5 Sea Zone");
    final Territory eastEurope = gameData.getMap().getTerritory("Eastern Europe");
    final Territory norway = gameData.getMap().getTerritory("Norway");
    final UnitType infantryType = infantry(gameData);
    final GamePlayer germans = germans(gameData);
    final MoveDelegate moveDelegate = (MoveDelegate) gameData.getDelegate("move");
    final IDelegateBridge bridge = newDelegateBridge(germans);
    advanceToStep(bridge, "CombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(bridge);
    moveDelegate.start();
    final Route eeToSz5 = new Route(eastEurope, sz5);
    // load the transport in the baltic
    final List<Unit> infantry =
        eastEurope.getUnitCollection().getMatches(Matches.unitIsOfType(infantryType));
    assertEquals(2, infantry.size());
    final Unit transport = sz5.getUnitCollection().getMatches(Matches.unitIsSeaTransport()).get(0);
    // load the transport
    Optional<String> error =
        moveDelegate.performMove(
            new MoveDescription(
                infantry, eeToSz5, Map.of(infantry.get(0), transport, infantry.get(1), transport)));
    assertValid(error);
    final Route sz5ToNorway = new Route(sz5, norway);
    // move the infantry in two steps
    error = moveDelegate.move(infantry.subList(0, 1), sz5ToNorway);
    assertValid(error);
    error = moveDelegate.move(infantry.subList(1, 2), sz5ToNorway);
    assertValid(error);
    assertEquals(3, moveDelegate.getMovesMade().size());
    // the load
    final UndoableMove move1 = moveDelegate.getMovesMade().get(0);
    // the first unload
    // AbstractUndoableMove move2 = moveDelegate.getMovesMade().get(0);
    // the second unload must be done first
    assertFalse(move1.getCanUndo());
    String undoError = moveDelegate.undoMove(2);
    assertNull(undoError);
    // the second unload must be done first
    assertFalse(move1.getCanUndo());
    undoError = moveDelegate.undoMove(1);
    assertNull(undoError);
    // we can now be undone
    assertTrue(move1.getCanUndo());
  }

  @Test
  void testLoadUndoInWrongOrder() {
    final Territory sz5 = gameData.getMap().getTerritory("5 Sea Zone");
    final Territory eastEurope = gameData.getMap().getTerritory("Eastern Europe");
    final UnitType infantryType = GameDataTestUtil.infantry(gameData);
    final GamePlayer germans = GameDataTestUtil.germans(gameData);
    final MoveDelegate moveDelegate = (MoveDelegate) gameData.getDelegate("move");
    final IDelegateBridge bridge = newDelegateBridge(germans);
    advanceToStep(bridge, "CombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(bridge);
    moveDelegate.start();
    final Route eeToSz5 = new Route(eastEurope, sz5);
    // load the transport in the baltic
    final List<Unit> infantry =
        eastEurope.getUnitCollection().getMatches(Matches.unitIsOfType(infantryType));
    assertEquals(2, infantry.size());
    final Unit transport = sz5.getUnitCollection().getMatches(Matches.unitIsSeaTransport()).get(0);
    // load the transports
    // in two moves
    Optional<String> error =
        moveDelegate.performMove(
            new MoveDescription(
                infantry.subList(0, 1), eeToSz5, Map.of(infantry.get(0), transport)));
    assertValid(error);
    error =
        moveDelegate.performMove(
            new MoveDescription(
                infantry.subList(1, 2), eeToSz5, Map.of(infantry.get(1), transport)));
    assertValid(error);
    // make sure the transport was loaded
    assertTrue(moveDelegate.getMovesMade().get(0).wasTransportLoaded(transport));
    assertTrue(moveDelegate.getMovesMade().get(1).wasTransportLoaded(transport));
    // udo the moves in reverse order
    moveDelegate.undoMove(0);
    moveDelegate.undoMove(0);
    // make sure that loaded is not set
    assertTrue(transport.getTransporting().isEmpty());
    assertFalse(infantry.get(0).getWasLoadedThisTurn());
  }

  @Test
  void testLoadUnloadAlliedTransport() {
    // you cant load and unload an allied transport the same turn
    final UnitType infantryType = infantry(gameData);
    final Territory eastEurope = gameData.getMap().getTerritory("Eastern Europe");
    // add japanese infantry to eastern europe
    final GamePlayer japanese = japanese(gameData);
    final Change change = ChangeFactory.addUnits(eastEurope, infantryType.create(1, japanese));
    gameData.performChange(change);
    final Territory sz5 = gameData.getMap().getTerritory("5 Sea Zone");
    final MoveDelegate moveDelegate = (MoveDelegate) gameData.getDelegate("move");
    final IDelegateBridge bridge = newDelegateBridge(japanese);
    advanceToStep(bridge, "CombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(bridge);
    moveDelegate.start();
    final Route eeToSz5 = new Route(eastEurope, sz5);
    // load the transport in the baltic
    final List<Unit> infantry =
        eastEurope
            .getUnitCollection()
            .getMatches(Matches.unitIsOfType(infantryType).and(Matches.unitIsOwnedBy(japanese)));
    assertEquals(1, infantry.size());
    final Unit transport = sz5.getUnitCollection().getMatches(Matches.unitIsSeaTransport()).get(0);
    Optional<String> error =
        moveDelegate.performMove(
            new MoveDescription(infantry, eeToSz5, Map.of(infantry.get(0), transport)));
    assertValid(error);
    // try to unload
    final Route sz5ToEee = new Route(sz5, eastEurope);
    error = moveDelegate.move(infantry, sz5ToEee);
    assertEquals(
        MoveValidator.CANNOT_LOAD_AND_UNLOAD_AN_ALLIED_TRANSPORT_IN_THE_SAME_ROUND,
        error.orElse(""));
  }

  @Test
  void testUnloadMultipleTerritories() {
    // in revised a transport may only unload to 1 territory.
    final Territory sz5 = gameData.getMap().getTerritory("5 Sea Zone");
    final Territory eastEurope = gameData.getMap().getTerritory("Eastern Europe");
    final UnitType infantryType = GameDataTestUtil.infantry(gameData);
    final GamePlayer germans = GameDataTestUtil.germans(gameData);
    final MoveDelegate moveDelegate = (MoveDelegate) gameData.getDelegate("move");
    final IDelegateBridge bridge = newDelegateBridge(germans);
    advanceToStep(bridge, "CombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(bridge);
    moveDelegate.start();
    final Route eeToSz5 = new Route(eastEurope, sz5);
    // load the transport in the baltic
    final List<Unit> infantry =
        eastEurope.getUnitCollection().getMatches(Matches.unitIsOfType(infantryType));
    assertEquals(2, infantry.size());
    final Unit transport = sz5.getUnitCollection().getMatches(Matches.unitIsSeaTransport()).get(0);
    Optional<String> error =
        moveDelegate.performMove(
            new MoveDescription(
                infantry, eeToSz5, Map.of(infantry.get(0), transport, infantry.get(1), transport)));
    assertValid(error);
    // unload one infantry to Norway
    final Territory norway = gameData.getMap().getTerritory("Norway");
    final Route sz5ToNorway = new Route(sz5, norway);
    error = moveDelegate.move(infantry.subList(0, 1), sz5ToNorway);
    assertValid(error);
    // make sure the transport was unloaded
    assertTrue(moveDelegate.getMovesMade().get(1).wasTransportUnloaded(transport));
    // try to unload the other infantry somewhere else, an error occurs
    final Route sz5ToEastEurope = new Route(sz5, eastEurope);
    error = moveDelegate.move(infantry.subList(1, 2), sz5ToEastEurope);
    assertError(error);
    assertTrue(error.get().startsWith(MoveValidator.TRANSPORT_HAS_ALREADY_UNLOADED_UNITS_TO));
    // end the round
    moveDelegate.end();
    advanceToStep(bridge, "NonCombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(bridge);
    moveDelegate.start();
    moveDelegate.end();
    advanceToStep(bridge, "CombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(bridge);
    moveDelegate.start();
    // a new round, the move should work
    moveDelegate.setDelegateBridgeAndPlayer(bridge);
    moveDelegate.start();
    error = moveDelegate.move(infantry.subList(1, 2), sz5ToEastEurope);
    assertValid(error);
  }

  @Test
  void testUnloadInPreviousPhase() {
    // a transport may not unload in both combat and non combat
    final Territory sz5 = gameData.getMap().getTerritory("5 Sea Zone");
    final Territory eastEurope = gameData.getMap().getTerritory("Eastern Europe");
    final UnitType infantryType = GameDataTestUtil.infantry(gameData);
    final GamePlayer germans = GameDataTestUtil.germans(gameData);
    final MoveDelegate moveDelegate = (MoveDelegate) gameData.getDelegate("move");
    final IDelegateBridge bridge = newDelegateBridge(germans);
    advanceToStep(bridge, "CombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(bridge);
    moveDelegate.start();
    final Route eeToSz5 = new Route(eastEurope, sz5);
    // load the transport in the baltic
    final List<Unit> infantry =
        eastEurope.getUnitCollection().getMatches(Matches.unitIsOfType(infantryType));
    assertEquals(2, infantry.size());
    final Unit transport = sz5.getUnitCollection().getMatches(Matches.unitIsSeaTransport()).get(0);
    Optional<String> error =
        moveDelegate.performMove(
            new MoveDescription(
                infantry, eeToSz5, Map.of(infantry.get(0), transport, infantry.get(1), transport)));
    assertValid(error);
    // unload one infantry to Norway
    final Territory norway = gameData.getMap().getTerritory("Norway");
    final Route sz5ToNorway = new Route(sz5, norway);
    error = moveDelegate.move(infantry.subList(0, 1), sz5ToNorway);
    assertValid(error);
    assertTrue(infantry.get(0).getWasUnloadedInCombatPhase());
    // start non combat
    moveDelegate.end();
    advanceToStep(bridge, "germanNonCombatMove");
    // the transport tracker relies on the step name
    while (!gameData.getSequence().getStep().getName().equals("germanNonCombatMove")) {
      gameData.getSequence().next();
    }
    moveDelegate.setDelegateBridgeAndPlayer(bridge);
    moveDelegate.start();
    // try to unload the other infantry somewhere else, an error occurs
    error = moveDelegate.move(infantry.subList(1, 2), sz5ToNorway);
    assertError(error);
    assertTrue(
        error
            .get()
            .startsWith(MoveValidator.TRANSPORT_HAS_ALREADY_UNLOADED_UNITS_IN_A_PREVIOUS_PHASE));
  }

  @Test
  void testSubAttackTransportNonCombat() {
    final Territory sz1 = territory("1 Sea Zone", gameData);
    final Territory sz8 = territory("8 Sea Zone", gameData);
    final GamePlayer germans = germans(gameData);
    // german sub tries to attack a transport in non combat
    // should be an error
    final MoveDelegate moveDelegate = (MoveDelegate) gameData.getDelegate("move");
    final IDelegateBridge bridge = newDelegateBridge(germans);
    advanceToStep(bridge, "NonCombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(bridge);
    moveDelegate.start();
    final Optional<String> error = moveDelegate(gameData).move(sz8.getUnits(), new Route(sz8, sz1));
    assertError(error);
  }

  @Test
  void testSubAttackNonCombat() {
    final Territory sz2 = territory("2 Sea Zone", gameData);
    final Territory sz8 = territory("8 Sea Zone", gameData);
    final GamePlayer germans = germans(gameData);
    // german sub tries to attack a transport in non combat
    // should be an error
    final MoveDelegate moveDelegate = (MoveDelegate) gameData.getDelegate("move");
    final IDelegateBridge bridge = newDelegateBridge(germans);
    advanceToStep(bridge, "NonCombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(bridge);
    moveDelegate.start();
    final Optional<String> error = moveDelegate(gameData).move(sz8.getUnits(), new Route(sz8, sz2));
    assertError(error);
  }

  @Test
  void testTransportAttackSubNonCombat() {
    final Territory sz1 = territory("1 Sea Zone", gameData);
    final Territory sz8 = territory("8 Sea Zone", gameData);
    final GamePlayer british = british(gameData);
    // german sub tries to attack a transport in non combat
    // should be an error
    final MoveDelegate moveDelegate = (MoveDelegate) gameData.getDelegate("move");
    final IDelegateBridge bridge = newDelegateBridge(british);
    advanceToStep(bridge, "NonCombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(bridge);
    moveDelegate.start();
    final Optional<String> error = moveDelegate(gameData).move(sz8.getUnits(), new Route(sz1, sz8));
    assertError(error);
  }

  @Test
  void testMoveSubAwayFromSubmergedSubsInBattleZone() {
    final Territory sz45 = gameData.getMap().getTerritory("45 Sea Zone");
    final Territory sz50 = gameData.getMap().getTerritory("50 Sea Zone");
    final GamePlayer british = british(gameData);
    final GamePlayer japanese = japanese(gameData);
    // put 1 british sub in sz 45, this simulates a submerged enemy sub
    final UnitType sub = submarine(gameData);
    final Change c = ChangeFactory.addUnits(sz45, sub.create(1, british));
    gameData.performChange(c);
    // new move delegate
    final MoveDelegate moveDelegate = (MoveDelegate) gameData.getDelegate("move");
    final IDelegateBridge bridge = newDelegateBridge(japanese);
    advanceToStep(bridge, "CombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(bridge);
    moveDelegate.start();
    // move a fighter into the sea zone, this will cause a battle
    final Route sz50To45 = new Route(sz50, sz45);
    Optional<String> error =
        moveDelegate.move(sz50.getUnitCollection().getMatches(Matches.unitIsAir()), sz50To45);
    assertValid(error);
    final var battleTracker = AbstractMoveDelegate.getBattleTracker(gameData);
    assertEquals(1, battleTracker.getPendingBattleSitesWithoutBombing().size());
    // we should be able to move the sub out of the sz
    final Route sz45To50 = new Route(sz45, sz50);
    final List<Unit> japSub =
        sz45.getUnitCollection()
            .getMatches(Matches.unitCanEvade().and(Matches.unitIsOwnedBy(japanese)));
    error = moveDelegate.move(japSub, sz45To50);
    // make sure no error
    assertValid(error);
    // make sure the battle is still there
    assertEquals(1, battleTracker.getPendingBattleSitesWithoutBombing().size());
    // we should be able to undo the move of the sub
    String undoError = moveDelegate.undoMove(1);
    assertNull(undoError);
    // undo the move of the fighter, should be no battles now
    undoError = moveDelegate.undoMove(0);
    assertNull(undoError);
    assertEquals(0, battleTracker.getPendingBattleSitesWithoutBombing().size());
  }

  @Test
  void testAaOwnership() {
    // Set up players
    // PlayerId british = GameDataTestUtil.british(gameData);
    final GamePlayer japanese = GameDataTestUtil.japanese(gameData);
    // PlayerId americans = GameDataTestUtil.americans(gameData);
    // Set up the territories
    final Territory india = territory("India", gameData);
    final Territory fic = territory("French Indochina", gameData);
    final Territory china = territory("China", gameData);
    final Territory kwang = territory("Kwantung", gameData);
    // Preset units in FIC
    final UnitType infType = infantry(gameData);
    // UnitType aaType = GameDataTestUtil.aaGun(gameData);
    removeFrom(fic, fic.getUnits());
    addTo(fic, aaGun(gameData).create(1, japanese));
    addTo(fic, infantry(gameData).create(1, japanese));
    assertEquals(2, fic.getUnitCollection().getUnitCount());
    // Get attacking units
    final Collection<Unit> britishUnits = india.getUnitCollection().getUnits(infType, 1);
    final Collection<Unit> japaneseUnits = kwang.getUnitCollection().getUnits(infType, 1);
    final Collection<Unit> americanUnits = china.getUnitCollection().getUnits(infType, 1);
    // Get Owner prior to battle
    assertTrue(fic.getUnitCollection().allMatch(Matches.unitIsOwnedBy(japanese(gameData))));
    final String preOwner = fic.getOwner().getName();
    assertEquals(Constants.PLAYER_NAME_JAPANESE, preOwner);
    // Set up the move delegate
    IDelegateBridge delegateBridge = newDelegateBridge(british(gameData));
    final MoveDelegate moveDelegate = moveDelegate(gameData);
    advanceToStep(delegateBridge, "CombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(delegateBridge);
    moveDelegate.start();
    /*
     * add a VALID BRITISH attack
     */
    Optional<String> validResults = moveDelegate.move(britishUnits, new Route(india, fic));
    assertValid(validResults);
    moveDelegate(gameData).end();
    // Set up battle
    MustFightBattle battle =
        (MustFightBattle)
            AbstractMoveDelegate.getBattleTracker(gameData)
                .getPendingBattle(fic, BattleType.NORMAL);
    // fight
    whenGetRandom(delegateBridge).thenAnswer(withValues(0)).thenAnswer(withValues(5));
    battle.fight(delegateBridge);
    // Get Owner after to battle
    assertTrue(fic.getUnitCollection().allMatch(Matches.unitIsOwnedBy(british(gameData))));
    final String postOwner = fic.getOwner().getName();
    assertEquals(Constants.PLAYER_NAME_BRITISH, postOwner);
    /*
     * add a VALID JAPANESE attack
     */
    // Set up battle
    delegateBridge = newDelegateBridge(japanese(gameData));
    advanceToStep(delegateBridge, "CombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(delegateBridge);
    moveDelegate.start();
    // Move to battle
    validResults = moveDelegate.move(japaneseUnits, new Route(kwang, fic));
    assertValid(validResults);
    moveDelegate(gameData).end();
    battle =
        (MustFightBattle)
            AbstractMoveDelegate.getBattleTracker(gameData)
                .getPendingBattle(fic, BattleType.NORMAL);
    // fight
    whenGetRandom(delegateBridge).thenAnswer(withValues(0)).thenAnswer(withValues(5));
    battle.fight(delegateBridge);
    // Get Owner after to battle
    assertTrue(fic.getUnitCollection().allMatch(Matches.unitIsOwnedBy(japanese(gameData))));
    final String midOwner = fic.getOwner().getName();
    assertEquals(Constants.PLAYER_NAME_JAPANESE, midOwner);
    /*
     * add a VALID AMERICAN attack
     */
    // Set up battle
    delegateBridge = newDelegateBridge(americans(gameData));
    advanceToStep(delegateBridge, "CombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(delegateBridge);
    moveDelegate.start();
    // Move to battle
    validResults = moveDelegate.move(americanUnits, new Route(china, fic));
    assertValid(validResults);
    moveDelegate(gameData).end();
    battle =
        (MustFightBattle)
            AbstractMoveDelegate.getBattleTracker(gameData).getPendingNonBombingBattle(fic);
    // fight
    whenGetRandom(delegateBridge).thenAnswer(withValues(0)).thenAnswer(withValues(5));
    battle.fight(delegateBridge);
    // Get Owner after to battle
    assertTrue(fic.getUnitCollection().allMatch(Matches.unitIsOwnedBy(americans(gameData))));
    final String endOwner = fic.getOwner().getName();
    assertEquals(Constants.PLAYER_NAME_AMERICANS, endOwner);
  }

  @Test
  void testStratBombCasualties() {
    final Territory germany = gameData.getMap().getTerritory("Germany");
    final Territory uk = gameData.getMap().getTerritory("United Kingdom");
    final GamePlayer germans = germans(gameData);
    final GamePlayer british = british(gameData);
    final BattleTracker tracker = new BattleTracker();
    final IBattle battle = new StrategicBombingRaidBattle(germany, gameData, british, tracker);
    final List<Unit> bombers = uk.getUnitCollection().getMatches(Matches.unitIsStrategicBomber());
    addTo(germany, bombers);
    battle.addAttackChange(
        gameData.getMap().getRouteOrElseThrow(uk, germany, it -> true), bombers, null);
    tracker
        .getBattleRecords()
        .addBattle(british, battle.getBattleId(), germany, battle.getBattleType());
    final IDelegateBridge bridge = newDelegateBridge(british);
    // aa guns rolls 0 and hits
    whenGetRandom(bridge).thenAnswer(withValues(0));
    final int pusBeforeRaid =
        germans.getResources().getQuantity(gameData.getResourceList().getResource(Constants.PUS));
    battle.fight(bridge);
    final int pusAfterRaid =
        germans.getResources().getQuantity(gameData.getResourceList().getResource(Constants.PUS));
    assertEquals(pusBeforeRaid, pusAfterRaid);
    assertEquals(0, germany.getUnitCollection().getMatches(Matches.unitIsOwnedBy(british)).size());
    thenGetRandomShouldHaveBeenCalled(bridge, times(1));
  }

  @Test
  void testStratBombCasualtiesLowLuck() {
    makeGameLowLuck(gameData);
    final Territory germany = gameData.getMap().getTerritory("Germany");
    final Territory uk = gameData.getMap().getTerritory("United Kingdom");
    final GamePlayer germans = GameDataTestUtil.germans(gameData);
    final GamePlayer british = GameDataTestUtil.british(gameData);
    final BattleTracker tracker = new BattleTracker();
    final IBattle battle = new StrategicBombingRaidBattle(germany, gameData, british, tracker);
    final List<Unit> bombers = bomber(gameData).create(2, british);
    addTo(germany, bombers);
    battle.addAttackChange(
        gameData.getMap().getRouteOrElseThrow(uk, germany, it -> true), bombers, null);
    tracker
        .getBattleRecords()
        .addBattle(british, battle.getBattleId(), germany, battle.getBattleType());
    final IDelegateBridge bridge = newDelegateBridge(british);
    // should be exactly 3 rolls total. would be exactly 2 rolls if the number of units being shot
    // at = max dice side of
    // the AA gun, because the casualty selection roll would not happen in LL
    // first 0 is the AA gun rolling 1@2 and getting a 1, which is a hit
    // second 0 is the LL AA casualty selection randomly picking the first unit to die
    // third 0 is the single remaining bomber dealing 1 damage to the enemy's PUs
    whenGetRandom(bridge)
        .thenAnswer(withValues(0))
        .thenAnswer(withValues(0))
        .thenAnswer(withValues(0));
    final int pusBeforeRaid =
        germans.getResources().getQuantity(gameData.getResourceList().getResource(Constants.PUS));
    battle.fight(bridge);
    final int pusAfterRaid =
        germans.getResources().getQuantity(gameData.getResourceList().getResource(Constants.PUS));
    assertEquals(pusBeforeRaid - 1, pusAfterRaid);
    assertEquals(1, germany.getUnitCollection().getMatches(Matches.unitIsOwnedBy(british)).size());
    thenGetRandomShouldHaveBeenCalled(bridge, times(3));
  }

  @Test
  void testStratBombCasualtiesLowLuckManyBombers() {
    makeGameLowLuck(gameData);
    final Territory germany = gameData.getMap().getTerritory("Germany");
    final Territory uk = gameData.getMap().getTerritory("United Kingdom");
    final GamePlayer germans = GameDataTestUtil.germans(gameData);
    final GamePlayer british = GameDataTestUtil.british(gameData);
    final BattleTracker tracker = new BattleTracker();
    final IBattle battle = new StrategicBombingRaidBattle(germany, gameData, british, tracker);
    final List<Unit> bombers = bomber(gameData).create(7, british);
    addTo(germany, bombers);
    battle.addAttackChange(
        gameData.getMap().getRouteOrElseThrow(uk, germany, it -> true), bombers, null);
    tracker
        .getBattleRecords()
        .addBattle(british, battle.getBattleId(), germany, battle.getBattleType());
    final IDelegateBridge bridge = newDelegateBridge(british);
    // aa guns rolls 0 and hits, next 5 dice are for the bombing raid cost for the surviving bombers
    whenGetRandom(bridge).thenAnswer(withValues(0)).thenAnswer(withValues(0, 0, 0, 0, 0));
    final int pusBeforeRaid =
        germans.getResources().getQuantity(gameData.getResourceList().getResource(Constants.PUS));
    battle.fight(bridge);
    final int pusAfterRaid =
        germans.getResources().getQuantity(gameData.getResourceList().getResource(Constants.PUS));
    assertEquals(pusBeforeRaid - 5, pusAfterRaid);
    // 2 bombers get hit
    assertEquals(5, germany.getUnitCollection().getMatches(Matches.unitIsOwnedBy(british)).size());
    thenGetRandomShouldHaveBeenCalled(bridge, times(2));
  }

  @Test
  void testStratBombRaidWithHeavyBombers() {
    final Territory germany = gameData.getMap().getTerritory("Germany");
    final Territory uk = gameData.getMap().getTerritory("United Kingdom");
    final GamePlayer germans = GameDataTestUtil.germans(gameData);
    final GamePlayer british = GameDataTestUtil.british(gameData);
    final BattleTracker tracker = new BattleTracker();
    final IBattle battle = new StrategicBombingRaidBattle(germany, gameData, british, tracker);
    battle.addAttackChange(
        gameData.getMap().getRouteOrElseThrow(uk, germany, it -> true),
        uk.getUnitCollection().getMatches(Matches.unitIsStrategicBomber()),
        null);
    addTo(germany, uk.getUnitCollection().getMatches(Matches.unitIsStrategicBomber()));
    tracker
        .getBattleRecords()
        .addBattle(british, battle.getBattleId(), germany, battle.getBattleType());
    final IDelegateBridge bridge = newDelegateBridge(british);
    TechTracker.addAdvance(
        british,
        bridge,
        TechAdvance.findAdvance(
            TechAdvance.TECH_PROPERTY_HEAVY_BOMBER, gameData.getTechnologyFrontier(), british));
    // aa guns rolls 3, misses, bomber rolls 2 dice at 3
    whenGetRandom(bridge).thenAnswer(withValues(3)).thenAnswer(withValues(2, 2));
    final int pusBeforeRaid =
        germans.getResources().getQuantity(gameData.getResourceList().getResource(Constants.PUS));
    battle.fight(bridge);
    final int pusAfterRaid =
        germans.getResources().getQuantity(gameData.getResourceList().getResource(Constants.PUS));
    assertEquals(pusBeforeRaid - 6, pusAfterRaid);
  }

  @Test
  void testLandBattleNoSneakAttack() {
    final GamePlayer defenderPlayer = germans(gameData);
    final String attacker = "British";
    final GamePlayer attackerPlayer = british(gameData);
    final Territory attacked = territory("Libya", gameData);
    final Territory from = territory("Anglo Egypt", gameData);
    final IDelegateBridge bridge = newDelegateBridge(british(gameData));
    advanceToStep(bridge, "CombatMove");
    moveDelegate(gameData).setDelegateBridgeAndPlayer(bridge);
    moveDelegate(gameData).start();
    move(from.getUnits(), new Route(from, attacked));
    moveDelegate(gameData).end();
    final MustFightBattle battle =
        (MustFightBattle)
            AbstractMoveDelegate.getBattleTracker(gameData).getPendingNonBombingBattle(attacked);
    final List<String> steps = battle.determineStepStrings();
    assertEquals(
        BattleStepsTest.mergeSteps(
                BattleStepsTest.generalFightStepStrings(attackerPlayer, defenderPlayer),
                BattleStepsTest.generalFightStepStrings(defenderPlayer, attackerPlayer),
                List.of(REMOVE_CASUALTIES, attacker + ATTACKER_WITHDRAW))
            .toString(),
        steps.toString());
  }

  @Test
  void testSeaBattleNoSneakAttack() {
    final GamePlayer defenderPlayer = germans(gameData);
    final String attacker = "British";
    final GamePlayer attackerPlayer = british(gameData);
    final Territory attacked = territory("31 Sea Zone", gameData);
    final Territory from = territory("32 Sea Zone", gameData);
    // 1 destroyer attacks 1 destroyer
    addTo(from, destroyer(gameData).create(1, british(gameData)));
    addTo(attacked, destroyer(gameData).create(1, germans(gameData)));
    final IDelegateBridge bridge = newDelegateBridge(british(gameData));
    advanceToStep(bridge, "CombatMove");
    moveDelegate(gameData).setDelegateBridgeAndPlayer(bridge);
    moveDelegate(gameData).start();
    move(from.getUnits(), new Route(from, attacked));
    moveDelegate(gameData).end();
    final MustFightBattle battle =
        (MustFightBattle)
            AbstractMoveDelegate.getBattleTracker(gameData).getPendingNonBombingBattle(attacked);
    final List<String> steps = battle.determineStepStrings();
    assertEquals(
        BattleStepsTest.mergeSteps(
                BattleStepsTest.generalFightStepStrings(attackerPlayer, defenderPlayer),
                BattleStepsTest.generalFightStepStrings(defenderPlayer, attackerPlayer),
                List.of(REMOVE_CASUALTIES, attacker + ATTACKER_WITHDRAW))
            .toString(),
        steps.toString());
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
    addTo(from, submarine(gameData).create(1, british(gameData)));
    addTo(attacked, submarine(gameData).create(1, germans(gameData)));
    final IDelegateBridge bridge = newDelegateBridge(british(gameData));
    advanceToStep(bridge, "CombatMove");
    moveDelegate(gameData).setDelegateBridgeAndPlayer(bridge);
    moveDelegate(gameData).start();
    move(from.getUnits(), new Route(from, attacked));
    moveDelegate(gameData).end();
    final MustFightBattle battle =
        (MustFightBattle)
            AbstractMoveDelegate.getBattleTracker(gameData).getPendingNonBombingBattle(attacked);
    final List<String> steps = battle.determineStepStrings();
    assertEquals(
        BattleStepsTest.mergeSteps(
                BattleStepsTest.firstStrikeFightStepStrings(attackerPlayer, defenderPlayer),
                BattleStepsTest.firstStrikeFightStepStrings(defenderPlayer, attackerPlayer),
                List.of(
                    REMOVE_SNEAK_ATTACK_CASUALTIES,
                    REMOVE_CASUALTIES,
                    attacker + SUBS_SUBMERGE,
                    attacker + ATTACKER_WITHDRAW,
                    defender + SUBS_SUBMERGE))
            .toString(),
        steps.toString());
    final List<IExecutable> execs = battle.getBattleExecutables();
    final int attackSubs = getIndex(execs, OffensiveFirstStrike.class);
    final int defendSubs = getIndex(execs, DefensiveFirstStrike.class);
    assertTrue(attackSubs < defendSubs);
    // fight, each sub should fire and hit
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
    // 2 sub attacks 1 sub and 1 destroyer
    addTo(from, submarine(gameData).create(2, british(gameData)));
    addTo(attacked, submarine(gameData).create(1, germans(gameData)));
    addTo(attacked, destroyer(gameData).create(1, germans(gameData)));
    final IDelegateBridge bridge = newDelegateBridge(british(gameData));
    advanceToStep(bridge, "CombatMove");
    moveDelegate(gameData).setDelegateBridgeAndPlayer(bridge);
    moveDelegate(gameData).start();
    move(from.getUnits(), new Route(from, attacked));
    moveDelegate(gameData).end();
    final MustFightBattle battle =
        (MustFightBattle)
            AbstractMoveDelegate.getBattleTracker(gameData).getPendingNonBombingBattle(attacked);
    final List<String> steps = battle.determineStepStrings();
    /*
     * Here are the exact errata clarifications on how REVISED rules subs work:
     * Every sub, regardless of whether it is on the attacking or defending side, fires in the
     * Opening Fire step of combat. That is the only time a sub ever fires.
     * Losses caused by attacking or defending subs are removed at the end of the Opening Fire
     * step, before normal attack and defense rolls, unless the enemy has a destroyer present.
     * If the enemy (attacker or defender) has a destroyer, then hits caused by your subs are not
     * removed until the Remove Casualties step (step 6) of combat.
     * In other words, subs work exactly the same for the attacker and the defender. Nothing,
     * not even a destroyer, ever stops a sub from rolling its die (attack or defense) in
     * the Opening Fire step. What a destroyer does do is let you keep your units that were
     * sunk by enemy subs on the battle board until step 6, allowing them to fire back before
     * going to the scrap heap.
     */
    assertEquals(
        BattleStepsTest.mergeSteps(
                BattleStepsTest.firstStrikeFightStepStrings(attackerPlayer, defenderPlayer),
                BattleStepsTest.firstStrikeFightStepStrings(defenderPlayer, attackerPlayer),
                List.of(REMOVE_SNEAK_ATTACK_CASUALTIES),
                BattleStepsTest.generalFightStepStrings(defenderPlayer, attackerPlayer),
                List.of(
                    REMOVE_CASUALTIES,
                    attacker + SUBS_SUBMERGE,
                    attacker + ATTACKER_WITHDRAW,
                    defender + SUBS_SUBMERGE))
            .toString(),
        steps.toString());
    final List<IExecutable> execs = battle.getBattleExecutables();
    final int attackSubs = getIndex(execs, OffensiveFirstStrike.class);
    final int defendSubs = getIndex(execs, DefensiveFirstStrike.class);
    assertTrue(attackSubs < defendSubs);
    givenRemotePlayerWillSelectDefaultCasualties(bridge);
    // attacking subs fires, defending destroyer and sub still gets to fire
    // attacking subs still gets to fire even if defending sub hits
    whenGetRandom(bridge)
        .thenAnswer(withValues(0, 2))
        .thenAnswer(withValues(0))
        .thenAnswer(withValues(0));
    battle.fight(bridge);
    thenGetRandomShouldHaveBeenCalled(bridge, times(3));
    assertTrue(
        attacked
            .getUnitCollection()
            .getMatches(Matches.unitIsOwnedBy(british(gameData)))
            .isEmpty());
    assertEquals(1, attacked.getUnitCollection().size());
  }

  @Test
  void testAttackSubsAndBattleshipOnDestroyerAndSubs() {
    final String defender = "Germans";
    final GamePlayer defenderPlayer = germans(gameData);
    final String attacker = "British";
    final GamePlayer attackerPlayer = british(gameData);
    final Territory attacked = territory("31 Sea Zone", gameData);
    final Territory from = territory("32 Sea Zone", gameData);
    // 1 sub and 1 BB (two hp) attacks 3 subs and 1 destroyer
    addTo(from, submarine(gameData).create(1, british(gameData)));
    addTo(from, battleship(gameData).create(1, british(gameData)));
    addTo(attacked, submarine(gameData).create(3, germans(gameData)));
    addTo(attacked, destroyer(gameData).create(1, germans(gameData)));
    final IDelegateBridge bridge = newDelegateBridge(british(gameData));
    advanceToStep(bridge, "CombatMove");
    moveDelegate(gameData).setDelegateBridgeAndPlayer(bridge);
    moveDelegate(gameData).start();
    move(from.getUnits(), new Route(from, attacked));
    moveDelegate(gameData).end();
    final MustFightBattle battle =
        (MustFightBattle)
            AbstractMoveDelegate.getBattleTracker(gameData).getPendingNonBombingBattle(attacked);
    final List<String> steps = battle.determineStepStrings();
    /*
     * Here are the exact errata clarifications on how REVISED rules subs work:
     * Every sub, regardless of whether it is on the attacking or defending side, fires in the
     * Opening Fire step of combat. That is the only time a sub ever fires.
     * Losses caused by attacking or defending subs are removed at the end of the Opening Fire
     * step, before normal attack and defense rolls, unless the enemy has a destroyer present.
     * If the enemy (attacker or defender) has a destroyer, then hits caused by your subs are
     * not removed until the Remove Casualties step (step 6) of combat.
     * In other words, subs work exactly the same for the attacker and the defender. Nothing,
     * not even a destroyer, ever stops a sub from rolling its die (attack or defense) in the
     * Opening Fire step. What a destroyer does do is let you keep your units that were sunk
     * by enemy subs on the battle board until step 6, allowing them to fire back before
     * going to the scrap heap.
     */
    assertEquals(
        BattleStepsTest.mergeSteps(
                BattleStepsTest.firstStrikeFightStepStrings(attackerPlayer, defenderPlayer),
                BattleStepsTest.firstStrikeFightStepStrings(defenderPlayer, attackerPlayer),
                List.of(REMOVE_SNEAK_ATTACK_CASUALTIES),
                BattleStepsTest.generalFightStepStrings(attackerPlayer, defenderPlayer),
                BattleStepsTest.generalFightStepStrings(defenderPlayer, attackerPlayer),
                List.of(
                    REMOVE_CASUALTIES,
                    attacker + SUBS_SUBMERGE,
                    attacker + ATTACKER_WITHDRAW,
                    defender + SUBS_SUBMERGE))
            .toString(),
        steps.toString());
    final List<IExecutable> execs = battle.getBattleExecutables();
    final int attackSubs = getIndex(execs, OffensiveFirstStrike.class);
    final int defendSubs = getIndex(execs, DefensiveFirstStrike.class);
    assertTrue(attackSubs < defendSubs);
    givenRemotePlayerWillSelectDefaultCasualties(bridge);
    // attacking subs fires, defending destroyer and sub still gets to fire
    // attacking subs still gets to fire even if defending sub hits
    // battleship will not get to fire since it is killed by defending sub's sneak attack
    whenGetRandom(bridge).thenAnswer(withValues(0)).thenAnswer(withValues(0, 0, 0));
    battle.fight(bridge);
    thenGetRandomShouldHaveBeenCalled(bridge, times(2));
    assertTrue(
        attacked
            .getUnitCollection()
            .getMatches(Matches.unitIsOwnedBy(british(gameData)))
            .isEmpty());
    assertEquals(3, attacked.getUnitCollection().size());
  }

  @Test
  void testAttackDestroyerAndSubsAgainstSub() {
    final String defender = "Germans";
    final GamePlayer defenderPlayer = germans(gameData);
    final String attacker = "British";
    final GamePlayer attackerPlayer = british(gameData);
    final Territory attacked = territory("31 Sea Zone", gameData);
    final Territory from = territory("32 Sea Zone", gameData);
    // 1 sub and 1 destroyer attack 1 sub
    // defender sneak attacks, not attacker
    addTo(from, submarine(gameData).create(1, british(gameData)));
    addTo(from, destroyer(gameData).create(1, british(gameData)));
    addTo(attacked, submarine(gameData).create(1, germans(gameData)));
    final IDelegateBridge bridge = newDelegateBridge(british(gameData));
    advanceToStep(bridge, "CombatMove");
    moveDelegate(gameData).setDelegateBridgeAndPlayer(bridge);
    moveDelegate(gameData).start();
    move(from.getUnits(), new Route(from, attacked));
    moveDelegate(gameData).end();
    final MustFightBattle battle =
        (MustFightBattle)
            AbstractMoveDelegate.getBattleTracker(gameData).getPendingNonBombingBattle(attacked);
    final List<String> steps = battle.determineStepStrings();
    assertEquals(
        BattleStepsTest.mergeSteps(
                BattleStepsTest.firstStrikeFightStepStrings(attackerPlayer, defenderPlayer),
                BattleStepsTest.firstStrikeFightStepStrings(defenderPlayer, attackerPlayer),
                List.of(REMOVE_SNEAK_ATTACK_CASUALTIES),
                BattleStepsTest.generalFightStepStrings(attackerPlayer, defenderPlayer),
                List.of(
                    REMOVE_CASUALTIES,
                    attacker + SUBS_SUBMERGE,
                    attacker + ATTACKER_WITHDRAW,
                    defender + SUBS_SUBMERGE))
            .toString(),
        steps.toString());
    final List<IExecutable> execs = battle.getBattleExecutables();
    final int attackSubs = getIndex(execs, OffensiveFirstStrike.class);
    final int defendSubs = getIndex(execs, DefensiveFirstStrike.class);
    assertTrue(attackSubs < defendSubs);
    givenRemotePlayerWillSelectDefaultCasualties(bridge);
    // attacking sub hits with sneak attack, but defending sub gets to return fire because it is a
    // sub and this is
    // revised rules
    whenGetRandom(bridge).thenAnswer(withValues(0)).thenAnswer(withValues(0));
    battle.fight(bridge);
    thenGetRandomShouldHaveBeenCalled(bridge, times(2));
    assertTrue(
        attacked
            .getUnitCollection()
            .getMatches(Matches.unitIsOwnedBy(germans(gameData)))
            .isEmpty());
    assertEquals(1, attacked.getUnitCollection().size());
  }

  @Test
  void testAttackSubsAndDestroyerOnBatleshipAndSubs() {
    final String defender = "Germans";
    final GamePlayer defenderPlayer = germans(gameData);
    final String attacker = "British";
    final GamePlayer attackerPlayer = british(gameData);
    final Territory attacked = territory("31 Sea Zone", gameData);
    final Territory from = territory("32 Sea Zone", gameData);
    // 1 sub and 1 BB (two hp) attacks 3 subs and 1 destroyer
    addTo(from, submarine(gameData).create(3, british(gameData)));
    addTo(from, destroyer(gameData).create(1, british(gameData)));
    addTo(attacked, submarine(gameData).create(1, germans(gameData)));
    addTo(attacked, battleship(gameData).create(1, germans(gameData)));
    final IDelegateBridge bridge = newDelegateBridge(british(gameData));
    advanceToStep(bridge, "CombatMove");
    moveDelegate(gameData).setDelegateBridgeAndPlayer(bridge);
    moveDelegate(gameData).start();
    move(from.getUnits(), new Route(from, attacked));
    moveDelegate(gameData).end();
    final MustFightBattle battle =
        (MustFightBattle)
            AbstractMoveDelegate.getBattleTracker(gameData).getPendingNonBombingBattle(attacked);
    final List<String> steps = battle.determineStepStrings();
    /*
     * Here are the exact errata clarifications on how REVISED rules subs work:
     * Every sub, regardless of whether it is on the attacking or defending side,
     * fires in the Opening Fire step of combat. That is the only time a sub ever fires.
     * Losses caused by attacking or defending subs are removed at the end of the Opening
     * Fire step, before normal attack and defense rolls, unless the enemy has a destroyer present.
     * If the enemy (attacker or defender) has a destroyer, then hits caused by your subs are
     * not removed until the Remove Casualties step (step 6) of combat.
     * In other words, subs work exactly the same for the attacker and the defender. Nothing, not
     * even a destroyer, ever stops a sub from rolling its die (attack or defense) in the Opening
     * Fire step. What a destroyer does do is let you keep your units that were sunk by enemy subs
     * on the battle board until step 6, allowing them to fire back before going to the scrap heap.
     */
    assertEquals(
        BattleStepsTest.mergeSteps(
                BattleStepsTest.firstStrikeFightStepStrings(attackerPlayer, defenderPlayer),
                BattleStepsTest.firstStrikeFightStepStrings(defenderPlayer, attackerPlayer),
                List.of(REMOVE_SNEAK_ATTACK_CASUALTIES),
                BattleStepsTest.generalFightStepStrings(attackerPlayer, defenderPlayer),
                BattleStepsTest.generalFightStepStrings(defenderPlayer, attackerPlayer),
                List.of(
                    REMOVE_CASUALTIES,
                    attacker + SUBS_SUBMERGE,
                    attacker + ATTACKER_WITHDRAW,
                    defender + SUBS_SUBMERGE))
            .toString(),
        steps.toString());
    final List<IExecutable> execs = battle.getBattleExecutables();
    final int attackSubs = getIndex(execs, OffensiveFirstStrike.class);
    final int defendSubs = getIndex(execs, DefensiveFirstStrike.class);
    assertTrue(attackSubs < defendSubs);
    givenRemotePlayerWillSelectDefaultCasualties(bridge);
    // attacking subs fires, defending destroyer and sub still gets to fire
    // attacking subs still gets to fire even if defending sub hits
    // battleship will not get to fire since it is killed by defending sub's sneak attack
    whenGetRandom(bridge).thenAnswer(withValues(0, 0, 0)).thenAnswer(withValues(0));
    battle.fight(bridge);
    thenGetRandomShouldHaveBeenCalled(bridge, times(2));
    assertTrue(
        attacked
            .getUnitCollection()
            .getMatches(Matches.unitIsOwnedBy(germans(gameData)))
            .isEmpty());
    assertEquals(3, attacked.getUnitCollection().size());
  }

  @Test
  void testAttackDestroyerAndSubsAgainstSubAndDestroyer() {
    final String defender = "Germans";
    final GamePlayer defenderPlayer = germans(gameData);
    final String attacker = "British";
    final GamePlayer attackerPlayer = british(gameData);
    final Territory attacked = territory("31 Sea Zone", gameData);
    final Territory from = territory("32 Sea Zone", gameData);
    // 1 sub and 1 destroyer attack 1 sub and 1 destroyer
    // defender sneak attacks, not attacker
    addTo(from, submarine(gameData).create(1, british(gameData)));
    addTo(from, destroyer(gameData).create(1, british(gameData)));
    addTo(attacked, submarine(gameData).create(1, germans(gameData)));
    addTo(attacked, destroyer(gameData).create(1, germans(gameData)));
    final IDelegateBridge bridge = newDelegateBridge(british(gameData));
    advanceToStep(bridge, "CombatMove");
    moveDelegate(gameData).setDelegateBridgeAndPlayer(bridge);
    moveDelegate(gameData).start();
    move(from.getUnits(), new Route(from, attacked));
    moveDelegate(gameData).end();
    final MustFightBattle battle =
        (MustFightBattle)
            AbstractMoveDelegate.getBattleTracker(gameData).getPendingNonBombingBattle(attacked);
    final List<String> steps = battle.determineStepStrings();
    assertEquals(
        BattleStepsTest.mergeSteps(
                BattleStepsTest.firstStrikeFightStepStrings(attackerPlayer, defenderPlayer),
                BattleStepsTest.firstStrikeFightStepStrings(defenderPlayer, attackerPlayer),
                BattleStepsTest.generalFightStepStrings(attackerPlayer, defenderPlayer),
                BattleStepsTest.generalFightStepStrings(defenderPlayer, attackerPlayer),
                List.of(
                    REMOVE_CASUALTIES,
                    attacker + SUBS_SUBMERGE,
                    attacker + ATTACKER_WITHDRAW,
                    defender + SUBS_SUBMERGE))
            .toString(),
        steps.toString());
    final List<IExecutable> execs = battle.getBattleExecutables();
    final int attackSubs = getIndex(execs, OffensiveFirstStrike.class);
    final int defendSubs = getIndex(execs, DefensiveFirstStrike.class);
    assertTrue(attackSubs < defendSubs);
    givenRemotePlayerWillSelectCasualtiesPer(
        bridge,
        invocation -> {
          final Collection<Unit> selectFrom = invocation.getArgument(0);
          return new CasualtyDetails(List.of(selectFrom.iterator().next()), List.of(), false);
        });
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
  void testUnplacedDie() {
    final PlaceDelegate del = placeDelegate(gameData);
    del.setDelegateBridgeAndPlayer(newDelegateBridge(british(gameData)));
    del.start();
    addTo(british(gameData), transport(gameData).create(1, british(gameData)), gameData);
    del.end();
    // unplaced units die
    assertTrue(british(gameData).getUnitCollection().isEmpty());
  }

  @Test
  void testRocketsDontFireInConquered() {
    final MoveDelegate move = moveDelegate(gameData);
    final IDelegateBridge bridge = newDelegateBridge(germans(gameData));
    advanceToStep(bridge, "CombatMove");
    move.setDelegateBridgeAndPlayer(bridge);
    move.start();
    // remove the russians units in caucasus so we can blitz
    final Territory cauc = territory("Caucasus", gameData);
    removeFrom(cauc, cauc.getUnitCollection().getMatches(Matches.unitIsNotAa()));
    // blitz
    final Territory wr = territory("West Russia", gameData);
    move(wr.getUnitCollection().getMatches(Matches.unitCanBlitz()), new Route(wr, cauc));
    final Set<Territory> fire =
        RocketsFireHelper.getTerritoriesWithRockets(gameData, germans(gameData));
    // germany, WE, SE, but not caucusus
    assertEquals(3, fire.size());
  }

  @Test
  void testTechRolls() {
    // Set up the test
    final GamePlayer germans = GameDataTestUtil.germans(gameData);
    final IDelegateBridge delegateBridge = newDelegateBridge(germans);
    advanceToStep(delegateBridge, "germanTech");
    final TechnologyDelegate techDelegate = techDelegate(gameData);
    techDelegate.setDelegateBridgeAndPlayer(delegateBridge);
    techDelegate.start();
    final TechAttachment ta = germans.getTechAttachment();
    // PlayerAttachment pa = PlayerAttachment.get(germans);
    final TechnologyFrontier rockets = new TechnologyFrontier("", gameData);
    rockets.addAdvance(
        TechAdvance.findAdvance(
            TechAdvance.TECH_PROPERTY_ROCKETS, gameData.getTechnologyFrontier(), null));
    final TechnologyFrontier jet = new TechnologyFrontier("", gameData);
    jet.addAdvance(
        TechAdvance.findAdvance(
            TechAdvance.TECH_PROPERTY_JET_POWER, gameData.getTechnologyFrontier(), null));
    // Check to make sure it was successful
    final int initPUs = germans.getResources().getQuantity("PUs");
    whenGetRandom(delegateBridge)
        .thenAnswer(withValues(3, 4)) // Fail the roll
        .thenAnswer(withValues(5)) // Make a Successful roll
        .thenAnswer(withValues(5)); // Make a Successful roll
    // Fail the roll
    final TechResults roll = techDelegate.rollTech(2, rockets, 0, null);
    // Check to make sure it failed
    assertEquals(0, roll.getHits());
    final int midPUs = germans.getResources().getQuantity("PUs");
    assertEquals(initPUs - 10, midPUs);
    // Make a Successful roll
    final TechResults roll2 = techDelegate.rollTech(1, rockets, 0, null);
    // Check to make sure it succeeded
    assertEquals(1, roll2.getHits());
    final int finalPUs = germans.getResources().getQuantity("PUs");
    assertEquals(midPUs - 5, finalPUs);
    // Test the variable tech cost
    // Make a Successful roll
    ta.setTechCost("6");
    final TechResults roll3 = techDelegate.rollTech(1, jet, 0, null);
    // Check to make sure it succeeded
    assertEquals(1, roll3.getHits());
    final int variablePus = germans.getResources().getQuantity("PUs");
    assertEquals(finalPUs - 6, variablePus);
  }

  @Test
  void testTransportsUnloadingToMultipleTerritoriesDie() {
    // two transports enter a battle, but drop off
    // their units to two allied territories before
    // the begin the battle
    // the units they drop off should die with the transports
    final GamePlayer germans = germans(gameData);
    final GamePlayer british = british(gameData);
    final Territory sz6 = territory("6 Sea Zone", gameData);
    final Territory sz5 = territory("5 Sea Zone", gameData);
    final Territory germany = territory("Germany", gameData);
    final Territory norway = territory("Norway", gameData);
    final Territory we = territory("Western Europe", gameData);
    final Territory uk = territory("United Kingdom", gameData);
    addTo(sz6, destroyer(gameData).create(2, british));
    addTo(sz5, transport(gameData).create(3, germans));
    addTo(germany, armour(gameData).create(3, germans));
    final IDelegateBridge bridge = newDelegateBridge(germans(gameData));
    advanceToStep(bridge, "CombatMove");
    givenRemotePlayerWillSelectDefaultCasualties(bridge);
    moveDelegate(gameData).setDelegateBridgeAndPlayer(bridge);
    moveDelegate(gameData).start();
    // load two transports, 1 tank each
    load(
        germany.getUnitCollection().getMatches(Matches.unitCanBlitz()).subList(0, 1),
        new Route(germany, sz5));
    load(
        germany.getUnitCollection().getMatches(Matches.unitCanBlitz()).subList(0, 1),
        new Route(germany, sz5));
    load(
        germany.getUnitCollection().getMatches(Matches.unitCanBlitz()).subList(0, 1),
        new Route(germany, sz5));
    // attack sz 6
    move(
        sz5.getUnitCollection().getMatches(Matches.unitCanBlitz().or(Matches.unitIsSeaTransport())),
        new Route(sz5, sz6));
    // unload transports, 1 each to a different country
    // this move is illegal now
    assertMoveError(
        sz6.getUnitCollection().getMatches(Matches.unitCanBlitz()).subList(0, 1),
        new Route(sz6, norway));
    // this move is illegal now
    assertMoveError(
        sz6.getUnitCollection().getMatches(Matches.unitCanBlitz()).subList(0, 1),
        new Route(sz6, we));
    move(
        sz6.getUnitCollection().getMatches(Matches.unitCanBlitz()).subList(0, 1),
        new Route(sz6, uk));
    // fight the battle
    moveDelegate(gameData).end();
    final IBattle battle =
        AbstractMoveDelegate.getBattleTracker(gameData).getPendingNonBombingBattle(sz6);
    // everything hits, this will kill both transports
    whenGetRandom(bridge).thenAnswer(withValues(0, 0)).thenAnswer(withValues(0, 0));
    battle.fight(bridge);
    // the armour should have died
    assertEquals(0, norway.getUnitCollection().countMatches(Matches.unitCanBlitz()));
    assertEquals(2, we.getUnitCollection().countMatches(Matches.unitCanBlitz()));
    assertEquals(0, uk.getUnitCollection().countMatches(Matches.unitIsOwnedBy(germans)));
  }

  @Test
  void testCanalMovePass() {
    final Territory sz15 = territory("15 Sea Zone", gameData);
    final Territory sz34 = territory("34 Sea Zone", gameData);
    final IDelegateBridge bridge = newDelegateBridge(british(gameData));
    advanceToStep(bridge, "CombatMove");
    final MoveDelegate moveDelegate = moveDelegate(gameData);
    moveDelegate.setDelegateBridgeAndPlayer(bridge);
    moveDelegate.start();
    final Optional<String> error = moveDelegate.move(sz15.getUnits(), new Route(sz15, sz34));
    assertValid(error);
  }

  @Test
  void testCanalMovementFail() {
    final Territory sz14 = territory("14 Sea Zone", gameData);
    final Territory sz15 = territory("15 Sea Zone", gameData);
    final Territory sz34 = territory("34 Sea Zone", gameData);
    // clear the british in sz 15
    removeFrom(sz15, sz15.getUnits());
    final IDelegateBridge bridge = newDelegateBridge(germans(gameData));
    advanceToStep(bridge, "CombatMove");
    final MoveDelegate moveDelegate = moveDelegate(gameData);
    moveDelegate.setDelegateBridgeAndPlayer(bridge);
    moveDelegate.start();
    final Optional<String> error = moveDelegate.move(sz14.getUnits(), new Route(sz14, sz15, sz34));
    assertError(error);
  }

  @Test
  void testTransportIsTransport() {
    assertTrue(Matches.unitIsSeaTransport().test(transport(gameData).create(british(gameData))));
    assertFalse(Matches.unitIsSeaTransport().test(infantry(gameData).create(british(gameData))));
  }
}
