package games.strategy.net;

import java.net.InetAddress;
import java.util.Collection;
import java.util.Optional;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Utility class that will assign a name to a newly logging in player.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PlayerNameAssigner {

  /**
   * Name string returned in the case of a hacked client sending us an invalid/unexpected name.
   */
  @VisibleForTesting
  static final String DUMMY_NAME = "aa";

  /**
   * Returns a node name, based on the specified node name, that is unique across all nodes. The node name is made
   * unique by adding a numbered suffix to the existing node name. For example, for the second node with the name "foo",
   * this method will return "foo (1)".
   *
   * @param desiredName The name being requested by a new user joining.
   * @param ipAddress The IP address of the user that is joining. Used to determine if the user
   *        is already connected under a different name.
   * @param loggedInNodes Collection of nodes that have already joined.
   */
  public static String assignName(
      final String desiredName,
      final InetAddress ipAddress,
      final Collection<INode> loggedInNodes) {
    Preconditions.checkNotNull(ipAddress);
    Preconditions.checkNotNull(loggedInNodes);

    String currentName =
        (desiredName == null || desiredName.length() < 3)
            ? DUMMY_NAME
            : findLoggedInName(ipAddress, loggedInNodes).orElse(desiredName);
    if (currentName.length() > 50) {
      currentName = currentName.substring(0, 50);
    }
    if (currentName.length() < 2) {
      currentName = "aa" + currentName;
    }
    if (isNameTaken(currentName, loggedInNodes)) {
      int i = 1;
      while (true) {
        final String newName = currentName + " (" + i + ")";
        if (!isNameTaken(newName, loggedInNodes)) {
          currentName = newName;
          break;
        }
        i++;
      }
    }
    return currentName;
  }

  private static Optional<String> findLoggedInName(
      final InetAddress socketAddress, final Collection<INode> nodes) {
    return nodes
        .stream()
        .sorted()
        .filter(node -> node.getAddress().equals(socketAddress))
        .findFirst()
        .map(INode::getName);
  }

  private static boolean isNameTaken(final String nodeName, final Collection<INode> nodes) {
    return nodes
        .stream()
        .sorted()
        .map(INode::getName)
        .anyMatch(nodeName::equalsIgnoreCase);
  }
}
