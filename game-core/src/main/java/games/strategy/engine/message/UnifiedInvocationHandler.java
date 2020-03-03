package games.strategy.engine.message;

import games.strategy.engine.message.unifiedmessenger.UnifiedMessenger;
import java.io.Serializable;
import java.lang.reflect.Method;

/**
 * Invocation handler for the UnifiedMessenger.
 *
 * <p>Handles the invocation for a channel
 */
class UnifiedInvocationHandler extends WrappedInvocationHandler {
  private final UnifiedMessenger messenger;
  private final String endPointName;
  private final boolean ignoreResults;

  UnifiedInvocationHandler(
      final UnifiedMessenger messenger, final String endPointName, final boolean ignoreResults) {
    // equality and hash code are based on end point name
    super(endPointName);
    this.messenger = messenger;
    this.endPointName = endPointName;
    this.ignoreResults = ignoreResults;
  }

  @Override
  public Object invoke(final Object proxy, final Method method, final Object[] args)
      throws RemoteNotFoundException {
    if (super.shouldHandle(method, args)) {
      return super.handle(method, args);
    }
    if (args != null) {
      for (final Object o : args) {
        if (o != null && !(o instanceof Serializable)) {
          throw new IllegalArgumentException(
              o
                  + " is not serializable, all remote method args must be serializable.  method:"
                  + method);
        }
      }
    }
    final RemoteMethodCall remoteMethodMsg = new RemoteMethodCall(endPointName, method, args);
    if (ignoreResults) {
      messenger.invoke(endPointName, remoteMethodMsg);
      return null;
    }

    final RemoteMethodCallResults response = messenger.invokeAndWait(endPointName, remoteMethodMsg);
    if (response.getException() != null) {
      throw new RuntimeException("Exception on remote", response.getException());
    }
    return response.getRVal();
  }
}
