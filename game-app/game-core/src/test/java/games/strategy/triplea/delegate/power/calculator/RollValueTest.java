package games.strategy.triplea.delegate.power.calculator;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RollValueTest {

  @Test
  void addValue() {
    final RollValue roll = RollValue.of(1);
    assertThat(roll.add(2).getValue()).as("1 + 2 = 3").isEqualTo(3);
  }

  @Test
  void infiniteDoesNotAddValue() {
    final RollValue roll = RollValue.of(-1);
    assertThat(roll.add(2).getValue()).as("Infinite can not be added to").isEqualTo(-1);
  }

  @Test
  void zeroIsMinimum() {
    final RollValue roll = RollValue.of(1);
    assertThat(roll.add(-2).getValue()).as("1 - 2 with limit of 0 = 0").isEqualTo(0);
  }
}
