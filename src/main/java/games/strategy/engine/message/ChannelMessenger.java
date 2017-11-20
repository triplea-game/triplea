package games.strategy.engine.message;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

import games.strategy.engine.message.unifiedmessenger.UnifiedMessenger;
import games.strategy.net.INode;

/**
 * Implementation of IChannelMessenger built on top of an IMessenger.
 */
public class ChannelMessenger implements IChannelMessenger {
  private final UnifiedMessenger unifiedMessenger;

  public ChannelMessenger(final UnifiedMessenger messenger) {
    unifiedMessenger = messenger;
  }

  public UnifiedMessenger getUnifiedMessenger() {
    return unifiedMessenger;
  }

  @Override
  public IChannelSubscribor getChannelBroadcastor(final RemoteName channelName) {
    final InvocationHandler ih =
        new UnifiedInvocationHandler(unifiedMessenger, channelName.getName(), true, channelName.getClazz());
    return (IChannelSubscribor) Proxy
        .newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class<?>[] {channelName.getClazz()}, ih);
  }

  @Override
  public void registerChannelSubscriber(final Object implementor, final RemoteName channelName) {
    if (!IChannelSubscribor.class.isAssignableFrom(channelName.getClazz())) {
      throw new IllegalStateException(channelName.getClazz() + " is not a channel subscribor");
    }
    unifiedMessenger.addImplementor(channelName, implementor, true);
  }

  @Override
  public void unregisterChannelSubscriber(final Object implementor, final RemoteName channelName) {
    unifiedMessenger.removeImplementor(channelName.getName(), implementor);
  }

  @Override
  public INode getLocalNode() {
    return unifiedMessenger.getLocalNode();
  }

  @Override
  public boolean isServer() {
    return unifiedMessenger.isServer();
  }

  @Override
  public String toString() {
    return "ChannelMessenger: " + unifiedMessenger.toString();
  }
}
