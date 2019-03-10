package games.strategy.net;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import games.strategy.engine.framework.system.SystemProperties;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * Written very often over the network, so make externalizable to make faster and reduce traffic.
 */
@ToString
@Getter(onMethod_ = {@Override})
@AllArgsConstructor
@EqualsAndHashCode(exclude = "name")
public class Node implements INode {
  public static final INode NULL_NODE;
  private static final long serialVersionUID = -2908980662926959943L;

  private final String name;
  private final InetAddress address;
  private final int port;

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
