package org.triplea.modules.moderation.player.info;

import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.jdbi.v3.core.Jdbi;
import org.triplea.db.dao.api.key.PlayerApiKeyDaoWrapper;
import org.triplea.db.dao.api.key.PlayerIdentifiersByApiKeyLookup;
import org.triplea.db.dao.moderator.player.info.PlayerAliasRecord;
import org.triplea.db.dao.moderator.player.info.PlayerBanRecord;
import org.triplea.db.dao.moderator.player.info.PlayerInfoForModeratorDao;
import org.triplea.domain.data.PlayerChatId;
import org.triplea.domain.data.SystemId;
import org.triplea.http.client.lobby.moderator.PlayerSummary;
import org.triplea.http.client.lobby.moderator.PlayerSummary.Alias;
import org.triplea.http.client.lobby.moderator.PlayerSummary.BanInformation;

@AllArgsConstructor
class FetchPlayerInfoModule implements Function<PlayerChatId, PlayerSummary> {
  private final PlayerApiKeyDaoWrapper apiKeyDaoWrapper;
  private final PlayerInfoForModeratorDao playerInfoForModeratorDao;

  static FetchPlayerInfoModule build(final Jdbi jdbi) {
    return new FetchPlayerInfoModule(
        PlayerApiKeyDaoWrapper.build(jdbi), jdbi.onDemand(PlayerInfoForModeratorDao.class));
  }

  @Override
  public PlayerSummary apply(final PlayerChatId playerChatId) {
    final PlayerIdentifiersByApiKeyLookup gamePlayerLookup =
        apiKeyDaoWrapper
            .lookupPlayerByChatId(playerChatId)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Player could not be found, have they left chat?"));

    return PlayerSummary.builder()
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
