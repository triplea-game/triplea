package games.strategy.engine.data;

import games.strategy.net.INode;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Tracks what Node in the networks is playing which roles in the game. */
public class PlayerManager {
  private final Map<String, INode> playerMapping;

  public PlayerManager(final Map<String, INode> map) {
    playerMapping = new HashMap<>(map);
  }

  public Map<String, INode> getPlayerMapping() {
    return new HashMap<>(playerMapping);
  }

  public boolean isEmpty() {
    return playerMapping.isEmpty();
  }

  @Override
  public String toString() {
    if (playerMapping.isEmpty()) {
      return "empty";
    }
    return playerMapping.entrySet().stream()
        .map(e -> String.format("%s=%s", e.getKey(), e.getValue().getName()))
        .collect(Collectors.joining(", "));
  }

  public INode getNode(final String playerName) {
    return playerMapping.get(playerName);
  }

  /**
   * Indicates the given node is playing as anyone.
   *
   * @param node referring node
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
   * Get a player from an opposing side, if possible, else get a player playing at a remote
   * computer, if possible.
   */
  public GamePlayer getRemoteOpponent(final INode localNode, final GameState data) {
    // find a local player
    final GamePlayer local =
        playerMapping.entrySet().stream()
            .filter(e -> e.getValue().equals(localNode))
            .map(Map.Entry::getKey)
            .map(data.getPlayerList()::getPlayerId)
            .findAny()
            .orElse(null);
    // we aren't playing anyone, return any
    if (local == null) {
      final String remote = playerMapping.keySet().iterator().next();
      return data.getPlayerList().getPlayerId(remote);
    }
    String any = null;
    for (final Map.Entry<String, INode> entry : playerMapping.entrySet()) {
      final String player = entry.getKey();
      if (!entry.getValue().equals(localNode)) {
        any = player;
        final GamePlayer remoteGamePlayer = data.getPlayerList().getPlayerId(player);
        if (!data.getRelationshipTracker().isAllied(local, remoteGamePlayer)) {
          return remoteGamePlayer;
        }
      }
    }
    // no un allied players were found, any will do
    return data.getPlayerList().getPlayerId(any);
  }
}
