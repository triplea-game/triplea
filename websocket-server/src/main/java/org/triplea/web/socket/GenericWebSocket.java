package org.triplea.web.socket;

import static org.triplea.web.socket.WebSocketMessagingBus.MESSAGING_BUS_KEY;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Ascii;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import java.io.IOException;
import java.net.InetAddress;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import javax.websocket.CloseReason;
import javax.websocket.Session;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.triplea.http.client.web.socket.MessageEnvelope;
import org.triplea.http.client.web.socket.messages.envelopes.ServerErrorMessage;

/**
 * Extracts common code between websocket server methods. Each websocket endpoint is essentially
 * identical and they delegate their behavior to a 'messagingBus' which has listeners that will act
 * on messages received. In general there should not be many websocket endpoints and they are
 * grouped by the "types" of connections that are created to them (eg: players or games)
 */
@Slf4j
@UtilityClass
public class GenericWebSocket {
  public static final String BAN_CHECK_KEY = "session.ban.checker";

  @VisibleForTesting static final int MAX_BAD_MESSAGES = 2;

  private static final Gson GSON = new Gson();

  private static final Cache<InetAddress, AtomicInteger> badMessageCache =
      Caffeine.newBuilder().expireAfterWrite(Duration.ofSeconds(30)).build();

  static void onOpen(final Session session) {
    if (isSessionBanned(session)) {
      disconnectBannedSession(session);
    } else {
      ((WebSocketMessagingBus) session.getUserProperties().get(MESSAGING_BUS_KEY)).onOpen(session);
    }
  }

  @SuppressWarnings("unchecked")
  private static boolean isSessionBanned(final Session session) {
    final Object banCheckObject = session.getUserProperties().get(BAN_CHECK_KEY);

    return Optional.ofNullable(banCheckObject)
        .map(obj -> (Predicate<Session>) obj)
        .map(check -> check.test(session))
        .orElse(false);
  }

  private static void disconnectBannedSession(final Session session) {
    try {
      session.close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, "You have been banned"));
    } catch (final IOException e) {
      log.warn("IOException while closing new session of banned user", e);
    }
  }

  static void onMessage(final Session session, final String message) {
    onMessage(session, message, badMessageCache, new MessageSender());
  }

  @VisibleForTesting
  static void onMessage(
      final Session session,
      final String message,
      final Cache<InetAddress, AtomicInteger> badMessageCache,
      final MessageSender messageSender) {

    readJsonMessage(session, message, badMessageCache, messageSender)
        .ifPresent(
            envelope ->
                ((WebSocketMessagingBus) session.getUserProperties().get(MESSAGING_BUS_KEY))
                    .onMessage(session, envelope));
  }

  /**
   * Checks if session has sent too many bad messages, if so we ignore messages from that session
   * and return an empty. Otherwise we will convert the message JSON string to a {@code
   * MessageEnvelope} and return that. If the message is badly formatted, we'll send back an error
   * message response to the session, increment the bad message count and return an empty.
   */
  private static Optional<MessageEnvelope> readJsonMessage(
      final Session session,
      final String message,
      final Cache<InetAddress, AtomicInteger> badMessageCache,
      final MessageSender messageSender) {

    if (burnMessagesFromThisSession(session, badMessageCache)) {
      // Burn the message -> no-op
      // To conserve server resources, we do not even log the message.
      return Optional.empty();
    }

    try {
      return Optional.of(GSON.fromJson(message, MessageEnvelope.class));
    } catch (final JsonSyntaxException e) {
      final InetAddress inetAddress = InetExtractor.extract(session.getUserProperties());
      incrementBadMessageCount(session, badMessageCache);
      logBadMessage(inetAddress, message);
      respondWithServerError(messageSender, session);
      return Optional.empty();
    }
  }

  private static boolean burnMessagesFromThisSession(
      final Session session, final Cache<InetAddress, AtomicInteger> badMessageCache) {
    final InetAddress inetAddress = InetExtractor.extract(session.getUserProperties());

    final int badMessageCount =
        Optional.ofNullable(badMessageCache.getIfPresent(inetAddress))
            .map(AtomicInteger::get)
            .orElse(0);

    return badMessageCount > MAX_BAD_MESSAGES;
  }

  private static void incrementBadMessageCount(
      final Session session, final Cache<InetAddress, AtomicInteger> badMessageCache) {
    badMessageCache
        .asMap()
        .computeIfAbsent(
            InetExtractor.extract(session.getUserProperties()), inet -> new AtomicInteger(0))
        .incrementAndGet();
  }

  private static void logBadMessage(final InetAddress inetAddress, final String message) {
    log.warn(
        "Failed to decode JSON string from IP {}, into a MessageEnvelope: {}",
        inetAddress,
        Ascii.truncate(message, 500, "..."));
  }

  private static void respondWithServerError(
      final MessageSender messageSender, final Session session) {
    messageSender.accept(
        session,
        new ServerErrorMessage("Server is unable to process request, error reading message")
            .toEnvelope());
  }

  static void onClose(final Session session, final CloseReason closeReason) {
    ((WebSocketMessagingBus) session.getUserProperties().get(MESSAGING_BUS_KEY)).onClose(session);
  }

  static void onError(final Session session, final Throwable throwable) {
    ((WebSocketMessagingBus) session.getUserProperties().get(MESSAGING_BUS_KEY))
        .onError(session, throwable);
  }
}
