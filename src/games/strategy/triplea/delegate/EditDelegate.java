/*
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version. This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details. You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package games.strategy.triplea.delegate;

import games.strategy.common.delegate.BaseEditDelegate;
import games.strategy.engine.data.ChangeFactory;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.RelationshipType;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.message.IRemote;
import games.strategy.triplea.Constants;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.delegate.remote.IEditDelegate;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.util.CompositeMatch;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.IntegerMap;
import games.strategy.util.Match;
import games.strategy.util.Triple;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * 
 * Edit game state
 * 
 * @author Tony Clayton
 */
public class EditDelegate extends BaseEditDelegate implements IEditDelegate
{
	/**
	 * Called before the delegate will run.
	 */
	@Override
	public void start()
	{
		super.start();
	}
	
	@Override
	public void end()
	{
	}
	
	public String removeUnits(final Territory territory, final Collection<Unit> units)
	{
		String result = null;
		if (null != (result = checkEditMode()))
			return result;
		if (null != (result = EditValidator.validateRemoveUnits(getData(), territory, units)))
			return result;
		if (units == null || units.isEmpty())
			return null;
		final Collection<PlayerID> owners = new HashSet<PlayerID>();
		for (final Unit u : units)
		{
			owners.add(u.getOwner());
		}
		for (final PlayerID p : owners)
		{
			final List<Unit> unitsOwned = Match.getMatches(units, Matches.unitIsOwnedBy(p));
			logEvent("Removing units owned by " + p.getName() + " from " + territory.getName() + ": " + MyFormatter.unitsToTextNoOwner(unitsOwned), unitsOwned);
			m_bridge.addChange(ChangeFactory.removeUnits(territory, unitsOwned));
		}
		return null;
	}
	
	public String addUnits(final Territory territory, final Collection<Unit> units)
	{
		String result = null;
		if (null != (result = checkEditMode()))
			return result;
		if (null != (result = EditValidator.validateAddUnits(getData(), territory, units)))
			return result;
		if (units == null || units.isEmpty())
			return null;
		// now make sure land units are put on transports properly
		final PlayerID player = units.iterator().next().getOwner();
		final GameData data = getData();
		Map<Unit, Unit> mapLoading = null;
		if (territory.isWater())
		{
			if (!Match.allMatch(units, Matches.UnitIsSea))
			{
				if (Match.someMatch(units, Matches.UnitIsLand))
				{
					// this should be exact same as the one in the EditValidator
					if (!Match.allMatch(units, Matches.alliedUnit(player, data)))
						return "Can't add mixed nationality units to water";
					final Match<Unit> friendlySeaTransports = new CompositeMatchAnd<Unit>(Matches.UnitIsTransport, Matches.UnitIsSea, Matches.alliedUnit(player, data));
					final Collection<Unit> seaTransports = Match.getMatches(units, friendlySeaTransports);
					final Collection<Unit> landUnitsToAdd = Match.getMatches(units, Matches.UnitIsLand);
					if (!Match.allMatch(landUnitsToAdd, Matches.UnitCanBeTransported))
						return "Can't add land units that can't be transported, to water";
					seaTransports.addAll(territory.getUnits().getMatches(friendlySeaTransports));
					if (seaTransports.isEmpty())
						return "Can't add land units to water without enough transports";
					mapLoading = MoveDelegate.mapTransports(null, landUnitsToAdd, seaTransports, true, player);
					if (!mapLoading.keySet().containsAll(landUnitsToAdd))
						return "Can't add land units to water without enough transports";
				}
			}
		}
		// now perform the changes
		logEvent("Adding units owned by " + units.iterator().next().getOwner().getName() + " to " + territory.getName() + ": " + MyFormatter.unitsToTextNoOwner(units), units);
		m_bridge.addChange(ChangeFactory.addUnits(territory, units));
		if (mapLoading != null && !mapLoading.isEmpty())
		{
			final TransportTracker transportTracker = new TransportTracker();
			for (final Entry<Unit, Unit> entry : mapLoading.entrySet())
			{
				m_bridge.addChange(transportTracker.loadTransportChange((TripleAUnit) entry.getValue(), entry.getKey(), m_player));
			}
		}
		return null;
	}
	
	/**
	 * @return gets the production of the territory, ignores whether the territory was an original factory
	 */
	protected int getProduction(final Territory territory)
	{
		final TerritoryAttachment ta = TerritoryAttachment.get(territory);
		if (ta != null)
			return ta.getProduction();
		return 0;
	}
	
