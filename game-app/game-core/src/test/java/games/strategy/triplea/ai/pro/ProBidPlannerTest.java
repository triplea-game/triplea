package games.strategy.triplea.ai.pro;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class ProBidPlannerTest {

  @Test
  void bundlesAreMultisetsOfUpToThreeWithinBudget() {
    final List<List<Integer>> bundles = ProBidPlanner.bundles(List.of(3, 4), cost -> cost, 9);

    assertEquals(
        List.of(
            List.of(3), List.of(3, 3), List.of(3, 3, 3), List.of(3, 4), List.of(4), List.of(4, 4)),
        bundles);
  }

  @Test
  void bundlesAreEmptyWhenNothingFitsBudget() {
    assertEquals(List.of(), ProBidPlanner.bundles(List.of(3, 4), cost -> cost, 2));
  }

  @Test
  void clampSwingCapsCalculatorEarlyExits() {
    assertEquals(200.0, ProBidPlanner.clampSwing(999));
    assertEquals(-200.0, ProBidPlanner.clampSwing(-999));
    assertEquals(12.5, ProBidPlanner.clampSwing(12.5));
  }
}
