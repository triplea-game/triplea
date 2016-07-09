package games.strategy.engine.message;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

import games.strategy.engine.message.unifiedmessenger.UnifiedMessenger;
import games.strategy.net.INode;

/**
 * Implementation of IChannelMessenger built on top of an IMessenger
 */
public class ChannelMessenger implements IChannelMessenger {
  private final UnifiedMessenger m_unifiedMessenger;

  public ChannelMessenger(final UnifiedMessenger messenger) {
    m_unifiedMessenger = messenger;
  }

  public UnifiedMessenger getUnifiedMessenger() {
    return m_unifiedMessenger;
  }

  @Override
  public IChannelSubscribor getChannelBroadcastor(final RemoteName channelName) {
    final InvocationHandler ih =
        new UnifiedInvocationHandler(m_unifiedMessenger, channelName.getName(), true, channelName.getClazz());
    final IChannelSubscribor rVal = (IChannelSubscribor) Proxy
        .newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class<?>[]{channelName.getClazz()}, ih);
    return rVal;
  }

  @Override
  public void registerChannelSubscriber(final Object implementor, final RemoteName channelName) {
    if (!IChannelSubscribor.class.isAssignableFrom(channelName.getClazz())) {
      throw new IllegalStateException(channelName.getClazz() + " is not a channel subscribor");
    }
    m_unifiedMessenger.addImplementor(channelName, implementor, true);
  }

  @Override
  public void unregisterChannelSubscriber(final Object implementor, final RemoteName channelName) {
    m_unifiedMessenger.removeImplementor(channelName.getName(), implementor);
  }

  @Override
  public INode getLocalNode() {
    return m_unifiedMessenger.getLocalNode();
  }

  @Override
  public boolean isServer() {
    return m_unifiedMessenger.isServer();
  }

  @Override
  public String toString() {
    return "ChannelMessenger: " + m_unifiedMessenger.toString();
  }
}
