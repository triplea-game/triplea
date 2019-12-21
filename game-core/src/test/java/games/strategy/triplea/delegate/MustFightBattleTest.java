package games.strategy.triplea.delegate;

import static games.strategy.triplea.delegate.GameDataTestUtil.addTo;
import static games.strategy.triplea.delegate.GameDataTestUtil.battleDelegate;
import static games.strategy.triplea.delegate.GameDataTestUtil.move;
import static games.strategy.triplea.delegate.GameDataTestUtil.moveDelegate;
import static games.strategy.triplea.delegate.GameDataTestUtil.territory;
import static games.strategy.triplea.delegate.MockDelegateBridge.advanceToStep;
import static games.strategy.triplea.delegate.MockDelegateBridge.newDelegateBridge;
import static games.strategy.triplea.delegate.MockDelegateBridge.thenGetRandomShouldHaveBeenCalled;
import static games.strategy.triplea.delegate.MockDelegateBridge.whenGetRandom;
import static games.strategy.triplea.delegate.MockDelegateBridge.withValues;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerId;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.xml.TestMapGameData;
import org.junit.jupiter.api.Test;

class MustFightBattleTest extends AbstractDelegateTestCase {
  @Test
  void testFightWithIsSuicideOnHit() {
    final GameData twwGameData = TestMapGameData.TWW.getGameData();

    // Create battle with 1 cruiser attacking 1 mine
    final PlayerId usa = GameDataTestUtil.usa(twwGameData);
    final PlayerId germany = GameDataTestUtil.germany(twwGameData);
    final Territory sz33 = territory("33 Sea Zone", twwGameData);
    addTo(sz33, GameDataTestUtil.americanCruiser(twwGameData).create(1, usa));
    final Territory sz40 = territory("40 Sea Zone", twwGameData);
    addTo(sz40, GameDataTestUtil.germanMine(twwGameData).create(1, germany));
    final IDelegateBridge bridge = newDelegateBridge(usa);
    advanceToStep(bridge, "CombatMove");
    moveDelegate(twwGameData).setDelegateBridgeAndPlayer(bridge);
    moveDelegate(twwGameData).start();
    move(sz33.getUnits(), new Route(sz33, sz40));
    moveDelegate(twwGameData).end();
    final IBattle battle =
        AbstractMoveDelegate.getBattleTracker(twwGameData).getPendingBattle(sz40);

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
    final PlayerId usa = GameDataTestUtil.usa(twwGameData);
    final PlayerId germany = GameDataTestUtil.germany(twwGameData);
    final Territory celebes = territory("Celebes", twwGameData);
    celebes.getUnitCollection().clear();
    addTo(celebes, GameDataTestUtil.americanStrategicBomber(twwGameData).create(1, usa));
    addTo(celebes, GameDataTestUtil.germanInfantry(twwGameData).create(1, germany));
    final IDelegateBridge bridge = newDelegateBridge(germany);
    battleDelegate(twwGameData).setDelegateBridgeAndPlayer(bridge);
    BattleDelegate.doInitialize(battleDelegate(twwGameData).getBattleTracker(), bridge);
    final IBattle battle =
        AbstractMoveDelegate.getBattleTracker(twwGameData).getPendingBattle(celebes);

    // Ensure battle ends, both units remain, and has 0 rolls
    battle.fight(bridge);
    assertEquals(2, celebes.getUnitCollection().size());
    thenGetRandomShouldHaveBeenCalled(bridge, times(0));
  }
}
