package games.strategy.grid.go.ui;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.gamePlayer.IPlayerBridge;
import games.strategy.grid.go.delegate.EndTurnDelegate;
import games.strategy.grid.go.delegate.PlayDelegate;
import games.strategy.grid.ui.GridEndTurnData;
import games.strategy.grid.ui.GridGameFrame;
import games.strategy.grid.ui.GridMapData;
import games.strategy.grid.ui.GridMapPanel;
import games.strategy.grid.ui.GridPlayData;
import games.strategy.grid.ui.IGridEndTurnData;
import games.strategy.grid.ui.IGridPlayData;
import games.strategy.ui.ImageScrollModel;
import games.strategy.util.Match;
import games.strategy.util.Triple;
import games.strategy.util.Tuple;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseMotionListener;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import javax.swing.SwingUtilities;

/**
 * 
 * @author veqryn
 * 
 */
public class GoMapPanel extends GridMapPanel
{
	private static final long serialVersionUID = 2560185139493126853L;
	protected Set<Territory> m_territoryGroupsThatShouldDie = null; // which stones are dead?
	protected Triple<List<Territory>, List<Tuple<Territory, Collection<Territory>>>, List<Territory>> m_allMovesPossibleList = null;
	protected boolean m_passing = false; // two passes sends us to end turn (end game) delegate
	protected boolean m_wantToContinuePlaying = false; // after we get to end game, if players disagree on score, game will continue
	protected GO_DELEGATE_PHASE m_phase = GO_DELEGATE_PHASE.PLAY;
	
	
	public enum GO_DELEGATE_PHASE
	{
		PLAY, ENDGAME
	}
	
	public GoMapPanel(final GameData data, final GridMapData mapData, final GridGameFrame parentGridGameFrame, final ImageScrollModel imageScrollModel)
	{
		super(data, mapData, parentGridGameFrame, imageScrollModel);
	}
	
	public void changePhase(final GO_DELEGATE_PHASE newPhase)
	{
		m_phase = newPhase;
	}
	
	@Override
	protected String isValidPlay(final IGridPlayData play)
	{
		final String error;
		try
		{
			m_gameData.acquireReadLock();
			error = PlayDelegate.isValidPlay(play, m_parentGridGameFrame.getActivePlayer(), m_gameData);
		} finally
		{
			m_gameData.releaseReadLock();
		}
		return error;
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
		final Collection<IGridPlayData> plays = new ArrayList<IGridPlayData>();
		final Collection<Territory> captures = new HashSet<Territory>();
		final GridPlayData play = new GridPlayData(clickedOn, null, player);
		if (isValidPlay(play) == null)
		{
			plays.add(play);
			captures.addAll(getCapturesForPlay(play));
		}
		return new Tuple<Collection<IGridPlayData>, Collection<Territory>>(plays, captures);
	}
	
	protected Triple<List<Territory>, List<Tuple<Territory, Collection<Territory>>>, List<Territory>> getAllPossibleMovesList(final PlayerID activePlayer)
	{
		final Triple<List<Territory>, List<Tuple<Territory, Collection<Territory>>>, List<Territory>> moves;
		try
		{
			m_gameData.acquireReadLock();
			moves = PlayDelegate.getAllValidMovesCaptureMovesAndInvalidMoves(activePlayer, m_gameData);
		} finally
		{
			m_gameData.releaseReadLock();
		}
		return moves;
	}
	
