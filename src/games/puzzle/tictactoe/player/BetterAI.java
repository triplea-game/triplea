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

package games.puzzle.tictactoe.player;

import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.gamePlayer.IPlayerBridge;
import games.puzzle.tictactoe.delegate.remote.IPlayDelegate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map.Entry;

public class BetterAI extends AbstractAI {

	private int m_xDimension;
	private int m_yDimension;
	
    private PlayerID m_opponent;
    private PlayerID m_player;
    
    public enum Algorithm { MINIMAX, ALPHABETA }
        
    private final Algorithm algorithm;
    
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
    
    
	protected void play() 
    {
	    GameState initial_state = getInitialState();

	    Move move;
        
        if (algorithm == Algorithm.MINIMAX)
            move = minimaxSearch(initial_state);
        else
            move = alphaBetaSearch(initial_state);

	    IPlayDelegate playDel = (IPlayDelegate) m_bridge.getRemote();
	    Territory start = m_bridge.getGameData().getMap().getTerritoryFromCoordinates(move.getX(), move.getY());

	    playDel.play(start);
	}

	
	private GameState getInitialState()
	{
        return new GameState(m_bridge.getGameData().getMap().getTerritories());
	}
	

    private Collection<GameState> successorStates(GameState state)
    {
        Collection<GameState> successors = new ArrayList<GameState>();

        for (int x=0; x<m_xDimension; x++)
        {
            for (int y=0; y<m_yDimension; y++)
            {
                if (state.get(x, y).equals(PlayerID.NULL_PLAYERID))
                {
                    Move play = new Move(x,y);
                    successors.add(new GameState(play,state));
                }
            }
        }
    
        return successors;
    }
    
    
	private Move alphaBetaSearch(GameState state)
	{
	    Pair<Float,Move> m = maxValue(state, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY);
	    //System.out.println("score of move is " + m.getFirst());
	    return m.getSecond();

	}
	
	private Move minimaxSearch(GameState state)
    {
        Pair<Float,Move> m = maxValue(state);
        return m.getSecond();
    }
    
    
    private Pair<Float,Move> maxValue(GameState state)
    {
        float value = Float.NEGATIVE_INFINITY;
        Move bestMove = null;

        for (GameState s : successorStates(state))
        {   
            Move a = s.getMove();
            
            float minValue;
            if (s.gameIsOver())
                minValue = s.getUtility();
            else
                minValue = minValue(s).getFirst();
            
            
            if (minValue > value)
            {
                value = minValue;
                bestMove = a;
            }
        }
        
        return new Pair<Float,Move>(value,bestMove);
    }
    
    

    private Pair<Float,Move> minValue(GameState state)
    {
        float value = Float.POSITIVE_INFINITY;
        Move bestMove = null;

        for (GameState s : successorStates(state))
        {   
            Move a = s.getMove();
            
            float maxValue;
            if (s.gameIsOver())
                maxValue = s.getUtility();
            else
                maxValue = maxValue(s).getFirst();
            
            if (maxValue < value)
            {   
                value = maxValue;
                bestMove = a;
            }
            
        }
        
        return new Pair<Float,Move>(value,bestMove);
    }
    
    
    
