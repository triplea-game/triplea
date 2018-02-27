package games.strategy.triplea.ui.screen.drawable;

import java.io.Serializable;
import java.util.Comparator;

public class DrawableComparator implements Comparator<IDrawable>, Serializable {
  private static final long serialVersionUID = 3242168173112031334L;

  @Override
  public int compare(final IDrawable o1, final IDrawable o2) {
    return o1.getLevel() - o2.getLevel();
  }
}
