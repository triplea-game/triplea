package games.strategy.triplea.delegate;

import static games.strategy.triplea.delegate.GameDataTestUtil.addTo;
import static games.strategy.triplea.delegate.GameDataTestUtil.battleDelegate;
import static games.strategy.triplea.delegate.GameDataTestUtil.battleship;
import static games.strategy.triplea.delegate.GameDataTestUtil.carrier;
import static games.strategy.triplea.delegate.GameDataTestUtil.fighter;
import static games.strategy.triplea.delegate.GameDataTestUtil.germans;
import static games.strategy.triplea.delegate.GameDataTestUtil.italians;
import static games.strategy.triplea.delegate.GameDataTestUtil.move;
import static games.strategy.triplea.delegate.GameDataTestUtil.moveDelegate;
import static games.strategy.triplea.delegate.GameDataTestUtil.removeFrom;
import static games.strategy.triplea.delegate.GameDataTestUtil.russians;
import static games.strategy.triplea.delegate.GameDataTestUtil.territory;
import static games.strategy.triplea.delegate.MockDelegateBridge.advanceToStep;
import static games.strategy.triplea.delegate.MockDelegateBridge.newDelegateBridge;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.delegate.battle.BattleDelegate;
import games.strategy.triplea.delegate.battle.BattleTracker;
import games.strategy.triplea.delegate.battle.IBattle;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.List;
import org.junit.jupiter.api.Test;

class WW2V3Year42Test {
  private final GameData gameData = TestMapGameData.WW2V3_1942.getGameData();

  @Test
  void testTransportAttack() {
    final Territory sz13 = gameData.getMap().getTerritory("13 Sea Zone");
    final Territory sz12 = gameData.getMap().getTerritory("12 Sea Zone");
    final GamePlayer germans = germans(gameData);
    final IDelegateBridge bridge = newDelegateBridge(germans);
    advanceToStep(bridge, "CombatMove");
    moveDelegate(gameData).setDelegateBridgeAndPlayer(bridge);
    moveDelegate(gameData).start();
    final Route sz13To12 = new Route(sz13, sz12);
    final List<Unit> transports = sz13.getUnitCollection().getMatches(Matches.unitIsSeaTransport());
    assertEquals(1, transports.size());
    move(transports, sz13To12);
  }

