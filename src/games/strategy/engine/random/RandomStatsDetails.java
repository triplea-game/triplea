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

import games.strategy.util.IntegerMap;

import java.io.Serializable;



public class RandomStatsDetails implements Serializable
{
    private final IntegerMap m_data;
    private final float m_average;
    
    public RandomStatsDetails(IntegerMap data)
    {
        m_data = data;
        
        int total = 0;
        for(int i = 1; i <= 6; i++)
        {
            total += i * m_data.getInt(new Integer(i));
        }
        m_average = ((float) total) / ((float) data.totalValues());
        
        
    }
    
    public float getAverage()
    {
        return m_average;
    }
    
    public IntegerMap getData()
    {
        return m_data;
    }

}
