package org.triplea.modules.moderation.remote.actions;

import java.net.InetAddress;
import javax.annotation.Nonnull;
import lombok.Builder;
import org.jdbi.v3.core.Jdbi;
import org.triplea.db.dao.ModeratorAuditHistoryDao;
import org.triplea.db.dao.ModeratorAuditHistoryDao.AuditAction;
import org.triplea.db.dao.ModeratorAuditHistoryDao.AuditArgs;
import org.triplea.db.dao.user.ban.UserBanDao;

@Builder
class RemoteActionsModule {
  @Nonnull private final UserBanDao userBanDao;
  @Nonnull private final ModeratorAuditHistoryDao auditHistoryDao;
  @Nonnull private final RemoteActionsEventQueue remoteActionsEventQueue;

  public static RemoteActionsModule build(
      final Jdbi jdbi, final RemoteActionsEventQueue remoteActionsEventQueue) {
    return RemoteActionsModule.builder()
        .auditHistoryDao(jdbi.onDemand(ModeratorAuditHistoryDao.class))
        .userBanDao(jdbi.onDemand(UserBanDao.class))
        .remoteActionsEventQueue(remoteActionsEventQueue)
        .build();
  }

  boolean isUserBanned(final InetAddress ip) {
    return userBanDao.isBannedByIp(ip.getHostAddress());
  }

  void addIpForShutdown(final int moderatorId, final InetAddress ipToShutdown) {
    auditHistoryDao.addAuditRecord(
        AuditArgs.builder()
            .actionName(AuditAction.REMOTE_SHUTDOWN)
            .actionTarget(ipToShutdown.getHostAddress())
            .moderatorUserId(moderatorId)
            .build());
    remoteActionsEventQueue.addShutdownRequestEvent(ipToShutdown);
  }
}
