package games.strategy.triplea.delegate.scoring;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameState;
import games.strategy.triplea.delegate.EndRoundDelegate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Ends the game once the scoring round is reached, awarding the win to the highest scorer.
 *
 * <p>Termination is opt-in through the {@code Auto Termination} property: with it off the delegate
 * behaves exactly like {@link EndRoundDelegate}, so a scenario can still be played past the scoring
 * round to record a full position for balance testing.
 */
public final class SmallFrontEndRoundDelegate extends EndRoundDelegate {

  @Override
  public void start() {
    super.start();
    final GameState data = getData();
    if (isGameOver() || !SmallFrontScoringService.isAutoTerminationEnabled(data)) {
      return;
    }
    if (data.getSequence().getRound() < SmallFrontScoringService.getScoringRound(data)) {
      return;
    }
    final Map<GamePlayer, Integer> scores = SmallFrontScoringService.score(data);
    if (scores.isEmpty()) {
      return;
    }
    final Optional<GamePlayer> winner = SmallFrontScoringService.winner(data);
    final String tally =
        scores.entrySet().stream()
            .map(entry -> entry.getKey().getName() + " " + entry.getValue())
            .collect(Collectors.joining(", "));
    final String status =
        winner
            .map(player -> player.getName() + " wins on operational score (" + tally + ")")
            .orElse("Game ends in a draw on operational score (" + tally + ")");
    signalGameOver(status, winner.map(List::of).orElse(List.of()), bridge);
  }
}
