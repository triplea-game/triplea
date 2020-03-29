package org.triplea.modules.chat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.web.socket.WebSocketMessagingBus;

@ExtendWith(MockitoExtension.class)
class ChatMessagingServiceTest {
  @Mock private Chatters chatters;
  @Mock private WebSocketMessagingBus webSocketMessagingBus;

  @Test
  @DisplayName("Verify that we add listeners to messaging bus when configure is called")
  void configureAddsListeners() {
    ChatMessagingService.build(chatters).configure(webSocketMessagingBus);

    verify(webSocketMessagingBus, atLeastOnce()).addListener(any(), any());
  }
}
