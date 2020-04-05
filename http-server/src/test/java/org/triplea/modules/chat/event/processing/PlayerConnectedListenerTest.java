package org.triplea.modules.chat.event.processing;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import javax.websocket.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.domain.data.ApiKey;
import org.triplea.domain.data.ChatParticipant;
import org.triplea.http.client.web.socket.messages.envelopes.chat.ChatterListingMessage;
import org.triplea.http.client.web.socket.messages.envelopes.chat.ConnectToChatMessage;
import org.triplea.http.client.web.socket.messages.envelopes.chat.PlayerJoinedMessage;
import org.triplea.modules.chat.Chatters;
import org.triplea.web.socket.WebSocketMessageContext;

@ExtendWith(MockitoExtension.class)
class PlayerConnectedListenerTest {

  private static final ChatParticipant CHAT_PARTICIPANT =
      ChatParticipant.builder()
          .userName("user-name")
          .isModerator(true)
          .status("status")
          .playerChatId("123")
          .build();

  @Mock private Chatters chatters;
  private PlayerConnectedListener playerConnectedListener;

  @Mock private Session session;
  @Mock private WebSocketMessageContext<ConnectToChatMessage> context;

  private ArgumentCaptor<ChatterListingMessage> responseCaptor =
      ArgumentCaptor.forClass(ChatterListingMessage.class);
  private ArgumentCaptor<PlayerJoinedMessage> broadcastCaptor =
      ArgumentCaptor.forClass(PlayerJoinedMessage.class);

  @BeforeEach
  void setup() {
    playerConnectedListener = new PlayerConnectedListener(chatters);
  }

  @Test
  void noOpIfChattersDoesNotAllowPlayerToConnect() {
    when(context.getMessage()).thenReturn(new ConnectToChatMessage(ApiKey.of("api-key")));
    when(context.getSenderSession()).thenReturn(session);
    when(chatters.connectPlayer(any(), any())).thenReturn(Optional.empty());

    playerConnectedListener.accept(context);

    verify(context, never()).sendResponse(any());
    verify(context, never()).broadcastMessage(any());
  }

  @Test
  void playerConnects() {
    when(context.getMessage()).thenReturn(new ConnectToChatMessage(ApiKey.of("api-key")));
    when(context.getSenderSession()).thenReturn(session);
    when(chatters.connectPlayer(any(), any())).thenReturn(Optional.of(CHAT_PARTICIPANT));
    when(chatters.getChatters()).thenReturn(List.of());

    playerConnectedListener.accept(context);

    verify(context).sendResponse(responseCaptor.capture());
    assertThat(responseCaptor.getValue().getChatters(), is(List.of()));

    verify(context).broadcastMessage(broadcastCaptor.capture());
    assertThat(broadcastCaptor.getValue().getChatParticipant(), is(CHAT_PARTICIPANT));
  }
}
