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

/*
 * Battle.java
 * 
 * Created on November 15, 2001, 12:39 PM
 * 
 * @version $LastChangedDate$
 */

package games.strategy.triplea.delegate;

import games.strategy.engine.GameOverException;
import games.strategy.engine.data.Change;
import games.strategy.engine.data.ChangeFactory;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.message.ConnectionLostException;
import games.strategy.net.GUID;
import games.strategy.triplea.Constants;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.attatchments.TechAttachment;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.triplea.delegate.dataObjects.CasualtyDetails;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.triplea.player.ITripleaPlayer;
import games.strategy.triplea.ui.BattleDisplay;
import games.strategy.triplea.ui.display.ITripleaDisplay;
import games.strategy.triplea.util.UnitSeperator;
import games.strategy.triplea.weakAI.WeakAI;
import games.strategy.util.CompositeMatch;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.CompositeMatchOr;
import games.strategy.util.IntegerMap;
import games.strategy.util.InverseMatch;
import games.strategy.util.Match;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 
 * Handles logic for battles in which fighting actually occurs.
 * 
 * @author Sean Bridges
 * 
 */

public class MustFightBattle implements Battle, BattleStepStrings
{
	
	public static final String RETREAT_PLANES = " retreat planes?";
	public static final String AIRBASE = "air base";
	
	
	public static enum ReturnFire
	{
		ALL, SUBS, NONE
	}
	

	// these class exist for testing
	public static abstract class AttackSubs implements IExecutable
	{
	}
	

	public static abstract class DefendSubs implements IExecutable
	{
	}
	
	// compatible with 0.9.0.2 saved games
	private static final long serialVersionUID = 5879502298361231540L;
	
	public static final int DEFAULT_RETREAT_TYPE = 0;
	public static final int SUBS_RETREAT_TYPE = 1;
	public static final int PLANES_RETREAT_TYPE = 2;
	public static final int PARTIAL_AMPHIB_RETREAT_TYPE = 3;
	
	private final Territory m_battleSite;
	
	// In headless mode we are just being used to calculate results
	// for an odds calculator
	// we can skip some steps for effeciency.
	// as well, in headless mode we should
	// not access Delegates
	private boolean m_headless = false;
	
	// maps Territory-> units
	// stores a collection of who is attacking from where, needed
	// for undoing moves
	private Map<Territory, Collection<Unit>> m_attackingFromMap = new HashMap<Territory, Collection<Unit>>();
	private List<Unit> m_attackingUnits = new ArrayList<Unit>();
	
	private Collection<Unit> m_attackingWaitingToDie = new ArrayList<Unit>();
	private Set<Territory> m_attackingFrom = new HashSet<Territory>();
	private Collection<Territory> m_amphibiousAttackFrom = new ArrayList<Territory>();
	private Collection<Unit> m_amphibiousLandAttackers = new ArrayList<Unit>();
	private List<Unit> m_defendingUnits = new ArrayList<Unit>();
	private Collection<Unit> m_defendingWaitingToDie = new ArrayList<Unit>();
	private Collection<Unit> m_bombardingUnits = new ArrayList<Unit>();
	private boolean m_amphibious = false;
	private boolean m_over = false;
	private BattleTracker m_tracker;
	private Collection<Unit> m_defendingAir = new ArrayList<Unit>();
	
	private PlayerID m_defender;
	private PlayerID m_attacker;
	
	private GameData m_data;
	
	private final GUID m_battleID = new GUID();
	
	// dependent units
	// maps unit -> Collection of units
	// if unit is lost in a battle we are dependent on
	// then we lose the corresponding collection of units
	private Map<Unit, Collection<Unit>> m_dependentUnits = new HashMap<Unit, Collection<Unit>>();
	
	// keep track of all the units that die in the battle to show in the history
	// window
	private Collection<Unit> m_killed = new ArrayList<Unit>();
	private Collection<Unit> m_scrambled = new ArrayList<Unit>();
	
	private int m_round = 0;
	
	// our current execution state
	// we keep a stack of executables
	// this allows us to save our state
	// and resume while in the middle of a battle
	private final ExecutionStack m_stack = new ExecutionStack();
	private List<String> m_stepStrings;
	
	private TransportTracker getTransportTracker()
	{
		return new TransportTracker();
	}
	
	public MustFightBattle(Territory battleSite, PlayerID attacker,
				GameData data, BattleTracker tracker)
	{
		m_data = data;
		m_tracker = tracker;
		m_battleSite = battleSite;
		m_attacker = attacker;
		
		m_defendingUnits.addAll(m_battleSite.getUnits().getMatches(
					Matches.enemyUnit(attacker, data)));
		m_defender = findDefender(battleSite);
	}
	
	/**
	 * Used for headless battles
	 */
	public void setUnits(Collection<Unit> defending, Collection<Unit> attacking, Collection<Unit> bombarding, PlayerID defender)
	{
		m_defendingUnits = new ArrayList<Unit>(defending);
		m_attackingUnits = new ArrayList<Unit>(attacking);
		m_bombardingUnits = new ArrayList<Unit>(bombarding);
		m_defender = defender;
	}
	
	public void setHeadless(boolean aBool)
	{
		m_headless = aBool;
	}
	
	private boolean canSubsSubmerge()
	{
		return games.strategy.triplea.Properties.getSubmersible_Subs(m_data);
	}
	
	private boolean getScramble_Rules_In_Effect()
	{
		return games.strategy.triplea.Properties.getScramble_Rules_In_Effect(m_data);
	}
	
	private boolean getScramble_From_Islands_Only()
	{
		return games.strategy.triplea.Properties.getScramble_From_Island_Only(m_data);
	}
	
	private boolean getScramble_To_Sea_Only()
	{
		return games.strategy.triplea.Properties.getScramble_To_Sea_Only(m_data);
	}
	
	private void determineScrambledUnits(IDelegateBridge bridge)
	{
		// If we can only scramble to water and it's not, nevermind
		if (!m_battleSite.isWater() && getScramble_To_Sea_Only())
		{
			return;
		}
		
		// Get the unit types that can scramble
		Collection<UnitType> unitTypesCanScramble = new ArrayList<UnitType>();
		int maxScrambleDistance = 1;
		// Get all the players except the attacker
		for (PlayerID p : m_data.getPlayerList())
		{
			// Get their production rules
			if (!p.equals(m_attacker) && !p.equals(PlayerID.NULL_PLAYERID))
			{
				List<ProductionRule> rules = p.getProductionFrontier().getRules();
				
				// Get the unit types that can scramble
				// TODO: kev, why on earth are you looking at production rules here. what if the player has a unit that can scramble, but they can't build it? then it would not be in their production rules!
				for (ProductionRule rule : rules)
				{
					UnitType ut = (UnitType) rule.getResults().keySet().iterator().next();
					if (!unitTypesCanScramble.contains(ut) && UnitAttachment.get(ut).getCanScramble())
					{
						unitTypesCanScramble.add(ut);
						// Get their maxScrambleDistance
						maxScrambleDistance = UnitAttachment.get(ut).getMaxScrambleDistance() > maxScrambleDistance ? UnitAttachment.get(ut).getMaxScrambleDistance() : maxScrambleDistance;
					}
				}
			}
		}
		
		// See if there are any airbases within that distance that are operable
		Collection<Territory> neighbors = m_data.getMap().getNeighbors(m_battleSite, maxScrambleDistance);
		Collection<Territory> neighborsWithActiveAirbases = new ArrayList<Territory>();
		
		boolean scrambleFromIsland = getScramble_From_Islands_Only();
		
		for (Territory t : neighbors)
		{
			// If we scramble from islands only, and it's not- skip it
			if (scrambleFromIsland && m_data.getMap().getNeighbors(t).size() != 1)
				continue;
			
			// check if the airbase(s) is/are operational
			if (t.getUnits().someMatch(Matches.UnitIsAirBase) && t.getUnits().someMatch(Matches.UnitCanScramble))
			{
				for (Unit u : t.getUnits())
				{
					// TODO we can add other types of bases here as well (naval base, fire base, etc...)
					// if it's an airbase and operational
					if (Matches.UnitIsAirBase.match(u) && Matches.UnitIsDisabled().invert().match(u))
					{
						neighborsWithActiveAirbases.add(t);
						break;
					}
				}
			}
		}
		
		if (neighborsWithActiveAirbases.isEmpty())
		{
			return;
		}
		
		// Ask the aircraft owners if they wish to scramble the units
		queryScrambleUnits(SCRAMBLE_UNITS, bridge, neighborsWithActiveAirbases);
	}
	
	@Override
	public boolean isOver()
	{
		return m_over;
	}
	
	@Override
	public void removeAttack(Route route, Collection<Unit> units)
	{
		m_attackingUnits.removeAll(units);
		
		// the route could be null, in the case of a unit in a territory where a sub is submerged.
		if (route == null)
			return;
		Territory attackingFrom = getAttackFrom(route);
		
		Collection<Unit> attackingFromMapUnits = m_attackingFromMap
					.get(attackingFrom);
		// handle possible null pointer
		if (attackingFromMapUnits == null)
		{
			attackingFromMapUnits = new ArrayList<Unit>();
		}
		attackingFromMapUnits.removeAll(units);
		if (attackingFromMapUnits.isEmpty())
		{
			m_attackingFrom.remove(attackingFrom);
		}
		
		// deal with amphibious assaults
		if (attackingFrom.isWater())
		{
			if (route.getEnd() != null
						&& !route.getEnd().isWater()
						&& Match.someMatch(units, Matches.UnitIsLand))
			{
				m_amphibiousLandAttackers.removeAll(Match.getMatches(units, Matches.UnitIsLand));
			}
			
			// if none of the units is a land unit, the attack from
			// that territory is no longer an amphibious assault
			if (Match.noneMatch(attackingFromMapUnits, Matches.UnitIsLand))
			{
				m_amphibiousAttackFrom.remove(attackingFrom);
				// do we have any amphibious attacks left?
				m_amphibious = !m_amphibiousAttackFrom.isEmpty();
			}
		}
		
		Iterator<Unit> dependentHolders = m_dependentUnits.keySet().iterator();
		while (dependentHolders.hasNext())
		{
			Unit holder = dependentHolders.next();
			Collection<Unit> dependents = m_dependentUnits.get(holder);
			dependents.removeAll(units);
		}
	}
	
	@Override
	public boolean isEmpty()
	{
		return m_attackingUnits.isEmpty() && m_attackingWaitingToDie.isEmpty();
	}
	
	@Override
	public Change addAttackChange(Route route, Collection<Unit> units)
	{
		CompositeChange change = new CompositeChange();
		
		// Filter out allied units if WW2V2
		Match<Unit> ownedBy = Matches.unitIsOwnedBy(m_attacker);
		Collection<Unit> attackingUnits = isWW2V2() ? Match.getMatches(units,
					ownedBy) : units;
		
		Territory attackingFrom = getAttackFrom(route);
		
		m_attackingFrom.add(attackingFrom);
		
		m_attackingUnits.addAll(attackingUnits);
		
		if (m_attackingFromMap.get(attackingFrom) == null)
		{
			m_attackingFromMap.put(attackingFrom, new ArrayList<Unit>());
		}
		{
			Collection<Unit> attackingFromMapUnits = m_attackingFromMap
						.get(attackingFrom);
			attackingFromMapUnits.addAll(attackingUnits);
		}
		
		// are we amphibious
		if (route.getStart().isWater() && route.getEnd() != null
					&& !route.getEnd().isWater()
					&& Match.someMatch(attackingUnits, Matches.UnitIsLand))
		{
			m_amphibiousAttackFrom.add(getAttackFrom(route));
			m_amphibiousLandAttackers.addAll(Match.getMatches(attackingUnits,
						Matches.UnitIsLand));
			m_amphibious = true;
		}
		
		// TODO add dependencies for transported units?
		Map<Unit, Collection<Unit>> dependencies = transporting(units);
		
		if (isAlliedAirDependents())
		{
			dependencies.putAll(MoveValidator.carrierMustMoveWith(units, units, m_data, m_attacker));
			for (Unit carrier : dependencies.keySet())
			{
				UnitAttachment ua = UnitAttachment.get(carrier.getUnitType());
				if (ua.getCarrierCapacity() == -1)
					continue;
				Collection<Unit> fighters = dependencies.get(carrier);
				// Dependencies count both land and air units. Land units could be allied or owned, while air is just allied since owned already launched at beginning of turn
				fighters.retainAll(Match.getMatches(fighters, Matches.UnitIsAir));
				for (Unit fighter : fighters)
				{
					// Set transportedBy for fighter
					change.add(ChangeFactory.unitPropertyChange(fighter, carrier, TripleAUnit.TRANSPORTED_BY));
				}
				// remove transported fighters from battle display
				m_attackingUnits.removeAll(fighters);
			}
		}
		
		// Set the dependent paratroopers so they die if the bomber dies.
		if (isParatroopers(m_attacker))
		{
			Collection<Unit> airTransports = Match.getMatches(units, Matches.UnitIsAirTransport);
			Collection<Unit> paratroops = Match.getMatches(units, Matches.UnitIsAirTransportable);
			if (!airTransports.isEmpty() && !paratroops.isEmpty())
			{
				// Load capable bombers by default>
				Map<Unit, Unit> unitsToCapableAirTransports = MoveDelegate.mapAirTransports(route, paratroops, airTransports, true, m_attacker);
				
				HashMap<Unit, Collection<Unit>> dependentUnits = new HashMap<Unit, Collection<Unit>>();
				Collection<Unit> singleCollection = new ArrayList<Unit>();
				for (Unit unit : unitsToCapableAirTransports.keySet())
				{
					Collection<Unit> unitList = new ArrayList<Unit>();
					unitList.add(unit);
					Unit bomber = unitsToCapableAirTransports.get(unit);
					singleCollection.add(unit);
					
					// Set transportedBy for paratrooper
					change.add(ChangeFactory.unitPropertyChange(unit, bomber, TripleAUnit.TRANSPORTED_BY));
					
					// Set the dependents
					if (dependentUnits.get(bomber) != null)
						dependentUnits.get(bomber).addAll(unitList);
					else
						dependentUnits.put(bomber, unitList);
				}
				
				dependencies.putAll(dependentUnits);
				
				UnitSeperator.categorize(airTransports, dependentUnits, false, false);
			}
		}
		
		addDependentUnits(dependencies);
		
		// mark units with no movement
		// for all but air
		Collection<Unit> nonAir = Match.getMatches(attackingUnits,
					Matches.UnitIsNotAir);
		
		// we dont want to change the movement of transported land units if this is a sea battle
		// so restrict non air to remove land units
		if (m_battleSite.isWater())
			nonAir = Match.getMatches(nonAir, Matches.UnitIsNotLand);
		
		// TODO This checks for ignored sub/trns and skips the set of the attackers to 0 movement left
		// If attacker stops in an occupied territory, movement stops (battle is optional)
		if (MoveValidator.onlyIgnoredUnitsOnPath(route, m_attacker, m_data, false))
			return change;
		
		change.add(ChangeFactory.markNoMovementChange(nonAir));
		return change;
	}
	
	@Override
	public Change addCombatChange(Route route, Collection<Unit> units, PlayerID player)
	{
		CompositeChange change = new CompositeChange();
		
		// Filter out allied units if WW2V2
		Match<Unit> ownedBy = Matches.unitIsOwnedBy(player);
		Collection<Unit> fightingUnits = isWW2V2() ? Match.getMatches(units, ownedBy) : units;
		
		Territory fightingFrom = getAttackFrom(route);
		
		m_attackingFrom.add(fightingFrom);
		
		if (player == m_attacker)
		{
			m_attackingUnits.addAll(fightingUnits);
		}
		else
		{
			m_defendingUnits.addAll(fightingUnits);
		}
		
		/* if (m_attackingFromMap.get(fightingFrom) == null)
		 {
		     m_attackingFromMap.put(fightingFrom, new ArrayList<Unit>());
		 }
		 
		 Collection<Unit> fightingFromMapUnits = m_attackingFromMap.get(fightingFrom);
		 fightingFromMapUnits.addAll(fightingUnits);
		*/

		/*//are we amphibious
		if (route.getStart().isWater() && route.getEnd() != null
		        && !route.getEnd().isWater()
		        && Match.someMatch(fightingUnits, Matches.UnitIsLand))
		{
		    m_amphibiousAttackFrom.add(getAttackFrom(route));
		    m_amphibiousLandAttackers.addAll(Match.getMatches(fightingUnits,
		            Matches.UnitIsLand));
		    m_amphibious = true;
		}*/

		/*//TODO add dependencies for transported units?
		  Map<Unit, Collection<Unit>> dependencies = transporting(units);
		  
		  if (isAlliedAirDependents())
		  {
		      dependencies.putAll(MoveValidator.carrierMustMoveWith(units, units, m_data, m_attacker));
		      for(Unit carrier : dependencies.keySet())
		      {            	
		      	UnitAttachment ua = UnitAttachment.get(carrier.getUnitType());
		      	if (ua.getCarrierCapacity() == -1)
		      		continue;
		      	Collection<Unit> fighters = dependencies.get(carrier);
		      	for (Unit fighter : fighters)
		      	{
		      		//Set transportedBy for fighter
		      		change.add(ChangeFactory.unitPropertyChange(fighter, carrier, TripleAUnit.TRANSPORTED_BY ));
		      	}
		      	//remove transported fighters from battle display
		      	m_attackingUnits.removeAll(fighters);
		      }
		  }
		  
		  //Set the dependent paratroopers so they die if the bomber dies.        
		  if(isParatroopers(m_attacker))
		  {
		      Collection<Unit> airTransports = Match.getMatches(units, Matches.UnitIsAirTransport);
		      Collection<Unit> paratroops = Match.getMatches(units, Matches.UnitIsAirTransportable);
		      if(!airTransports.isEmpty() && !paratroops.isEmpty())
		      {
		      	//Load capable bombers by default>
		      	Map<Unit,Unit> unitsToCapableAirTransports = MoveDelegate.mapAirTransports(route, paratroops, airTransports, true, m_attacker);

		      	HashMap<Unit, Collection<Unit>> dependentUnits = new HashMap<Unit, Collection<Unit>>();
		      	Collection<Unit> singleCollection = new ArrayList<Unit>();
		      	for (Unit unit : unitsToCapableAirTransports.keySet())
		      	{
		              Collection<Unit> unitList = new ArrayList<Unit>();
		              unitList.add(unit);
		      		Unit bomber = unitsToCapableAirTransports.get(unit);                
		      		singleCollection.add(unit);

		      		//Set transportedBy for paratrooper
		      		change.add(ChangeFactory.unitPropertyChange(unit, bomber, TripleAUnit.TRANSPORTED_BY ));            	

		      		//Set the dependents
		      		if (dependentUnits.get(bomber) != null)
		      			dependentUnits.get(bomber).addAll(unitList);
		      		else
		      			dependentUnits.put(bomber, unitList);
		      	}

		      	dependencies.putAll(dependentUnits);

		      	UnitSeperator.categorize(airTransports, dependentUnits, false, false);
		      }
		  }
		  
		  
		  addDependentUnits(dependencies);*/

		// mark units with no movement
		// for all but air
		Collection<Unit> nonAir = Match.getMatches(fightingUnits, Matches.UnitIsNotAir);
		
		// we dont want to change the movement of transported land units if this is a sea battle
		// so restrict non air to remove land units
		if (m_battleSite.isWater())
			nonAir = Match.getMatches(nonAir, Matches.UnitIsNotLand);
		
		// TODO This checks for ignored sub/trns and skips the set of the attackers to 0 movement left
		// If attacker stops in an occupied territory, movement stops (battle is optional)
		if (MoveValidator.onlyIgnoredUnitsOnPath(route, player, m_data, false))
			return change;
		
		change.add(ChangeFactory.markNoMovementChange(nonAir));
		return change;
	}
	
