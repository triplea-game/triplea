package org.triplea.modules.chat.event.processing;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.db.dao.chat.history.LobbyChatHistoryDao;
import org.triplea.domain.data.ChatParticipant;
import org.triplea.domain.data.PlayerChatId;
import org.triplea.domain.data.UserName;
import org.triplea.http.client.web.socket.messages.envelopes.chat.ChatEventReceivedMessage;
import org.triplea.http.client.web.socket.messages.envelopes.chat.ChatReceivedMessage;
import org.triplea.http.client.web.socket.messages.envelopes.chat.ChatSentMessage;
import org.triplea.java.IpAddressParser;
import org.triplea.modules.chat.ChatterSession;
import org.triplea.modules.chat.Chatters;
import org.triplea.web.socket.WebSocketMessageContext;
import org.triplea.web.socket.WebSocketSession;

@ExtendWith(MockitoExtension.class)
class ChatMessageListenerTest {

  @Mock private Chatters chatters;
  @Mock private LobbyChatHistoryDao lobbyChatHistoryDao;
  @InjectMocks private ChatMessageListener chatMessageListener;

  @Mock private WebSocketSession session;
  @Mock private WebSocketMessageContext<ChatSentMessage> messageContext;

  private final ArgumentCaptor<ChatReceivedMessage> messageCaptor =
      ArgumentCaptor.forClass(ChatReceivedMessage.class);

  private ChatterSession chatterSession;

  @BeforeEach
  void dataSetup() {
    chatterSession =
        ChatterSession.builder()
            .session(session)
            .chatParticipant(
                ChatParticipant.builder()
                    .playerChatId(PlayerChatId.newId().getValue())
                    .userName("user-name")
                    .build())
            .apiKeyId(123)
            .ip(IpAddressParser.fromString("3.3.3.3"))
            .build();
  }

  @Test
  @DisplayName("If a player is not in the chatter session, then we do not relay their message")
  void ifPlayerSessionDoesNotExistThenDoNotRelayTheirMessage() {
    when(messageContext.getSenderSession()).thenReturn(session);
    when(session.getRemoteAddress()).thenReturn(chatterSession.getIp());
    when(chatters.lookupPlayerBySession(session)).thenReturn(Optional.empty());

    chatMessageListener.accept(messageContext);

    verify(messageContext, never()).broadcastMessage(any());
    verify(lobbyChatHistoryDao, never()).recordMessage(any(), anyInt());
  }

  @Test
  @DisplayName("If a player is in the chatter session, then we do relay their message")
  void ifPlayerSessionDoesExistThenRelayTheirMessage() {
    when(messageContext.getSenderSession()).thenReturn(session);
    when(messageContext.getMessage()).thenReturn(new ChatSentMessage("message"));
    when(chatters.lookupPlayerBySession(session)).thenReturn(Optional.of(chatterSession));
    when(session.getRemoteAddress()).thenReturn(chatterSession.getIp());
    when(chatters.getPlayerMuteExpiration(chatterSession.getIp())).thenReturn(Optional.empty());

    chatMessageListener.accept(messageContext);

    verify(messageContext).broadcastMessage(messageCaptor.capture());
    final ChatReceivedMessage chatReceivedMessage = messageCaptor.getValue();
    assertThat(chatReceivedMessage.getMessage(), is("message"));
    assertThat(
        chatReceivedMessage.getSender(),
        is(UserName.of(chatterSession.getChatParticipant().getUserName().getValue())));
    verify(lobbyChatHistoryDao, timeout(1000)).recordMessage(chatReceivedMessage, 123);
  }

  @Test
  void mutedPlayerMessagesAreNotSent() {
    when(messageContext.getSenderSession()).thenReturn(session);
    when(chatters.lookupPlayerBySession(session)).thenReturn(Optional.of(chatterSession));
    when(session.getRemoteAddress()).thenReturn(chatterSession.getIp());
    when(chatters.getPlayerMuteExpiration(chatterSession.getIp()))
        .thenReturn(Optional.of(Instant.now().plusSeconds(60)));

    chatMessageListener.accept(messageContext);

    // should not broadcast a muted players chat message
    verify(messageContext, never()).broadcastMessage(any());
    // should send response to muted player that they are muted.
    verify(messageContext).sendResponse(any(ChatEventReceivedMessage.class));
  }
}
