package org.triplea.modules.chat.event.processing;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.domain.data.ChatParticipant;
import org.triplea.domain.data.PlayerChatId;
import org.triplea.domain.data.UserName;
import org.triplea.http.client.web.socket.messages.envelopes.chat.PlayerSlapReceivedMessage;
import org.triplea.http.client.web.socket.messages.envelopes.chat.PlayerSlapSentMessage;
import org.triplea.modules.chat.ChatterSession;
import org.triplea.modules.chat.Chatters;
import org.triplea.web.socket.WebSocketMessageContext;
import org.triplea.web.socket.WebSocketSession;

@ExtendWith(MockitoExtension.class)
class SlapListenerTest {
  @Mock private Chatters chatters;
  @InjectMocks private SlapListener slapListener;

  @Mock private WebSocketSession session;
  @Mock private WebSocketMessageContext<PlayerSlapSentMessage> messageContext;

  private final ArgumentCaptor<PlayerSlapReceivedMessage> messageCaptor =
      ArgumentCaptor.forClass(PlayerSlapReceivedMessage.class);

  @Test
  void noopIfChattersSessionDoesNotExist() {
    when(messageContext.getSenderSession()).thenReturn(session);
    when(chatters.lookupPlayerBySession(session)).thenReturn(Optional.empty());

    slapListener.accept(messageContext);

    verify(messageContext, never()).broadcastMessage(any());
  }

  @Test
  @DisplayName("If a player is in the chatter session, then we do relay their message")
  void ifPlayerSessionDoesExistThenRelayTheirMessage() {
    when(messageContext.getSenderSession()).thenReturn(session);
    when(messageContext.getMessage())
        .thenReturn(new PlayerSlapSentMessage(UserName.of("slapped-player")));
    givenChatterSession(
        session,
        ChatParticipant.builder()
            .playerChatId(PlayerChatId.newId().getValue())
            .userName("user-name")
            .build());

    slapListener.accept(messageContext);

    verify(messageContext).broadcastMessage(messageCaptor.capture());
    verifyMessageContents(messageCaptor.getValue());
  }

  private void givenChatterSession(
      final WebSocketSession session, final ChatParticipant chatParticipant) {
    when(chatters.lookupPlayerBySession(session))
        .thenReturn(
            Optional.of(
                ChatterSession.builder()
                    .session(session)
                    .chatParticipant(chatParticipant)
                    .apiKeyId(123)
                    .build()));
  }

  private static void verifyMessageContents(final PlayerSlapReceivedMessage chatReceivedMessage) {
    assertThat(chatReceivedMessage.getSlappedPlayer(), is(UserName.of("slapped-player")));
    assertThat(chatReceivedMessage.getSlappingPlayer(), is(UserName.of("user-name")));
  }
}