	private void addDependentUnits(Map<Unit, Collection<Unit>> dependencies)
	{
		Iterator iter = dependencies.keySet().iterator();
		while (iter.hasNext())
		{
			Unit holder = (Unit) iter.next();
			Collection<Unit> transporting = dependencies.get(holder);
			if (m_dependentUnits.get(holder) != null)
				m_dependentUnits.get(holder)
							.addAll(transporting);
			else
				m_dependentUnits.put(holder, new LinkedHashSet<Unit>(transporting));
		}
	}
	
	private Territory getAttackFrom(Route route)
	{
		int routeSize = route.getLength();
		
		if (routeSize <= 1)
			return route.getStart();
		else
			return route.at(routeSize - 2);
	}
	
	private String getBattleTitle()
	{
		
		return m_attacker.getName() + " attack " + m_defender.getName()
					+ " in " + m_battleSite.getName();
	}
	
	private PlayerID findDefender(Territory battleSite)
	{
		
		if (!battleSite.isWater())
			return battleSite.getOwner();
		// if water find the defender based on who has the most units in the
		// territory
		IntegerMap<PlayerID> players = battleSite.getUnits().getPlayerUnitCounts();
		int max = -1;
		PlayerID defender = null;
		Iterator<PlayerID> iter = players.keySet().iterator();
		while (iter.hasNext())
		{
			PlayerID current = iter.next();
			if (m_data.getRelationshipTracker().isAllied(m_attacker, current)
						|| current.equals(m_attacker))
				continue;
			int count = players.getInt(current);
			if (count > max)
			{
				max = count;
				defender = current;
			}
		}
		if (max == -1)
		{
			// this is ok, we are a headless battle
		}
		
		return defender;
	}
	
	@Override
	public boolean isBombingRun()
	{
		return false;
	}
	
	@Override
	public Territory getTerritory()
	{
		return m_battleSite;
	}
	
	@Override
	public int hashCode()
	{
		return m_battleSite.hashCode();
	}
	
	@Override
	public boolean equals(Object o)
	{
		
		// 2 battles are equal if they are both the same type (boming or not)
		// and occur on the same territory
		// equals in the sense that they should never occupy the same Set
		// if these conditions are met
		if (o == null || !(o instanceof Battle))
			return false;
		
		Battle other = (Battle) o;
		return other.getTerritory().equals(this.m_battleSite)
					&& other.isBombingRun() == this.isBombingRun();
	}
	
	private void removeUnitsThatNoLongerExist()
	{
		// we were having a problem with units that had been killed previously were still part of MFB's variables, so we double check that the stuff still exists here.
		m_defendingUnits.retainAll(m_battleSite.getUnits().getUnits());
		m_attackingUnits.retainAll(m_battleSite.getUnits().getUnits());
	}
	
	@Override
	public void fight(IDelegateBridge bridge)
	{
		// remove units that may already be dead due to a previous event (like they died from a strategic bombing raid, rocket attack, or during scrambling, etc)
		removeUnitsThatNoLongerExist();
		
		// we have already started
		if (m_stack.isExecuting())
		{
			ITripleaDisplay display = getDisplay(bridge);
			display.showBattle(m_battleID, m_battleSite, getBattleTitle(), removeNonCombatants(m_attackingUnits, true, m_attacker), removeNonCombatants(m_defendingUnits, false, m_defender),
						m_dependentUnits, m_attacker, m_defender);
			
			display.listBattleSteps(m_battleID, m_stepStrings);
			
			m_stack.execute(bridge);
			return;
		}
		
		bridge.getHistoryWriter().startEvent("Battle in " + m_battleSite);
		bridge.getHistoryWriter().setRenderingData(m_battleSite);
		removeAirNoLongerInTerritory();
		
		writeUnitsToHistory(bridge);
		
		// it is possible that no attacking units are present, if so end now
		// changed to only look at units that can be destroyed in combat, and therefore not include factories, aaguns, and infrastructure.
		if (Match.getMatches(m_attackingUnits, Matches.UnitIsDestructibleInCombatShort).size() == 0)
		{
			endBattle(bridge);
			defenderWins(bridge);
			return;
		}
		
		// it is possible that no defending units exist
		// changed to only look at units that can be destroyed in combat, and therefore not include factories, aaguns, and infrastructure.
		if (Match.getMatches(m_defendingUnits, Matches.UnitIsDestructibleInCombatShort).size() == 0)
		{
			endBattle(bridge);
			attackerWins(bridge);
			return;
		}
		
		addDependentUnits(transporting(m_defendingUnits));
		addDependentUnits(transporting(m_attackingUnits));
		
		// list the steps
		m_stepStrings = determineStepStrings(true, bridge);
		
		ITripleaDisplay display = getDisplay(bridge);
		display.showBattle(m_battleID, m_battleSite, getBattleTitle(), removeNonCombatants(m_attackingUnits, true, m_attacker), removeNonCombatants(m_defendingUnits, false, m_defender),
					m_dependentUnits, m_attacker, m_defender);
		
		display.listBattleSteps(m_battleID, m_stepStrings);
		
		if (!m_headless)
		{
			// take the casualties with least movement first
			if (isAmphibious())
				sortAmphib(m_attackingUnits, m_data);
			else
				BattleCalculator.sortPreBattle(m_attackingUnits, m_data);
			BattleCalculator.sortPreBattle(m_defendingUnits, m_data);
		}
		
		// push on stack in opposite order of execution
		pushFightLoopOnStack(bridge);
		pushFightStartOnStack();
		m_stack.execute(bridge);
	}
	
	private void writeUnitsToHistory(IDelegateBridge bridge)
	{
		Set<PlayerID> playerSet = m_battleSite.getUnits().getPlayersWithUnits();
		
		String transcriptText;
		
		// find all attacking players (unsorted)
		Collection<PlayerID> attackers = new ArrayList<PlayerID>();
		Collection<Unit> allAttackingUnits = new ArrayList<Unit>();
		transcriptText = "";
		for (PlayerID current : playerSet)
		{
			if (m_data.getRelationshipTracker().isAllied(m_attacker, current)
						|| current.equals(m_attacker))
				attackers.add(current);
		}
		
		// find all attacking units (unsorted)
		for (Iterator attackersIter = attackers.iterator(); attackersIter.hasNext();)
		{
			PlayerID current = (PlayerID) attackersIter.next();
			String delim;
			if (attackersIter.hasNext())
				delim = "; ";
			else
				delim = "";
			Collection<Unit> attackingUnits = Match.getMatches(m_attackingUnits, Matches.unitIsOwnedBy(current));
			
			String verb = current.equals(m_attacker) ? "attack" : "loiter and taunt";
			transcriptText += current.getName() + " " + verb + " with "
							+ MyFormatter.unitsToTextNoOwner(attackingUnits)
							+ delim;
			allAttackingUnits.addAll(attackingUnits);
			
			// If any attacking transports are in the battle, set their status to later restrict load/unload
			if (current.equals(m_attacker))
			{
				CompositeChange change = new CompositeChange();
				Collection<Unit> transports = Match.getMatches(attackingUnits, Matches.UnitCanTransport);
				Iterator<Unit> attackTranIter = transports.iterator();
				
				while (attackTranIter.hasNext())
				{
					change.add(ChangeFactory.unitPropertyChange(attackTranIter.next(), true, TripleAUnit.WAS_IN_COMBAT));
				}
				bridge.addChange(change);
			}
		}
		// write attacking units to history
		if (m_attackingUnits.size() > 0)
			bridge.getHistoryWriter().addChildToEvent(transcriptText, allAttackingUnits);
		
		// find all defending players (unsorted)
		Collection<PlayerID> defenders = new ArrayList<PlayerID>();
		Collection<Unit> allDefendingUnits = new ArrayList<Unit>();
		transcriptText = "";
		for (PlayerID current : playerSet)
		{
			if (m_data.getRelationshipTracker().isAllied(m_defender, current)
						|| current.equals(m_defender))
			{
				defenders.add(current);
			}
		}
		
		// find all defending units (unsorted)
		for (Iterator defendersIter = defenders.iterator(); defendersIter.hasNext();)
		{
			PlayerID current = (PlayerID) defendersIter.next();
			Collection<Unit> defendingUnits;
			String delim;
			if (defendersIter.hasNext())
				delim = "; ";
			else
				delim = "";
			defendingUnits = Match.getMatches(m_defendingUnits, Matches.unitIsOwnedBy(current));
			
			transcriptText += current.getName() + " defend with "
							+ MyFormatter.unitsToTextNoOwner(defendingUnits)
							+ delim;
			allDefendingUnits.addAll(defendingUnits);
		}
		// write defending units to history
		if (m_defendingUnits.size() > 0)
			bridge.getHistoryWriter().addChildToEvent(transcriptText, allDefendingUnits);
	}
	
	private void removeAirNoLongerInTerritory()
	{
		if (m_headless)
			return;
		
		// remove any air units that were once in this attack, but have now
		// moved out of the territory
		// this is an ilegant way to handle this bug
		CompositeMatch<Unit> airNotInTerritory = new CompositeMatchAnd<Unit>();
		airNotInTerritory.add(new InverseMatch<Unit>(Matches
					.unitIsInTerritory(m_battleSite)));
		
		m_attackingUnits.removeAll(Match.getMatches(m_attackingUnits,
					airNotInTerritory));
	}
	
	public List<String> determineStepStrings(boolean showFirstRun, IDelegateBridge bridge)
	{
		List<String> steps = new ArrayList<String>();
		
		if (showFirstRun)
		{
			if (canFireAA())
			{
				steps.add(AA_GUNS_FIRE);
				steps.add(SELECT_AA_CASUALTIES);
				steps.add(REMOVE_AA_CASUALTIES);
			}
			
			if (!m_battleSite.isWater() && !getBombardingUnits().isEmpty())
			{
				steps.add(NAVAL_BOMBARDMENT);
				steps.add(SELECT_NAVAL_BOMBARDMENT_CASUALTIES);
			}
			
			if (Match.someMatch(m_attackingUnits, Matches.UnitIsSuicide))
			{
				steps.add(SUICIDE_ATTACK);
				steps.add(m_defender.getName() + SELECT_CASUALTIES_SUICIDE);
			}
			
			if (Match.someMatch(m_defendingUnits, Matches.UnitIsSuicide) && !isDefendingSuicideAndMunitionUnitsDoNotFire())
			{
				steps.add(SUICIDE_DEFEND);
				steps.add(m_attacker.getName() + SELECT_CASUALTIES_SUICIDE);
			}
			
			if (!m_battleSite.isWater() && isParatroopers(m_attacker))
			{
				Collection<Unit> bombers = Match.getMatches(m_battleSite.getUnits().getUnits(), Matches.UnitIsStrategicBomber);
				
				if (!bombers.isEmpty())
				{
					Collection<Unit> dependents = getDependentUnits(bombers);
					if (!dependents.isEmpty())
					{
						steps.add(LAND_PARATROOPS);
					}
				}
			}
			
			if (getScramble_Rules_In_Effect())
			{
				steps.add(SCRAMBLE_UNITS_FOR_DEFENSE);
			}
		}
		
		// Check if defending subs can submerge before battle
		if (isSubRetreatBeforeBattle())
		{
			if (!Match.someMatch(m_attackingUnits, Matches.UnitIsDestroyer) &&
						Match.someMatch(m_defendingUnits, Matches.UnitIsSub))
				steps.add(m_defender.getName() + SUBS_SUBMERGE);
		}
		
		// Check if attack subs can submerge before battle
		if (isSubRetreatBeforeBattle())
		{
			if (!Match.someMatch(m_defendingUnits, Matches.UnitIsDestroyer) &&
						Match.someMatch(m_attackingUnits, Matches.UnitIsSub))
				steps.add(m_attacker.getName() + SUBS_SUBMERGE);
		}
		
		// See if there any unescorted trns
		if (m_battleSite.isWater() && isTransportCasualtiesRestricted())
		{
			if (Match.someMatch(m_attackingUnits, Matches.UnitIsTransport) || Match.someMatch(m_defendingUnits, Matches.UnitIsTransport))
				steps.add(REMOVE_UNESCORTED_TRANSPORTS);
		}
		
		boolean defenderSubsFireFirst =
					defenderSubsFireFirst();
		
		if (defenderSubsFireFirst && Match.someMatch(m_defendingUnits, Matches.UnitIsSub))
		{
			steps.add(m_defender.getName() + SUBS_FIRE);
			steps.add(m_attacker.getName() + SELECT_SUB_CASUALTIES);
			steps.add(REMOVE_SNEAK_ATTACK_CASUALTIES);
		}
		
		// attacker subs sneak attack
		// Attacking subs have no sneak attack if Destroyers are present
		if (m_battleSite.isWater())
		{
			if (Match.someMatch(m_attackingUnits, Matches.UnitIsSub))
			{
				steps.add(m_attacker.getName() + SUBS_FIRE);
				steps.add(m_defender.getName() + SELECT_SUB_CASUALTIES);
			}
		}
		
		boolean onlyAttackerSneakAttack =
					!defenderSubsFireFirst &&
								returnFireAgainstAttackingSubs() == ReturnFire.NONE &&
								returnFireAgainstDefendingSubs() == ReturnFire.ALL;
		
		if (onlyAttackerSneakAttack)
		{
			steps.add(REMOVE_SNEAK_ATTACK_CASUALTIES);
		}
		
		// defender subs sneak attack
		// Defending subs have no sneak attack in Pacific/Europe Theaters or if Destroyers are present
		if (m_battleSite.isWater())
		{
			if (!defenderSubsFireFirst && Match.someMatch(m_defendingUnits, Matches.UnitIsSub))
			{
				steps.add(m_defender.getName() + SUBS_FIRE);
				steps.add(m_attacker.getName() + SELECT_SUB_CASUALTIES);
			}
		}
		
		if (m_battleSite.isWater() &&
					!defenderSubsFireFirst && !onlyAttackerSneakAttack &&
					(returnFireAgainstDefendingSubs() != ReturnFire.ALL || returnFireAgainstAttackingSubs() != ReturnFire.ALL))
		{
			steps.add(REMOVE_SNEAK_ATTACK_CASUALTIES);
		}
		
		// Air only Units can't attack subs without Destroyers present
		if (isAirAttackSubRestricted())
		{
			Collection<Unit> units = new ArrayList<Unit>(m_attackingUnits.size() + m_attackingWaitingToDie.size());
			units.addAll(m_attackingUnits);
			
			// if(!Match.someMatch(m_attackingUnits, Matches.UnitIsDestroyer) && Match.allMatch(m_attackingUnits, Matches.UnitIsAir))
			if (Match.someMatch(m_attackingUnits, Matches.UnitIsAir) && !canAirAttackSubs(m_defendingUnits, units))
				steps.add(SUBMERGE_SUBS_VS_AIR_ONLY);
		}
		
		// Air Units can't attack subs without Destroyers present
		if (m_battleSite.isWater() && isAirAttackSubRestricted())
		{
			Collection<Unit> units = new ArrayList<Unit>(m_attackingUnits.size() + m_attackingWaitingToDie.size());
			units.addAll(m_attackingUnits);
			
			// if(!Match.someMatch(m_attackingUnits, Matches.UnitIsDestroyer) && Match.someMatch(m_attackingUnits, Matches.UnitIsAir) && Match.someMatch(m_defendingUnits, Matches.UnitIsSub))
			if (Match.someMatch(m_attackingUnits, Matches.UnitIsAir) && !canAirAttackSubs(m_defendingUnits, units))
				steps.add(AIR_ATTACK_NON_SUBS);
		}
		
		if (Match.someMatch(m_attackingUnits, Matches.UnitIsNotSub))
		{
			steps.add(m_attacker.getName() + FIRE);
			steps.add(m_defender.getName() + SELECT_CASUALTIES);
			
		}
		
		// Air Units can't attack subs without Destroyers present
		if (m_battleSite.isWater() && isAirAttackSubRestricted())
		{
			Collection<Unit> units = new ArrayList<Unit>(m_defendingUnits.size() + m_defendingWaitingToDie.size());
			units.addAll(m_defendingUnits);
			units.addAll(m_defendingWaitingToDie);
			
			// if(!Match.someMatch(m_defendingUnits, Matches.UnitIsDestroyer) && Match.someMatch(m_defendingUnits, Matches.UnitIsAir) && Match.someMatch(m_attackingUnits, Matches.UnitIsSub))
			if (Match.someMatch(m_defendingUnits, Matches.UnitIsAir) && !canAirAttackSubs(m_attackingUnits, units))
				steps.add(AIR_DEFEND_NON_SUBS);
		}
		
		if (Match.someMatch(m_defendingUnits, Matches.UnitIsNotSub))
		{
			
			steps.add(m_defender.getName() + FIRE);
			steps.add(m_attacker.getName() + SELECT_CASUALTIES);
			
		}
		
		// remove casualties
		steps.add(REMOVE_CASUALTIES);
		
		// retreat subs
		if (m_battleSite.isWater())
		{
			if (canSubsSubmerge())
			{
				if (!isSubRetreatBeforeBattle())
				{
					if (canAttackerRetreatSubs())
					{
						if (Match.someMatch(m_attackingUnits, Matches.UnitIsSub))
						{
							steps.add(m_attacker.getName() + SUBS_SUBMERGE);
						}
					}
					if (canDefenderRetreatSubs())
					{
						if (Match.someMatch(m_defendingUnits, Matches.UnitIsSub))
						{
							steps.add(m_defender.getName() + SUBS_SUBMERGE);
						}
					}
				}
				
			}
			else
			{
				if (canAttackerRetreatSubs())
				{
					if (Match.someMatch(m_attackingUnits, Matches.UnitIsSub))
					{
						steps.add(m_attacker.getName() + SUBS_WITHDRAW);
					}
				}
				if (canDefenderRetreatSubs())
				{
					if (Match.someMatch(m_defendingUnits, Matches.UnitIsSub))
					{
						steps.add(m_defender.getName() + SUBS_WITHDRAW);
					}
				}
			}
		}
		
		// if we are a sea zone, then we may not be able to retreat
		// (ie a sub travelled under another unit to get to the battle site)
		// or an enemy sub retreated to our sea zone
		// however, if all our sea units die, then
		// the air units can still retreat, so if we have any air units attacking in
		// a sea zone, we always have to have the retreat
		// option shown
		// later, if our sea units die, we may ask the user to retreat
		boolean someAirAtSea = m_battleSite.isWater() && Match.someMatch(m_attackingUnits, Matches.UnitIsAir);
		
		if (canAttackerRetreat() || someAirAtSea)
		{
			steps.add(m_attacker.getName() + ATTACKER_WITHDRAW);
		}
		else if (canAttackerRetreatPartialAmphib())
		{
			steps.add(m_attacker.getName() + NONAMPHIB_WITHDRAW);
		}
		else if (canAttackerRetreatPlanes())
		{
			steps.add(m_attacker.getName() + PLANES_WITHDRAW);
		}
		
		return steps;
	}
	
