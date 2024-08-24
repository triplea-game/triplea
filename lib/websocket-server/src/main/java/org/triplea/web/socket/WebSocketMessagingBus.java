package org.triplea.web.socket;

import com.google.common.annotations.VisibleForTesting;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NonNls;
import org.triplea.http.client.web.socket.MessageEnvelope;
import org.triplea.http.client.web.socket.messages.MessageType;
import org.triplea.http.client.web.socket.messages.WebSocketMessage;
import org.triplea.http.client.web.socket.messages.envelopes.ServerErrorMessage;

@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @VisibleForTesting)
@Slf4j
public class WebSocketMessagingBus {
  @NonNls public static final String MESSAGING_BUS_KEY = "messaging.bus";

  @Nonnull private final MessageBroadcaster messageBroadcaster;
  @Nonnull private final MessageSender messageSender;
  @Nonnull private final SessionSet sessionSet;

  private final List<BiConsumer<WebSocketMessagingBus, WebSocketSession>> sessionClosedListeners =
      new ArrayList<>();

  @Value
  private static class MessageListener<T extends WebSocketMessage> {
    MessageType<T> messageType;
    Consumer<WebSocketMessageContext<T>> listener;
  }

  /** These listeners are for specific message types. */
  private final List<MessageListener<?>> messageListeners = new ArrayList<>();

  /** These listeners are invoked when we receive any type of message. */
  private final List<Consumer<MessageEnvelope>> anyMessageListeners = new ArrayList<>();

  public WebSocketMessagingBus() {
    messageSender = new MessageSender();
    messageBroadcaster = new MessageBroadcaster(messageSender);
    sessionSet = new SessionSet();
  }

  public <X extends WebSocketMessage> void sendResponse(
      final WebSocketSession session, final X responseMessage) {
    messageSender.accept(session, responseMessage.toEnvelope());
  }

  public <X extends WebSocketMessage> void broadcastMessage(final X broadcastMessage) {
    broadcastMessage(broadcastMessage.toEnvelope());
  }

  public void broadcastMessage(final MessageEnvelope messageEnvelope) {
    messageBroadcaster.accept(sessionSet.getSessions(), messageEnvelope);
  }

  /**
   * Adds a listener for specific message types. The messaging bus will automatically exclude any
   * messages that are not of a matching type.
   *
   * @param type The message type to listen for.
   * @param listener The listener that will be invoked with messages of a matching type.
   */
  public <T extends WebSocketMessage> void addMessageListener(
      final MessageType<T> type, final Consumer<WebSocketMessageContext<T>> listener) {
    messageListeners.add(new MessageListener<>(type, listener));
  }

  /**
   * Adds a listener that will be invoked when any message is received
   *
   * @param messageListener The message listener to be added, will be invoked with any message
   *     received.
   */
  public void addMessageListener(final Consumer<MessageEnvelope> messageListener) {
    anyMessageListeners.add(messageListener);
  }

  @SuppressWarnings("unchecked")
  <T extends WebSocketMessage> void onMessage(
      final WebSocketSession session, final MessageEnvelope envelope) {
    anyMessageListeners.forEach(listener -> listener.accept(envelope));

    determineMatchingMessageType(envelope)
        .ifPresent(
            messageType -> {
              final T payload = (T) envelope.getPayload(messageType.getPayloadType());

              getListenersForMessageTypeId(envelope.getMessageTypeId())
                  .map(messageListener -> (MessageListener<T>) messageListener)
                  .forEach(
                      messageListener ->
                          messageListener.listener.accept(
                              WebSocketMessageContext.<T>builder()
                                  .messagingBus(this)
                                  .senderSession(session)
                                  .message(payload)
                                  .build()));
            });
  }

  private Optional<MessageType<?>> determineMatchingMessageType(final MessageEnvelope envelope) {
    return messageListeners.stream()
        .filter(matchListenersWithMessageTypeId(envelope.getMessageTypeId()))
        .findAny()
        .map(messageListener -> messageListener.messageType);
  }

  private static Predicate<MessageListener<?>> matchListenersWithMessageTypeId(
      @NonNls final String messageTypeId) {
    return messageListener -> messageListener.messageType.getMessageTypeId().equals(messageTypeId);
  }

  private Stream<MessageListener<?>> getListenersForMessageTypeId(
      @NonNls final String messageTypeId) {
    return messageListeners.stream()
        .filter(
            messageListener ->
                messageListener.messageType.getMessageTypeId().equals(messageTypeId));
  }

  public void addSessionDisconnectListener(
      final BiConsumer<WebSocketMessagingBus, WebSocketSession> listener) {
    sessionClosedListeners.add(listener);
  }

  void onClose(final WebSocketSession session) {
    sessionSet.remove(session);
    sessionClosedListeners.forEach(listener -> listener.accept(this, session));
  }

  void onOpen(final WebSocketSession session) {
    sessionSet.put(session);
  }

  void onError(final WebSocketSession session, final Throwable throwable) {
    @NonNls final String errorId = UUID.randomUUID().toString();
    log.error(
        "Error-id processing websocket message, returning an error message to user. "
            + "Error id: {}",
        errorId,
        throwable);

    messageSender.accept(
        session,
        new ServerErrorMessage(
                "Server error, error-id#" + errorId + ", please report this to TripleA")
            .toEnvelope());
  }
}
