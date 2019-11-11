package games.strategy.triplea.delegate.data;

import games.strategy.engine.data.Route;
import games.strategy.engine.data.Unit;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;

/** Describes an action that moves one or more units along a specific route. */
public class MoveDescription extends AbstractMoveDescription {
  private static final long serialVersionUID = 2199608152808948043L;
  private final Route route;
  private final Map<Unit, Unit> unitsToTransports;
  private final Map<Unit, Collection<Unit>> dependentUnits;

  public MoveDescription(
      final Collection<Unit> units,
      final Route route,
      final Map<Unit, Unit> unitsToTransports,
      final Map<Unit, Collection<Unit>> dependentUnits) {
    super(units);
    this.route = route;
    this.unitsToTransports = unitsToTransports;
    if (dependentUnits != null && !dependentUnits.isEmpty()) {
      this.dependentUnits = new HashMap<>();
      for (final Entry<Unit, Collection<Unit>> entry : dependentUnits.entrySet()) {
        this.dependentUnits.put(entry.getKey(), new HashSet<>(entry.getValue()));
      }
    } else {
      this.dependentUnits = null;
    }
  }

  public MoveDescription(final Collection<Unit> units, final Route route) {
    this(units, route, null, null);
  }

  public Route getRoute() {
    return route;
  }

  @Override
  public String toString() {
    return "Move message route:" + route + " units:" + getUnits();
  }

  public Map<Unit, Unit> getUnitsToTransports() {
    if (unitsToTransports == null) {
      return Map.of();
    }
    return unitsToTransports;
  }

  public Map<Unit, Collection<Unit>> getDependentUnits() {
    if (dependentUnits == null) {
      return new HashMap<>();
    }
    return dependentUnits;
  }
}
