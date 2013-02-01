package games.strategy.grid.chess.ui;

import games.strategy.engine.data.Territory;
import games.strategy.grid.ui.GridMapData;
import games.strategy.grid.ui.GridMapPanel;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.util.Map;

public class ChessMapPanel extends GridMapPanel
{
	private static final long serialVersionUID = -8631830615396608727L;
	
	public ChessMapPanel(final GridMapData mapData)
	{
		super(mapData);
	}
	
	/**
	 * Draw the current map and pieces.
	 */
	@Override
	protected void paintComponentMiddleLayer(final Graphics2D g)
	{
		g.setColor(Color.lightGray);
		g.fillRect(0, 0, getWidth(), getHeight());
		final Color tileOdd = new Color(255, 206, 158);
		final Color tileEven = new Color(209, 139, 71);
		for (final Map.Entry<Territory, Polygon> entry : m_mapData.getPolygons().entrySet())
		{
			final Polygon p = entry.getValue();
			final Territory at = entry.getKey();
			final Color backgroundColor;
			if ((at.getX() + at.getY()) % 2 == 0)
				backgroundColor = tileEven;
			else
				backgroundColor = tileOdd;
			g.setColor(backgroundColor);
			g.fillPolygon(p);
			
			g.setColor(Color.black);
			final Image image = m_images.get(at);
			if (image != null)
			{
				final Rectangle square = p.getBounds();
				if (at.equals(m_clickedAt))
					g.drawImage(image, square.x - 10, square.y - 10, square.width + 20, square.height + 20, null, null);
				else
					g.drawImage(image, square.x, square.y, square.width, square.height, null, null);
			}
			g.drawPolygon(p);
		}
	}
}
