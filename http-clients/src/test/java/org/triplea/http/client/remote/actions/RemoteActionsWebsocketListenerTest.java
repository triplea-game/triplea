package org.triplea.http.client.remote.actions;

import static org.mockito.Mockito.verify;

import java.net.InetAddress;
import java.util.function.Consumer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.http.client.IpAddressParser;
import org.triplea.http.client.remote.actions.messages.server.RemoteActionsEnvelopeFactory;
import org.triplea.http.client.web.socket.GenericWebSocketClient;

@ExtendWith(MockitoExtension.class)
class RemoteActionsWebsocketListenerTest {
  private static final InetAddress IP = IpAddressParser.fromString("99.99.55.99");

  @Mock private GenericWebSocketClient genericWebSocketClient;

  @InjectMocks private RemoteActionsWebsocketListener remoteActionsWebsocketListener;

  @Mock private Consumer<InetAddress> playerBannedAction;
  @Mock private Runnable shutdownServerAction;

  @Test
  @DisplayName("Make sure remote actions listeners registers as a listener to generic websocket")
  void constructorRegistersItselfAsMessageLIstener() {
    verify(genericWebSocketClient).addMessageListener(remoteActionsWebsocketListener);
  }

  @Test
  @DisplayName("Verify shutdown requests are routed to shutdown listener")
  void shutdownMessageTriggersListener() {
    remoteActionsWebsocketListener.addShutdownRequestListener(shutdownServerAction);

    remoteActionsWebsocketListener.accept(RemoteActionsEnvelopeFactory.newShutdownMessage());

    verify(shutdownServerAction).run();
  }

  @Test
  @DisplayName("Verify banned player requests are routed to player ban listener")
  void playerBannedListener() {
    remoteActionsWebsocketListener.addPlayerBannedListener(playerBannedAction);

    remoteActionsWebsocketListener.accept(RemoteActionsEnvelopeFactory.newBannedPlayer(IP));

    verify(playerBannedAction).accept(IP);
  }
}
