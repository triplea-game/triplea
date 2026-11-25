package games.strategy.triplea.odds.calculator.adapter;

import games.strategy.engine.random.IRandomSource;
import games.strategy.triplea.odds.calculator.context.seam.RandomSource;

/**
 * Feeds engine dice into the engine-free calc core — the one place an {@code IRandomSource} crosses
 * the boundary into a core {@link RandomSource}.
 */
public class EngineRandomSource implements RandomSource {
  private final IRandomSource delegate;

  public EngineRandomSource(final IRandomSource delegate) {
    this.delegate = delegate;
  }

  @Override
  public int getRandom(final int max, final String annotation) {
    return delegate.getRandom(max, annotation);
  }

  @Override
  public int[] getRandom(final int max, final int count, final String annotation) {
    return delegate.getRandom(max, count, annotation);
  }
}
