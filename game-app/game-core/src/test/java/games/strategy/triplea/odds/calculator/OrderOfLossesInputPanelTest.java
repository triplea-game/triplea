package games.strategy.triplea.odds.calculator;

import static games.strategy.triplea.odds.calculator.OrderOfLossesInputPanel.splitOrderOfLoss;
import static games.strategy.triplea.odds.calculator.OrderOfLossesInputPanel.splitOrderOfLossSection;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

final class OrderOfLossesInputPanelTest {
  @Nested
  final class SplitOrderOfLossTest {
    @Test
    void shouldReturnOneSectionWhenOrderOfLossDoesNotContainSeparator() {
      assertThat(splitOrderOfLoss("  *^infantry  ")).containsExactly("*^infantry");
    }

    @Test
    void shouldReturnOneSectionPerUnitTypeWhenOrderOfLossContainsSeparator() {
      assertThat(splitOrderOfLoss("  *^infantry;1^artillery;2^fighter  "))
          .containsExactly("*^infantry", "1^artillery", "2^fighter");
    }
  }

  @Nested
  final class SplitOrderOfLossSectionTest {
    @Test
    void shouldReturnAmountAndUnitType() {
      assertThat(splitOrderOfLossSection("*^infantry")).containsExactly("*", "infantry");
      assertThat(splitOrderOfLossSection("1^artillery")).containsExactly("1", "artillery");
      assertThat(splitOrderOfLossSection("99^fighter")).containsExactly("99", "fighter");
    }
  }
}
