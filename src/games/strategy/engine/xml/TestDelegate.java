/*
 * TestDelegate.java
 *
 * Created on October 22, 2001, 9:39 AM
 */

package games.strategy.engine.xml;

import games.strategy.engine.delegate.*;
import games.strategy.engine.data.*;
import games.strategy.engine.message.Message;

/**
 *
 * @author  Sean Bridges
 * @version 1.0
 *
 * A simple dumb delegate, dont acutally call these methods.
 * Simply to satisfy the interface requirements for testing.
 */
public final class TestDelegate implements Delegate
{
	private String m_name;

	public TestDelegate() {}

	public boolean supportsTransactions() {return false;}
	public void initialize(String name) { m_name = name;}
	public void startTransaction() {}
	public void rollback() {}
	public void commit() {}
	public boolean inTransaction() {return false;}
	public String getName() {return m_name;}
	public void cancelTransaction() {}
	public void start(DelegateBridge aBridge, GameData gameData) {	}
	public Message sendMessage(Message aMessage) {return null;	}
	public void end() {	}
	
}
