package org.triplea.modules.chat.moderation;

import java.util.UUID;
import javax.annotation.Nonnull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.triplea.db.dao.ModeratorAuditHistoryDao;
import org.triplea.db.dao.ModeratorAuditHistoryDao.AuditAction;
import org.triplea.db.dao.api.key.GamePlayerLookup;
import org.triplea.db.dao.user.ban.UserBanDao;
import org.triplea.http.client.lobby.moderator.BanDurationFormatter;
import org.triplea.http.client.lobby.moderator.BanPlayerRequest;

@AllArgsConstructor
@Builder
class ModeratorActionPersistence {
  @Nonnull private final UserBanDao userBanDao;
  @Nonnull private final ModeratorAuditHistoryDao moderatorAuditHistoryDao;

  void recordBan(
      final int moderatorId,
      final GamePlayerLookup gamePlayerLookup,
      final BanPlayerRequest banPlayerRequest) {
    userBanDao.addBan(
        UUID.randomUUID().toString(),
        gamePlayerLookup.getUserName().getValue(),
        gamePlayerLookup.getSystemId().getValue(),
        gamePlayerLookup.getIp(),
        banPlayerRequest.getBanMinutes());
    moderatorAuditHistoryDao.addAuditRecord(
        ModeratorAuditHistoryDao.AuditArgs.builder()
            .moderatorUserId(moderatorId)
            .actionName(AuditAction.BAN_USER)
            .actionTarget(
                gamePlayerLookup.getUserName().getValue()
                    + " "
                    + BanDurationFormatter.formatBanMinutes(banPlayerRequest.getBanMinutes()))
            .build());
  }

  void recordPlayerDisconnect(final int moderatorId, final GamePlayerLookup gamePlayerLookup) {
    moderatorAuditHistoryDao.addAuditRecord(
        ModeratorAuditHistoryDao.AuditArgs.builder()
            .moderatorUserId(moderatorId)
            .actionName(AuditAction.DISCONNECT_USER)
            .actionTarget(gamePlayerLookup.getUserName().getValue())
            .build());
  }
}
