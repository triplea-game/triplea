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
 * Delegate.java
 *
 * Created on October 13, 2001, 4:27 PM
 */

package games.strategy.engine.delegate;

import games.strategy.engine.data.GameData;


/**
 *
 * @author  Sean Bridges
 * @version 1.0
 *
 * A section of code that implements game logic.
 * The delegate should be deterministic.  All random events should be
 * obtained through calls to the delegateBridge.
 *
 * Delegates make changes to gameData by calling the addChange method in DelegateBridge.
 *
 * All delegates must have a zero argument constructor, due to reflection constraints.
 * The delegate will be initialized with a call of initialize(..) before used.
 *
 * Delegates start executing with the start method, and stop with the end message.
 * 
 * Delegates can be made accesseble to players through implementing an IRemote,
 * and will be called through RemoteMessenger.
 *
 */
public interface IDelegate 
{
	/*
	 * Uses name as the interal unique name and displayName for display to users
	 */
	public void initialize(String name, String displayName);

	/**
	 * Called before the delegate will run.
	 */
	public void start(IDelegateBridge aBridge, GameData gameData);
	/**
	 * Called before the delegate will stop running.
	 */
	public void end();

	public String getName();

	public String getDisplayName();
	
	/**
	 * Get the remote type of this delegate for use 
	 * by a RemoteMessenger.
	 * 
	 * Class must be an interface that extends IRemote
	 * 
	 * If the return value is null, then it indicates that this 
	 * delegate should not be used as in IRemote.
	 */
	public Class getRemoteType();
}
