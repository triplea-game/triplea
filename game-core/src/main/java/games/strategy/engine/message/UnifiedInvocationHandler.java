package games.strategy.engine.message;

import java.io.Serializable;
import java.lang.reflect.Method;

import games.strategy.engine.message.unifiedmessenger.UnifiedMessenger;
import games.strategy.triplea.util.WrappedInvocationHandler;

/**
 * Invocation handler for the UnifiedMessenger.
 *
 * <p>
 * Handles the invocation for a channel
 * </p>
 */
class UnifiedInvocationHandler extends WrappedInvocationHandler {
  private final UnifiedMessenger messenger;
  private final String endPointName;
  private final boolean ignoreResults;
  private final Class<?> remoteType;

  public UnifiedInvocationHandler(final UnifiedMessenger messenger, final String endPointName,
      final boolean ignoreResults, final Class<?> remoteType) {
    // equality and hash code are bassed on end point name
    super(endPointName);
    this.messenger = messenger;
    this.endPointName = endPointName;
    this.ignoreResults = ignoreResults;
    this.remoteType = remoteType;
  }

  @Override
  public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
    if (super.shouldHandle(method, args)) {
      return super.handle(method, args);
    }
    if (args != null) {
      for (final Object o : args) {
        if ((o != null) && !(o instanceof Serializable)) {
          throw new IllegalArgumentException(
              o + " is not serializable, all remote method args must be serializable.  method:" + method);
        }
      }
    }
    final RemoteMethodCall remoteMethodMsg =
        new RemoteMethodCall(endPointName, method.getName(), args, method.getParameterTypes(), remoteType);
    if (ignoreResults) {
      messenger.invoke(endPointName, remoteMethodMsg);
      return null;
    }

    final RemoteMethodCallResults response = messenger.invokeAndWait(endPointName, remoteMethodMsg);
    if (response.getException() != null) {
      if (response.getException() instanceof MessengerException) {
        final MessengerException cle = (MessengerException) response.getException();
        cle.fillInInvokerStackTrace();
      } else {
        // do not chain the exception, we want to keep whatever the original exception's class was, so just add our
        // bit to the stack
        // trace.
        final Throwable throwable = response.getException();
        final StackTraceElement[] exceptionTrace = throwable.getStackTrace();
        final Exception ourException =
            new Exception(throwable.getMessage() + " exception in response from other system");
        // Thread.currentThread().getStackTrace();
        final StackTraceElement[] ourTrace = ourException.getStackTrace();
        if ((exceptionTrace != null) && (ourTrace != null)) {
          final StackTraceElement[] combinedTrace = new StackTraceElement[(exceptionTrace.length + ourTrace.length)];
          int i = 0;
          for (final StackTraceElement element : exceptionTrace) {
            combinedTrace[i] = element;
            i++;
          }
          for (final StackTraceElement element : ourTrace) {
            combinedTrace[i] = element;
            i++;
          }
          throwable.setStackTrace(combinedTrace);
        }
      }
      throw response.getException();
    }
    return response.getRVal();
  }
}
