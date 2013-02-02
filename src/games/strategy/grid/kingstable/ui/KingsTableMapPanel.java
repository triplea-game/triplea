package games.strategy.grid.kingstable.ui;

import games.strategy.engine.data.Territory;
import games.strategy.grid.kingstable.attachments.TerritoryAttachment;
import games.strategy.grid.kingstable.delegate.PlayDelegate;
import games.strategy.grid.ui.GridGameFrame;
import games.strategy.grid.ui.GridMapData;
import games.strategy.grid.ui.GridMapPanel;
import games.strategy.grid.ui.GridPlayData;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.util.Map;

public class KingsTableMapPanel extends GridMapPanel
{
	private static final long serialVersionUID = 9111624780451084800L;
	
	public KingsTableMapPanel(final GridMapData mapData, final GridGameFrame parentGridGameFrame)
	{
		super(mapData, parentGridGameFrame);
	}
	
	@Override
	protected String isValidPlay(final GridPlayData play)
	{
		return PlayDelegate.isValidPlay(play.getStart(), play.getEnd(), m_parentGridGameFrame.getActivePlayer(), m_gameData);
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
		g.fillRect(GridGameFrame.OUTSIDE_BEVEL_SIZE, GridGameFrame.OUTSIDE_BEVEL_SIZE, getWidth() - (GridGameFrame.OUTSIDE_BEVEL_SIZE * 2), getHeight() - (GridGameFrame.OUTSIDE_BEVEL_SIZE * 2));
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
					g.drawImage(image, square.x - (GridGameFrame.SQUARE_SIZE / 5), square.y - (GridGameFrame.SQUARE_SIZE / 5),
								square.width + (2 * GridGameFrame.SQUARE_SIZE / 5), square.height + (2 * GridGameFrame.SQUARE_SIZE / 5), null, null);
				else
					g.drawImage(image, square.x, square.y, square.width, square.height, null, null);
			}
			g.drawPolygon(p);
		}
	}
	
}
