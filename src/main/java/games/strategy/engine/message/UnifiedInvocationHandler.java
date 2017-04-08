package games.strategy.engine.message;

import java.io.Serializable;
import java.lang.reflect.Method;

import games.strategy.engine.message.unifiedmessenger.UnifiedMessenger;
import games.strategy.triplea.util.WrappedInvocationHandler;

/**
 * Invocation handler for the UnifiedMessenger.
 *
 * Handles the invocation for a channel
 */
class UnifiedInvocationHandler extends WrappedInvocationHandler {
  private final UnifiedMessenger m_messenger;
  private final String m_endPointName;
  private final boolean m_ignoreResults;
  private final Class<?> m_remoteType;

  public UnifiedInvocationHandler(final UnifiedMessenger messenger, final String endPointName,
      final boolean ignoreResults, final Class<?> remoteType) {
    // equality and hash code are bassed on end point name
    super(endPointName);
    m_messenger = messenger;
    m_endPointName = endPointName;
    m_ignoreResults = ignoreResults;
    m_remoteType = remoteType;
  }

  @Override
  public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
    if (super.shouldHandle(method, args)) {
      return super.handle(method, args);
    }
    if (args != null) {
      for (final Object o : args) {
        if (o != null && !(o instanceof Serializable)) {
          throw new IllegalArgumentException(
              o + " is not serializable, all remote method args must be serializable.  method:" + method);
        }
      }
    }
    final RemoteMethodCall remoteMethodMsg =
        new RemoteMethodCall(m_endPointName, method.getName(), args, method.getParameterTypes(), m_remoteType);
    if (m_ignoreResults) {
      m_messenger.invoke(m_endPointName, remoteMethodMsg);
      return null;
    } else {
      final RemoteMethodCallResults response = m_messenger.invokeAndWait(m_endPointName, remoteMethodMsg);
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
          if (exceptionTrace != null && ourTrace != null) {
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
}
