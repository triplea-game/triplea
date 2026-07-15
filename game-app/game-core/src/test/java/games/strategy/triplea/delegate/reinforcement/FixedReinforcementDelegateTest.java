package games.strategy.triplea.delegate.reinforcement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import games.strategy.engine.data.GamePlayer;
import java.io.Serializable;
import java.util.List;
import org.junit.jupiter.api.Test;

class FixedReinforcementDelegateTest {
  @Test
  void preservesQueueAndLastProcessedRoundAcrossDelegateState() {
    final GamePlayer player = mock(GamePlayer.class);
    when(player.getName()).thenReturn("Allies");
    final FixedReinforcementDelegate delegate = new FixedReinforcementDelegate();
    delegate
        .getTracker()
        .completeRound(player, 3, List.of(new FixedReinforcementOrder(2, "Front", "infantry", 1)));

    final Serializable state = delegate.saveState();
    final FixedReinforcementDelegate restored = new FixedReinforcementDelegate();
    restored.loadState(state);

    assertThat(restored.getTracker().getLastProcessedRound(player)).isEqualTo(3);
    assertThat(restored.getTracker().getPending(player))
        .containsExactly(new FixedReinforcementOrder(2, "Front", "infantry", 1));
    assertThat(restored.delegateCurrentlyRequiresUserInput()).isFalse();
  }
}
