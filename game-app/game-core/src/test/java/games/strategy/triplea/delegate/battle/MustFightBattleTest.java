package games.strategy.triplea.delegate.battle;

import static games.strategy.triplea.delegate.GameDataTestUtil.addTo;
import static games.strategy.triplea.delegate.GameDataTestUtil.artillery;
import static games.strategy.triplea.delegate.GameDataTestUtil.battleDelegate;
import static games.strategy.triplea.delegate.GameDataTestUtil.britain;
import static games.strategy.triplea.delegate.GameDataTestUtil.british;
import static games.strategy.triplea.delegate.GameDataTestUtil.britishArtillery;
import static games.strategy.triplea.delegate.GameDataTestUtil.britishInfantry;
import static games.strategy.triplea.delegate.GameDataTestUtil.carrier;
import static games.strategy.triplea.delegate.GameDataTestUtil.chinese;
import static games.strategy.triplea.delegate.GameDataTestUtil.fighter;
import static games.strategy.triplea.delegate.GameDataTestUtil.french;
import static games.strategy.triplea.delegate.GameDataTestUtil.germanBattleship;
import static games.strategy.triplea.delegate.GameDataTestUtil.germans;
import static games.strategy.triplea.delegate.GameDataTestUtil.infantry;
import static games.strategy.triplea.delegate.GameDataTestUtil.japan;
import static games.strategy.triplea.delegate.GameDataTestUtil.japaneseInfantry;
import static games.strategy.triplea.delegate.GameDataTestUtil.move;
import static games.strategy.triplea.delegate.GameDataTestUtil.moveDelegate;
import static games.strategy.triplea.delegate.GameDataTestUtil.removeFrom;
import static games.strategy.triplea.delegate.GameDataTestUtil.territory;
import static games.strategy.triplea.delegate.GameDataTestUtil.transport;
import static games.strategy.triplea.delegate.GameDataTestUtil.unitType;
import static games.strategy.triplea.delegate.MockDelegateBridge.advanceToStep;
import static games.strategy.triplea.delegate.MockDelegateBridge.newDelegateBridge;
import static games.strategy.triplea.delegate.MockDelegateBridge.thenGetRandomShouldHaveBeenCalled;
import static games.strategy.triplea.delegate.MockDelegateBridge.whenGetRandom;
import static games.strategy.triplea.delegate.MockDelegateBridge.withDiceValues;
import static games.strategy.triplea.delegate.MockDelegateBridge.withValues;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.in;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.properties.IEditableProperty;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.display.IDisplay;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attachments.UnitSupportAttachment;
import games.strategy.triplea.delegate.AbstractMoveDelegate;
import games.strategy.triplea.delegate.GameDataTestUtil;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.MoveDelegate;
import games.strategy.triplea.settings.AbstractClientSettingTestCase;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.triplea.java.collections.CollectionUtils;

class MustFightBattleTest extends AbstractClientSettingTestCase {
  @Test
  void testFightWithIsSuicideOnHit() {
    final GameData twwGameData = TestMapGameData.TWW.getGameData();

    // Create battle with 1 cruiser attacking 1 mine
    final GamePlayer usa = GameDataTestUtil.usa(twwGameData);
    final GamePlayer germany = GameDataTestUtil.germany(twwGameData);
    final Territory sz33 = territory("33 Sea Zone", twwGameData);
    addTo(sz33, GameDataTestUtil.americanCruiser(twwGameData).create(1, usa));
    final Territory sz40 = territory("40 Sea Zone", twwGameData);
    addTo(sz40, GameDataTestUtil.germanMine(twwGameData).create(1, germany));

    final IDelegateBridge bridge = performCombatMove(usa, sz33.getUnits(), new Route(sz33, sz40));
    final IBattle battle =
        AbstractMoveDelegate.getBattleTracker(twwGameData).getPendingNonBombingBattle(sz40);
    assertNotNull(battle);

    // Set first roll to hit (mine AA) and check that both units are killed
    whenGetRandom(bridge).thenAnswer(withValues(0));
    battle.fight(bridge);
    assertEquals(0, sz40.getUnitCollection().size());
    thenGetRandomShouldHaveBeenCalled(bridge, times(1));
  }

