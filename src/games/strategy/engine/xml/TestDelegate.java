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

/*
 * TestDelegate.java
 *
 * Created on October 22, 2001, 9:39 AM
 */

package games.strategy.engine.xml;

import games.strategy.engine.delegate.*;
import games.strategy.engine.data.*;
import games.strategy.engine.message.Message;
import games.strategy.net.IRemote;

/**
 *
 * @author  Sean Bridges
 * @version 1.0
 *
 * A simple dumb delegate, dont acutally call these methods.
 * Simply to satisfy the interface requirements for testing.
 */
public final class TestDelegate implements IDelegate
{
	private String m_name;

	public TestDelegate() {}

	public boolean supportsTransactions() {return false;}
	public void initialize(String name) { m_name = name;}
	public void initialize(String name, String displayName) { m_name = name;}
	public void startTransaction() {}
	public void rollback() {}
	public void commit() {}
	public boolean inTransaction() {return false;}
	public String getName() {return m_name;}
	public void cancelTransaction() {}
	public void start(IDelegateBridge aBridge, GameData gameData) {	}
	public Message sendMessage(Message aMessage) {return null;	}
	public void end() {	}
	public String getDisplayName() {return "displayName";}
	public Class getRemoteType() {return IRemote.class;}

}
