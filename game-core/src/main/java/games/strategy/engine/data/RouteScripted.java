package games.strategy.engine.data;

import java.util.Collections;
import java.util.List;

/**
 * A scripted or cheating Route, designed for use with Triggers and with units stranded in enemy territory, or other
 * situations where you
 * want the "end" to not be null.
 * If the Route only has a start, it will return the start when you call .end(), and it will return a length of 1 if the
 * length is really
 * zero.
 */
public class RouteScripted extends Route {
  private static final long serialVersionUID = 604474811874966546L;

  public RouteScripted() {}

  /**
   * Shameless cheating. Making a fake route, so as to handle battles properly without breaking battleTracker protected
   * status or
   * duplicating a zillion lines of code.
   * The End will return the Start, and the Length will be 1.
   */
  public RouteScripted(final Territory terr) {
    super(terr);
  }

  public RouteScripted(final Territory start, final Territory... route) {
    super(start, route);
  }

  @Override
  public void add(final Territory t) {
    // maybe we don't check for loops?
    super.add(t);
  }

  @Override
  public int getMovementCost(final Unit u) {
    if (super.getMovementCost(u) <= 0) {
      return 1;
    }
    return super.getMovementCost(u);
  }

  @Override
  public int numberOfSteps() {
    if (super.numberOfSteps() <= 0) {
      return 1;
    }
    return super.numberOfSteps();
  }

  @Override
  public List<Territory> getSteps() {
    if (numberOfSteps() <= 0) {
      return Collections.singletonList(getStart());
    }
    return super.getSteps();
  }

  @Override
  public Territory getEnd() {
    if (super.getEnd() == null) {
      return super.getStart();
    }
    return super.getEnd();
  }

  @Override
  public Territory getTerritoryAtStep(final int i) {
    try {
      if ((super.getEnd() == null) || (super.getTerritoryAtStep(i) == null)) {
        return super.getStart();
      }
    } catch (final ArrayIndexOutOfBoundsException e) {
      return super.getStart();
    }
    return super.getTerritoryAtStep(i);
  }

  @Override
  public boolean hasSteps() {
    return true;
  }

  @Override
  public boolean hasNoSteps() {
    return false;
  }

  @Override
  public boolean hasExactlyOneStep() {
    return numberOfSteps() <= 1;
  }
}
