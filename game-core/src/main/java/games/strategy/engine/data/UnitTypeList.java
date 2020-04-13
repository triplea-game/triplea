package games.strategy.engine.data;

import com.google.common.annotations.VisibleForTesting;
import games.strategy.triplea.attachments.UnitSupportAttachment;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

/** A collection of unit types. */
public class UnitTypeList extends GameDataComponent implements Iterable<UnitType> {
  private static final long serialVersionUID = 9002927658524651749L;

  private final Map<String, UnitType> unitTypes = new LinkedHashMap<>();
  private @Nullable Set<UnitSupportAttachment> supportRules;

  public UnitTypeList(final GameData data) {
    super(data);
  }

  @VisibleForTesting
  public void addUnitType(final UnitType type) {
    unitTypes.put(type.getName(), type);
  }

  public UnitType getUnitType(final String name) {
    return unitTypes.get(name);
  }

  /** Will return null if even a single name is not on the unit list. */
  public Set<UnitType> getUnitTypes(final String[] names) {
    final Set<UnitType> types = new HashSet<>();
    for (final String name : names) {
      final UnitType type = unitTypes.get(name);
      if (type == null) {
        return null;
      }
      types.add(type);
    }
    return types;
  }

  /**
   * Returns the unit support rules for the unit types. Computed once and cached afterwards.
   *
   * @return The unit support rules.
   */
  public Set<UnitSupportAttachment> getSupportRules() {
    if (supportRules == null) {
      supportRules =
          UnitSupportAttachment.get(getData())
              .parallelStream()
              .filter(usa -> (usa.getRoll() || usa.getStrength()))
              .collect(Collectors.toSet());
    }
    return supportRules;
  }

  public int size() {
    return unitTypes.size();
  }

  @Override
  public Iterator<UnitType> iterator() {
    return unitTypes.values().iterator();
  }

  public Set<UnitType> getAllUnitTypes() {
    return new LinkedHashSet<>(unitTypes.values());
  }
}
