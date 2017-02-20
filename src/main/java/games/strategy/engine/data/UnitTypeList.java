package games.strategy.engine.data;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * A collection of unit types
 */
public class UnitTypeList extends GameDataComponent implements Iterable<UnitType> {
  private static final long serialVersionUID = 9002927658524651749L;
  private final Map<String, UnitType> m_unitTypes = new HashMap<>();

  /**
   * Creates new UnitTypeCollection
   *
   * @param data
   *        game data
   */
  public UnitTypeList(final GameData data) {
    super(data);
  }

  protected void addUnitType(final UnitType type) {
    m_unitTypes.put(type.getName(), type);
  }

  public UnitType getUnitType(final String name) {
    return m_unitTypes.get(name);
  }

  /**
   * Will return null if even a single name is not on the unit list.
   *
   * @param names
   */
  public Set<UnitType> getUnitTypes(final String[] names) {
    final Set<UnitType> types = new HashSet<>();
    for (final String name : names) {
      final UnitType type = m_unitTypes.get(name);
      if (type == null) {
        return null;
      }
      types.add(type);
    }
    return types;
  }

  public int size() {
    return m_unitTypes.size();
  }

  @Override
  public Iterator<UnitType> iterator() {
    return m_unitTypes.values().iterator();
  }

  public Set<UnitType> getAllUnitTypes() {
    return new LinkedHashSet<>(m_unitTypes.values());
  }
}
