package org.triplea.server.lobby.chat.moderation;

import lombok.experimental.UtilityClass;
import org.jdbi.v3.core.Jdbi;
import org.triplea.lobby.server.db.dao.ModeratorAuditHistoryDao;
import org.triplea.lobby.server.db.dao.api.key.LobbyApiKeyDaoWrapper;
import org.triplea.lobby.server.db.dao.user.ban.UserBanDao;
import org.triplea.server.lobby.chat.MessageBroadcaster;
import org.triplea.server.lobby.chat.MessageSender;
import org.triplea.server.lobby.chat.event.processing.Chatters;

@UtilityClass
public class ModeratorChatControllerFactory {

  public static ModeratorChatController buildController(final Jdbi jdbi, final Chatters chatters) {
    return ModeratorChatController.builder()
        .moderatorChatService(
            ModeratorChatService.builder()
                .chatters(chatters)
                .lobbyApiKeyDaoWrapper(new LobbyApiKeyDaoWrapper(jdbi))
                .messageBroadcaster(new MessageBroadcaster(new MessageSender()))
                .moderatorActionPersistence(
                    ModeratorActionPersistence.builder()
                        .moderatorAuditHistoryDao(jdbi.onDemand(ModeratorAuditHistoryDao.class))
                        .userBanDao(jdbi.onDemand(UserBanDao.class))
                        .build())
                .build())
        .build();
  }
}
