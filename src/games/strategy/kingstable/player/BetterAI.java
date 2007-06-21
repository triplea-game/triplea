/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package games.strategy.kingstable.player;

import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.gamePlayer.IPlayerBridge;
import games.strategy.kingstable.attachments.PlayerAttachment;
import games.strategy.kingstable.delegate.remote.IPlayDelegate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

public class BetterAI extends AbstractAI {

	private int m_maxX;
	private int m_maxY;
	private boolean m_kingPlayer;
	
	private int cutoffDepth = 1;
	
	public BetterAI(String name)
    {
        super(name);

    }
	
    public void initialize(IPlayerBridge bridge, PlayerID id)
    {
    	super.initialize(bridge, id);
    	
    	m_maxX = m_bridge.getGameData().getMap().getXDimension();
    	m_maxY = m_bridge.getGameData().getMap().getYDimension();

    	PlayerAttachment pa = (PlayerAttachment) m_id.getAttachment("playerAttachment");
    	if (pa!=null && pa.needsKing())
    		m_kingPlayer = true;
    	else
    		m_kingPlayer = false;
    }
    
	@Override
	protected void play() {

		GameState initial_state = getInitialState();
		//Move move = minimaxDecision(initial_state);
		try {
			Move move = alphaBetaSearch(initial_state);
			//System.out.println(initial_state.m_currentPlayer.getName() + " should move from (" + move.getStart().getFirst() + "," +move.getStart().getSecond() + ") to (" + move.getEnd().getFirst()+ "," +move.getEnd().getSecond() + ")");

			IPlayDelegate playDel = (IPlayDelegate) m_bridge.getRemote();
			Territory start = m_bridge.getGameData().getMap().getTerritoryFromCoordinates(move.getStart().getFirst(), move.getStart().getSecond());
			Territory end = m_bridge.getGameData().getMap().getTerritoryFromCoordinates(move.getEnd().getFirst(), move.getEnd().getSecond());
			
			playDel.play(start,end);
		} catch (OutOfMemoryError e)
		{
			System.out.println(counter);
			System.exit(-1);
		}
		
	}

	
	
	private GameState getInitialState()
	{
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
		
		//System.out.println("Current player is " + currentPlayer.getName());
		//System.out.println("Other player is " + otherPlayer.getName());
		
		//System.out.println(m_maxX + " " + m_maxY);
		
		return new GameState(currentPlayer, otherPlayer, m_maxX, m_maxY, m_bridge.getGameData().getMap().getTerritories());

	}
	
	/*
	private boolean gameIsOver(GameState state)
	{
		return state.gameIsOver();
	}
	*/
	
