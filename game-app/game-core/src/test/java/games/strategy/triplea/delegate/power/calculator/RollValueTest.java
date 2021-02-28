package games.strategy.triplea.delegate.power.calculator;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

class RollValueTest {

  @Test
  void addValue() {
    final RollValue roll = RollValue.of(1);
    assertThat("1 + 2 = 3", roll.add(2).getValue(), is(3));
  }

  @Test
  void infiniteDoesNotAddValue() {
    final RollValue roll = RollValue.of(-1);
    assertThat("Infinite can not be added to", roll.add(2).getValue(), is(-1));
  }

  @Test
  void zeroIsMinimum() {
    final RollValue roll = RollValue.of(1);
    assertThat("1 - 2 with limit of 0 = 0", roll.add(-2).getValue(), is(0));
  }
}
