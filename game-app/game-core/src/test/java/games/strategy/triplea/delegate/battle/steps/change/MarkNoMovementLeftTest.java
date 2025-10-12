package games.strategy.triplea.delegate.battle.steps.change;

import static games.strategy.engine.data.CompositeChangeMatcher.compositeChangeContains;
import static games.strategy.engine.data.changefactory.ObjectPropertyChangeMatcher.propertyChange;
import static games.strategy.triplea.Constants.UNIT_ATTACHMENT_NAME;
import static games.strategy.triplea.delegate.battle.FakeBattleState.givenBattleStateBuilder;
import static games.strategy.triplea.delegate.battle.steps.MockGameData.givenGameData;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.ExecutionStack;
import games.strategy.triplea.delegate.battle.BattleActions;
import games.strategy.triplea.delegate.battle.BattleState;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MarkNoMovementLeftTest {

  @Mock ExecutionStack executionStack;
  @Mock IDelegateBridge delegateBridge;
  @Mock BattleActions battleActions;

  @Test
  void doesNotRunOnFirstRoundAndHeadless() {
    final BattleState battleState = givenBattleStateBuilder().battleRound(1).headless(true).build();
    final MarkNoMovementLeft markNoMovementLeft =
        new MarkNoMovementLeft(battleState, battleActions);

    markNoMovementLeft.execute(executionStack, delegateBridge);

    verify(delegateBridge, never()).addChange(any());
  }

  @Test
  void doesNotRunOnSecondRound() {
    final BattleState battleState =
        givenBattleStateBuilder().battleRound(2).headless(false).build();
    final MarkNoMovementLeft markNoMovementLeft =
        new MarkNoMovementLeft(battleState, battleActions);

    markNoMovementLeft.execute(executionStack, delegateBridge);

    verify(delegateBridge, never()).addChange(any());
  }

  @Test
  void nonAirWithMovementLeftAreMarkedAsMoved() {
    final BattleState battleState =
        givenBattleStateBuilder()
            .battleRound(1)
            .headless(false)
            .attackingUnits(List.of(givenNonAirUnitWithMovementLeft(BigDecimal.ONE)))
            .build();
    final MarkNoMovementLeft markNoMovementLeft =
        new MarkNoMovementLeft(battleState, battleActions);

    markNoMovementLeft.execute(executionStack, delegateBridge);

    verify(delegateBridge)
        .addChange(
            argThat(
                compositeChangeContains(
                    propertyChange(
                        Unit.PropertyName.ALREADY_MOVED.toString(),
                        BigDecimal.ONE,
                        BigDecimal.ZERO))));
  }

  private Unit givenNonAirUnitWithMovementLeft(final BigDecimal movement) {
    final UnitType unitType = mock(UnitType.class);
    final UnitAttachment unitAttachment = mock(UnitAttachment.class);
    when(unitType.getAttachment(UNIT_ATTACHMENT_NAME)).thenReturn(unitAttachment);
    when(unitAttachment.isAir()).thenReturn(false);
    final Unit unit = spy(new Unit(unitType, mock(GamePlayer.class), givenGameData().build()));
    doReturn(movement).when(unit).getMovementLeft();
    return unit;
  }

  @Test
  @DisplayName("Units with ZERO movement left still get a markNoMovementChange")
  void nonAirWithZeroMovementLeftAreMarkedAsMoved() {
    final BattleState battleState =
        givenBattleStateBuilder()
            .battleRound(1)
            .headless(false)
            .attackingUnits(List.of(givenNonAirUnitWithMovementLeft(BigDecimal.ZERO)))
            .build();
    final MarkNoMovementLeft markNoMovementLeft =
        new MarkNoMovementLeft(battleState, battleActions);

    markNoMovementLeft.execute(executionStack, delegateBridge);

    verify(delegateBridge)
        .addChange(
            argThat(
                compositeChangeContains(
                    propertyChange(
                        Unit.PropertyName.ALREADY_MOVED.toString(),
                        BigDecimal.ONE,
                        BigDecimal.ZERO))));
  }

  @Test
  void nonAirWithNegativeMovementLeftAreNotMarkedAsMoved() {
    final BattleState battleState =
        givenBattleStateBuilder()
            .battleRound(1)
            .headless(false)
            .attackingUnits(List.of(givenNonAirUnitWithMovementLeft(BigDecimal.valueOf(-1))))
            .build();
    final MarkNoMovementLeft markNoMovementLeft =
        new MarkNoMovementLeft(battleState, battleActions);

    markNoMovementLeft.execute(executionStack, delegateBridge);

    verify(delegateBridge, never()).addChange(any());
  }
}
