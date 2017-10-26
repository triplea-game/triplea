package games.strategy.triplea.oddsCalculator.ta;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import games.strategy.triplea.oddscalc.OddsCalculatorParameters;

/**
 * Concurrent wrapper class for the OddsCalculator. It spawns multiple worker threads and splits up the run count
 * across these workers. This is mainly to be used by AIs since they call the OddsCalculator a lot.
 */
public class ConcurrentOddsCalculator extends OddsCalculator {
  private static final int MAX_THREADS = Math.max(1, Runtime.getRuntime().availableProcessors());

  private boolean cancelled = false;

  /**
   * Concurrently calculates odds using the OddsCalculatorWorker. It uses Executor to process the results. Then waits
   * for all the future results and combines them together.
   */
  @Override
  public AggregateResults calculate(final OddsCalculatorParameters parameters) {
    final ExecutorService executor = Executors.newFixedThreadPool(MAX_THREADS);
    final long start = System.currentTimeMillis();
    final AggregateResults aggregateResults = new AggregateResults(parameters.runCount);

    final int totalRunCount = parameters.runCount;
    for (int i = 0; i < totalRunCount && !cancelled; i++) {
      parameters.setRunCount(1);
      executor.submit(() -> aggregateResults.addResult(new OddsCalculator().doSimulation(parameters)));
    }
    parameters.setRunCount(totalRunCount);

    executor.shutdown();
    try {
      executor.awaitTermination(15, TimeUnit.SECONDS);
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    aggregateResults.setTime(System.currentTimeMillis() - start);
    return aggregateResults;
  }

  @Override
  public void cancel() {
    cancelled = true;
  }
}
