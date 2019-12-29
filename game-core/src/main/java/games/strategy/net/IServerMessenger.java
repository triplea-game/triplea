package games.strategy.net;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Set;
import javax.annotation.Nullable;
import org.triplea.domain.data.UserName;

/** A server messenger. Additional methods for accepting new connections. */
public interface IServerMessenger extends IMessenger {
  void setAcceptNewConnections(boolean accept);

  void setLoginValidator(ILoginValidator loginValidator);

  ILoginValidator getLoginValidator();

  /** Remove the node from the network. */
  void removeConnection(INode node);

  /** Get a list of nodes. */
  Set<INode> getNodes();

  /**
   * Notifies the server that the specified IP address and MAC should be banned.
   *
   * @param ip The IP address to ban.
   * @param mac The hashed MAC address to ban.
   */
  void banPlayer(String ip, String mac);

  /**
   * Returns the hashed MAC address for the user with the specified name or {@code null} if unknown.
   */
  @Nullable
  String getPlayerMac(UserName name);

  boolean isPlayerBanned(String ip, String mac);

  /**
   * Returns the real username for the specified (possibly unique) username.
   *
   * <p>Node usernames may contain a " (n)" suffix when the same user is logged in multiple times.
   * This method removes such a suffix yielding the original (real) username.
   */
  static String getRealName(final String name) {
    checkNotNull(name);

    final int spaceIndex = name.indexOf(' ');
    return (spaceIndex != -1) ? name.substring(0, spaceIndex) : name;
  }
}
