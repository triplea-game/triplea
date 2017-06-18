package games.strategy.net;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.time.Instant;
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
  public void send(final Serializable msg, final INode to) {}

  @Override
  public void broadcast(final Serializable msg) {}

  @Override
  public void addMessageListener(final IMessageListener listener) {}

  @Override
  public void removeMessageListener(final IMessageListener listener) {}

  @Override
  public void addErrorListener(final IMessengerErrorListener listener) {}

  @Override
  public void removeErrorListener(final IMessengerErrorListener listener) {}

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
  public void setAcceptNewConnections(final boolean accept) {}

  @Override
  public boolean isAcceptNewConnections() {
    return false;
  }

  @Override
  public void setLoginValidator(final ILoginValidator loginValidator) {}

  @Override
  public ILoginValidator getLoginValidator() {
    return null;
  }

  @Override
  public void addConnectionChangeListener(final IConnectionChangeListener listener) {}

  @Override
  public void removeConnectionChangeListener(final IConnectionChangeListener listener) {}

  @Override
  public void removeConnection(final INode node) {}

  @Override
  public Set<INode> getNodes() {
    return null;
  }

  @Override
  public void notifyIPMiniBanningOfPlayer(final String ip, final Instant expires) {}

  @Override
  public void notifyMacMiniBanningOfPlayer(final String mac, final Instant expires) {}

  @Override
  public void notifyUsernameMiniBanningOfPlayer(final String username, final Instant expires) {}

  @Override
  public String getPlayerMac(final String name) {
    return null;
  }

  @Override
  public void notifyUsernameMutingOfPlayer(final String username, final Instant muteExpires) {}

  @Override
  public void notifyIPMutingOfPlayer(final String ip, final Instant muteExpires) {}

  @Override
  public void notifyMacMutingOfPlayer(final String mac, final Instant muteExpires) {}

  @Override
  public boolean isUsernameMiniBanned(final String username) {
    return false;
  }

  @Override
  public boolean isIpMiniBanned(final String ip) {
    return false;
  }

  @Override
  public boolean isMacMiniBanned(final String mac) {
    return false;
  }
}
