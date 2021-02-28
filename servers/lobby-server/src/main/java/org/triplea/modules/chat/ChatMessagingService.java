package org.triplea.modules.chat;

import com.google.common.base.Preconditions;
import lombok.Builder;
import org.jdbi.v3.core.Jdbi;
import org.triplea.http.client.web.socket.messages.envelopes.chat.ChatSentMessage;
import org.triplea.http.client.web.socket.messages.envelopes.chat.ConnectToChatMessage;
import org.triplea.http.client.web.socket.messages.envelopes.chat.PlayerSlapSentMessage;
import org.triplea.http.client.web.socket.messages.envelopes.chat.PlayerStatusUpdateSentMessage;
import org.triplea.modules.chat.event.processing.ChatMessageListener;
import org.triplea.modules.chat.event.processing.PlayerConnectedListener;
import org.triplea.modules.chat.event.processing.PlayerLeftListener;
import org.triplea.modules.chat.event.processing.SlapListener;
import org.triplea.modules.chat.event.processing.StatusUpdateListener;
import org.triplea.web.socket.WebSocketMessagingBus;

@Builder
public class ChatMessagingService {
  private final PlayerConnectedListener playerConnectedListener;
  private final ChatMessageListener chatMessageListener;
  private final StatusUpdateListener statusUpdateListener;
  private final SlapListener slapListener;
  private final PlayerLeftListener playerLeftListener;

  public static ChatMessagingService build(final Chatters chatters, final Jdbi jdbi) {
    Preconditions.checkNotNull(chatters);
    return ChatMessagingService.builder()
        .playerConnectedListener(PlayerConnectedListener.build(chatters, jdbi))
        .chatMessageListener(ChatMessageListener.build(chatters, jdbi))
        .statusUpdateListener(new StatusUpdateListener(chatters))
        .slapListener(new SlapListener(chatters))
        .playerLeftListener(new PlayerLeftListener(chatters))
        .build();
  }

  public void configure(final WebSocketMessagingBus playerConnectionMessagingBus) {
    playerConnectionMessagingBus.addMessageListener(
        ConnectToChatMessage.TYPE, playerConnectedListener);
    playerConnectionMessagingBus.addMessageListener(ChatSentMessage.TYPE, chatMessageListener);
    playerConnectionMessagingBus.addMessageListener(PlayerSlapSentMessage.TYPE, slapListener);
    playerConnectionMessagingBus.addMessageListener(
        PlayerStatusUpdateSentMessage.TYPE, statusUpdateListener);
    playerConnectionMessagingBus.addSessionDisconnectListener(playerLeftListener);
  }
}
