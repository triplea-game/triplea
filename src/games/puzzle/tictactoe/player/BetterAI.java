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
import games.strategy.engine.data.Unit;
import games.strategy.engine.gamePlayer.IPlayerBridge;
//import games.puzzles.tictactoe.attachments.PlayerAttachment;
import games.puzzle.tictactoe.delegate.remote.IPlayDelegate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

public class BetterAI extends AbstractAI {

	private int m_xDimension;
	private int m_yDimension;
	
    private PlayerID m_opponent;
    private PlayerID m_player;
    
    
	private int cutoffDepth = 1;
	
	public BetterAI(String name)
    {
        super(name);

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
/*
        m_minPlayer = m_id;
        m_maxPlayer = null;
        for (PlayerID p : m_bridge.getGameData().getPlayerList().getPlayers()) 
        {
            if (!p.equals(m_minPlayer) && !p.equals(PlayerID.NULL_PLAYERID))
            {
                m_maxPlayer = p;
                break;
            }
        }
        */

    }
    
	@Override
	protected void play() {

		GameState initial_state = getInitialState();
		//Move move = minimaxDecision(initial_state);
		try {
			//Move move = alphaBetaSearch(initial_state);
            Move move = minimaxSearch(initial_state);
			System.out.println(m_player.getName() + " should play at (" + move.getX() + "," +move.getY() + ")");

			IPlayDelegate playDel = (IPlayDelegate) m_bridge.getRemote();
			Territory start = m_bridge.getGameData().getMap().getTerritoryFromCoordinates(move.getX(), move.getY());
			
			playDel.play(start);
		} catch (OutOfMemoryError e)
		{
			System.out.println(counter);
			System.exit(-1);
		}
		
	}

	
	
	private GameState getInitialState()
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
		//System.out.println("Current player is " + currentPlayer.getName());
		//System.out.println("Other player is " + otherPlayer.getName());
		
		//System.out.println(m_maxX + " " + m_maxY);
		
