package games.strategy.engine.data;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import games.strategy.net.INode;

/**
 * Tracks what Node in the networks is playing which roles in the game.
 */
public class PlayerManager {
  private final Map<String, INode> m_playerMapping;

  public PlayerManager(final Map<String, INode> map) {
    m_playerMapping = new HashMap<>(map);
  }

  public Map<String, INode> getPlayerMapping() {
    return new HashMap<>(m_playerMapping);
  }

  public boolean isEmpty() {
    return m_playerMapping == null || m_playerMapping.isEmpty();
  }

  @Override
  public String toString() {
    if (m_playerMapping == null || m_playerMapping.isEmpty()) {
      return "empty";
    }
    final StringBuilder sb = new StringBuilder();
    final Iterator<String> iter = m_playerMapping.keySet().iterator();
    while (iter.hasNext()) {
      final String key = iter.next();
      final INode value = m_playerMapping.get(key);
      sb.append(key).append("=").append(value.getName());
      if (iter.hasNext()) {
        sb.append(", ");
      }
    }
    return sb.toString();
  }

  public Set<INode> getNodes() {
    return new HashSet<>(m_playerMapping.values());
  }

  public INode getNode(final String playerName) {
    return m_playerMapping.get(playerName);
  }

  /**
   * @param node
   *        referring node
   * @return whether the given node playing as anyone.
   */
  public boolean isPlaying(final INode node) {
    return m_playerMapping.containsValue(node);
  }

  public Set<String> getPlayers() {
    return new HashSet<>(m_playerMapping.keySet());
  }

  public Set<String> getPlayedBy(final INode playerNode) {
    final Set<String> rVal = new HashSet<>();
    for (final String player : m_playerMapping.keySet()) {
      if (m_playerMapping.get(player).equals(playerNode)) {
        rVal.add(player);
      }
    }
    return rVal;
  }

  /**
   * Get a player from an opposing side, if possible, else
   * get a player playing at a remote computer, if possible.
   *
   * @param localNode
   *        local node
   * @param data
   *        game data
   * @return player found
   */
  public PlayerID getRemoteOpponent(final INode localNode, final GameData data) {
    // find a local player
    PlayerID local = null;
    for (final String player : m_playerMapping.keySet()) {
      if (m_playerMapping.get(player).equals(localNode)) {
        local = data.getPlayerList().getPlayerID(player);
        break;
      }
    }
    // we arent playing anyone, return any
    if (local == null) {
      final String remote = m_playerMapping.keySet().iterator().next();
      return data.getPlayerList().getPlayerID(remote);
    }
    String any = null;
    for (final String player : m_playerMapping.keySet()) {
      if (!m_playerMapping.get(player).equals(localNode)) {
        any = player;
        final PlayerID remotePlayerId = data.getPlayerList().getPlayerID(player);
        if (!data.getRelationshipTracker().isAllied(local, remotePlayerId)) {
          return remotePlayerId;
        }
      }
    }
    // no un allied players were found, any will do
    return data.getPlayerList().getPlayerID(any);
  }
}
