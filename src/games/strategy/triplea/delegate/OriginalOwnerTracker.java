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
 * OriginalOwnerTracker.java
 *
 * Created on December 10, 2001, 9:04 AM
 */

package games.strategy.triplea.delegate;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.attatchments.TerritoryAttachment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 *
 * @author  Sean Bridges
 * @version 1.0
 *
 * Tracks the original owner of things.
 * Needed since territories and factories must revert
 * to their original owner when captured from the enemy.
 */
public class OriginalOwnerTracker implements java.io.Serializable
{

    /** Creates new OriginalOwnerTracker */
    public OriginalOwnerTracker()
    {
    }

    public void addOriginalOwner(Territory t, PlayerID player)
    {
        TerritoryAttachment.get(t).setOriginalOwner(player);
    }

    public void addOriginalOwner(Unit unit, PlayerID player)
    {
        TripleAUnit.get(unit).setOriginalOwner(player);
    }

    public void addOriginalOwner(Collection<Unit> units, PlayerID player)
    {
        for (Unit unit : units)
            addOriginalOwner(unit, player);
    }

    public PlayerID getOriginalOwner(Unit unit)
    {
        return TripleAUnit.get(unit).getOriginalOwner();
    }

    public PlayerID getOriginalOwner(Territory t)
    {
        return TerritoryAttachment.get(t).getOriginalOwner();
    }

    public Collection<Territory> getOriginallyOwned(GameData data, PlayerID player)
    {
        Collection<Territory> rVal = new ArrayList<Territory>();
        Iterator<Territory> iter = data.getMap().iterator();
        while (iter.hasNext())
        {
            Territory t = iter.next();
            if(getOriginalOwner(t).equals(player))
            {
                rVal.add(t);
            }
        }
        return rVal;
    }

}
