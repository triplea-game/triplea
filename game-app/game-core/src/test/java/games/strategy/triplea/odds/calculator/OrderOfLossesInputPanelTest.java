package games.strategy.triplea.odds.calculator;

import static games.strategy.triplea.odds.calculator.OrderOfLossesInputPanel.splitOrderOfLoss;
import static games.strategy.triplea.odds.calculator.OrderOfLossesInputPanel.splitOrderOfLossSection;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

final class OrderOfLossesInputPanelTest {
  @Nested
  final class SplitOrderOfLossTest {
    @Test
    void shouldReturnOneSectionWhenOrderOfLossDoesNotContainSeparator() {
      assertThat(splitOrderOfLoss("  *^infantry  "), contains("*^infantry"));
    }

    @Test
    void shouldReturnOneSectionPerUnitTypeWhenOrderOfLossContainsSeparator() {
      assertThat(
          splitOrderOfLoss("  *^infantry;1^artillery;2^fighter  "),
          contains("*^infantry", "1^artillery", "2^fighter"));
    }
  }

  @Nested
  final class SplitOrderOfLossSectionTest {
    @Test
    void shouldReturnAmountAndUnitType() {
      assertThat(splitOrderOfLossSection("*^infantry"), is(arrayContaining("*", "infantry")));
      assertThat(splitOrderOfLossSection("1^artillery"), is(arrayContaining("1", "artillery")));
      assertThat(splitOrderOfLossSection("99^fighter"), is(arrayContaining("99", "fighter")));
    }
  }
}
