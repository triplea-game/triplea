package games.strategy.net;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import org.triplea.domain.data.PlayerName;

/**
 * A Node in a network.
 *
 * <p>Node identity is based on address/port. The name is just a display name
 *
 * <p>Since different nodes may appear as different addresses to different nodes (eg the server sees
 * a node as its nat accessible address, while the node itself sees itself as a subnet address), the
 * address for a node is defined as the address that the server sees!
 */
public interface INode extends Serializable, Comparable<INode> {
  /** Returns the display/user name for the node. */
  String getName();

  default PlayerName getPlayerName() {
    return PlayerName.of(getName());
  }

  /**
   * Returns the address for the node as seen by the server. <br>
   * WARNING! 'getAddress().getHostAddress(); can return IP address *and* network interface, eg:
   * 2603:603:f00:ed0:5d12:e3b4:a4d3:c2ea%enp0s10, notice the trailing "enp0s10"
   */
  InetAddress getAddress();

  /** Returns the port for the node as seen by the server. */
  int getPort();

  /** Returns the address for the node as seen by the server. */
  InetSocketAddress getSocketAddress();

  /** Returns the IP address of this node. */
  default String getIpAddress() {
    return getSocketAddress().getAddress().getHostAddress();
  }
}
