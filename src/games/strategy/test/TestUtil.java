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

package games.strategy.test;

public class TestUtil
{
    
    /**
     * A server socket has a time to live after it is closed in which it is still
     * bound to its port.  For testing, we need to use a new port each time
     * to prevent socket already bound errors
     */
    public static int getUniquePort()
    {
        //store/get from SystemProperties
        //to get around junit reloading
        
        String KEY = "triplea.test.port";
        
        String prop = System.getProperties().getProperty(KEY);
        if(prop == null)
        {
            //start off with something fairly random, between 12000 - 14000
            prop = Integer.toString(12000 + (int) (Math.random() % 2000)); 
        }
        
        int val = Integer.parseInt(prop);
        val++;
        
        if(val > 15000)
            val = 12000;
        
        System.getProperties().put(KEY, "" + val);
        
        return val;
        
        
    }
}
