package org.triplea.modules.game.listing;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
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
import org.triplea.db.dao.lobby.games.LobbyGameDao;
import org.triplea.db.dao.moderator.ModeratorAuditHistoryDao;
import org.triplea.domain.data.ApiKey;
import org.triplea.domain.data.LobbyGame;
import org.triplea.domain.data.UserName;
import org.triplea.http.client.lobby.game.lobby.watcher.GameListingClient;
import org.triplea.http.client.lobby.game.lobby.watcher.LobbyGameListing;
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
  @NonNull private final LobbyGameDao lobbyGameDao;
  @NonNull private final TtlCache<GameId, LobbyGame> games;
  @NonNull private final WebSocketMessagingBus playerMessagingBus;

  /** Map of player names to the games they are in, both observing and playing. */
  @NonNull private final Multimap<UserName, GameId> playerIsInGames = HashMultimap.create();

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
        .lobbyGameDao(jdbi.onDemand(LobbyGameDao.class))
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
  public String postGame(final ApiKey apiKey, final LobbyGame lobbyGame) {
    final String id = UUID.randomUUID().toString();
    games.put(new GameId(apiKey, id), lobbyGame);
    final var lobbyGameListing = LobbyGameListing.builder().gameId(id).lobbyGame(lobbyGame).build();
    lobbyGameDao.insertLobbyGame(apiKey, lobbyGameListing);
    playerMessagingBus.broadcastMessage(new LobbyGameUpdatedMessage(lobbyGameListing));
    log.info("Posted game: {}", id);
    return id;
  }

  /** Adds or updates a game. Returns true if game is updated, false if game was not found. */
  public boolean updateGame(final ApiKey apiKey, final String id, final LobbyGame lobbyGame) {
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

  /**
   * Removes a game from the active listing, any players marked as in the game are updated to no
   * longer be listed as participating in that game.
   */
  public void removeGame(final ApiKey apiKey, final String id) {
    log.info("Removing game: {}", id);
    final GameId key = new GameId(apiKey, id);

    final var gameEntries =
        playerIsInGames.entries().stream()
            .filter(entry -> entry.getValue().equals(key))
            .collect(Collectors.toList());
    gameEntries.forEach(entry -> playerIsInGames.remove(entry.getKey(), entry.getValue()));

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

  /** Checks if a given api-key and game-id pair are valid and match an active game. */
  public boolean isValidApiKeyAndGameId(final ApiKey apiKey, final String gameId) {
    return games.get(new GameId(apiKey, gameId)).isPresent();
  }

  /**
   * If a game does not receive a 'keepAlive' in a timely manner, it is removed from the list.
   *
   * @return Return false to inform client game is not present. Client can respond by re-posting
   *     their game. Otherwise true indicates the game is present and the keep-alive period has been
   *     extended.
   */
  public boolean keepAlive(final ApiKey apiKey, final String id) {
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

  public Optional<InetSocketAddress> getHostForGame(final ApiKey apiKey, final String id) {
    return games
        .get(new GameId(apiKey, id))
        .map(
            lobbyGame ->
                new InetSocketAddress(lobbyGame.getHostAddress(), lobbyGame.getHostPort()));
  }

  public void addPlayerToGame(final UserName userName, final GameId gameId) {
    playerIsInGames.put(userName, gameId);
  }

  /**
   * Gets the collection of active games (identified by hostname) that a player is playing in or has
   * joined as an observer.
   */
  public Collection<String> getGameNamesPlayerHasJoined(final UserName userName) {
    final Collection<GameId> expiredGames =
        playerIsInGames.get(userName).stream()
            .filter(gameId -> games.get(gameId).isEmpty())
            .collect(Collectors.toList());
    expiredGames.forEach(gameId -> playerIsInGames.remove(userName, gameId));

    return playerIsInGames.get(userName).stream()
        .map(gameId -> games.get(gameId).map(LobbyGame::getHostName).orElse(null))
        .collect(Collectors.toList());
  }
}
