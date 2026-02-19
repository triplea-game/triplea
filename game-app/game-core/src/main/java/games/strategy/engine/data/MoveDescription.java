package games.strategy.engine.data;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Maps;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;

/** Describes an action that moves one or more units along a specific route. */
@Getter
public class MoveDescription extends AbstractMoveDescription {
  private static final long serialVersionUID = 2199608152808948043L;
  private final Route route;
  // Maps units to the sea transports that are carrying them.
  private final Map<Unit, Unit> unitsToSeaTransports;
  // Maps air transports to units they're transporting on this move.
  private final Map<Unit, Collection<Unit>> airTransportsDependents;

  public MoveDescription(
      final Collection<Unit> units,
      final Route route,
      final Map<Unit, Unit> unitsToSeaTransports,
      final Map<Unit, Collection<Unit>> airTransportsDependents) {
    super(Collections.unmodifiableCollection(units));
    this.route = Preconditions.checkNotNull(route);
    Preconditions.checkArgument(route.hasSteps());
    this.unitsToSeaTransports = Collections.unmodifiableMap(unitsToSeaTransports);
    this.airTransportsDependents =
        Collections.unmodifiableMap(
            airTransportsDependents.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> Set.copyOf(e.getValue()))));
  }

  public MoveDescription(
      final Collection<Unit> units, final Route route, final Map<Unit, Unit> unitsToSeaTransports) {
    this(units, route, unitsToSeaTransports, Map.of());
  }

  public MoveDescription(final Collection<Unit> units, final Route route) {
    this(units, route, Map.of(), Map.of());
  }

  @Override
  public final boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    } else if (obj instanceof MoveDescription other) {
      return getClass().equals(other.getClass())
          && route.equals(other.route)
          && Maps.difference(unitsToSeaTransports, other.unitsToSeaTransports).areEqual()
          && Maps.difference(airTransportsDependents, other.airTransportsDependents).areEqual()
          && collectionsAreEqual(getUnits(), other.getUnits());
    }
    return false;
  }

  @Override
  public final int hashCode() {
    return Objects.hash(
        HashMultiset.create(getUnits()),
        route,
        new HashMap<>(unitsToSeaTransports),
        airTransportsDependents);
  }

  private static boolean collectionsAreEqual(final Collection<Unit> a, final Collection<Unit> b) {
    // https://stackoverflow.com/questions/1565214/is-there-a-way-to-check-if-two-collections-contain-the-same-elements-independen
    return HashMultiset.create(a).equals(HashMultiset.create(b));
  }

  @Override
  public String toString() {
    return "Move message route: " + route + " units: " + getUnits();
  }
}