	public String changeTerritoryOwner(final Territory territory, final PlayerID player)
	{
		String result = null;
		if (null != (result = checkEditMode()))
			return result;
		final GameData data = getData();
		// validate this edit
		if (null != (result = EditValidator.validateChangeTerritoryOwner(data, territory, player)))
			return result;
		logEvent("Changing ownership of " + territory.getName() + " from " + territory.getOwner().getName() + " to " + player.getName(), territory);
		if (!data.getRelationshipTracker().isAtWar(territory.getOwner(), player))
		{
			// change ownership of friendly factories
			final Collection<Unit> units = territory.getUnits().getMatches(Matches.UnitIsInfrastructure);
			for (final Unit unit : units)
			{
				m_bridge.addChange(ChangeFactory.changeOwner(unit, player, territory));
			}
		}
		else
		{
			final CompositeMatch<Unit> enemyNonCom = new CompositeMatchAnd<Unit>();
			enemyNonCom.add(Matches.UnitIsInfrastructure);
			enemyNonCom.add(Matches.enemyUnit(player, data));
			final Collection<Unit> units = territory.getUnits().getMatches(enemyNonCom);
			// mark no movement for enemy units
			m_bridge.addChange(ChangeFactory.markNoMovementChange(units));
			// change ownership of enemy AA and factories
			for (final Unit unit : units)
			{
				m_bridge.addChange(ChangeFactory.changeOwner(unit, player, territory));
			}
		}
		// change ownership of territory
		m_bridge.addChange(ChangeFactory.changeOwner(territory, player));
		return null;
	}
	
	public String changePUs(final PlayerID player, final int newTotal)
	{
		String result = null;
		if (null != (result = checkEditMode()))
			return result;
		final Resource PUs = getData().getResourceList().getResource(Constants.PUS);
		final int oldTotal = player.getResources().getQuantity(PUs);
		if (oldTotal == newTotal)
			return "New PUs total is unchanged";
		if (newTotal < 0)
			return "New PUs total is invalid";
		logEvent("Changing PUs for " + player.getName() + " from " + oldTotal + " to " + newTotal, null);
		m_bridge.addChange(ChangeFactory.changeResourcesChange(player, PUs, (newTotal - oldTotal)));
		return null;
	}
	
	public String changeTechTokens(final PlayerID player, final int newTotal)
	{
		String result = null;
		if (null != (result = checkEditMode()))
			return result;
		final Resource techTokens = getData().getResourceList().getResource(Constants.TECH_TOKENS);
		final int oldTotal = player.getResources().getQuantity(techTokens);
		if (oldTotal == newTotal)
			return "New token total is unchanged";
		if (newTotal < 0)
			return "New token total is invalid";
		logEvent("Changing tech tokens for " + player.getName() + " from " + oldTotal + " to " + newTotal, null);
		m_bridge.addChange(ChangeFactory.changeResourcesChange(player, techTokens, (newTotal - oldTotal)));
		return null;
	}
	
	public String addTechAdvance(final PlayerID player, final Collection<TechAdvance> advances)
	{
		String result = null;
		if (null != (result = checkEditMode()))
			return result;
		if (null != (result = EditValidator.validateAddTech(getData(), advances, player)))
			return result;
		for (final TechAdvance advance : advances)
		{
			logEvent("Adding Technology " + advance.getName() + " for " + player.getName(), null);
			TechTracker.addAdvance(player, m_bridge, advance);
		}
		return null;
	}
	
	public String removeTechAdvance(final PlayerID player, final Collection<TechAdvance> advances)
	{
		String result = null;
		if (null != (result = checkEditMode()))
			return result;
		if (null != (result = EditValidator.validateRemoveTech(getData(), advances, player)))
			return result;
		for (final TechAdvance advance : advances)
		{
			logEvent("Removing Technology " + advance.getName() + " for " + player.getName(), null);
			TechTracker.removeAdvance(player, m_bridge, advance);
		}
		return null;
	}
	
	public String changeUnitHitDamage(final IntegerMap<Unit> unitDamageMap, final Territory territory)
	{
		String result = null;
		if (null != (result = checkEditMode()))
			return result;
		if (null != (result = EditValidator.validateChangeHitDamage(getData(), unitDamageMap, territory)))
			return result;
		// remove anyone who is the same
		final Collection<Unit> units = new ArrayList<Unit>(unitDamageMap.keySet());
		for (final Unit u : units)
		{
			final int dmg = unitDamageMap.getInt(u);
			if (u.getHits() == dmg)
				unitDamageMap.removeKey(u);
		}
		if (unitDamageMap.isEmpty())
			return null;
		final Collection<Unit> unitsFinal = new ArrayList<Unit>(unitDamageMap.keySet());
		logEvent("Changing unit hit damage for these " + unitsFinal.iterator().next().getOwner().getName() + " owned units to: "
					+ MyFormatter.integerUnitMapToString(unitDamageMap, ", ", " = ", false), unitsFinal);
		m_bridge.addChange(ChangeFactory.unitsHit(unitDamageMap));
		territory.notifyChanged();
		return null;
	}
	
