package games.puzzle.slidingtiles.ui;

import java.awt.Rectangle;
import java.util.HashMap;
import java.util.Map;

import games.puzzle.slidingtiles.attachments.Tile;
import games.strategy.engine.data.GameMap;
import games.strategy.engine.data.Territory;
import games.strategy.grid.ui.GridMapData;

public class NPuzzleMapData extends GridMapData {
  protected Map<Integer, Rectangle> m_rects;

  public NPuzzleMapData(final GameMap map, final int x_dim, final int y_dim, final int squareWidth,
      final int squareHeight, final int topLeftOffsetWidth, final int topLeftOffsetHeight) {
    super(map, x_dim, y_dim, squareWidth, squareHeight, topLeftOffsetWidth, topLeftOffsetHeight);
  }

  @Override
  public synchronized void initializeGridMapData(final GameMap map) {
    m_rects = new HashMap<Integer, Rectangle>();
    for (final Territory territory : map.getTerritories()) {
      final Tile tile = (Tile) territory.getAttachment("tile");
      if (tile != null) {
        final int value = tile.getValue();
        if (value != 0) {
          final int tileX = value % m_gridWidth;
          final int tileY = value / m_gridWidth;
          final Rectangle rectangle =
              new Rectangle(tileX * m_squareWidth, tileY * m_squareHeight, m_squareWidth, m_squareHeight);
          m_rects.put(value, rectangle);
        }
      }
    }
  }

  public synchronized Rectangle getLocation(final int value) {
    return m_rects.get(value);
  }
}
