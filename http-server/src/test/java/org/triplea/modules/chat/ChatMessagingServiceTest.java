package org.triplea.modules.chat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.db.dao.chat.history.LobbyChatHistoryDao;
import org.triplea.web.socket.WebSocketMessagingBus;

@ExtendWith(MockitoExtension.class)
class ChatMessagingServiceTest {
  @Mock private Chatters chatters;
  @Mock private Jdbi jdbi;
  @Mock private LobbyChatHistoryDao lobbyChatHistoryDao;
  @Mock private WebSocketMessagingBus webSocketMessagingBus;

  @Test
  @DisplayName("Verify that we add listeners to messaging bus when configure is called")
  void configureAddsListeners() {
    when(jdbi.onDemand(LobbyChatHistoryDao.class)).thenReturn(lobbyChatHistoryDao);

    ChatMessagingService.build(chatters, jdbi).configure(webSocketMessagingBus);

    verify(webSocketMessagingBus, atLeastOnce()).addListener(any(), any());
  }
}
