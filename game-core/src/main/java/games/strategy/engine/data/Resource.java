package games.strategy.engine.data;

import java.util.ArrayList;
import java.util.List;

public class Resource extends NamedAttachable {
  private static final long serialVersionUID = 7471431759007499935L;

  private final List<PlayerID> players;

  /**
   * Creates new Resource.
   *
   * @param name
   *        name of the resource
   * @param data
   *        game data
   */
  public Resource(final String name, final GameData data) {
    this(name, data, new ArrayList<>());
  }

  public Resource(final String name, final GameData data, final List<PlayerID> players) {
    super(name, data);
    this.players = players;
  }

  public boolean isDisplayedFor(final PlayerID player) {
    // TODO: remove null check on incompatible release
    return (players == null) || players.contains(player);
  }

}
