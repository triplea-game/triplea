package games.strategy.engine.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.annotations.VisibleForTesting;

import lombok.ToString;

/**
 * Wrapper around the set of players in a game to provide utility functions and methods.
 */
@ToString
public class PlayerList extends GameDataComponent implements Iterable<PlayerID> {
  private static final long serialVersionUID = -3895068111754745446L;
  // maps String playerName -> PlayerID
  private final Map<String, PlayerID> m_players = new LinkedHashMap<>();

  public PlayerList(final GameData data) {
    super(data);
  }

  @VisibleForTesting
  public void addPlayerId(final PlayerID player) {
    m_players.put(player.getName(), player);
  }

  public int size() {
    return m_players.size();
  }

  public PlayerID getPlayerId(final String name) {
    if (PlayerID.NULL_PLAYERID.getName().equals(name)) {
      return PlayerID.NULL_PLAYERID;
    }
    return m_players.get(name);
  }

  public List<PlayerID> getPlayers() {
    return new ArrayList<>(m_players.values());
  }

  /**
   * an iterator of a new arraylist copy of the players.
   */
  @Override
  public Iterator<PlayerID> iterator() {
    return getPlayers().iterator();
  }

  public Collection<String> getPlayersThatMayBeDisabled() {
    return m_players.values().stream()
        .filter(PlayerID::getCanBeDisabled)
        .filter(p -> !p.getIsDisabled()).map(DefaultNamed::getName)
        .collect(Collectors.toSet());
  }

  public HashMap<String, Boolean> getPlayersEnabledListing() {
    final HashMap<String, Boolean> playersEnabledListing = new HashMap<>();
    for (final PlayerID p : m_players.values()) {
      playersEnabledListing.put(p.getName(), !p.getIsDisabled());
    }
    return playersEnabledListing;
  }
}
