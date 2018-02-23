package games.strategy.triplea.delegate.dataObjects;

import java.util.List;

import games.strategy.engine.data.Unit;

public class CasualtyDetails extends CasualtyList {
  private static final long serialVersionUID = 2261683015991514918L;
  private final boolean m_autoCalculated;

  /**
   * Creates new SelectCasualtyMessage
   *
   * @param killed
   *        killed units
   * @param damaged
   *        damaged units (Can have multiple of the same unit, to show multiple hits to that unit.)
   * @param autoCalculated
   *        whether casualties should be selected automatically
   */
  public CasualtyDetails(final List<Unit> killed, final List<Unit> damaged, final boolean autoCalculated) {
    super(killed, damaged);
    m_autoCalculated = autoCalculated;
  }

  public CasualtyDetails(final CasualtyList casualties, final boolean autoCalculated) {
    super(((casualties == null) ? null : casualties.getKilled()),
        ((casualties == null) ? null : casualties.getDamaged()));
    m_autoCalculated = autoCalculated;
  }

  public CasualtyDetails(final boolean autoCalculated) {
    super();
    m_autoCalculated = autoCalculated;
  }

  /**
   * Empty details, with autoCalculated as true.
   */
  public CasualtyDetails() {
    super();
    m_autoCalculated = true;
  }

  public boolean getAutoCalculated() {
    return m_autoCalculated;
  }
}
