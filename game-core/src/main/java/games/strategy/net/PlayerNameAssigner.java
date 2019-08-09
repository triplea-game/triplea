package games.strategy.net;

import com.google.common.base.Preconditions;
import games.strategy.engine.lobby.PlayerNameValidation;
import java.net.InetAddress;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.triplea.lobby.common.LobbyConstants;

/** Utility class that will assign a name to a newly logging in player. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PlayerNameAssigner {

  /**
   * Returns a node name, based on the specified node name, that is unique across all nodes. The
   * node name is made unique by adding a numbered suffix to the existing node name. For example,
   * for the second node with the name "foo", this method will return "foo (1)".
   *
   * @param desiredName The name being requested by a new user joining.
   * @param ipAddress The IP address of the user that is joining. Used to determine if the user is
   *     already connected under a different name.
   * @param loggedInNodes Collection of nodes that have already joined.
   */
  public static String assignName(
      final String desiredName,
      final InetAddress ipAddress,
      final Collection<INode> loggedInNodes) {
    Preconditions.checkNotNull(ipAddress);
    Preconditions.checkNotNull(loggedInNodes);
    Preconditions.checkArgument(PlayerNameValidation.serverSideValidate(desiredName) == null);

    String currentName = findLoggedInName(ipAddress, loggedInNodes).orElse(desiredName);
    if (currentName.length() > 50) {
      currentName = currentName.substring(0, 50);
    }
    final Collection<String> playerNames =
        new HashSet<>(loggedInNodes.stream().map(INode::getName).collect(Collectors.toSet()));
    final String originalName = currentName;
    for (int i = 1; playerNames.contains(currentName); i++) {
      currentName = originalName + " (" + i + ")";
    }
    return currentName;
  }

  private static Optional<String> findLoggedInName(
      final InetAddress socketAddress, final Collection<INode> nodes) {
    return nodes.stream()
        .filter(node -> node.getAddress().equals(socketAddress) && !isBotName(node.getName()))
        .min(Comparator.naturalOrder())
        .map(INode::getName);
  }

  private static boolean isBotName(final String desiredName) {
    return desiredName.startsWith("Bot") && desiredName.endsWith(LobbyConstants.LOBBY_WATCHER_NAME);
  }
}
