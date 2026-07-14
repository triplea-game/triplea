package games.strategy.triplea.delegate.reinforcement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import games.strategy.engine.data.GamePlayer;
import java.util.List;
import org.junit.jupiter.api.Test;

class FixedReinforcementTrackerTest {
  private final GamePlayer player = player("Allies");

  @Test
  void queuesFirstThenAddsNewlyDueRulesAndProcessesOnlyOncePerRound() {
    final FixedReinforcementTracker tracker = new FixedReinforcementTracker();
    final List<FixedReinforcementRule> schedule =
        List.of(
            new FixedReinforcementRule(1, "Front", "infantry", 2),
            new FixedReinforcementRule(3, "Front", "armor", 1));

    final List<FixedReinforcementOrder> roundOne = tracker.getOrdersForRound(player, 1, schedule);
    assertThat(roundOne).containsExactly(new FixedReinforcementOrder(1, "Front", "infantry", 2));
    tracker.completeRound(
        player, 1, List.of(new FixedReinforcementOrder(1, "Front", "infantry", 1)));

    assertThat(tracker.getOrdersForRound(player, 1, schedule)).isEmpty();
    assertThat(tracker.getOrdersForRound(player, 3, schedule))
        .containsExactly(
            new FixedReinforcementOrder(1, "Front", "infantry", 1),
            new FixedReinforcementOrder(3, "Front", "armor", 1));
  }

  private static GamePlayer player(final String name) {
    final GamePlayer player = mock(GamePlayer.class);
    when(player.getName()).thenReturn(name);
    return player;
  }
}