	private Collection<Pair<Move, GameState>> successor(GameState state)//, PlayerID player)
	{
		//if (player==null)
			//throw new IllegalArgumentException("player must not be null");
		PlayerID currentPlayer = state.m_currentPlayer;
		
		Collection<Pair<Move, GameState>> successors = new ArrayList<Pair<Move, GameState>>();
		int countCurrentPlayerPieces = 0;
		for (Entry<Integer,PlayerID> start : state.squareOwner.entrySet())
		{
			PlayerID s_owner = start.getValue();
			
			// Only consider squares that player owns
			if (currentPlayer.equals(s_owner))
			{
				countCurrentPlayerPieces++;
				
				int startX = start.getKey() / m_maxX;//(m_maxX-1);
				int startY = start.getKey() % m_maxX;//(m_maxX-1);
				//System.out.println(s_owner.getName() + " could move from (" + startX + "," + startY + ") key==" + start.getKey());
				for (int x=startX-1; x>=0; x--)
				{
					//if (x==0 && startY==0 && startX==2)
					//	System.out.println("Possible dest = (" + x + "," + startY + ")");
					PlayerID destination = state.get(x, startY);
					if (destination.equals(PlayerID.NULL_PLAYERID))
					{
						Move move = new Move(new Pair<Integer,Integer>(startX,startY), new Pair<Integer,Integer>(x,startY));
						successors.add(new Pair<Move,GameState>(move, new GameState(move,state)));
					}
					else
						break;
				}
				
				for (int x=startX+1; x<m_maxX; x++)
				{
					//System.out.println("Possible dest = (" + x + "," + startY + ")");
					PlayerID destination = state.get(x, startY);
					if (destination.equals(PlayerID.NULL_PLAYERID))
					{
						Move move = new Move(new Pair<Integer,Integer>(startX,startY), new Pair<Integer,Integer>(x,startY));
						successors.add(new Pair<Move,GameState>(move, new GameState(move,state)));
					}
					else
						break;
				}
				
				for (int y=startY-1; y>=0; y--)
				{
					//System.out.println("Possible dest = (" + startX + "," + y + ")");
					PlayerID destination = state.get(startX, y);
					if (destination.equals(PlayerID.NULL_PLAYERID))
					{
						Move move = new Move(new Pair<Integer,Integer>(startX,startY), new Pair<Integer,Integer>(startX,y));
						successors.add(new Pair<Move,GameState>(move, new GameState(move,state)));
					}
					else
						break;
					/*
					GameSquare destination = state.get(startX, y);
					if (destination.isEmpty())
					{
						Move move = new Move(start, destination);
						successors.add(new Pair<Move,GameState>(move, new GameState(move,state)));
					}
					else
						break;
					 */
				}
				
				for (int y=startY+1; y<m_maxY; y++)
				{
					//System.out.println("Possible dest = (" + startX + "," + y + ")");
					PlayerID destination = state.get(startX, y);
					if (destination.equals(PlayerID.NULL_PLAYERID))
					{
						Move move = new Move(new Pair<Integer,Integer>(startX,startY), new Pair<Integer,Integer>(startX,y));
						successors.add(new Pair<Move,GameState>(move, new GameState(move,state)));
					}
					else
						break;
					/*GameSquare destination = state.get(startX, y);
					if (destination.isEmpty())
					{
						Move move = new Move(start, destination);
						successors.add(new Pair<Move,GameState>(move, new GameState(move,state)));
					}
					else
						break;*/
				}
			}
			
		}
		
		//System.out.println(currentPlayer.getName() + " has " + countCurrentPlayerPieces + " pieces on the board.");
		return successors;
	}

	/*
	private Move minimaxDecision(GameState state)
	{
		if (m_kingPlayer)
			return maxValue(state).getSecond();
		else
			return minValue(state).getSecond();
	}
	*/
	private Move alphaBetaSearch(GameState state)
	{
		if (m_kingPlayer)
		{
			Pair<Float,Move> m = maxValue(state, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY);
			//System.out.println("score of move is " + m.getFirst());
			return m.getSecond();
		}
		else
		{	Pair<Float,Move> m = minValue(state, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY);
			//System.out.println("score of move is " + m.getFirst());
			return m.getSecond();
		}
	}
	
	
	private Pair<Float,Move> maxValue(GameState state, float alpha, float beta)
	{
		//if (state.captureIsPossible)
		//	System.out.println("capture is possible with score " + state.getUtility());
		//if (state.gameIsOver())
		if (state.cutoffTest())
			return new Pair<Float,Move>(state.getUtility(),null);
		
		float value = Float.NEGATIVE_INFINITY;
		Move bestMove = null;
		
		for (Pair<Move, GameState> move_state : successor(state))//, state.m_otherPlayer))
		{
			GameState s = move_state.getSecond();
			Move a = move_state.getFirst();
			
			float minValue = minValue(s, alpha, beta).getFirst();
			if (minValue > value)
			{
				value = minValue;
				bestMove = a;
			}
			if (value >= beta)
				new Pair<Float,Move>(value,bestMove);
			if (value > alpha)
				alpha = value;
		}
		
		return new Pair<Float,Move>(value,bestMove);
	}
	
	
	private Pair<Float,Move> minValue(GameState state, float alpha, float beta)
	{
		//if (state.captureIsPossible)
		//	System.out.println("capture is possible with score " + state.getUtility());
		//if (state.gameIsOver())
		if (state.cutoffTest())
		{	//System.out.println("cutting off at depth " + state.depth);
			return new Pair<Float,Move>(state.getUtility(),null);
		}
		float value = Float.POSITIVE_INFINITY;
		Move bestMove = null;
		
		for (Pair<Move, GameState> move_state : successor(state))//, state.m_otherPlayer))
		{	
			GameState s = move_state.getSecond();
			Move a = move_state.getFirst();
			
			float maxValue = maxValue(s, alpha, beta).getFirst();
			if (maxValue < value)
			{	System.out.println("best so far is " + maxValue);
				value = maxValue;
				bestMove = a;
			}
			if (value <= alpha)
				new Pair<Float,Move>(value,bestMove);
			if (value < beta)
				beta = value;
		}
		
		return new Pair<Float,Move>(value,bestMove);
	}
	/*
	private Pair<Float,Move> maxValue(GameState state)
	{
		if (state.gameIsOver())
			return new Pair<Float,Move>(state.getUtility(),null);
		
		float value = Float.NEGATIVE_INFINITY;
		Move bestMove = null;
		
		for (Pair<Move, GameState> s : successor(state, state.m_otherPlayer))
		{
			float minValue = minValue(s.getSecond()).getFirst();
			if (minValue > value)
			{
				value = minValue;
				bestMove = s.getFirst();
			}
		}
		
		return new Pair<Float,Move>(value,bestMove);
	}
	
	private Pair<Float,Move> minValue(GameState state)
	{
		if (state.gameIsOver())
			return new Pair<Float,Move>(state.getUtility(),null);
		
		float value = Float.POSITIVE_INFINITY;
		Move bestMove = null;
		
		for (Pair<Move, GameState> s : successor(state, state.m_otherPlayer))
		{
			float maxValue = maxValue(s.getSecond()).getFirst();
			if (maxValue > value)
			{
				value = maxValue;
				bestMove = s.getFirst();
			}
		}
		
		return new Pair<Float,Move>(value,bestMove);
	}
	*/
	
