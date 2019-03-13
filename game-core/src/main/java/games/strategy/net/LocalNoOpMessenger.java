package games.strategy.net;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.Set;

import javax.annotation.Nullable;

/**
 * Implementation of {@link IServerMessenger} for a local game server.
 */
public class LocalNoOpMessenger implements IServerMessenger {

  private final INode node;

  public LocalNoOpMessenger() {
    try {
      node = new Node("dummy", Node.getLocalHost(), 0);
    } catch (final UnknownHostException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public void send(final Serializable msg, final INode to) {}

  @Override
  public void addMessageListener(final IMessageListener listener) {}

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
  public void notifyIpMiniBanningOfPlayer(final String ip) {}

  @Override
  public void notifyMacMiniBanningOfPlayer(final String mac) {}

  @Override
  public @Nullable String getPlayerMac(final String name) {
    return null;
  }

  @Override
  public void notifyMacMutingOfPlayer(final String mac, final @Nullable Instant muteExpires) {}

  @Override
  public boolean isIpMiniBanned(final String ip) {
    return false;
  }

  @Override
  public boolean isMacMiniBanned(final String mac) {
    return false;
  }
}
