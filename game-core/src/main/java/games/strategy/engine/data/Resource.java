package games.strategy.engine.data;

import java.util.Collections;
import java.util.List;

public class Resource extends NamedAttachable {
  private static final long serialVersionUID = 7471431759007499935L;

  private final List<PlayerID> players;

  public Resource(final String resourceName, final GameData data) {
    this(resourceName, data, Collections.emptyList());
  }

  public Resource(final String resourceName, final GameData data, final List<PlayerID> players) {
    super(resourceName, data);
    this.players = players;
  }

  public boolean isDisplayedFor(final PlayerID player) {
    // TODO: remove null check on incompatible release
    return (players == null) || players.contains(player);
  }

}
