package org.triplea.server.http.web.socket;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.gson.Gson;
import java.util.concurrent.Future;
import javax.websocket.RemoteEndpoint;
import javax.websocket.Session;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.http.client.web.socket.messages.ServerMessageEnvelope;

@ExtendWith(MockitoExtension.class)
class MessageSenderTest {

  private static final ServerMessageEnvelope SERVER_MESSAGE =
      ServerMessageEnvelope.packageMessage("messageType", "message");

  private static final String SERVER_MESSAGE_JSON = new Gson().toJson(SERVER_MESSAGE);

  @Mock private Session session;
  @Mock private RemoteEndpoint.Async asyncRemote;
  @Mock private Future<Void> future;

  @Test
  void sendToOnlyOpenSession() {
    new MessageSender().accept(session, SERVER_MESSAGE);

    verify(session, timeout(500)).isOpen();
    verify(session, never()).getAsyncRemote();
  }

  @Test
  void sendMessage() throws Exception {
    when(session.isOpen()).thenReturn(true);
    when(session.getAsyncRemote()).thenReturn(asyncRemote);
    when(asyncRemote.sendText(SERVER_MESSAGE_JSON)).thenReturn(future);

    new MessageSender().accept(session, SERVER_MESSAGE);

    verify(future, timeout(500)).get();
  }
}