  @Test
  void testBombAndAttackEmptyTerritory() {
    final Territory karrelia = territory("Karelia S.S.R.", gameData);
    final Territory baltic = territory("Baltic States", gameData);
    final Territory sz5 = territory("5 Sea Zone", gameData);
    final Territory germany = territory("Germany", gameData);
    final GamePlayer germans = germans(gameData);
    final MoveDelegate moveDelegate = gameData.getMoveDelegate();
    final IDelegateBridge bridge = newDelegateBridge(germans);
    advanceToStep(bridge, "CombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(bridge);
    moveDelegate.start();
    when(bridge.getRemotePlayer().shouldBomberBomb(any())).thenReturn(true);
    // remove the russian units
    removeFrom(
        karrelia, karrelia.getUnitCollection().getMatches(Matches.unitCanBeDamaged().negate()));
    // move the bomber to attack
    move(
        germany.getUnitCollection().getMatches(Matches.unitIsStrategicBomber()),
        new Route(germany, sz5, karrelia));
    // move an infantry to invade
    move(
        baltic.getUnitCollection().getMatches(Matches.unitIsLandTransportable()),
        new Route(baltic, karrelia));
    final BattleTracker battleTracker = MoveDelegate.getBattleTracker(gameData);
    // we should have a pending land battle, and a pending bombing raid
    assertNotNull(battleTracker.getPendingNonBombingBattle(karrelia));
    assertNotNull(battleTracker.getPendingBombingBattle(karrelia));
    // the territory should not be conquered
    assertEquals(karrelia.getOwner(), russians(gameData));
  }

  @Test
  void testLingeringSeaUnitsJoinBattle() {
    final Territory sz5 = territory("5 Sea Zone", gameData);
    final Territory sz6 = territory("6 Sea Zone", gameData);
    final Territory sz7 = territory("7 Sea Zone", gameData);
    // add a russian battleship
    addTo(sz5, battleship(gameData).create(1, russians(gameData)));
    final IDelegateBridge bridge = newDelegateBridge(germans(gameData));
    advanceToStep(bridge, "CombatMove");
    moveDelegate(gameData).setDelegateBridgeAndPlayer(bridge);
    moveDelegate(gameData).start();
    // attack with a german sub
    move(sz7.getUnits(), new Route(sz7, sz6, sz5));
    moveDelegate(gameData).end();
    // adding of lingering units was moved from end of combat-move phase, to start of battle phase
    battleDelegate(gameData).setDelegateBridgeAndPlayer(bridge);
    BattleDelegate.doInitialize(battleDelegate(gameData).getBattleTracker(), bridge);
    // all units in sz5 should be involved in the battle
    final IBattle battle = MoveDelegate.getBattleTracker(gameData).getPendingNonBombingBattle(sz5);
    assertEquals(5, battle.getAttackingUnits().size());
  }

  @Test
  void testLingeringFightersAndAlliedUnitsJoinBattle() {
    final Territory sz5 = territory("5 Sea Zone", gameData);
    final Territory sz6 = territory("6 Sea Zone", gameData);
    final Territory sz7 = territory("7 Sea Zone", gameData);
    // add a russian battleship
    addTo(sz5, battleship(gameData).create(1, russians(gameData)));
    // add an allied carrier and a fighter
    addTo(sz5, carrier(gameData).create(1, italians(gameData)));
    addTo(sz5, fighter(gameData).create(1, germans(gameData)));
    final IDelegateBridge bridge = newDelegateBridge(germans(gameData));
    advanceToStep(bridge, "CombatMove");
    moveDelegate(gameData).setDelegateBridgeAndPlayer(bridge);
    moveDelegate(gameData).start();
    // attack with a german sub
    move(sz7.getUnits(), new Route(sz7, sz6, sz5));
    moveDelegate(gameData).end();
    // adding of lingering units was moved from end of combat-move phase, to start of battle phase
    battleDelegate(gameData).setDelegateBridgeAndPlayer(bridge);
    BattleDelegate.doInitialize(battleDelegate(gameData).getBattleTracker(), bridge);
    // all units in sz5 should be involved in the battle except the italian carrier
    final IBattle battle = MoveDelegate.getBattleTracker(gameData).getPendingNonBombingBattle(sz5);
    assertEquals(6, battle.getAttackingUnits().size());
  }

  @Test
  void testLingeringSeaUnitsCanMoveAwayFromBattle() {
    final Territory sz5 = territory("5 Sea Zone", gameData);
    final Territory sz6 = territory("6 Sea Zone", gameData);
    final Territory sz7 = territory("7 Sea Zone", gameData);
    // add a russian battleship
    addTo(sz5, battleship(gameData).create(1, russians(gameData)));
    final IDelegateBridge bridge = newDelegateBridge(germans(gameData));
    advanceToStep(bridge, "CombatMove");
    moveDelegate(gameData).setDelegateBridgeAndPlayer(bridge);
    moveDelegate(gameData).start();
    // attack with a german sub
    move(sz7.getUnits(), new Route(sz7, sz6, sz5));
    // move the transport away
    move(sz5.getUnitCollection().getMatches(Matches.unitIsSeaTransport()), new Route(sz5, sz6));
    moveDelegate(gameData).end();
    // adding of lingering units was moved from end of combat-move phase, to start of battle phase
    battleDelegate(gameData).setDelegateBridgeAndPlayer(bridge);
    BattleDelegate.doInitialize(battleDelegate(gameData).getBattleTracker(), bridge);
    // all units in sz5 should be involved in the battle
    final IBattle battle = MoveDelegate.getBattleTracker(gameData).getPendingNonBombingBattle(sz5);
    assertEquals(4, battle.getAttackingUnits().size());
  }
}
