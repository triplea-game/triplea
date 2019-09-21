package org.triplea.server.lobby.game.listing;

import com.google.common.annotations.VisibleForTesting;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.triplea.http.client.lobby.game.listing.LobbyGame;
import org.triplea.http.client.lobby.game.listing.LobbyGameListing;
import org.triplea.lobby.server.db.dao.ModeratorAuditHistoryDao;

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
 *
 * <h2>Event Updates</h2>
 *
 * A listener for game created or updated events and another listener game removed are injected into
 * this class. Those listeners are invoked for the respective events. The listener is then intended
 * to be a websocket to notify players of the change event.
 */
@Builder
@Slf4j
// TODO: Project#12 Create a thread that will invoke game reaper periodically to prune dead games
class GameListing {
  @NonNull private final Consumer<LobbyGameListing> gameUpdateListener;
  @NonNull private final Consumer<String> gameRemoveListener;
  @NonNull private final GameReaper gameReaper;
  @NonNull private final ModeratorAuditHistoryDao auditHistoryDao;

  private final Map<GameId, LobbyGame> games = new HashMap<>();

  @Builder
  @EqualsAndHashCode
  @Getter
  static class GameId {
    @NonNull private final String apiKey;
    @NonNull private final String id;
  }

  @VisibleForTesting
  class GameNotFound extends RuntimeException {
    private static final long serialVersionUID = 2161752095040334977L;

    private GameNotFound(final String gameId) {
      super("Error: Game not found, report this error to TripleA Development");
      log.error(
          "Game ID not found: {}, available game IDs: {}",
          gameId,
          games.keySet().stream().map(GameId::getId).collect(Collectors.toList()));
    }
  }

  @VisibleForTesting
  static class IncorrectApiKey extends RuntimeException {
    private static final long serialVersionUID = -131279328512375629L;

    IncorrectApiKey() {
      super("Illegal game modification attempt, access key is incorrect.");
    }
  }

  /** Adds a game. Duplicate postings return the same gameId. */
  String postGame(final String apiKey, final LobbyGame lobbyGame) {
    final Optional<String> existingGameId =
        games.entrySet().stream()
            .filter(entry -> entry.getKey().apiKey.equals(apiKey))
            .filter(entry -> entry.getValue().equals(lobbyGame))
            .map(entry -> entry.getKey().id)
            .findAny();
    if (existingGameId.isPresent()) {
      log.warn("Received duplicate game post request for game: {}", existingGameId.get());
      return existingGameId.get();
    }

    final String gameId = UUID.randomUUID().toString();
    gameReaper.registerKeepAlive(gameId);
    games.put(GameId.builder().id(gameId).apiKey(apiKey).build(), lobbyGame);
    gameUpdateListener.accept(
        LobbyGameListing.builder().gameId(gameId).lobbyGame(lobbyGame).build());
    log.info("Posted game: {}", gameId);
    return gameId;
  }

  /** Adds or updates a game. If game is updated, emits a game update event. */
  void updateGame(final String apiKey, final String gameId, final LobbyGame lobbyGame) {
    final Map.Entry<GameId, LobbyGame> game =
        // find game by gameId
        games.entrySet().stream()
            .filter(entry -> entry.getKey().id.equals(gameId))
            .findAny()
            .orElseThrow(() -> new GameNotFound(gameId));
    if (!game.getKey().apiKey.equals(apiKey)) {
      throw new IncorrectApiKey();
    }

    games.put(game.getKey(), lobbyGame);
    gameUpdateListener.accept(
        LobbyGameListing.builder().gameId(game.getKey().id).lobbyGame(lobbyGame).build());
  }

  void removeGame(final String apiKey, final String gameId) {
    final var game =
        games.entrySet().stream().filter(entry -> entry.getKey().id.equals(gameId)).findAny();
    game.ifPresent(
        gameEntryToRemove -> {
          final GameId gameIdToRemove = gameEntryToRemove.getKey();
          if (!gameIdToRemove.apiKey.equals(apiKey)) {
            throw new IncorrectApiKey();
          }
          removeGame(gameIdToRemove);
        });
  }

  private void removeGame(final GameId gameId) {
    log.info("Removing game: {}", gameId);
    games.remove(gameId);
    gameRemoveListener.accept(gameId.id);
  }

  List<LobbyGameListing> getGames() {
    gameReaper.findDeadGames(games.keySet()).forEach(this::removeGame);

    return games.entrySet().stream()
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
  boolean keepAlive(final String apiKey, final String gameId) {
    final var gameEntry =
        games.entrySet().stream().filter(entry -> entry.getKey().id.equals(gameId)).findAny();
    if (gameEntry.isEmpty()) {
      log.warn("Keep alive received for DEAD game: {}", gameId);
      return false;
    }

    final var id = gameEntry.get().getKey();

    if (!id.apiKey.equals(apiKey)) {
      throw new IncorrectApiKey();
    }
    gameReaper.registerKeepAlive(gameId);
    log.info("Keep alive received for game: {}", gameId);
    return true;
  }

  /** Moderator action to remove a game. */
  void bootGame(final int moderatorId, final String gameId) {
    final GameId gameToRemove =
        games.entrySet().stream()
            .filter(entry -> entry.getKey().id.equals(gameId))
            .findAny()
            .map(Map.Entry::getKey)
            .orElseThrow(() -> new GameNotFound(gameId));

    final String hostName = games.get(gameToRemove).getHostName();

    removeGame(gameToRemove);
    log.info("Moderator {} booted game: {}, hosted by: {}", moderatorId, gameId, hostName);
    auditHistoryDao.addAuditRecord(
        ModeratorAuditHistoryDao.AuditArgs.builder()
            .moderatorUserId(moderatorId)
            .actionName(ModeratorAuditHistoryDao.AuditAction.BOOT_GAME)
            .actionTarget(hostName)
            .build());
  }
}
