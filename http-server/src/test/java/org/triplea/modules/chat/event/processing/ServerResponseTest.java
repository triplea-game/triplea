package org.triplea.modules.chat.event.processing;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.http.client.web.socket.messages.ServerMessageEnvelope;

@ExtendWith(MockitoExtension.class)
class ServerResponseTest {

  @Mock private ServerMessageEnvelope serverEventEnvelope;

  @Test
  void broadCastMessage() {
    final ServerResponse serverResponse = ServerResponse.broadcast(serverEventEnvelope);

    final boolean result = serverResponse.isBroadcast();

    assertThat(result, is(true));
  }

  @Test
  void backToClientMessage() {
    final ServerResponse serverResponse = ServerResponse.backToClient(serverEventEnvelope);

    final boolean result = serverResponse.isBroadcast();

    assertThat(result, is(false));
  }
}
