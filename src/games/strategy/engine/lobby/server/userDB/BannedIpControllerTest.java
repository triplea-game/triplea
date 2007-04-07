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

package games.strategy.engine.lobby.server.userDB;

import games.strategy.util.Util;
import junit.framework.TestCase;

public class BannedIpControllerTest extends TestCase
{

    
    public void testCRUD() 
    {
        BannedIpController controller = new BannedIpController();
        String ip = Util.createUniqueTimeStamp();
        controller.addBannedIp(ip);
        
        assertTrue(controller.isIpBanned(ip));
        
        controller.removeBannedIp(ip);
        assertFalse(controller.isIpBanned(ip));
    }
}
