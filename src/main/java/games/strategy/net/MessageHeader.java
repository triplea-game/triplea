package games.strategy.net;

import java.io.Serializable;

// written over the network very often, so make externalizable to
// increase performance
public class MessageHeader {
  // if null, then a broadcast
  private final INode m_for;
  private final Serializable m_message;
  private final INode m_from;

  /**
   * Creates a broadcast message.
   */
  public MessageHeader(final INode from, final Serializable message) {
    this(null, from, message);
  }

  public MessageHeader(final INode to, final INode from, final Serializable message) {
    // for can be null if we are a broadcast
    m_for = to;
    // from can be null if the sending node doesnt know its own address
    m_from = from;
    m_message = message;
  }

  /**
   * null if a broadcast.
   */
  public INode getFor() {
    return m_for;
  }

  public INode getFrom() {
    return m_from;
  }

  public boolean isBroadcast() {
    return m_for == null;
  }

  public Serializable getMessage() {
    return m_message;
  }

  @Override
  public String toString() {
    return "Message header. msg:" + m_message + " to:" + m_for + " from:" + m_from;
  }
}
