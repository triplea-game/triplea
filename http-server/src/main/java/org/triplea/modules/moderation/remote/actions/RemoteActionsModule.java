package org.triplea.modules.moderation.remote.actions;

import java.net.InetAddress;
import javax.annotation.Nonnull;
import lombok.Builder;
import org.jdbi.v3.core.Jdbi;
import org.triplea.db.dao.moderator.ModeratorAuditHistoryDao;
import org.triplea.db.dao.moderator.ModeratorAuditHistoryDao.AuditAction;
import org.triplea.db.dao.moderator.ModeratorAuditHistoryDao.AuditArgs;
import org.triplea.db.dao.user.ban.UserBanDao;
import org.triplea.http.client.web.socket.messages.envelopes.remote.actions.ShutdownServerMessage;
import org.triplea.web.socket.WebSocketMessagingBus;

@Builder
class RemoteActionsModule {
  @Nonnull private final UserBanDao userBanDao;
  @Nonnull private final ModeratorAuditHistoryDao auditHistoryDao;
  @Nonnull private final WebSocketMessagingBus gameMessagingBus;

  public static RemoteActionsModule build(
      final Jdbi jdbi, final WebSocketMessagingBus gameMessagingBus) {
    return RemoteActionsModule.builder()
        .auditHistoryDao(jdbi.onDemand(ModeratorAuditHistoryDao.class))
        .userBanDao(jdbi.onDemand(UserBanDao.class))
        .gameMessagingBus(gameMessagingBus)
        .build();
  }

  boolean isUserBanned(final InetAddress ip) {
    return userBanDao.isBannedByIp(ip.getHostAddress());
  }

  void addGameIdForShutdown(final int moderatorId, final String gameId) {
    auditHistoryDao.addAuditRecord(
        AuditArgs.builder()
            .actionName(AuditAction.REMOTE_SHUTDOWN)
            .actionTarget(gameId)
            .moderatorUserId(moderatorId)
            .build());

    gameMessagingBus.broadcastMessage(new ShutdownServerMessage(gameId));
  }
}
