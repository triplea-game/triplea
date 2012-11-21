/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package games.strategy.engine.lobby.server.userDB;

import games.strategy.util.Util;

import java.util.Date;

import junit.framework.TestCase;

public class BannedIpControllerTest extends TestCase
{
	public void testCRUD()
	{
		final BannedIpController controller = new BannedIpController();
		final String ip = Util.createUniqueTimeStamp();
		controller.addBannedIp(ip);
		assertTrue(controller.isIpBanned(ip).getFirst());
		controller.removeBannedIp(ip);
		assertFalse(controller.isIpBanned(ip).getFirst());
	}
	
	public void testNonBannedIp()
	{
		final BannedIpController controller = new BannedIpController();
		assertFalse(controller.isIpBanned(Util.createUniqueTimeStamp()).getFirst());
	}
	
	public void testBanExpires()
	{
		final BannedIpController controller = new BannedIpController();
		final String ip = Util.createUniqueTimeStamp();
		final Date expire = new Date(System.currentTimeMillis() - 5000);
		controller.addBannedIp(ip, expire);
		assertFalse(controller.isIpBanned(ip).getFirst());
	}
	
	public void testUpdate()
	{
		final BannedIpController controller = new BannedIpController();
		final String ip = Util.createUniqueTimeStamp();
		final Date expire = new Date(System.currentTimeMillis() - 5000);
		controller.addBannedIp(ip, expire);
		controller.addBannedIp(ip);
		assertTrue(controller.isIpBanned(ip).getFirst());
	}
}
