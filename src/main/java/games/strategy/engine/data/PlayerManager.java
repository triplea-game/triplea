package games.strategy.engine.data;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import games.strategy.net.INode;

/**
 * Tracks what Node in the networks is playing which roles in the game.
 */
public class PlayerManager {
  private final Map<String, INode> playerMapping;

  public PlayerManager(final Map<String, INode> map) {
    playerMapping = new HashMap<>(map);
  }

  public Map<String, INode> getPlayerMapping() {
    return new HashMap<>(playerMapping);
  }

  public boolean isEmpty() {
    return playerMapping == null || playerMapping.isEmpty();
  }

  @Override
  public String toString() {
    if (playerMapping == null || playerMapping.isEmpty()) {
      return "empty";
    }
    return playerMapping.entrySet().stream()
        .map(e -> String.format("%s=%s", e.getKey(), e.getValue().getName()))
        .collect(Collectors.joining(", "));

  }

  public Set<INode> getNodes() {
    return new HashSet<>(playerMapping.values());
  }

  public INode getNode(final String playerName) {
    return playerMapping.get(playerName);
  }

  /**
   * @param node
   *        referring node
   * @return whether the given node playing as anyone.
   */
  public boolean isPlaying(final INode node) {
    return playerMapping.containsValue(node);
  }

  public Set<String> getPlayers() {
    return new HashSet<>(playerMapping.keySet());
  }

  public Set<String> getPlayedBy(final INode playerNode) {
    final Set<String> players = new HashSet<>();
    for (final String player : playerMapping.keySet()) {
      if (playerMapping.get(player).equals(playerNode)) {
        players.add(player);
      }
    }
    return players;
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
    for (final String player : playerMapping.keySet()) {
      if (playerMapping.get(player).equals(localNode)) {
        local = data.getPlayerList().getPlayerId(player);
        break;
      }
    }
    // we arent playing anyone, return any
    if (local == null) {
      final String remote = playerMapping.keySet().iterator().next();
      return data.getPlayerList().getPlayerId(remote);
    }
    String any = null;
    for (final String player : playerMapping.keySet()) {
      if (!playerMapping.get(player).equals(localNode)) {
        any = player;
        final PlayerID remotePlayerId = data.getPlayerList().getPlayerId(player);
        if (!data.getRelationshipTracker().isAllied(local, remotePlayerId)) {
          return remotePlayerId;
        }
      }
    }
    // no un allied players were found, any will do
    return data.getPlayerList().getPlayerId(any);
  }
}
