package games.strategy.triplea.delegate.supply;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import games.strategy.engine.data.GamePlayer;
import java.io.Serializable;
import org.junit.jupiter.api.Test;

class SupplyDelegateTest {
  @Test
  void preservesTrackerAcrossDelegateState() {
    final GamePlayer player = mock(GamePlayer.class);
    when(player.getName()).thenReturn("Blue");
    final SupplyDelegate delegate = new SupplyDelegate();
    delegate.getTracker().completeRound(player, 3);

    final Serializable state = delegate.saveState();
    final SupplyDelegate restored = new SupplyDelegate();
    restored.loadState(state);

    assertThat(restored.getTracker().getLastProcessedRound(player)).isEqualTo(3);
    assertThat(restored.delegateCurrentlyRequiresUserInput()).isFalse();
  }
}
