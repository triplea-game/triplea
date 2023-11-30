package games.strategy.engine.data;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

/** A measurement of value used by players to purchase units. */
@Getter
public class Resource extends NamedAttachable {
  private static final long serialVersionUID = 7471431759007499935L;

  private final List<GamePlayer> players;

  public Resource(final String resourceName, final GameData data) {
    this(resourceName, data, List.of());
  }

  public Resource(final String resourceName, final GameData data, final List<GamePlayer> players) {
    super(resourceName, data);
    this.players = new ArrayList<>(players);
  }

  public boolean isDisplayedFor(final GamePlayer player) {
    return players.contains(player);
  }
}
