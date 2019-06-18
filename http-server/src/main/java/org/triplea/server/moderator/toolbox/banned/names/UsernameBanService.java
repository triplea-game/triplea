package org.triplea.server.moderator.toolbox.banned.names;

import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.triplea.http.client.moderator.toolbox.banned.name.UsernameBanData;
import org.triplea.lobby.server.db.dao.ModeratorAuditHistoryDao;
import org.triplea.lobby.server.db.dao.UsernameBanDao;

import lombok.AllArgsConstructor;
import lombok.Builder;

@AllArgsConstructor
@Builder
class UsernameBanService {
  @Nonnull
  private final UsernameBanDao bannedUserNamesDao;
  @Nonnull
  private final ModeratorAuditHistoryDao moderatorAuditHistoryDao;

  boolean removeUsernameBan(final int moderatorUserId, final String namneToUnBan) {
    if (bannedUserNamesDao.removeBannedUserName(namneToUnBan) > 0) {
      moderatorAuditHistoryDao.addAuditRecord(ModeratorAuditHistoryDao.AuditArgs.builder()
          .moderatorUserId(moderatorUserId)
          .actionName(ModeratorAuditHistoryDao.AuditAction.REMOVE_USERNAME_BAN)
          .actionTarget(namneToUnBan)
          .build());
      return true;
    }
    return false;
  }

  boolean addBannedUserName(final int moderatorUserId, final String nameToBan) {
    if (bannedUserNamesDao.addBannedUserName(nameToBan) == 1) {
      moderatorAuditHistoryDao.addAuditRecord(ModeratorAuditHistoryDao.AuditArgs.builder()
          .moderatorUserId(moderatorUserId)
          .actionName(ModeratorAuditHistoryDao.AuditAction.BAN_USERNAME)
          .actionTarget(nameToBan)
          .build());
      return true;
    }
    return false;
  }

  List<UsernameBanData> getBannedUserNames() {
    return bannedUserNamesDao.getBannedUserNames()
        .stream()
        .map(data -> UsernameBanData.builder()
            .bannedName(data.getUsername())
            .banDate(data.getDateCreated())
            .build())
        .collect(Collectors.toList());
  }
}
