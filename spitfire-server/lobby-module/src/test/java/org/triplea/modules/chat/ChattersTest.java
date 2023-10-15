package org.triplea.modules.chat;

import static com.github.npathai.hamcrestopt.OptionalMatchers.isEmpty;
import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresentAndIs;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.triplea.java.DateTimeUtil.utcInstantOf;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import javax.websocket.CloseReason;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.domain.data.ChatParticipant;
import org.triplea.domain.data.PlayerChatId;
import org.triplea.http.client.web.socket.MessageEnvelope;
import org.triplea.java.IpAddressParser;
import org.triplea.web.socket.MessageBroadcaster;
import org.triplea.web.socket.WebSocketSession;

@SuppressWarnings("InnerClassMayBeStatic")
@ExtendWith(MockitoExtension.class)
class ChattersTest {

  private static final ChatParticipant CHAT_PARTICIPANT =
      ChatParticipant.builder().userName("player-name").playerChatId("333").build();
  private static final ChatParticipant CHAT_PARTICIPANT_2 =
      ChatParticipant.builder().userName("player-name2").playerChatId("4444").build();

  private final Chatters chatters = new Chatters();

  @Mock private WebSocketSession session;
  @Mock private WebSocketSession session2;
  @Mock private MessageBroadcaster messageBroadcaster;

  @Test
  void chattersIsInitiallyEmpty() {
    assertThat(chatters.getChatters(), is(empty()));
  }

  @Nested
  class PlayerLookup {
    @Test
    void lookupPlayerBySessionEmptyCase() {
      when(session2.getId()).thenReturn("session2-id");
      chatters.getParticipants().put("session-id", buildChatterSession(session));

      assertThat(
          "Searching for wrong session, session2 DNE",
          chatters.lookupPlayerBySession(session2),
          isEmpty());
    }

    @Test
    void lookupPlayerBySession() {
      when(session.getId()).thenReturn("session-id");
      final ChatterSession chatterSession = buildChatterSession(session);
      chatters.getParticipants().put("session-id", buildChatterSession(session));

      assertThat(chatters.lookupPlayerBySession(session), isPresentAndIs(chatterSession));
    }

    @Test
    void lookupPlayerByChatIdEmptyCase() {
      chatters.getParticipants().put("session-id", buildChatterSession(session));

      assertThat(
          chatters.lookupPlayerByChatId(PlayerChatId.of("DNE")), //
          isEmpty());
    }

    @Test
    void lookupPlayerByChatId() {
      final ChatterSession chatterSession = buildChatterSession(session);
      chatters.getParticipants().put("session-id", buildChatterSession(session));

      assertThat(
          chatters.lookupPlayerByChatId(chatterSession.getChatParticipant().getPlayerChatId()),
          isPresentAndIs(chatterSession));
    }
  }

  @Nested
  class IsPlayerConnected {
    @Test
    void hasPlayerReturnsFalseWithNoChatters() {
      assertThat(chatters.isPlayerConnected(CHAT_PARTICIPANT.getUserName()), is(false));
      assertThat(chatters.isPlayerConnected(CHAT_PARTICIPANT_2.getUserName()), is(false));
    }

    @Test
    void hasPlayerPlayerReturnsTrueIfNameMatches() {
      when(session.getId()).thenReturn("session-id");
      chatters.connectPlayer(buildChatterSession(session));

      // one chatter is added
      assertThat(chatters.isPlayerConnected(CHAT_PARTICIPANT.getUserName()), is(true));
      assertThat(chatters.isPlayerConnected(CHAT_PARTICIPANT_2.getUserName()), is(false));
    }
  }

  @Nested
  class FetchAnyOpenSession {
    @Test
    void noSessions() {
      assertThat(chatters.fetchOpenSessions(), empty());
    }

    @Test
    void fetchSession() {
      when(session.isOpen()).thenReturn(true);
      when(session.getId()).thenReturn("9");

      chatters.connectPlayer(buildChatterSession(session));
      assertThat(chatters.fetchOpenSessions(), hasItems(session));
    }

