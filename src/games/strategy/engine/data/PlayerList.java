package games.strategy.engine.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PlayerList extends GameDataComponent implements Iterable<PlayerID> {
  private static final long serialVersionUID = -3895068111754745446L;
  // maps String playerName -> PlayerID
  private final Map<String, PlayerID> m_players = new LinkedHashMap<>();

  /**
   * Creates new PlayerCollection
   *
   * @param data
   *        game data
   */
  public PlayerList(final GameData data) {
    super(data);
  }

  protected void addPlayerID(final PlayerID player) {
    m_players.put(player.getName(), player);
  }

  public int size() {
    return m_players.size();
  }

  public PlayerID getPlayerID(final String name) {
    if (PlayerID.NULL_PLAYERID.getName().equals(name)) {
      return PlayerID.NULL_PLAYERID;
    }
    return m_players.get(name);
  }

  public String[] getNames() {
    final String[] values = new String[size()];
    m_players.keySet().toArray(values);
    return values;
  }

  /**
   * @return a new arraylist copy of the players
   */
  public List<PlayerID> getPlayers() {
    return new ArrayList<>(m_players.values());
  }

  /**
   * an iterator of a new arraylist copy of the players
   */
  @Override
  public Iterator<PlayerID> iterator() {
    return getPlayers().iterator();
  }

  public Collection<String> getPlayersThatMayBeDisabled() {
    final Collection<String> disableable = new HashSet<>();
    for (final PlayerID p : m_players.values()) {
      // already disabled players can not be reenabled
      if (p.getCanBeDisabled() && !p.getIsDisabled()) {
        disableable.add(p.getName());
      }
    }
    return disableable;
  }

  public HashMap<String, Boolean> getPlayersEnabledListing() {
    final HashMap<String, Boolean> playersEnabledListing = new HashMap<>();
    for (final PlayerID p : m_players.values()) {
      playersEnabledListing.put(p.getName(), !p.getIsDisabled());
    }
    return playersEnabledListing;
  }
}
