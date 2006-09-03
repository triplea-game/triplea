/*
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version. This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details. You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package games.strategy.engine.chat;

import games.strategy.engine.message.DummyMessenger;
import games.strategy.net.Messengers;
import junit.framework.TestCase;

public class StatusTest extends TestCase
{

    
    public void testStatus() throws Exception
    {
        DummyMessenger messenger = new DummyMessenger();
        Messengers messengers = new Messengers(messenger);
        
        StatusManager manager = new StatusManager(messengers);
        
        assertNull(manager.getStatus(messenger.getLocalNode()));
        
        manager.setStatus("test");
        
        
        Thread.sleep(200);
        
        assertEquals("test", manager.getStatus(messenger.getLocalNode()));
        
        
        
        assertEquals("test", new StatusManager(messengers).getStatus(messenger.getLocalNode()));
        
    }
    
}
 