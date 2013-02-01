package games.strategy.grid.kingstable.ui;

import games.strategy.engine.data.Territory;
import games.strategy.grid.kingstable.attachments.TerritoryAttachment;
import games.strategy.grid.ui.GridMapData;
import games.strategy.grid.ui.GridMapPanel;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.util.Map;

public class KingsTableMapPanel extends GridMapPanel
{
	private static final long serialVersionUID = 9111624780451084800L;
	
	public KingsTableMapPanel(final GridMapData mapData)
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
		g.setColor(Color.white);
		g.fillRect(50, 50, getWidth() - 100, getHeight() - 100);
		for (final Map.Entry<Territory, Polygon> entry : m_mapData.getPolygons().entrySet())
		{
			final Polygon p = entry.getValue();
			final Territory at = entry.getKey();
			Color backgroundColor = Color.WHITE;
			final TerritoryAttachment ta = (TerritoryAttachment) at.getAttachment("territoryAttachment");
			if (ta != null)
			{
				if (ta.getKingsExit())
					backgroundColor = new Color(225, 225, 255);
				else if (ta.getKingsSquare())
					backgroundColor = new Color(235, 235, 235);
				g.setColor(backgroundColor);
				g.fillPolygon(p);
			}
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
