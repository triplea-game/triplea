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

import junit.framework.TestCase;

/**
 * @author Sean Bridges
 */
public class CryptoRandomSourceTest extends TestCase
{

    public CryptoRandomSourceTest(String name)
    {
        super(name);
    }

    public void testIntToRandom()
    {
        byte[] bytes = CryptoRandomSource.intsToBytes(new int[] {0xDDCCBBAA});
        assertEquals(bytes.length, 4);
        
        assertEquals(bytes[0], (byte) 0xAA);
        assertEquals(bytes[1], (byte) 0xBB);
        assertEquals(bytes[2], (byte) 0xCC);
        assertEquals(bytes[3], (byte) 0xDD);
        
    }
    
    public void testBytes()
    {
        assertEquals(CryptoRandomSource.byteToIntUnsigned((byte) 0), 0);
        assertEquals(CryptoRandomSource.byteToIntUnsigned((byte) 1), 1);
        assertEquals(CryptoRandomSource.byteToIntUnsigned(( (byte) 0xFF)), 0xFF);        
    }
    
    public void testThereAndBackAgain()
    {
      int[] ints = new int[] {0,1,12,123,0xFF, 0x100,-1,124152,532153,123121, 0xABCDEF12, 0xFF00DD00, Integer.MAX_VALUE, Integer.MIN_VALUE};
      
      int[] thereAndBack = CryptoRandomSource.bytesToInts(CryptoRandomSource.intsToBytes(ints));
      for(int i = 0; i < ints.length; i++)
      {
          assertEquals("at " + i, ints[i], thereAndBack[i]);
      }
      
    }
    
}
