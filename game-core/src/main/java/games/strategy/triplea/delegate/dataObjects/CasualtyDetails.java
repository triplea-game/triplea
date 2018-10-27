package games.strategy.triplea.delegate.dataObjects;

import java.util.List;

import games.strategy.engine.data.Unit;

public class CasualtyDetails extends CasualtyList {
  private static final long serialVersionUID = 2261683015991514918L;
  private final boolean autoCalculated;

  /**
   * Creates new SelectCasualtyMessage.
   *
   * @param killed killed units
   * @param damaged damaged units (Can have multiple of the same unit, to show multiple hits to that unit.)
   * @param autoCalculated whether casualties should be selected automatically
   */
  public CasualtyDetails(final List<Unit> killed, final List<Unit> damaged, final boolean autoCalculated) {
    super(killed, damaged);
    this.autoCalculated = autoCalculated;
  }

  public CasualtyDetails(final CasualtyList casualties, final boolean autoCalculated) {
    super((casualties == null ? null : casualties.getKilled()), (casualties == null ? null : casualties.getDamaged()));
    this.autoCalculated = autoCalculated;
  }

  public CasualtyDetails(final boolean autoCalculated) {
    this.autoCalculated = autoCalculated;
  }

  /**
   * Empty details, with autoCalculated as true.
   */
  public CasualtyDetails() {
    autoCalculated = true;
  }

  public boolean getAutoCalculated() {
    return autoCalculated;
  }
}
