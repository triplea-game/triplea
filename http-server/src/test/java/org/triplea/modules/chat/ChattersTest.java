package org.triplea.modules.chat;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.Optional;
import javax.websocket.CloseReason;
import javax.websocket.Session;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.db.dao.api.key.ApiKeyDaoWrapper;
import org.triplea.db.dao.api.key.UserWithRoleRecord;
import org.triplea.db.data.UserRole;
import org.triplea.domain.data.ApiKey;
import org.triplea.domain.data.ChatParticipant;

@SuppressWarnings("InnerClassMayBeStatic")
@ExtendWith(MockitoExtension.class)
class ChattersTest {

  private static final ChatParticipant CHAT_PARTICIPANT =
      ChatParticipant.builder().userName("player-name").playerChatId("333").build();
  private static final ChatParticipant CHAT_PARTICIPANT_2 =
      ChatParticipant.builder().userName("player-name2").playerChatId("4444").build();
  private static final String ID = "id";

  private static final ApiKey API_KEY = ApiKey.of("api-key");
  private static final ApiKey API_KEY_2 = ApiKey.of("api-key-2");

  private static final UserWithRoleRecord USER_WITH_ROLE_RECORD =
      UserWithRoleRecord.builder()
          .role(UserRole.PLAYER)
          .playerChatId("chat-id")
          .userId(123)
          .username("username")
          .build();
  @Mock private ApiKeyDaoWrapper apiKeyDaoWrapper;
  @Mock private ChatParticipantAdapter chatParticipantAdapter;

  @InjectMocks private Chatters chatters;

  @Mock private Session session;
  @Mock private Session session2;

  @Test
  void chattersIsInitiallyEmpty() {
    assertThat(chatters.getChatters(), is(empty()));
  }

  @Test
  void chatterWithValidApiKeyCanConnect() {
    when(session.getId()).thenReturn(ID);
    when(apiKeyDaoWrapper.lookupByApiKey(API_KEY)).thenReturn(Optional.of(USER_WITH_ROLE_RECORD));
    when(chatParticipantAdapter.apply(USER_WITH_ROLE_RECORD)).thenReturn(CHAT_PARTICIPANT);
    chatters.connectPlayer(API_KEY, session);

    final Collection<ChatParticipant> result = chatters.getChatters();

    assertThat(result, hasSize(1));
  }

  @Test
  void chatterWithInValidApiKeyDoesNotConnect() {
    when(apiKeyDaoWrapper.lookupByApiKey(API_KEY)).thenReturn(Optional.empty());
    chatters.connectPlayer(API_KEY, session);

    final Collection<ChatParticipant> result = chatters.getChatters();

    assertThat(result, is(empty()));
  }

  @Nested
  class HasPlayer {
    @Test
    void hasPlayerReturnsFalseWithNoChatters() {
      assertThat(chatters.hasPlayer(CHAT_PARTICIPANT.getUserName()), is(false));
      assertThat(chatters.hasPlayer(CHAT_PARTICIPANT_2.getUserName()), is(false));
    }

    @Test
    void hasPlayerPlayerReturnsTrueIfNameMatches() {
      when(session.getId()).thenReturn(ID);
      when(apiKeyDaoWrapper.lookupByApiKey(API_KEY)).thenReturn(Optional.of(USER_WITH_ROLE_RECORD));
      when(chatParticipantAdapter.apply(USER_WITH_ROLE_RECORD)).thenReturn(CHAT_PARTICIPANT);

      chatters.connectPlayer(API_KEY, session);

      // one chatter is added
      assertThat(chatters.hasPlayer(CHAT_PARTICIPANT.getUserName()), is(true));
      assertThat(chatters.hasPlayer(CHAT_PARTICIPANT_2.getUserName()), is(false));
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
      givenChatterWithApiKey(API_KEY, CHAT_PARTICIPANT);
      when(session.isOpen()).thenReturn(true);
      when(session.getId()).thenReturn("9");

      chatters.connectPlayer(API_KEY, session);
      assertThat(chatters.fetchOpenSessions(), hasItems(session));
    }

    @Test
    void fetchOnlyOpenSessions() {
      givenChatterWithApiKey(API_KEY, CHAT_PARTICIPANT);
      givenChatterWithApiKey(API_KEY_2, CHAT_PARTICIPANT_2);
      when(session.isOpen()).thenReturn(true);
      when(session.getId()).thenReturn("30");
      when(session2.isOpen()).thenReturn(false);
      when(session2.getId()).thenReturn("31");

      chatters.connectPlayer(API_KEY, session);
      chatters.connectPlayer(API_KEY_2, session2);

      assertThat(chatters.fetchOpenSessions(), hasItems(session));
    }

    @Test
    void fetchMultipleOpenSession() {
      givenChatterWithApiKey(API_KEY, CHAT_PARTICIPANT);
      givenChatterWithApiKey(API_KEY_2, CHAT_PARTICIPANT_2);
      when(session.isOpen()).thenReturn(true);
      when(session.getId()).thenReturn("90");

      when(session2.isOpen()).thenReturn(true);
      when(session2.getId()).thenReturn("91");

      chatters.connectPlayer(API_KEY, session);
      chatters.connectPlayer(API_KEY_2, session2);

      assertThat(chatters.fetchOpenSessions(), hasItems(session, session2));
    }
  }

  @SuppressWarnings("SameParameterValue")
  private void givenChatterWithApiKey(final ApiKey apiKey, final ChatParticipant chatParticipant) {
    final var userWithRoleRecord =
        UserWithRoleRecord.builder()
            .userId(123)
            .playerChatId(chatParticipant.getPlayerChatId().getValue())
            .username(chatParticipant.getUserName().getValue())
            .build();
    when(apiKeyDaoWrapper.lookupByApiKey(apiKey)).thenReturn(Optional.of(userWithRoleRecord));
    when(chatParticipantAdapter.apply(userWithRoleRecord)).thenReturn(CHAT_PARTICIPANT);
  }

  @Nested
  class DisconnectPlayerSessions {
    @Test
    void noOpIfPlayerNotConnected() {
      chatters.disconnectPlayerSessions(CHAT_PARTICIPANT.getUserName(), "disconnect message");
    }

    @Test
    void singleSessionDisconnected() throws Exception {
      givenChatterWithApiKey(API_KEY, CHAT_PARTICIPANT);
      when(session.getId()).thenReturn("100");
      chatters.connectPlayer(API_KEY, session);

      chatters.disconnectPlayerSessions(CHAT_PARTICIPANT.getUserName(), "disconnect message");

      verify(session).close(any(CloseReason.class));
    }

    @Test
    @DisplayName("Players can have multiple sessions, verify they are all closed")
    void allSameNamePlayersAreDisconnected() throws Exception {
      givenChatterWithApiKey(API_KEY, CHAT_PARTICIPANT);
      givenChatterWithApiKey(API_KEY_2, CHAT_PARTICIPANT);

      when(session.getId()).thenReturn("1");
      when(session2.getId()).thenReturn("2");

      chatters.connectPlayer(API_KEY, session);
      chatters.connectPlayer(API_KEY_2, session2);

      chatters.disconnectPlayerSessions(CHAT_PARTICIPANT.getUserName(), "disconnect message");

      verify(session).close(any(CloseReason.class));
      verify(session2).close(any(CloseReason.class));
    }
  }
}
