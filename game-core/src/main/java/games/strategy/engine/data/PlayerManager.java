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
    return (playerMapping == null) || playerMapping.isEmpty();
  }

  @Override
  public String toString() {
    if ((playerMapping == null) || playerMapping.isEmpty()) {
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
    return playerMapping.entrySet().stream()
        .filter(e -> e.getValue().equals(playerNode))
        .map(Map.Entry::getKey)
        .collect(Collectors.toSet());
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
    final PlayerID local = playerMapping.entrySet().stream()
        .filter(e -> e.getValue().equals(localNode))
        .map(Map.Entry::getKey)
        .map(data.getPlayerList()::getPlayerId)
        .findAny()
        .orElse(null);
    // we arent playing anyone, return any
    if (local == null) {
      final String remote = playerMapping.keySet().iterator().next();
      return data.getPlayerList().getPlayerId(remote);
    }
    String any = null;
    for (final Map.Entry<String, INode> entry : playerMapping.entrySet()) {
      final String player = entry.getKey();
      if (!entry.getValue().equals(localNode)) {
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
