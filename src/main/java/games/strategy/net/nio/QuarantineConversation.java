package games.strategy.net.nio;

/**
 * When a connection is first made, it is quarantined until it logs in.
 *
 * <p>
 * When quaratined, all messages sent by the node are sent to this quarntine conversation.
 * </p>
 *
 * <p>
 * The quarantine conversation can only write to the node across the socket from it.
 * </p>
 *
 * <p>
 * All messages sent to a conversation must be done in the Decode thread.
 * </p>
 */
public abstract class QuarantineConversation {
  public static enum ACTION {
    NONE, TERMINATE, UNQUARANTINE
  }

  /**
   * A message has been read. What should we do?
   */
  public abstract ACTION message(Object o);

  /**
   * called if this conversation has been removed, either after a TERMINATE was
   * returned from a message, or the channel has been closed.
   */
  public abstract void close();
}