	private boolean defenderSubsFireFirst()
	{
		return returnFireAgainstAttackingSubs() == ReturnFire.ALL &&
					returnFireAgainstDefendingSubs() == ReturnFire.NONE;
	}
	
	private void pushFightStartOnStack()
	{
		IExecutable fireAAGuns = new IExecutable()
		{
			
			@Override
			public void execute(ExecutionStack stack, IDelegateBridge bridge)
			{
				fireAAGuns(bridge);
			}
			
		};
		
		IExecutable fireNavalBombardment = new IExecutable()
		{
			
			@Override
			public void execute(ExecutionStack stack, IDelegateBridge bridge)
			{
				fireNavalBombardment(bridge);
			}
			
		};
		
		IExecutable fireSuicideUnitsAttack = new IExecutable()
		{
			
			@Override
			public void execute(ExecutionStack stack, IDelegateBridge bridge)
			{
				fireSuicideUnitsAttack(bridge);
			}
			
		};
		
		IExecutable fireSuicideUnitsDefend = new IExecutable()
		{
			
			@Override
			public void execute(ExecutionStack stack, IDelegateBridge bridge)
			{
				fireSuicideUnitsDefend(bridge);
			}
			
		};
		
		IExecutable removeNonCombatants = new IExecutable()
		{
			
			@Override
			public void execute(ExecutionStack stack, IDelegateBridge bridge)
			{
				removeNonCombatants();
			}
			
		};
		
		IExecutable landParatroops = new IExecutable()
		{
			
			@Override
			public void execute(ExecutionStack stack, IDelegateBridge bridge)
			{
				landParatroops(bridge);
			}
		};
		
		IExecutable scrambleUnits = new IExecutable()
		{
			@Override
			public void execute(ExecutionStack stack, IDelegateBridge bridge)
			{
				if (getScramble_Rules_In_Effect())
					determineScrambledUnits(bridge);
			}
		};
		
		IExecutable notifyScrambleUnits = new IExecutable()
		{
			@Override
			public void execute(ExecutionStack stack, IDelegateBridge bridge)
			{
				if (getScramble_Rules_In_Effect())
					MustFightBattle.getDisplay(bridge).scrambleNotification(m_battleID, SCRAMBLE_UNITS_FOR_DEFENSE, findDefender(m_battleSite), new ArrayList<Unit>(m_scrambled), m_dependentUnits);
			}
		};
		
		// push in opposite order of execution
		m_stack.push(notifyScrambleUnits);
		m_stack.push(scrambleUnits);
		m_stack.push(landParatroops);
		m_stack.push(removeNonCombatants);
		m_stack.push(fireSuicideUnitsDefend);
		m_stack.push(fireSuicideUnitsAttack);
		m_stack.push(fireNavalBombardment);
		m_stack.push(fireAAGuns);
	}
	
	private void pushFightLoopOnStack(IDelegateBridge bridge)
	{
		if (m_over)
			return;
		
		List<IExecutable> steps = getBattleExecutables();
		
		// add in the reverse order we create them
		Collections.reverse(steps);
		for (IExecutable step : steps)
		{
			m_stack.push(step);
		}
		
		return;
	}
	
	List<IExecutable> getBattleExecutables()
	{
		// the code here is a bit odd to read
		// basically, we need to break the code into seperate atomic pieces.
		// If there is a network error, or some other unfortunate event,
		// then we need to keep track of what pieces we have executed, and what is left
		// to do
		// each atomic step is in its own IExecutable
		// the definition of atomic is that either
		// 1) the code does not call to an IDisplay,IPlayer, or IRandomSource
		// 2) if the code calls to an IDisplay, IPlayer, IRandomSource, and an exception is
		// called from one of those methods, the exception will be propogated out of execute(),
		// and the execute method can be called again
		// it is allowed for an iexecutable to add other iexecutables to the stack
		//
		// if you read the code in linear order, ignore wrapping stuff in annonymous iexecutables, then the code
		// can be read as it will execute
		
		// store the steps in a list
		// we need to push them in reverse order that we
		// create them, and its easier to track if we just add them
		// to a list while creating. then reverse the list and add
		// to the stack at the end
		List<IExecutable> steps = new ArrayList<IExecutable>();
		
		addFightStepsNonEditMode(steps);
		
		// we must grab these here, when we clear waiting to die, we might remove
		// all the opposing destroyers, and this would change the canRetreatSubs rVal
		final boolean canAttackerRetreatSubs = canAttackerRetreatSubs();
		final boolean canDefenderRetreatSubs = canDefenderRetreatSubs();
		
		steps.add(new IExecutable()
		{
			// compatible with 0.9.0.2 saved games
			private static final long serialVersionUID = 8611067962952500496L;
			
			@Override
			public void execute(ExecutionStack stack, IDelegateBridge bridge)
			{
				clearWaitingToDie(bridge);
			}
		});
		
		steps.add(new IExecutable()
		{
			// not compatible with 0.9.0.2 saved games. this is new for 1.2.6.0
			private static final long serialVersionUID = 6387198382888361848L;
			
			@Override
			public void execute(ExecutionStack stack, IDelegateBridge bridge)
			{
				checkSuicideUnits(bridge);
			}
		});
		
		steps.add(new IExecutable()
		{
			// compatible with 0.9.0.2 saved games
			private static final long serialVersionUID = 5259103822937067667L;
			
			@Override
			public void execute(ExecutionStack stack, IDelegateBridge bridge)
			{
				
				// changed to only look at units that can be destroyed in combat, and therefore not include factories, aaguns, and infrastructure.
				if (Match.getMatches(m_attackingUnits, Matches.UnitIsDestructibleInCombatShort).size() == 0)
				{
					if (isTransportCasualtiesRestricted())
					{
						// Get all allied transports in the territory
						CompositeMatch<Unit> matchAllied = new CompositeMatchAnd<Unit>();
						matchAllied.add(Matches.UnitIsTransport);
						matchAllied.add(Matches.UnitIsNotCombatTransport);
						matchAllied.add(Matches.isUnitAllied(m_attacker, m_data));
						
						List<Unit> alliedTransports = Match.getMatches(m_battleSite.getUnits().getUnits(), matchAllied);
						
						// If no transports, just end the battle
						if (alliedTransports.isEmpty())
						{
							endBattle(bridge);
							defenderWins(bridge);
						}
						else if (m_round == 0)
						{
							// TODO Need to determine how combined forces on attack work- trn left in terr by prev player, ally moves in and attacks
							// add back in the non-combat units (Trns)
							m_attackingUnits = Match.getMatches(m_battleSite.getUnits().getUnits(), Matches.unitIsOwnedBy(m_attacker));
						}
						else
						{
							endBattle(bridge);
							defenderWins(bridge);
						}
					}
					else
					{
						endBattle(bridge);
						defenderWins(bridge);
					}
				}
				// changed to only look at units that can be destroyed in combat, and therefore not include factories, aaguns, and infrastructure.
				else if (Match.getMatches(m_defendingUnits, Matches.UnitIsDestructibleInCombatShort).size() == 0)
				{
					if (isTransportCasualtiesRestricted())
					{
						// If there are undefended attacking transports, determine if they automatically die
						checkUndefendedTransports(bridge, m_defender);
					}
					
					endBattle(bridge);
					attackerWins(bridge);
				}
				else if ((Match.allMatch(m_attackingUnits, Matches.unitHasAttackValueOfAtLeast(1).invert())) && Match.allMatch(m_defendingUnits, Matches.unitHasDefendValueOfAtLeast(1).invert()))
				{
					endBattle(bridge);
					nobodyWins(bridge);
				}
				
			}
			
		});
		
		steps.add(new IExecutable()
		{
			// compatible with 0.9.0.2 saved games
			private static final long serialVersionUID = 6775880082912594489L;
			
			@Override
			public void execute(ExecutionStack stack, IDelegateBridge bridge)
			{
				if (!m_over && canAttackerRetreatSubs && !isSubRetreatBeforeBattle())
					attackerRetreatSubs(bridge);
			}
		});
		
		steps.add(new IExecutable()
		{
			// compatible with 0.9.0.2 saved games
			private static final long serialVersionUID = -1544916305666912480L;
			
			@Override
			public void execute(ExecutionStack stack, IDelegateBridge bridge)
			{
				if (!m_over && canDefenderRetreatSubs && !isSubRetreatBeforeBattle())
					defenderRetreatSubs(bridge);
			}
		});
		
		steps.add(new IExecutable()
		{
			// compatible with 0.9.0.2 saved games
			private static final long serialVersionUID = -1150863964807721395L;
			
			@Override
			public void execute(ExecutionStack stack, IDelegateBridge bridge)
			{
				if (canAttackerRetreatPlanes() && !canAttackerRetreatPartialAmphib() && !m_over)
					attackerRetreatPlanes(bridge);
			}
		});
		
		steps.add(new IExecutable()
		{
			// compatible with 0.9.0.2 saved games
			private static final long serialVersionUID = -1150863964807721395L;
			
			@Override
			public void execute(ExecutionStack stack, IDelegateBridge bridge)
			{
				if (canAttackerRetreatPartialAmphib() && !m_over)
					attackerRetreatNonAmphibUnits(bridge);
			}
		});
		
		steps.add(new IExecutable()
		{
			// compatible with 0.9.0.2 saved games
			private static final long serialVersionUID = 669349383898975048L;
			
			@Override
			public void execute(ExecutionStack stack, IDelegateBridge bridge)
			{
				attackerRetreat(bridge);
			}
		});
		
		final IExecutable loop = new IExecutable()
		{
			// compatible with 0.9.0.2 saved games
			private static final long serialVersionUID = 3118458517320468680L;
			
			@Override
			public void execute(ExecutionStack stack, IDelegateBridge bridge)
			{
				pushFightLoopOnStack(bridge);
			}
		};
		
		steps.add(new IExecutable()
		{
			// compatible with 0.9.0.2 saved games
			private static final long serialVersionUID = -3993599528368570254L;
			
			@Override
			public void execute(ExecutionStack stack, IDelegateBridge bridge)
			{
				if (!m_over)
				{
					m_stepStrings = determineStepStrings(false, bridge);
					ITripleaDisplay display = getDisplay(bridge);
					display.listBattleSteps(m_battleID, m_stepStrings);
					m_round++;
					
					// continue fighting
					// the recursive step
					// this should always be the base of the stack
					// when we execute the loop, it will populate the stack with the battle steps
					if (!m_stack.isEmpty())
						throw new IllegalStateException("Stack not empty:" + m_stack);
					m_stack.push(loop);
				}
				
			}
		});
		return steps;
	}
	
	private void addFightStepsNonEditMode(List<IExecutable> steps)
	{
		/** Ask to retreat defending subs before battle */
		if (isSubRetreatBeforeBattle())
		{
			steps.add(new IExecutable()
			{
				// compatible with 0.9.0.2 saved games
				private static final long serialVersionUID = 7056448091800764539L;
				
				@Override
				public void execute(ExecutionStack stack, IDelegateBridge bridge)
				{
					if (!m_over)
						defenderRetreatSubs(bridge);
				}
			});
			steps.add(new IExecutable()
			{
				// compatible with 0.9.0.2 saved games
				private static final long serialVersionUID = 6775880082912594489L;
				
				@Override
				public void execute(ExecutionStack stack, IDelegateBridge bridge)
				{
					if (!m_over)
						attackerRetreatSubs(bridge);
				}
			});
		}
		
		/** Remove Suicide Units */
		steps.add(new IExecutable()
		{
			private static final long serialVersionUID = 99988L;
			
			@Override
			public void execute(ExecutionStack stack, IDelegateBridge bridge)
				{
					checkSuicideUnits(bridge);
				}
		});
		
		/** Remove undefended trns */
		if (isTransportCasualtiesRestricted())
			steps.add(new IExecutable()
			{
				private static final long serialVersionUID = 99989L;
				
				@Override
				public void execute(ExecutionStack stack, IDelegateBridge bridge)
					{
						checkUndefendedTransports(bridge, m_defender);
						checkUndefendedTransports(bridge, m_attacker);
					}
			});
		
		/** Submerge subs if -vs air only & air restricted from attacking subs */
		if (isAirAttackSubRestricted())
			steps.add(new IExecutable()
			{
				private static final long serialVersionUID = 99990L;
				
				@Override
				public void execute(ExecutionStack stack, IDelegateBridge bridge)
					{
						submergeSubsVsOnlyAir(bridge);
					}
			});
		
		final ReturnFire returnFireAgainstAttackingSubs = returnFireAgainstAttackingSubs();
		final ReturnFire returnFireAgainstDefendingSubs = returnFireAgainstDefendingSubs();
		
		if (defenderSubsFireFirst())
		{
			steps.add(new DefendSubs()
			{
				// compatible with 0.9.0.2 saved games
				private static final long serialVersionUID = 99992L;
				
				@Override
				public void execute(ExecutionStack stack, IDelegateBridge bridge)
				{
					defendSubs(bridge, returnFireAgainstDefendingSubs);
				}
			});
		}
		
		steps.add(new AttackSubs()
		{
			private static final long serialVersionUID = 99991L;
			
			@Override
			public void execute(ExecutionStack stack, IDelegateBridge bridge)
			{
				attackSubs(bridge, returnFireAgainstAttackingSubs);
			}
		});
		
		if (!defenderSubsFireFirst())
		{
			steps.add(new DefendSubs()
			{
				// compatible with 0.9.0.2 saved games
				private static final long serialVersionUID = 99992L;
				
				@Override
				public void execute(ExecutionStack stack, IDelegateBridge bridge)
				{
					defendSubs(bridge, returnFireAgainstDefendingSubs);
				}
			});
		}
		
		/** Attacker air fire on NON subs */
		if (isAirAttackSubRestricted())
			steps.add(new IExecutable()
			{
				// compatible with 0.9.0.2 saved games
				private static final long serialVersionUID = 99993L;
				
				@Override
				public void execute(ExecutionStack stack, IDelegateBridge bridge)
				{
					attackAirOnNonSubs(bridge);
				}
				
			});
		
		/** Attacker fire remaining units */
		
		steps.add(new IExecutable()
		{
			// compatible with 0.9.0.2 saved games
			private static final long serialVersionUID = 99994L;
			
			@Override
			public void execute(ExecutionStack stack, IDelegateBridge bridge)
			{
				attackNonSubs(bridge);
			}
			
		});
		
		/** Defender air fire on NON subs */
		if (isAirAttackSubRestricted())
			steps.add(new IExecutable()
			{
				// compatible with 0.9.0.2 saved games
				private static final long serialVersionUID = 1560702114917865123L;
				
				@Override
				public void execute(ExecutionStack stack, IDelegateBridge bridge)
				{
					defendAirOnNonSubs(bridge);
				}
				
			});
		
		steps.add(new IExecutable()
		{
			// compatible with 0.9.0.2 saved games
			private static final long serialVersionUID = 1560702114917865290L;
			
			@Override
			public void execute(ExecutionStack stack, IDelegateBridge bridge)
			{
				defendNonSubs(bridge);
			}
		});
	}
	
