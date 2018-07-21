package games.strategy.engine.framework.message;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;

import games.strategy.engine.framework.startup.ui.PlayerType;
import games.strategy.triplea.NetworkData;
import games.strategy.util.Version;

/**
 * data from the server indicating what players are available to be
 * taken, and what players are being played.
 * This object also contains versioning info which the client should
 * check to ensure that it is playing the same game as the server.
 * (updated by veqryn to be the object that, besides game options, determines the starting setup for game. ie: who is
 * playing what)
 */
@NetworkData
public class PlayerListing implements Serializable {
  // keep compatability with older versions
  private static final long serialVersionUID = -8913538086737733980L;
  /**
   * Maps String player name -> node Name
   * if node name is null then the player is available to play.
   */
  private final Map<String, String> m_playerToNodeListing;
  private final Map<String, Boolean> m_playersEnabledListing;
  private final Map<String, String> m_localPlayerTypes;
  private final Collection<String> m_playersAllowedToBeDisabled;
  private final Version m_gameVersion;
  private final String m_gameName;
  private final String m_gameRound;
  private final Map<String, Collection<String>> m_playerNamesAndAlliancesInTurnOrder;

  /**
   * Creates a new instance of PlayerListing.
   */
  public PlayerListing(final Map<String, String> playerToNodeListing, final Map<String, Boolean> playersEnabledListing,
      final Map<String, PlayerType> localPlayerTypes, final Version gameVersion, final String gameName,
      final String gameRound, final Collection<String> playersAllowedToBeDisabled,
      final Map<String, Collection<String>> playerNamesAndAlliancesInTurnOrder) {

    m_playerToNodeListing = Optional.ofNullable(playerToNodeListing).orElse(Collections.emptyMap());
    m_playersEnabledListing = Optional.ofNullable(playersEnabledListing).orElse(Collections.emptyMap());
    m_localPlayerTypes = Optional.ofNullable(localPlayerTypes.entrySet()
        .stream()
        // convert Map<String,PlayerType> -> Map<String,String>
        .collect(Collectors.toMap(Entry::getKey, e -> e.getValue().getLabel())))
        .orElse(Collections.emptyMap());
    m_gameVersion = gameVersion;
    m_gameName = gameName;
    m_gameRound = gameRound;
    m_playersAllowedToBeDisabled = Optional.ofNullable(playersAllowedToBeDisabled).orElse(Collections.emptySet());
    m_playerNamesAndAlliancesInTurnOrder =
        Optional.ofNullable(playerNamesAndAlliancesInTurnOrder).orElse(Collections.emptyMap());

    // make sure none of the collection values are null.
    Preconditions.checkArgument(playerToNodeListing.values().stream().noneMatch(Objects::isNull),
        playerToNodeListing.toString());
    Preconditions.checkArgument(playersEnabledListing.values().stream().noneMatch(Objects::isNull),
        playerToNodeListing.toString());
    Preconditions.checkArgument(playersEnabledListing.values().stream().noneMatch(Objects::isNull),
        playerToNodeListing.toString());
    Preconditions.checkArgument(playersAllowedToBeDisabled.stream().noneMatch(Objects::isNull),
        playerToNodeListing.toString());
    Preconditions.checkArgument(playerNamesAndAlliancesInTurnOrder.values().stream().noneMatch(Objects::isNull),
        playerToNodeListing.toString());
  }

  public Collection<String> getPlayersAllowedToBeDisabled() {
    return m_playersAllowedToBeDisabled;
  }

  public Map<String, String> getPlayerToNodeListing() {
    return m_playerToNodeListing;
  }

  public Map<String, Boolean> getPlayersEnabledListing() {
    return m_playersEnabledListing;
  }

  public Map<String, Collection<String>> getPlayerNamesAndAlliancesInTurnOrderLinkedHashMap() {
    return m_playerNamesAndAlliancesInTurnOrder;
  }

  public String getGameName() {
    return m_gameName;
  }

  public Version getGameVersion() {
    return m_gameVersion;
  }

  @Override
  public String toString() {
    return "PlayerListingMessage:" + m_playerToNodeListing;
  }

  public Set<String> getPlayers() {
    return m_playerToNodeListing.keySet();
  }

  public String getGameRound() {
    return m_gameRound;
  }

  public Map<String, PlayerType> getLocalPlayerTypeMap() {
    return m_localPlayerTypes.entrySet()
        .stream()
        .collect(Collectors.toMap(Entry::getKey, e -> PlayerType.fromLabel(e.getValue())));
  }

}
