package games.strategy.engine.data;

import java.util.ArrayList;
import java.util.List;

public class Resource extends NamedAttachable {
  private static final long serialVersionUID = 7471431759007499935L;

  private List<PlayerID> players = new ArrayList<>();

  /**
   * Creates new Resource.
   *
   * @param name
   *        name of the resource
   * @param data
   *        game data
   */
  public Resource(final String name, final GameData data) {
    super(name, data);
  }

  public Resource(final String name, final GameData data, final List<PlayerID> players) {
    this(name, data);
    this.players = players;
  }

  public boolean isDisplayedFor(final PlayerID player) {
    return players != null ? players.contains(player) : true;
  }

}
