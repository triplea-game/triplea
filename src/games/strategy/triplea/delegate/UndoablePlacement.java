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

package games.strategy.triplea.delegate;

import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.delegate.dataObjects.PlacementDescription;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Contains all the data to describe a placement and to undo it.
 *
 * @author Erik von der Osten
 */
@SuppressWarnings("serial")
class UndoablePlacement extends AbstractUndoableMove
{
    final Territory m_territory;
    PlayerID m_player;

    public UndoablePlacement(PlayerID player, CompositeChange change, Territory territory, Collection<Unit> units)
    {
        super(change, units);
        m_territory = territory;
        m_player = player;
    }

    protected final void undoSpecific(GameData data, IDelegateBridge bridge)
    {
        bridge.getHistoryWriter().startEvent(bridge.getPlayerID().getName() + " undo move " + (m_index + 1) + ".");
        bridge.getHistoryWriter().setRenderingData(new PlacementDescription(m_units, m_territory));

        Map<Territory, Collection<Unit>> produced = DelegateFinder.placeDelegate(data).getProduced();
        Collection<Unit> units = produced.get(m_territory);
        units.removeAll(getUnits());
        if (units.isEmpty())
        {
            produced.remove(m_territory);
        }
        DelegateFinder.placeDelegate(data).setProduced(new HashMap<Territory, Collection<Unit>>(produced));
    }

    public final String getMoveLabel()
    {
        return m_territory.getName();
    }

    public final Territory getEnd()
    {
        return m_territory;
    }

    public final PlacementDescription getDescriptionObject() {
        return new PlacementDescription(m_units, m_territory);
    }
}
