package games.strategy.engine.message;

/**
 * Called when the connection to a node is lost while invoking a remote method.
 * <p>
 * Only returned on remotes or channels that wait for the results of the method invocation.
 * <p>
 */
public class ConnectionLostException extends MessengerException {
  private static final long serialVersionUID = -5310065420171098696L;

  public ConnectionLostException(final String message) {
    super(message, new Exception("Invoker Stack"));
  }
}
