/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package games.strategy.grid.ui;

import games.strategy.common.image.UnitImageFactory;
import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.events.GameDataChangeListener;
import games.strategy.engine.gamePlayer.IPlayerBridge;
import games.strategy.triplea.Constants;
import games.strategy.triplea.ResourceLoader;
import games.strategy.ui.ImageScrollModel;
import games.strategy.ui.ImageScrollerLargeView;
import games.strategy.ui.ScrollListener;
import games.strategy.ui.Util;
import games.strategy.util.Tuple;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import javax.swing.SwingUtilities;

/**
 * Custom component for displaying a Grid Game gameboard and pieces.
 * 
 * @author Lane Schwartz (original) and Veqryn (abstraction and major rewrite)
 * @version $LastChangedDate: 2012-04-19 18:13:58 +0800 (Thu, 19 Apr 2012) $
 */
public abstract class GridMapPanel extends ImageScrollerLargeView implements MouseListener
{
	private static final long serialVersionUID = -1318170171400997968L;
	protected final GridMapData m_mapData;
	protected GameData m_gameData;
	protected Territory m_clickedAt = null;
	protected Territory m_releasedAt = null;
	protected final Map<Territory, Image> m_images;
	protected CountDownLatch m_waiting;
	protected final UnitImageFactory m_imageFactory;
	protected BufferedImage m_mouseShadowImage = null;
	protected Tuple<Collection<Territory>, Collection<Territory>> m_validMovesList = null;
	protected Point m_currentMouseLocation = new Point(0, 0);
	protected Territory m_currentMouseLocationTerritory = null;
	protected final GridGameFrame m_parentGridGameFrame;
	protected IGridPlayData m_lastMove = null;
	
	public GridMapPanel(final GameData data, final GridMapData mapData, final GridGameFrame parentGridGameFrame, final ImageScrollModel imageScrollModel)
	{
		super(mapData.getMapDimensions(), imageScrollModel);
		m_waiting = null;
		m_mapData = mapData;
		m_parentGridGameFrame = parentGridGameFrame;
		setGameData(data);
		final String mapName = (String) m_gameData.getProperties().get(Constants.MAP_NAME);
		if (mapName == null || mapName.trim().length() == 0)
		{
			throw new IllegalStateException("Map name property not set on game");
		}
		m_imageFactory = new UnitImageFactory();
		m_imageFactory.setResourceLoader(ResourceLoader.getMapResourceLoader(mapName, true));
		final Dimension mapDimension = m_mapData.getMapDimensions();
		this.setMinimumSize(mapDimension);
		this.setPreferredSize(mapDimension);
		this.setSize(mapDimension);
		m_images = new HashMap<Territory, Image>();
		for (final Territory at : m_gameData.getMap().getTerritories())
		{
			updateImage(at);
		}
		this.addMouseListener(this);
		this.addMouseMotionListener(getMouseMotionListener());
		this.addScrollListener(new ScrollListener()
		{
			public void scrolled(final int x, final int y)
			{
				SwingUtilities.invokeLater(new Runnable()
				{
					public void run()
					{
						repaint();
					}
				});
			}
		});
		this.setOpaque(true);
	}
	
	@Override
	public Dimension getPreferredSize()
	{
		return getImageDimensions();
	}
	
	@Override
	public Dimension getMinimumSize()
	{
		return new Dimension(100, 100);
	}
	
	boolean mapWidthFitsOnScreen()
	{
		return m_model.getMaxWidth() < getScaledWidth();
	}
	
	boolean mapHeightFitsOnScreen()
	{
		return m_model.getMaxHeight() < getScaledHeight();
	}
	
