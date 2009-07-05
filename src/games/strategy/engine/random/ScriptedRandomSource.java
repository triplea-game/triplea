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
package games.strategy.engine.random;

import java.util.StringTokenizer;

/**
 * A random source for use while debugging.<p> 
 * 
 * Returns the random numbers designated in the system property triplea.scriptedRandom<p>
 * 
 * for example, to roll 1,2,3 use -Dtriplea.scriptedRandom=1,2,3<p>
 * 
 * When scripted random runs out of numbers, the numbers will repeat.<p>
 * 
 * Special characters are also allowed in the sequence.
 * 
 * e - the random source will throw an error
 * p - the random source will pause and never return.
 *
 *
 * @author Sean Bridges
 */
public class ScriptedRandomSource implements IRandomSource
{
    public static final int PAUSE = -2;
    public static final int ERROR = -3;
    
    private static final String SCRIPTED_RANDOM_PROPERTY = "triplea.scriptedRandom";
 

    private final int[] m_numbers;
    private int m_currentIndex = 0;
    
    /**
     * Should we use a scripted random sourcce.
     */
    public static boolean useScriptedRandom()
    {
        return System.getProperty(SCRIPTED_RANDOM_PROPERTY) != null &&
               System.getProperty(SCRIPTED_RANDOM_PROPERTY).trim().length() > 0 ;
    }
    
    /**
     * Create a scripted random source from the system property triplea.scriptedRandom.
     */
    public ScriptedRandomSource()
    {
        String property = System.getProperty(SCRIPTED_RANDOM_PROPERTY, "1,2,3");
        int length = property.split(",").length ;
        StringTokenizer tokenizer = new StringTokenizer(property, ",");
        m_numbers = new int[length];
        
        for(int i = 0; i < m_numbers.length; i++)
        {
            String token = tokenizer.nextToken();
            if(token.equals("e"))
            {
                m_numbers[i] = ERROR;
            }
            else if(token.equals("p"))
            {
                m_numbers[i] = PAUSE;
            }
            else
            {
                m_numbers[i] = Integer.parseInt(token) -1;
            }
        }
    }
    
    /**
     * Create a scripted random from the given numbers.  The scripted random will return
     * the numbers supplied in order.  When the scripted source runs out of random numbers, it 
     * starts returning elements from the beginning.
     */
    public ScriptedRandomSource(int[] numbers)
    {
        m_numbers = numbers;
    }
    
    public ScriptedRandomSource(Integer... numbers)
    {
        m_numbers = new int[numbers.length];
        for(int i =0; i < numbers.length; i++) {
            m_numbers[i] = numbers[i];
        }
    }
    
    public int getRandom(int max, String annotation)
    {
        return getRandom(max,1,null)[0];
    }

    public int[] getRandom(int max, int count, String annotation)
    {
        int[] rVal = new int[count];
        for(int i = 0; i <count; i++)
        {
            if(m_numbers[m_currentIndex] == PAUSE)
            {
                try
                {
                    Object o = new Object();
                    synchronized(o)
                    {
                        o.wait();
                    }
                } catch (InterruptedException e)
                {
                }
            }
            if(m_numbers[m_currentIndex] == ERROR)
            {
                throw new IllegalStateException("Random number generator generating scripted error");
            }
            
            rVal[i] = m_numbers[m_currentIndex];
            m_currentIndex ++;
            if(m_currentIndex >= m_numbers.length)
                m_currentIndex = 0;
        }
        return rVal;
    }
}
