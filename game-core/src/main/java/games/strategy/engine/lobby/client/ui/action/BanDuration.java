package games.strategy.engine.lobby.client.ui.action;

import com.google.common.base.Preconditions;
import lombok.AllArgsConstructor;

@AllArgsConstructor
final class BanDuration {
  private final Integer duration;
  private final BanTimeUnit timeUnit;

  long toMinutes() {
    Preconditions.checkNotNull(duration);
    return timeUnit.toMinutes(duration);
  }
}
