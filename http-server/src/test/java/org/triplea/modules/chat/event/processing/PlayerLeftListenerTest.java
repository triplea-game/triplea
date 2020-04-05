package org.triplea.modules.chat.event.processing;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import javax.websocket.Session;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.domain.data.UserName;
import org.triplea.http.client.web.socket.messages.envelopes.chat.PlayerLeftMessage;
import org.triplea.modules.chat.Chatters;
import org.triplea.web.socket.WebSocketMessagingBus;

@ExtendWith(MockitoExtension.class)
class PlayerLeftListenerTest {
  @Mock private Chatters chatters;
  @InjectMocks private PlayerLeftListener playerLeftListener;

  @Mock private Session session;
  @Mock private WebSocketMessagingBus webSocketMessagingBus;

  private ArgumentCaptor<PlayerLeftMessage> messageCaptor =
      ArgumentCaptor.forClass(PlayerLeftMessage.class);

  @Test
  void noopIfChattersSessionDoesNotExist() {
    when(chatters.playerLeft(session)).thenReturn(Optional.empty());

    playerLeftListener.accept(webSocketMessagingBus, session);

    verify(webSocketMessagingBus, never()).broadcastMessage(any());
  }

  @Test
  @DisplayName("If a player is in the chatter session, then we do relay their message")
  void ifPlayerSessionDoesExistThenRelayTheirMessage() {
    when(chatters.playerLeft(session)).thenReturn(Optional.of(UserName.of("user-name")));

    playerLeftListener.accept(webSocketMessagingBus, session);

    verify(webSocketMessagingBus).broadcastMessage(messageCaptor.capture());
    assertThat(messageCaptor.getValue().getUserName(), is(UserName.of("user-name")));
  }
}
