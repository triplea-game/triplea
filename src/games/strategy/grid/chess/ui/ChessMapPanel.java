package games.strategy.grid.chess.ui;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Territory;
import games.strategy.grid.chess.delegate.PlayDelegate;
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
 * @author veqryn
 * 
 */
public class ChessMapPanel extends GridMapPanel
{
	private static final long serialVersionUID = -8631830615396608727L;
	
	public ChessMapPanel(final GameData data, final GridMapData mapData, final GridGameFrame parentGridGameFrame, final ImageScrollModel imageScrollModel)
	{
		super(data, mapData, parentGridGameFrame, imageScrollModel);
	}
	
	@Override
	protected String isValidPlay(final IGridPlayData play)
	{
		return PlayDelegate.isValidPlay(play.getStart(), play.getEnd(), m_parentGridGameFrame.getActivePlayer(), m_gameData, 2);
	}
	
	@Override
	protected Collection<Territory> getCapturesForPlay(final IGridPlayData play)
	{
		return PlayDelegate.checkForCaptures(play.getStart(), play.getEnd(), m_parentGridGameFrame.getActivePlayer(), m_gameData);
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
		final Color tileOdd = new Color(255, 206, 158);
		final Color tileEven = new Color(209, 139, 71);
		for (final Map.Entry<Territory, Polygon> entry : m_mapData.getTerritoryPolygons(m_gameData.getMap()).entrySet())
		{
			final Polygon p = entry.getValue();
			final Territory at = entry.getKey();
			final Color backgroundColor;
			if ((at.getX() + at.getY()) % 2 == 0)
				backgroundColor = tileEven;
			else
				backgroundColor = tileOdd;
			g2d.setColor(backgroundColor);
			g2d.fillPolygon(p);
			
			g2d.setColor(Color.black);
			final Image image = m_images.get(at);
			if (image != null)
			{
				final Rectangle square = p.getBounds();
				if (at.equals(m_clickedAt))
					g2d.drawImage(image, square.x - (GridGameFrame.SQUARE_SIZE / 5), square.y - (GridGameFrame.SQUARE_SIZE / 5),
								square.width + (2 * GridGameFrame.SQUARE_SIZE / 5), square.height + (2 * GridGameFrame.SQUARE_SIZE / 5), null, null);
				else
					g2d.drawImage(image, square.x, square.y, square.width, square.height, null, null);
			}
			g2d.drawPolygon(p);
		}
	}
}
