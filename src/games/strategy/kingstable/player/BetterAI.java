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

import games.strategy.common.player.ai.AIAlgorithm;
import games.strategy.common.player.ai.GameState;
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

	private int m_xDimension;
	private int m_yDimension;
	private boolean m_kingPlayer;
	
    private PlayerID m_opponent;
    private PlayerID m_player;
    
    /** Algorithms available for use in BetterAI */
    public enum Algorithm { MINIMAX, ALPHABETA }
    
    // The algorithm to be used
    private final Algorithm algorithm;
    
    // Default to a search depth of 2-ply
	private int cutoffDepth = 2;
	
    public BetterAI(String name, Algorithm algorithm)
    {
        super(name);
        this.algorithm = algorithm;

    }
	
    public void initialize(IPlayerBridge bridge, PlayerID id)
    {
    	super.initialize(bridge, id);
    	
    	m_xDimension = m_bridge.getGameData().getMap().getXDimension();
    	m_yDimension = m_bridge.getGameData().getMap().getYDimension();

    	PlayerAttachment pa = (PlayerAttachment) m_id.getAttachment("playerAttachment");
    	if (pa!=null)
    	{
    	    if (pa.needsKing())
                m_kingPlayer = true;
            
            cutoffDepth = pa.getAlphaBetaSearchDepth();
        }	
    	else
    		m_kingPlayer = false;
        
        m_player = m_id;
        m_opponent = null;
        for (PlayerID p : m_bridge.getGameData().getPlayerList().getPlayers()) 
        {
            if (!p.equals(m_player) && !p.equals(PlayerID.NULL_PLAYERID))
            {
                m_opponent = p;
                break;
            }
        }
    }
    
	@Override
	protected void play() {

		State initial_state = getInitialState();
		//Move move = minimaxDecision(initial_state);
		try {
			Move move;
            if (algorithm.equals(Algorithm.ALPHABETA))
                move = AIAlgorithm.alphaBetaSearch(initial_state);
            else
                move = AIAlgorithm.minimaxSearch(initial_state);
			
            //System.out.println(m_id.getName() + " should move from (" + move.getStart().getFirst() + "," +move.getStart().getSecond() + ") to (" + move.getEnd().getFirst()+ "," +move.getEnd().getSecond() + ")");

			IPlayDelegate playDel = (IPlayDelegate) m_bridge.getRemote();
			Territory start = m_bridge.getGameData().getMap().getTerritoryFromCoordinates(move.getStart().getFirst(), move.getStart().getSecond());
			Territory end = m_bridge.getGameData().getMap().getTerritoryFromCoordinates(move.getEnd().getFirst(), move.getEnd().getSecond());
			
			playDel.play(start,end);
		} catch (OutOfMemoryError e)
		{
			System.out.println("Ran out of memory while searching for next move: " +counter + " moves examined.");
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
		return new State(m_bridge.getGameData().getMap().getTerritories());
		//return new State(currentPlayer, otherPlayer, m_xDimension, m_yDimension, m_bridge.getGameData().getMap().getTerritories());

	}
	public static int counter = 0;
	
	class State extends GameState<Move> //implements Iterable<PlayerID> 
	{
		
		//private HashMap<Integer,GameSquare> square;
		private HashMap<Integer,PlayerID> squareOwner;
		//private int m_x;
		//private int m_y;
		
		private int m_kingX = -1;
		private int m_kingY = -1;
		
		//private PlayerID m_currentPlayer;
		//private PlayerID m_otherPlayer;
		private int m_depth;
		
        private final Move m_move;
        
        public State(Collection<Territory> territories)
        {
            m_depth = 0;
            m_move = null;

            m_playerPerformingMove = m_opponent;
            m_otherPlayer = m_player;

            
            squareOwner = new HashMap<Integer,PlayerID>(m_xDimension*m_yDimension);
            
            for (Territory t : territories) 
            {
                squareOwner.put((t.getX()*m_xDimension + t.getY()), t.getOwner());
                
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
        }
        
        /*
		public State(PlayerID currentPlayer, PlayerID otherPlayer, int x, int y, Collection<Territory> territories)
		{
			depth=0;
            m_move = null;
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
        */
		
		//public boolean captureIsPossible = false;
		
        public State getSuccessor(Move move)
        {
            return new State(move, this);            
        }
        
        public Move getMove() 
        {
            return m_move;
        }
        
        private final PlayerID m_playerPerformingMove, m_otherPlayer;
        
		private State(Move move, State parentState)
		{
            m_move = move;
			m_depth = parentState.m_depth+1;
			counter++;

			
			int startX = move.getStart().getFirst();
			int startY = move.getStart().getSecond();
			
			int endX = move.getEnd().getFirst();
			int endY = move.getEnd().getSecond();
			
			if (startX==parentState.m_kingX && startY==parentState.m_kingY)
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
            if (parentState.m_depth%2==0)
            {
                m_playerPerformingMove = m_player;
                m_otherPlayer = m_opponent;
            }
            else
            {
                m_playerPerformingMove = m_opponent;
                m_otherPlayer = m_player;
            }

            
            // Clone the map from the parent state
            squareOwner = new HashMap<Integer,PlayerID>(parentState.squareOwner.size());
            for (Entry<Integer,PlayerID> s : parentState.squareOwner.entrySet())
                squareOwner.put(s.getKey(), s.getValue());

            // Now enter the new move
            squareOwner.put(move.getStart().getFirst()*m_xDimension + move.getStart().getSecond(), PlayerID.NULL_PLAYERID);
            squareOwner.put(move.getEnd().getFirst()*m_xDimension + move.getEnd().getSecond(), m_playerPerformingMove);
            
            // Now check for captures
            checkForCaptures(move.getEnd().getFirst(), move.getEnd().getSecond());
			
			
			//m_currentPlayer = state.m_otherPlayer;
			//m_otherPlayer = state.m_currentPlayer;
		}
		
        
        private void checkForCaptures(int endX, int endY)
        {
            for (Entry<Integer,PlayerID> s : squareOwner.entrySet())
            {
                int squareX = s.getKey() / m_xDimension;//(m_x-1);
                int squareY = s.getKey() % m_xDimension;//(m_x-1);
                PlayerID s_owner = s.getValue();

                //PlayerID owner = s_owner;

                if (s_owner.equals(m_otherPlayer))
                {   
                    if (squareX==endX)
                    {
                        if (squareY==endY+1)
                        {
                            PlayerID above = get(endX, endY+2);
                            if (above!=null && above.equals(m_playerPerformingMove))
                            {
                                // Can the king be captured?
                                if (squareX==m_kingX && squareY==m_kingY)
                                {   
                                    System.out.println("Possible king capture with king at (" + squareX + "," + squareY + ")");
                                    PlayerID left = get(endX-1, endY+1);
                                    PlayerID right = get(endX+1, endY+1);

                                    if (left!=null && right!=null && 
                                            left.equals(m_playerPerformingMove) &&
                                            right.equals(m_playerPerformingMove))
                                    {
                                        squareOwner.put(squareX*m_xDimension + squareY, PlayerID.NULL_PLAYERID);

                                        m_kingX = -1;
                                        m_kingY = -1;
                                    }

                                } 
                                // Can a pawn be captured?
                                else
                                {
                                    squareOwner.put(squareX*m_xDimension + squareY, PlayerID.NULL_PLAYERID);
                                }

                            }
                        }
                        else if (squareY==endY-1)
                        {
                            PlayerID below = get(endX, endY-2);
                            if (below!=null && below.equals(m_playerPerformingMove))
                            {
                                // Can the king be captured?
                                if (squareX==m_kingX && squareY==m_kingY)
                                {   
                                    System.out.println("Possible king capture with king at (" + squareX + "," + squareY + ")");
                                    PlayerID left = get(endX-1, endY-1);
                                    PlayerID right = get(endX+1, endY-1);

                                    if (left!=null && right!=null && 
                                            left.equals(m_playerPerformingMove) &&
                                            right.equals(m_playerPerformingMove))
                                    {
                                        squareOwner.put(squareX*m_xDimension + squareY, PlayerID.NULL_PLAYERID);

                                        m_kingX = -1;
                                        m_kingY = -1;
                                    }

                                } 
                                // Can a pawn be captured?
                                else
                                {
                                    squareOwner.put(squareX*m_xDimension + squareY, PlayerID.NULL_PLAYERID);
                                }

                            }
                        }
                    }

                    if (endY==squareY)
                    {
                        if (squareX==endX+1)
                        {
                            PlayerID right = get(endX+2, endY);
                            if (right!=null && right.equals(m_playerPerformingMove))
                            {
                                // Can the king be captured?
                                if (squareX==m_kingX && squareY==m_kingY)
                                {   
                                    System.out.println("Possible king capture with king at (" + squareX + "," + squareY + ")");
                                    PlayerID above = get(endX+1, endY-1);
                                    PlayerID below = get(endX+1, endY+1);

                                    if (above!=null && below!=null && 
                                            above.equals(m_playerPerformingMove) &&
                                            below.equals(m_playerPerformingMove))
                                    {
                                        squareOwner.put(squareX*m_xDimension + squareY, PlayerID.NULL_PLAYERID);

                                        m_kingX = -1;
                                        m_kingY = -1;
                                    }

                                } 
                                // Can a pawn be captured?
                                else
                                {
                                    squareOwner.put(squareX*m_xDimension + squareY, PlayerID.NULL_PLAYERID);
                                }

                            }
                        }
                        
                        else if (squareX==endX-1)
                        {
                            PlayerID left = get(endX-2, endY);
                            if (left!=null && left.equals(m_playerPerformingMove))
                            {
                                // Can the king be captured?
                                if (squareX==m_kingX && squareY==m_kingY)
                                {   
                                    System.out.println("Possible king capture with king at (" + squareX + "," + squareY + ")");
                                    PlayerID above = get(endX-1, endY-1);
                                    PlayerID below = get(endX-1, endY+1);

                                    if (above!=null && below!=null && 
                                            above.equals(m_playerPerformingMove) &&
                                            below.equals(m_playerPerformingMove))
                                    {
                                        squareOwner.put(squareX*m_xDimension + squareY, PlayerID.NULL_PLAYERID);

                                        m_kingX = -1;
                                        m_kingY = -1;
                                    }

                                } 
                                // Can a pawn be captured?
                                else
                                {
                                    squareOwner.put(squareX*m_xDimension + squareY, PlayerID.NULL_PLAYERID);
                                }

                            }
                        }
                    }

                }
            }            
        }
        

        private boolean isKingsSquare(int x, int y)
        {
            if (x==5 && y==5)
                return true;
            else if ((x==0 && (y==0 || y==m_yDimension-1)) ||
                    (x==m_xDimension-1 && (y==0 || y==m_yDimension-1)))
                return true;
            else
                return false;
        }
        
        public Collection<GameState> successors()
        {
            PlayerID successorPlayer = m_otherPlayer;//m_playerPerformingMove;//m_otherPlayer;
            
            Collection<GameState> successors = new ArrayList<GameState>();
            int countCurrentPlayerPieces = 0;
            for (Entry<Integer,PlayerID> start : this.squareOwner.entrySet())
            {
                PlayerID s_owner = start.getValue();
                
                // Only consider squares that player owns
                if (successorPlayer.equals(s_owner))
                {
                    countCurrentPlayerPieces++;
                    
                    int startX = start.getKey() / m_xDimension;//(m_maxX-1);
                    int startY = start.getKey() % m_xDimension;//(m_maxX-1);
                    
                    boolean kingIsMoving;
                    if (startX==m_kingX && startY==m_kingY)
                        kingIsMoving = true;
                    else
                        kingIsMoving = false;
                    
                    //System.out.println(s_owner.getName() + " could move from (" + startX + "," + startY + ") key==" + start.getKey());
                    for (int x=startX-1; x>=0; x--)
                    {
                        //if (x==0 && startY==0 && startX==2)
                        //  System.out.println("Possible dest = (" + x + "," + startY + ")");
                        PlayerID destination = this.get(x, startY);
                        if (destination.equals(PlayerID.NULL_PLAYERID))
                        {   
                            if (kingIsMoving || !isKingsSquare(x,startY))
                            {
                                Move move = new Move(new Pair<Integer,Integer>(startX,startY), new Pair<Integer,Integer>(x,startY));
                                successors.add(new State(move,this));
                            }
                            /*
                            State newstate = new State(move,this);
                            successors.add(newstate);
                            if (startX==m_kingX && startY==m_kingY)
                                System.out.println("King may be moving from (" + startX + "," + startY + ") to (" + newstate.m_kingX + "," + newstate.m_kingY + ") : utility of this would be " + newstate.getUtility());
                        */
                        }
                        else
                            break;
                    }
                    
                    for (int x=startX+1; x<m_xDimension; x++)
                    {
                        //System.out.println("Possible dest = (" + x + "," + startY + ")");
                        PlayerID destination = this.get(x, startY);
                        if (destination.equals(PlayerID.NULL_PLAYERID))
                        {
                            if (kingIsMoving || !isKingsSquare(x,startY))
                            {
                                Move move = new Move(new Pair<Integer,Integer>(startX,startY), new Pair<Integer,Integer>(x,startY));
                                successors.add(new State(move,this));
                            }
                        }
                        else
                            break;
                    }
                    
                    for (int y=startY-1; y>=0; y--)
                    {
                        //System.out.println("Possible dest = (" + startX + "," + y + ")");
                        PlayerID destination = this.get(startX, y);
                        if (destination.equals(PlayerID.NULL_PLAYERID))
                        {
                            if (kingIsMoving || !isKingsSquare(startX,y))
                            {
                                Move move = new Move(new Pair<Integer,Integer>(startX,startY), new Pair<Integer,Integer>(startX,y));
                                successors.add(new State(move,this));
                            }
                        }
                        else
                            break;
                        /*
                        GameSquare destination = this.get(startX, y);
                        if (destination.isEmpty())
                        {
                            Move move = new Move(start, destination);
                            successors.add(new Pair<Move,GameState>(move, new GameState(move,state)));
                        }
                        else
                            break;
                         */
                    }
                    
                    for (int y=startY+1; y<m_yDimension; y++)
                    {
                        //System.out.println("Possible dest = (" + startX + "," + y + ")");
                        PlayerID destination = this.get(startX, y);
                        if (destination.equals(PlayerID.NULL_PLAYERID))
                        {
                            if (kingIsMoving || !isKingsSquare(startX,y))
                            {
                                Move move = new Move(new Pair<Integer,Integer>(startX,startY), new Pair<Integer,Integer>(startX,y));
                                successors.add(new State(move,this));
                            }
                        }
                        else
                            break;
                        /*GameSquare destination = this.get(startX, y);
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
        
        
		public PlayerID get(int x, int y)
		{
			//System.out.println("trying " + ((m_x-1)*x + y));
			//try 
			//{
				//return squareOwner.get(((m_x-1)*x + y));
			return squareOwner.get((m_xDimension*x + y));
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
                if (m_kingPlayer)
                    return Integer.MIN_VALUE;
                else
                    return Integer.MAX_VALUE;
			}
			
			// or if the king is in the corner...
			else if ((m_kingX==0 && (m_kingY==0 || m_kingY==m_yDimension-1)) ||
					(m_kingX==m_xDimension-1 && (m_kingY==0 || m_kingY==m_yDimension-1)))
			{
				//System.out.println("I see an endgame where white wins!");
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
                    return Integer.MIN_VALUE;
                    /*
					if (m_kingPlayer)
						return Integer.MIN_VALUE;
					else
						return Integer.MAX_VALUE;
					*/
				}
				
				if (numOpponentPieces==0)
				{
                    return Integer.MAX_VALUE;
                    /*
					if (m_kingPlayer)
						return Integer.MAX_VALUE;
					else
						return Integer.MIN_VALUE;
                        */
				}
				
                return numPieces / numOpponentPieces;
                /*
				if (m_kingPlayer)
					//return numPieces;
					return numPieces / numOpponentPieces;
				else
					//return numOpponentPieces;
					return numOpponentPieces / numPieces;
				*/
			}
			
			
		}
		
		public boolean gameIsOver()
		{
			if ((m_kingX==-1 || m_kingY==-1) ||
					(m_kingX==0 && (m_kingY==0 || m_kingY==m_yDimension-1)) ||
					(m_kingX==m_xDimension-1 && (m_kingY==0 || m_kingY==m_yDimension-1)))
				return true;
			else
				return false;
		}
		
		
		public boolean cutoffTest()
		{
			/*if ((m_kingX==-1 || m_kingY==-1) ||
					(m_kingX==0 && (m_kingY==0 || m_kingY==m_yDimension)) ||
					(m_kingX==m_xDimension && (m_kingY==0 || m_kingY==m_yDimension)))
				return true;*/
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
	/*
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
	*/
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
