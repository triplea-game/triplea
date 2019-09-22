package org.triplea.server.lobby.game.listing;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.Cache;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.triplea.http.client.lobby.game.listing.LobbyGame;
import org.triplea.http.client.lobby.game.listing.LobbyGameListing;
import org.triplea.lobby.server.db.dao.ModeratorAuditHistoryDao;
import org.triplea.server.access.ApiKey;

/**
 * Class that stores the set of games in the lobby. Games are identified by a combination of two
 * values, the api-key of the user that created the game and a UUID assigned when the game is
 * posted. Keep in mind that the 'gameId' is a publicly known value. Therefore we use API key, for
 * example, to ensure that other users can't invoke endpoints directly to remove the games of other
 * players.<br>
 *
 * <h2>Keep-Alive</h2>
 *
 * The game listing has a concept of 'keep-alive' where games, after posting, need to send
 * 'keep-alive' messages periodically or they will be de-listed. If a game has been de-listed, and
 * we get a 'keep-alive' message, the client will get a 'false' value back indicating they would
 * need to (automatically) re-post their game. This way for example if the lobby were to be
 * restarted, clients sending keep-alives would notice that their game is no longer listed and would
 * automatically send a game post message to re-post their game.
 *
 * <h2>Moderator Boot Game</h2>
 *
 * The moderator boot is similar to remove game but there is no check for an API key, any moderator
 * can boot any game.
 */
@Builder
@Slf4j
class GameListing {
  @NonNull private final ModeratorAuditHistoryDao auditHistoryDao;
  @NonNull private final Cache<GameId, LobbyGame> games;

  @AllArgsConstructor
  @EqualsAndHashCode
  @Getter
  @VisibleForTesting
  @ToString
  static class GameId {
    @NonNull private final ApiKey apiKey;
    @NonNull private final String id;
  }

  /** Adds a game. */
  String postGame(final ApiKey apiKey, final LobbyGame lobbyGame) {
    final String gameId = UUID.randomUUID().toString();
    games.put(new GameId(apiKey, gameId), lobbyGame);
    log.info("Posted game: {}", gameId);
    return gameId;
  }

  /** Adds or updates a game. If game is updated, emits a game update event. */
  boolean updateGame(final ApiKey apiKey, final String gameId, final LobbyGame lobbyGame) {
    final GameId listedGameId = new GameId(apiKey, gameId);
    return Optional.ofNullable(games.getIfPresent(listedGameId))
        .map(
            game -> {
              games.put(listedGameId, lobbyGame);
              return true;
            })
        .orElse(false);
  }

  void removeGame(final ApiKey apiKey, final String gameId) {
    log.info("Removing game: {}", gameId);
    games.invalidate(new GameId(apiKey, gameId));
  }

  List<LobbyGameListing> getGames() {
    return games.asMap().entrySet().stream()
        .map(
            entry ->
                LobbyGameListing.builder()
                    .gameId(entry.getKey().id)
                    .lobbyGame(entry.getValue())
                    .build())
        .collect(Collectors.toList());
  }

  /**
   * If a game does not receive a 'keepAlive' in a timely manner, it is removed from the list.
   *
   * @return Return false to inform client game is not present. Client can respond by re-posting
   *     their game. Otherwise true indicates the game is present and the keep-alive period has been
   *     extended.
   */
  boolean keepAlive(final ApiKey apiKey, final String gameId) {
    final Optional<LobbyGame> game =
        Optional.ofNullable(games.getIfPresent(new GameId(apiKey, gameId)));
    if (game.isEmpty()) {
      log.warn("Keep alive for removed game: {}", gameId);
      return false;
    }
    updateGame(apiKey, gameId, game.get());
    return true;
  }

  /** Moderator action to remove a game. */
  void bootGame(final int moderatorId, final String gameId) {
    games.asMap().entrySet().stream()
        .filter(entry -> entry.getKey().id.equals(gameId))
        .findAny()
        .ifPresent(
            gameToRemove -> {
              final String hostName = gameToRemove.getValue().getHostName();
              removeGame(gameToRemove.getKey().apiKey, gameToRemove.getKey().id);

              log.info(
                  "Moderator {} booted game: {}, hosted by: {}", moderatorId, gameId, hostName);
              auditHistoryDao.addAuditRecord(
                  ModeratorAuditHistoryDao.AuditArgs.builder()
                      .moderatorUserId(moderatorId)
                      .actionName(ModeratorAuditHistoryDao.AuditAction.BOOT_GAME)
                      .actionTarget(hostName)
                      .build());
            });
  }
}