	private ReturnFire returnFireAgainstAttackingSubs()
	{
		final boolean attackingSubsSneakAttack = !Match.someMatch(m_defendingUnits, Matches.UnitIsDestroyer);
		final boolean defendingSubsSneakAttack = defendingSubsSneakAttack2();
		
		final ReturnFire returnFireAgainstAttackingSubs;
		if (!attackingSubsSneakAttack)
		{
			returnFireAgainstAttackingSubs = ReturnFire.ALL;
		}
		else if (defendingSubsSneakAttack || isWW2V2())
		{
			returnFireAgainstAttackingSubs = ReturnFire.SUBS;
		}
		else
		{
			returnFireAgainstAttackingSubs = ReturnFire.NONE;
		}
		return returnFireAgainstAttackingSubs;
		
	}
	
	private ReturnFire returnFireAgainstDefendingSubs()
	{
		/** Attacker subs fire */
		/*calculate here, this holds for the fight round, but can't be computed later
		since destroyers may die*/
		final boolean attackingSubsSneakAttack = !Match.someMatch(m_defendingUnits, Matches.UnitIsDestroyer);
		final boolean defendingSubsSneakAttack = defendingSubsSneakAttack2();
		
		final ReturnFire returnFireAgainstDefendingSubs;
		if (!defendingSubsSneakAttack)
		{
			returnFireAgainstDefendingSubs = ReturnFire.ALL;
		}
		else if (attackingSubsSneakAttack || isWW2V2())
		{
			returnFireAgainstDefendingSubs = ReturnFire.SUBS;
		}
		else
		{
			returnFireAgainstDefendingSubs = ReturnFire.NONE;
		}
		return returnFireAgainstDefendingSubs;
	}
	
	private boolean defendingSubsSneakAttack2()
	{
		return (isWW2V2() ||
					isDefendingSubsSneakAttack()) && !Match.someMatch(m_attackingUnits, Matches.UnitIsDestroyer);
	}
	
	/**
	 * @param bridge
	 * @return
	 */
	static ITripleaDisplay getDisplay(IDelegateBridge bridge)
	{
		return (ITripleaDisplay) bridge.getDisplayChannelBroadcaster();
	}
	
	/**
	 * @return
	 */
	private boolean canAttackerRetreatPlanes()
	{
		return (isWW2V2() || isAttackerRetreatPlanes() || isPartialAmphibiousRetreat()) && m_amphibious
					&& Match.someMatch(m_attackingUnits, Matches.UnitIsAir);
	}
	
	/**
	 * @return
	 */
	private boolean canAttackerRetreatPartialAmphib()
	{
		
		if (m_amphibious && isPartialAmphibiousRetreat())
		{
			List<Unit> landUnits = Match.getMatches(m_attackingUnits, Matches.UnitIsLand); // Only include land units when checking for allow amphibious retreat
			for (Unit unit : landUnits)
			{
				TripleAUnit taUnit = (TripleAUnit) unit;
				if (!taUnit.getWasAmphibious())
					return true;
			}
		}
		return false;
	}
	
	Collection<Territory> getAttackerRetreatTerritories()
	{
		// If attacker is all planes, just return collection of current
		// territory
		if (Match.allMatch(m_attackingUnits, Matches.UnitIsAir))
		{
			Collection<Territory> oneTerritory = new ArrayList<Territory>(2);
			oneTerritory.add(m_battleSite);
			return oneTerritory;
		}
		
		// its possible that a sub retreated to a territory we came from,
		// if so we can no longer retreat there
		Collection<Territory> possible = Match.getMatches(m_attackingFrom, Matches
					.territoryHasNoEnemyUnits(m_attacker, m_data));
		
		// In WW2V2 we need to filter out territories where only planes
		// came from since planes cannot define retreat paths
		if (isWW2V2() || isWW2V3())
		{
			possible = Match.getMatches(possible, new Match<Territory>()
			{
				@Override
				public boolean match(Territory t)
				{
					Collection<Unit> units = m_attackingFromMap.get(t);
					return !Match.allMatch(units, Matches.UnitIsAir);
				}
			});
		}
		/*else 
		{*/
		// the air unit may have come from a conquered or enemy territory, don't allow retreating
		Match<Territory> conqueuredOrEnemy = new CompositeMatchOr<Territory>(
					Matches.isTerritoryEnemyAndNotUnownedWaterOrImpassibleOrRestricted(m_attacker, m_data),
					new CompositeMatchAnd<Territory>(
								// Matches.TerritoryIsLand,
								Matches.TerritoryIsWater,
								Matches.territoryWasFoughOver(m_tracker)
					)
					);
		possible.removeAll(Match.getMatches(possible, conqueuredOrEnemy));
		// }
		
		// the battle site is in the attacking from
		// if sea units are fighting a submerged sub
		possible.remove(m_battleSite);
		
		if (Match.someMatch(m_attackingUnits, Matches.UnitIsLand)
					&& !m_battleSite.isWater())
			possible = Match.getMatches(possible, Matches.TerritoryIsLand);
		
		if (Match.someMatch(m_attackingUnits, Matches.UnitIsSea))
			possible = Match.getMatches(possible, Matches.TerritoryIsWater);
		
		return possible;
	}
	
	private boolean canAttackerRetreat()
	{
		if (onlyDefenselessAttackingTransportsLeft() || onlyDefenselessDefendingTransportsLeft())
		{
			return false;
		}
		
		// if (m_amphibious && !isPartialAmphibiousRetreat())
		if (m_amphibious)
			return false;
		
		Collection<Territory> options = getAttackerRetreatTerritories();
		
		if (options.size() == 0)
			return false;
		
		return true;
	}
	
	private boolean onlyDefenselessDefendingTransportsLeft()
	{
		if (!isTransportCasualtiesRestricted())
		{
			return false;
		}
		return Match.allMatch(m_defendingUnits, Matches.UnitIsTransportButNotCombatTransport);
	}
	
	private boolean onlyDefenselessAttackingTransportsLeft()
	{
		if (!isTransportCasualtiesRestricted())
		{
			return false;
		}
		return Match.allMatch(m_attackingUnits, Matches.UnitIsTransportButNotCombatTransport);
	}
	
	private boolean canAttackerRetreatSubs()
	{
		if (Match.someMatch(m_defendingUnits, Matches.UnitIsDestroyer))
			return false;
		
		if (Match.someMatch(m_defendingWaitingToDie, Matches.UnitIsDestroyer))
			return false;
		
		return canAttackerRetreat() || canSubsSubmerge();
	}
	
	// Added for test case calls
	void externalRetreat(Collection<Unit> retreaters, Territory retreatTo, Boolean defender, IDelegateBridge bridge)
	{
		m_over = true;
		retreatUnits(retreaters, retreatTo, defender, bridge);
	}
	
	private void attackerRetreat(IDelegateBridge bridge)
	{
		if (!canAttackerRetreat())
			return;
		
		Collection<Territory> possible = getAttackerRetreatTerritories();
		
		if (!m_over)
		{
			if (m_amphibious)
				queryRetreat(false, PARTIAL_AMPHIB_RETREAT_TYPE, bridge, possible);
			else
				queryRetreat(false, DEFAULT_RETREAT_TYPE, bridge, possible);
		}
	}
	
	private void attackerRetreatPlanes(IDelegateBridge bridge)
	{
		// planes retreat to the same square the battle is in, and then should
		// move during non combat to their landing site, or be scrapped if they
		// can't find one.
		Collection<Territory> possible = new ArrayList<Territory>(2);
		possible.add(m_battleSite);
		
		// retreat planes
		if (Match.someMatch(m_attackingUnits, Matches.UnitIsAir))
			queryRetreat(false, PLANES_RETREAT_TYPE, bridge, possible);
	}
	
	private void attackerRetreatNonAmphibUnits(IDelegateBridge bridge)
	{
		Collection<Territory> possible = getAttackerRetreatTerritories();
		
		queryRetreat(false, PARTIAL_AMPHIB_RETREAT_TYPE, bridge, possible);
	}
	
	private boolean canDefenderRetreatSubs()
	{
		if (m_headless)
			return false;
		
		if (Match.someMatch(m_attackingUnits, Matches.UnitIsDestroyer))
			return false;
		
		if (Match.someMatch(m_attackingWaitingToDie, Matches.UnitIsDestroyer))
			return false;
		
		return getEmptyOrFriendlySeaNeighbors(m_defender).size() != 0
					|| canSubsSubmerge();
	}
	
	private void attackerRetreatSubs(IDelegateBridge bridge)
	{
		if (!canAttackerRetreatSubs())
			return;
		
		if (Match.someMatch(m_attackingUnits, Matches.UnitIsSub))
			queryRetreat(false, SUBS_RETREAT_TYPE, bridge, getAttackerRetreatTerritories());
	}
	
	private void defenderRetreatSubs(IDelegateBridge bridge)
	{
		if (!canDefenderRetreatSubs())
			return;
		
		if (!m_over && Match.someMatch(m_defendingUnits, Matches.UnitIsSub))
			queryRetreat(true, SUBS_RETREAT_TYPE, bridge, getEmptyOrFriendlySeaNeighbors(m_defender));
	}
	
	private Collection<Territory> getEmptyOrFriendlySeaNeighbors(PlayerID player)
	{
		Collection<Territory> possible = m_data.getMap().getNeighbors(m_battleSite);
		CompositeMatch<Territory> match = new CompositeMatchAnd<Territory>(Matches.TerritoryIsWater,
					Matches.territoryHasNoEnemyUnits(player, m_data));
		
		// make sure we can move through the any canals
		Match<Territory> canalMatch = new Match<Territory>()
		{
			@Override
			public boolean match(Territory t)
			{
				
				Route r = new Route();
				r.setStart(m_battleSite);
				r.add(t);
				if (MoveValidator.validateCanal(r, m_defender, m_data) != null)
					return false;
				return true;
			}
		};
		match.add(canalMatch);
		
		possible = Match.getMatches(possible, match);
		return possible;
	}
	
	private void queryScrambleUnits(String actionType, IDelegateBridge bridge, Collection<Territory> availableTerritories)
	{
		// TODO kev break out each Scrambled Unit's owner for the query
		PlayerID scramblingPlayer = findDefender(m_battleSite);
		String text = scramblingPlayer.getName() + SCRAMBLE_UNITS;
		
		String step = SCRAMBLE_UNITS_FOR_DEFENSE;
		
		getDisplay(bridge).gotoBattleStep(m_battleID, step);
		
		// Ask players if they want to scramble and get the list of scrambled units with territories
		getRemote(scramblingPlayer, bridge).scrambleQuery(m_battleID, availableTerritories, text);
		
		Map<Unit, Territory> scrambledUnits = BattleDisplay.getScrambledUnits();
		// Move the scrambled units to the battle
		if (!scrambledUnits.isEmpty())
		{
			BattleTracker tracker = getBattleTracker();
			Battle battle = tracker.getPendingBattle(m_battleSite, false);
			
			CompositeChange change = new CompositeChange();
			m_scrambled.addAll(scrambledUnits.keySet());
			for (Unit u : scrambledUnits.keySet())
			{
				Territory t = scrambledUnits.get(u);
				Route route = m_data.getMap().getRoute(t, m_battleSite);
				
				change.add(ChangeFactory.unitPropertyChange(u, t, TripleAUnit.ORIGINATED_FROM));
				change.add(ChangeFactory.unitPropertyChange(u, route.getLength(), TripleAUnit.ALREADY_MOVED));
				change.add(ChangeFactory.unitPropertyChange(u, true, TripleAUnit.WAS_SCRAMBLED));
				change.add(ChangeFactory.moveUnits(t, m_battleSite, Collections.singleton(u)));
				change.add(battle.addCombatChange(route, Collections.singleton(u), scramblingPlayer));
			}
			bridge.addChange(change);
			
			String messageShort = scramblingPlayer.getName() + " scramble units";
			getDisplay(bridge).notifyScramble(messageShort, messageShort, step, scramblingPlayer);
		}
	}
	
	private void queryRetreat(boolean defender, int retreatType,
				IDelegateBridge bridge, Collection<Territory> availableTerritories)
	{
		boolean subs;
		boolean planes;
		boolean partialAmphib;
		planes = retreatType == PLANES_RETREAT_TYPE;
		subs = retreatType == SUBS_RETREAT_TYPE;
		partialAmphib = retreatType == PARTIAL_AMPHIB_RETREAT_TYPE;
		
		if (availableTerritories.isEmpty() && !(subs && canSubsSubmerge()))
			return;
		
		Collection<Unit> units = defender ? m_defendingUnits : m_attackingUnits;
		
		if (subs)
		{
			units = Match.getMatches(units, Matches.UnitIsSub);
		}
		else if (planes)
		{
			units = Match.getMatches(units, Matches.UnitIsAir);
		}
		else if (partialAmphib)
		{
			units = Match.getMatches(units, Matches.UnitWasNotAmphibious);
		}
		
		if (Match.someMatch(units, Matches.UnitIsSea))
		{
			availableTerritories = Match.getMatches(availableTerritories,
						Matches.TerritoryIsWater);
		}
		
		if (units.size() == 0)
			return;
		
		PlayerID retreatingPlayer = defender ? m_defender : m_attacker;
		String text;
		if (subs)
			text = retreatingPlayer.getName() + " retreat subs?";
		else if (planes)
			text = retreatingPlayer.getName() + RETREAT_PLANES;
		else if (partialAmphib)
			text = retreatingPlayer.getName() + " retreat non-amphibious units?";
		else
			text = retreatingPlayer.getName() + " retreat?";
		String step;
		if (defender)
		{
			step = m_defender.getName()
						+ (canSubsSubmerge() ? SUBS_SUBMERGE : SUBS_WITHDRAW);
		}
		else
		{
			if (subs)
				step = m_attacker.getName()
							+ (canSubsSubmerge() ? SUBS_SUBMERGE : SUBS_WITHDRAW);
			else if (planes)
				step = m_attacker.getName() + PLANES_WITHDRAW;
			else if (partialAmphib)
				step = m_attacker.getName() + NONAMPHIB_WITHDRAW;
			else
				step = m_attacker.getName() + ATTACKER_WITHDRAW;
		}
		
		boolean submerge = subs && canSubsSubmerge();
		getDisplay(bridge).gotoBattleStep(m_battleID, step);
		Territory retreatTo = getRemote(retreatingPlayer, bridge).retreatQuery(m_battleID, submerge, availableTerritories, text);
		
		if (retreatTo != null && !availableTerritories.contains(retreatTo) && !subs)
		{
			System.err.println("Invalid retreat selection :" + retreatTo + " not in " + MyFormatter.territoriesToText(availableTerritories));
			Thread.dumpStack();
			return;
		}
		
		if (retreatTo != null)
		{
			// if attacker retreating non subs then its all over
			if (!defender && !subs && !planes && !partialAmphib)
			{
				ensureAttackingAirCanRetreat(bridge);
				m_over = true;
			}
			
			if (submerge)
			{
				submergeUnits(units, defender, bridge);
				String messageShort = retreatingPlayer.getName()
							+ " submerges subs";
				getDisplay(bridge).notifyRetreat(messageShort, messageShort, step, retreatingPlayer);
			}
			else if (planes)
			{
				retreatPlanes(units, defender, bridge);
				String messageShort = retreatingPlayer.getName()
							+ " retreats planes";
				getDisplay(bridge).notifyRetreat(messageShort, messageShort, step, retreatingPlayer);
			}
			else if (partialAmphib)
			{
				// remove amphib units from those retreating
				units = Match.getMatches(units, Matches.UnitWasNotAmphibious);
				retreatUnitsAndPlanes(units, retreatTo, defender, bridge);
				String messageShort = retreatingPlayer.getName()
							+ " retreats non-amphibious units";
				getDisplay(bridge).notifyRetreat(messageShort, messageShort, step, retreatingPlayer);
			}
			else
			{
				retreatUnits(units, retreatTo, defender, bridge);
				
				String messageShort = retreatingPlayer.getName() + " retreats";
				String messageLong;
				if (subs)
					messageLong = retreatingPlayer.getName()
								+ " retreats subs to " + retreatTo.getName();
				else if (planes)
					messageLong = retreatingPlayer.getName()
								+ " retreats planes to " + retreatTo.getName();
				else if (partialAmphib)
					messageLong = retreatingPlayer.getName()
								+ " retreats non-amphibious units to " + retreatTo.getName();
				else
					messageLong = retreatingPlayer.getName()
								+ " retreats all units to " + retreatTo.getName();
				getDisplay(bridge).notifyRetreat(messageShort, messageLong, step, retreatingPlayer);
				
			}
		}
	}
	
