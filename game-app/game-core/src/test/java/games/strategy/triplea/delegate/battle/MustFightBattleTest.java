package games.strategy.triplea.delegate.battle;

import static games.strategy.triplea.delegate.GameDataTestUtil.addTo;
import static games.strategy.triplea.delegate.GameDataTestUtil.battleDelegate;
import static games.strategy.triplea.delegate.GameDataTestUtil.britain;
import static games.strategy.triplea.delegate.GameDataTestUtil.britishArtillery;
import static games.strategy.triplea.delegate.GameDataTestUtil.britishInfantry;
import static games.strategy.triplea.delegate.GameDataTestUtil.japan;
import static games.strategy.triplea.delegate.GameDataTestUtil.japaneseInfantry;
import static games.strategy.triplea.delegate.GameDataTestUtil.move;
import static games.strategy.triplea.delegate.GameDataTestUtil.moveDelegate;
import static games.strategy.triplea.delegate.GameDataTestUtil.removeFrom;
import static games.strategy.triplea.delegate.GameDataTestUtil.territory;
import static games.strategy.triplea.delegate.MockDelegateBridge.advanceToStep;
import static games.strategy.triplea.delegate.MockDelegateBridge.newDelegateBridge;
import static games.strategy.triplea.delegate.MockDelegateBridge.thenGetRandomShouldHaveBeenCalled;
import static games.strategy.triplea.delegate.MockDelegateBridge.whenGetRandom;
import static games.strategy.triplea.delegate.MockDelegateBridge.withDiceValues;
import static games.strategy.triplea.delegate.MockDelegateBridge.withValues;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.times;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.properties.IEditableProperty;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attachments.UnitSupportAttachment;
import games.strategy.triplea.delegate.AbstractMoveDelegate;
import games.strategy.triplea.delegate.GameDataTestUtil;
import games.strategy.triplea.settings.AbstractClientSettingTestCase;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

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
        AbstractMoveDelegate.getBattleTracker(twwGameData).getPendingBattle(sz40);
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
        AbstractMoveDelegate.getBattleTracker(twwGameData).getPendingBattle(celebes);
    assertNotNull(battle);

    // Ensure battle ends, both units remain, and has 0 rolls
    battle.fight(bridge);
    assertEquals(2, celebes.getUnitCollection().size());
    thenGetRandomShouldHaveBeenCalled(bridge, times(0));
  }

  @Test
  void testCasualtyDefendersProvideSupport() throws Exception {
    final GameData gameData = TestMapGameData.TWW.getGameData();
    IEditableProperty<Boolean> lowLuck =
        (IEditableProperty<Boolean>)
            gameData.getProperties().getEditablePropertiesByName().get(Constants.LOW_LUCK);
    lowLuck.setValue(false);

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

    final IBattle battle = AbstractMoveDelegate.getBattleTracker(gameData).getPendingBattle(burma);
    assertNotNull(battle);

    // Attacking infantry roll two dice (both hit, killing the defenders).
    // Defenders should roll 3 dice, via support attachment, even if they got killed by attackers.
    // Note: This verifies that this exact number of dice are requested.
    whenGetRandom(bridge).thenAnswer(withDiceValues(1, 1)).thenAnswer(withDiceValues(6, 6, 6));
    battle.fight(bridge);
    // Attackers killed the two defenders, while defenders failed to hit anything.
    assertThat(burma.getUnits(), containsInAnyOrder(attackers.toArray()));
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
