package games.strategy.triplea.ai.pro;

import static games.strategy.triplea.delegate.GameDataTestUtil.germans;
import static games.strategy.triplea.delegate.GameDataTestUtil.territory;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.MutableProperty;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attachments.PlayerAttachment;
import games.strategy.triplea.xml.TestMapGameData;
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

  @Test
  void capitalWhoseCaptureKeepsItsIncomeIsValuedForProductionOnly()
      throws MutableProperty.InvalidValueException {
    final GameData data = TestMapGameData.LHTR.getGameData();
    final PlayerAttachment pa =
        new PlayerAttachment(Constants.PLAYER_ATTACHMENT_NAME, germans(data), data);
    pa.getPropertyOrThrow("retainCapitalProduceNumber").setValue(0);
    germans(data).addAttachment(Constants.PLAYER_ATTACHMENT_NAME, pa);

    assertThat(ProBidPlanner.value(territory("Germany", data))).isEqualTo(3 * 10);
  }
}