	public static int counter = 0;
	
	class GameState //implements Iterable<PlayerID> 
	{
		
		//private HashMap<Integer,GameSquare> square;
		private HashMap<Integer,PlayerID> squareOwner;
		private int m_x;
		private int m_y;
		
		private int m_kingX = -1;
		private int m_kingY = -1;
		
		private PlayerID m_currentPlayer;
		private PlayerID m_otherPlayer;
		private int depth;
		
		public GameState(PlayerID currentPlayer, PlayerID otherPlayer, int x, int y, Collection<Territory> territories)
		{
			depth=0;
			counter++;
			m_x = x;
			m_y = y;
			m_currentPlayer = currentPlayer;//otherPlayer;
			m_otherPlayer = otherPlayer;//currentPlayer;
			
			//square = new HashMap<Integer,GameSquare>(x*y);
			squareOwner = new HashMap<Integer,PlayerID>(x*y);
			//System.out.println(x*y);
			
			int countCurrentPlayerPieces = 0;
			int countOtherPlayerPieces = 0;
			int countBlankSquares = 0;
			
			for (Territory t : territories) 
			{
				//System.out.println("Adding " + (t.getX()*(x-1) + t.getY()));
				//square.put((t.getX()*(x-1) + t.getY()), new GameSquare(t.getX(), t.getY(), t.getOwner()));
				//squareOwner.put((t.getX()*(x-1) + t.getY()), t.getOwner());
				squareOwner.put((t.getX()*x + t.getY()), t.getOwner());
				/*
				if (t.getOwner().equals(m_currentPlayer))
					countCurrentPlayerPieces++;
				else if (t.getOwner().equals(m_otherPlayer))
					countOtherPlayerPieces++;
				else
					countBlankSquares++;
				*/
				if (!t.getUnits().isEmpty())
				{
					Unit unit = (Unit) t.getUnits().getUnits().toArray()[0];
					if (unit.getType().getName().equals("king"))
					{
						m_kingX = t.getX();
						m_kingY = t.getY();
					}
				}
					
			}
			
			for (PlayerID player : squareOwner.values())
			{
				if (player.equals(m_currentPlayer))
					countCurrentPlayerPieces++;
				else if (player.equals(m_otherPlayer))
					countOtherPlayerPieces++;
				else
					countBlankSquares++;
			}
			
			//System.out.println(countBlankSquares + " empty squares on the board.");
			//System.out.println(m_otherPlayer.getName() + " has " + countOtherPlayerPieces + " pieces on the board.");
			//System.out.println(m_currentPlayer.getName() + " has " + countCurrentPlayerPieces + " pieces on the board.");
			
		}
		
		public boolean captureIsPossible = false;
		
