package games.strategy.triplea.ai.pro;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.ResourceCollection;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attachments.PoliticalActionAttachment;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.triplea.java.collections.IntegerMap;

class ProPoliticsAiTest {
  private final GameData gameData = TestMapGameData.LHTR.getGameData();
  private final Resource pus = gameData.getResourceList().getResourceOrThrow(Constants.PUS);

  @Test
  void actionsThatTogetherCostMoreThanWeHaveAreNotAllChosen() {
    final var first = actionCosting(8);
    final var second = actionCosting(8);

    final var chosen =
        ProPoliticsAi.chooseAffordableActions(List.of(first, second), a -> true, funds(10), 5);

    assertThat(chosen).containsExactly(first);
  }

  @Test
  void anAffordableActionAfterAnUnaffordableOneIsStillChosen() {
    final var tooExpensive = actionCosting(20);
    final var cheap = actionCosting(3);
    final var alsoCheap = actionCosting(3);

    final var chosen =
        ProPoliticsAi.chooseAffordableActions(
            List.of(tooExpensive, cheap, alsoCheap), a -> true, funds(10), 5);

    assertThat(chosen).containsExactly(cheap, alsoCheap);
  }

  @Test
  void neverChoosesMoreThanTheMaximum() {
    final var chosen =
        ProPoliticsAi.chooseAffordableActions(
            List.of(actionCosting(0), actionCosting(0), actionCosting(0)), a -> true, funds(0), 2);

    assertThat(chosen).hasSize(2);
  }

  @Test
  void skipsActionsThatCannotBeAttempted() {
    final var blocked = actionCosting(0);
    final var allowed = actionCosting(0);

    final var chosen =
        ProPoliticsAi.chooseAffordableActions(
            List.of(blocked, allowed), a -> a != blocked, funds(0), 5);

    assertThat(chosen).containsExactly(allowed);
  }

  private ResourceCollection funds(final int amount) {
    final var funds = new ResourceCollection(gameData);
    funds.addResource(pus, amount);
    return funds;
  }

  private PoliticalActionAttachment actionCosting(final int amount) {
    final var cost = new IntegerMap<Resource>();
    if (amount > 0) {
      cost.put(pus, amount);
    }
    final var action = mock(PoliticalActionAttachment.class);
    when(action.getCostResources()).thenReturn(cost);
    return action;
  }
}
