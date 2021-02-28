package games.strategy.engine.lobby.client.ui.action;

import com.google.common.base.Preconditions;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public final class ActionDuration {
  private final Integer duration;
  private final ActionTimeUnit timeUnit;

  public long toMinutes() {
    Preconditions.checkNotNull(duration);
    return timeUnit.toMinutes(duration);
  }

  @Override
  public String toString() {
    return duration + " " + timeUnit;
  }
}
