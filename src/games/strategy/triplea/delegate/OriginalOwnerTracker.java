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
 * OriginalOwnerTracker.java
 * 
 * Created on December 10, 2001, 9:04 AM
 */
package games.strategy.triplea.delegate;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.ChangeFactory;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.Constants;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.attatchments.TerritoryAttachment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * 
 * @author Sean Bridges
 * @version 1.0
 * 
 *          Tracks the original owner of things.
 *          Needed since territories and factories must revert
 *          to their original owner when captured from the enemy.
 */
public class OriginalOwnerTracker implements java.io.Serializable
{
	/** Creates new OriginalOwnerTracker */
	public OriginalOwnerTracker()
	{
	}
	
	public Change addOriginalOwnerChange(final Territory t, final PlayerID player)
	{
		return ChangeFactory.attachmentPropertyChange(TerritoryAttachment.get(t), player, Constants.ORIGINAL_OWNER);
	}
	
	public Change addOriginalOwnerChange(final Unit unit, final PlayerID player)
	{
		return ChangeFactory.unitPropertyChange(unit, player, Constants.ORIGINAL_OWNER);
	}
	
	public Change addOriginalOwnerChange(final Collection<Unit> units, final PlayerID player)
	{
		final CompositeChange change = new CompositeChange();
		for (final Unit unit : units)
		{
			change.add(addOriginalOwnerChange(unit, player));
		}
		return change;
	}
	
	public PlayerID getOriginalOwner(final Unit unit)
	{
		return TripleAUnit.get(unit).getOriginalOwner();
	}
	
	public PlayerID getOriginalOwner(final Territory t)
	{
		final TerritoryAttachment ta = TerritoryAttachment.get(t);
		if (ta == null)
			return null;
		return ta.getOriginalOwner();
	}
	
	public Collection<Territory> getOriginallyOwned(final GameData data, final PlayerID player)
	{
		final Collection<Territory> rVal = new ArrayList<Territory>();
		for (Territory t  : data.getMap()) {
			PlayerID originalOwner = getOriginalOwner(t);
			if (originalOwner == null)
			{
				originalOwner = PlayerID.NULL_PLAYERID;
			}
			if (originalOwner.equals(player))
			{
				rVal.add(t);
			}
		}
		return rVal;
	}
}