	public String changeUnitBombingDamage(final IntegerMap<Unit> unitDamageMap, final Territory territory)
	{
		String result = null;
		if (null != (result = checkEditMode()))
			return result;
		if (null != (result = EditValidator.validateChangeBombingDamage(getData(), unitDamageMap, territory)))
			return result;
		final CompositeChange changes = new CompositeChange();
		final IntegerMap<Unit> unitHitDamageMap = new IntegerMap<Unit>();
		final TerritoryAttachment ta = TerritoryAttachment.get(territory);
		// remove anyone who is the same
		final Collection<Unit> units = new ArrayList<Unit>(unitDamageMap.keySet());
		if (games.strategy.triplea.Properties.getSBRAffectsUnitProduction(getData()))
		{
			// we are damaging the territory, not the unit
			if (ta == null)
				return null;
			final int production = ta.getProduction();
			final int unitProduction = ta.getUnitProduction();
			final int currentDamage = production - unitProduction;
			final int damageToPut = unitDamageMap.highestValue();
			if (currentDamage == damageToPut)
				return null;
			for (final Unit u : units)
			{
				if (damageToPut == 0 && currentDamage > 0)
					unitHitDamageMap.put(u, 0);
				else if (currentDamage == 0 && damageToPut > 0)
					unitHitDamageMap.put(u, 1); // mark as damaged so we get the _hit picture
				// else both must be greater than zero, so we are already marked
			}
		}
		else
		// if (games.strategy.triplea.Properties.getDamageFromBombingDoneToUnitsInsteadOfTerritories(getData()))
		{
			for (final Unit u : units)
			{
				final int dmg = unitDamageMap.getInt(u);
				final int currentDamage = ((TripleAUnit) u).getUnitDamage();
				if (currentDamage == dmg)
					unitDamageMap.removeKey(u);
				else
				{
					if (dmg == 0 && currentDamage > 0)
						unitHitDamageMap.put(u, 0);
					else if (currentDamage == 0 && dmg > 0)
						unitHitDamageMap.put(u, 1); // mark as damaged so we get the _hit picture
					// else both must be greater than zero, so we are already marked
				}
			}
		}
		if (unitDamageMap.isEmpty())
			return null;
		changes.add(ChangeFactory.unitsHit(unitHitDamageMap));
		if (games.strategy.triplea.Properties.getSBRAffectsUnitProduction(getData()))
		{
			// we do damage to the territory (use the largest value)
			// remember to subtract it from the current production value (not the current unit production value) because we are setting the total damage, not adding more damage.
			changes.add(ChangeFactory.attachmentPropertyChange(ta, Integer.toString(ta.getProduction() - unitDamageMap.highestValue()), "unitProduction"));
		}
		else
		// if (games.strategy.triplea.Properties.getDamageFromBombingDoneToUnitsInsteadOfTerritories(getData()))
		{
			// we do damage to the unit
			for (final Entry<Unit, Integer> entry : unitDamageMap.entrySet())
			{
				changes.add(ChangeFactory.unitPropertyChange(entry.getKey(), entry.getValue(), TripleAUnit.UNIT_DAMAGE));
			}
		}
		final Collection<Unit> unitsFinal = new ArrayList<Unit>(unitDamageMap.keySet());
		logEvent("Changing unit bombing damage for these " + unitsFinal.iterator().next().getOwner().getName() + " owned units to: "
					+ MyFormatter.integerUnitMapToString(unitDamageMap, ", ", " = ", false), unitsFinal);
		m_bridge.addChange(changes);
		territory.notifyChanged();
		return null;
	}
	
	public String changePoliticalRelationships(final Collection<Triple<PlayerID, PlayerID, RelationshipType>> relationshipChanges)
	{
		String result = null;
		if (relationshipChanges == null || relationshipChanges.isEmpty())
			return result;
		if (null != (result = checkEditMode()))
			return result;
		if (null != (result = EditValidator.validateChangePoliticalRelationships(getData(), relationshipChanges)))
			return result;
		final BattleTracker battleTracker = MoveDelegate.getBattleTracker(getData());
		for (final Triple<PlayerID, PlayerID, RelationshipType> relationshipChange : relationshipChanges)
		{
			final RelationshipType currentRelation = getData().getRelationshipTracker().getRelationshipType(relationshipChange.getFirst(), relationshipChange.getSecond());
			if (!currentRelation.equals(relationshipChange.getThird()))
			{
				logEvent("Editing Political Relationship for " + relationshipChange.getFirst().getName() + " and " + relationshipChange.getSecond().getName() + " from " +
							currentRelation.getName() + " to " + relationshipChange.getThird().getName(), null);
				m_bridge.addChange(ChangeFactory.relationshipChange(relationshipChange.getFirst(), relationshipChange.getSecond(), currentRelation, relationshipChange.getThird()));
				battleTracker.addRelationshipChangesThisTurn(relationshipChange.getFirst(), relationshipChange.getSecond(), currentRelation, relationshipChange.getThird());
			}
		}
		return null;
	}
	
	/*
	 * @see games.strategy.engine.delegate.IDelegate#getRemoteType()
	 */
	@Override
	public Class<? extends IRemote> getRemoteType()
	{
		return IEditDelegate.class;
	}
}
