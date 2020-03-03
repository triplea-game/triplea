package games.strategy.engine.message;

import games.strategy.engine.message.unifiedmessenger.UnifiedMessenger;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

/** An implementation of IRemoteMessenger based on MessageManager and Messenger. */
public class RemoteMessenger implements IRemoteMessenger {
  private final UnifiedMessenger unifiedMessenger;

  public RemoteMessenger(final UnifiedMessenger messenger) {
    unifiedMessenger = messenger;
  }

  @Override
  public IRemote getRemote(final RemoteName remoteName) {
    return getRemote(remoteName, false);
  }

  @Override
  public IRemote getRemote(final RemoteName remoteName, final boolean ignoreResults) {
    final InvocationHandler ih =
        new UnifiedInvocationHandler(unifiedMessenger, remoteName.getName(), ignoreResults);
    return (IRemote)
        Proxy.newProxyInstance(
            Thread.currentThread().getContextClassLoader(),
            new Class<?>[] {remoteName.getClazz()},
            ih);
  }

  @Override
  public void registerRemote(final Object implementor, final RemoteName name) {
    unifiedMessenger.addImplementor(name, implementor, false);
  }

  @Override
  public void unregisterRemote(final RemoteName name) {
    unregisterRemote(name.getName());
  }

  @Override
  public void unregisterRemote(final String name) {
    unifiedMessenger.removeImplementor(name, unifiedMessenger.getImplementor(name));
  }

  @Override
  public boolean isServer() {
    return unifiedMessenger.isServer();
  }

  @Override
  public boolean hasLocalImplementor(final RemoteName descriptor) {
    return unifiedMessenger.getLocalEndPointCount(descriptor) == 1;
  }

  @Override
  public String toString() {
    return "RemoteMessenger: " + unifiedMessenger.toString();
  }
}