		public GameState(Move move, GameState state)
		{
			depth = state.depth+1;
			//if (depth % 10 == 0) System.out.println("depth==" + depth);
			counter++;
			m_x = state.m_x;
			m_y = state.m_y;

			m_currentPlayer = state.m_currentPlayer;
			m_otherPlayer = state.m_otherPlayer;
			
			int startX = move.getStart().getFirst();
			int startY = move.getStart().getSecond();
			
			int endX = move.getEnd().getFirst();
			int endY = move.getEnd().getSecond();
			
			if (startX==state.m_kingX && startY==state.m_kingY)
			{
				m_kingX = endX;
				m_kingY = endY;
			}
			else
			{
				m_kingX = state.m_kingX;
				m_kingY = state.m_kingY;
			}
			
			//int startSquareKey = startX*(m_x-1) + startY;
			PlayerID movingPlayer = state.get(startX, startY);//.squareOwner.get(startSquareKey);
			if (!movingPlayer.equals(state.m_currentPlayer))
				throw new RuntimeException("Only the current player can move");
			
			
			
			//square = new HashMap<Integer,GameSquare>(state.square.size());
			squareOwner = new HashMap<Integer,PlayerID>(state.squareOwner.size());
			
			for (Entry<Integer,PlayerID> s : state.squareOwner.entrySet())
			{
				int squareX = s.getKey() / m_x;//(m_x-1);
				int squareY = s.getKey() % m_x;//(m_x-1);
				PlayerID s_owner = s.getValue();
				
				if (squareX==startX && squareY==startY)
				{
					//square.put(s.getX()*(m_x-1) + s.getY(), new GameSquare(s.getX(), s.getY(), PlayerID.NULL_PLAYERID));
					//squareOwner.put(squareX*(m_x-1) + squareY, PlayerID.NULL_PLAYERID);
					squareOwner.put(squareX*m_x + squareY, PlayerID.NULL_PLAYERID);
					
				} 
				else if (squareX==endX && squareY==endY)
				{
					//square.put(s.getX()*(m_x-1) + s.getY(), new GameSquare(s.getX(), s.getY(), move.getStart().getOwner()));
					//Pair<Integer,Integer> startSquareCoordinates = move.getStart();
					
					//squareOwner.put(squareX*(m_x-1) + squareY, movingPlayer);
					squareOwner.put(squareX*m_x + squareY, movingPlayer);
				} 
				else
				{
					PlayerID owner = s_owner;
					//if (squareX==m_kingX && squareY==m_kingY)
					//	System.out.println("Square (" + squareX + "," + squareY + ") contains the king");
					//int endX = move.getEnd().getX();
					//int endY = move.getEnd().getY();
					if (s_owner.equals(state.m_otherPlayer))
					//if (!s_owner.equals(PlayerID.NULL_PLAYERID) && !s_owner.equals(m_id))
					{	//if (squareX==1 && squareY==4 && startX==0 && startY==3 && endX==1 && endY==3)
						//	System.out.println("Square (" + squareX + "," + squareY + ") contains a " + m_otherPlayer.getName() + " piece.");
						if (squareX==endX)
						{
							if (squareY==endY+1)
							{
								//GameSquare other = get(move.getEnd().getX(), move.getEnd().getY()+2);
								PlayerID other = state.get(endX, endY+2);
								//if (other!=null && other.getOwner().equals(m_currentPlayer))
								if (other!=null && other.equals(movingPlayer))
								{
									// Can the king be captured?
									if (squareX==m_kingX && squareY==m_kingY)
									{	System.out.println("Possible king capture with king at (" + squareX + "," + squareY + ")");
										PlayerID otherLeft = state.get(endX-1, endY+1);
										PlayerID otherRight = state.get(endX+1, endY+1);
										
										if (otherLeft!=null && otherRight!=null && 
												otherLeft.equals(m_currentPlayer) &&
												otherRight.equals(m_currentPlayer))
										{
											owner = PlayerID.NULL_PLAYERID; 
											m_kingX = -1;
											m_kingY = -1;
										}
											
									} 
									// Can a pawn be captured?
									else
									{
										//System.out.println("A " + s_owner.getName() + " pawn can be captured by moving from (" + move.getStart().getFirst() + "," + move.getStart().getSecond() + ") to (" + move.getEnd().getFirst() + "," + move.getEnd().getSecond() + ")");
										//captureIsPossible = true;
										owner = PlayerID.NULL_PLAYERID; 
									}

								}
							}
							else if (squareY==endY-1)
							{
								//GameSquare other = get(move.getEnd().getX(), move.getEnd().getY()+2);
								PlayerID other = state.get(endX, endY-2);
								//if (other!=null && other.getOwner().equals(m_currentPlayer))
								if (other!=null && other.equals(movingPlayer))
								{
									// Can the king be captured?
									if (squareX==m_kingX && squareY==m_kingY)
									{	System.out.println("Possible king capture with king at (" + squareX + "," + squareY + ")");
										PlayerID otherLeft = state.get(endX-1, endY-1);
										PlayerID otherRight = state.get(endX+1, endY-1);
										
										if (otherLeft!=null && otherRight!=null && 
												otherLeft.equals(m_currentPlayer) &&
												otherRight.equals(m_currentPlayer))
										{
											owner = PlayerID.NULL_PLAYERID; 
											m_kingX = -1;
											m_kingY = -1;
										}
											
									} 
									// Can a pawn be captured?
									else
									{
										//System.out.println("A " + s_owner.getName() + " pawn can be captured by moving from (" + move.getStart().getFirst() + "," + move.getStart().getSecond() + ") to (" + move.getEnd().getFirst() + "," + move.getEnd().getSecond() + ")");
										//captureIsPossible = true;
										owner = PlayerID.NULL_PLAYERID; 
									}

								}
							}
						}
						
						if (endY==squareY)
						{
							if (squareX==endX+1)
							{
								//GameSquare other = get(move.getEnd().getX(), move.getEnd().getY()+2);
								PlayerID other = state.get(endX+2, endY);
								//if (other!=null && other.getOwner().equals(m_currentPlayer))
								if (other!=null && other.equals(movingPlayer))
								{
									// Can the king be captured?
									if (squareX==m_kingX && squareY==m_kingY)
									{	System.out.println("Possible king capture with king at (" + squareX + "," + squareY + ")");
										PlayerID otherAbove = state.get(endX+1, endY-1);
										PlayerID otherBelow = state.get(endX+1, endY+1);
										
										if (otherAbove!=null && otherBelow!=null && 
												otherAbove.equals(m_currentPlayer) &&
												otherBelow.equals(m_currentPlayer))
										{
											owner = PlayerID.NULL_PLAYERID; 
											m_kingX = -1;
											m_kingY = -1;
										}
											
									} 
									// Can a pawn be captured?
									else
									{
										//System.out.println("A " + s_owner.getName() + " pawn can be captured by moving from (" + move.getStart().getFirst() + "," + move.getStart().getSecond() + ") to (" + move.getEnd().getFirst() + "," + move.getEnd().getSecond() + ")");
										//captureIsPossible = true;
										owner = PlayerID.NULL_PLAYERID; 
									}

								}
							}
							else if (squareX==endX-1)
							{
								//GameSquare other = get(move.getEnd().getX(), move.getEnd().getY()+2);
								PlayerID other = state.get(endX-2, endY);
								//if (other!=null && other.getOwner().equals(m_currentPlayer))
								if (other!=null && other.equals(movingPlayer))
								{
									// Can the king be captured?
									if (squareX==m_kingX && squareY==m_kingY)
									{	System.out.println("Possible king capture with king at (" + squareX + "," + squareY + ")");
										PlayerID otherAbove = state.get(endX-1, endY-1);
										PlayerID otherBelow = state.get(endX-1, endY+1);
										
										if (otherAbove!=null && otherBelow!=null && 
												otherAbove.equals(m_currentPlayer) &&
												otherBelow.equals(m_currentPlayer))
										{
											owner = PlayerID.NULL_PLAYERID; 
											m_kingX = -1;
											m_kingY = -1;
										}
											
									} 
									// Can a pawn be captured?
									else
									{
										//System.out.println("A " + s_owner.getName() + " pawn can be captured by moving from (" + move.getStart().getFirst() + "," + move.getStart().getSecond() + ") to (" + move.getEnd().getFirst() + "," + move.getEnd().getSecond() + ")");
										//captureIsPossible = true;
										owner = PlayerID.NULL_PLAYERID; 
									}

								}
							}
						}
						
					}
					//square.put(s.getX()*(m_x-1) + s.getY(), new GameSquare(s.getX(), s.getY(), owner));
					//squareOwner.put(squareX*(m_x-1) + squareY, owner);
					squareOwner.put(squareX*m_x + squareY, owner);
				}
			}
			
			m_currentPlayer = state.m_otherPlayer;
			m_otherPlayer = state.m_currentPlayer;
		}
		
