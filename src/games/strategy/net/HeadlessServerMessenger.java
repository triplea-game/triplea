package games.strategy.net;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.Set;

import games.strategy.debug.ClientLogger;

public class HeadlessServerMessenger implements IServerMessenger {

  private INode node;

  public HeadlessServerMessenger() {
    try {
      node = new Node("dummy", InetAddress.getLocalHost(), 0);
    } catch (final UnknownHostException e) {
      ClientLogger.logQuietly(e);
      throw new IllegalStateException(e.getMessage());
    }
  }

  @Override
  public void send(Serializable msg, INode to) {}

  @Override
  public void broadcast(Serializable msg) {}

  @Override
  public void addMessageListener(IMessageListener listener) {}

  @Override
  public void removeMessageListener(IMessageListener listener) {}

  @Override
  public void addErrorListener(IMessengerErrorListener listener) {}

  @Override
  public void removeErrorListener(IMessengerErrorListener listener) {}

  @Override
  public INode getLocalNode() {
    return node;
  }

  @Override
  public boolean isConnected() {
    return false;
  }

  @Override
  public void shutDown() {}

  @Override
  public boolean isServer() {
    return true;
  }

  @Override
  public INode getServerNode() {
    return node;
  }

  @Override
  public InetSocketAddress getRemoteServerSocketAddress() {
    return null;
  }

  @Override
  public void setAcceptNewConnections(boolean accept) {}

  @Override
  public boolean isAcceptNewConnections() {
    return false;
  }

  @Override
  public void setLoginValidator(ILoginValidator loginValidator) {}

  @Override
  public ILoginValidator getLoginValidator() {
    return null;
  }

  @Override
  public void addConnectionChangeListener(IConnectionChangeListener listener) {}

  @Override
  public void removeConnectionChangeListener(IConnectionChangeListener listener) {}

  @Override
  public void removeConnection(INode node) {}

  @Override
  public Set<INode> getNodes() {
    return null;
  }

  @Override
  public void NotifyIPMiniBanningOfPlayer(String ip, Date expires) {}

  @Override
  public void NotifyMacMiniBanningOfPlayer(String mac, Date expires) {}

  @Override
  public void NotifyUsernameMiniBanningOfPlayer(String username, Date expires) {}

  @Override
  public String getPlayerMac(String name) {
    return null;
  }

  @Override
  public void NotifyUsernameMutingOfPlayer(String username, Date muteExpires) {}

  @Override
  public void NotifyIPMutingOfPlayer(String ip, Date muteExpires) {}

  @Override
  public void NotifyMacMutingOfPlayer(String mac, Date muteExpires) {}

  @Override
  public boolean IsUsernameMiniBanned(String username) {
    return false;
  }

  @Override
  public boolean IsIpMiniBanned(String ip) {
    return false;
  }

  @Override
  public boolean IsMacMiniBanned(String mac) {
    return false;
  }
}