	private Pair<Float,Move> maxValue(GameState state, float alpha, float beta)
	{
		float value = Float.NEGATIVE_INFINITY;
		Move bestMove = null;
		
        for (GameState s : successorStates(state))
        {   Move a = s.getMove();
			float minValue;
            if (s.gameIsOver())
                minValue = s.getUtility();
            else
                minValue = minValue(s, alpha, beta).getFirst();
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
		float value = Float.POSITIVE_INFINITY;
		Move bestMove = null;
		
        for (GameState s : successorStates(state))
        {   Move a = s.getMove();
			float maxValue;
            if (s.gameIsOver())
                maxValue = s.getUtility();
            else
                maxValue = maxValue(s, alpha, beta).getFirst();
			if (maxValue < value)
			{	
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
	
	class GameState
	{
		private HashMap<Integer,PlayerID> squareOwner;
		private int depth;
        private Move m_move;
	
        public GameState(Collection<Territory> territories)
        {
            depth=0;
            
            squareOwner = new HashMap<Integer,PlayerID>(m_xDimension*m_yDimension);
            
            for (Territory t : territories) 
            {
                squareOwner.put((t.getX()*m_xDimension + t.getY()), t.getOwner());
            }            
        }
        
        
		public GameState(Move move, GameState parentState)
		{
			depth = parentState.depth+1;
            m_move = move;
            
            // The start state is at depth 0
            PlayerID playerPerformingMove;
            if (parentState.depth%2==0)
                playerPerformingMove = m_player;
            else
                playerPerformingMove = m_opponent;
            
            // Clone the map from the parent state
            squareOwner = new HashMap<Integer,PlayerID>(parentState.squareOwner.size());
            for (Entry<Integer,PlayerID> s : parentState.squareOwner.entrySet())
                squareOwner.put(s.getKey(), s.getValue());
            
            // Now enter the new move
            squareOwner.put(move.getX()*m_xDimension + move.getY(), playerPerformingMove);
            
		}
        
        public Move getMove()
        {
            return m_move;
        }
		
		public PlayerID get(int x, int y)
		{
			return squareOwner.get((m_xDimension*x + y));
		}
	
		public float getUtility()
		{
            for (int y=0; y<m_yDimension; y++)
            {
                PlayerID player = get(0,y);

                if (!player.equals(PlayerID.NULL_PLAYERID))
                {
                    for (int x=0; x<m_xDimension; x++)
                    {
                        if (!player.equals(get(x,y)))
                        {
                            player = null;
                            break;
                        }
                    }
                    
                    // If player!=null, then player is the winner
                    if (player != null)
                    {
                        if (player.equals(m_player))
                            return 1;
                        
                        else if (player.equals(m_opponent))
                            return -1;
                    }
                        
                }
            }
            
            
            // Look for a vertical win
            for (int x=0; x<m_xDimension; x++)
            {
                PlayerID player = get(x,0);
                if (!player.equals(PlayerID.NULL_PLAYERID))
                {
                    for (int y=0; y<m_yDimension; y++)
                    {
                        if (!player.equals(get(x,y)))
                        {
                            player = null;
                            break;
                        }
                    }
                    
                    if (player != null)
                    {
                        if (player.equals(m_player))
                            return 1;
                        
                        else if (player.equals(m_opponent))
                            return -1;
                    }
                }

            }          
            
            
            {
                PlayerID player = get(0,0);
                if (!player.equals(PlayerID.NULL_PLAYERID))
                {
                    for (int x=0; x<m_xDimension; x++)
                    {
                        int y = x;
                        if (!player.equals(get(x,y)))
                        {
                            player = null;
                            break;
                        }
                        
                    } 

                    if (player != null)
                    {
                        if (player.equals(m_player))
                            return 1;
                        
                        else if (player.equals(m_opponent))
                            return -1;
                    }
                }
            }
            
            {
                PlayerID player = get(m_xDimension-1,0);
                if (!player.equals(PlayerID.NULL_PLAYERID))
                {
                    int y = -1;
                    for (int x=m_xDimension-1; x>=0; x--)
                    {
                        y++; if (y>=m_yDimension) break;
                        
                        if (!player.equals(get(x,y)))
                        {
                            player = null;
                            break;
                        }                      
                    }   
                    
                    if (player != null)
                    {
                        if (player.equals(m_player))
                            return 1;
                        
                        else if (player.equals(m_opponent))
                            return -1;
                    }
                }
            }
            
            return 0;
		}
		
		public boolean gameIsOver()
		{
            if (getUtility()!=0)
                return true;
            else
            {
                for (PlayerID player : squareOwner.values())
                {
                    if (player.equals(PlayerID.NULL_PLAYERID))
                        return false;
                }
                
                return true;
            }
            
		}
        
        public String toString() 
        {
            String string = "";
            for (int y=0; y<m_yDimension; y++)
            {
                for (int x=0; x<m_xDimension; x++)
                {
                    String player = get(x,y).getName();
                    if (player.equals("X"))
                        string += "X ";
                    else if (player.equals("O"))
                        string += "O ";
                    else 
                        string += "_ ";
                }
                
                string += "\n";
            }
            return string;
        }
		
	}
    
	class Move {
        private int m_x;
        private int m_y;
		
		public Move(int x, int y)
		{
			m_x = x;
            m_y = y;
		}
		
		public int getX() { return m_x; }
        public int getY() { return m_y; }
        
        public String toString() { return "(" + m_x + "," + m_y + ")";}
		
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
