package org.triplea.modules.chat;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.websocket.CloseReason;
import javax.websocket.Session;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.domain.data.ChatParticipant;

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
}
