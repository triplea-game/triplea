package org.triplea.web.socket;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.gson.Gson;
import java.net.InetAddress;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.http.client.IpAddressParser;
import org.triplea.http.client.web.socket.MessageEnvelope;
import org.triplea.http.client.web.socket.messages.envelopes.ServerErrorMessage;
import org.triplea.http.client.web.socket.messages.envelopes.chat.PlayerStatusUpdateSentMessage;

@SuppressWarnings({"SameParameterValue", "InnerClassMayBeStatic"})
@ExtendWith(MockitoExtension.class)
class GenericWebSocketTest {

  @Mock private WebSocketSession session;

  @Mock private MessageSender messageSender;

  @Mock private WebSocketMessagingBus webSocketMessagingBus;

  @Mock private Predicate<InetAddress> banCheck;

  @InjectMocks private GenericWebSocket genericWebSocket;

  private final Cache<InetAddress, AtomicInteger> cache =
      Caffeine.newBuilder().expireAfterWrite(Duration.ofSeconds(60)).build();

  private final ArgumentCaptor<MessageEnvelope> messageCaptor =
      ArgumentCaptor.forClass(MessageEnvelope.class);

  @Test
  void verifyBannedSessionsAreDisconnected() {
    givenIpInSession("1.1.1.1");
    when(banCheck.test(IpAddressParser.fromString("1.1.1.1"))).thenReturn(true);

    genericWebSocket.onOpen(session);

    verify(webSocketMessagingBus, never()).onOpen(any());
    verify(session).close(any());
  }

  @Test
  void verifyNotBannedSessionsAreSentToMessagingBux() {
    givenOnOpenSessionWithIp("1.1.1.1");
    when(banCheck.test(IpAddressParser.fromString("1.1.1.1"))).thenReturn(false);

    genericWebSocket.onOpen(session);

    verify(webSocketMessagingBus).onOpen(session);
    verify(session, never()).close(any());
  }

  void givenOnOpenSessionWithIp(final String ip) {
    when(session.getRemoteAddress()).thenReturn(IpAddressParser.fromString(ip));
  }

  @Test
  @DisplayName(
      "Verify server responds to a bad message with error and increments bad message tracking")
  void invalidMessageButNotBurned() {
    givenIpInSession("1.1.1.1");

    genericWebSocket.onMessage(session, "message", cache);

    verify(messageSender).accept(eq(session), messageCaptor.capture());
    assertThat(
        "Server should responsd back with an error message, while under the bad message burn limit",
        messageCaptor.getValue().getMessageTypeId(),
        is(ServerErrorMessage.TYPE.getMessageTypeId()));
    assertThat(
        "Verify cache has been populated and incremented",
        cache.getIfPresent(IpAddressParser.fromString("1.1.1.1")).get(),
        is(1));
  }

  @Test
  void verifyBadMessageCacheIsIncremented() {
    givenIpInSession("1.1.1.1");
    givenIpHasBadMessageCount("1.1.1.1", 1);

    genericWebSocket.onMessage(session, "message", cache);

    assertThat(
        "Verify cache has been incremented",
        cache.getIfPresent(IpAddressParser.fromString("1.1.1.1")).get(),
        is(2));
  }

  private void givenIpInSession(final String ip) {
    when(session.getRemoteAddress()).thenReturn(IpAddressParser.fromString(ip));
  }

  @DisplayName("If a session has hit max bad messages, we ignore all messages from that session")
  @ParameterizedTest
  @MethodSource
  void atMaxBadMessagesWillBurnMessages(final String message) {
    givenIpInSession("1.1.1.1");
    givenIpHasBadMessageCount("1.1.1.1", GenericWebSocket.MAX_BAD_MESSAGES + 1);

    genericWebSocket.onMessage(session, message, cache);

    verify(webSocketMessagingBus, never()).onMessage(any(), any());
  }

  @SuppressWarnings("unused")
  private static List<String> atMaxBadMessagesWillBurnMessages() {
    return List.of(
        "invalid json message",
        new Gson().toJson(new PlayerStatusUpdateSentMessage("valid json").toEnvelope()));
  }

  private void givenIpHasBadMessageCount(final String ip, final int count) {
    cache.put(IpAddressParser.fromString(ip), new AtomicInteger(count));
  }

  @Test
  @DisplayName("Happy case - Valid messages when under bad message limit are processed")
  void validMessageUnderLimit() {
    givenIpInSession("1.1.1.1");
    givenIpHasBadMessageCount("1.1.1.1", 1);
    //noinspection ConstantConditions
    assertThat("Verify test assumptions", 1 < GenericWebSocket.MAX_BAD_MESSAGES, is(true));

    final var messageEnvelope = new PlayerStatusUpdateSentMessage("status").toEnvelope();

    genericWebSocket.onMessage(session, new Gson().toJson(messageEnvelope));

    verify(webSocketMessagingBus).onMessage(session, messageEnvelope);
  }
}
