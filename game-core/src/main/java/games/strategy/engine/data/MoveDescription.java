package games.strategy.engine.data;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import lombok.Getter;

/** Describes an action that moves one or more units along a specific route. */
public class MoveDescription extends AbstractMoveDescription {
  private static final long serialVersionUID = 2199608152808948043L;
  @Getter private final Route route;
  @Getter private final Map<Unit, Unit> unitsToTransports;
  @Getter private final Map<Unit, Collection<Unit>> dependentUnits;

  public MoveDescription(
      final Collection<Unit> units,
      final Route route,
      final Map<Unit, Unit> unitsToTransports,
      final Map<Unit, Collection<Unit>> dependentUnits) {
    super(units);
    this.route = route;
    if (this.route == null) {
      throw new NullPointerException();
    }
    this.unitsToTransports = unitsToTransports;
    if (this.unitsToTransports == null) {
      throw new NullPointerException();
    }
    if (!dependentUnits.isEmpty()) {
      this.dependentUnits = new HashMap<>();
      for (final Entry<Unit, Collection<Unit>> entry : dependentUnits.entrySet()) {
        this.dependentUnits.put(entry.getKey(), new HashSet<>(entry.getValue()));
      }
    } else {
      this.dependentUnits = Map.of();
    }
  }

  public MoveDescription(final Collection<Unit> units, final Route route) {
    this(units, route, Map.of(), Map.of());
  }

  @Override
  public String toString() {
    return "Move message route:" + route + " units:" + getUnits();
  }
}
