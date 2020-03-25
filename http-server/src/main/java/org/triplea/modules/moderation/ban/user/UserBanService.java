package org.triplea.modules.moderation.ban.user;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.websocket.Session;
import lombok.Builder;
import org.jdbi.v3.core.Jdbi;
import org.triplea.db.dao.ModeratorAuditHistoryDao;
import org.triplea.db.dao.api.key.ApiKeyDaoWrapper;
import org.triplea.db.dao.api.key.GamePlayerLookup;
import org.triplea.db.dao.user.ban.UserBanDao;
import org.triplea.domain.data.PlayerChatId;
import org.triplea.domain.data.UserName;
import org.triplea.http.client.IpAddressParser;
import org.triplea.http.client.lobby.chat.messages.server.ChatServerEnvelopeFactory;
import org.triplea.http.client.lobby.moderator.BanDurationFormatter;
import org.triplea.http.client.lobby.moderator.BanPlayerRequest;
import org.triplea.http.client.lobby.moderator.toolbox.banned.user.UserBanData;
import org.triplea.http.client.lobby.moderator.toolbox.banned.user.UserBanParams;
import org.triplea.http.client.web.socket.messages.ServerMessageEnvelope;
import org.triplea.modules.chat.event.processing.Chatters;
import org.triplea.modules.moderation.remote.actions.RemoteActionsEventQueue;
import org.triplea.web.socket.MessageBroadcaster;

/**
 * Service layer for managing user bans, get bans, add and remove. User bans are done by MAC and IP
 * address, they are removed by the 'public ban id' that is assigned when a ban is issued.
 */
@Builder
public class UserBanService {

  @Nonnull private final ModeratorAuditHistoryDao moderatorAuditHistoryDao;
  @Nonnull private final UserBanDao userBanDao;
  @Nonnull private final Supplier<String> publicIdSupplier;
  @Nonnull private final Chatters chatters;
  @Nonnull private final RemoteActionsEventQueue remoteActionsEventQueue;
  @Nonnull private final ApiKeyDaoWrapper apiKeyDaoWrapper;
  @Nonnull private final BiConsumer<Collection<Session>, ServerMessageEnvelope> messageBroadcaster;

  public static UserBanService build(
      final Jdbi jdbi,
      final Chatters chatters,
      final RemoteActionsEventQueue remoteActionsEventQueue) {

    return UserBanService.builder()
        .moderatorAuditHistoryDao(jdbi.onDemand(ModeratorAuditHistoryDao.class))
        .userBanDao(jdbi.onDemand(UserBanDao.class))
        .publicIdSupplier(() -> UUID.randomUUID().toString())
        .chatters(chatters)
        .remoteActionsEventQueue(remoteActionsEventQueue)
        .apiKeyDaoWrapper(ApiKeyDaoWrapper.build(jdbi))
        .messageBroadcaster(MessageBroadcaster.build())
        .build();
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

  boolean banUser(final int moderatorId, final BanPlayerRequest banPlayerRequest) {
    return apiKeyDaoWrapper
        .lookupPlayerByChatId(PlayerChatId.of(banPlayerRequest.getPlayerChatId()))
        .map(
            gamePlayerLookup -> toUserBanParams(gamePlayerLookup, banPlayerRequest.getBanMinutes()))
        .map(banUserParams -> banUser(moderatorId, banUserParams))
        .orElse(false);
  }

  boolean banUser(final int moderatorId, final UserBanParams banUserParams) {
    if (userBanDao.addBan(
            publicIdSupplier.get(),
            banUserParams.getUsername(),
            banUserParams.getSystemId(),
            banUserParams.getIp(),
            banUserParams.getMinutesToBan())
        != 1) {
      return false;
    }

    if (chatters.hasPlayer(UserName.of(banUserParams.getUsername()))) {
      chatters.disconnectPlayerSessions(
          UserName.of(banUserParams.getUsername()),
          playerBannedMessage(banUserParams.getMinutesToBan()));

      messageBroadcaster.accept(
          chatters.fetchOpenSessions(),
          ChatServerEnvelopeFactory.newEventMessage(
              playerBannedNotification(
                  UserName.of(banUserParams.getUsername()), banUserParams.getMinutesToBan())));
    }

    remoteActionsEventQueue.addPlayerBannedEvent(IpAddressParser.fromString(banUserParams.getIp()));

    moderatorAuditHistoryDao.addAuditRecord(
        ModeratorAuditHistoryDao.AuditArgs.builder()
            .moderatorUserId(moderatorId)
            .actionName(ModeratorAuditHistoryDao.AuditAction.BAN_USER)
            .actionTarget(
                banUserParams.getUsername() + " " + banUserParams.getMinutesToBan() + " hours")
            .build());
    return true;
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

  private static String playerBannedMessage(final long banMinutes) {
    return String.format(
        "You have been banned for %s for violating lobby rules",
        BanDurationFormatter.formatBanMinutes(banMinutes));
  }

  private static String playerBannedNotification(
      final UserName bannedUserName, final long banMinutes) {
    return String.format(
        "%s violated lobby rules and was banned for %s",
        bannedUserName, BanDurationFormatter.formatBanMinutes(banMinutes));
  }
}
