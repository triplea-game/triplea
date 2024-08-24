package org.triplea.web.socket;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.gson.Gson;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NonNls;
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

  @NonNls private static final String SERVER_MESSAGE_JSON = new Gson().toJson(MESSAGE_ENVELOPE);

  @Mock private WebSocketSession session;

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
    verify(session, never()).sendText(any());
  }

  @Test
  void sendMessage() {
    when(session.isOpen()).thenReturn(true);

    new MessageSender().accept(session, MESSAGE_ENVELOPE);

    verify(session, timeout(1000)).sendText(SERVER_MESSAGE_JSON);
  }
}
