package org.triplea.ai.benchmark.runner;

import games.strategy.engine.GameOverException;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameStep;
import games.strategy.engine.framework.ServerGame;
import games.strategy.triplea.delegate.AbstractEndTurnDelegate;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

/**
 * Steps one game to its end, the round cap, the stop-after player's first turn end, or the timeout,
 * calling back after every step, at the end of every player turn, and once more on the final state.
 */
final class GameDriver {
  /** Receives game state at a turn end ({@code player} set) or at the end of the game. */
  interface TurnListener {
    void onSnapshot(
        GameData data, int round, @Nullable GamePlayer player, int turnIndex, boolean last);

    /** Called after every step has run, with the step that just ran. */
    void onStep(GameData data, int round, GameStep step);
  }

  /** How a game ended. {@code winner} is the winning alliance names joined by "+", or null. */
  record Outcome(
      Status status,
      @Nullable String winner,
      List<String> winners,
      int rounds,
      boolean capped,
      boolean stopped) {}

  enum Status {
    OK,
    TIMEOUT
  }

  private GameDriver() {}

  static Outcome play(
      final ServerGame game,
      final int roundCap,
      final Duration timeout,
      @Nullable final String stopAfter,
      final TurnListener listener) {
    final GameData data = game.getData();
    final long deadline = System.nanoTime() + timeout.toNanos();
    int turnIndex = 0;
    Status status = Status.OK;
    boolean stopped = false;
    try {
      while (!isOver(game) && data.getSequence().getRound() <= roundCap) {
        if (System.nanoTime() > deadline) {
          status = Status.TIMEOUT;
          break;
        }
        final GameStep step = data.getSequence().getStep();
        final int round = data.getSequence().getRound();
        game.runNextStep();
        listener.onStep(data, round, step);
        if (step.getDelegate() instanceof AbstractEndTurnDelegate && step.getPlayerId() != null) {
          listener.onSnapshot(data, round, step.getPlayerId(), turnIndex++, false);
          if (step.getPlayerId().getName().equals(stopAfter)) {
            stopped = true;
            break;
          }
        }
      }
    } catch (final GameOverException e) {
      // The engine signals some game ends by throwing; isOver() below reads the result.
    }
    final boolean over = isOver(game);
    final Collection<GamePlayer> winners = over ? winners(data) : List.of();
    final int rounds = Math.min(data.getSequence().getRound(), roundCap);
    listener.onSnapshot(data, rounds, null, turnIndex, true);
    if (!game.isGameOver()) {
      // Also resets ProAi's static battle calculator so the next game in this JVM starts clean.
      game.stopGame();
    }
    return new Outcome(
        status,
        winners.isEmpty() ? null : winnerAlliances(winners),
        winners.stream().map(GamePlayer::getName).sorted().toList(),
        rounds,
        status == Status.OK && !over && !stopped,
        stopped);
  }

  private static boolean isOver(final ServerGame game) {
    return game.isGameOver() || !winners(game.getData()).isEmpty();
  }

  private static Collection<GamePlayer> winners(final GameData data) {
    return data.getEndRoundDelegate().getWinners();
  }

  private static String winnerAlliances(final Collection<GamePlayer> winners) {
    return winners.stream()
        .flatMap(p -> p.getData().getAllianceTracker().getAlliancesPlayerIsIn(p).stream())
        .distinct()
        .sorted()
        .collect(Collectors.joining("+"));
  }
}
