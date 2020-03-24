package org.triplea.modules.chat;

import lombok.experimental.UtilityClass;
import org.jdbi.v3.core.Jdbi;
import org.triplea.db.dao.api.key.ApiKeyDaoWrapper;
import org.triplea.modules.chat.event.processing.ChatEventProcessor;
import org.triplea.modules.chat.event.processing.Chatters;
import org.triplea.web.socket.MessageBroadcaster;
import org.triplea.web.socket.MessageSender;
import org.triplea.web.socket.SessionSet;

@UtilityClass
public class MessagingServiceFactory {

  public ChatMessagingService build(
      final Jdbi jdbi, final SessionSet sessionSet, final Chatters chatters) {
    return ChatMessagingService.builder()
        .apiKeyDaoWrapper(new ApiKeyDaoWrapper(jdbi))
        .chatEventProcessor(new ChatEventProcessor(chatters, sessionSet))
        .messageSender(new MessageSender())
        .messageBroadcaster(new MessageBroadcaster(new MessageSender()))
        .chatParticipantAdapter(new ChatParticipantAdapter())
        .build();
  }
}