	private BattleTracker getBattleTracker()
	{
		return DelegateFinder.battleDelegate(m_data).getBattleTracker();
	}
	
	private Change retreatFromDependents(Collection<Unit> units,
				IDelegateBridge bridge, Territory retreatTo, Collection<Battle> dependentBattles)
	{
		CompositeChange change = new CompositeChange();
		Iterator<Battle> iter = dependentBattles.iterator();
		while (iter.hasNext())
		{
			Battle dependent = iter.next();
			Route route = new Route();
			route.setStart(m_battleSite);
			route.add(dependent.getTerritory());
			
			Collection<Unit> retreatedUnits = dependent.getDependentUnits(units);
			
			dependent.removeAttack(route, retreatedUnits);
			
			reLoadTransports(units, change);
			
			change.add(ChangeFactory.moveUnits(dependent.getTerritory(),
						retreatTo, retreatedUnits));
		}
		return change;
	}
	
	// Retreat landed units from allied territory when their transport retreats
	private Change retreatFromNonCombat(Collection<Unit> units, IDelegateBridge bridge, Territory retreatTo)
	{
		CompositeChange change = new CompositeChange();
		
		units = Match.getMatches(units, Matches.UnitIsTransport);
		Collection<Unit> retreated = getTransportDependents(units, m_data);
		if (!retreated.isEmpty())
		{
			Territory retreatedFrom = null;
			
			Iterator<Unit> iter = units.iterator();
			while (iter.hasNext())
			{
				Unit unit = iter.next();
				retreatedFrom = getTransportTracker().getTerritoryTransportHasUnloadedTo(unit);
				
				if (retreatedFrom != null)
				{
					reLoadTransports(units, change);
					
					change.add(ChangeFactory.moveUnits(retreatedFrom, retreatTo, retreated));
				}
			}
		}
		return change;
	}
	
	private void reLoadTransports(Collection<Unit> units, CompositeChange change)
	{
		Collection<Unit> transports = Match.getMatches(units,
					Matches.UnitCanTransport);
		
		// Put units back on their transports
		Iterator<Unit> transportsIter = transports.iterator();
		
		while (transportsIter.hasNext())
		{
			Unit transport = transportsIter.next();
			Collection<Unit> unloaded = getTransportTracker().unloaded(transport);
			Iterator<Unit> unloadedIter = unloaded.iterator();
			while (unloadedIter.hasNext())
			{
				Unit load = (Unit) unloadedIter.next();
				Change loadChange = getTransportTracker().loadTransportChange((TripleAUnit) transport, load, m_attacker);
				change.add(loadChange);
			}
		}
	}
	
	private void retreatPlanes(Collection<Unit> retreating, boolean defender,
				IDelegateBridge bridge)
	{
		String transcriptText = MyFormatter.unitsToText(retreating)
					+ " retreated";
		
		Collection<Unit> units = defender ? m_defendingUnits : m_attackingUnits;
		/** @todo Does this need to happen with planes retreating too? */
		units.removeAll(retreating);
		if (units.isEmpty() || m_over)
		{
			endBattle(bridge);
			if (defender)
				attackerWins(bridge);
			else
				defenderWins(bridge);
		}
		else
		{
			getDisplay(bridge).notifyRetreat(m_battleID, retreating);
			
		}
		
		bridge.getHistoryWriter().addChildToEvent(transcriptText, retreating);
	}
	
	private void submergeUnits(Collection<Unit> submerging, boolean defender,
				IDelegateBridge bridge)
	{
		String transcriptText = MyFormatter.unitsToText(submerging)
					+ " Submerged";
		
		Collection<Unit> units = defender ? m_defendingUnits : m_attackingUnits;
		CompositeChange change = new CompositeChange();
		for (Unit u : submerging)
		{
			change.add(ChangeFactory.unitPropertyChange(u, true, TripleAUnit.SUBMERGED));
		}
		bridge.addChange(change);
		
		units.removeAll(submerging);
		
		if (!units.isEmpty() && !m_over)
		{
			getDisplay(bridge).notifyRetreat(m_battleID, submerging);
			
		}
		
		bridge.getHistoryWriter().addChildToEvent(transcriptText, submerging);
	}
	
	private void retreatUnits(Collection<Unit> retreating, Territory to,
				boolean defender, IDelegateBridge bridge)
	{
		retreating.addAll(getDependentUnits(retreating));
		
		// our own air units dont retreat with land units
		Match<Unit> notMyAir = new CompositeMatchOr<Unit>(Matches.UnitIsNotAir,
					new InverseMatch<Unit>(Matches.unitIsOwnedBy(m_attacker)));
		retreating = Match.getMatches(retreating, notMyAir);
		
		String transcriptText;
		// in WW2V1, defending subs can retreat so show owner
		if (isWW2V2())
			transcriptText = MyFormatter.unitsToTextNoOwner(retreating) + " retreated to " + to.getName();
		else
			transcriptText = MyFormatter.unitsToText(retreating) + " retreated to " + to.getName();
		bridge.getHistoryWriter().addChildToEvent(transcriptText,
					new ArrayList<Unit>(retreating));
		
		CompositeChange change = new CompositeChange();
		change.add(ChangeFactory.moveUnits(m_battleSite, to, retreating));
		
		if (m_over)
		{
			Collection<Battle> dependentBattles = m_tracker.getBlocked(this);
			// If there are no dependent battles, check landings in allied territories
			if (dependentBattles.isEmpty())
				change.add(retreatFromNonCombat(retreating, bridge, to));
			// Else retreat the units from combat when their transport retreats
			else
				change.add(retreatFromDependents(retreating, bridge, to, dependentBattles));
		}
		
		bridge.addChange(change);
		
		Collection<Unit> units = defender ? m_defendingUnits : m_attackingUnits;
		
		units.removeAll(retreating);
		if (units.isEmpty() || m_over)
		{
			endBattle(bridge);
			if (defender)
				attackerWins(bridge);
			else
				defenderWins(bridge);
		}
		else
		{
			getDisplay(bridge).notifyRetreat(m_battleID, retreating);
			
		}
	}
	
	private void retreatUnitsAndPlanes(Collection<Unit> retreating, Territory to,
				boolean defender, IDelegateBridge bridge)
	{
		// Remove air from battle
		Collection<Unit> units = defender ? m_defendingUnits : m_attackingUnits;
		units.removeAll(Match.getMatches(units, Matches.UnitIsAir));
		
		// add all land units' dependents
		// retreating.addAll(getDependentUnits(retreating));
		retreating.addAll(getDependentUnits(units));
		
		// our own air units dont retreat with land units
		Match<Unit> notMyAir = new CompositeMatchOr<Unit>(Matches.UnitIsNotAir,
					new InverseMatch<Unit>(Matches.unitIsOwnedBy(m_attacker)));
		Collection<Unit> nonAirRetreating = Match.getMatches(retreating, notMyAir);
		
		String transcriptText = MyFormatter.unitsToTextNoOwner(nonAirRetreating) + " retreated to " + to.getName();
		
		bridge.getHistoryWriter().addChildToEvent(transcriptText, new ArrayList<Unit>(nonAirRetreating));
		
		CompositeChange change = new CompositeChange();
		change.add(ChangeFactory.moveUnits(m_battleSite, to, nonAirRetreating));
		
		if (m_over)
		{
			Collection<Battle> dependentBattles = m_tracker.getBlocked(this);
			// If there are no dependent battles, check landings in allied territories
			if (dependentBattles.isEmpty())
				change.add(retreatFromNonCombat(nonAirRetreating, bridge, to));
			// Else retreat the units from combat when their transport retreats
			else
				change.add(retreatFromDependents(nonAirRetreating, bridge, to, dependentBattles));
		}
		
		bridge.addChange(change);
		
		units.removeAll(nonAirRetreating);
		if (units.isEmpty() || m_over)
		{
			endBattle(bridge);
			if (defender)
				attackerWins(bridge);
			else
				defenderWins(bridge);
		}
		else
		{
			getDisplay(bridge).notifyRetreat(m_battleID, retreating);
		}
	}
	
	// the maximum number of hits that this collection of units can sustain
	// takes into account units with two hits
	public static int getMaxHits(Collection<Unit> units)
	{
		
		int count = 0;
		Iterator<Unit> unitIter = units.iterator();
		while (unitIter.hasNext())
		{
			Unit unit = unitIter.next();
			if (UnitAttachment.get(unit.getUnitType()).isTwoHit())
			{
				count += 2;
				count -= unit.getHits();
			}
			else
			{
				count++;
			}
		}
		return count;
	}
	
	private void fire(final String stepName, Collection<Unit> firingUnits,
				Collection<Unit> attackableUnits, boolean defender,
				ReturnFire returnFire, final IDelegateBridge bridge, String text)
	{
		PlayerID firing = defender ? m_defender : m_attacker;
		PlayerID defending = !defender ? m_defender : m_attacker;
		
		if (firingUnits.isEmpty())
		{
			return;
		}
		
		m_stack.push(new Fire(attackableUnits, returnFire, firing, defending, firingUnits, stepName, text, this, defender,
					m_dependentUnits, m_stack, m_headless));
	}
	
	/**
	 * Check for suicide units and kill them immediately (they get to shoot back, which is the point)
	 * 
	 * @param bridge
	 * @param player
	 * @param defender
	 */
	private void checkSuicideUnits(IDelegateBridge bridge)
	{
		if (isDefendingSuicideAndMunitionUnitsDoNotFire())
		{
			remove(Match.getMatches(m_battleSite.getUnits().getUnits(), new CompositeMatchAnd<Unit>(Matches.UnitIsSuicide, Matches.unitIsOwnedBy(m_attacker))), bridge, m_battleSite, false);
			getDisplay(bridge).deadUnitNotification(m_battleID, m_attacker, Match.getMatches(m_attackingUnits, Matches.UnitIsSuicide), m_dependentUnits);
			m_attackingUnits.removeAll(Match.getMatches(m_attackingUnits, Matches.UnitIsSuicide));
		}
		else
		{
			remove(Match.getMatches(m_battleSite.getUnits().getUnits(), Matches.UnitIsSuicide), bridge, m_battleSite, false);
			// and remove them from the battle display
			getDisplay(bridge).deadUnitNotification(m_battleID, m_attacker, Match.getMatches(m_attackingUnits, Matches.UnitIsSuicide), m_dependentUnits);
			getDisplay(bridge).deadUnitNotification(m_battleID, m_defender, Match.getMatches(m_defendingUnits, Matches.UnitIsSuicide), m_dependentUnits);
			// and remove them from the map display
			m_defendingUnits.removeAll(Match.getMatches(m_defendingUnits, Matches.UnitIsSuicide));
			m_attackingUnits.removeAll(Match.getMatches(m_attackingUnits, Matches.UnitIsSuicide));
		}
	}
	
	/**
	 * Check for unescorted TRNS and kill them immediately
	 * 
	 * @param bridge
	 * @param player
	 * @param defender
	 */
	private void checkUndefendedTransports(IDelegateBridge bridge, PlayerID player)
	{
		// Get all allied transports in the territory
		CompositeMatch<Unit> matchAllied = new CompositeMatchAnd<Unit>();
		matchAllied.add(Matches.UnitIsTransport);
		matchAllied.add(Matches.UnitIsNotCombatTransport);
		matchAllied.add(Matches.isUnitAllied(player, m_data));
		matchAllied.add(Matches.UnitIsSea);
		
		List<Unit> alliedTransports = Match.getMatches(m_battleSite.getUnits().getUnits(), matchAllied);
		
		// If no transports, just return
		if (alliedTransports.isEmpty())
			return;
		
		// Get all ALLIED, sea & air units in the territory (that are NOT submerged)
		CompositeMatch<Unit> alliedUnitsMatch = new CompositeMatchAnd<Unit>();
		alliedUnitsMatch.add(Matches.isUnitAllied(player, m_data));
		alliedUnitsMatch.add(Matches.UnitIsNotLand);
		alliedUnitsMatch.add(new InverseMatch<Unit>(Matches.unitIsSubmerged(m_data)));
		Collection<Unit> alliedUnits = Match.getMatches(m_battleSite.getUnits().getUnits(), alliedUnitsMatch);
		
		// If transports are unescorted, check opposing forces to see if the Trns die automatically
		if (alliedTransports.size() == alliedUnits.size())
		{
			// Get all the ENEMY sea and air units (that can attack) in the territory
			CompositeMatch<Unit> enemyUnitsMatch = new CompositeMatchAnd<Unit>();
			enemyUnitsMatch.add(Matches.UnitIsNotLand);
			// enemyUnitsMatch.add(Matches.UnitIsNotTransportButCouldBeCombatTransport);
			enemyUnitsMatch.add(Matches.unitIsNotSubmerged(m_data));
			enemyUnitsMatch.add(Matches.unitCanAttack(player));
			Collection<Unit> enemyUnits = Match.getMatches(m_battleSite.getUnits().getUnits(), enemyUnitsMatch);
			
			// If there are attackers set their movement to 0 and kill the transports
			if (enemyUnits.size() > 0)
			{
				Change change = ChangeFactory.markNoMovementChange(Match.getMatches(enemyUnits, Matches.UnitIsSea));
				bridge.addChange(change);
				
				remove(alliedTransports, bridge, m_battleSite, false);
				// and remove them from the battle display
				if (player.equals(m_defender))
					m_defendingUnits.removeAll(alliedTransports);
				else
					m_attackingUnits.removeAll(alliedTransports);
			}
		}
	}
	
	/**
	 * Submerge attacking/defending SUBS if they're alone OR with TRNS against only AIRCRAFT
	 * 
	 * @param bridge
	 * @param player
	 * @param defender
	 */
	private void submergeSubsVsOnlyAir(IDelegateBridge bridge)
	{
		// if All attackers are AIR submerge any defending subs ..m_defendingUnits.removeAll(m_killed);
		if (Match.allMatch(m_attackingUnits, Matches.UnitIsAir) && Match.someMatch(m_defendingUnits, Matches.UnitIsSub))
		{
			// Get all defending subs (including allies) in the territory
			CompositeMatch<Unit> matchDefendingSubs = new CompositeMatchAnd<Unit>();
			matchDefendingSubs.add(Matches.UnitIsSub);
			matchDefendingSubs.add(Matches.isUnitAllied(m_defender, m_data));
			List<Unit> defendingSubs = Match.getMatches(m_defendingUnits, matchDefendingSubs);
			
			// submerge defending subs
			submergeUnits(defendingSubs, true, bridge);
			// getDisplay(bridge).notifyRetreat(messageShort, messageShort, step, m_defender);
			
		} // checking defending air on attacking subs
		else if (Match.allMatch(m_defendingUnits, Matches.UnitIsAir) && Match.someMatch(m_attackingUnits, Matches.UnitIsSub))
		{
			// Get all attacking subs in the territory
			CompositeMatch<Unit> matchAttackingSubs = new CompositeMatchAnd<Unit>();
			matchAttackingSubs.add(Matches.UnitIsSub);
			matchAttackingSubs.add(Matches.isUnitAllied(m_attacker, m_data));
			List<Unit> attackingSubs = Match.getMatches(m_attackingUnits, matchAttackingSubs);
			
			// submerge attacking subs
			submergeUnits(attackingSubs, false, bridge);
		}
	}
	
	private void defendNonSubs(IDelegateBridge bridge)
	{
		if (m_attackingUnits.size() == 0)
			return;
		
		Collection<Unit> units = new ArrayList<Unit>(m_defendingUnits.size()
					+ m_defendingWaitingToDie.size());
		units.addAll(m_defendingUnits);
		units.addAll(m_defendingWaitingToDie);
		units = Match.getMatches(units, Matches.UnitIsNotSub);
		
		// if restricted, remove aircraft from attackers
		if (isAirAttackSubRestricted() && !canAirAttackSubs(m_attackingUnits, units))
		{
			units.removeAll(Match.getMatches(units, Matches.UnitIsAir));
		}
		
		if (units.isEmpty())
			return;
		
		fire(m_attacker.getName() + SELECT_CASUALTIES, units,
					m_attackingUnits, true, ReturnFire.ALL, bridge, "Defenders fire, ");
	}
	
	// If there are no attacking DDs but defending SUBs, fire AIR at non-SUB forces ONLY
	private void attackAirOnNonSubs(IDelegateBridge bridge)
	{
		if (m_defendingUnits.size() == 0)
			return;
		
		Collection<Unit> units = new ArrayList<Unit>(m_attackingUnits.size() + m_attackingWaitingToDie.size());
		units.addAll(m_attackingUnits);
		units.addAll(m_attackingWaitingToDie);
		// See if allied air can participate in combat
		if (isAlliedAirDependents())
			units = Match.getMatches(units, Matches.unitIsOwnedBy(m_attacker));
		
		if (!canAirAttackSubs(m_defendingUnits, units))
		{
			units = Match.getMatches(units, Matches.UnitIsAir);
			Collection<Unit> enemyUnitsNotSubs = Match.getMatches(m_defendingUnits, Matches.UnitIsNotSub);
			
			fire(m_defender.getName() + SELECT_CASUALTIES, units, enemyUnitsNotSubs, false, ReturnFire.ALL, bridge, "Attacker's aircraft fire,");
		}
	}
	
	private boolean canAirAttackSubs(Collection<Unit> firedAt, Collection<Unit> firing)
	{
		if (m_battleSite.isWater() && Match.someMatch(firedAt, Matches.UnitIsSub) && Match.noneMatch(firing, Matches.UnitIsDestroyer))
		{
			return false;
		}
		
		return true;
	}
	
