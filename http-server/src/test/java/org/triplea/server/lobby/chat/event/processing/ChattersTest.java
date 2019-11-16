package org.triplea.server.lobby.chat.event.processing;

import static com.github.npathai.hamcrestopt.OptionalMatchers.isEmpty;
import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresentAndIs;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.Map;
import javax.websocket.Session;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.domain.data.PlayerName;
import org.triplea.http.client.lobby.chat.ChatParticipant;
import org.triplea.server.lobby.chat.event.processing.Chatters.ChatterSession;

@ExtendWith(MockitoExtension.class)
class ChattersTest {

  private static final ChatParticipant CHAT_PARTICIPANT =
      ChatParticipant.builder().playerName(PlayerName.of("player-name")).build();
  private static final ChatParticipant CHAT_PARTICIPANT_2 =
      ChatParticipant.builder().playerName(PlayerName.of("player-name2")).build();

  private static final String ID = "id";

  private final Chatters chatters = new Chatters();

  @Mock private Session session;
  @Mock private Session session2;

  @Test
  void removeSession() {
    when(session.getId()).thenReturn(ID);
    chatters.put(session, CHAT_PARTICIPANT);

    assertThat(chatters.removeSession(session), isPresentAndIs(CHAT_PARTICIPANT.getPlayerName()));

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
    assertThat(chatters.hasPlayer(CHAT_PARTICIPANT.getPlayerName()), is(false));
    assertThat(chatters.hasPlayer(CHAT_PARTICIPANT_2.getPlayerName()), is(false));

    when(session.getId()).thenReturn(ID);
    chatters.put(session, CHAT_PARTICIPANT);

    // one chatter is added
    assertThat(chatters.hasPlayer(CHAT_PARTICIPANT.getPlayerName()), is(true));
    assertThat(chatters.hasPlayer(CHAT_PARTICIPANT_2.getPlayerName()), is(false));

    when(session2.getId()).thenReturn("id2");
    chatters.put(session2, CHAT_PARTICIPANT_2);

    // two chatters added, both should exist
    assertThat(chatters.hasPlayer(CHAT_PARTICIPANT.getPlayerName()), is(true));
    assertThat(chatters.hasPlayer(CHAT_PARTICIPANT_2.getPlayerName()), is(true));
  }
}
