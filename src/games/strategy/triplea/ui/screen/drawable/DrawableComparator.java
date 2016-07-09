package games.strategy.triplea.ui.screen.drawable;

import java.util.Comparator;

class DrawableComparator implements Comparator<IDrawable> {
  @Override
  public int compare(final IDrawable o1, final IDrawable o2) {
    return o1.getLevel() - o2.getLevel();
  }
}
