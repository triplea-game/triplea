package games.strategy.net.nio;

import java.io.Serializable;

/**
 * When a connection is first made, it is quarantined until it logs in.
 *
 * <p>When quarantined, all messages sent by the node are sent to this quarantine conversation.
 *
 * <p>The quarantine conversation can only write to the node across the socket from it.
 *
 * <p>All messages sent to a conversation must be done in the Decode thread.
 */
public abstract class QuarantineConversation {
  /** The action to be performed after reading a message. */
  public enum Action {
    NONE,
    TERMINATE,
    UNQUARANTINE
  }

  /** A message has been read. What should we do? */
  public abstract Action message(Serializable serializable);

  /**
   * called if this conversation has been removed, either after a TERMINATE was returned from a
   * message, or the channel has been closed.
   */
  public abstract void close();
}
