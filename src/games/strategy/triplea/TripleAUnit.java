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
package games.strategy.triplea;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.ChangeFactory;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.annotations.GameProperty;
import games.strategy.triplea.attatchments.TechAbilityAttachment;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.util.IntegerMap;
import games.strategy.util.Match;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Extended unit for triplea games.
 * <p>
 * 
 * As with all game data components, changes made to this unit must be made through a Change instance. Calling setters on this directly will not serialize the changes across the network.
 * <p>
 * 
 * @author sgb
 * @version $LastChangedDate: 2008-06-10 12:09:22 -0600 (Thu, 14 Feb 2008) $
 */
public class TripleAUnit extends Unit
{
	// compatable with 0.9.2
	private static final long serialVersionUID = 8811372406957115036L;
	public static final String TRANSPORTED_BY = "transportedBy";
	public static final String UNLOADED = "unloaded";
	public static final String LOADED_THIS_TURN = "wasLoadedThisTurn";
	public static final String UNLOADED_TO = "unloadedTo";
	public static final String UNLOADED_IN_COMBAT_PHASE = "wasUnloadedInCombatPhase";
	public static final String ALREADY_MOVED = "alreadyMoved";
	public static final String BONUS_MOVEMENT = "bonusMovement";
	public static final String MOVEMENT_LEFT = "movementLeft";
	public static final String SUBMERGED = "submerged";
	public static final String ORIGINAL_OWNER = "originalOwner";
	public static final String WAS_IN_COMBAT = "wasInCombat";
	public static final String LOADED_AFTER_COMBAT = "wasLoadedAfterCombat";
	public static final String UNLOADED_AMPHIBIOUS = "wasAmphibious";
	public static final String ORIGINATED_FROM = "originatedFrom";
	public static final String WAS_SCRAMBLED = "wasScrambled";
	public static final String MAX_SCRAMBLE_COUNT = "maxScrambleCount";
	public static final String WAS_IN_AIR_BATTLE = "wasInAirBattle";
	public static final String UNIT_DAMAGE = "unitDamage";
	public static final String DISABLED = "disabled";
	
	private TripleAUnit m_transportedBy = null; // the transport that is currently transporting us
	private List<Unit> m_unloaded = Collections.emptyList(); // the units we have unloaded this turn
	private Boolean m_wasLoadedThisTurn = Boolean.FALSE; // was this unit loaded this turn?
	private Territory m_unloadedTo = null; // the territory this unit was unloaded to this turn
	private Boolean m_wasUnloadedInCombatPhase = Boolean.FALSE; // was this unit unloaded in combat phase this turn?
	
	private int m_alreadyMoved = 0; // movement used this turn
	private int m_bonusMovement = 0; // movement used this turn
	private int m_unitDamage = 0; // amount of damage unit has sustained
	
	private boolean m_submerged = false; // is this submarine submerged
	private PlayerID m_originalOwner = null; // original owner of this unit
	
	private boolean m_wasInCombat = false; // Was this unit in combat
	private boolean m_wasLoadedAfterCombat = false;
	private boolean m_wasAmphibious = false;
	
	private Territory m_originatedFrom = null; // the territory this unit started in (for use with scrambling)
	private boolean m_wasScrambled = false;
	private int m_maxScrambleCount = -1;
	private boolean m_wasInAirBattle = false;
	private boolean m_disabled = false;
	
	public static TripleAUnit get(final Unit u)
	{
		return (TripleAUnit) u;
	}
	
	public TripleAUnit(final UnitType type, final PlayerID owner, final GameData data)
	{
		super(type, owner, data);
	}
	
	public Unit getTransportedBy()
	{
		return m_transportedBy;
	}
	
	@GameProperty(xmlProperty = false, gameProperty = true, adds = false)
	public void setTransportedBy(final TripleAUnit transportedBy)
	{
		m_transportedBy = transportedBy;
	}
	
