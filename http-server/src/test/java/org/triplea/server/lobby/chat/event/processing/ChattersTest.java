package org.triplea.server.lobby.chat.event.processing;

import static com.github.npathai.hamcrestopt.OptionalMatchers.isEmpty;
import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresentAndIs;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.websocket.CloseReason;
import javax.websocket.Session;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.domain.data.PlayerChatId;
import org.triplea.domain.data.UserName;
import org.triplea.http.client.lobby.chat.ChatParticipant;
import org.triplea.server.lobby.chat.event.processing.Chatters.ChatterSession;

@SuppressWarnings("InnerClassMayBeStatic")
@ExtendWith(MockitoExtension.class)
class ChattersTest {

  private static final ChatParticipant CHAT_PARTICIPANT =
      ChatParticipant.builder().userName(UserName.of("player-name")).build();
  private static final ChatParticipant CHAT_PARTICIPANT_2 =
      ChatParticipant.builder().userName(UserName.of("player-name2")).build();
  private static final String ID = "id";

  private final Chatters chatters = new Chatters();

  @Mock private Session session;
  @Mock private Session session2;

  @Test
  void removeSession() {
    when(session.getId()).thenReturn(ID);
    chatters.put(session, CHAT_PARTICIPANT);

    assertThat(chatters.removeSession(session), isPresentAndIs(CHAT_PARTICIPANT.getUserName()));

    assertThat(
        "Second remove should be empty, session is already removed.",
        chatters.removeSession(session),
        isEmpty());
  }

  @Test
  void put() {
    when(session.getId()).thenReturn(ID);
    chatters.put(session, CHAT_PARTICIPANT);

    final Map<String, ChatterSession> chatterMap = chatters.getParticipants();
    assertThat(chatterMap.values(), hasSize(1));
    assertThat(chatterMap.get(ID).getChatParticipant(), is(CHAT_PARTICIPANT));
    assertThat(chatterMap.get(ID).getSession(), sameInstance(session));
  }

  @Test
  void getAllParticipants() {
    when(session.getId()).thenReturn(ID);
    chatters.put(session, CHAT_PARTICIPANT);

    final Collection<ChatParticipant> result = chatters.getAllParticipants();

    assertThat(result, hasSize(1));
    assertThat(result, hasItem(CHAT_PARTICIPANT));
  }

  @Test
  void hasPlayer() {
    // no chatters added yet
    assertThat(chatters.hasPlayer(CHAT_PARTICIPANT.getUserName()), is(false));
    assertThat(chatters.hasPlayer(CHAT_PARTICIPANT_2.getUserName()), is(false));

    when(session.getId()).thenReturn(ID);
    chatters.put(session, CHAT_PARTICIPANT);

    // one chatter is added
    assertThat(chatters.hasPlayer(CHAT_PARTICIPANT.getUserName()), is(true));
    assertThat(chatters.hasPlayer(CHAT_PARTICIPANT_2.getUserName()), is(false));

    when(session2.getId()).thenReturn("id2");
    chatters.put(session2, CHAT_PARTICIPANT_2);

    // two chatters added, both should exist
    assertThat(chatters.hasPlayer(CHAT_PARTICIPANT.getUserName()), is(true));
    assertThat(chatters.hasPlayer(CHAT_PARTICIPANT_2.getUserName()), is(true));
  }

  @Nested
  class FetchAnyOpenSession {
    @Test
    void noSessions() {
      assertThat(chatters.fetchOpenSessions(), empty());
    }

    @Test
    void fetchSession() {
      when(session.getId()).thenReturn(ID);
      chatters.put(session, CHAT_PARTICIPANT);
      when(session.isOpen()).thenReturn(true);
      when(session.getOpenSessions()).thenReturn(Set.of(session, session2));

      assertThat(chatters.fetchOpenSessions(), hasItems(session, session2));
    }

    @Test
    void fetchOnlyOpenSessions() {
      when(session.getId()).thenReturn(ID);
      chatters.put(session, CHAT_PARTICIPANT);
      when(session.isOpen()).thenReturn(false);

      assertThat(chatters.fetchOpenSessions(), empty());
    }
  }

  @Nested
  class DisconnectPlayerSessions {
    @Test
    void noOpIfPlayerNotConnected() {
      chatters.disconnectPlayerSessions(CHAT_PARTICIPANT.getUserName(), "disconnect message");
    }

    @Test
    void singleSessionDisconnected() throws Exception {
      when(session.getId()).thenReturn(ID);
      chatters.put(session, CHAT_PARTICIPANT);

      chatters.disconnectPlayerSessions(CHAT_PARTICIPANT.getUserName(), "disconnect message");

      verify(session).close(any(CloseReason.class));
    }

    @Test
    @DisplayName("Players can have multiple sessions, verify they are all closed")
    void allSameNamePlayersAreDisconnected() throws Exception {
      final ChatParticipant participant1 = givenChatParticipant(session);
      final ChatParticipant participant2 = givenChatParticipant(session2);
      assertThat(
          "verify test data assumption",
          participant1.getUserName(),
          is(participant2.getUserName()));

      chatters.disconnectPlayerSessions(participant1.getUserName(), "disconnect message");

      verify(session).close(any(CloseReason.class));
      verify(session2).close(any(CloseReason.class));
    }

    private ChatParticipant givenChatParticipant(final Session chatterSession) {
      final var chatParticipant =
          ChatParticipant.builder()
              .playerChatId(PlayerChatId.newId())
              .userName(UserName.of("player-name"))
              .build();

      when(chatterSession.getId()).thenReturn(UUID.randomUUID().toString());
      chatters.put(chatterSession, chatParticipant);
      return chatParticipant;
    }
  }
}
