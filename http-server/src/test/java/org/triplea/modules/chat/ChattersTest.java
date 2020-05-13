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
import java.util.Optional;
import javax.websocket.CloseReason;
import javax.websocket.Session;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.domain.data.ChatParticipant;
import org.triplea.domain.data.PlayerChatId;

@SuppressWarnings("InnerClassMayBeStatic")
@ExtendWith(MockitoExtension.class)
class ChattersTest {

  private static final ChatParticipant CHAT_PARTICIPANT =
      ChatParticipant.builder().userName("player-name").playerChatId("333").build();
  private static final ChatParticipant CHAT_PARTICIPANT_2 =
      ChatParticipant.builder().userName("player-name2").playerChatId("4444").build();

  private Chatters chatters = new Chatters();

  @Mock private Session session;
  @Mock private Session session2;

  @Test
  void chattersIsInitiallyEmpty() {
    assertThat(chatters.getChatters(), is(empty()));
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

  private ChatterSession buildChatterSession(final Session session) {
    return ChatterSession.builder()
        .session(session)
        .chatParticipant(CHAT_PARTICIPANT)
        .apiKeyId(123)
        .build();
  }

  @Nested
  class DisconnectPlayerSessions {
    @Test
    void noOpIfPlayerNotConnected() {
      final boolean result =
          chatters.disconnectPlayerSessions(CHAT_PARTICIPANT.getUserName(), "disconnect message");
      assertThat(result, is(false));
    }

    @Test
    void singleSessionDisconnected() throws Exception {
      when(session.getId()).thenReturn("100");
      chatters.connectPlayer(buildChatterSession(session));

      final boolean result =
          chatters.disconnectPlayerSessions(CHAT_PARTICIPANT.getUserName(), "disconnect message");
      assertThat(result, is(true));

      verify(session).close(any(CloseReason.class));
    }

    @Test
    @DisplayName("Players can have multiple sessions, verify they are all closed")
    void allSameNamePlayersAreDisconnected() throws Exception {
      when(session.getId()).thenReturn("1");
      when(session2.getId()).thenReturn("2");

      chatters.connectPlayer(buildChatterSession(session));
      chatters.connectPlayer(buildChatterSession(session2));

      final boolean result =
          chatters.disconnectPlayerSessions(CHAT_PARTICIPANT.getUserName(), "disconnect message");
      assertThat(result, is(true));

      verify(session).close(any(CloseReason.class));
      verify(session2).close(any(CloseReason.class));
    }
  }

  @Nested
  class Muting {
    private final Instant muteExpiry = utcInstantOf(2000, 1, 1, 12, 20);

    @Test
    @DisplayName("With no players connected, checking if a player is muted is trivially false")
    void playerIsNotConnected() {
      assertThat(chatters.isPlayerMuted(PlayerChatId.of("any")), isEmpty());
    }

    @Test
    @DisplayName("A player is connected, but not muted")
    void playerIsNotMuted() {
      when(session.getId()).thenReturn("session-id");
      final var chatterSession = buildChatterSession(session);
      chatters.connectPlayer(chatterSession);

      assertThat(
          chatters.isPlayerMuted(chatterSession.getChatParticipant().getPlayerChatId()), isEmpty());
    }

    @Test
    @DisplayName("A player is connected, was muted, but mute has expired")
    void playerMuteIsExpired() {
      when(session.getId()).thenReturn("session-id");
      final var chatterSession = buildChatterSession(session);
      chatters.connectPlayer(chatterSession);

      chatters.mutePlayer(chatterSession.getChatParticipant().getPlayerChatId(), muteExpiry);

      final Optional<Instant> result =
          chatters.isPlayerMuted(
              chatterSession.getChatParticipant().getPlayerChatId(),
              Clock.fixed(muteExpiry.plusSeconds(10), ZoneOffset.UTC));

      assertThat("Current time is *after* mute expiry => not muted", result, isEmpty());
    }

    @Test
    void playerIsMuted() {
      when(session.getId()).thenReturn("session-id");
      final var chatterSession = buildChatterSession(session);
      chatters.connectPlayer(chatterSession);

      chatters.mutePlayer(chatterSession.getChatParticipant().getPlayerChatId(), muteExpiry);

      final Optional<Instant> result =
          chatters.isPlayerMuted(
              chatterSession.getChatParticipant().getPlayerChatId(),
              Clock.fixed(muteExpiry.minusSeconds(10), ZoneOffset.UTC));

      assertThat(
          "Current time is *before* mute expiry => muted", result, isPresentAndIs(muteExpiry));
    }

    @Test
    @DisplayName("Check that an expired mute is removed")
    void expiredPlayerMutesAreExpunged() {
      when(session.getId()).thenReturn("session-id");
      final var chatterSession = buildChatterSession(session);
      chatters.connectPlayer(chatterSession);

      chatters.mutePlayer(chatterSession.getChatParticipant().getPlayerChatId(), muteExpiry);

      // current time is after the mute expiry, we expect the mute to be expunged
      chatters.isPlayerMuted(
          chatterSession.getChatParticipant().getPlayerChatId(),
          Clock.fixed(muteExpiry.plusSeconds(10), ZoneOffset.UTC));

      assertThat(
          "Querying for the mute again, this time with current time before the mute."
              + "Given the mute is expunged, we would expect an empty result.",
          chatters.isPlayerMuted(
              chatterSession.getChatParticipant().getPlayerChatId(),
              Clock.fixed(muteExpiry.plusSeconds(10), ZoneOffset.UTC)),
          isEmpty());
    }
  }
}