		public PlayerID get(int x, int y)
		{
			//System.out.println("trying " + ((m_x-1)*x + y));
			//try 
			//{
				//return squareOwner.get(((m_x-1)*x + y));
			return squareOwner.get((m_x*x + y));
			//} catch (IndexOutOfBoundsException e) 
			//{
			//	return null;
			//}
		}
	
		public float getUtility()
		{
			// if the king has been captured...
			if (m_kingX==-1 || m_kingY==-1)
			{
				//System.out.println("I see an endgame where black wins!");
				return Integer.MIN_VALUE;
			}
			
			// or if the king is in the corner...
			else if ((m_kingX==0 && (m_kingY==0 || m_kingY==m_y)) ||
					(m_kingX==m_x && (m_kingY==0 || m_kingY==m_y)))
			{
				//System.out.println("I see an endgame where white wins!");
				return Integer.MAX_VALUE;
			}
			
			// otherwise...
			else 
			{
				float numPieces = 0;
				float numOpponentPieces = 0;
				
				//int c = 0;
				for (PlayerID p : squareOwner.values())
				{	//c++;
					if (!p.equals(PlayerID.NULL_PLAYERID))
					{
						if (p.equals(m_id))
							numPieces += 1.0;
						else 
							numOpponentPieces += 1.0;
					}
				}
				//System.out.println("there are " + c + " squares, and " + numPieces + " and " + numOpponentPieces);
				
				if (numPieces==0)
				{
					if (m_kingPlayer)
						return Integer.MIN_VALUE;
					else
						return Integer.MAX_VALUE;
					
				}
				
				if (numOpponentPieces==0)
				{
					if (m_kingPlayer)
						return Integer.MAX_VALUE;
					else
						return Integer.MIN_VALUE;
				}
				
				if (m_kingPlayer)
					//return numPieces;
					return numPieces / numOpponentPieces;
				else
					//return numOpponentPieces;
					return numOpponentPieces / numPieces;
				
			}
			
			
		}
		
