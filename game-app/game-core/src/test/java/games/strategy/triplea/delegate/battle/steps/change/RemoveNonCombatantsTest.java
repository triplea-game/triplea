package games.strategy.triplea.delegate.battle.steps.change;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
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
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RemoveNonCombatantsTest {

  @Mock ExecutionStack executionStack;
  @Mock IDelegateBridge delegateBridge;
  @Mock BattleState battleState;
  @Mock BattleActions battleActions;
  @Mock GamePlayer attacker;
  @Mock GamePlayer defender;

  private static Unit givenUnitWithId() {
    final Unit unit = mock(Unit.class);
    lenient().when(unit.getId()).thenReturn(UUID.randomUUID());
    return unit;
  }

  @Test
  void notifiesBothOffenseAndDefenseNonCombat() {
    final RemoveNonCombatants removeNonCombatants =
        new RemoveNonCombatants(battleState, battleActions);

    when(battleState.getBattleId()).thenReturn(UUID.randomUUID());

    final Collection<Unit> offenseNonCombatants = List.of(givenUnitWithId());
    when(battleState.removeNonCombatants(BattleState.Side.OFFENSE))
        .thenReturn(offenseNonCombatants);
    final Collection<Unit> defenseNonCombatants = List.of(givenUnitWithId());
    when(battleState.removeNonCombatants(BattleState.Side.DEFENSE))
        .thenReturn(defenseNonCombatants);

    when(battleState.getPlayer(BattleState.Side.OFFENSE)).thenReturn(attacker);
    when(battleState.getPlayer(BattleState.Side.DEFENSE)).thenReturn(defender);
    when(attacker.getName()).thenReturn("attacker");
    when(defender.getName()).thenReturn("defender");

    removeNonCombatants.execute(executionStack, delegateBridge);

    verify(delegateBridge, times(2).description("Both offense and defense should be notified"))
        .sendDisplayMessage(any(IDisplay.ChangedUnitsNotificationMessage.class));
  }

  @Test
  void doesNotNotifyDefenseIfNoDefenseNonCombatants() {
    final RemoveNonCombatants removeNonCombatants =
        new RemoveNonCombatants(battleState, battleActions);

    when(battleState.getBattleId()).thenReturn(UUID.randomUUID());

    final Collection<Unit> offenseNonCombatants = List.of(givenUnitWithId());
    when(battleState.removeNonCombatants(BattleState.Side.OFFENSE))
        .thenReturn(offenseNonCombatants);
    final Collection<Unit> defenseNonCombatants = List.of();
    when(battleState.removeNonCombatants(BattleState.Side.DEFENSE))
        .thenReturn(defenseNonCombatants);

    when(battleState.getPlayer(BattleState.Side.OFFENSE)).thenReturn(attacker);
    when(attacker.getName()).thenReturn("attacker");

    removeNonCombatants.execute(executionStack, delegateBridge);

    verify(delegateBridge, times(1).description("Only offense should be notified"))
        .sendDisplayMessage(any(IDisplay.ChangedUnitsNotificationMessage.class));
  }

  @Test
  void doesNotNotifyOffenseIfNoOffenseNonCombatants() {
    final RemoveNonCombatants removeNonCombatants =
        new RemoveNonCombatants(battleState, battleActions);

    when(battleState.getBattleId()).thenReturn(UUID.randomUUID());

    final Collection<Unit> offenseNonCombatants = List.of();
    when(battleState.removeNonCombatants(BattleState.Side.OFFENSE))
        .thenReturn(offenseNonCombatants);
    final Collection<Unit> defenseNonCombatants = List.of(givenUnitWithId());
    when(battleState.removeNonCombatants(BattleState.Side.DEFENSE))
        .thenReturn(defenseNonCombatants);

    when(battleState.getPlayer(BattleState.Side.DEFENSE)).thenReturn(defender);
    when(defender.getName()).thenReturn("defender");

    removeNonCombatants.execute(executionStack, delegateBridge);

    verify(delegateBridge, times(1).description("Only defense should be notified"))
        .sendDisplayMessage(any(IDisplay.ChangedUnitsNotificationMessage.class));
  }
}