	public List<Unit> getTransporting()
	{
		// we don't store the units we are transporting
		// rather we look at the transported by property of units
		for (final Territory t : getData().getMap())
		{
			// find the territory this transport is in
			if (t.getUnits().getUnits().contains(this))
			{
				return t.getUnits().getMatches(new Match<Unit>()
					{
						@Override
						public boolean match(final Unit o)
						{
							return TripleAUnit.get(o).getTransportedBy() == TripleAUnit.this;
						}
					});
			}
		}
		return Collections.emptyList();
	}
	
	public List<Unit> getTransporting(final Collection<Unit> transportedUnitsPossible)
	{
		// we don't store the units we are transporting
		// rather we look at the transported by property of units
		return Match.getMatches(transportedUnitsPossible, new Match<Unit>()
			{
				@Override
				public boolean match(final Unit o)
				{
					return TripleAUnit.get(o).getTransportedBy() == TripleAUnit.this;
				}
			});
	}
	
	public List<Unit> getUnloaded()
	{
		return m_unloaded;
	}
	
	@GameProperty(xmlProperty = false, gameProperty = true, adds = false)
	public void setUnloaded(final List<Unit> unloaded)
	{
		if (unloaded == null || unloaded.isEmpty())
		{
			m_unloaded = Collections.emptyList();
		}
		else
		{
			m_unloaded = new ArrayList<Unit>(unloaded);
		}
	}
	
	public boolean getWasLoadedThisTurn()
	{
		return m_wasLoadedThisTurn.booleanValue();
	}
	
	@GameProperty(xmlProperty = false, gameProperty = true, adds = false)
	public void setWasLoadedThisTurn(final Boolean value)
	{
		m_wasLoadedThisTurn = Boolean.valueOf(value.booleanValue());
	}
	
	public Territory getUnloadedTo()
	{
		return m_unloadedTo;
	}
	
	@GameProperty(xmlProperty = false, gameProperty = true, adds = false)
	public void setUnloadedTo(final Territory unloadedTo)
	{
		m_unloadedTo = unloadedTo;
	}
	
	public Territory getOriginatedFrom()
	{
		return m_originatedFrom;
	}
	
	@GameProperty(xmlProperty = false, gameProperty = true, adds = false)
	public void setOriginatedFrom(final Territory t)
	{
		m_originatedFrom = t;
	}
	
	public boolean getWasUnloadedInCombatPhase()
	{
		return m_wasUnloadedInCombatPhase.booleanValue();
	}
	
	@GameProperty(xmlProperty = false, gameProperty = true, adds = false)
	public void setWasUnloadedInCombatPhase(final Boolean value)
	{
		m_wasUnloadedInCombatPhase = Boolean.valueOf(value.booleanValue());
	}
	
	public int getAlreadyMoved()
	{
		return m_alreadyMoved;
	}
	
	@GameProperty(xmlProperty = false, gameProperty = true, adds = false)
	public void setAlreadyMoved(final Integer alreadyMoved)
	{
		m_alreadyMoved = alreadyMoved;
	}
	
	@GameProperty(xmlProperty = false, gameProperty = true, adds = false)
	public void setBonusMovement(final Integer bonusMovement)
	{
		m_bonusMovement = bonusMovement;
	}
	
	public int getBonusMovement()
	{
		return m_bonusMovement;
	}
	
	/**
	 * Does not account for any movement already made. Generally equal to UnitType movement
	 */
	public int getMaxMovementAllowed()
	{
		return Math.max(0, m_bonusMovement + UnitAttachment.get(getType()).getMovement(getOwner()));
	}
	
	public int getMovementLeft()
	{
		return Math.max(0, UnitAttachment.get(getType()).getMovement(getOwner()) + m_bonusMovement - m_alreadyMoved);
	}
	
	public int getUnitDamage()
	{
		return m_unitDamage;
	}
	
	@GameProperty(xmlProperty = false, gameProperty = true, adds = false)
	public void setUnitDamage(final Integer unitDamage)
	{
		m_unitDamage = unitDamage;
	}
	
	public boolean getSubmerged()
	{
		return m_submerged;
	}
	
	@GameProperty(xmlProperty = false, gameProperty = true, adds = false)
	public void setSubmerged(final boolean submerged)
	{
		m_submerged = submerged;
	}
	
