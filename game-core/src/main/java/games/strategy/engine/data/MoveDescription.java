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
    super(Collections.unmodifiableCollection(units));
    this.route = Preconditions.checkNotNull(route);
    this.unitsToTransports = Collections.unmodifiableMap(unitsToTransports);
    this.dependentUnits =
        Collections.unmodifiableMap(
            dependentUnits.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> Set.copyOf(e.getValue()))));
  }

  public MoveDescription(
      final Collection<Unit> units, final Route route, final Map<Unit, Unit> unitsToTransports) {
    this(units, route, unitsToTransports, Map.of());
  }

  public MoveDescription(final Collection<Unit> units, final Route route) {
    this(units, route, Map.of(), Map.of());
  }

  @Override
  public final boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    } else if (!(obj instanceof MoveDescription)) {
      return false;
    }

    final MoveDescription other = (MoveDescription) obj;
    return getClass().equals(other.getClass())
        && route.equals(other.route)
        && Maps.difference(unitsToTransports, other.unitsToTransports).areEqual()
        && Maps.difference(dependentUnits, other.dependentUnits).areEqual()
        && collectionsAreEqual(getUnits(), other.getUnits());
  }

  @Override
  public final int hashCode() {
    return Objects.hash(
        HashMultiset.create(getUnits()), route, new HashMap<>(unitsToTransports), dependentUnits);
  }

  @SuppressWarnings("UndefinedEquals")
  private static boolean collectionsAreEqual(final Collection<Unit> a, final Collection<Unit> b) {
    // https://stackoverflow.com/questions/1565214/is-there-a-way-to-check-if-two-collections-contain-the-same-elements-independen
    return HashMultiset.create(a).equals(HashMultiset.create(b));
  }

  @Override
  public String toString() {
    return "Move message route:" + route + " units:" + getUnits();
  }
}