	/**
	 * Draw the current map and pieces.
	 */
	@Override
	protected void paintComponentMiddleLayer(final Graphics2D g2d, final int topLeftX, final int topLeftY)
	{
		g2d.setColor(Color.lightGray);
		g2d.fillRect(0, 0, m_model.getMaxWidth(), m_model.getMaxHeight());
		final Color tileOdd = new Color(226, 163, 103);
		final Color tileEven = new Color(225, 161, 101);
		final int maxX = m_gameData.getMap().getXDimension() - 1;
		final int maxY = m_gameData.getMap().getYDimension() - 1;
		for (final Map.Entry<Territory, Polygon> entry : m_mapData.getTerritoryPolygons(m_gameData.getMap()).entrySet())
		{
			final Polygon p = entry.getValue();
			final Territory at = entry.getKey();
			if (at == null || p == null)
				continue;
			final Color backgroundColor;
			if ((at.getX() + at.getY()) % 2 == 0)
				backgroundColor = tileEven;
			else
				backgroundColor = tileOdd;
			g2d.setColor(backgroundColor);
			g2d.fillPolygon(p);
			
			g2d.setColor(Color.black);
			
			// we want to draw the squares offset, so that it looks like we are placing pieces on the intersections of lines, not in a square
			final int xOffset = m_mapData.getSquareWidth() / 2;
			final int yOffset = m_mapData.getSquareHeight() / 2;
			g2d.translate(-xOffset, -yOffset);
			if (at.getY() != 0 && at.getX() != 0)
				g2d.drawPolygon(p);
			g2d.translate(xOffset * 2, 0);
			if (at.getY() != 0 && at.getX() != maxX)
				g2d.drawPolygon(p);
			g2d.translate(0, yOffset * 2);
			if (at.getY() != maxY && at.getX() != maxX)
				g2d.drawPolygon(p);
			g2d.translate(-(xOffset * 2), 0);
			if (at.getY() != maxY && at.getX() != 0)
				g2d.drawPolygon(p);
			// return it to original position
			g2d.translate(xOffset, -yOffset);
		}
		for (final Map.Entry<Territory, Polygon> entry : m_mapData.getTerritoryPolygons(m_gameData.getMap()).entrySet())
		{
			final Polygon p = entry.getValue();
			final Territory at = entry.getKey();
			if (at == null || p == null)
				continue;
			// now draw the units
			final Image image = m_images.get(at);
			if (image != null)
			{
				final Rectangle square = p.getBounds();
				g2d.drawImage(image, square.x, square.y, square.width, square.height, null, null);
			}
		}
		paintAllPossibleMoves(g2d, topLeftX, topLeftY);
	}
	
	@Override
	protected void paintLastMove(final Graphics2D g2d, final int topLeftX, final int topLeftY)
	{
		if (m_lastMove != null && m_lastMove.getStart() != null)
		{
			g2d.setColor(Color.gray);
			final Territory last = m_lastMove.getStart();
			final Rectangle start = m_mapData.getPolygon(last).getBounds();
			g2d.draw(new RoundRectangle2D.Double(start.x, start.y, start.width, start.height, start.width / 3, start.height / 3));
		}
	}
	