    @Test
    void fetchOnlyOpenSessions() {
      when(session.isOpen()).thenReturn(true);
      when(session.getId()).thenReturn("30");
      when(session2.isOpen()).thenReturn(false);
      when(session2.getId()).thenReturn("31");

      chatters.connectPlayer(buildChatterSession(session));
      chatters.connectPlayer(buildChatterSession(session2));

      assertThat(chatters.fetchOpenSessions(), hasItems(session));
    }

    @Test
    void fetchMultipleOpenSession() {
      when(session.isOpen()).thenReturn(true);
      when(session.getId()).thenReturn("90");

      when(session2.isOpen()).thenReturn(true);
      when(session2.getId()).thenReturn("91");

      chatters.connectPlayer(buildChatterSession(session));
      chatters.connectPlayer(buildChatterSession(session2));

      assertThat(chatters.fetchOpenSessions(), hasItems(session, session2));
    }
  }

  private ChatterSession buildChatterSession(final WebSocketSession session) {
    return ChatterSession.builder()
        .session(session)
        .chatParticipant(CHAT_PARTICIPANT)
        .apiKeyId(123)
        .ip(IpAddressParser.fromString("1.1.1.1"))
        .build();
  }

  @Nested
  class DisconnectPlayerByName {
    @Test
    void noOpIfPlayerNotConnected() {
      final boolean result =
          chatters.disconnectPlayerByName(CHAT_PARTICIPANT.getUserName(), "disconnect message");
      assertThat(result, is(false));
    }

    @Test
    void singleSessionDisconnected() {
      when(session.getId()).thenReturn("100");
      chatters.connectPlayer(buildChatterSession(session));

      final boolean result =
          chatters.disconnectPlayerByName(CHAT_PARTICIPANT.getUserName(), "disconnect message");
      assertThat(result, is(true));

      verify(session).close(any(CloseReason.class));
    }

    @Test
    @DisplayName("Players can have multiple sessions, verify they are all closed")
    void allSameNamePlayersAreDisconnected() {
      when(session.getId()).thenReturn("1");
      when(session2.getId()).thenReturn("2");

      chatters.connectPlayer(buildChatterSession(session));
      chatters.connectPlayer(buildChatterSession(session2));

      final boolean result =
          chatters.disconnectPlayerByName(CHAT_PARTICIPANT.getUserName(), "disconnect message");
      assertThat(result, is(true));

      verify(session).close(any(CloseReason.class));
      verify(session2).close(any(CloseReason.class));
    }
  }

  @Nested
  class DisconnectPlayerByIp {
    @Test
    void noOpIfPlayerNotConnected() {
      final boolean result =
          chatters.disconnectIp(IpAddressParser.fromString("1.1.1.1"), "disconnect message");
      assertThat(result, is(false));
    }

    @Test
    void singleSessionDisconnected() {
      when(session.getId()).thenReturn("100");
      final ChatterSession chatterSession = buildChatterSession(session);
      chatters.connectPlayer(chatterSession);

      final boolean result = chatters.disconnectIp(chatterSession.getIp(), "disconnect message");
      assertThat(result, is(true));

      verify(session).close(any(CloseReason.class));
    }

    @Test
    @DisplayName("Players can have multiple sessions, verify they are all closed")
    void allSameIpsAreDisconnected() {
      when(session.getId()).thenReturn("1");
      when(session2.getId()).thenReturn("2");

      final ChatterSession session1 = buildChatterSession(session);
      chatters.connectPlayer(session1);
      chatters.connectPlayer(buildChatterSession(session2));

      final boolean result = chatters.disconnectIp(session1.getIp(), "disconnect message");
      assertThat(result, is(true));

      verify(session).close(any(CloseReason.class));
      verify(session2).close(any(CloseReason.class));
    }
  }

