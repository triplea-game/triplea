package org.triplea.web.socket;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import java.net.InetAddress;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.websocket.CloseReason;
import javax.websocket.Session;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.WebSocket;
import org.triplea.http.client.web.socket.MessageEnvelope;
import org.triplea.http.client.web.socket.messages.envelopes.ServerErrorMessage;
import org.triplea.java.StringUtils;

/**
 * Extracts common code between websocket server methods. Each websocket endpoint is essentially
 * identical and they delegate their behavior to a 'messagingBus' which has listeners that will act
 * on messages received. In general there should not be many websocket endpoints and they are
 * grouped by the "types" of connections that are created to them (eg: players or games)
 */
@Slf4j
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class GenericWebSocket {
  @VisibleForTesting static final int MAX_BAD_MESSAGES = 2;

  private static final Gson GSON = new Gson();
  private static final Cache<InetAddress, AtomicInteger> badMessageCache =
      Caffeine.newBuilder().expireAfterWrite(Duration.ofSeconds(30)).build();
  private static final Map<Class<?>, GenericWebSocket> websockets = new HashMap<>();

  @Nonnull private final WebSocketMessagingBus webSocketMessagingBus;
  @Nullable private final Predicate<InetAddress> banCheck;
  @Nonnull private final MessageSender messageSender;

  public GenericWebSocket(@Nonnull final WebSocketMessagingBus webSocketMessagingBus) {
    this(webSocketMessagingBus, ip -> false, new MessageSender());
  }

  public static void init(
      final Class<?> websocketClass,
      final WebSocketMessagingBus webSocketMessagingBus,
      @Nullable final Predicate<InetAddress> banCheck) {

    final var genericWebsocket =
        new GenericWebSocket(webSocketMessagingBus, banCheck, new MessageSender());
    websockets.put(websocketClass, genericWebsocket);
  }

  public static GenericWebSocket getInstance(final Class<?> websocketClass) {
    return Preconditions.checkNotNull(
        websockets.get(websocketClass),
        "Error, unable to find generic websocket for: "
            + websocketClass
            + ", did you run GenericWebSocket.init("
            + websocketClass
            + ", ...) ?");
  }

  void onOpen(final WebSocket webSocket) {
    onOpen(WebSocketSessionAdapter.fromWebSocket(webSocket));
  }

  void onOpen(final WebSocketSession session) {
    if (isSessionBanned(session)) {
      disconnectBannedSession(session);
    } else {
      webSocketMessagingBus.onOpen(session);
    }
  }

  private boolean isSessionBanned(final WebSocketSession session) {
    return Optional.ofNullable(banCheck)
        .map(check -> check.test(session.getRemoteAddress()))
        .orElse(false);
  }

  private static void disconnectBannedSession(final WebSocketSession session) {
    session.close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, "You have been banned"));
  }

  public void onMessage(final WebSocket webSocket, final String message) {
    onMessage(WebSocketSessionAdapter.fromWebSocket(webSocket), message, badMessageCache);
  }

  public void onMessage(final WebSocketSession session, final String message) {
    onMessage(session, message, badMessageCache);
  }

  @VisibleForTesting
  void onMessage(
      final WebSocketSession session,
      final String message,
      final Cache<InetAddress, AtomicInteger> badMessageCache) {
    readJsonMessage(session, message, badMessageCache)
        .ifPresent(envelope -> webSocketMessagingBus.onMessage(session, envelope));
  }

  /**
   * Checks if session has sent too many bad messages, if so we ignore messages from that session
   * and return an empty. Otherwise we will convert the message JSON string to a {@code
   * MessageEnvelope} and return that. If the message is badly formatted, we'll send back an error
   * message response to the session, increment the bad message count and return an empty.
   */
  private Optional<MessageEnvelope> readJsonMessage(
      final WebSocketSession session,
      final String message,
      final Cache<InetAddress, AtomicInteger> badMessageCache) {

    if (burnMessagesFromThisSession(session, badMessageCache)) {
      // Burn the message -> no-op
      // To conserve server resources, we do not even log the message.
      return Optional.empty();
    }

    try {
      return Optional.of(GSON.fromJson(message, MessageEnvelope.class));
    } catch (final JsonSyntaxException e) {
      final InetAddress inetAddress = session.getRemoteAddress();
      incrementBadMessageCount(session, badMessageCache);
      logBadMessage(inetAddress, message);
      respondWithServerError(messageSender, session);
      return Optional.empty();
    }
  }

  private static boolean burnMessagesFromThisSession(
      final WebSocketSession session, final Cache<InetAddress, AtomicInteger> badMessageCache) {
    final InetAddress inetAddress = session.getRemoteAddress();

    final int badMessageCount =
        Optional.ofNullable(badMessageCache.getIfPresent(inetAddress))
            .map(AtomicInteger::get)
            .orElse(0);

    return badMessageCount > MAX_BAD_MESSAGES;
  }

  private static void incrementBadMessageCount(
      final WebSocketSession session, final Cache<InetAddress, AtomicInteger> badMessageCache) {
    badMessageCache
        .asMap()
        .computeIfAbsent(session.getRemoteAddress(), inet -> new AtomicInteger(0))
        .incrementAndGet();
  }

  private static void logBadMessage(final InetAddress inetAddress, final String message) {
    log.warn(
        "Failed to decode JSON string from IP {}, into a MessageEnvelope: {}",
        inetAddress,
        StringUtils.truncate(message, 500));
  }

  private static void respondWithServerError(
      final MessageSender messageSender, final WebSocketSession session) {
    messageSender.accept(
        session,
        new ServerErrorMessage("Server is unable to process request, error reading message")
            .toEnvelope());
  }

  void onClose(final WebSocket webSocket, final CloseReason closeReason) {
    onClose(WebSocketSessionAdapter.fromWebSocket(webSocket), closeReason);
  }

  void onClose(final Session session, final CloseReason closeReason) {
    onClose(WebSocketSessionAdapter.fromSession(session), closeReason);
  }

  void onClose(final WebSocketSession session, final CloseReason closeReason) {
    webSocketMessagingBus.onClose(session);
  }

  public void onError(final WebSocket webSocket, final Throwable throwable) {
    onError(WebSocketSessionAdapter.fromWebSocket(webSocket), throwable);
  }

  public void onError(final Session session, final Throwable throwable) {
    onError(WebSocketSessionAdapter.fromSession(session), throwable);
  }

  void onError(final WebSocketSession session, final Throwable throwable) {
    webSocketMessagingBus.onError(session, throwable);
  }
}
