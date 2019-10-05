package games.strategy.engine.message;

/**
 * No Remote could be found.
 *
 * <p>This can be thrown by the remote messenger in two cases,
 *
 * <ol>
 *   <li>looking up a someRemoteMessenger.getRemote(...)
 *   <li>invoking a method on the object returned by someRemoteMessenger.getRemote(...)
 * </ol>
 *
 * <p>There are two possible causes. Either the remote never existed, or a remote was once bound to
 * that name, but is no longer bound.
 */
public class RemoteNotFoundException extends MessengerException {
  private static final long serialVersionUID = 7169515572485196188L;

  public RemoteNotFoundException(final String string) {
    super(string);
  }
}
