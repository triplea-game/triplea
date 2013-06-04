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

import games.strategy.common.delegate.BaseEditDelegate;
import games.strategy.common.image.UnitImageFactory;
import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameStep;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.events.GameDataChangeListener;
import games.strategy.engine.data.events.GameMapListener;
import games.strategy.engine.gamePlayer.IPlayerBridge;
import games.strategy.engine.pbem.ForumPosterComponent;
import games.strategy.engine.pbem.PBEMMessagePoster;
import games.strategy.triplea.Constants;
import games.strategy.triplea.ResourceLoader;
import games.strategy.triplea.delegate.remote.IAbstractForumPosterDelegate;
import games.strategy.triplea.ui.MapSelectionListener;
import games.strategy.triplea.ui.MouseDetails;
import games.strategy.ui.ImageScrollModel;
import games.strategy.ui.ImageScrollerLargeView;
import games.strategy.ui.ScrollListener;
import games.strategy.ui.Util;
import games.strategy.util.CountDownLatchHandler;
import games.strategy.util.ListenerList;
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
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

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
	protected Tuple<Collection<IGridPlayData>, Collection<Territory>> m_validMovesList = null;
	protected Point m_currentMouseLocation = new Point(0, 0);
	protected Territory m_currentMouseLocationTerritory = null;
	protected final GridGameFrame m_parentGridGameFrame;
	protected IGridPlayData m_lastMove = null;
	protected final ImageScrollModel m_imageScrollModel;
	protected PBEMMessagePoster m_posterPBEM = null;
	protected ForumPosterComponent m_forumPosterComponent;
	private final CountDownLatchHandler m_latchesToCloseOnShutdown = new CountDownLatchHandler(false);
	
	public GridMapPanel(final GameData data, final GridMapData mapData, final GridGameFrame parentGridGameFrame, final ImageScrollModel imageScrollModel)
	{
		super(mapData.getMapDimensions(), imageScrollModel);
		m_imageScrollModel = imageScrollModel;
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
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				m_parentGridGameFrame.updateRightSidePanel(move.toString(), m_lastMove.getEnd().getUnits().getUnits());
			}
		});
	}
	
	public void showGridEndTurnData(final IGridEndTurnData endTurnData)
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				m_parentGridGameFrame.updateRightSidePanel(endTurnData.toString(), null);
			}
		});
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
			m_gameData.removeGameMapListener(GAME_MAP_LISTENER);
			m_gameData.removeDataChangeListener(GAME_DATA_CHANGE_LISTENER);
		}
		m_gameData = data;
		m_gameData.addGameMapListener(GAME_MAP_LISTENER);
		m_gameData.addDataChangeListener(GAME_DATA_CHANGE_LISTENER);
		updateAllImages();
	}
	
	protected GameMapListener GAME_MAP_LISTENER = new GameMapListener()
	{
		
		public void gameMapDataChanged()
		{
			m_mapData.setMapData(m_gameData.getMap(), m_gameData.getMap().getXDimension(), m_gameData.getMap().getYDimension(), m_mapData.getSquareWidth(), m_mapData.getSquareHeight(),
						m_mapData.getBevelHeight(), m_mapData.getBevelHeight());
			m_imageScrollModel.setMaxBounds(m_mapData.getMapDimensions());
			mapDataAndDimensionsChanged();
			updateAllImages();
		}
	};
	
	protected void mapDataAndDimensionsChanged()
	{
	}
	
	@Override
	protected void paintComponent(final Graphics g)
	{
		super.paintComponent(g);
		final Graphics2D g2d = (Graphics2D) g;
		g2d.setColor(this.getBackground());
		g2d.fillRect(0, 0, getWidth(), getHeight());
		g2d.clip(new Rectangle2D.Double(0, 0, (getImageWidth() * m_scale), (getImageHeight() * m_scale)));
		g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
		g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		final int topLeftX = m_model.getX();
		final int topLeftY = m_model.getY();
		// translate our g2d so that our image scrolls
		g2d.translate(-topLeftX, -topLeftY);
		paintComponentMiddleLayer(g2d, topLeftX, topLeftY);
		paintValidMoves(g2d, topLeftX, topLeftY);
		paintLastMove(g2d, topLeftX, topLeftY);
		paintMouseShadow(g2d, topLeftX, topLeftY);
	}
	
	protected abstract void paintComponentMiddleLayer(final Graphics2D g2d, final int topLeftX, final int topLeftY);
	
	protected void paintValidMoves(final Graphics2D g2d, final int topLeftX, final int topLeftY)
	{
		if (m_validMovesList != null)
		{
			g2d.setColor(Color.black);
			g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.6f));
			for (final IGridPlayData play : m_validMovesList.getFirst())
			{
				for (final Territory t : play.getAllStepsExceptStart())
				{
					final Polygon p = m_mapData.getPolygon(t);
					final Rectangle rect = p.getBounds();
					g2d.drawLine(rect.x, rect.y, rect.x + rect.width, rect.y + rect.height);
					g2d.drawLine(rect.x, rect.y + rect.height, rect.x + rect.width, rect.y);
				}
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
	}
	
	protected void paintLastMove(final Graphics2D g2d, final int topLeftX, final int topLeftY)
	{
		if (m_lastMove != null)
		{
			g2d.setColor(Color.gray);
			Territory last = m_lastMove.getStart();
			for (final Territory t : m_lastMove.getAllStepsExceptStart())
			{
				g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.6f));
				final Rectangle start = m_mapData.getPolygon(last).getBounds();
				final Rectangle end = m_mapData.getPolygon(t).getBounds();
				g2d.drawLine(start.x + (start.width / 2), start.y + (start.height / 2), end.x + (end.width / 2), end.y + (end.height / 2));
				last = t;
			}
		}
	}
	
	protected void paintMouseShadow(final Graphics2D g2d, final int topLeftX, final int topLeftY)
	{
		if (m_mouseShadowImage != null)
		{
			final AffineTransform t = new AffineTransform();
			t.translate(m_currentMouseLocation.x - (m_mapData.getSquareWidth() / 2), m_currentMouseLocation.y - (m_mapData.getSquareHeight() / 2));
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
			m_validMovesList = getValidMovesList(m_clickedAt, m_parentGridGameFrame.getActivePlayer());
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
		if (m_releasedAt != null)
			notifyTerritorySelected(m_releasedAt, new MouseDetails(e, e.getX(), e.getY()));
		// The waitForPlay method is waiting for mouse input.
		// Let it know that we have processed mouse input.
		if (!BaseEditDelegate.getEditMode(m_gameData) && (m_waiting != null && (m_clickedAt != null && m_releasedAt != null)))
		{
			m_waiting.countDown();
		}
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
		final int icon_width = m_mapData.getSquareWidth();
		final int icon_height = m_mapData.getSquareHeight();
		final int xSpace = m_mapData.getSquareWidth() / 5;
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
			final IGridPlayData play = new GridPlayData(m_clickedAt, m_releasedAt, player);
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
	
	public IGridEndTurnData waitForEndTurn(final PlayerID player, final IPlayerBridge bridge, final CountDownLatch waiting) throws InterruptedException
	{
		// Make sure we have a valid CountDownLatch.
		if (waiting == null || waiting.getCount() != 1)
			throw new IllegalArgumentException("CountDownLatch must be non-null and have getCount()==1");
		// The mouse listeners need access to the CountDownLatch, so store as a member variable.
		final boolean waitForPBEM = waitForPlayByEmailOrForumPoster(player, bridge);
		final IGridEndTurnData skipPhaseData = new GridEndTurnData(null, true, player);
		// returning null = loop this method; so we don't want to return null
		if (!waitForPBEM)
			return skipPhaseData;
		else
			showPlayByEmailOrForumPosterPanel(player, bridge, waiting);
		m_waiting = waiting;
		m_waiting.await();
		return skipPhaseData;
	}
	
	protected boolean waitForPlayByEmailOrForumPoster(final PlayerID player, final IPlayerBridge bridge)
	{
		int round = 0;
		try
		{
			m_gameData.acquireReadLock();
			round = m_gameData.getSequence().getRound();
		} finally
		{
			m_gameData.releaseReadLock();
		}
		m_posterPBEM = new PBEMMessagePoster(m_gameData, player, round, "Turn Summary");
		if (!m_posterPBEM.hasMessengers())
			return false;
		if (skipPosting() || Boolean.parseBoolean(bridge.getStepProperties().getProperty(GameStep.PROPERTY_skipPosting, "false")))
			return false;
		return true;
	}
	
	protected boolean skipPosting()
	{
		return false;
	}
	
	protected void showPlayByEmailOrForumPosterPanel(final PlayerID player, final IPlayerBridge bridge, final CountDownLatch waiting)
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				m_parentGridGameFrame.updateRightSidePanel(getPlayByEmailOrForumPosterPanel(player, bridge, waiting, m_posterPBEM, false, true, false, false, false));
				m_parentGridGameFrame.maximizeRightSidePanel();
			}
		});
	}
	
	protected JPanel getPlayByEmailOrForumPosterPanel(final PlayerID player, final IPlayerBridge bridge, final CountDownLatch waiting, final PBEMMessagePoster poster,
				final boolean allowIncludeTerritorySummary, final boolean allowIncludeTerritoryAllPlayersSummary, final boolean allowIncludeProductionSummary, final boolean allowDiceBattleDetails,
				final boolean allowDiceStatistics)
	{
		final JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setBorder(new EmptyBorder(5, 5, 0, 0));
		panel.add(new JLabel("PBEM/PBF " + player.getName() + " Turn Summary"));
		final IAbstractForumPosterDelegate delegate = (IAbstractForumPosterDelegate) bridge.getRemoteDelegate();
		final boolean hasPosted = delegate.getHasPostedTurnSummary();
		final Action doneAction = new AbstractAction("Done")
		{
			private static final long serialVersionUID = 2574764222648059066L;
			
			public void actionPerformed(final ActionEvent event)
			{
				if (waiting != null)
				{
					waiting.countDown();
					m_parentGridGameFrame.updateRightSidePanel(new JLabel(""));
				}
			}
		};
		m_forumPosterComponent = new ForumPosterComponent(m_gameData, doneAction, "Turn Summary");
		panel.add(m_forumPosterComponent.layoutComponents(poster, delegate, bridge, m_parentGridGameFrame, hasPosted,
					allowIncludeTerritorySummary, allowIncludeTerritoryAllPlayersSummary, allowIncludeProductionSummary, allowDiceBattleDetails, allowDiceStatistics));
		return panel;
	}
	
	protected abstract String isValidPlay(final IGridPlayData play);
	
	protected abstract Collection<Territory> getCapturesForPlay(final IGridPlayData play);
	
	protected Tuple<Collection<IGridPlayData>, Collection<Territory>> getValidMovesList(final Territory clickedOn, final PlayerID player)
	{
		if (clickedOn == null)
			return null;
		final Collection<IGridPlayData> validMovesList = new HashSet<IGridPlayData>();
		final Collection<Territory> capturesForValidMoves = new HashSet<Territory>();
		for (final Territory t : m_gameData.getMap().getTerritories())
		{
			final IGridPlayData play = new GridPlayData(clickedOn, t, player);
			if (isValidPlay(play) == null)
			{
				validMovesList.add(play);
				capturesForValidMoves.addAll(getCapturesForPlay(play));
			}
		}
		return new Tuple<Collection<IGridPlayData>, Collection<Territory>>(validMovesList, capturesForValidMoves);
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
	
	public void doKeyListenerEvents(final KeyEvent e)
	{
	}
	
	public void shutDown()
	{
		m_latchesToCloseOnShutdown.shutDown();
	}
	
	public void addShutdownLatch(final CountDownLatch latch)
	{
		m_latchesToCloseOnShutdown.addShutdownLatch(latch);
	}
	
	public void removeShutdownLatch(final CountDownLatch latch)
	{
		m_latchesToCloseOnShutdown.removeShutdownLatch(latch);
	}
	
	public CountDownLatchHandler getCountDownLatchHandler()
	{
		return m_latchesToCloseOnShutdown;
	}
	
	public GameData getData()
	{
		return m_gameData;
	}
	
	protected final ListenerList<MapSelectionListener> m_mapSelectionListeners = new ListenerList<MapSelectionListener>();
	
	public void addMapSelectionListener(final MapSelectionListener listener)
	{
		m_mapSelectionListeners.add(listener);
	}
	
	public void removeMapSelectionListener(final MapSelectionListener listener)
	{
		m_mapSelectionListeners.remove(listener);
	}
	
	protected void notifyTerritorySelected(final Territory t, final MouseDetails me)
	{
		for (final MapSelectionListener msl : m_mapSelectionListeners)
		{
			msl.territorySelected(t, me);
		}
	}
	
	protected void notifyMouseMoved(final Territory t, final MouseDetails me)
	{
		for (final MapSelectionListener msl : m_mapSelectionListeners)
		{
			msl.mouseMoved(t, me);
		}
	}
	
	protected void notifyMouseEntered(final Territory t)
	{
		for (final MapSelectionListener msl : m_mapSelectionListeners)
		{
			msl.mouseEntered(t);
		}
	}
}
