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
 * Steps one game to its end, a decisive lead, the round cap, the stop-after player's first turn
 * end, or the timeout, calling back after every step, at the end of every player turn, and once
 * more on the final state.
 */
final class GameDriver {
  /** Receives game state at a turn end ({@code player} set) or at the end of the game. */
  interface TurnListener {
    void onSnapshot(
        GameData data, int round, @Nullable GamePlayer player, int turnIndex, boolean last);

    /** Called after every step has run, with the step that just ran. */
    void onStep(GameData data, int round, GameStep step);
  }

  /**
   * How a game ended. {@code winner} is the winning alliance names joined by "+", or null; a
   * decisive game's winner is the alliance that led.
   */
  record Outcome(
      Status status,
      @Nullable String winner,
      List<String> winners,
      int rounds,
      boolean capped,
      boolean stopped,
      boolean decisive) {

    /** Why the game ended: won, decisive, stop, cap, or timeout. */
    String end() {
      if (status == Status.TIMEOUT) {
        return "timeout";
      }
      if (decisive) {
        return "decisive";
      }
      if (stopped) {
        return "stop";
      }
      return capped ? "cap" : "won";
    }
  }

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
    String decisiveAlliance = null;
    int decidedRound = 0;
    final Decisive decisive = new Decisive();
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
        if (stopAfter == null && !isOver(game) && data.getSequence().getRound() > round) {
          decisiveAlliance = decisive.observe(Decisive.leader(data));
          if (decisiveAlliance != null) {
            decidedRound = round;
            break;
          }
        }
      }
    } catch (final GameOverException e) {
      // The engine signals some game ends by throwing; isOver() below reads the result.
    }
    final boolean over = isOver(game);
    final Collection<GamePlayer> winners =
        over
            ? winners(data)
            : decisiveAlliance == null
                ? List.<GamePlayer>of()
                : data.getAllianceTracker().getPlayersInAlliance(decisiveAlliance);
    final int rounds =
        decisiveAlliance == null ? Math.min(data.getSequence().getRound(), roundCap) : decidedRound;
    listener.onSnapshot(data, rounds, null, turnIndex, true);
    if (!game.isGameOver()) {
      // Also resets ProAi's static battle calculator so the next game in this JVM starts clean.
      game.stopGame();
    }
    return new Outcome(
        status,
        decisiveAlliance != null
            ? decisiveAlliance
            : winners.isEmpty() ? null : winnerAlliances(winners),
        winners.stream().map(GamePlayer::getName).sorted().toList(),
        rounds,
        status == Status.OK && !over && !stopped && decisiveAlliance == null,
        stopped,
        decisiveAlliance != null);
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
