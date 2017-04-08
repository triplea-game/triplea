package games.strategy.net;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

// written very often over the network, so make externalizable to make faster and reduce traffic
public class Node implements INode, Externalizable {
  static final long serialVersionUID = -2908980662926959943L;
  private String name;
  private int port;
  private InetAddress m_address;
  public static final INode NULL_NODE;

  static {
    try {
      NULL_NODE = new Node("NULL", InetAddress.getLocalHost(), -1);
    } catch (final UnknownHostException e) {
      throw new IllegalStateException(e);
    }
  }

  // needed to support Externalizable
  public Node() {}

  /** Creates new Node. */
  public Node(final String name, final InetSocketAddress address) {
    this.name = name;
    m_address = address.getAddress();
    port = address.getPort();
  }

  /** Creates new Node. */
  public Node(final String name, final InetAddress address, final int port) {
    this.name = name;
    m_address = address;
    this.port = port;
  }

  @Override
  public String getName() {
    return name;
  }

  /**
   * Node equality is done based on network adress/port.
   * The name is not part of the node identity.
   */
  @Override
  public boolean equals(final Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof Node)) {
      return false;
    }
    final Node other = (Node) obj;
    return other.port == this.port && other.m_address.equals(this.m_address);
  }

  @Override
  public int hashCode() {
    return (37 * port) + m_address.hashCode();
  }

  @Override
  public String toString() {
    return name + " port:" + port + " ip:" + m_address.getHostAddress();
  }

  @Override
  public int getPort() {
    return port;
  }

  @Override
  public InetAddress getAddress() {
    return m_address;
  }

  @Override
  public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
    name = in.readUTF();
    port = in.readInt();
    final int length = in.read();
    final byte[] bytes = new byte[length];
    for (int i = 0; i < length; i++) {
      bytes[i] = in.readByte();
    }
    m_address = InetAddress.getByAddress(bytes);
  }

  @Override
  public void writeExternal(final ObjectOutput out) throws IOException {
    out.writeUTF(name);
    out.writeInt(port);
    out.write(m_address.getAddress().length);
    out.write(m_address.getAddress());
  }

  @Override
  public int compareTo(final INode o) {
    if (o == null) {
      return -1;
    }
    return this.name.compareToIgnoreCase(o.getName());
  }

  @Override
  public InetSocketAddress getSocketAddress() {
    return new InetSocketAddress(m_address, port);
  }
}
