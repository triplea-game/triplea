package games.strategy.grid.kingstable.ui;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Territory;
import games.strategy.grid.kingstable.attachments.TerritoryAttachment;
import games.strategy.grid.kingstable.delegate.PlayDelegate;
import games.strategy.grid.ui.GridGameFrame;
import games.strategy.grid.ui.GridMapData;
import games.strategy.grid.ui.GridMapPanel;
import games.strategy.grid.ui.IGridPlayData;
import games.strategy.ui.ImageScrollModel;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.util.Collection;
import java.util.Map;

/**
 * 
 * @author Lane Schwartz (original) and Veqryn (abstraction)
 * 
 */
public class KingsTableMapPanel extends GridMapPanel
{
	private static final long serialVersionUID = 9111624780451084800L;
	
	public KingsTableMapPanel(final GameData data, final GridMapData mapData, final GridGameFrame parentGridGameFrame, final ImageScrollModel imageScrollModel)
	{
		super(data, mapData, parentGridGameFrame, imageScrollModel);
	}
	
	@Override
	protected String isValidPlay(final IGridPlayData play)
	{
		return PlayDelegate.isValidPlay(play.getStart(), play.getEnd(), m_parentGridGameFrame.getActivePlayer(), m_gameData);
	}
	
	@Override
	protected Collection<Territory> getCapturesForPlay(final IGridPlayData play)
	{
		return PlayDelegate.checkForCaptures(play.getEnd(), m_parentGridGameFrame.getActivePlayer(), m_gameData);
	}
	
	/**
	 * Draw the current map and pieces.
	 */
	@Override
	protected void paintComponentMiddleLayer(final Graphics2D g2d, final int topLeftX, final int topLeftY)
	{
		g2d.setColor(Color.lightGray);
		// g2d.fillRect(0, 0, getWidth(), getHeight());
		g2d.fillRect(0, 0, m_model.getMaxWidth(), m_model.getMaxHeight());
		g2d.setColor(Color.white);
		g2d.fillRect(m_mapData.getBevelWidth(), m_mapData.getBevelHeight(), m_model.getMaxWidth() - (m_mapData.getBevelWidth() * 2),
					m_model.getMaxHeight() - (m_mapData.getBevelHeight() * 2));
		for (final Map.Entry<Territory, Polygon> entry : m_mapData.getTerritoryPolygons(m_gameData.getMap()).entrySet())
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
				g2d.setColor(backgroundColor);
				g2d.fillPolygon(p);
			}
			g2d.setColor(Color.black);
			final Image image = m_images.get(at);
			if (image != null)
			{
				final Rectangle square = p.getBounds();
				if (at.equals(m_clickedAt))
					g2d.drawImage(image, square.x - (m_mapData.getSquareWidth() / 5), square.y - (m_mapData.getSquareHeight() / 5),
								square.width + (2 * m_mapData.getSquareWidth() / 5), square.height + (2 * m_mapData.getSquareHeight() / 5), null, null);
				else
					g2d.drawImage(image, square.x, square.y, square.width, square.height, null, null);
			}
			g2d.drawPolygon(p);
		}
	}
	
}
