package games.strategy.triplea.delegate;

import java.io.Serializable;

class BaseDelegateState implements Serializable {
  private static final long serialVersionUID = 7130686697155151908L;

  public boolean startBaseStepsFinished = false;
  public boolean endBaseStepsFinished = false;
}
