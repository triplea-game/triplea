package games.strategy.net;

import java.io.Serializable;

/**
 * The envelope for a message consisting of both the header and payload. The header specifies the source and
 * destination nodes of the message.
 */
public class MessageHeader {
  // if null, then a broadcast
  private final INode to;
  private final Serializable message;
  private final INode from;

  /**
   * Creates a broadcast message.
   */
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

  public Serializable getMessage() {
    return message;
  }

  @Override
  public String toString() {
    return "Message header. msg:" + message + " to:" + to + " from:" + from;
  }
}
