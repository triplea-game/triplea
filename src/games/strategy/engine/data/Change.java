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
 * Change.java
 *
 * Created on October 25, 2001, 1:27 PM
 */

package games.strategy.engine.data;

import java.io.Serializable;

/**
 *
 * @author  Sean Bridges
 * @version 1.0
 *
 * Not an interface because we want the perform() method to be protected.
 * 
 * A Change encapsulates something that can be done to GameData.  We use changes so 
 * that we can serialize and track change to GameData as they occur.
 * 
 * When a change is performed on a GameData in a game, the change is serialized and sent
 * across the network to all the clients.  Since all changes to GameData are done through changes
 * and all changes are serialized to all clients, all clients should always be in sync.
 * 
 * A Change can be inverted to create an equal but opposite change.
 * 
 * Use ChangeFactory to create Changes.
 */
public abstract class Change implements Serializable
{	
	
	static final long serialVersionUID = -5563487769423328606L;

	protected abstract void perform(GameData data);
	public abstract Change invert();

}
