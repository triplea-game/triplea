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

import java.io.Serializable;


/**
 *
 * @author  Sean Bridges
 * @version 1.0
 *
 * A Delegate that can be saved and loaded.
 * 
 */
public interface ISaveableDelegate extends IDelegate
{
	
	/**
	 * Can the delegate be saved at the current time.
	 * @arg message, a String[] of size 1, hack to pass an error message back.
	 */
	public boolean canSave(String[] message);

	/**
	 * Returns the state of the Delegate.
	 */
	public Serializable saveState();
	
	/**
	 * Loads the delegates state
	 */
	public void loadState(Serializable state);	

}
