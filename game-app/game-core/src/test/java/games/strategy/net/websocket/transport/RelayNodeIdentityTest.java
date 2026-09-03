package games.strategy.net.websocket.transport;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import games.strategy.net.INode;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Node identity is address+port with the name excluded, and it is serialized inside every message
 * header, so the same relay nodeId MUST map to an {@code .equals()} {@link INode} on every VM.
 */
class RelayNodeIdentityTest {

  @Test
  @DisplayName("Same nodeId maps to equal nodes across VMs, even with different display names")
  void sameNodeIdIsEqualAcrossVms() {
    final String nodeId = UUID.randomUUID().toString();

    // Simulate two VMs building the same node: identity must not depend on the display name.
    final INode asSeenLocally = RelayNodeIdentity.toNode(nodeId, "player-as-it-sees-itself");
    final INode asSeenByPeer = RelayNodeIdentity.toNode(nodeId, "player (2)");

    assertThat(asSeenByPeer, is(equalTo(asSeenLocally)));
    assertThat(asSeenByPeer.hashCode(), is(equalTo(asSeenLocally.hashCode())));
  }

  @Test
  @DisplayName("Different nodeIds map to unequal nodes")
  void differentNodeIdsAreUnequal() {
    final INode a = RelayNodeIdentity.toNode(UUID.randomUUID().toString(), "same-name");
    final INode b = RelayNodeIdentity.toNode(UUID.randomUUID().toString(), "same-name");

    assertThat(b, is(not(equalTo(a))));
  }
}
