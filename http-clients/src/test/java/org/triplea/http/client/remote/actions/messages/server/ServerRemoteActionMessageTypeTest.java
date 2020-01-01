package org.triplea.http.client.remote.actions.messages.server;

import static org.mockito.Mockito.verify;

import java.net.InetAddress;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.http.client.IpAddressParser;

@ExtendWith(MockitoExtension.class)
class ServerRemoteActionMessageTypeTest {
  private static final InetAddress IP = IpAddressParser.fromString("55.66.77.99");

  @Mock private Consumer<String> shutdownListener;
  @Mock private Consumer<InetAddress> bannedPlayerListener;

  private RemoteActionListeners remoteActionListeners;

  @BeforeEach
  void setup() {
    remoteActionListeners =
        RemoteActionListeners.builder()
            .shutdownListener(shutdownListener)
            .bannedPlayerListener(bannedPlayerListener)
            .build();
  }

  @Test
  void shutdown() {
    ServerRemoteActionMessageType.SHUTDOWN.sendPayloadToListener(
        RemoteActionsEnvelopeFactory.newShutdownMessage(), remoteActionListeners);

    verify(shutdownListener).accept("");
  }

  @Test
  void playerBanned() {
    ServerRemoteActionMessageType.PLAYER_BANNED.sendPayloadToListener(
        RemoteActionsEnvelopeFactory.newBannedPlayer(IP), remoteActionListeners);

    verify(bannedPlayerListener).accept(IP);
  }
}