	public PlayerID getOriginalOwner()
	{
		return m_originalOwner;
	}
	
	@GameProperty(xmlProperty = false, gameProperty = true, adds = false)
	public void setOriginalOwner(final PlayerID originalOwner)
	{
		m_originalOwner = originalOwner;
	}
	
	public boolean getWasInCombat()
	{
		return m_wasInCombat;
	}
	
	@GameProperty(xmlProperty = false, gameProperty = true, adds = false)
	public void setWasInCombat(final Boolean value)
	{
		m_wasInCombat = Boolean.valueOf(value.booleanValue());
	}
	
	public boolean getWasScrambled()
	{
		return m_wasScrambled;
	}
	
	@GameProperty(xmlProperty = false, gameProperty = true, adds = false)
	public void setWasScrambled(final Boolean value)
	{
		m_wasScrambled = Boolean.valueOf(value.booleanValue());
	}
	
	public int getMaxScrambleCount()
	{
		return m_maxScrambleCount;
	}
	
	@GameProperty(xmlProperty = false, gameProperty = true, adds = false)
	public void setMaxScrambleCount(final Integer value)
	{
		m_maxScrambleCount = value;
	}
	
	@GameProperty(xmlProperty = false, gameProperty = true, adds = false)
	public void setWasInAirBattle(final Boolean value)
	{
		m_wasInAirBattle = Boolean.valueOf(value.booleanValue());
	}
	
	public boolean getWasInAirBattle()
	{
		return m_wasInAirBattle;
	}
	
	public boolean getWasLoadedAfterCombat()
	{
		return m_wasLoadedAfterCombat;
	}
	
	@GameProperty(xmlProperty = false, gameProperty = true, adds = false)
	public void setWasLoadedAfterCombat(final Boolean value)
	{
		m_wasLoadedAfterCombat = Boolean.valueOf(value.booleanValue());
	}
	
	public List<Unit> getDependents()
	{
		return getTransporting();
	}
	
	public Unit getDependentOf()
	{
		if (m_transportedBy != null)
			return m_transportedBy;
		// TODO: add support for carriers as well
		return null;
	}
	
	public boolean getWasAmphibious()
	{
		return m_wasAmphibious;
	}
	
	@GameProperty(xmlProperty = false, gameProperty = true, adds = false)
	public void setWasAmphibious(final Boolean value)
	{
		m_wasAmphibious = Boolean.valueOf(value.booleanValue());
	}
	
	public boolean getDisabled()
	{
		return m_disabled;
	}
	
	@GameProperty(xmlProperty = false, gameProperty = true, adds = false)
	public void setDisabled(final Boolean value)
	{
		m_disabled = Boolean.valueOf(value.booleanValue());
	}
	
	/**
	 * How much more damage can this unit take?
	 * Will return 0 if the unit can not be damaged, or is at max damage
	 */
	public int getHowMuchMoreDamageCanThisUnitTake(final Unit u, final Territory t)
	{
		if (!Matches.UnitIsFactoryOrCanBeDamaged.match(u))
			return 0;
		final TripleAUnit taUnit = (TripleAUnit) u;
		if (games.strategy.triplea.Properties.getSBRAffectsUnitProduction(u.getData()))
		{
			final TerritoryAttachment ta = TerritoryAttachment.get(t);
			if (ta == null)
				return 0;
			final int currentDamage = ta.getProduction() - ta.getUnitProduction();
			return (2 * ta.getProduction()) - currentDamage;
		}
		else if (games.strategy.triplea.Properties.getDamageFromBombingDoneToUnitsInsteadOfTerritories(u.getData()))
		{
			return Math.max(0, getHowMuchDamageCanThisUnitTakeTotal(u, t) - taUnit.getUnitDamage());
		}
		else
			return Integer.MAX_VALUE;
	}
	
