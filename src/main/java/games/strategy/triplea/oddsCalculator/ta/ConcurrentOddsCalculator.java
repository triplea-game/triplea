package games.strategy.triplea.oddsCalculator.ta;

import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import games.strategy.triplea.oddscalc.OddsCalculatorParameters;

/**
 * Concurrent wrapper class for the OddsCalculator. It spawns multiple worker threads and splits up the run count
 * across these workers. This is mainly to be used by AIs since they call the OddsCalculator a lot.
 */
public class ConcurrentOddsCalculator extends OddsCalculator {
  private volatile boolean cancelled = false;

  /**
   * Most calc's are completed under 5s. 10s or longer is a pretty extreme
   * amount of time to wait for a calc, so we add 50% to that and timeout there.
   */
  private static final long CALCULATOR_TIMEOUT_SECONDS = 15;

  /**
   * Concurrently calculates odds using the OddsCalculatorWorker. It uses Executor to process the results. Then waits
   * for all the future results and combines them together.
   */
  @Override
  public AggregateResults calculate(final OddsCalculatorParameters parameters) {
    final ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    final long start = System.currentTimeMillis();
    final Collection<BattleResults> battleResults = new ConcurrentLinkedQueue<>();

    final int totalRunCount = parameters.runCount;
    parameters.setRunCount(1);
    for (int i = 0; i < totalRunCount; i++) {
      executor.submit(() -> {
        if (!cancelled) {
          battleResults.add(OddsCalculator.doSimulation(parameters));
        }
      });
    }
    parameters.setRunCount(totalRunCount);

    executor.shutdown();
    try {
      executor.awaitTermination(CALCULATOR_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    return new AggregateResults(System.currentTimeMillis() - start, battleResults);
  }

  /*
   * Implementation note: Set flag to abort any new operations, then cancel any ongoing workers.
   */
  @Override
  public void cancel() {
    cancelled = true;
  }
}
