package games.strategy.net;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Objects;

import games.strategy.engine.framework.system.SystemProperties;

/**
 * Written very often over the network, so make externalizable to make faster and reduce traffic.
 */
public class Node implements INode, Externalizable {
  public static final INode NULL_NODE;
  private static final long serialVersionUID = -2908980662926959943L;

  private String name;
  private int port;
  private InetAddress address;

  static {
    try {
      NULL_NODE = new Node("NULL", getLocalHost(), -1);
    } catch (final UnknownHostException e) {
      throw new IllegalStateException(e);
    }
  }

  // needed to support Externalizable
  public Node() {}

  public Node(final String name, final InetSocketAddress address) {
    this.name = name;
    this.address = address.getAddress();
    port = address.getPort();
  }

  public Node(final String name, final InetAddress address, final int port) {
    this.name = name;
    this.address = address;
    this.port = port;
  }

  /** Returns the localhost InetAddress. */
  public static InetAddress getLocalHost() throws UnknownHostException {
    // On Mac, InetAddress.getLocalHost() can be extremely slow (30 seconds)
    // due to a bug in macOS Sierra and higher. Use a work around to avoid
    // this. See: https://thoeni.io/post/macos-sierra-java/
    return SystemProperties.isMac() ? InetAddress.getByName("localhost") : InetAddress.getLocalHost();
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
    return other.port == this.port && other.address.equals(this.address);
  }

  @Override
  public int hashCode() {
    return Objects.hash(port, address);
  }

  @Override
  public String toString() {
    return name + " port:" + port + " ip:" + address.getHostAddress();
  }

  @Override
  public int getPort() {
    return port;
  }

  @Override
  public InetAddress getAddress() {
    return address;
  }

  @Override
  public void readExternal(final ObjectInput in) throws IOException {
    name = in.readUTF();
    port = in.readInt();
    final int length = in.read();
    final byte[] bytes = new byte[length];
    for (int i = 0; i < length; i++) {
      bytes[i] = in.readByte();
    }
    address = InetAddress.getByAddress(bytes);
  }

  @Override
  public void writeExternal(final ObjectOutput out) throws IOException {
    out.writeUTF(name);
    out.writeInt(port);
    // InetAddress is Serializable, we should use that instead of implementing our own logic
    // in order to preserve the hostname if present in the next lobby-incompatible release
    out.write(address.getAddress().length);
    out.write(address.getAddress());
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
    return new InetSocketAddress(address, port);
  }
}
