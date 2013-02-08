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
package games.strategy.grid.kingstable.player;

import games.strategy.common.player.ai.AIAlgorithm;
import games.strategy.common.player.ai.GameState;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.gamePlayer.IPlayerBridge;
import games.strategy.grid.delegate.remote.IGridPlayDelegate;
import games.strategy.grid.kingstable.attachments.PlayerAttachment;
import games.strategy.grid.player.GridAbstractAI;
import games.strategy.grid.ui.GridPlayData;
import games.strategy.grid.ui.IGridPlayData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

/**
 * 
 * @author Lane Schwartz
 * 
 */
public class BetterAI extends GridAbstractAI
{
	private int m_xDimension;
	private int m_yDimension;
	private boolean m_kingPlayer;
	private PlayerID m_opponent;
	
	
	// private PlayerID m_attacker;
	// private PlayerID m_defender;
	/** Algorithms available for use in BetterAI */
	public enum Algorithm
	{
		MINIMAX, ALPHABETA
	}
	
	// The algorithm to be used
	private final Algorithm algorithm;
	// Default to a search depth of 2-ply
	private int cutoffDepth = 2;
	
	public BetterAI(final String name, final String type, final Algorithm algorithm)
	{
		super(name, type);
		this.algorithm = algorithm;
	}
	
	@Override
	public void initialize(final IPlayerBridge bridge, final PlayerID id)
	{
		super.initialize(bridge, id);
		m_xDimension = getGameData().getMap().getXDimension();
		m_yDimension = getGameData().getMap().getYDimension();
		{
			final PlayerAttachment pa = (PlayerAttachment) id.getAttachment("playerAttachment");
			if (pa != null)
			{
				if (pa.getNeedsKing())
					m_kingPlayer = true;
				cutoffDepth = pa.getAlphaBetaSearchDepth();
			}
			else
				m_kingPlayer = false;
		}
		m_opponent = null;
		for (final PlayerID p : getGameData().getPlayerList().getPlayers())
		{
			if (!p.equals(id) && !p.equals(PlayerID.NULL_PLAYERID))
			{
				m_opponent = p;
				break;
			}
		}
		// m_attacker = null;
		// m_defender = null;
		// for (PlayerID player : m_bridge.getGameData().getPlayerList().getPlayers())
		// {
		// PlayerAttachment pa = (PlayerAttachment) player.getAttachment("playerAttachment");
		//
		// if (pa==null)
		// m_attacker = player;
		// else if (pa.needsKing())
		// m_defender = player;
		// else
		// m_attacker = player;
		// }
	}
	
	@Override
	protected void play()
	{
		final State initial_state = getInitialState();
		// Move move = minimaxDecision(initial_state);
		try
		{
			Move move;
			if (algorithm.equals(Algorithm.ALPHABETA))
				move = AIAlgorithm.alphaBetaSearch(initial_state);
			else
				move = AIAlgorithm.minimaxSearch(initial_state);
			// System.out.println(m_id.getName() + " should move from (" + move.getStart().getFirst() + "," +move.getStart().getSecond() + ") to (" + move.getEnd().getFirst()+ "," +move.getEnd().getSecond() + ")");
			final IGridPlayDelegate playDel = (IGridPlayDelegate) getPlayerBridge().getRemote();
			final PlayerID me = getPlayerID();
			final Territory start = getGameData().getMap().getTerritoryFromCoordinates(move.getStart().getFirst(), move.getStart().getSecond());
			final Territory end = getGameData().getMap().getTerritoryFromCoordinates(move.getEnd().getFirst(), move.getEnd().getSecond());
			final IGridPlayData play = new GridPlayData(start, end, me);
			playDel.play(play);
		} catch (final OutOfMemoryError e)
		{
			System.out.println("Ran out of memory while searching for next move: " + counter + " moves examined.");
			System.exit(-1);
		}
	}
	
	private State getInitialState()
	{/*
		PlayerID currentPlayer = m_id;
		PlayerID otherPlayer = null;
		for (PlayerID p : m_bridge.getGameData().getPlayerList().getPlayers())
		{
			if (!p.equals(currentPlayer))
			{
				otherPlayer = p;
				break;
			}
		}
		   */
		return new State(getGameData().getMap().getTerritories());
		// return new State(currentPlayer, otherPlayer, m_xDimension, m_yDimension, m_bridge.getGameData().getMap().getTerritories());
	}
	
