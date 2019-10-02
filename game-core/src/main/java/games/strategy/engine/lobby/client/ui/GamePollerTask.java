package games.strategy.engine.lobby.client.ui;

import feign.FeignException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.triplea.http.client.lobby.game.listing.LobbyGame;
import org.triplea.http.client.lobby.game.listing.LobbyGameListing;
import org.triplea.lobby.common.LobbyGameUpdateListener;

@RequiredArgsConstructor
class GamePollerTask implements Runnable {
  /** Callback object, we notify this when we detect changes or updates. */
  private final LobbyGameUpdateListener lobbyGameBroadcaster;
  /** Fetches the latest game listing we have in our local model. */
  private final Supplier<Map<String, LobbyGame>> localGamesFetcher;
  /** Client object used to fetch the latest information from server. */
  private final Supplier<List<LobbyGameListing>> lobbyGamesFetcher;
  /** Callback object to report an error, should either log or show a dialog to user. */
  private final Consumer<String> errorMessageReporter;

  /** Tracking variable so we do not show an error message twice before we see a recovery. */
  private volatile boolean lastPollSuccess = false;

  @Override
  public void run() {
    fetchGamesFromLobby()
        .ifPresent(
            lobbyGames -> {
              final Map<String, LobbyGame> knownGames = localGamesFetcher.get();

              findNewGames(knownGames.keySet(), lobbyGames)
                  .forEach(lobbyGameBroadcaster::gameUpdated);

              findUpdatedGames(knownGames, lobbyGames) //
                  .forEach(lobbyGameBroadcaster::gameUpdated);

              findRemovedGames(knownGames.keySet(), lobbyGames)
                  .forEach(lobbyGameBroadcaster::gameRemoved);
            });
  }

  private Optional<List<LobbyGameListing>> fetchGamesFromLobby() {
    try {
      final List<LobbyGameListing> lobbyGames = lobbyGamesFetcher.get();
      lastPollSuccess = true;
      return Optional.of(lobbyGames);
    } catch (final FeignException e) {
      if (lastPollSuccess) {
        // only report new failures
        lastPollSuccess = false;
        errorMessageReporter.accept(e.getMessage());
      }
      return Optional.empty();
    }
  }

  private static Collection<LobbyGameListing> findNewGames(
      final Collection<String> knownGameIds, final List<LobbyGameListing> fetchedGames) {
    return fetchedGames.stream()
        .filter(listing -> !knownGameIds.contains(listing.getGameId()))
        .collect(Collectors.toSet());
  }

  private static Collection<String> findRemovedGames(
      final Collection<String> knownGameIds, final List<LobbyGameListing> fetchedGames) {

    final Collection<String> fetchedGameIds =
        fetchedGames.stream().map(LobbyGameListing::getGameId).collect(Collectors.toSet());

    return knownGameIds.stream()
        .filter(gameId -> !fetchedGameIds.contains(gameId))
        .collect(Collectors.toSet());
  }

  private static Collection<LobbyGameListing> findUpdatedGames(
      final Map<String, LobbyGame> knownGames, final List<LobbyGameListing> lobbyGames) {

    // Find each lobby game in the known game list.
    // If lobby game is present in the known game list, check if the lobby game is not equal to the
    // known game.
    // Return all lobby games that are present in the known set and do not match.
    return lobbyGames.stream()
        .filter(
            lobbyGameListing ->
                Optional.ofNullable(knownGames.get(lobbyGameListing.getGameId()))
                    .map(knownGame -> !knownGame.equals(lobbyGameListing.getLobbyGame()))
                    .orElse(false))
        .collect(Collectors.toSet());
  }
}
