package games.strategy.net;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import games.strategy.engine.framework.system.SystemProperties;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Written very often over the network, so make externalizable to make faster and reduce traffic.
 */
@ToString
@Getter(onMethod_ = {@Override})
// NoArgsConstructor needed to support Externalizable
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = "name")
public class Node implements INode, Externalizable {
  public static final INode NULL_NODE;
  private static final long serialVersionUID = -2908980662926959943L;

  private String name;
  private InetAddress address;
  private int port;

  static {
    try {
      NULL_NODE = new Node("NULL", getLocalHost(), -1);
    } catch (final UnknownHostException e) {
      throw new IllegalStateException(e);
    }
  }

  public Node(final String name, final InetSocketAddress address) {
    this(name, address.getAddress(), address.getPort());
  }

  /** Returns the localhost InetAddress. */
  static InetAddress getLocalHost() throws UnknownHostException {
    // On Mac, InetAddress.getLocalHost() can be extremely slow (30 seconds)
    // due to a bug in macOS Sierra and higher. Use a work around to avoid
    // this. See: https://thoeni.io/post/macos-sierra-java/
    return SystemProperties.isMac() ? InetAddress.getByName("localhost") : InetAddress.getLocalHost();
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
