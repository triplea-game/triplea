package org.triplea.modules.player.info;

import java.util.Collection;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import lombok.AllArgsConstructor;
import org.jdbi.v3.core.Jdbi;
import org.triplea.db.dao.api.key.PlayerApiKeyDaoWrapper;
import org.triplea.db.dao.api.key.PlayerIdentifiersByApiKeyLookup;
import org.triplea.db.dao.moderator.player.info.PlayerAliasRecord;
import org.triplea.db.dao.moderator.player.info.PlayerBanRecord;
import org.triplea.db.dao.moderator.player.info.PlayerInfoForModeratorDao;
import org.triplea.db.dao.user.history.PlayerHistoryDao;
import org.triplea.db.dao.user.history.PlayerHistoryRecord;
import org.triplea.domain.data.PlayerChatId;
import org.triplea.domain.data.SystemId;
import org.triplea.http.client.lobby.moderator.PlayerSummary;
import org.triplea.http.client.lobby.moderator.PlayerSummary.Alias;
import org.triplea.http.client.lobby.moderator.PlayerSummary.BanInformation;
import org.triplea.modules.chat.Chatters;
import org.triplea.modules.game.listing.GameListing;

@AllArgsConstructor
public class FetchPlayerInfoModule {
  private final PlayerApiKeyDaoWrapper apiKeyDaoWrapper;
  private final PlayerInfoForModeratorDao playerInfoForModeratorDao;
  private final PlayerHistoryDao playerHistoryDao;
  private final Chatters chatters;
  private final GameListing gameListing;

  public static FetchPlayerInfoModule build(
      final Jdbi jdbi, final Chatters chatters, final GameListing gameListing) {
    return new FetchPlayerInfoModule(
        PlayerApiKeyDaoWrapper.build(jdbi),
        jdbi.onDemand(PlayerInfoForModeratorDao.class),
        jdbi.onDemand(PlayerHistoryDao.class),
        chatters,
        gameListing);
  }

  public PlayerSummary fetchPlayerInfoAsModerator(final PlayerChatId playerChatId) {
    return fetchPlayerInfo(true, playerChatId);
  }

  public PlayerSummary fetchPlayerInfo(final PlayerChatId playerChatId) {
    return fetchPlayerInfo(false, playerChatId);
  }

  private PlayerSummary fetchPlayerInfo(
      final boolean isModerator, final PlayerChatId playerChatId) {
    final var chatterSession =
        chatters.lookupPlayerByChatId(playerChatId).orElseThrow(this::playerLeftChatException);

    var playerSummaryBuilder =
        PlayerSummary.builder()
            .registrationDateEpochMillis(lookupRegistrationDate(playerChatId))
            .currentGames(
                gameListing.getGameNamesPlayerHasJoined(
                    chatterSession.getChatParticipant().getUserName()));

    // if a moderator is requesting player data, then attach ban and aliases information
    if (isModerator) {
      final PlayerIdentifiersByApiKeyLookup gamePlayerLookup =
          apiKeyDaoWrapper
              .lookupPlayerByChatId(playerChatId)
              .orElseThrow(this::playerLeftChatException);

      playerSummaryBuilder =
          playerSummaryBuilder
              .systemId(gamePlayerLookup.getSystemId().getValue())
              .ip(chatterSession.getIp().toString())
              .aliases(
                  lookupPlayerAliases(gamePlayerLookup.getSystemId(), gamePlayerLookup.getIp()))
              .bans(lookupPlayerBans(gamePlayerLookup.getSystemId(), gamePlayerLookup.getIp()));
    }

    return playerSummaryBuilder.build();
  }

  @Nullable
  private Long lookupRegistrationDate(final PlayerChatId playerChatId) {
    return apiKeyDaoWrapper
        .lookupUserIdByChatId(playerChatId)
        .flatMap(playerHistoryDao::lookupPlayerHistoryByUserId)
        .map(PlayerHistoryRecord::getRegistrationDate)
        .orElse(null);
  }

  private IllegalArgumentException playerLeftChatException() {
    return new IllegalArgumentException("Player could not be found, have they left chat?");
  }

  private Collection<Alias> lookupPlayerAliases(final SystemId systemId, final String ip) {
    return playerInfoForModeratorDao.lookupPlayerAliasRecords(systemId.getValue(), ip).stream()
        .map(PlayerAliasRecord::toAlias)
        .collect(Collectors.toList());
  }

  private Collection<BanInformation> lookupPlayerBans(final SystemId systemId, final String ip) {
    return playerInfoForModeratorDao.lookupPlayerBanRecords(systemId.getValue(), ip).stream()
        .map(PlayerBanRecord::toBanInformation)
        .collect(Collectors.toList());
  }
}
