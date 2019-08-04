package games.strategy.triplea.ui.screen.drawable;

import java.util.Comparator;

public abstract class AbstractDrawable implements IDrawable {
  @Override
  public int compareTo(IDrawable other) {
    return Comparator.comparing(IDrawable::getLevel).compare(this, other);
  }
}