	private void defendAirOnNonSubs(IDelegateBridge bridge)
	{
		if (m_attackingUnits.size() == 0)
			return;
		
		Collection<Unit> units = new ArrayList<Unit>(m_defendingUnits.size() + m_defendingWaitingToDie.size());
		units.addAll(m_defendingUnits);
		units.addAll(m_defendingWaitingToDie);
		// units = Match.getMatches(units, Matches.unitIsOwnedBy(m_defender)); //why is this here? allied air units can still shoot!
		
		if (!canAirAttackSubs(m_attackingUnits, units))
		{
			units = Match.getMatches(units, Matches.UnitIsAir);
			Collection<Unit> enemyUnitsNotSubs = Match.getMatches(m_attackingUnits, Matches.UnitIsNotSub);
			if (enemyUnitsNotSubs.isEmpty())
				return;
			
			fire(m_defender.getName() + SELECT_CASUALTIES, units, enemyUnitsNotSubs, true, ReturnFire.ALL, bridge, "Defender's aircraft fire,");
		}
	}
	
	// If there are no attacking DDs, but defending SUBs, remove attacking AIR as they've already fired- otherwise fire all attackers.
	private void attackNonSubs(IDelegateBridge bridge)
	{
		if (m_defendingUnits.size() == 0)
			return;
		
		Collection<Unit> units = Match.getMatches(m_attackingUnits,
					Matches.UnitIsNotSub);
		units.addAll(Match.getMatches(m_attackingWaitingToDie,
					Matches.UnitIsNotSub));
		// See if allied air can participate in combat
		if (isAlliedAirDependents())
			units = Match.getMatches(units, Matches.unitIsOwnedBy(m_attacker));
		// if restricted, remove aircraft from attackers
		if (isAirAttackSubRestricted() && !canAirAttackSubs(m_defendingUnits, units))
		{
			units.removeAll(Match.getMatches(units, Matches.UnitIsAir));
		}
		
		if (units.isEmpty())
			return;
		
		fire(m_defender.getName() + SELECT_CASUALTIES, units,
					m_defendingUnits, false, ReturnFire.ALL, bridge, "Attackers fire,");
	}
	
	private void attackSubs(IDelegateBridge bridge, ReturnFire returnFire)
	{
		Collection<Unit> firing = Match.getMatches(m_attackingUnits,
					Matches.UnitIsSub);
		if (firing.isEmpty())
			return;
		
		Collection<Unit> attacked = Match.getMatches(m_defendingUnits,
					Matches.UnitIsNotAir);
		// if there are destroyers in the attacked units, we can return fire.
		
		fire(m_defender.getName() + SELECT_SUB_CASUALTIES, firing, attacked, false,
					returnFire, bridge, "Subs fire,");
	}
	
	private void defendSubs(IDelegateBridge bridge, ReturnFire returnFire)
	{
		if (m_attackingUnits.size() == 0)
			return;
		
		Collection<Unit> firing = new ArrayList<Unit>(m_defendingUnits.size() + m_defendingWaitingToDie.size());
		firing.addAll(m_defendingUnits);
		firing.addAll(m_defendingWaitingToDie);
		firing = Match.getMatches(firing, Matches.UnitIsSub);
		
		if (firing.isEmpty())
			return;
		
		Collection<Unit> attacked = Match.getMatches(m_attackingUnits,
					Matches.UnitIsNotAir);
		
		if (attacked.isEmpty())
			return;
		
		fire(m_attacker.getName() + SELECT_SUB_CASUALTIES, firing,
					attacked, true, returnFire, bridge, "Subs defend, ");
	}
	
	private void attackAny(IDelegateBridge bridge)
	{
		if (m_defendingUnits.size() == 0)
			return;
		
		Collection<Unit> units = new ArrayList<Unit>(m_attackingUnits.size()
					+ m_attackingWaitingToDie.size());
		units.addAll(m_attackingUnits);
		units.addAll(m_attackingWaitingToDie);
		
		if (isAirAttackSubRestricted() && !canAirAttackSubs(m_defendingUnits, units))
		{
			units.removeAll(Match.getMatches(units, Matches.UnitIsAir));
		}
		
		if (units.isEmpty())
			return;
		
		fire(m_defender.getName() + SELECT_CASUALTIES, units,
					m_defendingUnits, false, ReturnFire.ALL, bridge, "Attackers fire,");
	}
	
	private void defendAny(IDelegateBridge bridge)
	{
		
		if (m_attackingUnits.size() == 0)
			return;
		
		Collection<Unit> units = new ArrayList<Unit>(m_defendingUnits.size()
					+ m_defendingWaitingToDie.size());
		units.addAll(m_defendingUnits);
		units.addAll(m_defendingWaitingToDie);
		// if restricted, remove aircraft from attackers
		
		if (isAirAttackSubRestricted() && !canAirAttackSubs(m_attackingUnits, units))
		{
			units.removeAll(Match.getMatches(units, Matches.UnitIsAir));
		}
		
		if (units.isEmpty())
			return;
		
		fire(m_attacker.getName() + SELECT_CASUALTIES, units,
					m_attackingUnits, true, ReturnFire.ALL, bridge, "Defenders fire, ");
	}
	
	void removeCasualties(Collection<Unit> killed, ReturnFire returnFire,
				boolean defender, IDelegateBridge bridge, boolean isAA)
	{
		if (killed.isEmpty())
			return;
		
		if (returnFire == ReturnFire.ALL)
		{
			// move to waiting to die
			if (defender)
				m_defendingWaitingToDie.addAll(killed);
			else
				m_attackingWaitingToDie.addAll(killed);
		}
		else if (returnFire == ReturnFire.SUBS)
		{
			// move to waiting to die
			if (defender)
				m_defendingWaitingToDie.addAll(Match.getMatches(killed, Matches.UnitIsSub));
			else
				m_attackingWaitingToDie.addAll(Match.getMatches(killed, Matches.UnitIsSub));
			remove(Match.getMatches(killed, Matches.UnitIsNotSub), bridge, m_battleSite, isAA);
		}
		else if (returnFire == ReturnFire.NONE)
		{
			remove(killed, bridge, m_battleSite, isAA);
		}
		
		// remove from the active fighting
		if (defender)
			m_defendingUnits.removeAll(killed);
		else
			m_attackingUnits.removeAll(killed);
	}
	
	private void fireNavalBombardment(IDelegateBridge bridge)
	{
		// TODO - check within the method for the bombarding limitations
		Collection<Unit> bombard = getBombardingUnits();
		Collection<Unit> attacked = Match.getMatches(m_defendingUnits,
					Matches.UnitIsDestructibleInCombat(m_attacker, m_battleSite, m_data));
		
		// bombarding units cant move after bombarding
		if (!m_headless)
		{
			Change change = ChangeFactory.markNoMovementChange(bombard);
			bridge.addChange(change);
		}
		// TODO
		/**
		 * This code is actually a bug- the property is intended to tell if the return fire is
		 * RESTRICTED- but it's used as if it's ALLOWED. The reason is the default values on the
		 * property definition. However, fixing this will entail a fix to the XML to reverse
		 * all values. We'll leave it as is for now and try to figure out a patch strategy later.
		 */
		boolean canReturnFire = (isNavalBombardCasualtiesReturnFire());
		
		if (bombard.size() > 0 && attacked.size() > 0)
		{
			fire(SELECT_NAVAL_BOMBARDMENT_CASUALTIES, bombard, attacked, false,
						canReturnFire ? ReturnFire.ALL : ReturnFire.NONE, bridge, "Bombard");
		}
	}
	
	private void fireSuicideUnitsAttack(IDelegateBridge bridge)
	{
		// TODO: add a global toggle for returning fire (Veqryn)
		CompositeMatch<Unit> attackableUnits = new CompositeMatchAnd<Unit>(Matches.UnitIsDestructibleInCombat(m_attacker, m_battleSite, m_data), Matches.UnitIsSuicide.invert());
		Collection<Unit> suicideAttackers = Match.getMatches(m_attackingUnits, Matches.UnitIsSuicide);
		Collection<Unit> attackedDefenders = Match.getMatches(m_defendingUnits, attackableUnits);
		
		// comparatively simple rules for isSuicide units. if AirAttackSubRestricted and you have no destroyers, you can't attack subs with anything.
		if (isAirAttackSubRestricted() && !Match.someMatch(m_attackingUnits, Matches.UnitIsDestroyer) && Match.someMatch(attackedDefenders, Matches.UnitIsSub))
			attackedDefenders.removeAll(Match.getMatches(attackedDefenders, Matches.UnitIsSub));
		
		if (Match.allMatch(suicideAttackers, Matches.UnitIsSub))
			attackedDefenders.removeAll(Match.getMatches(attackedDefenders, Matches.UnitIsAir));
		
		if (suicideAttackers.size() == 0 || attackedDefenders.size() == 0)
			return;
		
		boolean canReturnFire = (!isSuicideAndMunitionCasualtiesRestricted());
		
		fire(m_defender.getName() + SELECT_CASUALTIES_SUICIDE, suicideAttackers, attackedDefenders, false, canReturnFire ? ReturnFire.ALL : ReturnFire.NONE, bridge, SUICIDE_ATTACK);
	}
	
	private void fireSuicideUnitsDefend(IDelegateBridge bridge)
	{
		if (isDefendingSuicideAndMunitionUnitsDoNotFire())
			return;
		
		// TODO: add a global toggle for returning fire (Veqryn)
		CompositeMatch<Unit> attackableUnits = new CompositeMatchAnd<Unit>(Matches.UnitIsDestructibleInCombatShort, Matches.UnitIsSuicide.invert());
		Collection<Unit> suicideDefenders = Match.getMatches(m_defendingUnits, Matches.UnitIsSuicide);
		Collection<Unit> attackedAttackers = Match.getMatches(m_attackingUnits, attackableUnits);
		
		// comparatively simple rules for isSuicide units. if AirAttackSubRestricted and you have no destroyers, you can't attack subs with anything.
		if (isAirAttackSubRestricted() && !Match.someMatch(m_defendingUnits, Matches.UnitIsDestroyer) && Match.someMatch(attackedAttackers, Matches.UnitIsSub))
			attackedAttackers.removeAll(Match.getMatches(attackedAttackers, Matches.UnitIsSub));
		
		if (Match.allMatch(suicideDefenders, Matches.UnitIsSub))
			suicideDefenders.removeAll(Match.getMatches(suicideDefenders, Matches.UnitIsAir));
		
		if (suicideDefenders.size() == 0 || attackedAttackers.size() == 0)
			return;
		
		boolean canReturnFire = (!isSuicideAndMunitionCasualtiesRestricted());
		
		fire(m_attacker.getName() + SELECT_CASUALTIES_SUICIDE, suicideDefenders, attackedAttackers, true, canReturnFire ? ReturnFire.ALL : ReturnFire.NONE, bridge, SUICIDE_DEFEND);
	}
	
	/**
	 * @return
	 */
	private boolean isWW2V2()
	{
		return games.strategy.triplea.Properties.getWW2V2(m_data);
	}
	
	private boolean isWW2V3()
	{
		return games.strategy.triplea.Properties.getWW2V3(m_data);
	}
	
	private boolean isPartialAmphibiousRetreat()
	{
		return games.strategy.triplea.Properties.getPartialAmphibiousRetreat(m_data);
	}
	
	private boolean isParatroopers(PlayerID player)
	{
		TechAttachment ta = (TechAttachment) player.getAttachment(Constants.TECH_ATTACHMENT_NAME);
		if (ta == null)
			return false;
		return ta.hasParatroopers();
	}
	
	/**
	 * @return
	 */
	private boolean isAlliedAirDependents()
	{
		return games.strategy.triplea.Properties.getAlliedAirDependents(m_data);
	}
	
	/**
	 * @return
	 */
	private boolean isDefendingSubsSneakAttack()
	{
		return games.strategy.triplea.Properties.getDefendingSubsSneakAttack(m_data);
	}
	
	/**
	 * @return
	 */
	private boolean isAttackerRetreatPlanes()
	{
		return games.strategy.triplea.Properties.getAttackerRetreatPlanes(m_data);
	}
	
	/**
	 * @return
	 */
	private boolean isNavalBombardCasualtiesReturnFire()
	{
		return games.strategy.triplea.Properties.getNavalBombardCasualtiesReturnFireRestricted(m_data);
	}
	
	/**
	 * @return
	 */
	private boolean isSuicideAndMunitionCasualtiesRestricted()
	{
		return games.strategy.triplea.Properties.getSuicideAndMunitionCasualtiesRestricted(m_data);
	}
	
	/**
	 * @return
	 */
	private boolean isDefendingSuicideAndMunitionUnitsDoNotFire()
	{
		return games.strategy.triplea.Properties.getDefendingSuicideAndMunitionUnitsDoNotFire(m_data);
	}
	
	/**
	 * @return
	 */
	private boolean isSurvivingAirMoveToLand()
	{
		return games.strategy.triplea.Properties.getSurvivingAirMoveToLand(m_data);
	}
	
	/**
	 * @return
	 */
	private boolean isAirAttackSubRestricted()
	{
		return games.strategy.triplea.Properties.getAirAttackSubRestricted(m_data);
	}
	
	/**
	 * @return
	 */
	private boolean isSubRetreatBeforeBattle()
	{
		return games.strategy.triplea.Properties.getSubRetreatBeforeBattle(m_data);
	}
	
	/**
	 * @return
	 */
	private boolean isTransportCasualtiesRestricted()
	{
		return games.strategy.triplea.Properties.getTransportCasualtiesRestricted(m_data);
	}
	
	/**
	 * Return the territories where there are amphibious attacks.
	 */
	public Collection<Territory> getAmphibiousAttackTerritories()
	{
		return m_amphibiousAttackFrom;
	}
	
	/**
	 * Add bombarding unit.
	 */
	@Override
	public void addBombardingUnit(Unit unit)
	{
		m_bombardingUnits.add(unit);
	}
	
	/**
	 * Return bombarding units.
	 */
	@Override
	public Collection<Unit> getBombardingUnits()
	{
		return m_bombardingUnits;
	}
	
	private void fireAAGuns(final IDelegateBridge bridge)
	{
		m_stack.push(new FireAA());
	}
	
	
	class FireAA implements IExecutable
	{
		private DiceRoll m_dice;
		private Collection<Unit> m_casualties;
		private List<Unit> m_hitUnits = new ArrayList<Unit>();
		
		@Override
		public void execute(ExecutionStack stack, final IDelegateBridge bridge)
		{
			if (!canFireAA())
				return;
			
			IExecutable rollDice = new IExecutable()
			{
				
				@Override
				public void execute(ExecutionStack stack, IDelegateBridge bridge)
				{
					rollDice(bridge);
				}
				
			};
			
			IExecutable selectCasualties = new IExecutable()
			{
				
				@Override
				public void execute(ExecutionStack stack, IDelegateBridge bridge)
				{
					selectCasualties(bridge);
				}
			};
			
			IExecutable notifyCasualties = new IExecutable()
			{
				
				@Override
				public void execute(ExecutionStack stack, IDelegateBridge bridge)
				{
					notifyCasualtiesAA(bridge);
					removeCasualties(m_casualties, ReturnFire.NONE, false, bridge, true);
				}
			};
			// push in reverse order of execution
			stack.push(notifyCasualties);
			stack.push(selectCasualties);
			stack.push(rollDice);
		}
		
		private void rollDice(IDelegateBridge bridge)
		{
			m_dice = DiceRoll.rollAA(m_attackingUnits, bridge, m_battleSite, Matches.UnitIsAAforCombat);
			
		}
		
		private void selectCasualties(final IDelegateBridge bridge)
		{
			// send defender the dice roll so he can see what the dice are while he
			// waits for attacker to select casualties
			getDisplay(bridge).notifyDice(m_battleID, m_dice, SELECT_AA_CASUALTIES);
			
			Collection<Unit> attackable = Match.getMatches(m_attackingUnits, Matches.UnitIsAir);
			
			m_casualties = BattleCalculator.getAACasualties(attackable, m_dice, bridge, m_defender, m_attacker, m_battleID, m_battleSite, Matches.UnitIsAAforCombat);
		}
		
		private void notifyCasualtiesAA(final IDelegateBridge bridge)
		{
			if (m_headless)
				return;
			
			getDisplay(bridge).casualtyNotification(m_battleID, SELECT_AA_CASUALTIES, m_dice, m_attacker, new ArrayList<Unit>(m_casualties), Collections.<Unit> emptyList(), m_dependentUnits);
			
			getRemote(m_attacker, bridge).confirmOwnCasualties(m_battleID, "Press space to continue");
			Runnable r = new Runnable()
			{
				@Override
				public void run()
				{
					try
					{
						getRemote(m_defender, bridge).confirmEnemyCasualties(m_battleID, "Press space to continue", m_attacker);
						
					}
						catch (ConnectionLostException cle)
					{
						// somone else will deal with this
						cle.printStackTrace(System.out);
					}
						catch (GameOverException e)
					{
						// ignore
					}
				}
			};
			Thread t = new Thread(r, "click to continue waiter");
			t.start();
			try
			{
				bridge.leaveDelegateExecution();
				t.join();
			} catch (InterruptedException e)
			{
				// ignore
			} finally
			{
				bridge.enterDelegateExecution();
			}
		}
	}
	
	private boolean canFireAA()
	{
		
		return Match.someMatch(m_defendingUnits, Matches.UnitIsAAforCombat)
					&& Match.someMatch(m_attackingUnits, Matches.UnitIsAir)
					&& !m_battleSite.isWater();
	}
	