	/**
	 * Update the user interface based on a game play.
	 * 
	 * @param territories
	 *            <code>Collection</code> of <code>Territory</code>s whose pieces have changed
	 */
	protected void refreshTerritories(final Collection<Territory> territories)
	{
		if (territories != null)
		{
			for (final Territory at : territories)
			{
				updateImage(at);
			}
		}
		else
		{
			for (final Territory at : m_gameData.getMap().getTerritories())
			{
				updateImage(at);
			}
		}
		// Ask Swing to repaint this panel when it's convenient
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				repaint();
			}
		});
	}
	
	protected void showGridPlayDataMove(final IGridPlayData move)
	{
		m_lastMove = move;
	}
	
	protected void updateAllImages()
	{
		refreshTerritories(m_gameData.getMap().getTerritories());
	}
	
	/**
	 * Update the image for a <code>Territory</code> based on the contents of that <code>Territory</code>.
	 * 
	 * @param at
	 *            the <code>Territory</code> to update
	 */
	private void updateImage(final Territory at)
	{
		if (at != null && m_images != null)
		{
			if (at.getUnits().size() == 1)
			{
				// Get image for exactly one unit
				final Unit u = (Unit) at.getUnits().getUnits().toArray()[0];
				final Image image = m_imageFactory.getImage(u.getType(), u.getOwner(), m_gameData);
				m_images.put(at, image);
			}
			else
			{
				m_images.remove(at);
			}
		}
	}
	
	public void setGameData(final GameData data)
	{
		if (m_gameData != null)
		{
			m_gameData.removeDataChangeListener(GAME_DATA_CHANGE_LISTENER);
		}
		m_gameData = data;
		m_gameData.addDataChangeListener(GAME_DATA_CHANGE_LISTENER);
		updateAllImages();
	}
	
	protected abstract void paintComponentMiddleLayer(final Graphics2D g2d, final int topLeftX, final int topLeftY);
	
	@Override
	protected void paintComponent(final Graphics g)
	{
		super.paintComponent(g);
		final Graphics2D g2d = (Graphics2D) g;
		g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
		g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		final int topLeftX = m_model.getX();
		final int topLeftY = m_model.getY();
		// translate our g2d so that our image scrolls
		g2d.translate(-topLeftX, -topLeftY);
		paintComponentMiddleLayer(g2d, topLeftX, topLeftY);
		if (m_validMovesList != null)
		{
			g2d.setColor(Color.black);
			g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.6f));
			for (final Territory t : m_validMovesList.getFirst())
			{
				final Polygon p = m_mapData.getPolygon(t);
				final Rectangle rect = p.getBounds();
				g2d.drawLine(rect.x, rect.y, rect.x + rect.width, rect.y + rect.height);
				g2d.drawLine(rect.x, rect.y + rect.height, rect.x + rect.width, rect.y);
			}
			g2d.setColor(Color.red);
			for (final Territory t : m_validMovesList.getSecond())
			{
				final Polygon p = m_mapData.getPolygon(t);
				final Rectangle rect = p.getBounds();
				g2d.drawLine(rect.x, rect.y, rect.x + rect.width, rect.y + rect.height);
				g2d.drawLine(rect.x, rect.y + rect.height, rect.x + rect.width, rect.y);
			}
		}
		if (m_lastMove != null)
		{
			g2d.setColor(Color.gray);
			g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.6f));
			final Rectangle start = m_mapData.getPolygon(m_lastMove.getStart()).getBounds();
			final Rectangle end = m_mapData.getPolygon(m_lastMove.getEnd()).getBounds();
			g2d.drawLine(start.x + (start.width / 2), start.y + (start.height / 2), end.x + (end.width / 2), end.y + (end.height / 2));
		}
		if (m_mouseShadowImage != null)
		{
			final AffineTransform t = new AffineTransform();
			t.translate(m_currentMouseLocation.x - (GridGameFrame.SQUARE_SIZE / 2), m_currentMouseLocation.y - (GridGameFrame.SQUARE_SIZE / 2));
			g2d.drawImage(m_mouseShadowImage, t, this);
		}
	}
	
	public void mouseClicked(final MouseEvent e)
	{
	}
	
	public void mouseEntered(final MouseEvent e)
	{
	}
	
	public void mouseExited(final MouseEvent e)
	{
	}
	
	/**
	 * Process the mouse button being pressed.
	 */
	public void mousePressed(final MouseEvent e)
	{
		// After this method has been called,
		// the Territory corresponding to the cursor location when the mouse was pressed
		// will be stored in the private member variable m_clickedAt.
		m_clickedAt = m_mapData.getTerritoryAt(e.getX() + m_model.getX(), e.getY() + m_model.getY(), m_gameData.getMap());
		// TODO: only shadow units if owned by current player
		if (m_clickedAt != null)
		{
			setMouseShadowUnits(m_clickedAt.getUnits().getUnits());
			m_validMovesList = getValidMovesList(m_clickedAt);
		}
	}
	
	/**
	 * Process the mouse button being released.
	 */
	public void mouseReleased(final MouseEvent e)
	{
		// After this method has been called,
		// the Territory corresponding to the cursor location when the mouse was released
		// will be stored in the private member variable m_releasedAt.
		m_releasedAt = m_mapData.getTerritoryAt(e.getX() + m_model.getX(), e.getY() + m_model.getY(), m_gameData.getMap());
		setMouseShadowUnits(null);
		m_validMovesList = null;
		// The waitForPlay method is waiting for mouse input.
		// Let it know that we have processed mouse input.
		if (m_waiting != null)
			m_waiting.countDown();
	}
	
	protected MouseMotionListener getMouseMotionListener()
	{
		return new MouseMotionAdapter()
		{
			@Override
			public void mouseMoved(final MouseEvent e)
			{
				m_currentMouseLocation = new Point(e.getX() + m_model.getX(), e.getY() + m_model.getY());
				m_currentMouseLocationTerritory = m_mapData.getTerritoryAt(e.getX() + m_model.getX(), e.getY() + m_model.getY(), m_gameData.getMap());
				SwingUtilities.invokeLater(new Runnable()
				{
					public void run()
					{
						repaint();
					}
				});
			}
			
			@Override
			public void mouseDragged(final MouseEvent e)
			{
				mouseMoved(e);
			}
		};
	}
	
	protected final GameDataChangeListener GAME_DATA_CHANGE_LISTENER = new GameDataChangeListener()
	{
		public void gameDataChanged(final Change aChange)
		{
			updateAllImages();
		}
	};
	
	public void setMouseShadowUnits(final Collection<Unit> units)
	{
		if (units == null || units.isEmpty())
		{
			m_mouseShadowImage = null;
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					repaint();
				}
			});
			return;
		}
		final int icon_width = GridGameFrame.SQUARE_SIZE;
		final int icon_height = GridGameFrame.SQUARE_SIZE;
		final int xSpace = GridGameFrame.SQUARE_SIZE / 5;
		final BufferedImage img = Util.createImage(units.size() * (xSpace + icon_width), icon_height, true);
		final Graphics2D g = (Graphics2D) img.getGraphics();
		g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.6f));
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		final Rectangle bounds = new Rectangle(0, 0, 0, 0);
		int i = 0;
		for (final Unit unit : units)
		{
			final Point place = new Point(i * (icon_width + xSpace), 0);
			final Image unitImage = m_imageFactory.getImage(unit.getType(), unit.getOwner(), m_gameData);
			g.drawImage(unitImage, place.x - bounds.x, place.y - bounds.y, null);
			i++;
		}
		m_mouseShadowImage = img;
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				repaint();
			}
		});
		g.dispose();
	}
	
	/**
	 * Wait for a player to play.
	 * 
	 * @param player
	 *            the player to wait on
	 * @param bridge
	 *            the bridge for player
	 * @param waiting
	 *            a <code>CountDownLatch</code> used to wait for user input - must be non-null and have and have <code>getCount()==1</code>
	 * @return PlayData representing a play, or <code>null</code> if the play started and stopped on the same <code>Territory</code>
	 * @throws InterruptedException
	 *             if the play was interrupted
	 */
	public GridPlayData waitForPlay(final PlayerID player, final IPlayerBridge bridge, final CountDownLatch waiting) throws InterruptedException
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
			return null;
		}
		else
		{
			// We have a valid play!
			// Reset the member variables, and return the play.
			final GridPlayData play = new GridPlayData(m_clickedAt, m_releasedAt);
			m_clickedAt = null;
			m_releasedAt = null;
			// check first if a valid move
			final String error = isValidPlay(play);
			if (error == null)
				return play;
			// if error, notify error, and return null
			m_parentGridGameFrame.notifyError(error);
			return null;
		}
	}
	
	protected abstract String isValidPlay(final IGridPlayData play);
	
	protected abstract Collection<Territory> getCapturesForPlay(final IGridPlayData play);
	
	protected Tuple<Collection<Territory>, Collection<Territory>> getValidMovesList(final Territory clickedOn)
	{
		if (clickedOn == null)
			return null;
		final Collection<Territory> validMovesList = new HashSet<Territory>();
		final Collection<Territory> capturesForValidMoves = new HashSet<Territory>();
		for (final Territory t : m_gameData.getMap().getTerritories())
		{
			final GridPlayData play = new GridPlayData(clickedOn, t);
			if (isValidPlay(play) == null)
			{
				validMovesList.add(t);
				capturesForValidMoves.addAll(getCapturesForPlay(play));
			}
		}
		return new Tuple<Collection<Territory>, Collection<Territory>>(validMovesList, capturesForValidMoves);
	}
	
	public UnitImageFactory getUnitImageFactory()
	{
		return m_imageFactory;
	}
	
	@Override
	public void setTopLeft(final int x, final int y)
	{
		m_model.set(x, y);
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				repaint();
			}
		});
	}
}
