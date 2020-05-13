package org.triplea.modules.moderation.disconnect.user;

import javax.annotation.Nonnull;
import lombok.Builder;
import org.jdbi.v3.core.Jdbi;
import org.triplea.db.dao.api.key.PlayerApiKeyDaoWrapper;
import org.triplea.db.dao.api.key.PlayerIdentifiersByApiKeyLookup;
import org.triplea.db.dao.moderator.ModeratorAuditHistoryDao;
import org.triplea.domain.data.PlayerChatId;
import org.triplea.http.client.web.socket.messages.envelopes.chat.ChatEventReceivedMessage;
import org.triplea.modules.chat.Chatters;
import org.triplea.web.socket.WebSocketMessagingBus;

@Builder
public class DisconnectUserAction {

  @Nonnull private final PlayerApiKeyDaoWrapper apiKeyDaoWrapper;
  @Nonnull private final Chatters chatters;
  @Nonnull private final WebSocketMessagingBus playerConnections;
  @Nonnull private final ModeratorAuditHistoryDao moderatorAuditHistoryDao;

  static DisconnectUserAction build(
      final Jdbi jdbi, final Chatters chatters, final WebSocketMessagingBus playerConnections) {
    return DisconnectUserAction.builder()
        .apiKeyDaoWrapper(PlayerApiKeyDaoWrapper.build(jdbi))
        .chatters(chatters)
        .playerConnections(playerConnections)
        .moderatorAuditHistoryDao(jdbi.onDemand(ModeratorAuditHistoryDao.class))
        .build();
  }

  /**
   * Does a simple disconnect of a given player from chat, records an audit log entry, and notifies
   * chatters of the disconnect.
   */
  boolean disconnectPlayer(final int moderatorId, final PlayerChatId playerChatId) {
    final PlayerIdentifiersByApiKeyLookup gamePlayerLookup =
        apiKeyDaoWrapper.lookupPlayerByChatId(playerChatId).orElse(null);
    if (gamePlayerLookup == null || !chatters.isPlayerConnected(gamePlayerLookup.getUserName())) {
      return false;
    }

    if (chatters.disconnectPlayerSessions(
        gamePlayerLookup.getUserName(), "Disconnected by moderator")) {
      playerConnections.broadcastMessage(
          new ChatEventReceivedMessage(
              gamePlayerLookup.getUserName() + " was disconnected by moderator"));
    }

    moderatorAuditHistoryDao.addAuditRecord(
        ModeratorAuditHistoryDao.AuditArgs.builder()
            .moderatorUserId(moderatorId)
            .actionName(ModeratorAuditHistoryDao.AuditAction.DISCONNECT_USER)
            .actionTarget(gamePlayerLookup.getUserName().toString())
            .build());
    return true;
  }
}
