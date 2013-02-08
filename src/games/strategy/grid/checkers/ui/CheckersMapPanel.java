package games.strategy.grid.checkers.ui;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.gamePlayer.IPlayerBridge;
import games.strategy.grid.checkers.delegate.PlayDelegate;
import games.strategy.grid.ui.GridGameFrame;
import games.strategy.grid.ui.GridMapData;
import games.strategy.grid.ui.GridMapPanel;
import games.strategy.grid.ui.GridPlayData;
import games.strategy.grid.ui.IGridPlayData;
import games.strategy.ui.ImageScrollModel;
import games.strategy.util.Tuple;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * 
 * @author veqryn
 * 
 */
public class CheckersMapPanel extends GridMapPanel
{
	private static final long serialVersionUID = 1086068265270157799L;
	protected List<Territory> m_middleClickedTerritories = null;
	
	public CheckersMapPanel(final GameData data, final GridMapData mapData, final GridGameFrame parentGridGameFrame, final ImageScrollModel imageScrollModel)
	{
		super(data, mapData, parentGridGameFrame, imageScrollModel);
	}
	
	@Override
	protected String isValidPlay(final IGridPlayData play)
	{
		return PlayDelegate.isValidPlayOverall(play, m_parentGridGameFrame.getActivePlayer(), m_gameData);
	}
	
	@Override
	protected Collection<Territory> getCapturesForPlay(final IGridPlayData play)
	{
		return PlayDelegate.checkForCaptures(play, m_parentGridGameFrame.getActivePlayer(), m_gameData);
	}
	
	@Override
	protected Tuple<Collection<IGridPlayData>, Collection<Territory>> getValidMovesList(final Territory clickedOn, final PlayerID player)
	{
		if (clickedOn == null)
			return null;
		final Collection<IGridPlayData> validMovesList = new HashSet<IGridPlayData>();
		final Collection<Territory> capturesForValidMoves = new HashSet<Territory>();
		for (final GridPlayData play : PlayDelegate.getAllValidMovesFromHere(clickedOn, player, m_gameData))
		{
			if (PlayDelegate.isValidPlayOverall(play, player, m_gameData) == null)
			{
				validMovesList.add(play);
				capturesForValidMoves.addAll(getCapturesForPlay(play));
			}
		}
		return new Tuple<Collection<IGridPlayData>, Collection<Territory>>(validMovesList, capturesForValidMoves);
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
		// paint middle steps
		{
			g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.6f));
			if (m_middleClickedTerritories != null)
			{
				for (final Territory t : m_middleClickedTerritories)
				{
					final Polygon p = m_mapData.getPolygon(t);
					final Rectangle rect = p.getBounds();
					g2d.drawImage(m_mouseShadowImage, rect.x, rect.y, rect.width, rect.height, null, null);
				}
			}
			if (m_releasedAt != null)
			{
				final Polygon p = m_mapData.getPolygon(m_releasedAt);
				final Rectangle rect = p.getBounds();
				g2d.drawImage(m_mouseShadowImage, rect.x, rect.y, rect.width, rect.height, null, null);
			}
			g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
		}
	}
	
	@Override
	public void mousePressed(final MouseEvent e)
	{
		final Territory at = m_mapData.getTerritoryAt(e.getX() + m_model.getX(), e.getY() + m_model.getY(), m_gameData.getMap());
		if (at != null)
		{
			if (m_clickedAt == null)
			{
				m_clickedAt = at;
			}
			else
			{
				if (m_releasedAt == null)
					m_releasedAt = at;
				else if (!at.equals(m_releasedAt))
				{
					if (m_middleClickedTerritories == null)
						m_middleClickedTerritories = new ArrayList<Territory>();
					m_middleClickedTerritories.add(m_releasedAt);
					m_releasedAt = at;
				}
			}
			if (m_validMovesList == null)
			{
				setMouseShadowUnits(m_clickedAt.getUnits().getUnits());
				m_validMovesList = getValidMovesList(m_clickedAt, m_parentGridGameFrame.getActivePlayer());
			}
		}
	}
	
	@Override
	public void mouseReleased(final MouseEvent e)
	{
		final Territory at = m_mapData.getTerritoryAt(e.getX() + m_model.getX(), e.getY() + m_model.getY(), m_gameData.getMap());
		if (at != null)
		{
			if (!at.equals(m_clickedAt) && !at.equals(m_releasedAt))
			{
				// they must be clicking and dragging, so treat this as if it was a new click
				mousePressed(e);
			}
		}
		// we right click to create middle steps, so only countDown if it is a normal click
		if (!(e.isControlDown() || e.isAltDown() || e.isShiftDown()) && e.getButton() == MouseEvent.BUTTON1 && (m_clickedAt != null && m_releasedAt != null))
		{
			setMouseShadowUnits(null);
			m_validMovesList = null;
			if (m_waiting != null)
			{
				m_waiting.countDown();
			}
		}
	}
	
	@Override
	public IGridPlayData waitForPlay(final PlayerID player, final IPlayerBridge bridge, final CountDownLatch waiting) throws InterruptedException
	{
		// Make sure we have a valid CountDownLatch.
		if (waiting == null || waiting.getCount() != 1)
			throw new IllegalArgumentException("CountDownLatch must be non-null and have getCount()==1");
		// The mouse listeners need access to the CountDownLatch, so store as a member variable.
		m_waiting = waiting;
		// Wait for a play or an attempt to leave the game
		m_waiting.await();
		if (m_clickedAt == null || m_releasedAt == null)
		{
			// If either m_clickedAt==null or m_releasedAt==null,
			// the play is invalid and must have been interrupted.
			// So, reset the member variables, and throw an exception.
			m_clickedAt = null;
			m_releasedAt = null;
			m_middleClickedTerritories = null;
			throw new InterruptedException("Interrupted while waiting for play.");
		}
		else if (m_clickedAt == m_releasedAt)
		{
			// If m_clickedAt==m_releasedAt,
			// the play started and stopped on the same Territory.
			// This is a blatantly invalid play, but we can't reset the CountDownLatch here,
			// so reset the member variables and return null.
			m_clickedAt = null;
			m_releasedAt = null;
			m_middleClickedTerritories = null;
			return null;
		}
		else
		{
			// We have a valid play!
			// Reset the member variables, and return the play.
			final IGridPlayData play = new GridPlayData(m_clickedAt, m_middleClickedTerritories, m_releasedAt, player);
			m_clickedAt = null;
			m_releasedAt = null;
			m_middleClickedTerritories = null;
			// check first if a valid move
			final String error = isValidPlay(play);
			if (error == null)
				return play;
			// if error, notify error, and return null
			m_parentGridGameFrame.notifyError(error);
			return null;
		}
	}
}