  @Test
  void testFightWithBothZeroStrength() {
    final GameData twwGameData = TestMapGameData.TWW.getGameData();

    // Create TWW battle in Celebes with 1 inf attacking 1 strat where both have 0 strength
    final GamePlayer usa = GameDataTestUtil.usa(twwGameData);
    final GamePlayer germany = GameDataTestUtil.germany(twwGameData);
    final Territory celebes = territory("Celebes", twwGameData);
    celebes.getUnitCollection().clear();
    addTo(celebes, GameDataTestUtil.americanStrategicBomber(twwGameData).create(1, usa));
    addTo(celebes, GameDataTestUtil.germanInfantry(twwGameData).create(1, germany));
    final IDelegateBridge bridge = newDelegateBridge(germany);
    battleDelegate(twwGameData).setDelegateBridgeAndPlayer(bridge);
    BattleDelegate.doInitialize(battleDelegate(twwGameData).getBattleTracker(), bridge);
    final IBattle battle =
        AbstractMoveDelegate.getBattleTracker(twwGameData).getPendingNonBombingBattle(celebes);
    assertNotNull(battle);

    // Ensure battle ends, both units remain, and has 0 rolls
    battle.fight(bridge);
    assertEquals(2, celebes.getUnitCollection().size());
    thenGetRandomShouldHaveBeenCalled(bridge, times(0));
  }

  @Test
  void testCasualtyDefendersProvideSupport() throws Exception {
    final GameData gameData = TestMapGameData.TWW.getGameData();
    setPropertyValue(gameData, Constants.LOW_LUCK, false);

    // Add a support rule to make british artillery provide an extra die to british infantry.
    var supportAttachment =
        new UnitSupportAttachment(
                Constants.SUPPORT_ATTACHMENT_PREFIX + "Test", britishArtillery(gameData), gameData)
            .setSide("defence")
            .setFaction("allied")
            .setPlayers(List.of(britain(gameData)))
            .setUnitType(Set.of(britishInfantry(gameData)))
            .setBonusType("bonus")
            .setBonus(1)
            .setDice("roll")
            .setNumber(1);
    britishArtillery(gameData).addAttachment(supportAttachment.getName(), supportAttachment);

    // Set up an attack by 2 japanese infantry into 1 british artillery and 1 british infantry.
    final Territory indoChina = territory("French Indochina", gameData);
    removeFrom(indoChina, indoChina.getUnits());
    addTo(indoChina, japaneseInfantry(gameData).create(2, japan(gameData)));
    final Territory burma = territory("Burma", gameData);
    removeFrom(burma, burma.getUnits());
    addTo(burma, britishArtillery(gameData).create(1, britain(gameData)));
    addTo(burma, britishInfantry(gameData).create(1, britain(gameData)));

    final Collection<Unit> attackers = List.copyOf(indoChina.getUnits());
    final IDelegateBridge bridge =
        performCombatMove(japan(gameData), attackers, new Route(indoChina, burma));

    final IBattle battle =
        AbstractMoveDelegate.getBattleTracker(gameData).getPendingNonBombingBattle(burma);
    assertNotNull(battle);

    // Attacking infantry roll two dice (both hit, killing the defenders).
    // Defenders should roll 3 dice, via support attachment, even if they got killed by attackers.
    // Note: This verifies that this exact number of dice are requested.
    whenGetRandom(bridge).thenAnswer(withDiceValues(1, 1)).thenAnswer(withDiceValues(6, 6, 6));
    battle.fight(bridge);
    // Attackers killed the two defenders, while defenders failed to hit anything.
    assertThat(burma.getUnits(), containsInAnyOrder(attackers.toArray()));
  }