	/**
	 * How much damage is the max this unit can take, accounting for territory, etc.
	 * Will return -1 if the unit is of the type that can not be damaged
	 */
	public int getHowMuchDamageCanThisUnitTakeTotal(final Unit u, final Territory t)
	{
		if (!Matches.UnitIsFactoryOrCanBeDamaged.match(u))
			return -1;
		final UnitAttachment ua = UnitAttachment.get(u.getType());
		final TerritoryAttachment ta = TerritoryAttachment.get(t);
		int territoryProduction = 0;
		int territoryUnitProduction = 0;
		if (ta != null)
		{
			territoryProduction = ta.getProduction();
			territoryUnitProduction = ta.getUnitProduction();
		}
		if (games.strategy.triplea.Properties.getSBRAffectsUnitProduction(u.getData()))
			return territoryProduction * 2;
		else if (games.strategy.triplea.Properties.getDamageFromBombingDoneToUnitsInsteadOfTerritories(u.getData()))
		{
			if (ua.getMaxDamage() <= 0)
			{
				// factories may or may not have max damage set, so we must still determine here
				// assume that if maxDamage <= 0, then the max damage must be based on the territory value
				return territoryUnitProduction * 2; // can use "production" or "unitProduction"
			}
			else
			{
				if (Matches.UnitIsFactoryOrCanProduceUnits.match(u))
				{
					if (ua.getCanProduceXUnits() < 0)
					{
						return territoryUnitProduction * ua.getMaxDamage(); // can use "production" or "unitProduction"
					}
					else
					{
						return ua.getMaxDamage();
					}
				}
				else
				{
					return ua.getMaxDamage();
				}
			}
		}
		else
			return Integer.MAX_VALUE;
	}
	
	public int getHowMuchCanThisUnitBeRepaired(final Unit u, final Territory t)
	{
		return Math.max(0, (this.getHowMuchDamageCanThisUnitTakeTotal(u, t) - this.getHowMuchMoreDamageCanThisUnitTake(u, t)));
	}
	
	public int getHowMuchShouldUnitBeRepairedToNotBeDisabled(final Unit u, final Territory t)
	{
		final UnitAttachment ua = UnitAttachment.get(u.getType());
		final int maxOperationalDamage = ua.getMaxOperationalDamage();
		if (maxOperationalDamage < 0)
			return 0;
		final TripleAUnit taUnit = (TripleAUnit) u;
		final int currentDamage = taUnit.getUnitDamage();
		return Math.max(0, currentDamage - maxOperationalDamage);
	}
	
	public static int getProductionPotentialOfTerritory(final Collection<Unit> unitsAtStartOfStepInTerritory, final Territory producer, final PlayerID player, final GameData data,
				final boolean accountForDamage, final boolean mathMaxZero)
	{
		return getHowMuchCanUnitProduce(getBiggestProducer(unitsAtStartOfStepInTerritory, producer, player, data, accountForDamage), producer, player, data, accountForDamage, mathMaxZero);
	}
	
	public static Unit getBiggestProducer(final Collection<Unit> units, final Territory producer, final PlayerID player, final GameData data, final boolean accountForDamage)
	{
		final Collection<Unit> factories = Match.getMatches(units, Matches.UnitIsOwnedAndIsFactoryOrCanProduceUnits(player));
		if (factories.isEmpty())
			return null;
		final IntegerMap<Unit> productionPotential = new IntegerMap<Unit>();
		Unit highestUnit = factories.iterator().next();
		int highestCapacity = Integer.MIN_VALUE;
		for (final Unit u : factories)
		{
			final int capacity = getHowMuchCanUnitProduce(u, producer, player, data, accountForDamage, false);
			productionPotential.put(u, capacity);
			if (capacity > highestCapacity)
			{
				highestCapacity = capacity;
				highestUnit = u;
			}
		}
		return highestUnit;
	}
	
