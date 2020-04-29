package org.triplea.modules.moderation.ban.name;

import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.jdbi.v3.core.Jdbi;
import org.triplea.db.dao.moderator.ModeratorAuditHistoryDao;
import org.triplea.db.dao.username.ban.UsernameBanDao;
import org.triplea.http.client.lobby.moderator.toolbox.banned.name.UsernameBanData;

@AllArgsConstructor
@Builder
class UsernameBanService {
  @Nonnull private final UsernameBanDao bannedUserNamesDao;
  @Nonnull private final ModeratorAuditHistoryDao moderatorAuditHistoryDao;

  public static UsernameBanService build(final Jdbi jdbi) {
    return UsernameBanService.builder()
        .bannedUserNamesDao(jdbi.onDemand(UsernameBanDao.class))
        .moderatorAuditHistoryDao(jdbi.onDemand(ModeratorAuditHistoryDao.class))
        .build();
  }

  boolean removeUsernameBan(final int moderatorUserId, final String nameToUnBan) {
    if (bannedUserNamesDao.removeBannedUserName(nameToUnBan) > 0) {
      moderatorAuditHistoryDao.addAuditRecord(
          ModeratorAuditHistoryDao.AuditArgs.builder()
              .moderatorUserId(moderatorUserId)
              .actionName(ModeratorAuditHistoryDao.AuditAction.REMOVE_USERNAME_BAN)
              .actionTarget(nameToUnBan)
              .build());
      return true;
    }
    return false;
  }

  boolean addBannedUserName(final int moderatorUserId, final String nameToBan) {
    if (bannedUserNamesDao.addBannedUserName(nameToBan) == 1) {
      moderatorAuditHistoryDao.addAuditRecord(
          ModeratorAuditHistoryDao.AuditArgs.builder()
              .moderatorUserId(moderatorUserId)
              .actionName(ModeratorAuditHistoryDao.AuditAction.BAN_USERNAME)
              .actionTarget(nameToBan)
              .build());
      return true;
    }
    return false;
  }

  List<UsernameBanData> getBannedUserNames() {
    return bannedUserNamesDao.getBannedUserNames().stream()
        .map(
            data ->
                UsernameBanData.builder()
                    .bannedName(data.getUsername())
                    .banDate(data.getDateCreated())
                    .build())
        .collect(Collectors.toList());
  }
}
