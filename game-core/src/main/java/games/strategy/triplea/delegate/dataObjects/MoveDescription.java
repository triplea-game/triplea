package games.strategy.triplea.delegate.dataObjects;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;

import games.strategy.engine.data.Route;
import games.strategy.engine.data.Unit;

public class MoveDescription extends AbstractMoveDescription {
  private static final long serialVersionUID = 2199608152808948043L;
  private final Route m_route;
  private final Collection<Unit> m_transportsThatCanBeLoaded;
  private final Map<Unit, Collection<Unit>> m_dependentUnits;

  public MoveDescription(final Collection<Unit> units, final Route route,
      final Collection<Unit> transportsThatCanBeLoaded, final Map<Unit, Collection<Unit>> dependentUnits) {
    super(units);
    m_route = route;
    m_transportsThatCanBeLoaded = transportsThatCanBeLoaded;
    if ((dependentUnits != null) && !dependentUnits.isEmpty()) {
      m_dependentUnits = new HashMap<>();
      for (final Entry<Unit, Collection<Unit>> entry : dependentUnits.entrySet()) {
        m_dependentUnits.put(entry.getKey(), new HashSet<>(entry.getValue()));
      }
    } else {
      m_dependentUnits = null;
    }
  }

  public MoveDescription(final Collection<Unit> units, final Route route) {
    super(units);
    m_route = route;
    m_transportsThatCanBeLoaded = null;
    m_dependentUnits = null;
  }

  public Route getRoute() {
    return m_route;
  }

  @Override
  public String toString() {
    return "Move message route:" + m_route + " units:" + getUnits();
  }

  public Collection<Unit> getTransportsThatCanBeLoaded() {
    if (m_transportsThatCanBeLoaded == null) {
      return Collections.emptyList();
    }
    return m_transportsThatCanBeLoaded;
  }

  public Map<Unit, Collection<Unit>> getDependentUnits() {
    if (m_dependentUnits == null) {
      return new HashMap<>();
    }
    return m_dependentUnits;
  }
}
