package games.strategy.triplea.delegate.dataObjects;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import games.strategy.engine.data.Unit;

public class CasualtyList implements Serializable {
  private static final long serialVersionUID = 6501752134047891398L;
  protected List<Unit> m_killed;
  protected List<Unit> m_damaged;

  /**
   * Creates a new CasualtyList
   *
   * @param killed
   * @param damaged
   *        (can have multiple of the same unit, to show multiple hits to that unit)
   */
  public CasualtyList(final List<Unit> killed, final List<Unit> damaged) {
    if (killed == null) {
      throw new IllegalArgumentException("null killed");
    }
    if (damaged == null) {
      throw new IllegalArgumentException("null damaged");
    }
    m_killed = new ArrayList<>(killed);
    m_damaged = new ArrayList<>(damaged);
  }

  /**
   * Creates a new blank CasualtyList with empty lists
   */
  public CasualtyList() {
    m_killed = new ArrayList<>();
    m_damaged = new ArrayList<>();
  }

  /**
   * @return list of killed units
   */
  public List<Unit> getKilled() {
    return m_killed;
  }

  /**
   * Can have multiple of the same unit, to show multiple hits to that unit.
   */
  public List<Unit> getDamaged() {
    return m_damaged;
  }

  /**
   * Can have multiple of the same unit, to show multiple hits to that unit.
   */
  public List<Unit> getKilledAndDamaged() {
    final List<Unit> all = new ArrayList<>(m_killed);
    all.addAll(m_damaged);
    return all;
  }

  public void addToKilled(final Unit deadUnit) {
    m_killed.add(deadUnit);
  }

  public void addToKilled(final Collection<Unit> deadUnits) {
    m_killed.addAll(deadUnits);
  }

  /**
   * Can have multiple of the same unit, to show multiple hits to that unit.
   */
  public void addToDamaged(final Unit damagedUnit) {
    m_damaged.add(damagedUnit);
  }

  /**
   * Can have multiple of the same unit, to show multiple hits to that unit.
   */
  public void addToDamaged(final Collection<Unit> damagedUnits) {
    m_damaged.addAll(damagedUnits);
  }

  public void removeFromKilled(final Unit deadUnit) {
    m_killed.remove(deadUnit);
  }

  public void removeFromKilled(final Collection<Unit> deadUnits) {
    m_killed.removeAll(deadUnits);
  }

  /**
   * Can have multiple of the same unit, to show multiple hits to that unit.
   */
  public void removeOnceFromDamaged(final Unit damagedUnit) {
    m_damaged.remove(damagedUnit);
  }

  /**
   * Can have multiple of the same unit, to show multiple hits to that unit.
   */
  public void removeOnceFromDamaged(final Collection<Unit> damagedUnits) {
    m_damaged.removeAll(damagedUnits);
  }

  /**
   * Can have multiple of the same unit, to show multiple hits to that unit.
   */
  public void removeAllFromDamaged(final Unit damagedUnit) {
    while (m_damaged.contains(damagedUnit)) {
      m_damaged.remove(damagedUnit);
    }
  }

  /**
   * Can have multiple of the same unit, to show multiple hits to that unit.
   */
  public void removeAllFromDamaged(final Collection<Unit> damagedUnits) {
    for (final Unit u : damagedUnits) {
      while (m_damaged.contains(u)) {
        m_damaged.remove(u);
      }
    }
  }

  /**
   * Can have multiple of the same unit, to show multiple hits to that unit.
   */
  public void addAll(final CasualtyList casualtyList) {
    m_damaged.addAll(casualtyList.getDamaged());
    m_killed.addAll(casualtyList.getKilled());
  }

  public void clear() {
    m_killed.clear();
    m_damaged.clear();
  }

  public int size() {
    return m_killed.size() + m_damaged.size();
  }

  @Override
  public String toString() {
    return "Selected Casualties: Damaged: [" + m_damaged + "],  Killed: [" + m_killed + "]";
  }
}