  @Nested
  class Muting {
    private final Instant now = utcInstantOf(2000, 1, 1, 12, 20);

    @Test
    @DisplayName("With no players connected, checking if a player is muted is trivially false")
    void playerIsNotConnected() {
      assertThat(
          chatters.getPlayerMuteExpiration(IpAddressParser.fromString("1.1.1.1")), isEmpty());
    }

    @Test
    @DisplayName("A player is connected, but not muted")
    void playerIsNotMuted() {
      when(session.getId()).thenReturn("session-id");
      final var chatterSession = buildChatterSession(session);
      chatters.connectPlayer(chatterSession);

      final Optional<Instant> result =
          chatters.getPlayerMuteExpiration(IpAddressParser.fromString("55.55.55.55"));

      assertThat(result, isEmpty());
    }

    @Test
    @DisplayName("A player is connected, was muted, but mute has expired")
    void playerMuteIsExpired() {
      when(session.getId()).thenReturn("session-id");
      final var chatterSession = buildChatterSession(session);
      chatters.connectPlayer(chatterSession);
      chatters.mutePlayer(
          chatterSession.getChatParticipant().getPlayerChatId(),
          20,
          Clock.fixed(now, ZoneOffset.UTC),
          messageBroadcaster);

      final Optional<Instant> result =
          chatters.getPlayerMuteExpiration(
              chatterSession.getIp(),
              Clock.fixed(now.plus(21, ChronoUnit.MINUTES), ZoneOffset.UTC));

      assertThat("Current time is *after* mute expiry => not muted", result, isEmpty());
    }

    @Test
    void playerIsMuted() {
      when(session.getId()).thenReturn("session-id");
      final var chatterSession = buildChatterSession(session);
      chatters.connectPlayer(chatterSession);
      chatters.mutePlayer(
          chatterSession.getChatParticipant().getPlayerChatId(),
          1,
          Clock.fixed(now, ZoneOffset.UTC),
          messageBroadcaster);

      final Optional<Instant> result =
          chatters.getPlayerMuteExpiration(
              chatterSession.getIp(), Clock.fixed(now, ZoneOffset.UTC));

      assertThat(
          "Current time is *before* mute expiry => muted",
          result,
          isPresentAndIs(now.plus(1, ChronoUnit.MINUTES)));
    }

    @Test
    @DisplayName("Check that an expired mute is removed")
    void expiredPlayerMutesAreExpunged() {
      when(session.getId()).thenReturn("session-id");
      final var chatterSession = buildChatterSession(session);
      chatters.connectPlayer(chatterSession);
      chatters.mutePlayer(
          chatterSession.getChatParticipant().getPlayerChatId(),
          20,
          Clock.fixed(now, ZoneOffset.UTC),
          messageBroadcaster);

      // current time is after the mute expiry, we expect the mute to be expunged
      chatters.getPlayerMuteExpiration(
          chatterSession.getIp(), Clock.fixed(now.plus(21, ChronoUnit.MINUTES), ZoneOffset.UTC));

      assertThat(
          "Querying for the mute again, this time with current time before the mute."
              + "Normally this would be a condition for a mute, but we expunged the mute."
              + "Given the mute is expunged, we expect an empty result.",
          chatters.getPlayerMuteExpiration(
              chatterSession.getIp(), Clock.fixed(now, ZoneOffset.UTC)),
          isEmpty());
    }

    @Test
    @DisplayName("Verify that we broadcast a 'player-was-muted' message to all players when muting")
    void playerMuteActionIsBroadcasted() {
      when(session.getId()).thenReturn("session-id");
      final var chatterSession = buildChatterSession(session);
      chatters.connectPlayer(chatterSession);

      chatters.mutePlayer(
          chatterSession.getChatParticipant().getPlayerChatId(),
          20,
          Clock.systemUTC(),
          messageBroadcaster);

      verify(messageBroadcaster).accept(any(), any(MessageEnvelope.class));
    }
  }
}
