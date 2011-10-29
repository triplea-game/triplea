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

/*
 * EndRoundDelegate.java
 * 
 * Created on January 18, 2002, 9:50 PM
 */

package games.strategy.triplea.delegate;

import games.strategy.common.delegate.BaseDelegate;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.message.IRemote;

import java.io.Serializable;

/**
 * 
 * A delegate used to transfer PUs to other players
 * 
 * @author Kevin Comcowich
 * @version 1.0
 */
public class GivePUsDelegate extends BaseDelegate
{
	/** Creates a new instance of GivePUsDelegate */
	public GivePUsDelegate()
	{
	}
	
	/**
	 * Called before the delegate will run.
	 */
	
	@Override
	public void start(IDelegateBridge aBridge)
	{
		super.start(aBridge);
	}
	
	@Override
	public void end()
	{
		super.end();
	}

	@Override
	public Serializable saveState()
	{
		GivePUsExtendedDelegateState state = new GivePUsExtendedDelegateState();
		state.superState = super.saveState();
		// add other variables to state here:
		return state;
	}

	@Override
	public void loadState(Serializable state)
	{
		GivePUsExtendedDelegateState s = (GivePUsExtendedDelegateState) state;
		super.loadState(s.superState);
		// load other variables from state here:
	}
	
	/*
	public int getProduction(PlayerID id)
	{
		int sum = 0;
		
		Iterator<Territory> territories = getData().getMap().iterator();
		while (territories.hasNext())
		{
			Territory current = territories.next();
			if (current.getOwner().equals(id))
			{
				TerritoryAttachment ta = TerritoryAttachment.get(current);
				sum += ta.getProduction();
			}
		}
		return sum;
	}
	*/
	
	/*
	 * @see games.strategy.engine.delegate.IDelegate#getRemoteType()
	 */

	@Override
	public Class<? extends IRemote> getRemoteType()
	{
		return null;
	}
	
}

@SuppressWarnings("serial")
class GivePUsExtendedDelegateState implements Serializable
{
	Serializable superState;
	// add other variables here:
}
