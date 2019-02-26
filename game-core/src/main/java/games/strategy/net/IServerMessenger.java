package games.strategy.net;

import static com.google.common.base.Preconditions.checkNotNull;

import java.time.Instant;
import java.util.Set;

import javax.annotation.Nullable;

/**
 * A server messenger. Additional methods for accepting new connections.
 */
public interface IServerMessenger extends IMessenger {
  void setAcceptNewConnections(boolean accept);

  void setLoginValidator(ILoginValidator loginValidator);

  ILoginValidator getLoginValidator();

  /**
   * Add a listener for change in connection status.
   */
  void addConnectionChangeListener(IConnectionChangeListener listener);

  /**
   * Remove a listener for change in connection status.
   */
  void removeConnectionChangeListener(IConnectionChangeListener listener);

  /**
   * Remove the node from the network.
   */
  void removeConnection(INode node);

  /**
   * Get a list of nodes.
   */
  Set<INode> getNodes();

  /**
   * Notifies the server that the specified IP address has been banned until the specified instant.
   *
   * @param ip The IP address to ban.
   * @param expires The time at which the ban expires or {@code null} if the ban is indefinite.
   */
  void notifyIpMiniBanningOfPlayer(String ip, @Nullable Instant expires);

  /**
   * Notifies the server that the specified hashed MAC address has been banned until the specified instant.
   *
   * @param mac The hashed MAC address to ban.
   * @param expires The time at which the ban expires or {@code null} if the ban is indefinite.
   */
  void notifyMacMiniBanningOfPlayer(String mac, @Nullable Instant expires);

  /**
   * Notifies the server that the specified username has been banned until the specified instant.
   *
   * @param username The username to ban.
   * @param expires The time at which the ban expires or {@code null} if the ban is indefinite.
   */
  void notifyUsernameMiniBanningOfPlayer(String username, @Nullable Instant expires);

  /**
   * Returns the hashed MAC address for the user with the specified name or {@code null} if unknown.
   */
  @Nullable
  String getPlayerMac(String name);

  /**
   * Notifies the server that the specified username has been muted until the specified instant.
   *
   * @param username The username to mute.
   * @param muteExpires The time at which the mute expires or {@code null} if the mute is indefinite.
   */
  void notifyUsernameMutingOfPlayer(String username, @Nullable Instant muteExpires);

  /**
   * Notifies the server that the specified hashed MAC address has been muted until the specified instant.
   *
   * @param mac The hashed MAC address to mute.
   * @param muteExpires The time at which the mute expires or {@code null} if the mute is indefinite.
   */
  void notifyMacMutingOfPlayer(String mac, @Nullable Instant muteExpires);

  boolean isUsernameMiniBanned(String username);

  boolean isIpMiniBanned(String ip);

  boolean isMacMiniBanned(String mac);

  /**
   * Returns the real username for the specified (possibly unique) username.
   *
   * <p>
   * Node usernames may contain a " (n)" suffix when the same user is logged in multiple times. This method removes such
   * a suffix yielding the original (real) username.
   * </p>
   */
  static String getRealName(final String name) {
    checkNotNull(name);

    final int spaceIndex = name.indexOf(' ');
    return (spaceIndex != -1) ? name.substring(0, spaceIndex) : name;
  }
}
