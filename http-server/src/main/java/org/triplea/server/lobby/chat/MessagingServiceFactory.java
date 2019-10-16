package org.triplea.server.lobby.chat;

import lombok.experimental.UtilityClass;
import org.jdbi.v3.core.Jdbi;
import org.triplea.lobby.server.db.dao.api.key.ApiKeyDaoWrapper;
import org.triplea.server.lobby.chat.event.processing.ChatEventProcessor;

@UtilityClass
public class MessagingServiceFactory {

  public MessagingService build(final Jdbi jdbi) {
    return MessagingService.builder()
        .apiKeyDaoWrapper(new ApiKeyDaoWrapper(jdbi))
        .chatEventProcessor(new ChatEventProcessor())
        .messageSender(new MessageSender())
        .messageBroadcaster(new MessageBroadcaster(new MessageSender()))
        .chatParticipantAdapter(new ChatParticipantAdapter())
        .build();
  }
}