	/**
	 * @return a collection containing all the combatants in units non
	 *         combatants include such things as factories, aaguns, land units
	 *         in a water battle.
	 */
	private List<Unit> removeNonCombatants(Collection<Unit> units, boolean attacking, PlayerID player)
	{
		CompositeMatch<Unit> combat = new CompositeMatchAnd<Unit>();
		combat.add(new InverseMatch<Unit>(Matches.UnitIsAAOrFactory));
		
		if (m_battleSite.isWater())
		{
			combat.add(Matches.UnitIsNotLand);
		}
		
		List<Unit> unitList = Match.getMatches(units, combat);
		
		// still allow infrastructure type units that can provide support have combat abilities
		CompositeMatch<Unit> infrastructureNotSupporterAndNotHasCombatAbilities = new CompositeMatchAnd<Unit>(Matches.UnitIsInfrastructure, Matches.UnitIsSupporterOrHasCombatAbility(attacking,
					player, m_data).invert());
		// remove infrastructure units that can't take part in combat (air/naval bases, etc...)
		unitList.removeAll(Match.getMatches(unitList, infrastructureNotSupporterAndNotHasCombatAbilities));
		
		// remove any disabled units from combat
		unitList.removeAll(Match.getMatches(unitList, Matches.UnitIsDisabled()));
		
		// remove capturableOnEntering units (veqryn)
		unitList.removeAll(Match.getMatches(unitList, Matches.UnitCanBeCapturedOnEnteringToInThisTerritory(m_attacker, m_battleSite, m_data)));
		
		// remove any allied air units that are stuck on damaged carriers (veqryn)
		unitList.removeAll(Match.getMatches(unitList, new CompositeMatchAnd<Unit>(Matches.unitIsBeingTransported(), Matches.UnitIsAir, Matches.UnitCanLandOnCarrier)));
		
		return unitList;
	}
	
	private void removeNonCombatants()
	{
		m_defendingUnits = removeNonCombatants(m_defendingUnits, false, m_defender);
		m_attackingUnits = removeNonCombatants(m_attackingUnits, true, m_attacker);
	}
	
	private void landParatroops(IDelegateBridge bridge)
	{
		if (isParatroopers(m_attacker))
		{
			Collection<Unit> airTransports = Match.getMatches(m_battleSite.getUnits().getUnits(), Matches.UnitIsAirTransport);
			
			if (!airTransports.isEmpty())
			{
				Collection<Unit> dependents = getDependentUnits(airTransports);
				if (!dependents.isEmpty())
				{
					Iterator<Unit> dependentsIter = dependents.iterator();
					
					CompositeChange change = new CompositeChange();
					// remove dependency from paratroops
					while (dependentsIter.hasNext())
					{
						Unit unit = dependentsIter.next();
						change.add(ChangeFactory.unitPropertyChange(unit, null, TripleAUnit.TRANSPORTED_BY));
					}
					bridge.addChange(change);
					
					// remove bombers from m_dependentUnits
					Iterator<Unit> bombersIter = airTransports.iterator();
					while (bombersIter.hasNext())
					{
						Unit unit = bombersIter.next();
						m_dependentUnits.remove(unit);
					}
				}
			}
		}
	}
	
	@Override
	public Collection<Unit> getDependentUnits(Collection<Unit> units)
	{
		
		Iterator<Unit> iter = units.iterator();
		Collection<Unit> dependents = new ArrayList<Unit>();
		while (iter.hasNext())
		{
			Unit currentUnit = iter.next();
			
			Collection<Unit> depending = m_dependentUnits.get(currentUnit);
			if (depending != null)
			{
				dependents.addAll(depending);
			}
		}
		return dependents;
	}
	
	// Figure out what units a transport is transported and has unloaded
	public Collection<Unit> getTransportDependents(Collection<Unit> targets, GameData data)
	{
		if (m_headless)
		{
			return Collections.emptyList();
		}
		Collection<Unit> dependents = new ArrayList<Unit>();
		if (Match.someMatch(targets, Matches.UnitCanTransport))
		{
			// just worry about transports
			TransportTracker tracker = DelegateFinder.moveDelegate(data).getTransportTracker();
			
			Iterator<Unit> iter = targets.iterator();
			while (iter.hasNext())
			{
				Unit target = iter.next();
				dependents.addAll(tracker.transportingAndUnloaded(target));
			}
		}
		return dependents;
	}
	
	void markDamaged(Collection<Unit> damaged, IDelegateBridge bridge)
	{
		if (damaged.size() == 0)
			return;
		Change damagedChange = null;
		IntegerMap<Unit> damagedMap = new IntegerMap<Unit>();
		damagedMap.putAll(damaged, 1);
		damagedChange = ChangeFactory.unitsHit(damagedMap);
		bridge.getHistoryWriter().addChildToEvent(
					"Units damaged: " + MyFormatter.unitsToText(damaged), damaged);
		bridge.addChange(damagedChange);
	}
	
	private void remove(Collection<Unit> killed, IDelegateBridge bridge, Territory battleSite, boolean isAA)
	{
		if (killed.size() == 0)
			return;
		
		Collection<Unit> dependent = getDependentUnits(killed);
		
		killed.addAll(dependent);
		
		Change killedChange = ChangeFactory.removeUnits(battleSite, killed);
		m_killed.addAll(killed);
		
		String transcriptText = MyFormatter.unitsToText(killed) + " lost in " + battleSite.getName();
		bridge.getHistoryWriter().addChildToEvent(transcriptText, killed);
		
		bridge.addChange(killedChange);
		
		Collection<Battle> dependentBattles = m_tracker.getBlocked(this);
		// If there are NO dependent battles, check for unloads in allied territories
		if (dependentBattles.isEmpty())
			removeFromNonCombatLandings(killed, bridge);
		// otherwise remove them and the units involved
		else
			removeFromDependents(killed, bridge, dependentBattles);
	}
	
	private void removeFromDependents(Collection<Unit> units, IDelegateBridge bridge, Collection<Battle> dependents)
	{
		Iterator<Battle> iter = dependents.iterator();
		while (iter.hasNext())
		{
			Battle dependent = iter.next();
			dependent.unitsLostInPrecedingBattle(this, units, bridge);
		}
	}
	
	// Remove landed units from allied territory when their transport sinks
	private void removeFromNonCombatLandings(Collection<Unit> units, IDelegateBridge bridge)
	{
		
		for (Unit transport : Match.getMatches(units, Matches.UnitIsTransport))
		{
			Collection<Unit> lost = getTransportDependents(Collections.singleton(transport), m_data);
			if (lost.isEmpty())
			{
				continue;
			}
			Territory landedTerritory = getTransportTracker().getTerritoryTransportHasUnloadedTo(transport);
			
			if (landedTerritory == null)
			{
				throw new IllegalStateException("not unloaded?:" + units);
			}
			m_attackingUnits.removeAll(lost);
			remove(lost, bridge, landedTerritory, false);
		}
	}
	
	private void clearWaitingToDie(IDelegateBridge bridge)
	{
		
		Collection<Unit> units = new ArrayList<Unit>();
		units.addAll(m_attackingWaitingToDie);
		units.addAll(m_defendingWaitingToDie);
		remove(units, bridge, m_battleSite, false);
		m_defendingWaitingToDie.clear();
		m_attackingWaitingToDie.clear();
	}
	
	private void defenderWins(IDelegateBridge bridge)
	{
		getDisplay(bridge).battleEnd(m_battleID, m_defender.getName() + " win");
		
		bridge.getHistoryWriter()
					.addChildToEvent(m_defender.getName() + " win", m_defendingUnits);
		showCasualties(bridge);
		
		checkDefendingPlanesCanLand(bridge, m_defender);
		
		BattleTracker.captureOrDestroyUnits(m_battleSite, m_defender, m_defender, bridge, null, m_defendingUnits);
	}
	
	private void nobodyWins(IDelegateBridge bridge)
	{
		getDisplay(bridge).battleEnd(m_battleID, "Stalemate");
		
		bridge.getHistoryWriter()
					.addChildToEvent(m_defender.getName() + " and " + m_attacker.getName() + " reach a stalemate");
		showCasualties(bridge);
		
	}
	
	static ITripleaPlayer getRemote(PlayerID player, IDelegateBridge bridge)
	{
		// if its the null player, return a do nothing proxy
		if (player.isNull())
			return new WeakAI(player.getName());
		return (ITripleaPlayer) bridge.getRemote(player);
	}
	
	/**
	 * If the attacker retreats, and this is a sea zone, then any attacking fighters with
	 * 0 movement get a 1 movement bonus to allow them to retreat.
	 * 
	 * This handles the case where fighters will die if they have 0 movement when they arrive
	 * in the attacking zone, but they arrived with a carrier which retreated
	 */
	private void ensureAttackingAirCanRetreat(IDelegateBridge bridge)
	{
		MoveDelegate moveDelegate = DelegateFinder.moveDelegate(m_data);
		
		CompositeMatch<Unit> canLandOnCarrier = new CompositeMatchAnd<Unit>();
		canLandOnCarrier.add(Matches.UnitIsAir);
		// this only applies to air units that can land on a carrier
		canLandOnCarrier.add(Matches.UnitCanLandOnCarrier);
		
		Collection<Unit> air = Match.getMatches(m_attackingUnits, canLandOnCarrier);
		
		// TODO interesting quirk- kamikaze aircraft may move their full movement, then one more on retreat due to this
		for (Unit unit : air)
		{
			bridge.addChange(moveDelegate.ensureCanMoveOneSpaceChange(unit));
		}
	}
	
	/**
	 * The defender has won, but there may be defending fighters that cant stay
	 * in the sea zone due to insufficient carriers.
	 */
	private void checkDefendingPlanesCanLand(IDelegateBridge bridge,
				PlayerID defender)
	{
		
		if (m_headless)
			return;
		
		// not water, not relevant.
		if (!m_battleSite.isWater())
			return;
		
		CompositeMatch<Unit> alliedDefendingAir = new CompositeMatchAnd<Unit>(
					Matches.UnitIsAir, Matches.isUnitAllied(m_defender, m_data));
		m_defendingAir = Match.getMatches(m_defendingUnits,
					alliedDefendingAir);
		
		// no planes, exit
		if (m_defendingAir.isEmpty())
			return;
		
		int carrierCost = MoveValidator.carrierCost(m_defendingAir);
		int carrierCapacity = MoveValidator.carrierCapacity(m_defendingUnits);
		// add dependant air to carrier cost
		carrierCost += MoveValidator.carrierCost(Match.getMatches(getDependentUnits(m_defendingUnits), alliedDefendingAir));
		
		// all planes can land, exit
		if (carrierCapacity >= carrierCost)
			return;
		
		// find out what we must remove
		// remove all the air that can land on carriers from defendingAir
		carrierCost = 0;
		// add dependant air to carrier cost
		carrierCost += MoveValidator.carrierCost(Match.getMatches(getDependentUnits(m_defendingUnits), alliedDefendingAir));
		Iterator<Unit> defendingAirIter = new ArrayList<Unit>(m_defendingAir).iterator();
		while (defendingAirIter.hasNext() && carrierCapacity >= carrierCost)
		{
			Unit currentUnit = defendingAirIter.next();
			carrierCost += UnitAttachment.get(currentUnit.getType())
						.getCarrierCost();
			if (carrierCapacity >= carrierCost)
			{
				m_defendingAir.remove(currentUnit);
			}
		}
		
		// Get all land territories where there are no pending battles
		Set<Territory> neighbors = m_data.getMap().getNeighbors(m_battleSite);
		CompositeMatch<Territory> alliedLandTerritories = new CompositeMatchAnd<Territory>(
					Matches.TerritoryIsLand, Matches.isTerritoryAllied(m_defender, m_data),
					Matches.territoryHasNoEnemyUnits(m_defender, m_data));
		// Get those that are neighbors
		Collection<Territory> canLandHere = Match.getMatches(neighbors, alliedLandTerritories);
		
		// Get all sea territories where there are allies and no pending battles
		CompositeMatch<Territory> neighboringSeaZonesWithAlliedUnits = new CompositeMatchAnd<Territory>(
					Matches.TerritoryIsWater, Matches.territoryHasEnemyUnits(m_attacker, m_data),
					Matches.territoryHasNoEnemyUnits(m_defender, m_data));
		// Get those that are neighbors
		Collection<Territory> areSeaNeighbors = Match.getMatches(neighbors, neighboringSeaZonesWithAlliedUnits);
		
		// Set up match criteria for allied carriers
		CompositeMatch<Unit> alliedCarrier = new CompositeMatchAnd<Unit>();
		alliedCarrier.add(Matches.UnitIsCarrier);
		alliedCarrier.add(Matches.alliedUnit(m_defender, m_data));
		
		// Set up match criteria for allied planes
		CompositeMatch<Unit> alliedPlane = new CompositeMatchAnd<Unit>();
		alliedPlane.add(Matches.UnitIsAir);
		alliedPlane.add(Matches.alliedUnit(m_defender, m_data));
		
		// See if neighboring carriers have any capacity available
		Iterator<Territory> neighborSeaZoneIter = areSeaNeighbors.iterator();
		while (neighborSeaZoneIter.hasNext())
		{
			Territory currentTerritory = neighborSeaZoneIter.next();
			
			// get the capacity of the carriers and cost of fighters
			Collection<Unit> alliedCarriers = currentTerritory.getUnits().getMatches(alliedCarrier);
			Collection<Unit> alliedPlanes = currentTerritory.getUnits().getMatches(alliedPlane);
			int alliedCarrierCapacity = MoveValidator.carrierCapacity(alliedCarriers);
			int alliedPlaneCost = MoveValidator.carrierCost(alliedPlanes);
			// if there is free capacity, add the territory to landing possibilities
			if (alliedCarrierCapacity - alliedPlaneCost >= 1)
			{
				canLandHere.add(currentTerritory);
			}
		}
		
		if (isWW2V2() || isSurvivingAirMoveToLand())
		{
			Territory territory = null;
			while (canLandHere.size() > 1 && m_defendingAir.size() > 0)
			{
				territory = getRemote(m_defender, bridge).selectTerritoryForAirToLand(canLandHere);
				
				// added for test script
				if (territory == null)
				{
					territory = canLandHere.iterator().next();
				}
				
				if (territory.isWater())
				{
					landPlanesOnCarriers(bridge, alliedDefendingAir, m_defendingAir, canLandHere, alliedCarrier, alliedPlane, territory);
				}
				else
				{
					moveAirAndLand(bridge, m_defendingAir, territory);
					return;
				}
				// remove the territory from those available
				canLandHere.remove(territory);
			}
			
			// Land in the last remaining territory
			if (canLandHere.size() > 0 && m_defendingAir.size() > 0)
			{
				territory = canLandHere.iterator().next();
				
				if (territory.isWater())
				{
					landPlanesOnCarriers(bridge, alliedDefendingAir, m_defendingAir, canLandHere, alliedCarrier, alliedPlane, territory);
				}
				else
				{
					moveAirAndLand(bridge, m_defendingAir, territory);
					return;
				}
				
			}
		}
		else if (canLandHere.size() > 0)
		{
			// now defending air has what cant stay, is there a place we can go?
			// check for an island in this sea zone
			Iterator<Territory> neighborsIter = canLandHere.iterator();
			while (neighborsIter.hasNext())
			{
				Territory currentTerritory = neighborsIter.next();
				// only one neighbor, its an island.
				if (m_data.getMap().getNeighbors(currentTerritory).size() == 1)
				{
					moveAirAndLand(bridge, m_defendingAir, currentTerritory);
					return;
				}
			}
		}
		
		if (m_defendingAir.size() > 0)
		{
			// no where to go, they must die
			bridge.getHistoryWriter().addChildToEvent(
						MyFormatter.unitsToText(m_defendingAir)
									+ " could not land and were killed", m_defendingAir);
			Change change = ChangeFactory.removeUnits(m_battleSite, m_defendingAir);
			bridge.addChange(change);
		}
	}
	
	// Refactored this method
	private void landPlanesOnCarriers(IDelegateBridge bridge,
				CompositeMatch<Unit> alliedDefendingAir,
				Collection<Unit> defendingAir, Collection<Territory> canLandHere,
				CompositeMatch<Unit> alliedCarrier,
				CompositeMatch<Unit> alliedPlane, Territory territory)
	{
		// Get the capacity of the carriers in the selected zone
		Collection<Unit> alliedCarriersSelected = territory.getUnits().getMatches(alliedCarrier);
		Collection<Unit> alliedPlanesSelected = territory.getUnits().getMatches(alliedPlane);
		int alliedCarrierCapacitySelected = MoveValidator.carrierCapacity(alliedCarriersSelected);
		int alliedPlaneCostSelected = MoveValidator.carrierCost(alliedPlanesSelected);
		
		// Find the available capacity of the carriers in that territory
		int territoryCapacity = alliedCarrierCapacitySelected - alliedPlaneCostSelected;
		if (territoryCapacity > 0)
		{
			// move that number of planes from the battlezone
			Collection<Unit> movingAir = Match.getNMatches(defendingAir, territoryCapacity, alliedDefendingAir);
			moveAirAndLand(bridge, movingAir, territory);
		}
	}
	
	// Refactored this method
	private void moveAirAndLand(IDelegateBridge bridge,
				Collection<Unit> defendingAir, Territory territory)
	{
		bridge.getHistoryWriter().addChildToEvent(
					MyFormatter.unitsToText(defendingAir) + " forced to land in "
								+ territory.getName(), defendingAir);
		Change change = ChangeFactory.moveUnits(m_battleSite, territory,
					defendingAir);
		bridge.addChange(change);
		
		// remove those that landed in case it was a carrier
		m_defendingAir.removeAll(defendingAir);
	}
	
