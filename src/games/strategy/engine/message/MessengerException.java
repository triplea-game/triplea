package games.strategy.engine.message;

/**
 * All methods called on an IRemote or an IChannelSubscribor may throw one of these exceptions.
 */
public class MessengerException extends RuntimeException {
  private static final long serialVersionUID = 1058615494612307887L;

  public MessengerException(final String message, final Throwable cause) {
    super(message, cause);
  }

  /**
   * We were created in a thread that is not related to the remote
   * that called the method. This allows us to see the stack trace of
   * the invoker.
   */
  public void fillInInvokerStackTrace() {
    getCause().setStackTrace(Thread.currentThread().getStackTrace());
  }
}
