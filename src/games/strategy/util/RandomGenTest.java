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

package games.strategy.util;

import junit.framework.TestCase;

public class RandomGenTest extends TestCase
{
    public void testRandomSeed()
    {
        RandomGen gen = new RandomGen(6,1, "test");
        RandomTriplet triplet = gen.getTriplet();
        RandomGen other = new RandomGen();
        other.setTriplet(triplet);
        other.setKey(gen.getKey());
        
        assertEquals(other.getRandomSeed(), gen.getRandomSeed());   
    }
    
    public void testLongConversion()
    {
        for(long l = -1000; l < 2000; l++)
        {
            assertEquals(l,  RandomGen.byteArrToLong(RandomGen.longToByteArr(l)));
        }
    }
    
    
}
