package games.strategy.triplea.delegate.battle;

import static games.strategy.triplea.delegate.GameDataTestUtil.armour;
import static games.strategy.triplea.delegate.GameDataTestUtil.battleship;
import static games.strategy.triplea.delegate.GameDataTestUtil.territory;
import static games.strategy.triplea.delegate.GameDataTestUtil.transport;
import static games.strategy.triplea.delegate.MockDelegateBridge.advanceToStep;
import static games.strategy.triplea.delegate.MockDelegateBridge.newDelegateBridge;
import static org.junit.jupiter.api.Assertions.assertEquals;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.delegate.IDelegateBridge;
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

  @Test
  void testTransportedByClearedAfterDependentBattle() {
    assertEquals("Germans", sz3.getOwner().getName());
    // Clear all units in Norway since we just want to land uncontested there.
    norway.getUnitCollection().clear();

    final Unit battleship = getSingleUnit(sz2, battleship(pos2GameData));
    final Unit transport = getSingleUnit(sz2, transport(pos2GameData));
    final Unit tank = getSingleUnit(uk, armour(pos2GameData));

    final GamePlayer british = GameDataTestUtil.british(pos2GameData);
    final IDelegateBridge bridge = newDelegateBridge(british);

    advanceToStep(bridge, "CombatMove");
    MoveDelegate moveDelegate = GameDataTestUtil.moveDelegate(pos2GameData);
    moveDelegate.setDelegateBridgeAndPlayer(bridge);
    moveDelegate.start();
    // First, move the battleship to sz3 to capture the convoy zone.
    GameDataTestUtil.move(List.of(battleship), new Route(sz2, sz3));
    // Then, load a transport and land a tank in Norway.
    GameDataTestUtil.load(List.of(tank), new Route(uk, sz2));
    GameDataTestUtil.move(List.of(transport, tank), new Route(sz2, sz3));
    GameDataTestUtil.move(List.of(tank), new Route(sz3, norway));
    moveDelegate.end();
    // TRANSPORTED_BY is still set, since there's a dependent battle.
    assertEquals(transport, tank.getTransportedBy());

    advanceToStep(bridge, "Combat");
    BattleDelegate battleDelegate = GameDataTestUtil.battleDelegate(pos2GameData);
    battleDelegate.setDelegateBridgeAndPlayer(bridge);
    battleDelegate.start();
    // TRANSPORTED_BY should now be cleared since the dependent battle has been resolved.
    assertEquals(null, tank.getTransportedBy());
  }

  private Unit getSingleUnit(Territory t, UnitType type) {
    Collection<Unit> units = CollectionUtils.getMatches(t.getUnits(), Matches.unitIsOfType(type));
    assertEquals(1, units.size());
    return CollectionUtils.getAny(units);
  }
}
