package games.strategy.triplea.delegate.dice.calculator;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import games.strategy.engine.data.GamePlayer;
import games.strategy.triplea.delegate.power.calculator.TotalPowerAndTotalRolls;
import org.junit.jupiter.api.Test;

class RolledDiceTest {

  @Test
  void calculateRejectsRandomArrayShorterThanRollCount() {
    final TotalPowerAndTotalRolls totalPowerAndTotalRolls = mock(TotalPowerAndTotalRolls.class);
    when(totalPowerAndTotalRolls.calculateTotalRolls()).thenReturn(3);
    when(totalPowerAndTotalRolls.getDiceSides()).thenReturn(6);
    final GamePlayer player = mock(GamePlayer.class);
    when(player.getName()).thenReturn("test");
    final RandomDiceGenerator emptyGenerator = (max, count, p, type, annotation) -> new int[0];

    assertThrows(
        IllegalStateException.class,
        () -> RolledDice.calculate(totalPowerAndTotalRolls, player, emptyGenerator, "test"));
  }
}
