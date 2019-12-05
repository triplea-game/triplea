package org.triplea.http.client.remote.actions.messages.server;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.text.IsEmptyString.emptyString;

import java.net.InetAddress;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.http.client.IpAddressParser;
import org.triplea.http.client.web.socket.messages.ServerMessageEnvelope;

@ExtendWith(MockitoExtension.class)
class RemoteActionsEnvelopeFactoryTest {

  private static final InetAddress IP = IpAddressParser.fromString("99.99.33.33");

  @Test
  void shutdownMessage() {
    final ServerMessageEnvelope serverMessageEnvelope =
        RemoteActionsEnvelopeFactory.newShutdownMessage();

    assertThat(
        serverMessageEnvelope.getMessageType(),
        is(ServerRemoteActionMessageType.SHUTDOWN.toString()));
    assertThat(serverMessageEnvelope.getPayload(String.class), is(emptyString()));
  }

  @Test
  void playerBannedMessage() {
    final ServerMessageEnvelope serverMessageEnvelope =
        RemoteActionsEnvelopeFactory.newBannedPlayer(IP);

    assertThat(
        serverMessageEnvelope.getMessageType(),
        is(ServerRemoteActionMessageType.PLAYER_BANNED.toString()));
    assertThat(serverMessageEnvelope.getPayload(InetAddress.class), is(IP));
  }
}