		public boolean gameIsOver()
		{
			if ((m_kingX==-1 || m_kingY==-1) ||
					(m_kingX==0 && (m_kingY==0 || m_kingY==m_y)) ||
					(m_kingX==m_x && (m_kingY==0 || m_kingY==m_y)))
				return true;
			else
				return false;
		}
		
		
		public boolean cutoffTest()
		{
			if ((m_kingX==-1 || m_kingY==-1) ||
					(m_kingX==0 && (m_kingY==0 || m_kingY==m_y)) ||
					(m_kingX==m_x && (m_kingY==0 || m_kingY==m_y)))
				return true;
			else if (depth > cutoffDepth)
				return true;
			else
				return false;
		}
		
		public Iterator<PlayerID> iterator()
		{
			return squareOwner.values().iterator();
		}
	}
	
	class GameSquare {
		private int m_x;
		private int m_y;
		private PlayerID m_owner;
		
		GameSquare(int x, int y, PlayerID owner)
		{
			m_x = x;
			m_y = y;
			m_owner = owner;
		}
		
		int getX() { return m_x; }
		int getY() { return m_y; }
		PlayerID getOwner() { return m_owner; }
		boolean isEmpty() { return m_owner.equals(PlayerID.NULL_PLAYERID); }
		
		public String toString() { return "(" + m_x + "," + m_y + ")";}
	}
	
	class Move {
		private Pair<Integer,Integer> m_start;
		private Pair<Integer,Integer> m_end;
		
		public Move(Pair<Integer,Integer> start, Pair<Integer,Integer> end)
		{
			m_start = start;
			m_end = end;
		}
		
		public Pair<Integer,Integer> getStart() { return m_start; }
		public Pair<Integer,Integer> getEnd() { return m_end; }
		
		
		/*
		private int m_startX;
		private int m_startY;
		private int m_endX;
		private int m_endY;
		
		Move(int startX, int startY, int endX, int endY)
		{
			m_startX = startX;
			m_startY = startY;
			m_endX = endX;
			m_endY = endY;
		}
		
		int getStartX() { return m_startX; }
		int getStartY() { return m_startY; }
		int getEndX() { return m_endX; }
		int getEndY() { return m_endY; }
		*/
	}
	
	class Pair<First,Second> {
		private First m_first;
		private Second m_second;
		
		Pair(First first, Second second)
		{
			m_first = first;
			m_second = second;
		}
		
		First getFirst() { return m_first; }
		Second getSecond() { return m_second; }
	}
}