	public static int counter = 0;
	
	
	class State extends GameState<Move>
	{
		private final HashMap<Integer, PlayerID> squareOwner;
		private int m_kingX = -1;
		private int m_kingY = -1;
		private final int m_depth;
		private final Move m_move;
		
		public State(final Collection<Territory> territories)
		{
			m_depth = 0;
			m_move = null;
			m_playerPerformingMove = m_opponent;
			m_otherPlayer = getPlayerID();
			squareOwner = new HashMap<Integer, PlayerID>(m_xDimension * m_yDimension);
			for (final Territory t : territories)
			{
				squareOwner.put((t.getX() * m_xDimension + t.getY()), t.getOwner());
				if (!t.getUnits().isEmpty())
				{
					final Unit unit = (Unit) t.getUnits().getUnits().toArray()[0];
					if (unit.getType().getName().equals("king"))
					{
						m_kingX = t.getX();
						m_kingY = t.getY();
					}
				}
			}
		}
		
		@Override
		public State getSuccessor(final Move move)
		{
			return new State(move, this);
		}
		
		@Override
		public Move getMove()
		{
			return m_move;
		}
		
		private final PlayerID m_playerPerformingMove, m_otherPlayer;
		
		private State(final Move move, final State parentState)
		{
			m_move = move;
			m_depth = parentState.m_depth + 1;
			counter++;
			final int startX = move.getStart().getFirst();
			final int startY = move.getStart().getSecond();
			final int endX = move.getEnd().getFirst();
			final int endY = move.getEnd().getSecond();
			if (startX == parentState.m_kingX && startY == parentState.m_kingY)
			{
				m_kingX = endX;
				m_kingY = endY;
			}
			else
			{
				m_kingX = parentState.m_kingX;
				m_kingY = parentState.m_kingY;
			}
			// The start state is at depth 0
			if (parentState.m_depth % 2 == 0)
			{
				m_playerPerformingMove = getPlayerID();
				m_otherPlayer = m_opponent;
			}
			else
			{
				m_playerPerformingMove = m_opponent;
				m_otherPlayer = getPlayerID();
			}
			// Clone the map from the parent state
			squareOwner = new HashMap<Integer, PlayerID>(parentState.squareOwner.size());
			for (final Entry<Integer, PlayerID> s : parentState.squareOwner.entrySet())
				squareOwner.put(s.getKey(), s.getValue());
			// Now enter the new move
			squareOwner.put(move.getStart().getFirst() * m_xDimension + move.getStart().getSecond(), PlayerID.NULL_PLAYERID);
			squareOwner.put(move.getEnd().getFirst() * m_xDimension + move.getEnd().getSecond(), m_playerPerformingMove);
			// Now check for captures
			checkForCaptures(move.getEnd().getFirst(), move.getEnd().getSecond());
		}
		