	protected void paintAllPossibleMoves(final Graphics2D g2d, final int topLeftX, final int topLeftY)
	{
		if (m_allMovesPossibleList != null)
		{
			g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.6f));
			// draw a yellow warning x for empty territories we can't place into
			g2d.setColor(Color.yellow);
			final List<Territory> invalid = Match.getMatches(m_allMovesPossibleList.getThird(), PlayDelegate.TerritoryHasNoUnits);
			for (final Territory t : invalid)
			{
				final Polygon p = m_mapData.getPolygon(t);
				final Rectangle rect = p.getBounds();
				g2d.drawLine(rect.x, rect.y, rect.x + rect.width, rect.y + rect.height);
				g2d.drawLine(rect.x, rect.y + rect.height, rect.x + rect.width, rect.y);
			}
			// draw red for territories we can capture, and black for the moves that capture them
			for (final Tuple<Territory, Collection<Territory>> entry : m_allMovesPossibleList.getSecond())
			{
				g2d.setColor(Color.black);
				{
					final Polygon p1 = m_mapData.getPolygon(entry.getFirst());
					final Rectangle rect1 = p1.getBounds();
					g2d.drawLine(rect1.x, rect1.y, rect1.x + rect1.width, rect1.y + rect1.height);
					g2d.drawLine(rect1.x, rect1.y + rect1.height, rect1.x + rect1.width, rect1.y);
				}
				g2d.setColor(Color.red);
				for (final Territory t : entry.getSecond())
				{
					final Polygon p2 = m_mapData.getPolygon(t);
					final Rectangle rect2 = p2.getBounds();
					g2d.drawLine(rect2.x, rect2.y, rect2.x + rect2.width, rect2.y + rect2.height);
					g2d.drawLine(rect2.x, rect2.y + rect2.height, rect2.x + rect2.width, rect2.y);
				}
			}
		}
	}
	
	@Override
	protected void paintValidMoves(final Graphics2D g2d, final int topLeftX, final int topLeftY)
	{
		if (m_phase == GO_DELEGATE_PHASE.ENDGAME)
		{
			g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
			final Map<Territory, PlayerID> currentScoreState = EndTurnDelegate.getCurrentAreaScoreState(m_territoryGroupsThatShouldDie, m_gameData);
			final Map<PlayerID, Color> playerColors = new HashMap<PlayerID, Color>();
			playerColors.put(null, Color.blue);
			for (final PlayerID player : m_gameData.getPlayerList().getPlayers())
			{
				if (player.getName().equalsIgnoreCase("white"))
					playerColors.put(player, Color.white);
				else if (player.getName().equalsIgnoreCase("black"))
					playerColors.put(player, Color.black);
				else
					playerColors.put(player, Color.red);
			}
			for (final Territory t : m_gameData.getMap().getTerritories())
			{
				final PlayerID player = currentScoreState.get(t);
				g2d.setColor(playerColors.get(player));
				final Polygon p2 = m_mapData.getPolygon(t);
				final Rectangle rect2 = p2.getBounds();
				g2d.fillRect(rect2.x, rect2.y, rect2.width, rect2.height);
				// g2d.fillRoundRect(rect2.x, rect2.y, rect2.width, rect2.height, rect2.width / 3, rect2.height / 3);
				// g2d.drawLine(rect2.x, rect2.y, rect2.x + rect2.width, rect2.y + rect2.height);
				// g2d.drawLine(rect2.x, rect2.y + rect2.height, rect2.x + rect2.width, rect2.y);
				// g2d.drawRect(rect2.x + 1, rect2.y + 1, rect2.width - 2, rect2.height - 2);
			}
		}
	}
	
	@Override
	protected void showGridPlayDataMove(final IGridPlayData move)
	{
		m_lastMove = move;
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				if (move != null)
					m_parentGridGameFrame.updateRightSidePanel(move.toString(),
								(m_lastMove.getStart() == null ? null : m_lastMove.getStart().getUnits().getUnits()));
			}
		});
	}
	
	@Override
	public void showGridEndTurnData(final IGridEndTurnData endTurnData)
	{
		super.showGridEndTurnData(endTurnData);
		m_territoryGroupsThatShouldDie = endTurnData.getTerritoryUnitsRemovalAdjustment();
		this.updateAllImages();
	}
	
	@Override
	public void mousePressed(final MouseEvent e)
	{
		if (m_phase == GO_DELEGATE_PHASE.PLAY)
		{
			m_clickedAt = m_mapData.getTerritoryAt(e.getX() + m_model.getX(), e.getY() + m_model.getY(), m_gameData.getMap());
			if (m_clickedAt != null && e.getButton() != MouseEvent.BUTTON1)
			{
				// setMouseShadowUnits(m_clickedAt.getUnits().getUnits());
				// m_validMovesList = getValidMovesList(m_clickedAt, m_parentGridGameFrame.getActivePlayer());
				m_allMovesPossibleList = getAllPossibleMovesList(m_parentGridGameFrame.getActivePlayer());
			}
		}
		else if (m_phase == GO_DELEGATE_PHASE.ENDGAME)
		{
			final Territory clickedAt = m_mapData.getTerritoryAt(e.getX() + m_model.getX(), e.getY() + m_model.getY(), m_gameData.getMap());
			if (clickedAt == null)
				return;
			if (m_territoryGroupsThatShouldDie == null)
				m_territoryGroupsThatShouldDie = new HashSet<Territory>();
			final Collection<Unit> units = clickedAt.getUnits().getUnits();
			if (units.isEmpty())
			{
				// TODO: allow switching ownership of blank spaces
				return;
			}
			else
			{
				final PlayerID owner = units.iterator().next().getOwner();
				final Set<Territory> group = PlayDelegate.getOwnedStoneChainsConnectedToThisTerritory(clickedAt, new HashSet<Territory>(), owner, m_gameData, true);
				if (m_territoryGroupsThatShouldDie.containsAll(group))
					m_territoryGroupsThatShouldDie.removeAll(group);
				else
					m_territoryGroupsThatShouldDie.addAll(group);
			}
		}
	}
	
	@Override
	public void mouseReleased(final MouseEvent e)
	{
		if (m_phase == GO_DELEGATE_PHASE.PLAY)
		{
			if (e.getButton() != MouseEvent.BUTTON1) // (e.isControlDown() || e.isAltDown() || e.isShiftDown())
			{
				SwingUtilities.invokeLater(new Runnable()
				{
					public void run()
					{
						repaint();
					}
				});
				return;
			}
			// m_releasedAt = m_mapData.getTerritoryAt(e.getX() + m_model.getX(), e.getY() + m_model.getY(), m_gameData.getMap());
			// setMouseShadowUnits(null);
			// m_validMovesList = null;
			m_allMovesPossibleList = null;
			if (m_waiting != null && (m_clickedAt != null))
			{
				m_passing = false;
				m_waiting.countDown();
			}
		}
		else if (m_phase == GO_DELEGATE_PHASE.ENDGAME)
		{
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					repaint();
				}
			});
		}
	}
	
	@Override
	public void doKeyListenerEvents(final KeyEvent e)
	{
		final int keyCode = e.getKeyCode();
		if (keyCode == KeyEvent.VK_P)
		{
			if (m_phase == GO_DELEGATE_PHASE.PLAY)
			{
				// we are passing
				m_passing = true;
				if (m_waiting != null)
				{
					m_waiting.countDown();
				}
			}
		}
		if (keyCode == KeyEvent.VK_R)
		{
			if (m_phase == GO_DELEGATE_PHASE.ENDGAME)
			{
				// we want to play on, because we disagree
				m_wantToContinuePlaying = true;
				m_territoryGroupsThatShouldDie = null;
				if (m_waiting != null)
				{
					m_waiting.countDown();
				}
			}
		}
		if (keyCode == KeyEvent.VK_C)
		{
			if (m_phase == GO_DELEGATE_PHASE.ENDGAME)
			{
				// send our score to the other player
				m_wantToContinuePlaying = false;
				if (m_territoryGroupsThatShouldDie == null)
					m_territoryGroupsThatShouldDie = new HashSet<Territory>();
				if (m_waiting != null)
				{
					m_waiting.countDown();
				}
			}
		}
	}
	
	@Override
	protected MouseMotionListener getMouseMotionListener()
	{
		return new MouseMotionAdapter()
		{
			// do nothing
		};
	}
	
	@Override
	public void setMouseShadowUnits(final Collection<Unit> units)
	{
		// do nothing
	}
	
	@Override
	public GridPlayData waitForPlay(final PlayerID player, final IPlayerBridge bridge, final CountDownLatch waiting) throws InterruptedException
	{
		// Make sure we have a valid CountDownLatch.
		if (waiting == null || waiting.getCount() != 1)
			throw new IllegalArgumentException("CountDownLatch must be non-null and have getCount()==1");
		// The mouse listeners need access to the CountDownLatch, so store as a member variable.
		m_waiting = waiting;
		// Wait for a play or an attempt to leave the game
		this.updateAllImages();
		m_waiting.await();
		if (m_clickedAt == null && !m_passing)
		{
			// If either m_clickedAt==null or m_releasedAt==null,
			// the play is invalid and must have been interrupted.
			// So, reset the member variables, and throw an exception.
			m_clickedAt = null;
			// m_releasedAt = null;
			throw new InterruptedException("Interrupted while waiting for play.");
		}
		else
		{
			// We have a valid play!
			// Reset the member variables, and return the play.
			final GridPlayData play;
			if (m_passing)
				play = new GridPlayData(true, player);
			else
				play = new GridPlayData(m_clickedAt, player);
			m_clickedAt = null;
			m_passing = false;
			// m_releasedAt = null;
			return play;
		}
	}
	
	@Override
	public IGridEndTurnData waitForEndTurn(final PlayerID player, final IPlayerBridge bridge, final CountDownLatch waiting) throws InterruptedException
	{
		// Make sure we have a valid CountDownLatch.
		if (waiting == null || waiting.getCount() != 1)
			throw new IllegalArgumentException("CountDownLatch must be non-null and have getCount()==1");
		// The mouse listeners need access to the CountDownLatch, so store as a member variable.
		m_waiting = waiting;
		// before we 'await', set the players data to show what the other player is reporting
		/* might no longer need, since we have the IDisplay do this instead....
		final IGoEndTurnDelegate endTurnDel = (IGoEndTurnDelegate) bridge.getRemote();
		final IGridEndTurnData otherPlayersTerritoryAdjustment = endTurnDel.getTerritoryAdjustment();
		if (otherPlayersTerritoryAdjustment != null)
		{
			m_territoryGroupsThatShouldDie = otherPlayersTerritoryAdjustment.getTerritoryUnitsRemovalAdjustment();
		}
		*/
		this.updateAllImages();
		// Wait for a play or an attempt to leave the game
		m_waiting.await();
		if (m_territoryGroupsThatShouldDie == null && !m_wantToContinuePlaying)
		{
			// the play is invalid and must have been interrupted. So, reset the member variables, and throw an exception.
			m_territoryGroupsThatShouldDie = null;
			m_wantToContinuePlaying = false;
			throw new InterruptedException("Interrupted while waiting for play.");
		}
		else
		{
			// We have a valid play! Reset the member variables, and return the play.
			final IGridEndTurnData endTurn = new GridEndTurnData(m_territoryGroupsThatShouldDie, m_wantToContinuePlaying, player);
			m_territoryGroupsThatShouldDie = null;
			m_wantToContinuePlaying = false;
			return endTurn;
		}
	}
}
