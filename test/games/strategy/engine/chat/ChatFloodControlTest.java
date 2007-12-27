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

package games.strategy.engine.chat;

import junit.framework.TestCase;

public class ChatFloodControlTest extends TestCase
{
    private ChatFloodControl fc = new ChatFloodControl();

    public void setUp() 
    {
        
    }
    
    public void testSimple() 
    {
        assertTrue(fc.allow("", System.currentTimeMillis()));
    }
    
    public void testDeny() 
    {
        for(int i =0; i < ChatFloodControl.EVENTS_PER_WINDOW; i++) 
        {
            fc.allow("",System.currentTimeMillis());
        }
        
        assertFalse(fc.allow("", System.currentTimeMillis()));
    }
    
    public void testReney() 
    {
        for(int i =0; i < 100; i++) 
        {
            fc.allow("",System.currentTimeMillis());
        }
        
        
        assertTrue(fc.allow("", System.currentTimeMillis() + 1000 * 60 * 60));
    }
    
        
}
