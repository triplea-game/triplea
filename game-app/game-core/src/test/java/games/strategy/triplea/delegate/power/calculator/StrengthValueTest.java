package games.strategy.triplea.delegate.power.calculator;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class StrengthValueTest {

  @Test
  void addValue() {
    final StrengthValue strength = StrengthValue.of(6, 1);
    assertThat(strength.add(2).getValue()).as("1 + 2 = 3").isEqualTo(3);
  }

  @Test
  void diceSidesIsMaximum() {
    final StrengthValue strength = StrengthValue.of(6, 1);
    assertThat(strength.add(2).add(4).getValue()).as("1 + 2 + 4 max 6 = 6").isEqualTo(6);
  }

  @Test
  void zeroIsMinimum() {
    final StrengthValue strength = StrengthValue.of(6, 1);
    assertThat(strength.add(-2).getValue()).as("1 - 2 min 0 = 0").isEqualTo(0);
  }
}