		private void checkForCaptures(final int endX, final int endY)
		{
			for (final Entry<Integer, PlayerID> s : squareOwner.entrySet())
			{
				final int squareX = s.getKey() / m_xDimension;
				final int squareY = s.getKey() % m_xDimension;
				final PlayerID s_owner = s.getValue();
				// PlayerID owner = s_owner;
				if (s_owner.equals(m_otherPlayer))
				{
					if (squareX == endX)
					{
						if (squareY == endY + 1)
						{
							final PlayerID above = get(endX, endY + 2);
							if (above != null && above.equals(m_playerPerformingMove))
							{
								// Can the king be captured?
								if (squareX == m_kingX && squareY == m_kingY)
								{
									final PlayerID left = get(endX - 1, endY + 1);
									final PlayerID right = get(endX + 1, endY + 1);
									if (left != null && right != null && left.equals(m_playerPerformingMove) && right.equals(m_playerPerformingMove))
									{
										squareOwner.put(squareX * m_xDimension + squareY, PlayerID.NULL_PLAYERID);
										m_kingX = -1;
										m_kingY = -1;
									}
								}
								// Can a pawn be captured?
								else
								{
									squareOwner.put(squareX * m_xDimension + squareY, PlayerID.NULL_PLAYERID);
								}
							}
						}
						else if (squareY == endY - 1)
						{
							final PlayerID below = get(endX, endY - 2);
							if (below != null && below.equals(m_playerPerformingMove))
							{
								// Can the king be captured?
								if (squareX == m_kingX && squareY == m_kingY)
								{
									System.out.println("Possible king capture with king at (" + squareX + "," + squareY + ")");
									final PlayerID left = get(endX - 1, endY - 1);
									final PlayerID right = get(endX + 1, endY - 1);
									if (left != null && right != null && left.equals(m_playerPerformingMove) && right.equals(m_playerPerformingMove))
									{
										squareOwner.put(squareX * m_xDimension + squareY, PlayerID.NULL_PLAYERID);
										m_kingX = -1;
										m_kingY = -1;
									}
								}
								// Can a pawn be captured?
								else
								{
									squareOwner.put(squareX * m_xDimension + squareY, PlayerID.NULL_PLAYERID);
								}
							}
						}
					}
					if (endY == squareY)
					{
						if (squareX == endX + 1)
						{
							final PlayerID right = get(endX + 2, endY);
							if (right != null && right.equals(m_playerPerformingMove))
							{
								// Can the king be captured?
								if (squareX == m_kingX && squareY == m_kingY)
								{
									System.out.println("Possible king capture with king at (" + squareX + "," + squareY + ")");
									final PlayerID above = get(endX + 1, endY - 1);
									final PlayerID below = get(endX + 1, endY + 1);
									if (above != null && below != null && above.equals(m_playerPerformingMove) && below.equals(m_playerPerformingMove))
									{
										squareOwner.put(squareX * m_xDimension + squareY, PlayerID.NULL_PLAYERID);
										m_kingX = -1;
										m_kingY = -1;
									}
								}
								// Can a pawn be captured?
								else
								{
									squareOwner.put(squareX * m_xDimension + squareY, PlayerID.NULL_PLAYERID);
								}
							}
						}
						else if (squareX == endX - 1)
						{
							final PlayerID left = get(endX - 2, endY);
							if (left != null && left.equals(m_playerPerformingMove))
							{
								// Can the king be captured?
								if (squareX == m_kingX && squareY == m_kingY)
								{
									System.out.println("Possible king capture with king at (" + squareX + "," + squareY + ")");
									final PlayerID above = get(endX - 1, endY - 1);
									final PlayerID below = get(endX - 1, endY + 1);
									if (above != null && below != null && above.equals(m_playerPerformingMove) && below.equals(m_playerPerformingMove))
									{
										squareOwner.put(squareX * m_xDimension + squareY, PlayerID.NULL_PLAYERID);
										m_kingX = -1;
										m_kingY = -1;
									}
								}
								// Can a pawn be captured?
								else
								{
									squareOwner.put(squareX * m_xDimension + squareY, PlayerID.NULL_PLAYERID);
								}
							}
						}
					}
				}
			}
		}
		
		private boolean isKingsSquare(final int x, final int y)
		{
			if (x == 5 && y == 5)
				return true;
			else if ((x == 0 && (y == 0 || y == m_yDimension - 1)) || (x == m_xDimension - 1 && (y == 0 || y == m_yDimension - 1)))
				return true;
			else
				return false;
		}
		
