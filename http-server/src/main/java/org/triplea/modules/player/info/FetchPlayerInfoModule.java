package org.triplea.modules.player.info;

import java.util.Collection;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.jdbi.v3.core.Jdbi;
import org.triplea.db.dao.api.key.PlayerApiKeyDaoWrapper;
import org.triplea.db.dao.api.key.PlayerIdentifiersByApiKeyLookup;
import org.triplea.db.dao.moderator.player.info.PlayerAliasRecord;
import org.triplea.db.dao.moderator.player.info.PlayerBanRecord;
import org.triplea.db.dao.moderator.player.info.PlayerInfoForModeratorDao;
import org.triplea.db.dao.user.role.UserRole;
import org.triplea.domain.data.PlayerChatId;
import org.triplea.domain.data.SystemId;
import org.triplea.http.client.lobby.moderator.PlayerSummary;
import org.triplea.http.client.lobby.moderator.PlayerSummary.Alias;
import org.triplea.http.client.lobby.moderator.PlayerSummary.BanInformation;
import org.triplea.modules.access.authentication.AuthenticatedUser;
import org.triplea.modules.chat.Chatters;
import org.triplea.modules.game.listing.GameListing;

@AllArgsConstructor
class FetchPlayerInfoModule implements BiFunction<AuthenticatedUser, PlayerChatId, PlayerSummary> {
  private final PlayerApiKeyDaoWrapper apiKeyDaoWrapper;
  private final PlayerInfoForModeratorDao playerInfoForModeratorDao;
  private final Chatters chatters;
  private final GameListing gameListing;

  static FetchPlayerInfoModule build(
      final Jdbi jdbi, final Chatters chatters, final GameListing gameListing) {
    return new FetchPlayerInfoModule(
        PlayerApiKeyDaoWrapper.build(jdbi),
        jdbi.onDemand(PlayerInfoForModeratorDao.class),
        chatters,
        gameListing);
  }

  @Override
  public PlayerSummary apply(
      final AuthenticatedUser authenticatedUser, final PlayerChatId playerChatId) {

    final var chatterSession =
        chatters.lookupPlayerByChatId(playerChatId).orElseThrow(this::playerLeftChatException);

    var playerSummaryBuilder =
        PlayerSummary.builder()
            .currentGames(
                gameListing.getGameNamesPlayerHasJoined(
                    chatterSession.getChatParticipant().getUserName()));

    // if a moderator is requesting player data, then attach ban and aliases information
    if (UserRole.isModerator(authenticatedUser.getUserRole())) {
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
