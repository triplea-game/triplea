package games.strategy.triplea.delegate.battle.steps.change;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.display.IDisplay;
import games.strategy.triplea.delegate.ExecutionStack;
import games.strategy.triplea.delegate.battle.BattleActions;
import games.strategy.triplea.delegate.battle.BattleState;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RemoveNonCombatantsTest {

  @Mock ExecutionStack executionStack;
  @Mock IDelegateBridge delegateBridge;
  @Mock IDisplay display;
  @Mock BattleState battleState;
  @Mock BattleActions battleActions;
  @Mock GamePlayer attacker;
  @Mock GamePlayer defender;

  @Test
  void notifiesBothOffenseAndDefenseNonCombat() {
    final RemoveNonCombatants removeNonCombatants =
        new RemoveNonCombatants(battleState, battleActions);

    when(delegateBridge.getDisplayChannelBroadcaster()).thenReturn(display);

    final Collection<Unit> offenseNonCombatants = List.of(mock(Unit.class));
    when(battleState.removeNonCombatants(BattleState.Side.OFFENSE))
        .thenReturn(offenseNonCombatants);
    final Collection<Unit> defenseNonCombatants = List.of(mock(Unit.class));
    when(battleState.removeNonCombatants(BattleState.Side.DEFENSE))
        .thenReturn(defenseNonCombatants);

    when(battleState.getPlayer(BattleState.Side.OFFENSE)).thenReturn(attacker);
    when(battleState.getPlayer(BattleState.Side.DEFENSE)).thenReturn(defender);

    removeNonCombatants.execute(executionStack, delegateBridge);

    verify(display, times(2).description("Both offense and defense should be notified"))
        .changedUnitsNotification(any(), any(), any(), any(), any());
    verify(display)
        .changedUnitsNotification(
            any(), eq(attacker), eq(offenseNonCombatants), eq(null), eq(null));
    verify(display)
        .changedUnitsNotification(
            any(), eq(defender), eq(defenseNonCombatants), eq(null), eq(null));
  }

  @Test
  void doesNotNotifyDefenseIfNoDefenseNonCombatants() {
    final RemoveNonCombatants removeNonCombatants =
        new RemoveNonCombatants(battleState, battleActions);

    when(delegateBridge.getDisplayChannelBroadcaster()).thenReturn(display);

    final Collection<Unit> offenseNonCombatants = List.of(mock(Unit.class));
    when(battleState.removeNonCombatants(BattleState.Side.OFFENSE))
        .thenReturn(offenseNonCombatants);
    final Collection<Unit> defenseNonCombatants = List.of();
    when(battleState.removeNonCombatants(BattleState.Side.DEFENSE))
        .thenReturn(defenseNonCombatants);

    when(battleState.getPlayer(BattleState.Side.OFFENSE)).thenReturn(attacker);

    removeNonCombatants.execute(executionStack, delegateBridge);

    verify(display, times(1).description("Only offense should be notified"))
        .changedUnitsNotification(any(), any(), any(), any(), any());
    verify(display)
        .changedUnitsNotification(
            any(), eq(attacker), eq(offenseNonCombatants), eq(null), eq(null));
  }

  @Test
  void doesNotNotifyOffenseIfNoOffenseNonCombatants() {
    final RemoveNonCombatants removeNonCombatants =
        new RemoveNonCombatants(battleState, battleActions);

    when(delegateBridge.getDisplayChannelBroadcaster()).thenReturn(display);

    final Collection<Unit> offenseNonCombatants = List.of();
    when(battleState.removeNonCombatants(BattleState.Side.OFFENSE))
        .thenReturn(offenseNonCombatants);
    final Collection<Unit> defenseNonCombatants = List.of(mock(Unit.class));
    when(battleState.removeNonCombatants(BattleState.Side.DEFENSE))
        .thenReturn(defenseNonCombatants);

    when(battleState.getPlayer(BattleState.Side.DEFENSE)).thenReturn(defender);

    removeNonCombatants.execute(executionStack, delegateBridge);

    verify(display, times(1).description("Only defense should be notified"))
        .changedUnitsNotification(any(), any(), any(), any(), any());
    verify(display)
        .changedUnitsNotification(
            any(), eq(defender), eq(defenseNonCombatants), eq(null), eq(null));
  }
}