		@Override
		public Collection<GameState<Move>> successors()
		{
			final PlayerID successorPlayer = m_otherPlayer;
			final Collection<GameState<Move>> successors = new ArrayList<GameState<Move>>();
			int countCurrentPlayerPieces = 0;
			for (final Entry<Integer, PlayerID> start : this.squareOwner.entrySet())
			{
				final PlayerID s_owner = start.getValue();
				// Only consider squares that player owns
				if (successorPlayer.equals(s_owner))
				{
					countCurrentPlayerPieces++;
					final int startX = start.getKey() / m_xDimension;// (m_maxX-1);
					final int startY = start.getKey() % m_xDimension;// (m_maxX-1);
					boolean kingIsMoving;
					if (startX == m_kingX && startY == m_kingY)
						kingIsMoving = true;
					else
						kingIsMoving = false;
					for (int x = startX - 1; x >= 0; x--)
					{
						final PlayerID destination = this.get(x, startY);
						if (destination.equals(PlayerID.NULL_PLAYERID))
						{
							if (kingIsMoving || !isKingsSquare(x, startY))
							{
								final Move move = new Move(new Pair<Integer, Integer>(startX, startY), new Pair<Integer, Integer>(x, startY));
								successors.add(new State(move, this));
							}
						}
						else
							break;
					}
					for (int x = startX + 1; x < m_xDimension; x++)
					{
						final PlayerID destination = this.get(x, startY);
						if (destination.equals(PlayerID.NULL_PLAYERID))
						{
							if (kingIsMoving || !isKingsSquare(x, startY))
							{
								final Move move = new Move(new Pair<Integer, Integer>(startX, startY), new Pair<Integer, Integer>(x, startY));
								successors.add(new State(move, this));
							}
						}
						else
							break;
					}
					for (int y = startY - 1; y >= 0; y--)
					{
						final PlayerID destination = this.get(startX, y);
						if (destination.equals(PlayerID.NULL_PLAYERID))
						{
							if (kingIsMoving || !isKingsSquare(startX, y))
							{
								final Move move = new Move(new Pair<Integer, Integer>(startX, startY), new Pair<Integer, Integer>(startX, y));
								successors.add(new State(move, this));
							}
						}
						else
							break;
					}
					for (int y = startY + 1; y < m_yDimension; y++)
					{
						final PlayerID destination = this.get(startX, y);
						if (destination.equals(PlayerID.NULL_PLAYERID))
						{
							if (kingIsMoving || !isKingsSquare(startX, y))
							{
								final Move move = new Move(new Pair<Integer, Integer>(startX, startY), new Pair<Integer, Integer>(startX, y));
								successors.add(new State(move, this));
							}
						}
						else
							break;
					}
				}
			}
			return successors;
		}
		
		public PlayerID get(final int x, final int y)
		{
			return squareOwner.get((m_xDimension * x + y));
		}
		
		/*
		private float getUtilityBasedOnMobility()
		{
		    
		}
		*/
		@Override
		public float getUtility()
		{
			// if the king has been captured...
			if (m_kingX == -1 || m_kingY == -1)
			{
				if (m_kingPlayer)
					return Integer.MIN_VALUE;
				else
					return Integer.MAX_VALUE;
			}
			// or if the king is in the corner...
			else if ((m_kingX == 0 && (m_kingY == 0 || m_kingY == m_yDimension - 1)) || (m_kingX == m_xDimension - 1 && (m_kingY == 0 || m_kingY == m_yDimension - 1)))
			{
				if (m_kingPlayer)
					return Integer.MAX_VALUE;
				else
					return Integer.MIN_VALUE;
			}
			// otherwise...
			else
			{
				float numPieces = 0;
				float numOpponentPieces = 0;
				// Count the number of pieces that each player has on the board
				for (final PlayerID p : squareOwner.values())
				{
					if (!p.equals(PlayerID.NULL_PLAYERID))
					{
						if (p.equals(getPlayerID()))
							numPieces += 1.0;
						else
							numOpponentPieces += 1.0;
					}
				}
				if (numPieces == 0)
					return Integer.MIN_VALUE;
				else if (numOpponentPieces == 0)
					return Integer.MAX_VALUE;
				else
					return numPieces / numOpponentPieces;
			}
		}
		
		@Override
		public boolean gameIsOver()
		{
			if ((m_kingX == -1 || m_kingY == -1) || (m_kingX == 0 && (m_kingY == 0 || m_kingY == m_yDimension - 1)) || (m_kingX == m_xDimension - 1 && (m_kingY == 0 || m_kingY == m_yDimension - 1)))
				return true;
			else
				return false;
		}
		
		@Override
		public boolean cutoffTest()
		{
			if (gameIsOver())
				return true;
			else if (m_depth >= cutoffDepth)
				return true;
			else
				return false;
		}
		
		public Iterator<PlayerID> iterator()
		{
			return squareOwner.values().iterator();
		}
	}
	

	class Move
	{
		private final Pair<Integer, Integer> m_start;
		private final Pair<Integer, Integer> m_end;
		
		public Move(final Pair<Integer, Integer> start, final Pair<Integer, Integer> end)
		{
			m_start = start;
			m_end = end;
		}
		
		public Pair<Integer, Integer> getStart()
		{
			return m_start;
		}
		
		public Pair<Integer, Integer> getEnd()
		{
			return m_end;
		}
	}
	

	class Pair<First, Second>
	{
		private final First m_first;
		private final Second m_second;
		
		Pair(final First first, final Second second)
		{
			m_first = first;
			m_second = second;
		}
		
		First getFirst()
		{
			return m_first;
		}
		
		Second getSecond()
		{
			return m_second;
		}
	}
}
