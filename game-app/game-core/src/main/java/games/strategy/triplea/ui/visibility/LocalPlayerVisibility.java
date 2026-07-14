package games.strategy.triplea.ui.visibility;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameState;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.delegate.visibility.VisibilityService;
import games.strategy.triplea.ui.UiContext;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Resolves the human players whose combined visibility is rendered on this client. */
public final class LocalPlayerVisibility {
  private LocalPlayerVisibility() {}

  public static List<GamePlayer> getViewers(final UiContext uiContext, final GameState data) {
    if (uiContext.getLocalPlayers() == null) {
      return List.of();
    }
    return uiContext.getLocalPlayers().getLocalPlayers().stream()
        .filter(player -> !player.isAi())
        .map(player -> player.getGamePlayer().getName())
        .map(data.getPlayerList()::getPlayerId)
        .filter(Objects::nonNull)
        .sorted((left, right) -> left.getName().compareTo(right.getName()))
        .toList();
  }

  public static boolean isMaskingEnabled(final UiContext uiContext, final GameState data) {
    return VisibilityService.isEnabled(data) && !getViewers(uiContext, data).isEmpty();
  }

  public static Set<Territory> getVisibleTerritories(
      final UiContext uiContext, final GameState data) {
    return VisibilityService.getVisibleTerritories(getViewers(uiContext, data), data);
  }
}
