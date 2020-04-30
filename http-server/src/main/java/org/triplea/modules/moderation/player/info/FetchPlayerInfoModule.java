package org.triplea.modules.moderation.player.info;

import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.jdbi.v3.core.Jdbi;
import org.triplea.db.dao.api.key.ApiKeyDaoWrapper;
import org.triplea.db.dao.api.key.GamePlayerLookup;
import org.triplea.db.dao.moderator.player.info.PlayerAliasRecord;
import org.triplea.db.dao.moderator.player.info.PlayerBanRecord;
import org.triplea.db.dao.moderator.player.info.PlayerInfoForModeratorDao;
import org.triplea.domain.data.PlayerChatId;
import org.triplea.domain.data.SystemId;
import org.triplea.http.client.lobby.moderator.PlayerSummaryForModerator;
import org.triplea.http.client.lobby.moderator.PlayerSummaryForModerator.Alias;
import org.triplea.http.client.lobby.moderator.PlayerSummaryForModerator.BanInformation;

@AllArgsConstructor
class FetchPlayerInfoModule implements Function<PlayerChatId, PlayerSummaryForModerator> {
  private final ApiKeyDaoWrapper apiKeyDaoWrapper;
  private final PlayerInfoForModeratorDao playerInfoForModeratorDao;

  static FetchPlayerInfoModule build(final Jdbi jdbi) {
    return new FetchPlayerInfoModule(
        ApiKeyDaoWrapper.build(jdbi), jdbi.onDemand(PlayerInfoForModeratorDao.class));
  }

  @Override
  public PlayerSummaryForModerator apply(final PlayerChatId playerChatId) {
    final GamePlayerLookup gamePlayerLookup =
        apiKeyDaoWrapper
            .lookupPlayerByChatId(playerChatId)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Player could not be found, have they left chat?"));

    return PlayerSummaryForModerator.builder()
        .name(gamePlayerLookup.getUserName().getValue())
        .systemId(gamePlayerLookup.getSystemId().getValue())
        .ip(gamePlayerLookup.getIp())
        .aliases(lookupPlayerAliases(gamePlayerLookup.getSystemId(), gamePlayerLookup.getIp()))
        .bans(lookupPlayerBans(gamePlayerLookup.getSystemId(), gamePlayerLookup.getIp()))
        .build();
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