		//return new GameState(currentPlayer, otherPlayer, m_maxX, m_maxY, m_bridge.getGameData().getMap().getTerritories());
        return new GameState(m_bridge.getGameData().getMap().getTerritories());

	}
	
	/*
	private boolean gameIsOver(GameState state)
	{
		return state.gameIsOver();
	}
	*/
	/*
	private Collection<Pair<Move, GameState>> successor(GameState state)//, PlayerID player)
	{

	    Collection<Pair<Move, GameState>> successors = new ArrayList<Pair<Move, GameState>>();

	    for (int x=0; x<m_xDimension; x++)
	    {
	        for (int y=0; y<m_yDimension; y++)
	        {
	            if (state.get(x, y).equals(PlayerID.NULL_PLAYERID))
	            {
	                Move play = new Move(x,y);
	                successors.add(new Pair<Move,GameState>(play,new GameState(play,state)));
	            }
	        }
	    }
	
        //System.out.println("There are " + successors.size() + " possible moves at this point");
		return successors;
	}
    */

    private Collection<GameState> successorStates(GameState state)//, PlayerID player)
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
    
        //System.out.println("There are " + successors.size() + " possible moves at this point");
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
	    Pair<Float,Move> m = maxValue(state, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY);
	    System.out.println("score of move is " + m.getFirst());
	    return m.getSecond();

	}
	
	private Move minimaxSearch(GameState state)
    {
        Pair<Float,Move> m = maxValue(state);
        System.out.println("score of move is " + m.getFirst());
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
		//if (state.gameIsOver())
		//if (state.cutoffTest())
		//	return new Pair<Float,Move>(state.getUtility(),null);
        //    return new Pair<Float,Move>(state.getUtility(),state.getMove());
		
		float value = Float.NEGATIVE_INFINITY;
		Move bestMove = null;
		
		/*for (Pair<Move, GameState> move_state : successor(state))//, state.m_otherPlayer))
		{
			GameState s = move_state.getSecond();
			Move a = move_state.getFirst();
		*/
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
		//if (state.captureIsPossible)
		//	System.out.println("capture is possible with score " + state.getUtility());
		//if (state.gameIsOver())
		//if (state.cutoffTest())
		//{	//System.out.println("cutting off at depth " + state.depth);
		//	return new Pair<Float,Move>(state.getUtility(),null);
        //    return new Pair<Float,Move>(state.getUtility(),state.getMove());
		//}
		float value = Float.POSITIVE_INFINITY;
		Move bestMove = null;
		
        /*
		for (Pair<Move, GameState> move_state : successor(state))//, state.m_otherPlayer))
		{	
			GameState s = move_state.getSecond();
			Move a = move_state.getFirst();
		*/
        for (GameState s : successorStates(state))
        {   Move a = s.getMove();
			float maxValue;
            if (s.gameIsOver())
                maxValue = s.getUtility();
            else
                maxValue = maxValue(s, alpha, beta).getFirst();
			if (maxValue < value)
			{	//System.out.println("best so far is " + maxValue);
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
		//private int m_xDimension;
		//private int m_yDimension;
		
		//private PlayerID m_currentPlayer;
		//private PlayerID m_otherPlayer;
		private int depth;
        
        private Move m_move;
		
        /*
		public GameState(PlayerID currentPlayer, PlayerID otherPlayer, int x, int y, Collection<Territory> territories)
		{
			depth=0;
			counter++;
			m_xDimension = x;
			m_yDimension = y;
            m_move = null;
			m_currentPlayer = currentPlayer;//otherPlayer;
			m_otherPlayer = otherPlayer;//currentPlayer;
			
			squareOwner = new HashMap<Integer,PlayerID>(x*y);
			
			for (Territory t : territories) 
			{
				squareOwner.put((t.getX()*x + t.getY()), t.getOwner());
			}
			
		}
		*/
        
        public GameState(Collection<Territory> territories)
        {
            depth=0;
            
            squareOwner = new HashMap<Integer,PlayerID>(m_xDimension*m_yDimension);
            
            for (Territory t : territories) 
            {
                squareOwner.put((t.getX()*m_xDimension + t.getY()), t.getOwner());
            }            
        }
        
		//public boolean captureIsPossible = false;
        
		public GameState(Move move, GameState parentState)
		{
			depth = parentState.depth+1;
            if (depth > 9)
                throw new RuntimeException("We should never be this deep");

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
            
            /*
			m_currentPlayer = state.m_currentPlayer;
			m_otherPlayer = state.m_otherPlayer;
			
			m_xDimension = state.m_xDimension;//move.getX();
			m_yDimension = state.m_yDimension;//move.getY();
			*/
			//square = new HashMap<Integer,GameSquare>(state.square.size());
			//squareOwner = new HashMap<Integer,PlayerID>(state.squareOwner.size());
			/*
			for (Entry<Integer,PlayerID> s : state.squareOwner.entrySet())
			{
				int squareX = s.getKey() / m_xDimension;//(m_x-1);
				int squareY = s.getKey() % m_xDimension;//(m_x-1);
				PlayerID s_owner = s.getValue();
				
				if (squareX==move.getX() && squareY==move.getY())
				{
					//square.put(s.getX()*(m_x-1) + s.getY(), new GameSquare(s.getX(), s.getY(), PlayerID.NULL_PLAYERID));
					//squareOwner.put(squareX*(m_x-1) + squareY, PlayerID.NULL_PLAYERID);
					squareOwner.put(squareX*m_xDimension + squareY, playerPerformingMove);
					//System.out.println("Player " + m_currentPlayer + " can play in (" + squareX + "," + squareY + ")");
				} 
				else
				{
                    squareOwner.put(squareX*m_xDimension + squareY, s_owner);
                }
			}
			
			m_currentPlayer = state.m_otherPlayer;
			m_otherPlayer = state.m_currentPlayer;
            */
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
//            if (gameIsOver())
  //              System.out.println("game could be over with score " + getUtility());
            
            for (int y=0; y<m_yDimension; y++)
            {
                PlayerID player = get(0,y);//squareOwner.get(m_xDimension*0 + y);
                //System.out.println("Player " + player.getName() + " has (0," + y + ")");
                if (!player.equals(PlayerID.NULL_PLAYERID))
                {
                    for (int x=0; x<m_xDimension; x++)
                    {
                        if (!player.equals(get(x,y)))//squareOwner.get(m_xDimension*x + y)))
                        {
//                            System.out.println("But player " + get(x,y).getName() + " has ("+ x + "," + y + ")");
                            player = null;
                            break;
                        }
                    }
                    
                    // If player!=null, then player is the winner
                    if (player != null)
                        if (player==m_player)
                        {   //System.out.println("game could be over with score 1");
                            return 1;
                        }
                        else if (player==m_opponent)
                        {   //System.out.println("game could be over with score -1");
                            return -1;
                        }
                        //else System.out.println("**********BAD**************");
                }
            }
            
            
            for (int x=0; x<m_xDimension; x++)
            {
                PlayerID player = get(x,0);//squareOwner.get(m_xDimension*x + 0);
                if (!player.equals(PlayerID.NULL_PLAYERID))
                {
                    for (int y=0; y<m_yDimension; y++)
                    {
                        if (!player.equals(get(x,y)))//squareOwner.get(m_xDimension*x + y)))
                        {
                            player = null;
                            break;
                        }
                    }
                    if (player != null)
                        if (player==m_player)
                        {   //System.out.println("game could be over with score 1");
                            return 1;
                        }
                        else if (player==m_opponent)
                        {   //System.out.println("game could be over with score -1");
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
                        if (player==m_player)
                        {   //System.out.println("game could be over with score 1");
                            return 1;
                        }
                        else if (player==m_opponent)
                        {   //System.out.println("game could be over with score -1");
                            return -1;
                        }
                }
            }
            
            {
                PlayerID player = get(m_xDimension-1,m_yDimension-1);
                if (!player.equals(PlayerID.NULL_PLAYERID))
                {
                    int y = 0;
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
                        if (player==m_player)
                        {   //System.out.println("game could be over with score 1");
                            return 1;
                        }
                        else if (player==m_opponent)
                        {   //System.out.println("game could be over with score -1");
                            return -1;
                        }
                }
            }
            
            //System.out.println("Depth is " + depth + " and no end in sight");
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
		
		/*
		public boolean cutoffTest()
		{
			if (depth > cutoffDepth)
				return true;
            else if (gameIsOver())
                return true;
			else
				return false;
		}
		
		public Iterator<PlayerID> iterator()
		{
			return squareOwner.values().iterator();
		}
        */
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
        private int m_x;
        private int m_y;
		//private Pair<Integer,Integer> m_start;
		//private Pair<Integer,Integer> m_end;
		
		public Move(int x, int y)
		{
			m_x = x;
            m_y = y;
		}
		
		public int getX() { return m_x; }
        public int getY() { return m_y; }
		
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
