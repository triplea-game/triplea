package games.strategy.engine.data;

import com.google.common.annotations.VisibleForTesting;
import games.strategy.triplea.Constants;
import games.strategy.triplea.util.PlayerOrderComparator;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import lombok.Getter;
import lombok.ToString;
import org.triplea.java.RemoveOnNextMajorRelease;

/** Wrapper around the set of players in a game to provide utility functions and methods. */
@ToString
public class PlayerList extends GameDataComponent implements Iterable<GamePlayer> {
  private static final long serialVersionUID = -3895068111754745446L;

  // maps String playerName -> PlayerId
  private final Map<String, GamePlayer> players = new LinkedHashMap<>();
  @Getter private GamePlayer nullPlayer;

  public PlayerList(final GameData data) {
    super(data);
    nullPlayer = createNullPlayer(data);
  }

  @RemoveOnNextMajorRelease
  private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    // Old save games may not have nullPlayer set.
    if (nullPlayer == null) {
      nullPlayer = createNullPlayer(getData());
    }
  }

  private GamePlayer createNullPlayer(GameData data) {
    return new GamePlayer(Constants.PLAYER_NAME_NEUTRAL, true, false, null, false, data) {
      private static final long serialVersionUID = 1;

      @Override
      public boolean isNull() {
        return true;
      }
    };
  }

  @VisibleForTesting
  public void addPlayerId(final GamePlayer player) {
    players.put(player.getName(), player);
  }

  public int size() {
    return players.size();
  }

  public @Nullable GamePlayer getPlayerId(final String name) {
    if (getNullPlayer().getName().equals(name)) {
      return getNullPlayer();
    }
    // Can return null if a bogus name was passed.
    return players.get(name);
  }

  public List<GamePlayer> getPlayers() {
    return new ArrayList<>(players.values());
  }

  public List<GamePlayer> getSortedPlayers() {
    final List<GamePlayer> players = getPlayers();
    players.sort(new PlayerOrderComparator(getData()));
    return players;
  }

  /** an iterator of a new ArrayList copy of the players. */
  @Override
  public Iterator<GamePlayer> iterator() {
    return getPlayers().iterator();
  }

  public Stream<GamePlayer> stream() {
    return players.values().stream();
  }

  public Collection<String> getPlayersThatMayBeDisabled() {
    return stream()
        .filter(GamePlayer::getCanBeDisabled)
        .filter(p -> !p.getIsDisabled())
        .map(DefaultNamed::getName)
        .collect(Collectors.toSet());
  }

  public Map<String, Boolean> getPlayersEnabledListing() {
    final Map<String, Boolean> playersEnabledListing = new HashMap<>();
    for (final GamePlayer p : players.values()) {
      playersEnabledListing.put(p.getName(), !p.getIsDisabled());
    }
    return playersEnabledListing;
  }
}
