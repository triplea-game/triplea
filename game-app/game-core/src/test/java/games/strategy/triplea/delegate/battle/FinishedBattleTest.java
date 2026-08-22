package games.strategy.triplea.delegate.battle;

import static games.strategy.triplea.delegate.GameDataTestUtil.addTo;
import static games.strategy.triplea.delegate.GameDataTestUtil.armour;
import static games.strategy.triplea.delegate.GameDataTestUtil.battleship;
import static games.strategy.triplea.delegate.GameDataTestUtil.territory;
import static games.strategy.triplea.delegate.GameDataTestUtil.transport;
import static games.strategy.triplea.delegate.MockDelegateBridge.advanceToStep;
import static games.strategy.triplea.delegate.MockDelegateBridge.newDelegateBridge;
import static games.strategy.triplea.delegate.MockDelegateBridge.whenGetRandom;
import static games.strategy.triplea.delegate.MockDelegateBridge.withValues;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.delegate.AbstractMoveDelegate;
import games.strategy.triplea.delegate.GameDataTestUtil;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.MoveDelegate;
import games.strategy.triplea.settings.AbstractClientSettingTestCase;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.triplea.java.collections.CollectionUtils;

public class FinishedBattleTest extends AbstractClientSettingTestCase {
  final GameData pos2GameData = TestMapGameData.PACT_OF_STEEL_2.getGameData();
  final Territory sz2 = territory("2 Sea Zone", pos2GameData);
  final Territory sz3 = territory("3 Sea Zone", pos2GameData);
  final Territory norway = territory("Norway", pos2GameData);
  final Territory uk = territory("United Kingdom", pos2GameData);
  private final GamePlayer germans = GameDataTestUtil.germans(pos2GameData);
  private final GamePlayer british = GameDataTestUtil.british(pos2GameData);

  private Unit britishBattleship;
  private Unit transport;
  private Unit tank;
  private IDelegateBridge bridge;

  @Test
  void testEmptyEnemyConvoyIsCapturedWithoutCreatingBattle() {
    moveBritishFleetToSz3AndUnloadTankInNorway();

    final BattleTracker battleTracker = AbstractMoveDelegate.getBattleTracker(pos2GameData);
    assertNull(
        battleTracker.getPendingBattle(sz3, IBattle.BattleType.NORMAL),
        "Expected no battle to be registered in sz3 - an empty enemy convoy - once the tank unloads to Norway.");

    // No battle means TRANSPORTED_BY should be cleared
    assertNull(tank.getTransportedBy());

    // sz3 is a German-owned convoy zone; moving an uncontested warship through it
    // should flip ownership to the British.
    assertEquals(british, sz3.getOwner());
  }

  @Test
  void testTransportedByClearedAfterDependentBattle() {
    // Place a German battleship in sz3 so the British fleet moving there triggers
    // a sea battle, which the Norway landing will then depend on.
    addTo(sz3, battleship(pos2GameData).create(1, germans));

    moveBritishFleetToSz3AndUnloadTankInNorway();

    advanceToStep(bridge, "Combat");
    BattleDelegate battleDelegate = GameDataTestUtil.battleDelegate(pos2GameData);
    battleDelegate.setDelegateBridgeAndPlayer(bridge);
    BattleDelegate.doInitialize(battleDelegate.getBattleTracker(), bridge);

    // At this stage, TRANSPORTED_BY should not have been cleared
    assertNotNull(tank.getTransportedBy());

    // Force both rounds to hit, guaranteeing the German battleship is sunk.
    whenGetRandom(bridge).thenAnswer(withValues(0)).thenAnswer(withValues(0));

    // Fight the sea battle in sz3; the Norway landing depends on its outcome.
    final IBattle seaBattle =
        battleDelegate.getBattleTracker().getPendingBattle(sz3, IBattle.BattleType.NORMAL);
    seaBattle.fight(bridge);
    assertNotNull(seaBattle);

    // Now that the dependent sea battle is resolved, TRANSPORTED_BY should be cleared.
    assertNull(tank.getTransportedBy());
  }

  /**
   * Moves the British battleship into sz3, then loads the tank onto the transport, moves both into
   * sz3, and lands the tank in Norway. Leaves {@link #bridge}, {@link #britishBattleship}, {@link
   * #transport}, and {@link #tank} populated for use in the Combat phase.
   */
  private void moveBritishFleetToSz3AndUnloadTankInNorway() {
    assertEquals("Germans", sz3.getOwner().getName());
    // Clear all units in Norway since we just want to land uncontested there.
    norway.getUnitCollection().clear();

    britishBattleship = getSingleUnit(sz2, battleship(pos2GameData));
    transport = getSingleUnit(sz2, transport(pos2GameData));
    tank = getSingleUnit(uk, armour(pos2GameData));

    bridge = newDelegateBridge(british);
    advanceToStep(bridge, "CombatMove");
    MoveDelegate moveDelegate = GameDataTestUtil.moveDelegate(pos2GameData);
    moveDelegate.setDelegateBridgeAndPlayer(bridge);
    moveDelegate.start();
    // First, move the battleship to sz3.
    GameDataTestUtil.move(List.of(britishBattleship), new Route(sz2, sz3));
    // Then, load a transport and land a tank in Norway.
    GameDataTestUtil.load(List.of(tank), new Route(uk, sz2));
    GameDataTestUtil.move(List.of(transport, tank), new Route(sz2, sz3));
    GameDataTestUtil.move(List.of(tank), new Route(sz3, norway));
    moveDelegate.end();
  }

  private static Unit getSingleUnit(final Territory territory, final UnitType type) {
    final Collection<Unit> units =
        CollectionUtils.getMatches(territory.getUnits(), Matches.unitIsOfType(type));
    assertEquals(1, units.size());
    return CollectionUtils.getAny(units);
  }
}
