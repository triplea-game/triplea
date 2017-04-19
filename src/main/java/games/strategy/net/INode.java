package games.strategy.net;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.InetSocketAddress;

/**
 * A Node in a network.
 *
 * <p>
 * Node identity is based on address/port. The name is just a display name
 * </p>
 *
 * <p>
 * Since different nodes may appear as different adresses to different nodes (eg the server sees a node as its nat
 * accesseble adress, while
 * the node itself sees itself as a subnet address), the address for a node is defined as the address that the server
 * sees!
 * </p>
 */
public interface INode extends Serializable, Comparable<INode> {
  /**
   * @return The display/user name for the node.
   */
  String getName();

  /**
   * @return The address for the node as seen by the server.
   */
  InetAddress getAddress();

  /**
   * @return The port for the node as seen by the server.
   */
  int getPort();

  /**
   * @return The address for the node as seen by the server.
   */
  InetSocketAddress getSocketAddress();
}
