package games.strategy.triplea.delegate.power.calculator;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

class StrengthValueTest {

  @Test
  void addValue() {
    final StrengthValue strength = StrengthValue.of(6, 1);
    assertThat("1 + 2 = 3", strength.add(2).minMax(), is(3));
  }

  @Test
  void diceSidesIsMaximum() {
    final StrengthValue strength = StrengthValue.of(6, 1);
    assertThat("1 + 2 + 4 max 6 = 6", strength.add(2).add(4).minMax(), is(6));
  }

  @Test
  void zeroIsMinimum() {
    final StrengthValue strength = StrengthValue.of(6, 1);
    assertThat("1 - 2 min 0 = 0", strength.add(-2).minMax(), is(0));
  }
}
