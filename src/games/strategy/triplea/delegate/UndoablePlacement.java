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
	final Territory m_place_territory;
	final Territory m_producer_territory;
	PlayerID m_player;
	
	public UndoablePlacement(final PlayerID player, final CompositeChange change, final Territory producer_territory, final Territory place_territory, final Collection<Unit> units)
	{
		super(change, units);
		m_place_territory = place_territory;
		m_producer_territory = producer_territory;
		m_player = player;
	}
	
	@Override
	protected final void undoSpecific(final IDelegateBridge bridge)
	{
		final GameData data = bridge.getData();
		final String currentStepName = bridge.getStepName(); // TODO: currently there is no good way to figure out what Delegate and Step we are currently in
		AbstractPlaceDelegate currentDelegate;
		if (currentStepName.endsWith("BidPlace"))
			currentDelegate = DelegateFinder.bidPlaceDelegate(data);
		else if (currentStepName.endsWith("Place"))
			currentDelegate = DelegateFinder.placeDelegate(data);
		else
			throw new IllegalStateException("Can not find Placement Delegate for current step: " + currentStepName);
		final Map<Territory, Collection<Unit>> produced = currentDelegate.getProduced();
		final Collection<Unit> units = produced.get(m_producer_territory);
		units.removeAll(getUnits());
		if (units.isEmpty())
		{
			produced.remove(m_producer_territory);
		}
		currentDelegate.setProduced(new HashMap<Territory, Collection<Unit>>(produced));
	}
	
	@Override
	public final String getMoveLabel()
	{
		return m_place_territory.getName();
	}
	
	@Override
	public final Territory getEnd()
	{
		return m_place_territory;
	}
	
	@Override
	protected final PlacementDescription getDescriptionObject()
	{
		return new PlacementDescription(m_units, m_place_territory);
	}
}
