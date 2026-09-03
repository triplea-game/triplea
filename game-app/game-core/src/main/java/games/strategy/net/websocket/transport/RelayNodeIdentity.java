package games.strategy.net.websocket.transport;

import games.strategy.net.INode;
import games.strategy.net.Node;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * Turns a relay-assigned {@code nodeId} (a UUID string) into a deterministic {@link INode}.
 *
 * <p>{@link INode} identity is {@code InetAddress}+port with the name excluded (see {@link Node}),
 * and the whole engine keys its maps on {@link INode}. Over a relay every peer shares the relay's
 * real socket address, so a synthetic address must be derived from the relay-assigned id such that
 * <em>every</em> VM computes the same {@link INode} for the same node (its own {@code getLocalNode}
 * and the same node as a peer sees it must be {@code .equals()}).
 *
 * <p>The synthetic address is the 16 bytes of the UUID interpreted as an IPv6 address with a fixed
 * port. 128 bits of UUID make collisions between distinct nodes effectively impossible, and the
 * mapping is pure so it is stable and identical across processes.
 */
final class RelayNodeIdentity {
  /**
   * Fixed synthetic port. Node identity is address+port; the address already carries the full 128
   * bits of the id, so the port is a constant and does not affect uniqueness.
   */
  static final int SYNTHETIC_PORT = 6000;

  private RelayNodeIdentity() {}

  static INode toNode(final String nodeId, final String name) {
    final UUID uuid = UUID.fromString(nodeId);
    final byte[] bytes =
        ByteBuffer.allocate(16)
            .putLong(uuid.getMostSignificantBits())
            .putLong(uuid.getLeastSignificantBits())
            .array();
    final InetAddress syntheticAddress;
    try {
      // A 16-byte address yields an Inet6Address with no DNS lookup; equal bytes => equal address.
      syntheticAddress = InetAddress.getByAddress(bytes);
    } catch (final UnknownHostException e) {
      // Only thrown for a wrong-length address; 16 bytes is always valid.
      throw new IllegalStateException("Failed to build synthetic address for nodeId " + nodeId, e);
    }
    return new Node(name, syntheticAddress, SYNTHETIC_PORT);
  }
}
