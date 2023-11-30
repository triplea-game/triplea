package games.strategy.triplea.delegate.data;

import static com.google.common.base.Preconditions.checkNotNull;

import games.strategy.engine.data.Unit;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import lombok.Getter;

/** A collection of units either killed or damaged during a battle. */
@Getter
public class CasualtyList implements Serializable {
  private static final long serialVersionUID = 6501752134047891398L;

  /** -- GETTER -- Returns the list of killed units. */
  final List<Unit> killed;

  /** -- GETTER -- Can have multiple of the same unit, to show multiple hits to that unit. */
  final List<Unit> damaged;

  /**
   * Creates a new CasualtyList.
   *
   * @param damaged (can have multiple of the same unit, to show multiple hits to that unit)
   */
  public CasualtyList(final Collection<Unit> killed, final Collection<Unit> damaged) {
    checkNotNull(killed);
    checkNotNull(damaged);

    this.killed = new ArrayList<>(killed);
    this.damaged = new ArrayList<>(damaged);
  }

  /** Creates a new blank CasualtyList with empty lists. */
  public CasualtyList() {
    this(new ArrayList<>(), new ArrayList<>());
  }

  public void addToKilled(final Unit deadUnit) {
    killed.add(deadUnit);
  }

  /** Can have multiple of the same unit, to show multiple hits to that unit. */
  public void addToDamaged(final Unit damagedUnit) {
    damaged.add(damagedUnit);
  }

  /** Can have multiple of the same unit, to show multiple hits to that unit. */
  public void addToDamaged(final Collection<Unit> damagedUnits) {
    damaged.addAll(damagedUnits);
  }

  public int size() {
    return killed.size() + damaged.size();
  }

  @Override
  public String toString() {
    return "Selected Casualties: Damaged: [" + damaged + "],  Killed: [" + killed + "]";
  }
}
