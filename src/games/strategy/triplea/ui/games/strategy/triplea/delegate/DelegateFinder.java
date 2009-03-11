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
 * DelegateFinder.java
 *
 * Created on November 28, 2001, 2:58 PM
 */

package games.strategy.triplea.delegate;

import games.strategy.engine.data.*;
import games.strategy.engine.delegate.*;

/**
 *
 * @author  Sean Bridges
 * @version 1.0
 */
public class DelegateFinder 
{

	public static final BattleDelegate battleDelegate(GameData data)
	{
		IDelegate delegate =  data.getDelegateList().getDelegate("battle");
		if(delegate == null)
			throw new IllegalStateException("Battle delegate not found");
		return (BattleDelegate) delegate;
		
	}
	
	public static final MoveDelegate moveDelegate(GameData data)
	{
		IDelegate delegate =  data.getDelegateList().getDelegate("move");
		if(delegate == null)
			throw new IllegalStateException("Move delegate not found");
		return (MoveDelegate) delegate;
		
	}

	public static final TechnologyDelegate techDelegate(GameData data)
	{
		IDelegate delegate =  data.getDelegateList().getDelegate("tech");
		if(delegate == null)
			throw new IllegalStateException("Tech delegate not found");
		return (TechnologyDelegate) delegate;
	}

	public static final GiveIPCsDelegate giveIPCsDelegate(GameData data)
	{
		IDelegate delegate =  data.getDelegateList().getDelegate("giveIPCs");
		if(delegate == null)
			throw new IllegalStateException("giveIPCs delegate not found");
		return (GiveIPCsDelegate) delegate;
	}
	
}
