package games.strategy.triplea.delegate;

import static games.strategy.triplea.delegate.GameDataTestUtil.addTo;
import static games.strategy.triplea.delegate.MockDelegateBridge.advanceToStep;
import static games.strategy.triplea.delegate.MockDelegateBridge.newDelegateBridge;
import static games.strategy.triplea.delegate.MockDelegateBridge.whenGetRandom;
import static games.strategy.triplea.delegate.MockDelegateBridge.withValues;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.battle.BattleTracker;
import games.strategy.triplea.delegate.battle.IBattle;
import games.strategy.triplea.delegate.battle.StrategicBombingRaidBattle;
import games.strategy.triplea.settings.AbstractClientSettingTestCase;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class LhtrTest extends AbstractClientSettingTestCase {
  private final GameData gameData = TestMapGameData.LHTR.getGameData();

  private static void thenRemotePlayerShouldNeverBeAskedToConfirmMove(
      final IDelegateBridge delegateBridge) {
    verify(delegateBridge.getRemotePlayer(), never()).confirmMoveInFaceOfAa(any());
    verify(delegateBridge.getRemotePlayer(), never()).confirmMoveKamikaze();
  }

  @Test
  void testFightersCanLandOnNewPlacedCarrier() {
    final MoveDelegate delegate = (MoveDelegate) gameData.getDelegate("move");
    delegate.initialize("MoveDelegate", "MoveDelegate");
    final GamePlayer germans = GameDataTestUtil.germans(gameData);
    final IDelegateBridge bridge = newDelegateBridge(germans);
    advanceToStep(bridge, "germanNonCombatMove");
    delegate.setDelegateBridgeAndPlayer(bridge);
    delegate.start();
    final Territory baltic = gameData.getMap().getTerritory("5 Sea Zone");
    final Territory easternEurope = gameData.getMap().getTerritory("Eastern Europe");
    final UnitType carrirType = GameDataTestUtil.carrier(gameData);
    // move a fighter to the baltic
    final Route route = new Route(easternEurope, baltic);
    final UnitType fighterType = GameDataTestUtil.fighter(gameData);
    delegate.move(
        easternEurope.getUnitCollection().getMatches(Matches.unitIsOfType(fighterType)), route);
    // add a carrier to be produced in germany
    final Unit carrier = new Unit(carrirType, germans, gameData);
    gameData.performChange(ChangeFactory.addUnits(germans, Set.of(carrier)));
    // end the move phase
    delegate.end();
    // make sure the fighter is still there
    // in lhtr fighters can hover, and carriers placed beneath them
    assertTrue(baltic.anyUnitsMatch(Matches.unitIsOfType(fighterType)));
  }

  @Test
  void testFightersDestroyedWhenNoPendingCarriers() {
    final MoveDelegate delegate = (MoveDelegate) gameData.getDelegate("move");
    delegate.initialize("MoveDelegate", "MoveDelegate");
    final GamePlayer germans = GameDataTestUtil.germans(gameData);
    final IDelegateBridge bridge = newDelegateBridge(germans);
    advanceToStep(bridge, "germanNonCombatMove");
    delegate.setDelegateBridgeAndPlayer(bridge);
    delegate.start();
    final Territory baltic = gameData.getMap().getTerritory("5 Sea Zone");
    final Territory easternEurope = gameData.getMap().getTerritory("Eastern Europe");
    // move a fighter to the baltic
    final Route route = new Route(easternEurope, baltic);
    final UnitType fighterType = GameDataTestUtil.fighter(gameData);
    delegate.move(
        easternEurope.getUnitCollection().getMatches(Matches.unitIsOfType(fighterType)), route);
    // end the move phase
    delegate.end();
    // there is no pending carrier to be placed
    // the fighter cannot hover
    assertFalse(baltic.anyUnitsMatch(Matches.unitIsOfType(fighterType)));
  }

  @Test
  void testAaGunsDontFireNonCombat() {
    final MoveDelegate delegate = (MoveDelegate) gameData.getDelegate("move");
    delegate.initialize("MoveDelegate", "MoveDelegate");
    final GamePlayer germans = GameDataTestUtil.germans(gameData);
    final IDelegateBridge bridge = newDelegateBridge(germans);
    advanceToStep(bridge, "germanNonCombatMove");
    delegate.setDelegateBridgeAndPlayer(bridge);
    delegate.start();
    // move 1 fighter over the aa gun in caucus
    final Route route =
        new Route(
            gameData.getMap().getTerritory("Ukraine S.S.R."),
            gameData.getMap().getTerritory("Caucasus"),
            gameData.getMap().getTerritory("West Russia"));
    final List<Unit> fighter = route.getStart().getUnitCollection().getMatches(Matches.unitIsAir());
    delegate.move(fighter, route);
    // if we try to move aa, then the game will ask us if we want to move
    thenRemotePlayerShouldNeverBeAskedToConfirmMove(bridge);
  }

  @Test
  void testSubDefenseBonus() {
    final UnitType sub = GameDataTestUtil.submarine(gameData);
    final UnitAttachment attachment = sub.getUnitAttachment();
    final GamePlayer japanese = GameDataTestUtil.japanese(gameData);
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
    assertEquals(3, attachment.getDefense(japanese));
    assertEquals(3, attachment.getAttack(japanese));
    // make sure this only changes for the player with the tech
    final GamePlayer americans = GameDataTestUtil.americans(gameData);
    assertEquals(2, attachment.getDefense(americans));
    assertEquals(2, attachment.getAttack(americans));
  }

  @Test
  void testLhtrBombingRaid() {
    final Territory germany = gameData.getMap().getTerritory("Germany");
    final Territory uk = gameData.getMap().getTerritory("United Kingdom");
    final GamePlayer germans = GameDataTestUtil.germans(gameData);
    final GamePlayer british = GameDataTestUtil.british(gameData);
    final BattleTracker tracker = new BattleTracker();
    final IBattle battle = new StrategicBombingRaidBattle(germany, gameData, british, tracker);
    battle.addAttackChange(
        gameData.getMap().getRoute(uk, germany, it -> true),
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
    // aa guns rolls 3, misses, bomber rolls 2 dice at 3 and 4
    whenGetRandom(bridge).thenAnswer(withValues(2)).thenAnswer(withValues(2, 3));
    final int pusBeforeRaid =
        germans
            .getResources()
            .getQuantity(gameData.getResourceList().getResource(Constants.PUS).orElse(null));
    battle.fight(bridge);
    final int pusAfterRaid =
        germans
            .getResources()
            .getQuantity(gameData.getResourceList().getResource(Constants.PUS).orElse(null));
    // targets dice is 4, so damage is 1 + 4 = 5
    // Changed to match StrategicBombingRaidBattle changes
    assertEquals(pusBeforeRaid - 5, pusAfterRaid);
  }

  @Test
  void testLhtrBombingRaid2Bombers() {
    final Territory germany = gameData.getMap().getTerritory("Germany");
    final Territory uk = gameData.getMap().getTerritory("United Kingdom");
    final GamePlayer germans = GameDataTestUtil.germans(gameData);
    final GamePlayer british = GameDataTestUtil.british(gameData);
    // add a unit
    final Unit bomber = GameDataTestUtil.bomber(gameData).create(british);
    final Change change = ChangeFactory.addUnits(uk, Set.of(bomber));
    gameData.performChange(change);
    final BattleTracker tracker = new BattleTracker();
    final IBattle battle = new StrategicBombingRaidBattle(germany, gameData, british, tracker);
    battle.addAttackChange(
        gameData.getMap().getRoute(uk, germany, it -> true),
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
    // aa guns rolls 3,3 both miss, bomber 1 rolls 2 dice at 3,4 and bomber 2 rolls dice at 1,2
    whenGetRandom(bridge).thenAnswer(withValues(3, 3)).thenAnswer(withValues(2, 3, 0, 1));
    final int pusBeforeRaid =
        germans
            .getResources()
            .getQuantity(gameData.getResourceList().getResource(Constants.PUS).orElse(null));
    battle.fight(bridge);
    final int pusAfterRaid =
        germans
            .getResources()
            .getQuantity(gameData.getResourceList().getResource(Constants.PUS).orElse(null));
    // targets dice is 4, so damage is 1 + 4 = 5
    // bomber 2 hits at 2, so damage is 3, for a total of 8
    // Changed to match StrategicBombingRaidBattle changes
    assertEquals(pusBeforeRaid - 8, pusAfterRaid);
  }
}
