package org.triplea.modules.moderation.disconnect.user;

import javax.annotation.Nonnull;
import lombok.Builder;
import org.jdbi.v3.core.Jdbi;
import org.triplea.db.dao.ModeratorAuditHistoryDao;
import org.triplea.db.dao.api.key.ApiKeyDaoWrapper;
import org.triplea.db.dao.api.key.GamePlayerLookup;
import org.triplea.domain.data.PlayerChatId;
import org.triplea.http.client.lobby.chat.messages.server.ChatServerEnvelopeFactory;
import org.triplea.modules.chat.event.processing.Chatters;
import org.triplea.web.socket.MessageBroadcaster;

@Builder
public class DisconnectUserAction {

  @Nonnull private final ApiKeyDaoWrapper apiKeyDaoWrapper;
  @Nonnull private final Chatters chatters;
  @Nonnull private final MessageBroadcaster messageBroadcaster;
  @Nonnull private final ModeratorAuditHistoryDao moderatorAuditHistoryDao;

  static DisconnectUserAction build(final Jdbi jdbi, final Chatters chatters) {
    return DisconnectUserAction.builder()
        .apiKeyDaoWrapper(ApiKeyDaoWrapper.build(jdbi))
        .chatters(chatters)
        .messageBroadcaster(MessageBroadcaster.build())
        .moderatorAuditHistoryDao(jdbi.onDemand(ModeratorAuditHistoryDao.class))
        .build();
  }

  /**
   * Does a simple disconnect of a given player from chat, records an audit log entry, and notifies
   * chatters of the disconnect.
   */
  boolean disconnectPlayer(final int moderatorId, final PlayerChatId playerChatId) {
    final GamePlayerLookup gamePlayerLookup =
        apiKeyDaoWrapper.lookupPlayerByChatId(playerChatId).orElse(null);
    if (gamePlayerLookup == null || !chatters.hasPlayer(gamePlayerLookup.getUserName())) {
      return false;
    }

    chatters.disconnectPlayerSessions(gamePlayerLookup.getUserName(), "Disconnected by moderator");
    messageBroadcaster.accept(
        chatters.fetchOpenSessions(),
        ChatServerEnvelopeFactory.newEventMessage(
            gamePlayerLookup.getUserName() + " was disconnected by moderator"));

    moderatorAuditHistoryDao.addAuditRecord(
        ModeratorAuditHistoryDao.AuditArgs.builder()
            .moderatorUserId(moderatorId)
            .actionName(ModeratorAuditHistoryDao.AuditAction.DISCONNECT_USER)
            .actionTarget(gamePlayerLookup.getUserName().toString())
            .build());
    return true;
  }
}
