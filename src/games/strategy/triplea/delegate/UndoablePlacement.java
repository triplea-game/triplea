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
import games.strategy.triplea.formatter.MyFormatter;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Contains all the data to describe a placement and to undo it.
 * 
 * @author Erik von der Osten
 */
class UndoablePlacement extends AbstractUndoableMove
{
	private static final long serialVersionUID = -1493488646587233451L;
	final Territory m_placeTerritory;
	Territory m_producerTerritory;
	PlayerID m_player;
	
	public UndoablePlacement(final PlayerID player, final CompositeChange change, final Territory producer_territory, final Territory place_territory, final Collection<Unit> units)
	{
		super(change, units);
		m_placeTerritory = place_territory;
		m_producerTerritory = producer_territory;
		m_player = player;
	}
	
	public Territory getProducerTerritory()
	{
		return m_producerTerritory;
	}
	
	public void setProducerTerritory(final Territory producerTerritory)
	{
		m_producerTerritory = producerTerritory;
	}
	
	public Territory getPlaceTerritory()
	{
		return m_placeTerritory;
	}
	
	@Override
	protected final void undoSpecific(final IDelegateBridge bridge)
	{
		final GameData data = bridge.getData();
		final String currentStepName = bridge.getStepName(); // TODO: currently there is no good way to figure out what Delegate and Step we are currently in
		AbstractPlaceDelegate currentDelegate;
		if (currentStepName.endsWith("BidPlace"))
			currentDelegate = DelegateFinder.bidPlaceDelegate(data);
		else if (currentStepName.endsWith("NoAirCheckPlace"))
			currentDelegate = DelegateFinder.placeNoAirCheckDelegate(data);
		else if (currentStepName.endsWith("Place"))
			currentDelegate = DelegateFinder.placeDelegate(data);
		else
			throw new IllegalStateException("Can not find Placement Delegate for current step: " + currentStepName);
		final Map<Territory, Collection<Unit>> produced = currentDelegate.getProduced();
		final Collection<Unit> units = produced.get(m_producerTerritory);
		units.removeAll(getUnits());
		if (units.isEmpty())
		{
			produced.remove(m_producerTerritory);
		}
		currentDelegate.setProduced(new HashMap<Territory, Collection<Unit>>(produced));
	}
	
	@Override
	public final String getMoveLabel()
	{
		if (m_producerTerritory != m_placeTerritory)
			return m_producerTerritory.getName() + " -> " + m_placeTerritory.getName();
		return m_placeTerritory.getName();
	}
	
	@Override
	public final Territory getEnd()
	{
		return m_placeTerritory;
	}
	
	@Override
	protected final PlacementDescription getDescriptionObject()
	{
		return new PlacementDescription(m_units, m_placeTerritory);
	}
	
	@Override
	public String toString()
	{
		if (m_producerTerritory != m_placeTerritory)
			return m_producerTerritory.getName() + " produces in " + m_placeTerritory.getName() + ": " + MyFormatter.unitsToTextNoOwner(m_units);
		return m_placeTerritory.getName() + ": " + MyFormatter.unitsToTextNoOwner(m_units);
	}
}
