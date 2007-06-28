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

package games.strategy.common.player.ai;

/**
 * Utility class implementing AI game algorithms.
 *
 * Currently, minimax and alpha-beta algorithms are implemented.
 * 
 * @author Lane Schwartz
 * @version $LastChangedDate$
 * @see "Chapter 6 of Artificial Intelligence, 2nd ed. by Stuart Russell & Peter Norvig"
 */
public class AIAlgorithm
{
    /**
     * Find the optimal next play to perform from the given game state, 
     * using the alpha-beta algorithm.
     * 
     * @param <Play> class capable of representing a game play
     * @param state current game state
     * @return the optimal next play
     */
    public static <Play> Play alphaBetaSearch(GameState<Play> state)
    {
        Pair<Float,Play> m = maxValue(state, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY);
        return m.getSecond();

    }
    
    /**
     * Find the optimal next play to perform from the given game state, 
     * using the minimax algorithm.
     * 
     * @param <Play> class capable of representing a game play
     * @param state current game state
     * @return the optimal next play
     */
    public static <Play> Play minimaxSearch(GameState<Play> state)
    {
        Pair<Float,Play> m = maxValue(state);
        return m.getSecond();
    }
    
    
    private static <Play> Pair<Float,Play> maxValue(GameState<Play> state)
    {
        float value = Float.NEGATIVE_INFINITY;
        Play bestMove = null;

        for (GameState<Play> s : state.successors())
        {   
            Play a = s.getMove();
            
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
        
        return new Pair<Float,Play>(value,bestMove);
    }
    
    

    private static <Play> Pair<Float,Play> minValue(GameState<Play> state)
    {
        float value = Float.POSITIVE_INFINITY;
        Play bestMove = null;

        for (GameState<Play> s : state.successors())
        {   
            Play a = s.getMove();
            
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
        
        return new Pair<Float,Play>(value,bestMove);
    }
    
    
    
    private static <Play> Pair<Float,Play> maxValue(GameState<Play> state, float alpha, float beta)
    {
        float value = Float.NEGATIVE_INFINITY;
        Play bestMove = null;
        
        for (GameState<Play> s : state.successors())
        {   Play a = s.getMove();
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
                new Pair<Float,Play>(value,bestMove);
            if (value > alpha)
                alpha = value;
        }
        
        return new Pair<Float,Play>(value,bestMove);
    }
    
    
    private static <Play> Pair<Float,Play> minValue(GameState<Play> state, float alpha, float beta)
    {
        float value = Float.POSITIVE_INFINITY;
        Play bestMove = null;
        
        for (GameState<Play> s : state.successors())
        {   Play a = s.getMove();
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
                new Pair<Float,Play>(value,bestMove);
            if (value < beta)
                beta = value;
        }
        
        return new Pair<Float,Play>(value,bestMove);
    }
    

    static class Pair<First,Second> {
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
