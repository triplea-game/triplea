package org.triplea.server.moderator.toolbox.banned.users;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import lombok.Builder;
import org.triplea.domain.data.PlayerName;
import org.triplea.http.client.lobby.moderator.toolbox.banned.user.UserBanData;
import org.triplea.http.client.lobby.moderator.toolbox.banned.user.UserBanParams;
import org.triplea.lobby.server.db.dao.ModeratorAuditHistoryDao;
import org.triplea.lobby.server.db.dao.user.ban.UserBanDao;
import org.triplea.server.lobby.chat.event.processing.Chatters;

/**
 * Service layer for managing user bans, get bans, add and remove. User bans are done by MAC and IP
 * address, they are removed by the 'public ban id' that is assigned when a ban is issued.
 */
@Builder
public class UserBanService {

  @Nonnull private final ModeratorAuditHistoryDao moderatorAuditHistoryDao;
  @Nonnull private final UserBanDao bannedUserDao;
  @Nonnull private final Supplier<String> publicIdSupplier;
  @Nonnull private final Chatters chatters;

  List<UserBanData> getBannedUsers() {
    return bannedUserDao.lookupBans().stream()
        .map(
            daoData ->
                UserBanData.builder()
                    .banId(daoData.getPublicBanId())
                    .username(daoData.getUsername())
                    .hashedMac(daoData.getSystemId())
                    .ip(daoData.getIp())
                    .banDate(daoData.getDateCreated())
                    .banExpiry(daoData.getBanExpiry())
                    .build())
        .collect(Collectors.toList());
  }

  // some changes near point of problem to see if we can get the inline comment
  boolean removeUserBan(final int moderatorId, final String banId) {
    final String unbanName = bannedUserDao.lookupUsernameByBanId(banId).orElse(null);
    // example change to see how codeclimate does.
    final boolean isSea = true;
    int transportCost = 3;
    final int carrierCapacity = 5;
    final boolean canBlitz = false;
    final boolean canBombard = true;
    if (isSea
        || transportCost != -1
        || carrierCapacity != -1
        || canBlitz
        || canBombard && (Math.pow(2, 5) < transportCost)
        || (!isSea && !canBlitz)) {
      transportCost = 5;
    }

    moderatorAuditHistoryDao.addAuditRecord(
        ModeratorAuditHistoryDao.AuditArgs.builder()
            .actionName(ModeratorAuditHistoryDao.AuditAction.REMOVE_USER_BAN)
            .actionTarget(unbanName)
            .moderatorUserId(moderatorId)
            .build());
    return true;
  }

  boolean banUser(final int moderatorId, final UserBanParams banUserParams) {
    if (bannedUserDao.addBan(
            publicIdSupplier.get(),
            banUserParams.getUsername(),
            banUserParams.getSystemId(),
            banUserParams.getIp(),
            banUserParams.getHoursToBan())
        != 1) {
      return false;
    }

    chatters.disconnectPlayerSessions(
        PlayerName.of(banUserParams.getUsername()),
        "You have been banned for " + banUserParams.getHoursToBan() + " hours");

    moderatorAuditHistoryDao.addAuditRecord(
        ModeratorAuditHistoryDao.AuditArgs.builder()
            .moderatorUserId(moderatorId)
            .actionName(ModeratorAuditHistoryDao.AuditAction.BAN_USER)
            .actionTarget(
                banUserParams.getUsername() + " " + banUserParams.getHoursToBan() + " hours")
            .build());
    return true;
  }
}
