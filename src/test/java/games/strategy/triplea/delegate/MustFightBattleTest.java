package games.strategy.triplea.delegate;

import static games.strategy.triplea.delegate.GameDataTestUtil.addTo;
import static games.strategy.triplea.delegate.GameDataTestUtil.move;
import static games.strategy.triplea.delegate.GameDataTestUtil.moveDelegate;
import static games.strategy.triplea.delegate.GameDataTestUtil.territory;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.ITestDelegateBridge;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.random.ScriptedRandomSource;
import games.strategy.triplea.player.ITripleAPlayer;
import games.strategy.triplea.xml.TestMapGameData;

public class MustFightBattleTest extends DelegateTest {

  private final ITripleAPlayer dummyPlayer = mock(ITripleAPlayer.class);

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
    bridge.setRemote(dummyPlayer);

    // Set first roll to hit (mine AA) and check that both units are killed
    final ScriptedRandomSource randomSource = new ScriptedRandomSource(0, ScriptedRandomSource.ERROR);
    bridge.setRandomSource(randomSource);
    battle.fight(bridge);
    assertEquals(1, randomSource.getTotalRolled());
    assertEquals(0, sz40.getUnits().size());
  }

}
