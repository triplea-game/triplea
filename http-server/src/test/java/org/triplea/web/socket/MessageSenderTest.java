package org.triplea.web.socket;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.gson.Gson;
import java.util.concurrent.Future;
import javax.websocket.RemoteEndpoint;
import javax.websocket.Session;
import lombok.AllArgsConstructor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.http.client.web.socket.MessageEnvelope;
import org.triplea.http.client.web.socket.messages.MessageType;
import org.triplea.http.client.web.socket.messages.WebSocketMessage;

@ExtendWith(MockitoExtension.class)
class MessageSenderTest {

  private static final MessageEnvelope MESSAGE_ENVELOPE =
      new StringMessage("message!").toEnvelope();

  private static final String SERVER_MESSAGE_JSON = new Gson().toJson(MESSAGE_ENVELOPE);

  @Mock private Session session;
  @Mock private RemoteEndpoint.Async asyncRemote;
  @Mock private Future<Void> future;

  @AllArgsConstructor
  private static class StringMessage implements WebSocketMessage {
    private static final MessageType<StringMessage> TYPE = MessageType.of(StringMessage.class);

    @SuppressWarnings("unused")
    private final String message;

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  @Test
  void sendToOnlyOpenSession() {
    new MessageSender().accept(session, MESSAGE_ENVELOPE);

    verify(session, timeout(500)).isOpen();
    verify(session, never()).getAsyncRemote();
  }

  @Test
  void sendMessage() throws Exception {
    when(session.isOpen()).thenReturn(true);
    when(session.getAsyncRemote()).thenReturn(asyncRemote);
    when(asyncRemote.sendText(SERVER_MESSAGE_JSON)).thenReturn(future);

    new MessageSender().accept(session, MESSAGE_ENVELOPE);

    verify(future, timeout(500)).get();
  }
}
