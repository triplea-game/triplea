package games.strategy.engine.framework.startup.mc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import games.strategy.net.INode;
import games.strategy.net.IServerMessenger;
import games.strategy.net.Node;
import java.net.InetSocketAddress;
import java.util.Set;
import org.jetbrains.annotations.NonNls;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.java.IpAddressParser;

@SuppressWarnings("SameParameterValue")
@ExtendWith(MockitoExtension.class)
class PlayerDisconnectActionTest {

  private static final INode NODE_1 =
      new Node("node-1", InetSocketAddress.createUnresolved("99.99.99.100", 5000));

  @NonNls private static final String NODE_2_NAME = "node-2";
  private static final INode NODE_2 =
      new Node(NODE_2_NAME + " (2)", InetSocketAddress.createUnresolved("2.2.2.2", 8000));

  @Mock private IServerMessenger serverMessenger;
  @Mock private Runnable shutdownCallback;

  @InjectMocks private PlayerDisconnectAction playerDisconnect;

  @Test
  @DisplayName("Disconnecting a different Node than what is present is a no-op")
  void disconnectIsNoOpWhenNoPlayersPresent() {
    givenServerNodeIs(NODE_1);
    givenNodes(NODE_1);

    playerDisconnect.accept(IpAddressParser.fromString(NODE_2.getIpAddress()));

    verifyNoConnectionsRemoved();
    verify(shutdownCallback, never()).run();
  }

  private void givenServerNodeIs(final INode node) {
    when(serverMessenger.getLocalNode()).thenReturn(node);
  }

  private void givenNodes(final INode... nodes) {
    when(serverMessenger.getNodes()).thenReturn(Set.of(nodes));
  }

  private void verifyNoConnectionsRemoved() {
    verify(serverMessenger, never()).removeConnection(any());
  }

  @Test
  @DisplayName("Disconnecting the server node invokes shutdown")
  void disconnectHost() {
    givenServerNodeIs(NODE_1);

    playerDisconnect.accept(IpAddressParser.fromString(NODE_1.getIpAddress()));

    verify(shutdownCallback).run();
  }

  @Test
  @DisplayName("Disconnecting a player removes their server messenger connection")
  void disconnectPlayer() {
    givenServerNodeIs(NODE_1);
    givenNodes(NODE_1, NODE_2);

    playerDisconnect.accept(IpAddressParser.fromString(NODE_2.getIpAddress()));

    verifyNodeWasDisconnected(NODE_2);
  }

  private void verifyNodeWasDisconnected(final INode node) {
    verify(serverMessenger, atLeastOnce()).removeConnection(node);
  }
}
