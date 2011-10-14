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
 * EndRoundDelegate.java
 *
 * Created on January 18, 2002, 9:50 PM
 */

package games.strategy.triplea.delegate;

import games.strategy.common.delegate.BaseDelegate;
import games.strategy.engine.data.*;
import games.strategy.engine.delegate.*;
import games.strategy.engine.message.IRemote;
import games.strategy.triplea.attatchments.TerritoryAttachment;

import java.io.Serializable;
import java.util.Iterator;

/**
 *
 *  A delegate used to transfer PUs to other players
 *
 * @author  Kevin Comcowich
 * @version 1.0
 */
public class GivePUsDelegate extends BaseDelegate
{

	private boolean m_gameOver = false;

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
		
		if(m_gameOver)
			return;

		if(isWW2V2())
		    return;
	}

	
	private boolean isWW2V2()
    {
        return games.strategy.triplea.Properties.getWW2V2(getData());
    }
	
	public int getProduction(PlayerID id)
	{
		int sum = 0;

        Iterator<Territory> territories = getData().getMap().iterator();
		while(territories.hasNext())
		{
            Territory current = territories.next();
			if(current.getOwner().equals(id))
			{
				TerritoryAttachment ta = TerritoryAttachment.get(current);
				sum += ta.getProduction();
			}
		}                
		return sum;
	}


	/**
	 * Returns the state of the Delegate.
	 */
	@Override
	public Serializable saveState()
	{
		return Boolean.valueOf(m_gameOver);
	}

	/**
	 * Loads the delegates state
	 */
	@Override
	public void loadState(Serializable state)
	{
		m_gameOver = ((Boolean) state).booleanValue();
	}

	
    /* 
     * @see games.strategy.engine.delegate.IDelegate#getRemoteType()
     */
    @Override
	public Class<? extends IRemote> getRemoteType()
    {
        return null;
    }


}
