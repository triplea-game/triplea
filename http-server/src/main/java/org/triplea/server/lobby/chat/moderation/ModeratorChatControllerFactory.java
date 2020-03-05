package org.triplea.server.lobby.chat.moderation;

import lombok.experimental.UtilityClass;
import org.jdbi.v3.core.Jdbi;
import org.triplea.lobby.server.db.dao.ModeratorAuditHistoryDao;
import org.triplea.lobby.server.db.dao.api.key.ApiKeyDaoWrapper;
import org.triplea.lobby.server.db.dao.user.ban.UserBanDao;
import org.triplea.server.http.web.socket.MessageBroadcaster;
import org.triplea.server.http.web.socket.MessageSender;
import org.triplea.server.lobby.chat.event.processing.Chatters;
import org.triplea.server.remote.actions.RemoteActionsEventQueue;

@UtilityClass
public class ModeratorChatControllerFactory {

  public static ModeratorChatController buildController(
      final Jdbi jdbi,
      final Chatters chatters,
      final RemoteActionsEventQueue remoteActionsEventQueue) {
    return ModeratorChatController.builder()
        .moderatorChatService(
            ModeratorChatService.builder()
                .chatters(chatters)
                .lobbyApiKeyDaoWrapper(new ApiKeyDaoWrapper(jdbi))
                .messageBroadcaster(new MessageBroadcaster(new MessageSender()))
                .moderatorActionPersistence(
                    ModeratorActionPersistence.builder()
                        .moderatorAuditHistoryDao(jdbi.onDemand(ModeratorAuditHistoryDao.class))
                        .userBanDao(jdbi.onDemand(UserBanDao.class))
                        .build())
                .remoteActionsEventQueue(remoteActionsEventQueue)
                .build())
        .build();
  }
}
