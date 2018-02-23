package games.strategy.engine.framework.message;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import games.strategy.util.Version;

/**
 * data from the server indicating what players are available to be
 * taken, and what players are being played.
 * This object also contains versioning info which the client should
 * check to ensure that it is playing the same game as the server.
 * (updated by veqryn to be the object that, besides game options, determines the starting setup for game. ie: who is
 * playing what)
 */
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
      final Map<String, String> localPlayerTypes, final Version gameVersion, final String gameName,
      final String gameRound, final Collection<String> playersAllowedToBeDisabled,
      final Map<String, Collection<String>> playerNamesAndAlliancesInTurnOrder) {
    m_playerToNodeListing =
        (playerToNodeListing == null) ? new HashMap<>() : new HashMap<>(playerToNodeListing);
    m_playersEnabledListing = (playersEnabledListing == null) ? new HashMap<>()
        : new HashMap<>(playersEnabledListing);
    m_localPlayerTypes =
        (localPlayerTypes == null) ? new HashMap<>() : new HashMap<>(localPlayerTypes);
    m_playersAllowedToBeDisabled =
        (playersAllowedToBeDisabled == null) ? new HashSet<>() : new HashSet<>(playersAllowedToBeDisabled);
    m_gameVersion = gameVersion;
    m_gameName = gameName;
    m_gameRound = gameRound;
    m_playerNamesAndAlliancesInTurnOrder = new LinkedHashMap<>();
    if (playerNamesAndAlliancesInTurnOrder != null) {
      for (final Entry<String, Collection<String>> entry : playerNamesAndAlliancesInTurnOrder.entrySet()) {
        m_playerNamesAndAlliancesInTurnOrder.put(entry.getKey(), new HashSet<>(entry.getValue()));
      }
    }
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

  public Map<String, String> getLocalPlayerTypes() {
    return m_localPlayerTypes;
  }
}
