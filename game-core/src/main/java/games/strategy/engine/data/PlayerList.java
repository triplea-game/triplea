package games.strategy.engine.data;

import com.google.common.annotations.VisibleForTesting;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.ToString;

/** Wrapper around the set of players in a game to provide utility functions and methods. */
@ToString
public class PlayerList extends GameDataComponent implements Iterable<PlayerId> {
  private static final long serialVersionUID = -3895068111754745446L;

  // maps String playerName -> PlayerId
  private final Map<String, PlayerId> players = new LinkedHashMap<>();

  public PlayerList(final GameData data) {
    super(data);
  }

  @VisibleForTesting
  public void addPlayerId(final PlayerId player) {
    players.put(player.getName(), player);
  }

  public int size() {
    return players.size();
  }

  public PlayerId getPlayerId(final String name) {
    if (PlayerId.NULL_PLAYERID.getName().equals(name)) {
      return PlayerId.NULL_PLAYERID;
    }
    return players.get(name);
  }

  public List<PlayerId> getPlayers() {
    return new ArrayList<>(players.values());
  }

  /** an iterator of a new ArrayList copy of the players. */
  @Override
  public Iterator<PlayerId> iterator() {
    return getPlayers().iterator();
  }

  public Collection<String> getPlayersThatMayBeDisabled() {
    return players.values().stream()
        .filter(PlayerId::getCanBeDisabled)
        .filter(p -> !p.getIsDisabled())
        .map(DefaultNamed::getName)
        .collect(Collectors.toSet());
  }

  public Map<String, Boolean> getPlayersEnabledListing() {
    final Map<String, Boolean> playersEnabledListing = new HashMap<>();
    for (final PlayerId p : players.values()) {
      playersEnabledListing.put(p.getName(), !p.getIsDisabled());
    }
    return playersEnabledListing;
  }
}
