package games.strategy.engine.message;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

import games.strategy.engine.message.unifiedmessenger.UnifiedMessenger;

/**
 * An implementation of IRemoteMessenger based on MessageManager and Messenger.
 */
public class RemoteMessenger implements IRemoteMessenger {
  private final UnifiedMessenger m_unifiedMessenger;

  public RemoteMessenger(final UnifiedMessenger messenger) {
    m_unifiedMessenger = messenger;
  }

  @Override
  public IRemote getRemote(final RemoteName remoteName) {
    return getRemote(remoteName, false);
  }

  @Override
  public IRemote getRemote(final RemoteName remoteName, final boolean ignoreResults) {
    final InvocationHandler ih =
        new UnifiedInvocationHandler(m_unifiedMessenger, remoteName.getName(), ignoreResults, remoteName.getClazz());
    final IRemote rVal = (IRemote) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
        new Class<?>[] {remoteName.getClazz()}, ih);
    return rVal;
  }

  @Override
  public void registerRemote(final Object implementor, final RemoteName name) {
    m_unifiedMessenger.addImplementor(name, implementor, false);
  }

  @Override
  public void unregisterRemote(final RemoteName name) {
    unregisterRemote(name.getName());
  }

  @Override
  public boolean isServer() {
    return m_unifiedMessenger.isServer();
  }

  @Override
  public void unregisterRemote(final String name) {
    m_unifiedMessenger.removeImplementor(name, m_unifiedMessenger.getImplementor(name));
  }

  @Override
  public boolean hasLocalImplementor(final RemoteName descriptor) {
    return m_unifiedMessenger.getLocalEndPointCount(descriptor) == 1;
  }

  @Override
  public String toString() {
    return "RemoteMessenger: " + m_unifiedMessenger.toString();
  }
}
