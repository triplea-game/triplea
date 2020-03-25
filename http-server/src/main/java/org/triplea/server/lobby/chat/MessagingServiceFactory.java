package org.triplea.server.lobby.chat;

import lombok.experimental.UtilityClass;
import org.jdbi.v3.core.Jdbi;
import org.triplea.lobby.server.db.dao.api.key.ApiKeyDaoWrapper;
import org.triplea.server.http.web.socket.MessageBroadcaster;
import org.triplea.server.http.web.socket.MessageSender;
import org.triplea.server.http.web.socket.SessionSet;
import org.triplea.server.lobby.chat.event.processing.ChatEventProcessor;
import org.triplea.server.lobby.chat.event.processing.Chatters;

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
