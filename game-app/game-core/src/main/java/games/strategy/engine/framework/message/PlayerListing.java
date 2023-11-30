package games.strategy.engine.framework.message;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import games.strategy.engine.data.GameData;
import games.strategy.engine.framework.startup.ui.PlayerTypes;
import games.strategy.triplea.NetworkData;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.Getter;

/**
 * Data from the server indicating what players are available to be taken, and what players are
 * being played. This object also contains versioning info which the client should check to ensure
 * that it is playing the same game as the server. Besides game options, determines the starting
 * setup for game. ie: who is playing what.
 */
@Getter
@NetworkData
public class PlayerListing implements Serializable {
  private static final long serialVersionUID = -8913538086737733980L;

  /**
   * Maps String player name -> node Name if node name is null then the player is available to play.
   */
  private final Map<String, String> playerToNodeListing;

  private final Map<String, Boolean> playersEnabledListing;
  private final Map<String, String> localPlayerTypes;
  private final Collection<String> playersAllowedToBeDisabled;
  private final String gameName;
  private final String gameRound;
  private final Map<String, Collection<String>> playerNamesAndAlliancesInTurnOrder;

  public PlayerListing(
      final Map<String, Boolean> playersEnabledListing,
      final Map<String, PlayerTypes.Type> localPlayerTypes,
      final String gameName,
      final String gameRound) {
    // we don't need the playerToNode list, the disable-able players, or the alliances list, for a
    // local game
    this(null, playersEnabledListing, localPlayerTypes, gameName, gameRound, null, null);
  }

  public PlayerListing(
      final Map<String, String> playerToNodeListing,
      final Map<String, Boolean> playersEnabledListing,
      final Map<String, PlayerTypes.Type> localPlayerTypes,
      final String gameName,
      final String gameRound,
      final Collection<String> playersAllowedToBeDisabled,
      final Map<String, Collection<String>> playerNamesAndAlliancesInTurnOrder) {

    // Note: Sets from guava immutables are not necessarily serializable (!)
    // We use copy constructors here to avoid this problem.

    this.playerToNodeListing =
        Optional.ofNullable(playerToNodeListing).map(HashMap::new).orElseGet(HashMap::new);
    this.playersEnabledListing =
        Optional.ofNullable(playersEnabledListing).map(HashMap::new).orElseGet(HashMap::new);
    this.localPlayerTypes =
        localPlayerTypes.entrySet().stream()
            // convert Map<String,PlayerType> -> Map<String,String>
            .collect(Collectors.toMap(Entry::getKey, e -> e.getValue().getLabel()));
    this.gameName = gameName;
    this.gameRound = gameRound;
    this.playersAllowedToBeDisabled =
        Optional.ofNullable(playersAllowedToBeDisabled).map(HashSet::new).orElseGet(HashSet::new);

    this.playerNamesAndAlliancesInTurnOrder =
        Optional.ofNullable(playerNamesAndAlliancesInTurnOrder).orElse(Map.of()).entrySet().stream()
            .collect(
                Collectors.toMap(
                    Entry::getKey,
                    e -> Sets.newHashSet(e.getValue()),
                    (u, v) -> {
                      throw new IllegalStateException(String.format("Duplicate key %s", u));
                    },
                    LinkedHashMap::new));

    // make sure none of the collection values are null.
    // playerToNodeListing is an exception, it can have null values meaning no user has chosen a
    // given nation.
    Preconditions.checkArgument(
        this.playersEnabledListing.values().stream().noneMatch(Objects::isNull),
        this.playersEnabledListing.toString());
    Preconditions.checkArgument(
        this.localPlayerTypes.values().stream().noneMatch(Objects::isNull),
        this.localPlayerTypes.toString());
    Preconditions.checkArgument(
        this.playersAllowedToBeDisabled.stream().noneMatch(Objects::isNull),
        this.playersAllowedToBeDisabled.toString());
    Preconditions.checkArgument(
        this.playerNamesAndAlliancesInTurnOrder.values().stream().noneMatch(Objects::isNull),
        this.playerNamesAndAlliancesInTurnOrder.toString());
  }

  @Override
  public String toString() {
    return "PlayerListingMessage:" + playerToNodeListing;
  }

  public Map<String, PlayerTypes.Type> getLocalPlayerTypeMap(final PlayerTypes playerTypes) {
    return localPlayerTypes.entrySet().stream()
        .collect(Collectors.toMap(Entry::getKey, e -> playerTypes.fromLabel(e.getValue())));
  }

  public void doPreGameStartDataModifications(final GameData gameData) {
    gameData.preGameDisablePlayers(p -> !playersEnabledListing.get(p.getName()));
  }
}
