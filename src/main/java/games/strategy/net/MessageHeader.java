package games.strategy.net;

import java.io.Serializable;

// written over the network very often, so make externalizable to
// increase performance
public class MessageHeader {
  // if null, then a broadcast
  private final INode to;
  private final Serializable message;
  private final INode from;

  /**
   * Creates a broadcast message.
   */
  public MessageHeader(final INode from, final Serializable message) {
    this(null, from, message);
  }

  public MessageHeader(final INode to, final INode from, final Serializable message) {
    // for can be null if we are a broadcast
    this.to = to;
    // from can be null if the sending node doesnt know its own address
    this.from = from;
    this.message = message;
  }

  /**
   * null if a broadcast.
   */
  public INode getFor() {
    return to;
  }

  public INode getFrom() {
    return from;
  }

  public boolean isBroadcast() {
    return to == null;
  }

  public Serializable getMessage() {
    return message;
  }

  @Override
  public String toString() {
    return "Message header. msg:" + message + " to:" + to + " from:" + from;
  }
}
