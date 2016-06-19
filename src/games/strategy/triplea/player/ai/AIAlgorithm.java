package games.strategy.triplea.player.ai;

/**
 * Utility class implementing AI game algorithms.
 * Currently, minimax and alpha-beta algorithms are implemented.
 */
public class AIAlgorithm {
  private static <Play> Pair<Float, Play> maxValue(final GameState<Play> state, float alpha, final float beta) {
    float value = Float.NEGATIVE_INFINITY;
    Play bestMove = null;
    for (final GameState<Play> s : state.successors()) {
      final Play a = s.getMove();
      float minValue;
      if (s.cutoffTest()) {
        minValue = s.getUtility();
      } else {
        minValue = minValue(s, alpha, beta).getFirst();
      }
      if (minValue > value) {
        value = minValue;
        bestMove = a;
      }
      if (value >= beta) {
        return new Pair<>(value, bestMove);
      }
      if (value > alpha) {
        alpha = value;
      }
    }
    return new Pair<>(value, bestMove);
  }

  private static <Play> Pair<Float, Play> minValue(final GameState<Play> state, final float alpha, float beta) {
    float value = Float.POSITIVE_INFINITY;
    Play bestMove = null;
    for (final GameState<Play> s : state.successors()) {
      final Play a = s.getMove();
      float maxValue;
      if (s.cutoffTest()) {
        maxValue = s.getUtility();
      } else {
        maxValue = maxValue(s, alpha, beta).getFirst();
      }
      if (maxValue < value) {
        value = maxValue;
        bestMove = a;
      }
      if (value <= alpha) {
        return new Pair<>(value, bestMove);
      }
      if (value < beta) {
        beta = value;
      }
    }
    return new Pair<>(value, bestMove);
  }

  private static <Play> Pair<Float, Play> maxValue(final GameState<Play> state) {
    float value = Float.NEGATIVE_INFINITY;
    Play bestMove = null;
    for (final GameState<Play> s : state.successors()) {
      final Play a = s.getMove();
      float minValue;
      if (s.gameIsOver()) {
        minValue = s.getUtility();
      } else {
        minValue = minValue(s).getFirst();
      }
      if (minValue > value) {
        value = minValue;
        bestMove = a;
      }
    }
    return new Pair<>(value, bestMove);
  }

  private static <Play> Pair<Float, Play> minValue(final GameState<Play> state) {
    float value = Float.POSITIVE_INFINITY;
    Play bestMove = null;
    for (final GameState<Play> s : state.successors()) {
      final Play a = s.getMove();
      float maxValue;
      if (s.gameIsOver()) {
        maxValue = s.getUtility();
      } else {
        maxValue = maxValue(s).getFirst();
      }
      if (maxValue < value) {
        value = maxValue;
        bestMove = a;
      }
    }
    return new Pair<>(value, bestMove);
  }

  static class Pair<First, Second> {
    private final First m_first;
    private final Second m_second;

    Pair(final First first, final Second second) {
      m_first = first;
      m_second = second;
    }

    First getFirst() {
      return m_first;
    }

    Second getSecond() {
      return m_second;
    }
  }
}
