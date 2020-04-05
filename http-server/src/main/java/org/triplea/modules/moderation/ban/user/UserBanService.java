package org.triplea.modules.moderation.ban.user;

import com.google.common.annotations.VisibleForTesting;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.jdbi.v3.core.Jdbi;
import org.triplea.db.dao.ModeratorAuditHistoryDao;
import org.triplea.db.dao.api.key.ApiKeyDaoWrapper;
import org.triplea.db.dao.api.key.GamePlayerLookup;
import org.triplea.db.dao.user.ban.UserBanDao;
import org.triplea.domain.data.PlayerChatId;
import org.triplea.domain.data.UserName;
import org.triplea.http.client.lobby.moderator.BanDurationFormatter;
import org.triplea.http.client.lobby.moderator.BanPlayerRequest;
import org.triplea.http.client.lobby.moderator.toolbox.banned.user.UserBanData;
import org.triplea.http.client.lobby.moderator.toolbox.banned.user.UserBanParams;
import org.triplea.http.client.web.socket.messages.envelopes.chat.ChatEventReceivedMessage;
import org.triplea.http.client.web.socket.messages.envelopes.remote.actions.PlayerBannedMessage;
import org.triplea.modules.chat.Chatters;
import org.triplea.web.socket.WebSocketMessagingBus;

/**
 * Service layer for managing user bans, get bans, add and remove. User bans are done by MAC and IP
 * address, they are removed by the 'public ban id' that is assigned when a ban is issued.
 */
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @VisibleForTesting)
public class UserBanService {

  private final ModeratorAuditHistoryDao moderatorAuditHistoryDao;
  private final UserBanDao userBanDao;
  private final Supplier<String> publicIdSupplier;
  private final Chatters chatters;
  private final ApiKeyDaoWrapper apiKeyDaoWrapper;
  private final WebSocketMessagingBus chatMessagingBus;
  private final WebSocketMessagingBus gameMessagingBus;

  @Builder
  public UserBanService(
      final Jdbi jdbi,
      final Chatters chatters,
      final WebSocketMessagingBus chatMessagingBus,
      final WebSocketMessagingBus gameMessagingBus) {
    moderatorAuditHistoryDao = jdbi.onDemand(ModeratorAuditHistoryDao.class);
    userBanDao = jdbi.onDemand(UserBanDao.class);
    publicIdSupplier = () -> UUID.randomUUID().toString();
    this.chatters = chatters;
    this.apiKeyDaoWrapper = ApiKeyDaoWrapper.build(jdbi);
    this.chatMessagingBus = chatMessagingBus;
    this.gameMessagingBus = gameMessagingBus;
  }

  List<UserBanData> getBannedUsers() {
    return userBanDao.lookupBans().stream()
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

  boolean removeUserBan(final int moderatorId, final String banId) {
    final String unbanName = userBanDao.lookupUsernameByBanId(banId).orElse(null);
    if (userBanDao.removeBan(banId) != 1) {
      return false;
    }
    if (unbanName == null) {
      throw new IllegalStateException(
          "Consistency error, unbanned "
              + banId
              + ", but "
              + "there was no matching name for that ban.");
    }

    moderatorAuditHistoryDao.addAuditRecord(
        ModeratorAuditHistoryDao.AuditArgs.builder()
            .actionName(ModeratorAuditHistoryDao.AuditAction.REMOVE_USER_BAN)
            .actionTarget(unbanName)
            .moderatorUserId(moderatorId)
            .build());
    return true;
  }

  void banUser(final int moderatorId, final BanPlayerRequest banPlayerRequest) {
    apiKeyDaoWrapper
        .lookupPlayerByChatId(PlayerChatId.of(banPlayerRequest.getPlayerChatId()))
        .map(
            gamePlayerLookup -> toUserBanParams(gamePlayerLookup, banPlayerRequest.getBanMinutes()))
        .ifPresent(banUserParams -> banUser(moderatorId, banUserParams));
  }

  void banUser(final int moderatorId, final UserBanParams userBanParams) {
    persistUserBanToDatabase(userBanParams);

    if (removePlayerFromChat(userBanParams)) {
      broadcastToChattersPlayerBannedMessage(userBanParams);
    }

    // notify game hosts of the ban
    gameMessagingBus.broadcastMessage(new PlayerBannedMessage(userBanParams.getIp()));

    recordBanInModeratorAuditLog(moderatorId, userBanParams);
  }

  private static UserBanParams toUserBanParams(
      final GamePlayerLookup gamePlayerLookup, final long banMinutes) {
    return UserBanParams.builder()
        .systemId(gamePlayerLookup.getSystemId().getValue())
        .username(gamePlayerLookup.getUserName().getValue())
        .ip(gamePlayerLookup.getIp())
        .minutesToBan(banMinutes)
        .build();
  }

  private void persistUserBanToDatabase(final UserBanParams userBanParams) {
    if (userBanDao.addBan(
            publicIdSupplier.get(),
            userBanParams.getUsername(),
            userBanParams.getSystemId(),
            userBanParams.getIp(),
            userBanParams.getMinutesToBan())
        != 1) {
      throw new IllegalStateException("Failed to insert ban record:" + userBanParams);
    }
  }

  private boolean removePlayerFromChat(final UserBanParams userBanParams) {
    return chatters.disconnectPlayerSessions(
        UserName.of(userBanParams.getUsername()),
        String.format(
            "You have been banned for %s for violating lobby rules",
            BanDurationFormatter.formatBanMinutes(userBanParams.getMinutesToBan())));
  }

  private void broadcastToChattersPlayerBannedMessage(final UserBanParams banUserParams) {
    chatMessagingBus.broadcastMessage(
        new ChatEventReceivedMessage(
            String.format(
                "%s violated lobby rules and was banned for %s",
                banUserParams.getUsername(),
                BanDurationFormatter.formatBanMinutes(banUserParams.getMinutesToBan()))));
  }

  private void recordBanInModeratorAuditLog(
      final int moderatorId, final UserBanParams userBanParams) {
    moderatorAuditHistoryDao.addAuditRecord(
        ModeratorAuditHistoryDao.AuditArgs.builder()
            .moderatorUserId(moderatorId)
            .actionName(ModeratorAuditHistoryDao.AuditAction.BAN_USER)
            .actionTarget(
                userBanParams.getUsername() + " " + userBanParams.getMinutesToBan() + " hours")
            .build());
  }
}
