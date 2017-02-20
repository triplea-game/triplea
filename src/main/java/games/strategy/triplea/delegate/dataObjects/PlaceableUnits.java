package games.strategy.triplea.delegate.dataObjects;

import java.util.Collection;

import games.strategy.engine.data.Unit;

public class PlaceableUnits implements java.io.Serializable {
  private static final long serialVersionUID = 6572719978603199091L;
  private String m_errorMessage;
  private Collection<Unit> m_units;
  private int m_maxUnits;

  /**
   * Creates new ProductionResponseMessage
   *
   * @param errorMessage
   *        error message
   */
  public PlaceableUnits(final String errorMessage) {
    m_errorMessage = errorMessage;
  }

  public PlaceableUnits(final Collection<Unit> units, final int maxUnits) {
    m_units = units;
    m_maxUnits = maxUnits;
  }

  public Collection<Unit> getUnits() {
    return m_units;
  }

  /**
   * @return -1 if no limit
   */
  public int getMaxUnits() {
    return m_maxUnits;
  }

  public String getErrorMessage() {
    return m_errorMessage;
  }

  public boolean isError() {
    return m_errorMessage != null;
  }

  @Override
  public String toString() {
    return "ProductionResponseMessage units:" + m_units;
  }
}
