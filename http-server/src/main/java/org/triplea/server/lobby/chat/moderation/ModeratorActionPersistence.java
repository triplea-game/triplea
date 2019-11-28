package org.triplea.server.lobby.chat.moderation;

import java.util.UUID;
import javax.annotation.Nonnull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.triplea.http.client.lobby.moderator.BanDurationFormatter;
import org.triplea.http.client.lobby.moderator.BanPlayerRequest;
import org.triplea.lobby.server.db.dao.ModeratorAuditHistoryDao;
import org.triplea.lobby.server.db.dao.ModeratorAuditHistoryDao.AuditAction;
import org.triplea.lobby.server.db.dao.api.key.PlayerIdLookup;
import org.triplea.lobby.server.db.dao.user.ban.UserBanDao;

@AllArgsConstructor
@Builder
class ModeratorActionPersistence {
  @Nonnull private final UserBanDao userBanDao;
  @Nonnull private final ModeratorAuditHistoryDao moderatorAuditHistoryDao;

  void recordBan(
      final int moderatorId,
      final PlayerIdLookup playerIdLookup,
      final BanPlayerRequest banPlayerRequest) {
    userBanDao.addBan(
        UUID.randomUUID().toString(),
        playerIdLookup.getPlayerName().getValue(),
        playerIdLookup.getSystemId().getValue(),
        playerIdLookup.getIp(),
        banPlayerRequest.getBanMinutes());
    moderatorAuditHistoryDao.addAuditRecord(
        ModeratorAuditHistoryDao.AuditArgs.builder()
            .moderatorUserId(moderatorId)
            .actionName(AuditAction.BAN_USER)
            .actionTarget(
                playerIdLookup.getPlayerName().getValue()
                    + " "
                    + BanDurationFormatter.formatBanMinutes(banPlayerRequest.getBanMinutes()))
            .build());
  }

  void recordPlayerDisconnect(final int moderatorId, final PlayerIdLookup playerIdLookup) {
    moderatorAuditHistoryDao.addAuditRecord(
        ModeratorAuditHistoryDao.AuditArgs.builder()
            .moderatorUserId(moderatorId)
            .actionName(AuditAction.DISCONNECT_USER)
            .actionTarget(playerIdLookup.getPlayerName().getValue())
            .build());
  }
}
