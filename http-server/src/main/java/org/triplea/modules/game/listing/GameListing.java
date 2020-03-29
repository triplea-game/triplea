package org.triplea.modules.game.listing;

import com.google.common.annotations.VisibleForTesting;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.jdbi.v3.core.Jdbi;
import org.triplea.db.dao.ModeratorAuditHistoryDao;
import org.triplea.domain.data.ApiKey;
import org.triplea.domain.data.LobbyGame;
import org.triplea.http.client.lobby.game.listing.GameListingClient;
import org.triplea.http.client.lobby.game.listing.LobbyGameListing;
import org.triplea.http.client.web.socket.messages.envelopes.game.listing.LobbyGameRemovedMessage;
import org.triplea.http.client.web.socket.messages.envelopes.game.listing.LobbyGameUpdatedMessage;
import org.triplea.java.cache.ExpiringAfterWriteCache;
import org.triplea.java.cache.TtlCache;
import org.triplea.web.socket.WebSocketMessagingBus;

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
public class GameListing {
  @NonNull private final ModeratorAuditHistoryDao auditHistoryDao;
  @NonNull private final TtlCache<GameId, LobbyGame> games;
  @NonNull private final WebSocketMessagingBus playerMessagingBus;

  @AllArgsConstructor
  @EqualsAndHashCode
  @Getter
  @VisibleForTesting
  @ToString
  static class GameId {
    @NonNull private final ApiKey apiKey;
    @NonNull private final String id;
  }

  public static GameListing build(final Jdbi jdbi, final WebSocketMessagingBus playerMessagingBus) {
    return GameListing.builder()
        .auditHistoryDao(jdbi.onDemand(ModeratorAuditHistoryDao.class))
        .playerMessagingBus(playerMessagingBus)
        .games(
            new ExpiringAfterWriteCache<>(
                GameListingClient.KEEP_ALIVE_SECONDS,
                TimeUnit.SECONDS,
                new GameTtlExpiredListener(playerMessagingBus)))
        .build();
  }

  /** Adds a game. */
  String postGame(final ApiKey apiKey, final LobbyGame lobbyGame) {
    final String id = UUID.randomUUID().toString();
    games.put(new GameId(apiKey, id), lobbyGame);
    playerMessagingBus.broadcastMessage(
        new LobbyGameUpdatedMessage(
            LobbyGameListing.builder().gameId(id).lobbyGame(lobbyGame).build()));
    log.info("Posted game: {}", id);
    return id;
  }

  /** Adds or updates a game. Returns true if game is updated, false if game was not found. */
  boolean updateGame(final ApiKey apiKey, final String id, final LobbyGame lobbyGame) {
    final var listedGameId = new GameId(apiKey, id);
    final LobbyGame existingValue = games.replace(listedGameId, lobbyGame).orElse(null);

    if (existingValue != null) {
      playerMessagingBus.broadcastMessage(
          new LobbyGameUpdatedMessage(
              LobbyGameListing.builder().gameId(id).lobbyGame(lobbyGame).build()));
      return true;
    } else {
      return false;
    }
  }

  void removeGame(final ApiKey apiKey, final String id) {
    log.info("Removing game: {}", id);
    final GameId key = new GameId(apiKey, id);

    games
        .invalidate(key)
        .ifPresent(value -> playerMessagingBus.broadcastMessage(new LobbyGameRemovedMessage(id)));
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
  boolean keepAlive(final ApiKey apiKey, final String id) {
    return games.refresh(new GameId(apiKey, id));
  }

  /** Moderator action to remove a game. */
  void bootGame(final int moderatorId, final String id) {
    games
        .findEntryByKey(gameId -> gameId.id.equals(id))
        .ifPresent(
            gameToRemove -> {
              final String hostName = gameToRemove.getValue().getHostName();
              removeGame(gameToRemove.getKey().apiKey, gameToRemove.getKey().id);

              log.info("Moderator {} booted game: {}, hosted by: {}", moderatorId, id, hostName);
              auditHistoryDao.addAuditRecord(
                  ModeratorAuditHistoryDao.AuditArgs.builder()
                      .moderatorUserId(moderatorId)
                      .actionName(ModeratorAuditHistoryDao.AuditAction.BOOT_GAME)
                      .actionTarget(hostName)
                      .build());
            });
  }
}
