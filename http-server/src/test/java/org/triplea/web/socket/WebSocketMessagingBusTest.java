package org.triplea.web.socket;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.hamcrest.core.StringContains.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.websocket.Session;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.http.client.web.socket.MessageEnvelope;
import org.triplea.http.client.web.socket.messages.MessageType;
import org.triplea.http.client.web.socket.messages.WebSocketMessage;
import org.triplea.http.client.web.socket.messages.envelopes.ServerErrorMessage;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("InnerClassMayBeStatic")
class WebSocketMessagingBusTest {
  private static class StringMessage implements WebSocketMessage {
    private static final MessageType<StringMessage> TYPE = MessageType.of(StringMessage.class);

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  @EqualsAndHashCode
  @AllArgsConstructor
  private static class BooleanMessage implements WebSocketMessage {
    private static final MessageType<BooleanMessage> TYPE = MessageType.of(BooleanMessage.class);

    private final boolean value;

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  @Mock private Consumer<WebSocketMessageContext<BooleanMessage>> booleanMessageListener;
  @Mock private Consumer<WebSocketMessageContext<BooleanMessage>> booleanMessageListenerSecond;
  @Mock private Consumer<WebSocketMessageContext<StringMessage>> stringMessageListener;
  @Mock private Session session;

  @Nested
  class MessageListening {

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("Add a listener, trigger message receipt, verify listener is invoked")
    void invokeListener() {
      final WebSocketMessagingBus webSocketMessagingBus = new WebSocketMessagingBus();
      webSocketMessagingBus.addListener(BooleanMessage.TYPE, booleanMessageListener);

      // trigger message
      final BooleanMessage message = new BooleanMessage(true);
      webSocketMessagingBus.onMessage(session, message.toEnvelope());

      // capture argument passed to listener
      final ArgumentCaptor<WebSocketMessageContext<BooleanMessage>> argumentCaptor =
          ArgumentCaptor.forClass(WebSocketMessageContext.class);
      verify(booleanMessageListener).accept(argumentCaptor.capture());

      // verify arg values
      final WebSocketMessageContext<BooleanMessage> arg = argumentCaptor.getValue();
      assertThat("Message received should be equal to message sent", arg.getMessage(), is(message));
      assertThat(
          "Expect new object created from JSON string",
          arg.getMessage(),
          not(sameInstance(message)));
      assertThat(
          "Session should be passed along to the listener", arg.getSenderSession(), is(session));
    }

    @DisplayName(
        "Add listeners of different types, trigger message receipt, "
            + "verify listener of matching type is invoked")
    @Test
    void invokesCorrectListener() {
      final WebSocketMessagingBus webSocketMessagingBus = new WebSocketMessagingBus();
      webSocketMessagingBus.addListener(BooleanMessage.TYPE, booleanMessageListener);
      webSocketMessagingBus.addListener(StringMessage.TYPE, stringMessageListener);

      webSocketMessagingBus.onMessage(session, new BooleanMessage(true).toEnvelope());

      verify(booleanMessageListener).accept(any());
      verify(stringMessageListener, never()).accept(any());
    }

    @DisplayName(
        "Add multiple listeners of same type, trigger message, verify all listeners invoked")
    @Test
    void invokesMultipleListeners() {
      final WebSocketMessagingBus webSocketMessagingBus = new WebSocketMessagingBus();
      webSocketMessagingBus.addListener(BooleanMessage.TYPE, booleanMessageListener);
      webSocketMessagingBus.addListener(BooleanMessage.TYPE, booleanMessageListenerSecond);

      webSocketMessagingBus.onMessage(session, new BooleanMessage(true).toEnvelope());

      verify(booleanMessageListener).accept(any());
      verify(booleanMessageListenerSecond).accept(any());
    }
  }

  @Nested
  class SendMessages {
    @Mock private MessageSender messageSender;
    @Mock private MessageBroadcaster messageBroadcaster;
    @Mock private SessionSet sessionSet;
    @InjectMocks private WebSocketMessagingBus webSocketMessagingBus;

    @Mock private Session session;

    @Test
    @DisplayName("Send response should forward to message sender implementation")
    void sendResponse() {
      final var booleanMessage = new BooleanMessage(true);

      webSocketMessagingBus.sendResponse(session, booleanMessage);

      verify(messageSender).accept(session, booleanMessage.toEnvelope());
    }

    @Test
    @DisplayName(
        "Broadcast should forward all sessions from session set "
            + "and the broadcasted message to broadcaster")
    void broadcast() {
      final Collection<Session> sessions = List.of(session);
      when(sessionSet.getSessions()).thenReturn(sessions);
      final var booleanMessage = new BooleanMessage(true);

      webSocketMessagingBus.broadcastMessage(booleanMessage);

      verify(messageBroadcaster).accept(sessions, booleanMessage.toEnvelope());
    }
  }

  @Nested
  class SessionDisconnectedListener {
    @Mock private MessageSender messageSender;
    @Mock private MessageBroadcaster messageBroadcaster;
    @Mock private SessionSet sessionSet;
    @InjectMocks private WebSocketMessagingBus webSocketMessagingBus;

    @Mock private BiConsumer<WebSocketMessagingBus, Session> disconnectListener;
    @Mock private Session session;

    @Test
    void invokeSessionDisconnectedListener() {
      webSocketMessagingBus.addSessionDisconnectListener(disconnectListener);

      webSocketMessagingBus.onClose(session);

      verify(disconnectListener).accept(webSocketMessagingBus, session);
    }

    @Test
    void sessionCloseRemoveSessionFromSessionSet() {
      webSocketMessagingBus.onClose(session);

      verify(sessionSet).remove(session);
    }
  }

  @Nested
  class OnOpen {
    @Mock private MessageSender messageSender;
    @Mock private MessageBroadcaster messageBroadcaster;
    @Mock private SessionSet sessionSet;
    @InjectMocks private WebSocketMessagingBus webSocketMessagingBus;

    @Mock private Session session;

    @Test
    void onOpenAddsToSessionSet() {
      webSocketMessagingBus.onOpen(session);

      verify(sessionSet).put(session);
    }
  }

  @Nested
  class OnError {
    @Mock private MessageSender messageSender;
    @Mock private MessageBroadcaster messageBroadcaster;
    @Mock private SessionSet sessionSet;
    @InjectMocks private WebSocketMessagingBus webSocketMessagingBus;

    @Mock private Session session;

    @Test
    void onOpenAddsToSessionSet() {
      final var throwable = new Throwable("error message");
      webSocketMessagingBus.onError(session, throwable);

      final ArgumentCaptor<MessageEnvelope> messageCaptor =
          ArgumentCaptor.forClass(MessageEnvelope.class);
      verify(messageSender).accept(eq(session), messageCaptor.capture());

      assertThat(
          "Make sure message type id is error message",
          messageCaptor.getValue().getMessageTypeId(),
          is(ServerErrorMessage.TYPE.getMessageTypeId()));

      assertThat(
          "Make sure the return message to the user does *not* contain the "
              + "error message from the throwable. If a user can craft an interesting error "
              + "message, it could potentially be used in an attack. Therefore we return an error "
              + "ID to the user instead of returning the underlying error message",
          messageCaptor.getValue().getPayload(ServerErrorMessage.TYPE.getPayloadType()).getError(),
          not(containsString("error message")));
    }
  }
}