	GUID getBattleID()
	{
		return m_battleID;
	}
	
	private void attackerWins(IDelegateBridge bridge)
	{
		getDisplay(bridge).battleEnd(m_battleID, m_attacker.getName() + " win");
		
		if (m_headless)
			return;
		
		// do we need to change ownership
		if (Match.someMatch(m_attackingUnits, Matches.UnitIsNotAir))
		{
			m_tracker.addToConquered(m_battleSite);
			m_tracker.takeOver(m_battleSite, m_attacker, bridge, null, m_attackingUnits);
		}
		
		// Clear the transported_by for successfully offloaded units
		Collection<Unit> transports = Match.getMatches(m_attackingUnits, Matches.UnitIsTransport);
		if (!transports.isEmpty())
		{
			CompositeChange change = new CompositeChange();
			Collection<Unit> dependents = getTransportDependents(transports, m_data);
			if (!dependents.isEmpty())
			{
				for (Unit unit : dependents)
				{
					// clear the loaded by ONLY for Combat unloads. NonCombat unloads are handled elsewhere.
					if (Matches.UnitWasUnloadedThisTurn.match(unit))
						change.add(ChangeFactory.unitPropertyChange(unit, null, TripleAUnit.TRANSPORTED_BY));
				}
				
				bridge.addChange(change);
			}
		}
		
		CompositeChange clearAlliedAir = clearTransportedByForAlliedAirOnCarrier(m_attackingUnits, m_battleSite, m_attacker, m_data);
		if (!clearAlliedAir.isEmpty())
			bridge.addChange(clearAlliedAir);
		
		bridge.getHistoryWriter()
					.addChildToEvent(m_attacker.getName() + " win", m_attackingUnits);
		showCasualties(bridge);
	}
	
	public static CompositeChange clearTransportedByForAlliedAirOnCarrier(Collection<Unit> attackingUnits, Territory battleSite, PlayerID attacker, GameData data)
	{
		CompositeChange change = new CompositeChange();
		
		// Clear the transported_by for successfully won battles where there was an allied air unit held as cargo by an carrier unit
		Collection<Unit> carriers = Match.getMatches(attackingUnits, Matches.UnitIsCarrier);
		if (!carriers.isEmpty() && games.strategy.triplea.Properties.getAlliedAirDependents(data))
		{
			Match<Unit> alliedFighters = new CompositeMatchAnd<Unit>(Matches.isUnitAllied(attacker, data), Matches.unitIsOwnedBy(attacker).invert(), Matches.UnitIsAir, Matches.UnitCanLandOnCarrier);
			Collection<Unit> alliedAirInTerr = Match.getMatches(battleSite.getUnits().getUnits(), alliedFighters);
			for (Unit fighter : alliedAirInTerr)
			{
				TripleAUnit taUnit = (TripleAUnit) fighter;
				if (taUnit.getTransportedBy() != null)
				{
					Unit carrierTransportingThisUnit = taUnit.getTransportedBy();
					if (!Matches.UnitHasWhenCombatDamagedEffect(UnitAttachment.UNITSMAYNOTLEAVEALLIEDCARRIER).match(carrierTransportingThisUnit))
						change.add(ChangeFactory.unitPropertyChange(fighter, null, TripleAUnit.TRANSPORTED_BY));
				}
			}
		}
		return change;
	}
	
	private void showCasualties(IDelegateBridge bridge)
	{
		if (m_killed.isEmpty())
			return;
		// a handy summary of all the units killed
		IntegerMap<UnitType> costs = BattleCalculator.getCostsForTUV(m_attacker, m_data);
		int tuvLostAttacker = BattleCalculator.getTUV(m_killed, m_attacker,
					costs, m_data);
		costs = BattleCalculator.getCostsForTUV(m_defender, m_data);
		int tuvLostDefender = BattleCalculator.getTUV(m_killed, m_defender,
					costs, m_data);
		int tuvChange = tuvLostDefender - tuvLostAttacker;
		bridge.getHistoryWriter().addChildToEvent(
					"Battle casualty summary: Battle score (TUV change) for attacker is "
								+ tuvChange, m_killed);
		
	}
	
	private void endBattle(IDelegateBridge bridge)
	{
		
		clearWaitingToDie(bridge);
		m_over = true;
		m_tracker.removeBattle(this);
	}
	
	public List<Unit> getRemainingAttackingUnits()
	{
		return m_attackingUnits;
	}
	
	public List<Unit> getRemainingDefendingUnits()
	{
		return m_defendingUnits;
	}
	
	@Override
	public String toString()
	{
		
		return "Battle in:" + m_battleSite + " attacked by:" + m_attackingUnits
					+ " from:" + m_attackingFrom + " defender:"
					+ m_defender.getName() + " bombing:" + isBombingRun();
	}
	
	// In an amphibious assault, sort on who is unloading from xports first
	// This will allow the marines with higher scores to get killed last
	public void sortAmphib(List<Unit> units, GameData data)
	{
		final Comparator<Unit> decreasingMovement = UnitComparator.getDecreasingMovementComparator();
		
		Comparator<Unit> comparator = new Comparator<Unit>()
		{
			@Override
			public int compare(Unit u1, Unit u2)
			{
				int amphibComp = 0;
				
				if (u1.getUnitType().equals(u2.getUnitType()))
				{
					UnitAttachment ua = UnitAttachment.get(u1.getType());
					UnitAttachment ua2 = UnitAttachment.get(u2.getType());
					if (ua.getIsMarine() && ua2.getIsMarine())
						amphibComp = compareAccordingToAmphibious(u1, u2);
					if (amphibComp == 0)
						return decreasingMovement.compare(u1, u2);
					else
						return amphibComp;
					
				}
				return u1.getUnitType().getName().compareTo(u2.getUnitType().getName());
			}
		};
		
		Collections.sort(units, comparator);
		
	}
	
	private int compareAccordingToAmphibious(Unit u1, Unit u2)
	{
		if (m_amphibiousLandAttackers.contains(u1) && !m_amphibiousLandAttackers.contains(u2))
			return -1;
		else if (m_amphibiousLandAttackers.contains(u2) && !m_amphibiousLandAttackers.contains(u1))
			return 1;
		return 0;
	}
	
	@Override
	public Collection<Unit> getAttackingUnits()
	{
		return m_attackingUnits;
	}
	
	@Override
	public Collection<Unit> getDefendingUnits()
	{
		return m_defendingUnits;
	}
	
	public Collection<Territory> getAttackingFrom()
	{
		return m_attackingFrom;
	}
	
	public Map<Territory, Collection<Unit>> getAttackingFromMap()
	{
		return m_attackingFromMap;
	}
	
	@Override
	public Collection<Unit> getAmphibiousLandAttackers()
	{
		return m_amphibiousLandAttackers;
	}
	
	@Override
	public void unitsLostInPrecedingBattle(Battle battle, Collection<Unit> units,
				IDelegateBridge bridge)
	{
		
		Collection<Unit> lost = getDependentUnits(units);
		
		// if all the amphibious attacking land units are lost, then we are
		// no longer a naval invasion
		m_amphibiousLandAttackers.removeAll(lost);
		if (m_amphibiousLandAttackers.isEmpty())
		{
			m_amphibious = false;
			m_bombardingUnits.clear();
		}
		
		m_attackingUnits.removeAll(lost);
		remove(lost, bridge, m_battleSite, false);
		
		if (m_attackingUnits.isEmpty())
			m_tracker.removeBattle(this);
	}
	
	/**
	 * Returns a map of transport -> collection of transported units.
	 */
	private Map<Unit, Collection<Unit>> transporting(Collection<Unit> units)
	{
		
		return getTransportTracker().transporting(units);
	}
	
	/**
	 * Return whether battle is amphibious.
	 */
	@Override
	public boolean isAmphibious()
	{
		return m_amphibious;
	}
	
	@Override
	public int getBattleRound()
	{
		return m_round;
	}
}


class Fire implements IExecutable
{
	// compatible with 0.9.0.2 saved games
	private static final long serialVersionUID = -3687054738070722403L;
	
	private final String m_stepName;
	private final Collection<Unit> m_firingUnits;
	private final Collection<Unit> m_attackableUnits;
	private final MustFightBattle.ReturnFire m_canReturnFire;
	
	private final String m_text;
	private final MustFightBattle m_battle;
	private final PlayerID m_firingPlayer;
	private final PlayerID m_hitPlayer;
	private final boolean m_defending;
	private final Map<Unit, Collection<Unit>> m_dependentUnits;
	private final GUID m_battleID;
	
	private DiceRoll m_dice;
	private Collection<Unit> m_killed;
	private Collection<Unit> m_damaged;
	private boolean m_confirmOwnCasualties = true;
	private final boolean m_isHeadless;
	
	public Fire(Collection<Unit> attackableUnits, MustFightBattle.ReturnFire canReturnFire, PlayerID firingPlayer, PlayerID hitPlayer,
				Collection<Unit> firingUnits, String stepName, String text, MustFightBattle battle,
				boolean defending, Map<Unit, Collection<Unit>> dependentUnits, ExecutionStack stack, boolean headless)
	{
		/* This is to remove any Factories, AAguns, and Infrastructure from possible targets for the firing. 
		 * If, in the future, Infrastructure or other things could be taken casualty, then this will need to be changed back to: 
		 * m_attackableUnits = attackableUnits;
		 */
		m_attackableUnits = Match.getMatches(attackableUnits, Matches.UnitIsDestructibleInCombatShort);
		
		m_canReturnFire = canReturnFire;
		
		m_firingUnits = firingUnits;
		m_stepName = stepName;
		m_text = text;
		m_battle = battle;
		m_hitPlayer = hitPlayer;
		m_firingPlayer = firingPlayer;
		m_defending = defending;
		m_dependentUnits = dependentUnits;
		m_isHeadless = headless;
		
		m_battleID = battle.getBattleID();
		
	}
	
	private void rollDice(IDelegateBridge bridge)
	{
		if (m_dice != null)
			throw new IllegalStateException("Already rolled");
		
		List<Unit> units = new ArrayList<Unit>(m_firingUnits);
		
		String annotation;
		if (m_isHeadless)
			annotation = "";
		else
			annotation = DiceRoll.getAnnotation(units, m_firingPlayer, m_battle);
		
		m_dice = DiceRoll.rollDice(units, m_defending,
					m_firingPlayer, bridge, m_battle, annotation);
	}
	
	private void selectCasualties(IDelegateBridge bridge)
	{
		int hitCount = m_dice.getHits();
		
		MustFightBattle.getDisplay(bridge).notifyDice(m_battle.getBattleID(), m_dice, m_stepName);
		
		int countTransports = Match.countMatches(m_attackableUnits, new CompositeMatchAnd<Unit>(Matches.UnitIsTransport, Matches.UnitIsSea));
		
		if (countTransports > 0 && isTransportCasualtiesRestricted(bridge.getData()))
		{
			CasualtyDetails message;
			Collection<Unit> nonTransports = Match.getMatches(m_attackableUnits, new CompositeMatchOr<Unit>(Matches.UnitIsNotTransportButCouldBeCombatTransport, Matches.UnitIsNotSea));
			Collection<Unit> transportsOnly = Match.getMatches(m_attackableUnits, new CompositeMatchAnd<Unit>(Matches.UnitIsTransportButNotCombatTransport, Matches.UnitIsSea));
			int numPossibleHits = MustFightBattle.getMaxHits(nonTransports);
			
			// more hits than combat units
			if (hitCount > numPossibleHits)
			{
				int extraHits = hitCount - numPossibleHits;
				Collection<Unit> remainingTargets = new ArrayList<Unit>();
				remainingTargets.addAll(m_attackableUnits);
				remainingTargets.removeAll(nonTransports);
				
				Collection<PlayerID> alliedHitPlayer = new ArrayList<PlayerID>();
				// find the players who have transports in the attackable pile
				for (Unit unit : transportsOnly)
				{
					if (!alliedHitPlayer.contains(unit.getOwner()))
						alliedHitPlayer.add(unit.getOwner());
				}
				
				Iterator<PlayerID> playerIter = alliedHitPlayer.iterator();
				// Leave enough transports for each defender for overlfows so they can select who loses them.
				while (playerIter.hasNext())
				{
					PlayerID player = playerIter.next();
					CompositeMatch<Unit> match = new CompositeMatchAnd<Unit>();
					match.add(Matches.UnitIsTransportButNotCombatTransport);
					match.add(Matches.unitIsOwnedBy(player));
					Collection<Unit> playerTransports = Match.getMatches(transportsOnly, match);
					int transportsToRemove = Math.max(0, playerTransports.size() - extraHits);
					transportsOnly.removeAll(Match.getNMatches(playerTransports, transportsToRemove, Matches.UnitIsTransportButNotCombatTransport));
				}
				
				m_killed = nonTransports;
				m_damaged = Collections.emptyList();
				// m_confirmOwnCasualties = true;
				if (extraHits > transportsOnly.size())
					extraHits = transportsOnly.size();
				
				message = BattleCalculator.selectCasualties(m_stepName, m_hitPlayer,
							transportsOnly, bridge, m_text, m_dice, !m_defending, m_battleID, m_isHeadless, extraHits);
				
				m_killed.addAll(message.getKilled());
				m_confirmOwnCasualties = true;
			}
			// exact number of combat units
			else if (hitCount == numPossibleHits)
			{
				m_killed = nonTransports;
				m_damaged = Collections.emptyList();
				m_confirmOwnCasualties = true;
			}
			// less than possible number
			else
			{
				message = BattleCalculator.selectCasualties(m_stepName, m_hitPlayer,
							nonTransports, bridge, m_text, m_dice, !m_defending, m_battleID, m_isHeadless, m_dice.getHits());
				
				m_killed = message.getKilled();
				m_damaged = message.getDamaged();
				m_confirmOwnCasualties = message.getAutoCalculated();
			}
		}
		else
		// not isTransportCasualtiesRestricted
		{
			// they all die
			if (hitCount >= MustFightBattle.getMaxHits(m_attackableUnits))
			{
				m_killed = m_attackableUnits;
				m_damaged = Collections.emptyList();
				// everything died, so we need to confirm
				m_confirmOwnCasualties = true;
			}
			// Choose casualties
			else
			{
				CasualtyDetails message;
				message = BattleCalculator.selectCasualties(m_stepName, m_hitPlayer,
							m_attackableUnits, bridge, m_text, m_dice, !m_defending, m_battleID, m_isHeadless, m_dice.getHits());
				
				m_killed = message.getKilled();
				m_damaged = message.getDamaged();
				m_confirmOwnCasualties = message.getAutoCalculated();
			}
		}
	}
	
	private void notifyCasualties(final IDelegateBridge bridge)
	{
		
		if (m_isHeadless)
			return;
		
		MustFightBattle.getDisplay(bridge).casualtyNotification(m_battleID, m_stepName, m_dice, m_hitPlayer, new ArrayList<Unit>(m_killed), new ArrayList<Unit>(m_damaged), m_dependentUnits);
		
		Runnable r = new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					MustFightBattle.getRemote(m_firingPlayer, bridge).confirmEnemyCasualties(m_battleID, "Press space to continue", m_hitPlayer);
				}
					catch (ConnectionLostException cle)
				{
					// somone else will deal with this
					cle.printStackTrace(System.out);
				}
			}
		};
		
		// execute in a seperate thread to allow either player to click continue first.
		Thread t = new Thread(r, "Click to continue waiter");
		t.start();
		
		if (m_confirmOwnCasualties)
			MustFightBattle.getRemote(m_hitPlayer, bridge).confirmOwnCasualties(m_battleID, "Press space to continue");
		
		try
		{
			bridge.leaveDelegateExecution();
			t.join();
		} catch (InterruptedException e)
		{
			// ignore
		} finally
		{
			bridge.enterDelegateExecution();
		}
	}
	
	/**
	 * We must execute in atomic steps, push these steps onto the stack, and let them execute
	 */
	@Override
	public void execute(ExecutionStack stack, IDelegateBridge bridge)
	{
		// add to the stack so we will execute,
		// we want to roll dice, select casualties, then notify in that order, so
		// push onto the stack in reverse order
		
		IExecutable rollDice = new IExecutable()
		{
			@Override
			public void execute(ExecutionStack stack, IDelegateBridge bridge)
			{
				rollDice(bridge);
			}
		};
		
		IExecutable selectCasualties = new IExecutable()
		{
			
			@Override
			public void execute(ExecutionStack stack, IDelegateBridge bridge)
			{
				selectCasualties(bridge);
			}
		};
		
		IExecutable notifyCasualties = new IExecutable()
		{
			// compatible with 0.9.0.2 saved games
			private static final long serialVersionUID = -9173385989239225660L;
			
			@Override
			public void execute(ExecutionStack stack, IDelegateBridge bridge)
			{
				notifyCasualties(bridge);
				
				if (m_damaged != null)
					m_battle.markDamaged(m_damaged, bridge);
				m_battle.removeCasualties(m_killed, m_canReturnFire, !m_defending, bridge, false);
				
			}
		};
		
		stack.push(notifyCasualties);
		stack.push(selectCasualties);
		stack.push(rollDice);
		
		return;
	}
	
	/**
	 * @return
	 */
	private boolean isTransportCasualtiesRestricted(GameData data)
	{
		return games.strategy.triplea.Properties.getTransportCasualtiesRestricted(data);
	}
	
}
