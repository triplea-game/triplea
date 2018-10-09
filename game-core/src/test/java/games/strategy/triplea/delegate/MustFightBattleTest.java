package games.strategy.triplea.delegate;

import static games.strategy.triplea.delegate.GameDataTestUtil.addTo;
import static games.strategy.triplea.delegate.GameDataTestUtil.move;
import static games.strategy.triplea.delegate.GameDataTestUtil.moveDelegate;
import static games.strategy.triplea.delegate.GameDataTestUtil.territory;
import static games.strategy.triplea.delegate.GameDataTestUtil.thenGetRandomShouldHaveBeenCalled;
import static games.strategy.triplea.delegate.GameDataTestUtil.whenGetRandom;
import static games.strategy.triplea.delegate.GameDataTestUtil.withValues;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;

import org.junit.jupiter.api.Test;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.xml.TestMapGameData;

public class MustFightBattleTest extends AbstractDelegateTestCase {
  @Test
  public void testFightWithIsSuicideOnHit() throws Exception {
    final GameData twwGameData = TestMapGameData.TWW.getGameData();

    // Create battle with 1 cruiser attacking 1 mine
    final PlayerID usa = GameDataTestUtil.usa(twwGameData);
    final PlayerID germany = GameDataTestUtil.germany(twwGameData);
    final Territory sz33 = territory("33 Sea Zone", twwGameData);
    addTo(sz33, GameDataTestUtil.americanCruiser(twwGameData).create(1, usa));
    final Territory sz40 = territory("40 Sea Zone", twwGameData);
    addTo(sz40, GameDataTestUtil.germanMine(twwGameData).create(1, germany));
    final ITestDelegateBridge bridge = GameDataTestUtil.getDelegateBridge(usa, twwGameData);
    bridge.setStepName("CombatMove");
    moveDelegate(twwGameData).setDelegateBridgeAndPlayer(bridge);
    moveDelegate(twwGameData).start();
    move(sz33.getUnits().getUnits(), new Route(sz33, sz40));
    moveDelegate(twwGameData).end();
    final MustFightBattle battle =
        (MustFightBattle) AbstractMoveDelegate.getBattleTracker(twwGameData).getPendingBattle(sz40, false, null);

    // Set first roll to hit (mine AA) and check that both units are killed
    whenGetRandom(bridge).thenAnswer(withValues(0));
    battle.fight(bridge);
    assertEquals(0, sz40.getUnits().size());
    thenGetRandomShouldHaveBeenCalled(bridge, times(1));
  }
}