  @Test
  void testStepNamesChangeDuringCombat() {
    final GameData gameData = TestMapGameData.VICTORY_TEST.getGameData();
    setPropertyValue(gameData, Constants.LOW_LUCK, false);
    setPropertyValue(gameData, Constants.LL_AA_ONLY, false);

    // Set up a chinese attack with infantry + fighter against a japanese infantry + artillery.
    // The units on the test map have been modified as follows:
    //   fighter has an offensive "AA" attack that can hit an artillery only.
    //   artillery has canNotTarget="fighter"
    //
    // We're testing the scenario where casualty steps will change after initial step names are
    // generated. Initially, since defenders can target different units, there will be multiple
    // casualty steps for attackers corresponding to the defending units dice roll steps.
    // But then, when an artillery will die due to offensive AA and not get a counterattack, the
    // casualty steps will become a single one. We're testing that this won't hit an error.
    // The error itself ("Could not find step name:") would typically happen in the UI code via
    // TripleADisplay, but this test can only validate that all invocations of notifyDice() pass
    // a valid step name (that exists in the MustFightBattle's stepNames list).

    final Territory china = territory("China", gameData);
    removeFrom(china, china.getUnits());
    addTo(china, infantry(gameData).create(1, chinese(gameData)));
    addTo(china, fighter(gameData).create(1, chinese(gameData)));
    // We'll need 3 fuel to move the fighter...
    final Resource fuel = gameData.getResourceList().getResource("Fuel").orElse(null);
    chinese(gameData).getResources().addResource(fuel, 3);

    final Territory indoChina = territory("French Indochina", gameData);
    removeFrom(indoChina, indoChina.getUnits());
    addTo(indoChina, infantry(gameData).create(1, japan(gameData)));
    addTo(indoChina, artillery(gameData).create(1, japan(gameData)));

    final Collection<Unit> attackers = List.copyOf(china.getUnits());
    final IDelegateBridge bridge =
        performCombatMove(chinese(gameData), attackers, new Route(china, indoChina));

    final IBattle battle =
        AbstractMoveDelegate.getBattleTracker(gameData).getPendingNonBombingBattle(indoChina);
    assertNotNull(battle);

    whenGetRandom(bridge)
        // First, there's an offensive AA roll by the fighter (hit).
        .thenAnswer(withDiceValues(1))
        // Then regular combat rolls, one by fighter and another by infantry.
        .thenAnswer(withDiceValues(1, 1))
        // Then the casualty infantry rolls.
        .thenAnswer(withDiceValues(6));

    // Ensure that all step names passed to notifyDice() exist in the battle's stepStrings.
    // This verifies what the real TripleADisplay's notifyDice() does (since it tries to select a
    // step in the UI created from stepStrings).
    // This verifies the MustFightBattle.findStepNameForFiringUnits() logic (and its caller) is able
    // to find the appropriate step even when the set of step names changes mid-battle.
    IDisplay display = bridge.getDisplayChannelBroadcaster();
    doAnswer(
            invocation -> {
              String stepName = invocation.getArgument(1);
              List<String> stepNames = ((MustFightBattle) battle).getStepStrings();
              assertThat(stepName, in(stepNames));
              return null;
            })
        .when(display)
        .notifyDice(any(), anyString());

    battle.fight(bridge);
    assertThat(indoChina.getUnits(), containsInAnyOrder(attackers.toArray()));
  }

