package games.strategy.triplea.delegate.visibility;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameState;
import games.strategy.engine.data.Territory;
import java.util.Collection;
import lombok.extern.slf4j.Slf4j;

/** Central audit hook for UI and agent surfaces that request territory information. */
@Slf4j
public final class VisibilityAudit {
  private VisibilityAudit() {}

  public static boolean canReveal(
      final String surface,
      final Territory territory,
      final Collection<GamePlayer> viewers,
      final GameState data) {
    final boolean visible = VisibilityService.isVisible(territory, viewers, data);
    if (!visible && VisibilityService.isEnabled(data)) {
      log.debug(
          "Fog of war masked {} access to territory {} for viewers {}",
          surface,
          territory.getName(),
          viewers.stream().map(GamePlayer::getName).sorted().toList());
    }
    return visible;
  }
}