	public static int getHowMuchCanUnitProduce(final Unit u, final Territory producer, final PlayerID player, final GameData data, final boolean accountForDamage, final boolean mathMaxZero)
	{
		if (u == null)
			return 0;
		if (!Matches.UnitIsFactoryOrCanProduceUnits.match(u))
			return 0;
		int productionCapacity = 0;
		final UnitAttachment ua = UnitAttachment.get(u.getType());
		final TripleAUnit taUnit = (TripleAUnit) u;
		final TerritoryAttachment ta = TerritoryAttachment.get(producer);
		int territoryProduction = 0;
		int territoryUnitProduction = 0;
		if (ta != null)
		{
			territoryProduction = ta.getProduction();
			territoryUnitProduction = ta.getUnitProduction();
		}
		if (accountForDamage)
		{
			if (games.strategy.triplea.Properties.getSBRAffectsUnitProduction(data))
			{
				if (ua.getCanProduceXUnits() < 0)
					productionCapacity = territoryUnitProduction;
				else
					productionCapacity = ua.getCanProduceXUnits() - (territoryProduction - territoryUnitProduction);
			}
			else if (games.strategy.triplea.Properties.getDamageFromBombingDoneToUnitsInsteadOfTerritories(data))
			{
				if (ua.getCanProduceXUnits() < 0)
					productionCapacity = territoryUnitProduction - taUnit.getUnitDamage(); // we could use territoryUnitProduction OR territoryProduction if we wanted to, however we should change damage to be based on whichever we choose.
				else
					productionCapacity = ua.getCanProduceXUnits() - taUnit.getUnitDamage();
			}
			else
			{
				productionCapacity = territoryProduction;
				if (productionCapacity < 1)
					productionCapacity = 1;
			}
		}
		else
		{
			if (ua.getCanProduceXUnits() < 0 && !games.strategy.triplea.Properties.getDamageFromBombingDoneToUnitsInsteadOfTerritories(data))
				productionCapacity = territoryProduction;
			else if (ua.getCanProduceXUnits() < 0 && games.strategy.triplea.Properties.getDamageFromBombingDoneToUnitsInsteadOfTerritories(data))
				productionCapacity = territoryUnitProduction;
			else
				productionCapacity = ua.getCanProduceXUnits();
			if (productionCapacity < 1 && !games.strategy.triplea.Properties.getSBRAffectsUnitProduction(data)
						&& !games.strategy.triplea.Properties.getDamageFromBombingDoneToUnitsInsteadOfTerritories(data))
				productionCapacity = 1;
		}
		// Increase production if have industrial technology
		if (territoryProduction >= TechAbilityAttachment.getMinimumTerritoryValueForProductionBonus(player, data))
			productionCapacity += TechAbilityAttachment.getProductionBonus(u.getType(), player, data);
		if (mathMaxZero)
			return Math.max(0, productionCapacity);
		else
			return productionCapacity;
	}
	
	/**
	 * Currently made for translating unit damage from one unit to another unit. Will adjust damage to be within max damage for the new units.
	 * 
	 * @param unitGivingAttributes
	 * @param unitsThatWillGetAttributes
	 * @param t
	 * @return change for unit's properties
	 */
	public static Change translateAttributesToOtherUnits(final Unit unitGivingAttributes, final Collection<Unit> unitsThatWillGetAttributes, final Territory t)
	{
		final CompositeChange changes = new CompositeChange();
		// must look for m_hits, m_unitDamage,
		final TripleAUnit taUnit = (TripleAUnit) unitGivingAttributes;
		final int combatDamage = taUnit.getHits();
		final IntegerMap<Unit> hits = new IntegerMap<Unit>();
		if (combatDamage > 0)
		{
			for (final Unit u : unitsThatWillGetAttributes)
			{
				hits.put(u, combatDamage);
			}
		}
		if (hits.size() > 0)
			changes.add(ChangeFactory.unitsHit(hits));
		final int unitDamage = taUnit.getUnitDamage();
		if (unitDamage > 0)
		{
			for (final Unit u : unitsThatWillGetAttributes)
			{
				final TripleAUnit taNew = (TripleAUnit) u;
				final int maxDamage = taNew.getHowMuchDamageCanThisUnitTakeTotal(u, t);
				final int transferDamage = Math.max(0, Math.min(unitDamage, maxDamage));
				if (transferDamage <= 0)
					continue;
				changes.add(ChangeFactory.unitPropertyChange(u, transferDamage, TripleAUnit.UNIT_DAMAGE));
			}
		}
		return changes;
	}
}
