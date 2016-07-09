package games.strategy.engine.data.changefactory;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameMap;
import games.strategy.engine.data.GameParser;
import games.strategy.engine.data.Territory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

class AddGridGameMapChange extends Change {
  private static final long serialVersionUID = -8326219690181895908L;
  final Map<Territory, Set<Territory>> m_removeTerritoriesAndConnections;
  final Map<Territory, Set<Territory>> m_addTerritoriesAndConnections;
  final String m_gridType;
  final String m_name;
  final int m_xs;
  final int m_ys;
  final Set<String> m_water;
  final Set<String> m_oldWater;
  final String m_horizontalConnections;
  final String m_verticalConnections;
  final String m_diagonalConnections;
  final int m_oldXs;
  final int m_oldYs;

  public AddGridGameMapChange(final GameMap map, final String gridType, final String name, final int xs, final int ys,
      final Set<String> water, final String horizontalConnections, final String verticalConnections,
      final String diagonalConnections) {
    m_oldXs = map.getXDimension();
    m_oldYs = map.getYDimension();
    m_gridType = gridType;
    m_name = name;
    m_xs = xs;
    m_ys = ys;
    m_water = water;
    m_horizontalConnections = horizontalConnections;
    m_verticalConnections = verticalConnections;
    m_diagonalConnections = diagonalConnections;
    m_oldWater = new HashSet<>();
    for (final Territory t : map.getTerritories()) {
      if (t.isWater()) {
        m_oldWater.add(t.getName());
      }
    }
    m_removeTerritoriesAndConnections = new HashMap<>();
    m_addTerritoriesAndConnections = new HashMap<>();
    if (map.getXDimension() > m_xs) {
      // we will be removing territories
      for (final Territory t : map.getTerritories()) {
        String tname = t.getName().replaceFirst(m_name + "_", "");
        tname = tname.substring(0, tname.indexOf("_"));
        final int tx = Integer.parseInt(tname);
        if (tx > m_xs - 1) {
          m_removeTerritoriesAndConnections.put(t, map.getNeighbors(t));
        }
      }
    } else if (map.getXDimension() < m_xs) {
      // adding territories
    }
    if (map.getYDimension() > m_ys) {
      // we will be removing territories
      for (final Territory t : map.getTerritories()) {
        String tname = t.getName().replaceFirst(m_name + "_", "");
        tname = tname.substring(tname.indexOf("_") + 1, tname.length());
        final int ty = Integer.parseInt(tname);
        if (ty > m_ys - 1) {
          m_removeTerritoriesAndConnections.put(t, map.getNeighbors(t));
        }
      }
    } else if (map.getYDimension() < m_ys) {
      // adding territories
    }
  }

  public AddGridGameMapChange(final String gridType, final String name, final int oldXs, final int oldYs,
      final int newXs, final int newYs, final Set<String> oldWater, final Set<String> water,
      final String horizontalConnections, final String verticalConnections, final String diagonalConnections,
      final Map<Territory, Set<Territory>> removeTerritoriesAndConnections,
      final Map<Territory, Set<Territory>> addTerritoriesAndConnections) {
    m_oldXs = oldXs;
    m_oldYs = oldYs;
    m_gridType = gridType;
    m_name = name;
    m_xs = newXs;
    m_ys = newYs;
    m_oldWater = oldWater;
    m_water = water;
    m_horizontalConnections = horizontalConnections;
    m_verticalConnections = verticalConnections;
    m_diagonalConnections = diagonalConnections;
    m_removeTerritoriesAndConnections = removeTerritoriesAndConnections;
    m_addTerritoriesAndConnections = addTerritoriesAndConnections;
  }

  @Override
  protected void perform(final GameData data) {
    if (data.getMap().getXDimension() != m_xs || data.getMap().getYDimension() != m_ys) {
      try {
        final GameMap map = data.getMap();
        for (final Territory t : m_removeTerritoriesAndConnections.keySet()) {
          map.removeTerritory(t);
        }
        GameParser.setGrids(data, m_gridType, m_name, Integer.toString(m_xs), Integer.toString(m_ys), m_water,
            m_horizontalConnections, m_verticalConnections, m_diagonalConnections, true);
        map.notifyChanged();
      } catch (final Exception e) {
        ClientLogger.logQuietly(e);
      }
    }
  }

  @Override
  public Change invert() {
    return new AddGridGameMapChange(m_gridType, m_name, m_xs, m_ys, m_oldXs, m_oldYs, m_water, m_oldWater,
        m_horizontalConnections, m_verticalConnections, m_diagonalConnections, m_addTerritoriesAndConnections,
        m_removeTerritoriesAndConnections);
  }
}
