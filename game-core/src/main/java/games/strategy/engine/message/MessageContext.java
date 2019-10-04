package games.strategy.engine.message;

import games.strategy.net.INode;

/** Information useful on invocation of remote networked events. */
public final class MessageContext {
  // the current caller of the remote or channel
  private static final ThreadLocal<INode> sender = new ThreadLocal<>();

  private MessageContext() {}

  // should only be called by EndPoint
  public static void setSenderNodeForThread(final INode node) {
    sender.set(node);
  }

  /**
   * Within the invocation on a remote method on an IRemote or an IChannelSubscriber, this method
   * will return the node that originated the message.
   *
   * <p>Will return null if the current thread is not currently executing a remote method of an
   * IRemote or IChannelSubscriber.
   *
   * <p>This is set by the server, and cannot be overwritten by the client, and can be used to
   * verify where messages come from.
   *
   * @return the node that originated the message being received
   */
  public static INode getSender() {
    return sender.get();
  }
}