  @Test
  void testAlliedCarriedPlanesTransportedByIsResetWhenCancelingBattle() {
    // Note: Test uses germans, british and france since other countries aren't at war on t1.
    final GameData gameData = TestMapGameData.GLOBAL1940.getGameData();

    // SZ 45 has a german transport.
    final Territory sz45 = territory("45 Sea Zone", gameData);
    removeFrom(sz45, sz45.getUnits());
    addTo(sz45, transport(gameData).create(1, germans(gameData)));

    // SZ 46 has a british carrier with 2 french planes.
    final Territory sz46 = territory("46 Sea Zone", gameData);
    removeFrom(sz46, sz46.getUnits());
    Unit carrier = carrier(gameData).create(1, british(gameData)).get(0);
    addTo(sz46, List.of(carrier));
    List<Unit> fighters = fighter(gameData).create(2, french(gameData));
    addTo(sz46, fighters);

    final Territory sz42 = territory("42 Sea Zone", gameData);

    final IDelegateBridge bridge = newDelegateBridge(british(gameData));
    advanceToStep(bridge, "CombatMove");
    MoveDelegate moveDelegate = GameDataTestUtil.moveDelegate(gameData);
    moveDelegate.setDelegateBridgeAndPlayer(bridge);
    moveDelegate.start();

    Collection<Unit> units = List.of(carrier, fighters.get(0), fighters.get(1));
    // For the battle, transportedBy will be set to the carrier so that it's shown in the UI and
    // the units destroyed if the carrier is sunk.
    GameDataTestUtil.move(units, new Route(sz46, sz45));
    assertThat(fighters.get(0).getTransportedBy(), is(carrier));
    assertThat(fighters.get(1).getTransportedBy(), is(carrier));
    // But if the units move out, then transportedBy should be cleared.
    GameDataTestUtil.move(units, new Route(sz45, sz42));
    assertThat(fighters.get(0).getTransportedBy(), is(nullValue()));
    assertThat(fighters.get(1).getTransportedBy(), is(nullValue()));
  }

  @Test
  void testCantMoveAfterHitTransform() {
    final GameData gameData = TestMapGameData.TWW.getGameData();
    final Territory sz23 = territory("23 Sea Zone", gameData);
    removeFrom(sz23, sz23.getUnits());
    final Territory sz25 = territory("25 Sea Zone", gameData);
    removeFrom(sz25, sz25.getUnits());

    addTo(sz25, germanBattleship(gameData).create(1, GameDataTestUtil.germany(gameData)));
    addTo(sz23, unitType("americanDestroyer", gameData).create(1, GameDataTestUtil.usa(gameData)));

    final Collection<Unit> attackers = List.copyOf(sz25.getUnits());
    final IDelegateBridge bridge =
        performCombatMove(GameDataTestUtil.germany(gameData), attackers, new Route(sz25, sz23));

    final IBattle battle =
        AbstractMoveDelegate.getBattleTracker(gameData).getPendingNonBombingBattle(sz23);
    assertNotNull(battle);
    // Attacking battleship rolls a die with a hit, killing the destroyer.
    // Defenders should roll a die with a hit, damaging the battleship.
    whenGetRandom(bridge).thenAnswer(withDiceValues(1)).thenAnswer(withDiceValues(1));
    battle.fight(bridge);
    // The unit left should be the damaged german battleship.
    assertEquals(1, sz23.getUnits().size());
    final Unit unit = CollectionUtils.getAny(sz23.getUnits());
    assertEquals("germanBattleship-damaged", unit.getType().getName());
    // And it should have no movement left.
    assertFalse(Matches.unitHasMovementLeft().test(unit));

    // And just to double check, we can't move it.
    advanceToNonCombatMove(bridge);
    GameDataTestUtil.assertMoveError(sz23.getUnits(), new Route(sz23, sz25));
  }

  private static <T> void setPropertyValue(GameData gameData, String propertyName, T value) {
    IEditableProperty<T> property =
        (IEditableProperty<T>)
            gameData.getProperties().getEditablePropertiesByName().get(propertyName);
    property.setValue(value);
  }

  private void advanceToNonCombatMove(IDelegateBridge bridge) {
    advanceToStep(bridge, "NonCombatMove");
    moveDelegate(bridge.getData()).setDelegateBridgeAndPlayer(bridge);
    moveDelegate(bridge.getData()).start();
  }

  private IDelegateBridge performCombatMove(
      GamePlayer player, Collection<Unit> units, Route route) {
    final IDelegateBridge bridge = newDelegateBridge(player);
    advanceToStep(bridge, "CombatMove");
    moveDelegate(player.getData()).setDelegateBridgeAndPlayer(bridge);
    moveDelegate(player.getData()).start();
    move(units, route);
    moveDelegate(player.getData()).end();
    return bridge;
  }
}
