package games.strategy.net;

import java.util.Date;
import java.util.Set;

/**
 * A server messenger. Additional methods for accepting new connections.
 */
public interface IServerMessenger extends IMessenger {
  void setAcceptNewConnections(boolean accept);

  boolean isAcceptNewConnections();

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

  void NotifyIPMiniBanningOfPlayer(String ip, Date expires);

  void NotifyMacMiniBanningOfPlayer(String mac, Date expires);

  void NotifyUsernameMiniBanningOfPlayer(String username, Date expires);

  String GetPlayerMac(String name);

  void NotifyUsernameMutingOfPlayer(String username, Date muteExpires);

  void NotifyIPMutingOfPlayer(String ip, Date muteExpires);

  void NotifyMacMutingOfPlayer(String mac, Date muteExpires);

  boolean IsUsernameMiniBanned(String username);

  boolean IsIpMiniBanned(String ip);

  boolean IsMacMiniBanned(String mac);
}
