package org.triplea.game.server.ban.check;

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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.domain.data.PlayerName;

@ExtendWith(MockitoExtension.class)
class PlayerDisconnectActionTest {
  private static final INode NODE_1 =
      new Node("node-1", InetSocketAddress.createUnresolved("1.1.1.1", 5000));

  private static final String NODE_2_NAME = "node-2";
  private static final INode NODE_2 =
      new Node(NODE_2_NAME + " (2)", InetSocketAddress.createUnresolved("2.2.2.2", 8000));

  @Mock private IServerMessenger serverMessenger;

  @InjectMocks private PlayerDisconnectAction playerDisconnect;

  @Test
  @DisplayName("Disconnecting a player with none present is a no-op")
  void disconnectIsNoOpWhenNoPlayersPresent() {
    givenNodes();

    playerDisconnect.accept(PlayerName.of("name"), "ip-address");

    verifyNoConnectionsRemoved();
  }

  private void givenNodes(final INode... nodes) {
    when(serverMessenger.getNodes()).thenReturn(Set.of(nodes));
  }

  private void verifyNoConnectionsRemoved() {
    verify(serverMessenger, never()).removeConnection(any());
  }

  @Test
  @DisplayName("Disconnecting a player not in the current node set is a no-op")
  void disconnectingPlayerNotPresentIsNoOp() {
    givenNodes(NODE_1, NODE_2);

    playerDisconnect.accept(PlayerName.of("name"), "ip-address");

    verifyNoConnectionsRemoved();
  }

  @Test
  @DisplayName("Disconnecting a player removes their server messenger connection")
  void disconnectPlayer() {
    givenNodes(NODE_1, NODE_2);

    playerDisconnect.accept(NODE_1.getPlayerName(), NODE_1.getIpAddress());
    playerDisconnect.accept(NODE_2.getPlayerName(), NODE_2.getIpAddress());

    verifyNodeWasDisconnected(NODE_1);
    verifyNodeWasDisconnected(NODE_2);
  }

  private void verifyNodeWasDisconnected(final INode node) {
    verify(serverMessenger, atLeastOnce()).removeConnection(node);
  }

  @Test
  @DisplayName("Players can be disconnected by name only, even if they have a new IP address")
  void disconnectPlayerByName() {
    givenNodes(NODE_1);

    playerDisconnect.accept(NODE_1.getPlayerName(), "some-new-ip-address");

    verifyNodeWasDisconnected(NODE_1);
  }

  @Test
  @DisplayName("Players can be disconnected by their IP address only, even if name mismatches")
  void disconnectPlayerByIp() {
    givenNodes(NODE_1);

    playerDisconnect.accept(PlayerName.of("new-player-name"), NODE_1.getIpAddress());

    verifyNodeWasDisconnected(NODE_1);
  }

  @Test
  @DisplayName("Name suffixes like ' (1)' are ignored when disconnecting players ")
  void disconnectingPlayersWillMatchNamePrefix() {
    givenNodes(NODE_1, NODE_2);

    playerDisconnect.accept(PlayerName.of(NODE_2_NAME), NODE_2.getIpAddress());

    verifyNodeWasDisconnected(NODE_2);
  }
}
