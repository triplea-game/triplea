package games.strategy.triplea.delegate.battle.simulation;

import java.util.List;

/** Ordered replay results and coarse worker throughput metrics for one batch request. */
public record BattleBatchResult(
    List<BattleReplayResult> results,
    int matchedEpisodes,
    int mismatchedEpisodes,
    int workerCount,
    long elapsedNanos,
    long usedMemoryBytes,
    double episodesPerSecond) {
  public BattleBatchResult {
    results = List.copyOf(results);
    if (matchedEpisodes < 0 || mismatchedEpisodes < 0) {
      throw new IllegalArgumentException("batch counts must not be negative");
    }
    if (matchedEpisodes + mismatchedEpisodes != results.size()) {
      throw new IllegalArgumentException("batch counts must equal result count");
    }
    if (workerCount < 0 || elapsedNanos < 0 || usedMemoryBytes < 0 || episodesPerSecond < 0) {
      throw new IllegalArgumentException("batch metrics must not be negative");
    }
  }

  public static BattleBatchResult from(
      final List<BattleReplayResult> results,
      final int workerCount,
      final long elapsedNanos,
      final long usedMemoryBytes) {
    final int matched = (int) results.stream().filter(BattleReplayResult::matched).count();
    final double episodesPerSecond =
        elapsedNanos == 0 ? 0 : results.size() * 1_000_000_000.0 / elapsedNanos;
    return new BattleBatchResult(
        results,
        matched,
        results.size() - matched,
        workerCount,
        elapsedNanos,
        usedMemoryBytes,
        episodesPerSecond);
  }
}
