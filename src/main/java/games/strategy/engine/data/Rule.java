package games.strategy.engine.data;

public class Rule extends NamedAttachable implements NamedUnitHolder, Comparable<Rule> {
  private static final long serialVersionUID = -6390555051736721082L;
  private final boolean m_water;
  private final PlayerID m_owner = PlayerID.NULL_PLAYERID;
  private final UnitCollection m_units;
  // In a grid-based game, stores the coordinate of the Territory
  int[] m_coordinate = null;

  /** Creates new Rule. */
  public Rule(final String name, final boolean water, final GameData data) {
    super(name, data);
    m_water = water;
    m_units = new UnitCollection(this, getData());
  }

  /** Creates new Rule. */
  public Rule(final String name, final boolean water, final GameData data, final int... coordinate) {
    super(name, data);
    m_water = water;
    m_units = new UnitCollection(this, getData());
    if (data.getMap().isCoordinateValid(coordinate)) {
      m_coordinate = coordinate;
    } else {
      throw new IllegalArgumentException("Invalid coordinate: " + coordinate[0] + "," + coordinate[1]);
    }
  }

  public boolean isWater() {
    return m_water;
  }

  /**
   * May be null if not owned.
   */
  public PlayerID getOwner() {
    return m_owner;
  }

  /**
   * Get the units in this territory.
   */
  @Override
  public UnitCollection getUnits() {
    return m_units;
  }

  @Override
  public void notifyChanged() {}

  @Override
  public String toString() {
    return getName();
  }

  @Override
  public int compareTo(final Rule r) {
    return getName().compareTo(r.getName());
  }

  @Override
  public String getType() {
    return UnitHolder.TERRITORY;
  }

  public boolean matchesCoordinates(final int... coordinate) {
    if (coordinate.length != m_coordinate.length) {
      return false;
    } else {
      for (int i = 0; i < coordinate.length; i++) {
        if (coordinate[i] != m_coordinate[i]) {
          return false;
        }
      }
    }
    return true;
  }

  public int getX() {
    try {
      return m_coordinate[0];
    } catch (final ArrayIndexOutOfBoundsException e) {
      throw new RuntimeException("Territory " + this.getName() + " doesn't have a defined x coordinate");
    }
  }

  public int getY() {
    try {
      return m_coordinate[1];
    } catch (final ArrayIndexOutOfBoundsException e) {
      throw new RuntimeException("Territory " + this.getName() + " doesn't have a defined y coordinate");
    }
  }
}
