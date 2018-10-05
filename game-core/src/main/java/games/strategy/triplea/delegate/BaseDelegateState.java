package games.strategy.triplea.delegate;

import java.io.Serializable;

class BaseDelegateState implements Serializable {
  private static final long serialVersionUID = 7130686697155151908L;

  @SuppressWarnings("checkstyle:MemberName") // rename upon next incompatible release
  public boolean m_startBaseStepsFinished = false;
  @SuppressWarnings("checkstyle:MemberName") // rename upon next incompatible release
  public boolean m_endBaseStepsFinished = false;
}
