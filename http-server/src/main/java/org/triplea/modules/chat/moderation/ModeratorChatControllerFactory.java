package org.triplea.modules.chat.moderation;

import lombok.experimental.UtilityClass;
import org.jdbi.v3.core.Jdbi;
import org.triplea.db.dao.ModeratorAuditHistoryDao;
import org.triplea.db.dao.api.key.ApiKeyDaoWrapper;
import org.triplea.db.dao.user.ban.UserBanDao;
import org.triplea.modules.chat.event.processing.Chatters;
import org.triplea.modules.moderation.remote.actions.RemoteActionsEventQueue;
import org.triplea.web.socket.MessageBroadcaster;
import org.triplea.web.socket.MessageSender;

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
