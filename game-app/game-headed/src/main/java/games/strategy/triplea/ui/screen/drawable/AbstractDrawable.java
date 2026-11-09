package games.strategy.triplea.ui.screen.drawable;

import java.util.Comparator;

/**
 * Abstract base class for IDrawable that implements the {@link Comparable} interface for all
 * Drawables.
 */
public abstract class AbstractDrawable implements IDrawable {
  @Override
  public int compareTo(final IDrawable other) {
    return Comparator.comparing(IDrawable::getLevel).compare(this, other);
  }
}
