package org.triplea.server.lobby.chat;

import lombok.experimental.UtilityClass;
import org.jdbi.v3.core.Jdbi;
import org.triplea.lobby.server.db.dao.api.key.LobbyApiKeyDaoWrapper;
import org.triplea.server.http.web.socket.MessageBroadcaster;
import org.triplea.server.http.web.socket.MessageSender;
import org.triplea.server.lobby.chat.event.processing.ChatEventProcessor;
import org.triplea.server.lobby.chat.event.processing.Chatters;

@UtilityClass
public class MessagingServiceFactory {

  public MessagingService build(final Jdbi jdbi, final Chatters chatters) {
    return MessagingService.builder()
        .apiKeyDaoWrapper(new LobbyApiKeyDaoWrapper(jdbi))
        .chatEventProcessor(new ChatEventProcessor(chatters))
        .messageSender(new MessageSender())
        .messageBroadcaster(new MessageBroadcaster(new MessageSender()))
        .chatParticipantAdapter(new ChatParticipantAdapter())
        .build();
  }
}
