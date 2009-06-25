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
 * @version $LastChangedDate$
 */

package games.strategy.triplea.delegate;

import games.strategy.engine.GameOverException;
import games.strategy.engine.data.Change;
import games.strategy.engine.data.ChangeFactory;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
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
import games.strategy.triplea.ui.display.ITripleaDisplay;
import games.strategy.triplea.util.UnitCategory;
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
    // compatible with 0.9.0.2 saved games
    private static final long serialVersionUID = 5879502298361231540L;
    
    public static final int DEFAULT_RETREAT_TYPE = 0;
    public static final int SUBS_RETREAT_TYPE = 1;
    public static final int PLANES_RETREAT_TYPE = 2;
    public static final int PARTIAL_AMPHIB_RETREAT_TYPE = 3;

    private final Territory m_battleSite;

    //In headless mode we are just being used to calculate results
    //for an odds calculator
    //we can skip some steps for effeciency.
    //as well, in headless mode we should
    //not access Delegates
    private boolean m_headless = false;
    
    //maps Territory-> units
    //stores a collection of who is attacking from where, needed
    //for undoing moves
    private Map<Territory,Collection<Unit>> m_attackingFromMap = new HashMap<Territory,Collection<Unit>>();
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

    //dependent units
    //maps unit -> Collection of units
    //if unit is lost in a battle we are dependent on
    //then we lose the corresponding collection of units
    private Map<Unit, Collection<Unit>> m_dependentUnits = new HashMap<Unit, Collection<Unit>>();

    //keep track of all the units that die in the battle to show in the history
    // window
    private Collection<Unit> m_killed = new ArrayList<Unit>();
    
    private int m_round = 0;
    
    //our current execution state
    //we keep a stack of executables
    //this allows us to save our state 
    //and resume while in the middle of a battle 
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
        return m_data.getProperties().get(Constants.SUBMERSIBLE_SUBS, false);
    }

    public boolean isOver()
    {
        return m_over;
    }
    
    public void removeAttack(Route route, Collection<Unit> units)
    {
        m_attackingUnits.removeAll(units);
        
        //the route could be null, in the case of a unit in a territory where a sub is submerged.
        if(route == null)
            return;
        Territory attackingFrom = getAttackFrom(route);

        Collection<Unit> attackingFromMapUnits = m_attackingFromMap
                .get(attackingFrom);
        //handle possible null pointer
        if(attackingFromMapUnits == null)
        {
            attackingFromMapUnits = new ArrayList<Unit>();
        }
        attackingFromMapUnits.removeAll(units);
        if (attackingFromMapUnits.isEmpty())
        {
            m_attackingFrom.remove(attackingFrom);
        }

        //deal with amphibious assaults
        if (attackingFrom.isWater())
        {
            //if none of the units is a land unit, the attack from
            //that territory is no longer an amphibious assault
            if (Match.noneMatch(attackingFromMapUnits, Matches.UnitIsLand))
            {
                m_amphibiousAttackFrom.remove(attackingFrom);
                //do we have any amphibious attacks left?
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

    public boolean isEmpty()
    {

        return m_attackingUnits.isEmpty() && m_attackingWaitingToDie.isEmpty();
    }

    public Change addAttackChange(Route route, Collection<Unit> units)
    {
    	CompositeChange change = new CompositeChange();
     
    	// Filter out allied units if fourth edition
        Match<Unit> ownedBy = Matches.unitIsOwnedBy(m_attacker);
        Collection<Unit> attackingUnits = isFourthEdition() ? Match.getMatches(units,
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

        //are we amphibious
        if (route.getStart().isWater() && route.getEnd() != null
                && !route.getEnd().isWater()
                && Match.someMatch(attackingUnits, Matches.UnitIsLand))
        {
            m_amphibiousAttackFrom.add(getAttackFrom(route));
            m_amphibiousLandAttackers.addAll(Match.getMatches(attackingUnits,
                    Matches.UnitIsLand));
            m_amphibious = true;
        }
        
        // transports
        Map<Unit, Collection<Unit>> dependencies = transporting(units);
        // If fourth edition, allied air on our carriers are also dependents
        if (isFourthEdition() || isAlliedAirDependents())
        {
            dependencies.putAll(MoveValidator.carrierMustMoveWith(units, units, m_data, m_attacker));
        }
        
        //Set the dependent paratroopers so they die if the bomber dies.        
        if(isParatroopers(m_attacker))
        {
            Collection<Unit> bombers = Match.getMatches(units, Matches.UnitIsStrategicBomber);
            Collection<Unit> paratroops = Match.getMatches(units, Matches.UnitIsParatroop);
            if(!bombers.isEmpty() && !paratroops.isEmpty())
            {
            	//Load capable bombers by default>
            	Map<Unit,Unit> unitsToCapableBombers = MoveDelegate.mapTransports(route, paratroops, bombers, true, m_attacker);

            	HashMap<Unit, Collection<Unit>> dependentUnits = new HashMap<Unit, Collection<Unit>>();
            	Collection<Unit> singleCollection = new ArrayList<Unit>();
            	for (Unit unit : unitsToCapableBombers.keySet())
            	{
                    Collection<Unit> unitList = new ArrayList<Unit>();
                    unitList.add(unit);
            		Unit bomber = unitsToCapableBombers.get(unit);                
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

            	UnitSeperator.categorize(bombers, dependentUnits, false);
            }
        }
        
        addDependentUnits(dependencies);

        //mark units with no movement
        //for all but air
        Collection<Unit> nonAir = Match.getMatches(attackingUnits,
                Matches.UnitIsNotAir);
        
        //we dont want to change the movement of transported land units if this is a sea battle
        //so restrict non air to remove land units
        if(m_battleSite.isWater())
            nonAir = Match.getMatches(nonAir, Matches.UnitIsNotLand);
        
        //TODO This checks for ignored sub/trns and skips the set of the attackers to 0 movement left
        //If attacker stops in an occupied territory, movement stops (battle is optional) 
        if(MoveValidator.onlyIgnoredUnitsOnPath(route, m_attacker, m_data, false))
            return change;
        

        change.add(DelegateFinder.moveDelegate(m_data).markNoMovementChange(nonAir));
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
                m_dependentUnits.put(holder, transporting);
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
        //if water find the defender based on who has the most units in the
        // territory
        IntegerMap<PlayerID> players = battleSite.getUnits().getPlayerUnitCounts();
        int max = -1;
        PlayerID defender = null;
        Iterator<PlayerID> iter = players.keySet().iterator();
        while (iter.hasNext())
        {
            PlayerID current = iter.next();
            if (m_data.getAllianceTracker().isAllied(m_attacker, current)
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
            //this is ok, we are a headless battle
        }
            

        return defender;
    }

    public boolean isBombingRun()
    {

        return false;
    }
    
    public Territory getTerritory()
    {

        return m_battleSite;
    }

    public int hashCode()
    {

        return m_battleSite.hashCode();
    }

    public boolean equals(Object o)
    {

        //2 battles are equal if they are both the same type (boming or not)
        //and occur on the same territory
        //equals in the sense that they should never occupy the same Set
        //if these conditions are met
        if (o == null || !(o instanceof Battle))
            return false;

        Battle other = (Battle) o;
        return other.getTerritory().equals(this.m_battleSite)
                && other.isBombingRun() == this.isBombingRun();
    }

    public void fight(IDelegateBridge bridge)
    {
        //we have already started
        if(m_stack.isExecuting())
        {
            ITripleaDisplay display = getDisplay(bridge);
            display.showBattle(m_battleID, m_battleSite, getBattleTitle(), removeNonCombatants(m_attackingUnits), removeNonCombatants(m_defendingUnits), m_dependentUnits, m_attacker, m_defender);

            display.listBattleSteps(m_battleID, m_stepStrings);
            
            m_stack.execute(bridge, m_data);
            return;
        }
        
        
        bridge.getHistoryWriter().startEvent("Battle in " + m_battleSite);
        bridge.getHistoryWriter().setRenderingData(m_battleSite);
        removeAirNoLongerInTerritory();
        
        writeUnitsToHistory(bridge);
        
        //it is possible that no attacking units are present, if so
        //end now
        if (m_attackingUnits.size() == 0)
        {
            endBattle(bridge);
            defenderWins(bridge);
            return;
        }

        //it is possible that no defending units exist
        if (m_defendingUnits.size() == 0)
        {
            endBattle(bridge);
            attackerWins(bridge);
            return;
        }

        // Add dependent defending units to dependent unit map
        Map<Unit, Collection<Unit>> defender_dependencies = transporting(m_defendingUnits);
        addDependentUnits(defender_dependencies);

        //list the steps
        m_stepStrings = determineStepStrings(true, bridge);

        ITripleaDisplay display = getDisplay(bridge);
        display.showBattle(m_battleID, m_battleSite, getBattleTitle(), removeNonCombatants(m_attackingUnits), removeNonCombatants(m_defendingUnits), m_dependentUnits, m_attacker, m_defender);

        display.listBattleSteps(m_battleID, m_stepStrings);

        if(!m_headless)
        {
            //take the casualties with least movement first
            if(isAmphibious())
                sortAmphib(m_attackingUnits, m_data);
            else 
                BattleCalculator.sortPreBattle(m_attackingUnits, m_data);
            BattleCalculator.sortPreBattle(m_defendingUnits, m_data);
        }
        
        //push on stack in opposite order of execution
        pushFightLoopOnStack(bridge);
        pushFightStartOnStack();
        m_stack.execute(bridge, m_data);
    }

    private void writeUnitsToHistory(IDelegateBridge bridge)
    {
        Set<PlayerID> playerSet = m_battleSite.getUnits().getPlayersWithUnits();

        String transcriptText;

        // find all attacking players (unsorted)
        Collection<PlayerID> attackers = new ArrayList<PlayerID>();
        Collection<Unit> allAttackingUnits = new ArrayList<Unit>();
        transcriptText = "";
        for (PlayerID current : playerSet )
        {
            if (m_data.getAllianceTracker().isAllied(m_attacker, current)
                    || current.equals(m_attacker))
                attackers.add(current);
        }

        // find all attacking units (unsorted)        
        for(Iterator attackersIter = attackers.iterator(); attackersIter.hasNext(); )
        {
            PlayerID current = (PlayerID)attackersIter.next();
            String delim;
            if(attackersIter.hasNext())
                delim = "; ";
            else
                delim = "";
            Collection<Unit> attackingUnits = Match.getMatches(m_attackingUnits, Matches.unitIsOwnedBy(current));
            
            String verb = current.equals(m_attacker) ? "attack" : "loiter and taunt";
            transcriptText += current.getName()+" "+verb+" with "
                           +MyFormatter.unitsToTextNoOwner(attackingUnits)
                           +delim;
            allAttackingUnits.addAll(attackingUnits);
            
            //If any attacking transports are in the battle, set their status to later restrict load/unload
            if (current.equals(m_attacker))
            {   
            	CompositeChange change = new CompositeChange();
            	Collection<Unit> transports = Match.getMatches(attackingUnits, Matches.UnitCanTransport);
            	Iterator <Unit> attackTranIter = transports.iterator();
                        
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
        for (PlayerID current : playerSet )
        {
            if (m_data.getAllianceTracker().isAllied(m_defender, current)
                    || current.equals(m_defender))
            {
                defenders.add(current);
            }
        }

        // find all defending units (unsorted)
        for(Iterator defendersIter = defenders.iterator(); defendersIter.hasNext(); )
        {
            PlayerID current = (PlayerID)defendersIter.next();
            Collection<Unit> defendingUnits;
            String delim;
            if(defendersIter.hasNext())
                delim = "; ";
            else
                delim = "";
            defendingUnits = Match.getMatches(m_defendingUnits, Matches.unitIsOwnedBy(current));
            
            transcriptText += current.getName()+" defend with "
                           +MyFormatter.unitsToTextNoOwner(defendingUnits)
                           +delim;
            allDefendingUnits.addAll(defendingUnits);
        }
        // write defending units to history
        if (m_defendingUnits.size() > 0)
            bridge.getHistoryWriter().addChildToEvent(transcriptText, allDefendingUnits);
    }

    private void removeAirNoLongerInTerritory()
    {
        if(m_headless)
            return;

        //remove any air units that were once in this attack, but have now
        // moved out of the territory
        //this is an ilegant way to handle this bug
        CompositeMatch<Unit> airNotInTerritory = new CompositeMatchAnd<Unit>();
        airNotInTerritory.add(new InverseMatch<Unit>(Matches
                .unitIsInTerritory(m_battleSite)));

        m_attackingUnits.removeAll(Match.getMatches(m_attackingUnits,
                airNotInTerritory));

    }

    public List<String> determineStepStrings(boolean showFirstRun, IDelegateBridge bridge)
    {
        boolean isEditMode = EditDelegate.getEditMode(m_data);
        boolean attackingSubsAlreadyFired = false;
        boolean defendingSubsAlreadyFired = false;
        List<String> steps = new ArrayList<String>();
        if (!isEditMode && showFirstRun)
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
            
            if(!m_battleSite.isWater() && isParatroopers(m_attacker))
            {
            	Collection<Unit> bombers = Match.getMatches(m_battleSite.getUnits().getUnits(), Matches.UnitIsStrategicBomber);

            	if(!bombers.isEmpty())
            	{
            		Collection<Unit> dependents = getDependentUnits(bombers);
            		if(!dependents.isEmpty())
            		{
            			steps.add(LAND_PARATROOPS);
            		}
            	}
            }
        }

        //Check if defending subs can submerge before battle
        if (isSubRetreatBeforeBattle())
        {
        	if(!Match.someMatch(m_attackingUnits, Matches.UnitIsDestroyer) && Match.someMatch(m_defendingUnits, Matches.UnitIsSub))
        		steps.add(m_defender.getName() + SUBS_SUBMERGE);
        }

        //Check if attack subs can submerge before battle
        if (isSubRetreatBeforeBattle())
        {
        	if(!Match.someMatch(m_defendingUnits, Matches.UnitIsDestroyer) && Match.someMatch(m_attackingUnits, Matches.UnitIsSub))
        		steps.add(m_attacker.getName() + SUBS_SUBMERGE);
        }
        
        // See if there any unescorted trns
        if (!isEditMode && m_battleSite.isWater() && isTransportCasualtiesRestricted())
        { 
            if(Match.someMatch(m_attackingUnits, Matches.UnitIsTransport) || Match.someMatch(m_defendingUnits, Matches.UnitIsTransport))
                steps.add(REMOVE_UNESCORTED_TRANSPORTS);
        }        

        // Air only Units can't attack subs without Destroyers present
        if (!isEditMode && isAirAttackSubRestricted()) 
        { 
            Collection<Unit> units = new ArrayList<Unit>(m_attackingUnits.size() + m_attackingWaitingToDie.size());
            units.addAll(m_attackingUnits);
            units.addAll(m_attackingWaitingToDie);
            
        	//if(!Match.someMatch(m_attackingUnits, Matches.UnitIsDestroyer) && Match.allMatch(m_attackingUnits, Matches.UnitIsAir))
            if(Match.someMatch(m_attackingUnits, Matches.UnitIsAir) && !canAirAttackSubs(m_defendingUnits, units))
                steps.add(SUBMERGE_SUBS_VS_AIR_ONLY);
        }

        //attacker subs sneak attack
        //Attacking subs have no sneak attack if Destroyers are present
        if (!isEditMode && m_battleSite.isWater()) 
        {
            if (Match.someMatch(m_attackingUnits, Matches.UnitIsSub))
            {
                if(Match.someMatch(m_defendingUnits, Matches.UnitIsDestroyer))
                {
                    steps.add(m_attacker.getName() + SUBS_FIRE);
                    steps.add(m_defender.getName() + SELECT_SUB_CASUALTIES);
                    attackingSubsAlreadyFired = true;
                }
                else
                {
                    steps.add(m_attacker.getName() + SUBS_SNEAK_ATTACK);
                    steps.add(m_defender.getName() + SELECT_SNEAK_ATTACK_CASUALTIES);
                    steps.add(REMOVE_SNEAK_ATTACK_CASUALTIES);
                    attackingSubsAlreadyFired = true;
                }
            }
        }
        
        //defender subs sneak attack        
        //Defending subs have no sneak attack in Pacific/Europe Editions or if Destroyers are present
        if (!isEditMode && m_battleSite.isWater() && (isFourthEdition() || isDefendingSubsSneakAttack()))
        {
            if (Match.someMatch(m_defendingUnits, Matches.UnitIsSub))
            {
                if(Match.someMatch(m_attackingUnits, Matches.UnitIsDestroyer))
                {
                    steps.add(m_defender.getName() + SUBS_FIRE);
                    steps.add(m_attacker.getName() + SELECT_SUB_CASUALTIES);
                    defendingSubsAlreadyFired = true;
                }
                else
                {
                    steps.add(m_defender.getName() + SUBS_SNEAK_ATTACK);
                    steps.add(m_attacker.getName() + SELECT_SNEAK_ATTACK_CASUALTIES);
                    steps.add(REMOVE_SNEAK_ATTACK_CASUALTIES);
                    defendingSubsAlreadyFired = true;
                }
            }
        }

        // Air Units can't attack subs without Destroyers present
        if (!isEditMode && m_battleSite.isWater() && isAirAttackSubRestricted())
        {  
            Collection<Unit> units = new ArrayList<Unit>(m_attackingUnits.size() + m_attackingWaitingToDie.size());
            units.addAll(m_attackingUnits);
            units.addAll(m_attackingWaitingToDie);
            
            //if(!Match.someMatch(m_attackingUnits, Matches.UnitIsDestroyer) && Match.someMatch(m_attackingUnits, Matches.UnitIsAir) && Match.someMatch(m_defendingUnits, Matches.UnitIsSub))
            if(Match.someMatch(m_attackingUnits, Matches.UnitIsAir) && !canAirAttackSubs(m_defendingUnits, units))
                steps.add(AIR_ATTACK_NON_SUBS);
        }

        //attacker fire        
        if (!attackingSubsAlreadyFired && m_battleSite.isWater() && Match.someMatch(m_attackingUnits, Matches.UnitIsSub))
        {
        	steps.add(m_attacker.getName() + SUBS_FIRE);
        	steps.add(m_defender.getName() + SELECT_SUB_CASUALTIES);
        }
        
        if (Match.someMatch(m_attackingUnits, Matches.UnitIsNotSub))
        {
            if (!isEditMode)
                steps.add(m_attacker.getName() + FIRE);
            steps.add(m_defender.getName() + SELECT_CASUALTIES);
        }

        //defender fire
        //defender subs, note this happens earlier for fourth edition
        if (!defendingSubsAlreadyFired && m_battleSite.isWater() && Match.someMatch(m_defendingUnits, Matches.UnitIsSub))
        {
        	steps.add(m_defender.getName() + SUBS_FIRE);
        	steps.add(m_attacker.getName() + SELECT_SUB_CASUALTIES);
        }

        // Air Units can't attack subs without Destroyers present
        if (!isEditMode && m_battleSite.isWater() && isAirAttackSubRestricted())
        {            
            Collection<Unit> units = new ArrayList<Unit>(m_defendingUnits.size() + m_defendingWaitingToDie.size());
            units.addAll(m_defendingUnits);
            units.addAll(m_defendingWaitingToDie);
        
            //if(!Match.someMatch(m_defendingUnits, Matches.UnitIsDestroyer) && Match.someMatch(m_defendingUnits, Matches.UnitIsAir) && Match.someMatch(m_attackingUnits, Matches.UnitIsSub))
            if(Match.someMatch(m_defendingUnits, Matches.UnitIsAir) && !canAirAttackSubs(m_attackingUnits, units))       
                steps.add(AIR_DEFEND_NON_SUBS);
        }

        if (Match.someMatch(m_defendingUnits, Matches.UnitIsNotSub))
        {
            if (!isEditMode)
                steps.add(m_defender.getName() + FIRE);
            steps.add(m_attacker.getName() + SELECT_CASUALTIES);
        }

        //remove casualties
        steps.add(REMOVE_CASUALTIES);

        //retreat subs
        if (m_battleSite.isWater())
        {
            if (canSubsSubmerge())
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

            } else
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

        //if we are a sea zone, then we may not be able to retreat
        //(ie a sub travelled under another unit to get to the battle site)
        //or an enemy sub retreated to our sea zone
        //however, if all our sea units die, then
        //the air units can still retreat, so if we have any air units attacking in
        //a sea zone, we always have to have the retreat
        //option shown
        //later, if our sea units die, we may ask the user to retreat
        boolean someAirAtSea = m_battleSite.isWater() && Match.someMatch(m_attackingUnits, Matches.UnitIsAir);
        
        if (canAttackerRetreat() || someAirAtSea)
        {
            steps.add(m_attacker.getName() + ATTACKER_WITHDRAW);
        } else if (canAttackerRetreatPartialAmphib())
        {
            steps.add(m_attacker.getName() + NONAMPHIB_WITHDRAW);
        } else if (canAttackerRetreatPlanes() & !canAttackerRetreatPartialAmphib())
        {
            steps.add(m_attacker.getName() + PLANES_WITHDRAW);
        }

        return steps;

    }

    private void pushFightStartOnStack()
    {
        IExecutable fireAAGuns = new IExecutable()
        {

            public void execute(ExecutionStack stack, IDelegateBridge bridge, GameData data)
            {
                fireAAGuns(bridge);
            }
            
        };

        IExecutable fireNavalBombardment = new IExecutable()
        {

            public void execute(ExecutionStack stack, IDelegateBridge bridge, GameData data)
            {
                fireNavalBombardment(bridge);
            }
            
        };        
        
        IExecutable removeNonCombatants = new IExecutable()
        {

            public void execute(ExecutionStack stack, IDelegateBridge bridge, GameData data)
            {
                removeNonCombatants();
            }
            
        }; 
        
        IExecutable landParatroops = new IExecutable()
        {

            public void execute(ExecutionStack stack, IDelegateBridge bridge, GameData data)
            {
            	landParatroops(bridge);
            }
        }; 
        //push in opposite order of execution
        m_stack.push(landParatroops);
        m_stack.push(removeNonCombatants);
        m_stack.push(fireNavalBombardment);
        m_stack.push(fireAAGuns);
    }

    private void pushFightLoopOnStack(IDelegateBridge bridge)
    {
        if (m_over)
            return;

        boolean isEditMode = EditDelegate.getEditMode(m_data);

        //the code here is a bit odd to read
        //basically, we need to break the code into seperate atomic pieces.
        //If there is a network error, or some other unfortunate event,
        //then we need to keep track of what pieces we have executed, and what is left 
        //to do
        //each atomic step is in its own IExecutable
        //the definition of atomic is that either
        //1) the code does not call to an IDisplay,IPlayer, or IRandomSource
        //2) if the code calls to an IDisplay, IPlayer, IRandomSource, and an exception is
        //called from one of those methods, the exception will be propogated out of execute(),
        //and the execute method can be called again
        //it is allowed for an iexecutable to add other iexecutables to the stack
        //
        //if you read the code in linear order, ignore wrapping stuff in annonymous iexecutables, then the code
        //can be read as it will execute
        
        

        
        //store the steps in a list
        //we need to push them in reverse order that we
        //create them, and its easier to track if we just add them 
        //to a list while creating. then reverse the list and add
        //to the stack at the end
        List<IExecutable> steps = new ArrayList<IExecutable>();
        
        
        //for 4th edition we need to fire the defending subs before the
        //attacking subs fire
        //this allows the dead subs to return fire, even if they are selected
        // as casualties
        final List<Unit> defendingSubs = Match.getMatches(m_defendingUnits,
                Matches.UnitIsSub);
        
        /**Ask to retreat defending subs before battle*/
        if (!isEditMode && isSubRetreatBeforeBattle())
            steps.add(new IExecutable(){
                // compatible with 0.9.0.2 saved games
                private static final long serialVersionUID = 7056448091800764539L;
                public void execute(ExecutionStack stack, IDelegateBridge bridge, GameData data)
                {
                    if(!m_over)
                        defenderRetreatSubs(bridge);
                }
            });  

        /**Ask to retreat attacking subs before battle*/
        if (!isEditMode && isSubRetreatBeforeBattle())
            steps.add(new IExecutable(){
                // compatible with 0.9.0.2 saved games
                private static final long serialVersionUID = 6775880082912594489L;
                public void execute(ExecutionStack stack, IDelegateBridge bridge, GameData data)
                {
                    if(!m_over)
                        attackerRetreatSubs(bridge);
                }
            });  

        /**Remove undefended trns*/
        if (!isEditMode && isTransportCasualtiesRestricted())
        	steps.add(new IExecutable(){
        		private static final long serialVersionUID = 99989L;
                
        		 public void execute(ExecutionStack stack, IDelegateBridge bridge, GameData data)
                 {
        			 checkUndefendedTransports(bridge, m_defender);
        			 checkUndefendedTransports(bridge, m_attacker);        			
                 }
        	});

        /**Submerge subs if -vs air only & air restricted from attacking subs */
        if (!isEditMode && isAirAttackSubRestricted())
        	steps.add(new IExecutable(){
        		private static final long serialVersionUID = 99990L;                
        		 public void execute(ExecutionStack stack, IDelegateBridge bridge, GameData data)
                 {
        		     submergeSubsVsOnlyAir(bridge);     			
                 }
        	});
                
        /**Attacker subs fire */
        if (!isEditMode)
            steps.add(new IExecutable(){
                private static final long serialVersionUID = 99991L;
                public void execute(ExecutionStack stack, IDelegateBridge bridge, GameData data)
                {
                    attackSubs(bridge);
                }                
            });
        
        /**Defending subs fire if 4th Edition or Defending subs sneak attack*/
        if (!isEditMode && (isFourthEdition() || isDefendingSubsSneakAttack()))
            steps.add(new IExecutable(){
                // compatible with 0.9.0.2 saved games
                private static final long serialVersionUID = 99992L;
                public void execute(ExecutionStack stack, IDelegateBridge bridge, GameData data)
                {
                    defendSubs(bridge);
                }                
            });

        /**Attacker air fire on NON subs */
        if (!isEditMode && isAirAttackSubRestricted())
            steps.add(new IExecutable(){
                // compatible with 0.9.0.2 saved games
                private static final long serialVersionUID = 99993L;
                public void execute(ExecutionStack stack, IDelegateBridge bridge, GameData data)
                {                		
                    attackAirOnNonSubs(bridge);
                }

            });

        /**Attacker fire remaining units */
        if (!isEditMode)
            steps.add(new IExecutable(){
                // compatible with 0.9.0.2 saved games
                private static final long serialVersionUID = 99994L;
                public void execute(ExecutionStack stack, IDelegateBridge bridge, GameData data)
                {
                    attackNonSubs(bridge);
                }

            });

        /**Defender fire subs if they haven't already fired */
        if (!isEditMode)
            steps.add(new IExecutable(){
                // compatible with 0.9.0.2 saved games
                private static final long serialVersionUID = -4316269766293144179L;
                public void execute(ExecutionStack stack, IDelegateBridge bridge, GameData data)
                {
                    if (!isFourthEdition() && !isDefendingSubsSneakAttack())
                    {
                        defendSubs(bridge);
                    }
                }
            });        

        /**Defender air fire on NON subs */
        if (!isEditMode && isAirAttackSubRestricted())
            steps.add(new IExecutable(){
                // compatible with 0.9.0.2 saved games
                private static final long serialVersionUID = 1560702114917865123L;
                public void execute(ExecutionStack stack, IDelegateBridge bridge, GameData data)
                {
                    defendAirOnNonSubs(bridge);
                }

            });

        if (!isEditMode)
            steps.add(new IExecutable(){
                // compatible with 0.9.0.2 saved games
                private static final long serialVersionUID = 1560702114917865290L;
                public void execute(ExecutionStack stack, IDelegateBridge bridge, GameData data)
                {
                    defendNonSubs(bridge);
                }

            });

        if (isEditMode)
            steps.add(new IExecutable(){
                public void execute(ExecutionStack stack, IDelegateBridge bridge, GameData data)
                {
                    attackAny(bridge);
                }

            });        

        if (isEditMode)
            steps.add(new IExecutable(){
                public void execute(ExecutionStack stack, IDelegateBridge bridge, GameData data)
                {
                    defendAny(bridge);
                }
            });        
        
        //we must grab these here, when we clear waiting to die, we might remove
        //all the opposing destroyers, and this would change the canRetreatSubs rVal
        final boolean canAttackerRetreatSubs = canAttackerRetreatSubs();
        final boolean canDefenderRetreatSubs = canDefenderRetreatSubs();
        

        steps.add(new IExecutable(){
            // compatible with 0.9.0.2 saved games
            private static final long serialVersionUID = 8611067962952500496L;

            public void execute(ExecutionStack stack, IDelegateBridge bridge, GameData data)
            {
               clearWaitingToDie(bridge);
            }            
        });        

        
        steps.add(new IExecutable(){
            // compatible with 0.9.0.2 saved games
            private static final long serialVersionUID = 5259103822937067667L;
            public void execute(ExecutionStack stack, IDelegateBridge bridge, GameData data)
            {            	
                if (m_attackingUnits.size() == 0)
                {
                	if(isTransportCasualtiesRestricted())
                	{ 
                    	//Get all allied transports in the territory
                        CompositeMatch<Unit> matchAllied = new CompositeMatchAnd<Unit>(); 
                        matchAllied.add(Matches.UnitIsTransport);
                        matchAllied.add(Matches.isUnitAllied(m_attacker, m_data));
                    	
                        List<Unit> alliedTransports = Match.getMatches(m_battleSite.getUnits().getUnits(), matchAllied);                    	
                    	
                    	//If no transports, just end the battle
                        if (alliedTransports.isEmpty())
                    	{
    	                	endBattle(bridge);
    	                	defenderWins(bridge);
                    	}
                        else
                        {
                        	//TODO Need to determine how combined forces on attack work- trn left in terr by prev player, ally moves in and attacks
                        	//add back in the non-combat units (Trns)
                        	m_attackingUnits = Match.getMatches(m_battleSite.getUnits().getUnits(), Matches.unitIsOwnedBy(m_attacker));
                        }
                	}
                	else
                	{
	                	endBattle(bridge);	                	
	                	defenderWins(bridge);
                	}
                } else if (m_defendingUnits.size() == 0)
                {
                	if(isTransportCasualtiesRestricted())
                	{
                    	//If there are undefended attacking transports, determine if they automatically die
                        checkUndefendedTransports(bridge, m_defender);
                	}
                	
                		endBattle(bridge);
                		attackerWins(bridge);
                } else if (Match.allMatch(m_attackingUnits, Matches.UnitIsTransport) && Match.allMatch(m_defendingUnits, Matches.UnitIsTransport))
                {
                    Match<Unit> attackerMatch = Matches.unitCanAttack(m_attacker);
                    Match<Unit> defenderMatch = Matches.unitCanAttack(m_defender);
                    
                    if(Match.noneMatch(m_attackingUnits, attackerMatch) && Match.noneMatch(m_defendingUnits, defenderMatch))
                    {
                        endBattle(bridge);
                        nobodyWins(bridge);
                    }
                }
                
            }
            
        });  
        
        
        steps.add(new IExecutable(){
            // compatible with 0.9.0.2 saved games
            private static final long serialVersionUID = 6775880082912594489L;
            public void execute(ExecutionStack stack, IDelegateBridge bridge, GameData data)
            {                
                if(!m_over && canAttackerRetreatSubs && !isSubRetreatBeforeBattle())
                    attackerRetreatSubs(bridge);
            }
        });  

        
        steps.add(new IExecutable(){
            // compatible with 0.9.0.2 saved games
            private static final long serialVersionUID = -1544916305666912480L;
            public void execute(ExecutionStack stack, IDelegateBridge bridge, GameData data)
            {
                if(!m_over &&  canDefenderRetreatSubs && !isSubRetreatBeforeBattle())
                    defenderRetreatSubs(bridge);
            }
        });         

        steps.add(new IExecutable(){
            // compatible with 0.9.0.2 saved games
            private static final long serialVersionUID = -1150863964807721395L;
            public void execute(ExecutionStack stack, IDelegateBridge bridge, GameData data)
            {
                if (canAttackerRetreatPlanes() && !m_over)
                    attackerRetreatPlanes(bridge);                
            }
        });    

        steps.add(new IExecutable(){
            // compatible with 0.9.0.2 saved games
            private static final long serialVersionUID = -1150863964807721395L;
            public void execute(ExecutionStack stack, IDelegateBridge bridge, GameData data)
            {
                if (canAttackerRetreatPartialAmphib() && !m_over)
                    attackerRetreatNonAmphibUnits(bridge);                
            }
        });    
        
        steps.add(new IExecutable(){
            // compatible with 0.9.0.2 saved games
            private static final long serialVersionUID = 669349383898975048L;            
            public void execute(ExecutionStack stack, IDelegateBridge bridge, GameData data)
            {
                attackerRetreat(bridge);                
            }
        });    
        
        
        final IExecutable loop = new IExecutable()
        {
            // compatible with 0.9.0.2 saved games
            private static final long serialVersionUID = 3118458517320468680L;            
            public void execute(ExecutionStack stack, IDelegateBridge bridge, GameData data)
            {
                pushFightLoopOnStack(bridge);
            }
        };
        
        steps.add(new IExecutable() {
            // compatible with 0.9.0.2 saved games
            private static final long serialVersionUID = -3993599528368570254L;            
            public void execute(ExecutionStack stack, IDelegateBridge bridge, GameData data)
            {
                if (!m_over)
                {
                    m_stepStrings = determineStepStrings(false, bridge);
                    ITripleaDisplay display = getDisplay(bridge);
                    display.listBattleSteps(m_battleID, m_stepStrings);
                    m_round++;

                    //continue fighting
                    //the recursive step
                    //this should always be the base of the stack
                    //when we execute the loop, it will populate the stack with the battle steps
                    if(!m_stack.isEmpty())
                        throw new IllegalStateException("Stack not empty:" + m_stack);
                    m_stack.push(loop);                   
                }
                
            }
        });   
        
        //add in the reverse order we create them
        Collections.reverse(steps);
        for (IExecutable step : steps)
        {
            m_stack.push(step);
        }


        return;
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
    	return (isFourthEdition() || isAttackerRetreatPlanes() || isPartialAmphibiousRetreat()) && m_amphibious
                && Match.someMatch(m_attackingUnits, Matches.UnitIsAir);
    }

    /**
     * @return
     */
    private boolean canAttackerRetreatPartialAmphib()
    {
        if(m_amphibious && isPartialAmphibiousRetreat())
        {
        	if (Match.someMatch(m_attackingUnits, Matches.UnitIsAir))
        	{
        		return true;
        	}

            for(Unit unit:m_attackingUnits)
            {
                TripleAUnit taUnit = (TripleAUnit) unit;
                if(!taUnit.getWasAmphibious())
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

        //its possible that a sub retreated to a territory we came from,
        //if so we can no longer retreat there
        Collection<Territory> possible = Match.getMatches(m_attackingFrom, Matches
                .territoryHasNoEnemyUnits(m_attacker, m_data));

        // In 4th edition we need to filter out territories where only planes
        // came from since planes cannot define retreat paths
        if (isFourthEdition())
        {
            possible = Match.getMatches(possible, new Match<Territory>()
            {
                public boolean match(Territory t)
                {
                    Collection<Unit> units = m_attackingFromMap.get(t);
                    return !Match.allMatch(units, Matches.UnitIsAir);
                }
            });
        }
        else 
        {         
            //the air unit may have come from a conquered or enemy territory, don't allow retreating
            Match<Territory> conqueuredOrEnemy = new CompositeMatchOr<Territory>(
                Matches.isTerritoryEnemy(m_attacker, m_data),
                new CompositeMatchAnd<Territory>(
                    Matches.TerritoryIsLand,
                    Matches.territoryWasFoughOver(m_tracker)
                    )
                );
            possible.removeAll(Match.getMatches(possible,conqueuredOrEnemy));
        }
        
        //the battle site is in the attacking from
        //if sea units are fighting a submerged sub
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
        //if (m_amphibious && !isPartialAmphibiousRetreat())
        if (m_amphibious)
            return false;

        Collection<Territory> options = getAttackerRetreatTerritories();

        if (options.size() == 0)
            return false;

        return true;
    }

    private boolean canAttackerRetreatSubs()
    {
        if (Match.someMatch(m_defendingUnits, Matches.UnitIsDestroyer))
            return false;
        
        if (Match.someMatch(m_defendingWaitingToDie, Matches.UnitIsDestroyer))
            return false;
        
        return canAttackerRetreat() || canSubsSubmerge();
    }
    
    //Added for test case calls
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
            if(m_amphibious)
                queryRetreat(false, PARTIAL_AMPHIB_RETREAT_TYPE, bridge, possible);
            else
                queryRetreat(false, DEFAULT_RETREAT_TYPE, bridge, possible);
        }
    }

    private void attackerRetreatPlanes(IDelegateBridge bridge)
    {
        //planes retreat to the same square the battle is in, and then should
        //move during non combat to their landing site, or be scrapped if they
        //can't find one.
        Collection<Territory> possible = new ArrayList<Territory>(2);
        possible.add(m_battleSite);

        //retreat planes
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
        if(m_headless)
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

        //make sure we can move through the any canals
        Match<Territory> canalMatch = new Match<Territory>()
        {
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
        } else if (planes)
        {
            units = Match.getMatches(units, Matches.UnitIsAir);
        } else if (partialAmphib)
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
            text = retreatingPlayer.getName() + " retreat planes?";
        else if (partialAmphib)
            text = retreatingPlayer.getName() + " retreat non-amphibious units?";
        else
            text = retreatingPlayer.getName() + " retreat?";
        String step;
        if (defender)
        {
            step = m_defender.getName()
                    + (canSubsSubmerge() ? SUBS_SUBMERGE : SUBS_WITHDRAW);
        } else
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
        
        if(retreatTo != null && !availableTerritories.contains(retreatTo) && !subs)
        {
            System.err.println("Invalid retreat selection :" + retreatTo + " not in " + MyFormatter.territoriesToText(availableTerritories));
            Thread.dumpStack();
            return;
        }
        
        if (retreatTo != null)
        {
            //if attacker retreating non subs then its all over
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
            } else if (planes)
            {
                retreatPlanes(units, defender, bridge);
                String messageShort = retreatingPlayer.getName()
                        + " retreats planes";
                getDisplay(bridge).notifyRetreat(messageShort, messageShort, step, retreatingPlayer);
            } else if(partialAmphib)
            {
            	//remove amphib units from those retreating
            	units = Match.getMatches(units, Matches.UnitWasNotAmphibious);
                retreatUnitsAndPlanes(units, retreatTo, defender, bridge);
                String messageShort = retreatingPlayer.getName()
                        + " retreats non-amphibious units";
                getDisplay(bridge).notifyRetreat(messageShort, messageShort, step, retreatingPlayer);
            } else
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
    
	//Retreat landed units from allied territory when their transport retreats
    private Change retreatFromNonCombat(Collection<Unit> units, IDelegateBridge bridge, Territory retreatTo)
    {
        CompositeChange change = new CompositeChange();

    	units = Match.getMatches(units, Matches.UnitIsTransport);
    	Collection<Unit> retreated = getTransportDependents(units,m_data);
    	if(!retreated.isEmpty())
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
    
	private void reLoadTransports(Collection<Unit> units, CompositeChange change) {
		Collection<Unit> transports = Match.getMatches(units,
		        Matches.UnitCanTransport);

		// Put units back on their transports
		Iterator<Unit> transportsIter = transports.iterator();
		
		while (transportsIter.hasNext())
		{
		    Unit transport = transportsIter.next();
		    Collection unloaded = getTransportTracker().unloaded(transport);
		    Iterator unloadedIter = unloaded.iterator();
		    while (unloadedIter.hasNext())
		    {
		        Unit load = (Unit) unloadedIter.next();
		        Change loadChange =  getTransportTracker().loadTransportChange( (TripleAUnit) transport, load, m_attacker);
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
        } else
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
        for(Unit u : submerging) 
        {
            change.add(ChangeFactory.unitPropertyChange(u, true, TripleAUnit.SUBMERGED));
        }
        bridge.addChange(change);

        units.removeAll(submerging);
        /*if (units.isEmpty() || m_over)
        {
            endBattle(bridge);
            if (defender)
                attackerWins(bridge);
            else
                defenderWins(bridge);
        } else*/
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
        
        //our own air units dont retreat with land units
        Match<Unit> notMyAir = new CompositeMatchOr<Unit>(Matches.UnitIsNotAir,
                new InverseMatch<Unit>(Matches.unitIsOwnedBy(m_attacker)));
        retreating = Match.getMatches(retreating, notMyAir);

        String transcriptText;
        // in classic A&A, defending subs can retreat so show owner
        if (isFourthEdition())
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
            //If there are no dependent battles, check landings in allied territories
            if(dependentBattles.isEmpty())
                change.add(retreatFromNonCombat(retreating, bridge, to));
            //Else retreat the units from combat when their transport retreats
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
        } else
        {
            getDisplay(bridge).notifyRetreat(m_battleID, retreating);


        }
    }
  
    private void retreatUnitsAndPlanes(Collection<Unit> retreating, Territory to,
            boolean defender, IDelegateBridge bridge)
    {
        //Remove air from battle
        Collection<Unit> units = defender ? m_defendingUnits : m_attackingUnits;
        units.removeAll(Match.getMatches(units, Matches.UnitIsAir));
        
        //add all land units' dependents
        //retreating.addAll(getDependentUnits(retreating));
        retreating.addAll(getDependentUnits(units));
    	
        //our own air units dont retreat with land units
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
            //If there are no dependent battles, check landings in allied territories
            if(dependentBattles.isEmpty())
                change.add(retreatFromNonCombat(nonAirRetreating, bridge, to));
            //Else retreat the units from combat when their transport retreats
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
        } else
        {
            getDisplay(bridge).notifyRetreat(m_battleID, retreating);


        }
    }
    
    //the maximum number of hits that this collection of units can sustain
    //takes into account units with two hits
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
            } else
            {
                count++;
            }
        }
        return count;
    }

    private void fire(final String stepName, Collection<Unit> firingUnits,
            Collection<Unit> attackableUnits, boolean defender,
            boolean canReturnFire, final IDelegateBridge bridge, String text)
    {
        PlayerID firing = defender ? m_defender : m_attacker;
        PlayerID defending = !defender ? m_defender : m_attacker;        
        
        
        m_stack.push(new Fire(attackableUnits, canReturnFire, firing, defending, firingUnits, stepName, text, this, defender, 
                m_dependentUnits, m_stack, m_headless));               
    }
    
    /**
     * Check for unescorted TRNS and kill them immediately
     * @param bridge
     * @param player
     * @param defender
     */
    private void checkUndefendedTransports(IDelegateBridge bridge, PlayerID player)
    {
    	//Get all allied transports in the territory
        CompositeMatch<Unit> matchAllied = new CompositeMatchAnd<Unit>(); 
        matchAllied.add(Matches.UnitIsTransport);
        matchAllied.add(Matches.isUnitAllied(player, m_data));
        matchAllied.add(Matches.UnitIsSea);
    	
        List<Unit> alliedTransports = Match.getMatches(m_battleSite.getUnits().getUnits(), matchAllied);
    	
    	
    	//If no transports, just return
        if (alliedTransports.isEmpty())
            return;

        //Get all ALLIED, sea & air units in the territory (that are NOT submerged)
        CompositeMatch<Unit> alliedUnitsMatch = new CompositeMatchAnd<Unit>(); 
        alliedUnitsMatch.add(Matches.isUnitAllied(player, m_data));
        alliedUnitsMatch.add(Matches.UnitIsNotLand);     
        alliedUnitsMatch.add(new InverseMatch<Unit>( Matches.unitIsSubmerged(m_data)));
    	Collection<Unit> alliedUnits = Match.getMatches(m_battleSite.getUnits().getUnits(), alliedUnitsMatch);

        //If transports are unescorted, check opposing forces to see if the Trns die automatically
    	if(alliedTransports.size() == alliedUnits.size())     
        {
        	//Get all the ENEMY sea and air units in the territory
            CompositeMatch<Unit> enemyUnitsMatch = new CompositeMatchAnd<Unit>();
            enemyUnitsMatch.add(Matches.UnitIsNotLand);
            enemyUnitsMatch.add(Matches.UnitIsNotTransport);
            enemyUnitsMatch.add(Matches.unitIsNotSubmerged(m_data));
            enemyUnitsMatch.add(Matches.unitCanAttack(player));            
            Collection<Unit> enemyUnits = Match.getMatches(m_battleSite.getUnits().getUnits(), enemyUnitsMatch);
            
    		//If there are opposing forces with attack power, kill the transports  
        	if (enemyUnits.size() > 0)
        	{
    			remove(alliedTransports, bridge, m_battleSite, false);   
            	//and remove them from the battle display
            	if(player.equals(m_defender))
            		m_defendingUnits.removeAll(alliedTransports);
            	else
            		m_attackingUnits.removeAll(alliedTransports);        		
        	}
        }
    }

    /**
     * Submerge attacking/defending SUBS if they're alone OR with TRNS against only AIRCRAFT
     * @param bridge
     * @param player
     * @param defender
     */
    private void submergeSubsVsOnlyAir(IDelegateBridge bridge)
    {
	    //if All attackers are AIR submerge any defending subs  ..m_defendingUnits.removeAll(m_killed);
	    if(Match.allMatch(m_attackingUnits, Matches.UnitIsAir) && Match.someMatch(m_defendingUnits, Matches.UnitIsSub))
	    {
	    	//Get all defending subs (including allies) in the territory
	        CompositeMatch<Unit> matchDefendingSubs = new CompositeMatchAnd<Unit>(); 
	        matchDefendingSubs.add(Matches.UnitIsSub);
	        matchDefendingSubs.add(Matches.isUnitAllied(m_defender, m_data));	    	
	        List<Unit> defendingSubs = Match.getMatches(m_defendingUnits, matchDefendingSubs);
	        	        
	    	//submerge defending subs
	        submergeUnits(defendingSubs, true, bridge);
	      //getDisplay(bridge).notifyRetreat(messageShort, messageShort, step, m_defender);
	        
	    }  //checking defending air on attacking subs
	    else if (Match.allMatch(m_defendingUnits, Matches.UnitIsAir) && Match.someMatch(m_attackingUnits, Matches.UnitIsSub))
	    {
	    	//Get all attacking subs in the territory
	        CompositeMatch<Unit> matchAttackingSubs = new CompositeMatchAnd<Unit>(); 
	        matchAttackingSubs.add(Matches.UnitIsSub);
	        matchAttackingSubs.add(Matches.isUnitAllied(m_attacker, m_data));	    	
	        List<Unit> attackingSubs = Match.getMatches(m_attackingUnits, matchAttackingSubs);

	    	//submerge attacking subs	        
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

        //if restricted, remove aircraft from attackers
        if(isAirAttackSubRestricted() && !canAirAttackSubs(m_attackingUnits, units))
        {
            units.removeAll(Match.getMatches(units, Matches.UnitIsAir));
        }
        
        if (units.isEmpty())
            return;

        fire(m_attacker.getName() + SELECT_CASUALTIES, units,
                m_attackingUnits, true, true, bridge, "Defenders fire, ");
    }
    
    //If there are no attacking DDs but defending SUBs, fire AIR at non-SUB forces ONLY
    private void attackAirOnNonSubs(IDelegateBridge bridge)
    {
        if (m_defendingUnits.size() == 0)
            return;
            
        Collection<Unit> units = new ArrayList<Unit>(m_attackingUnits.size() + m_attackingWaitingToDie.size());
        units.addAll(m_attackingUnits);
        units.addAll(m_attackingWaitingToDie); 
        units = Match.getMatches(units, Matches.unitIsOwnedBy(m_attacker));        
        
        if(!canAirAttackSubs(m_defendingUnits, units))
        {
            units = Match.getMatches(units, Matches.UnitIsAir);  
            Collection<Unit> enemyUnitsNotSubs = Match.getMatches(m_defendingUnits, Matches.UnitIsNotSub);

            fire(m_defender.getName() + SELECT_CASUALTIES, units, enemyUnitsNotSubs, false, true, bridge, "Attacker's aircraft fire,");            
        }
    }
    
    private boolean canAirAttackSubs(Collection<Unit> firedAt, Collection<Unit> firing)
    {
        if(m_battleSite.isWater() && Match.someMatch(firedAt, Matches.UnitIsSub) && Match.noneMatch(firing, Matches.UnitIsDestroyer))
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
        units = Match.getMatches(units, Matches.unitIsOwnedBy(m_defender));        
        
        if(!canAirAttackSubs(m_attackingUnits, units))
        {
            units = Match.getMatches(units, Matches.UnitIsAir);  
            Collection<Unit> enemyUnitsNotSubs = Match.getMatches(m_attackingUnits, Matches.UnitIsNotSub);
            if(enemyUnitsNotSubs.isEmpty())
            	return;
            
            fire(m_defender.getName() + SELECT_CASUALTIES, units, enemyUnitsNotSubs, true, true, bridge, "Defender's aircraft fire,");            
        }
    }
    
//If there are no attacking DDs, but defending SUBs, remove attacking AIR as they've already fired- otherwise fire all attackers.     
    private void attackNonSubs(IDelegateBridge bridge)
    {
        if (m_defendingUnits.size() == 0)
            return;

        Collection<Unit> units = Match.getMatches(m_attackingUnits,
                Matches.UnitIsNotSub);
        units.addAll(Match.getMatches(m_attackingWaitingToDie,
                Matches.UnitIsNotSub));
        units = Match.getMatches(units,Matches.unitIsOwnedBy(m_attacker));
        //if restricted, remove aircraft from attackers
        if(isAirAttackSubRestricted() && !canAirAttackSubs(m_defendingUnits, units))
        {
            units.removeAll(Match.getMatches(units, Matches.UnitIsAir));
        }
        
        if (units.isEmpty())
            return;

        fire(m_defender.getName() + SELECT_CASUALTIES, units,
                m_defendingUnits, false, true, bridge, "Attackers fire,");
    }

    private void attackSubs(IDelegateBridge bridge)
    {
        Collection<Unit> firing = Match.getMatches(m_attackingUnits,
                Matches.UnitIsSub);
        if (firing.isEmpty())
            return;

        Collection<Unit> attacked = Match.getMatches(m_defendingUnits,
                Matches.UnitIsNotAir);
        //if there are destroyers in the attacked units, we can return fire.
        boolean destroyersPresent = Match.someMatch(attacked,
                Matches.UnitIsDestroyer);
        fire(SELECT_SNEAK_ATTACK_CASUALTIES, firing, attacked, false,
                destroyersPresent, bridge, "Subs fire,");
    }

    private void defendSubs(IDelegateBridge bridge)
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

        boolean destroyersPresent = Match.someMatch(attacked,
                Matches.UnitIsDestroyer);
        fire(m_attacker.getName() + SELECT_SUB_CASUALTIES, firing,
                attacked, true, destroyersPresent, bridge, "Subs defend, ");
    }

    private void attackAny(IDelegateBridge bridge)
    {
        if (m_defendingUnits.size() == 0)
            return;

        Collection<Unit> units = new ArrayList<Unit>(m_attackingUnits.size()
                + m_attackingWaitingToDie.size());
        units.addAll(m_attackingUnits);
        units.addAll(m_attackingWaitingToDie);
        
        if(isAirAttackSubRestricted() && !canAirAttackSubs(m_defendingUnits, units))
        {
            units.removeAll(Match.getMatches(units, Matches.UnitIsAir));
        }
        
        if (units.isEmpty())
            return;

        fire(m_defender.getName() + SELECT_CASUALTIES, units,
                m_defendingUnits, false, true, bridge, "Attackers fire,");
    }

    private void defendAny(IDelegateBridge bridge)
    {

        if (m_attackingUnits.size() == 0)
            return;

        Collection<Unit> units = new ArrayList<Unit>(m_defendingUnits.size()
                + m_defendingWaitingToDie.size());
        units.addAll(m_defendingUnits);
        units.addAll(m_defendingWaitingToDie);
        //if restricted, remove aircraft from attackers
        
        if(isAirAttackSubRestricted() && !canAirAttackSubs(m_attackingUnits, units))
        {
            units.removeAll(Match.getMatches(units, Matches.UnitIsAir));
        }
         
        if (units.isEmpty())
            return;

        fire(m_attacker.getName() + SELECT_CASUALTIES, units,
                m_attackingUnits, true, true, bridge, "Defenders fire, ");
    }

    private CasualtyDetails selectCasualties(String step,
            IDelegateBridge bridge, Collection<Unit> attackableUnits,
            boolean defender, String text, DiceRoll dice)
    {

        PlayerID hit = defender ? m_defender : m_attacker;
        return BattleCalculator.selectCasualties(step, hit, attackableUnits,
                bridge, text, m_data, dice, defender, m_battleID, m_headless, dice.getHits());
    }

    void removeCasualties(Collection<Unit> killed, boolean canReturnFire,
            boolean defender, IDelegateBridge bridge, boolean isAA)
    {
        if(killed.isEmpty())
            return;

        if (canReturnFire)
        {
            //move to waiting to die
            if (defender)
                m_defendingWaitingToDie.addAll(killed);
            else
                m_attackingWaitingToDie.addAll(killed);
        } else
        {
            if(defender && isDefendingSubsSneakAttack())
            {   //if there are no attacking destroyers, allow killed defending subs to sneak attack
                if(Match.noneMatch(m_attackingUnits, Matches.UnitIsDestroyer))
                {
                    m_defendingWaitingToDie.addAll(Match.getMatches(killed, Matches.UnitIsSub));
                    //remove the rest immediately            
                    remove(Match.getMatches(killed, Matches.UnitIsNotSub), bridge, m_battleSite, false);
                    m_defendingUnits.removeAll(killed);
                    return;
                }
            }
            //remove immediately            
            remove(killed, bridge, m_battleSite, isAA);
        }

        //remove from the active fighting
        if (defender)
            m_defendingUnits.removeAll(killed);
        else
            m_attackingUnits.removeAll(killed);
               
    }

    private void fireNavalBombardment(IDelegateBridge bridge)
    {
        //TODO - check within the method for the bombarding limitations
        Collection<Unit> bombard = getBombardingUnits();
        Collection<Unit> attacked = Match.getMatches(m_defendingUnits,
                Matches.UnitIsDestructible);

        //bombarding units cant move after bombarding
        if(!m_headless) 
        {
            Change change =DelegateFinder.moveDelegate(m_data).markNoMovementChange(bombard);
            bridge.addChange(change);
        }

        //4th edition, bombardment casualties cant return fire
        boolean canReturnFire = (!isFourthEdition() && isNavalBombardCasualtiesReturnFire());

        if (bombard.size() > 0 && attacked.size() > 0)
        {
            fire(SELECT_NAVAL_BOMBARDMENT_CASUALTIES, bombard, attacked, false,
                    canReturnFire, bridge, "Bombard");
        }
    }
    
    /**
     * @return
     */
    private boolean isChooseAA()
	{
		return m_data.getProperties().get(Constants.CHOOSE_AA, false);
	}
    
    /**
     * @return
     */
    private boolean isFourthEdition()
    {
    	return games.strategy.triplea.Properties.getFourthEdition(m_data);
    }

    private boolean isPartialAmphibiousRetreat()
    {
        return games.strategy.triplea.Properties.getPartialAmphibiousRetreat(m_data);
    }
    
    private boolean isParatroopers(PlayerID player)    
    {
        TechAttachment ta = (TechAttachment) player.getAttachment(Constants.TECH_ATTATCHMENT_NAME);
        if(ta == null)
        	return false;
        return ta.hasParatroopers();
    }    

    /**
     * @return
     */
    private boolean isPacificEdition()
    {
        return m_data.getProperties().get(Constants.PACIFIC_EDITION, false);
    }
    
    /**
     * @return
     */
    private boolean isEuropeEdition()
    {
        return m_data.getProperties().get(Constants.EUROPE_EDITION, false);
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
    private boolean isRandomAACasualties()
    {
    	return games.strategy.triplea.Properties.getRandomAACasualties(m_data);
    }

    /**
     * @return
     */
    private boolean isRollAAIndividually()
    {
        return games.strategy.triplea.Properties.getRollAAIndividually(m_data);
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
    public void addBombardingUnit(Unit unit)
    {
        m_bombardingUnits.add(unit);
    }

    /**
     * Return bombarding units.
     */
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
     
        public void execute(ExecutionStack stack, final IDelegateBridge bridge, GameData data)
        {
            if (!canFireAA())
                return;
            
            IExecutable rollDice = new IExecutable()
            {
            
                public void execute(ExecutionStack stack, IDelegateBridge bridge,
                        GameData data)
                {
                    rollDice(bridge);
                }
            
            };
            
            
            IExecutable selectCasualties = new IExecutable()
            {
            
                public void execute(ExecutionStack stack, IDelegateBridge bridge,
                        GameData data)
                {
                    selectCasualties(bridge);
                }
            };
            
            IExecutable notifyCasualties = new IExecutable()
            {

                public void execute(ExecutionStack stack, IDelegateBridge bridge, GameData data)
                {
                    notifyCasualtiesAA(bridge);                    
                    removeCasualties(m_casualties, false, false, bridge, true);                    
                }                
            };
            //push in reverse order of execution
            stack.push(notifyCasualties);
            stack.push(selectCasualties);
            stack.push(rollDice);
        }
        
        
        private void rollDice(IDelegateBridge bridge)
        {
            
            int attackingAirCount = Match.countMatches(m_attackingUnits,
                    Matches.UnitIsAir);
            
            m_dice = DiceRoll.rollAA(attackingAirCount, bridge,
                    m_battleSite, m_data);
        }

        private void selectCasualties(final IDelegateBridge bridge)
        {
            //DiceRoll dice = DiceRoll.rollAA(attackingAirCount, bridge);
            // NEW VERSION
            
            boolean lowLuck = m_data.getProperties().get(Constants.LOW_LUCK, false);

            //send defender the dice roll so he can see what the dice are while he
            // waits for attacker to select casualties
            getDisplay(bridge).notifyDice(m_battleID,  m_dice, SELECT_AA_CASUALTIES);
           
            Collection<Unit> attackable = Match.getMatches(m_attackingUnits,
                    Matches.UnitIsAir);
                      
            if(!lowLuck && isRollAAIndividually())
            { // select casualties based on individual shots at each aircraft
                m_casualties = BattleCalculator.IndividuallyFiredAACasualties(attackable, m_dice, bridge, m_defender);
            }
            else if (!lowLuck && (isFourthEdition() || isRandomAACasualties()) && !isChooseAA())
            { // if 4th edition choose casualties randomnly
                m_casualties = BattleCalculator.fourthEditionAACasualties(attackable,
                        m_dice, bridge);
            } 
            else
            { // allow player to select casualties from entire set
                m_casualties = MustFightBattle.this.selectCasualties(SELECT_AA_CASUALTIES, bridge, attackable, false,
                        "AA guns fire,", m_dice).getKilled();
            }
        }


        private void notifyCasualtiesAA(final IDelegateBridge bridge)
        {
            if(m_headless)
                return;
            
            getDisplay(bridge).casualtyNotification(m_battleID, SELECT_AA_CASUALTIES, m_dice, m_attacker, new ArrayList<Unit>(m_casualties), Collections.<Unit>emptyList(), m_dependentUnits);
                        
            getRemote(m_attacker, bridge).confirmOwnCasualties(m_battleID, "Press space to continue");
            Runnable r = new Runnable()
            {
                public void run()
                {   
                    try
                    {
                        getRemote(m_defender, bridge).confirmEnemyCasualties(m_battleID, "Press space to continue", m_attacker);
                    
                    }
                    catch(ConnectionLostException cle)
                    {
                        //somone else will deal with this
                        cle.printStackTrace(System.out);
                    }
                    catch(GameOverException e) 
                    {
                      //ignore
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
              //ignore
            } 
            finally
            {
                bridge.enterDelegateExecution();
            }
        }
        
    }
    

    private boolean canFireAA()
    {

        return Match.someMatch(m_defendingUnits, Matches.UnitIsAA)
                && Match.someMatch(m_attackingUnits, Matches.UnitIsAir)
                && !m_battleSite.isWater();
    }

    /**
     * @return a collection containing all the combatants in units non
     *         combatants include such things as factories, aaguns, land units
     *         in a water battle.
     */
    private List<Unit> removeNonCombatants(Collection<Unit> units)
    {
        CompositeMatch<Unit> combat = new CompositeMatchAnd<Unit>();
        combat.add(new InverseMatch<Unit>(Matches.UnitIsAAOrFactory));

        if (m_battleSite.isWater())
        {
            combat.add(new InverseMatch<Unit>(Matches.UnitIsLand));
        }

        return Match.getMatches(units, combat);

    }

    private void removeNonCombatants()
    {
        m_defendingUnits = removeNonCombatants(m_defendingUnits);
        m_attackingUnits = removeNonCombatants(m_attackingUnits);
    }

    private void landParatroops(IDelegateBridge bridge)
    {
        if(isParatroopers(m_attacker))
        {
        	Collection<Unit> bombers = Match.getMatches(m_battleSite.getUnits().getUnits(), Matches.UnitIsStrategicBomber);

        	if(!bombers.isEmpty())
        	{
        		Collection<Unit> dependents = getDependentUnits(bombers);
        		if(!dependents.isEmpty())
        		{
        			Iterator<Unit> dependentsIter = dependents.iterator();

        			CompositeChange change = new CompositeChange();
        			//remove dependency from paratroops
        			while(dependentsIter.hasNext())
        			{
        				Unit unit = dependentsIter.next();
        				change.add(ChangeFactory.unitPropertyChange(unit, null, TripleAUnit.TRANSPORTED_BY ));
        			}
        			bridge.addChange(change);
        		
        			//remove bombers from m_dependentUnits
        			Iterator<Unit> bombersIter = bombers.iterator();
        			while(bombersIter.hasNext())
        			{
        				Unit unit = bombersIter.next();
        				m_dependentUnits.remove(unit);
        			}
        		}
        	}
        }
    }
    
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
    
    //Figure out what units a transport is transported and has unloaded
    public Collection<Unit>  getTransportDependents(Collection<Unit> targets, GameData data)
    {        
        if(m_headless) 
        {
            return Collections.emptyList();
        }
    	Collection<Unit> dependents = new ArrayList<Unit>();
    	if (Match.someMatch(targets, Matches.UnitCanTransport))
    	{    		 
    		//just worry about transports
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
      //If there are NO dependent battles, check for unloads in allied territories 
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

	//Remove landed units from allied territory when their transport sinks
    private void removeFromNonCombatLandings(Collection<Unit> units, IDelegateBridge bridge)
    {
    	units = Match.getMatches(units, Matches.UnitIsTransport);
    	Collection<Unit> lost = getTransportDependents(units, m_data);
    	Territory landedTerritory = null;
    	
    	Iterator<Unit> Iter = units.iterator();
    	while (Iter.hasNext())
    	{
    		Unit unit = Iter.next();
    		landedTerritory = getTransportTracker().getTerritoryTransportHasUnloadedTo(unit);
    	}
    	                
        m_attackingUnits.removeAll(lost);
        remove(lost, bridge, landedTerritory, false);
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
        getDisplay(bridge).battleEnd(m_battleID, m_defender.getName()+ " win");
        
        bridge.getHistoryWriter()
                .addChildToEvent(m_defender.getName() + " win", m_defendingUnits);
        showCasualties(bridge);

        checkDefendingPlanesCanLand(bridge, m_defender);

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
        //if its the null player, return a do nothing proxy
        if(player.isNull())
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
        //this only applies to air units that can land on a carrier
        canLandOnCarrier.add(Matches.UnitCanLandOnCarrier);
        
        Collection<Unit> air = Match.getMatches(m_attackingUnits, canLandOnCarrier);
        
        for(Unit unit : air)
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

        if(m_headless)
            return;

        //not water, not relevant.
        if (!m_battleSite.isWater())
            return;

        CompositeMatch<Unit> alliedDefendingAir = new CompositeMatchAnd<Unit>(
                Matches.UnitIsAir, Matches.isUnitAllied(m_defender, m_data));
        m_defendingAir  = Match.getMatches(m_defendingUnits,
                alliedDefendingAir);
        
        //no planes, exit
        if (m_defendingAir.isEmpty())
            return;

        int carrierCost = MoveValidator.carrierCost(m_defendingAir);
        int carrierCapacity = MoveValidator.carrierCapacity(m_defendingUnits);

        //all planes can land, exit
        if (carrierCapacity >= carrierCost)
            return;

        //find out what we must remove
        //remove all the air that can land on carriers from defendingAir
        carrierCost = 0;
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
                Matches.TerritoryIsLand, Matches.isTerritoryAllied(m_defender,m_data), 
                Matches.territoryHasNoEnemyUnits(m_defender, m_data));
        // Get those that are neighbors
        Collection<Territory> canLandHere = Match.getMatches(neighbors, alliedLandTerritories);

        // Get all sea territories where there are allies and no pending battles
        CompositeMatch<Territory> neighboringSeaZonesWithAlliedUnits = new CompositeMatchAnd<Territory>(
                Matches.TerritoryIsWater, Matches.territoryHasEnemyUnits(m_attacker, m_data),
                Matches.territoryHasNoEnemyUnits(m_defender, m_data));
        // Get those that are neighbors
        Collection<Territory> areSeaNeighbors = Match.getMatches(neighbors,neighboringSeaZonesWithAlliedUnits);
        
        //Set up match criteria for allied carriers
        CompositeMatch<Unit> alliedCarrier = new CompositeMatchAnd<Unit>();
        alliedCarrier.add(Matches.UnitIsCarrier);
        alliedCarrier.add(Matches.alliedUnit(m_defender, m_data));

        //Set up match criteria for allied planes
        CompositeMatch<Unit> alliedPlane = new CompositeMatchAnd<Unit>();
        alliedPlane.add(Matches.UnitIsAir);
        alliedPlane.add(Matches.alliedUnit(m_defender, m_data));
        
        //See if neighboring carriers have any capacity available
        Iterator<Territory> neighborSeaZoneIter = areSeaNeighbors.iterator();
        while (neighborSeaZoneIter.hasNext())
        {
        	Territory currentTerritory = neighborSeaZoneIter.next();
        	
            //get the capacity of the carriers and cost of fighters
            Collection<Unit> alliedCarriers = currentTerritory.getUnits().getMatches(alliedCarrier);
            Collection<Unit> alliedPlanes = currentTerritory.getUnits().getMatches(alliedPlane);
            int alliedCarrierCapacity = MoveValidator.carrierCapacity(alliedCarriers);
            int alliedPlaneCost = MoveValidator.carrierCost(alliedPlanes);
            //if there is free capacity, add the territory to landing possibilities
            if (alliedCarrierCapacity - alliedPlaneCost >= 1)
            {                
            	canLandHere.add(currentTerritory);
            }            
        }        
        
        if (isFourthEdition() || isSurvivingAirMoveToLand())
		{
        	Territory territory = null;
        	while (canLandHere.size() > 1 && m_defendingAir.size() > 0)
        	{
        		territory = getRemote(m_defender, bridge).selectTerritoryForAirToLand(canLandHere);
        		
        		//added for test script
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
    			//remove the territory from those available
    			canLandHere.remove(territory);
        	}
        	
        	//Land in the last remaining territory
        	if(canLandHere.size() > 0 && m_defendingAir.size() > 0)
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
        {	// 2nd edition
            //now defending air has what cant stay, is there a place we can go?
            //check for an island in this sea zone
            Iterator<Territory> neighborsIter = canLandHere.iterator();
            while (neighborsIter.hasNext())
            {
                Territory currentTerritory = neighborsIter.next();
                //only one neighbor, its an island.
                if (m_data.getMap().getNeighbors(currentTerritory).size() == 1)
                {
                    moveAirAndLand(bridge, m_defendingAir, currentTerritory);
                    return;
                }
            }            
        }        
        
        if (m_defendingAir.size() > 0)
        {	        	
	        //no where to go, they must die
	        bridge.getHistoryWriter().addChildToEvent(
	                MyFormatter.unitsToText(m_defendingAir)
	                        + " could not land and were killed", m_defendingAir);
	        Change change = ChangeFactory.removeUnits(m_battleSite, m_defendingAir);
	        bridge.addChange(change);
        }
    }

    //Refactored this method
	private void landPlanesOnCarriers(IDelegateBridge bridge,
			CompositeMatch<Unit> alliedDefendingAir,
			Collection<Unit> defendingAir, Collection<Territory> canLandHere,
			CompositeMatch<Unit> alliedCarrier,
			CompositeMatch<Unit> alliedPlane, Territory territory) {
		//Get the capacity of the carriers in the selected zone
		Collection<Unit> alliedCarriersSelected = territory.getUnits().getMatches(alliedCarrier);
		Collection<Unit> alliedPlanesSelected = territory.getUnits().getMatches(alliedPlane);
		int alliedCarrierCapacitySelected = MoveValidator.carrierCapacity(alliedCarriersSelected);
		int alliedPlaneCostSelected = MoveValidator.carrierCost(alliedPlanesSelected);

		//Find the available capacity of the carriers in that territory
		int territoryCapacity = alliedCarrierCapacitySelected - alliedPlaneCostSelected;
		if (territoryCapacity>0)
		{					
			//move that number of planes from the battlezone
			Collection<Unit> movingAir = Match.getNMatches(defendingAir,territoryCapacity,alliedDefendingAir);        		
			moveAirAndLand(bridge, movingAir, territory);			
		}
	}

    //Refactored this method
	private void moveAirAndLand(IDelegateBridge bridge,
			Collection<Unit> defendingAir, Territory territory) {
		bridge.getHistoryWriter().addChildToEvent(
		        MyFormatter.unitsToText(defendingAir) + " forced to land in "
		                + territory.getName(), defendingAir);
		Change change = ChangeFactory.moveUnits(m_battleSite, territory,
		        defendingAir);
		bridge.addChange(change);
		
		//remove those that landed in case it was a carrier
		m_defendingAir.removeAll(defendingAir);
	}
    
    GUID getBattleID()
    {
        return m_battleID;
    }

    private void attackerWins(IDelegateBridge bridge)
    {
        getDisplay(bridge).battleEnd(m_battleID, m_attacker.getName() + " win");

        if(m_headless)
            return;
        
        //do we need to change ownership
        if (Match.someMatch(m_attackingUnits, Matches.UnitIsNotAir))
        {
            m_tracker.addToConquered(m_battleSite);
            m_tracker.takeOver(m_battleSite, m_attacker, bridge, m_data,
                    null, m_attackingUnits);
        }

        bridge.getHistoryWriter()
                .addChildToEvent(m_attacker.getName() + " win", m_attackingUnits);
        showCasualties(bridge);
    }

    private void showCasualties(IDelegateBridge bridge)
    {
        if (m_killed.isEmpty())
            return;
        //a handy summary of all the units killed
        IntegerMap<UnitType> costs = BattleCalculator.getCosts(m_attacker, m_data);
        int tuvLostAttacker = BattleCalculator.getTUV(m_killed, m_attacker,
                costs, m_data);
        costs = BattleCalculator.getCosts(m_defender, m_data);
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
    
    public List getRemainingAttackingUnits()
    {
        return m_attackingUnits;
    }
    
    public List getRemainingDefendingUnits()
    {
        return m_defendingUnits;
    }

    public String toString()
    {

        return "Battle in:" + m_battleSite + " attacked by:" + m_attackingUnits
                + " from:" + m_attackingFrom + " defender:"
                + m_defender.getName() + " bombing:" + isBombingRun();
    }

    // In an amphibious assault, sort on who is unloading from xports first
    // This will allow the marines with higher scores to get killed last
    //TODO comco this may be used for partial retreats also
    public void sortAmphib(List<Unit> units, GameData data)
    {
        final Comparator<Unit> decreasingMovement = UnitComparator.getDecreasingMovementComparator();        

        Comparator<Unit> comparator = new Comparator<Unit>()
        {
          public int compare(Unit u1, Unit u2)
          {            
              int amphibComp = 0;

              if(u1.getUnitType().equals(u2.getUnitType()))
              {
                  UnitAttachment ua = UnitAttachment.get(u1.getType());
                  UnitAttachment ua2 = UnitAttachment.get(u2.getType());
                  if(ua.getIsMarine() && ua2.getIsMarine())
                      amphibComp = compareAccordingToAmphibious(u1, u2);
                  if(amphibComp == 0)
                      return  decreasingMovement.compare(u1,u2);
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
        if(m_amphibiousLandAttackers.contains(u1) && !m_amphibiousLandAttackers.contains(u2))
            return -1;
        else if(m_amphibiousLandAttackers.contains(u2) && !m_amphibiousLandAttackers.contains(u1)) 
            return 1;
        return 0; 
    }

    public Collection<Unit> getAttackingUnits()
    {
        return m_attackingUnits;
    }
    
    public Collection<Territory> getAttackingFrom()
    {
        return m_attackingFrom;
    }

    public Collection<Unit> getAmphibiousLandAttackers()
    {
        return m_amphibiousLandAttackers;
    }

    public void unitsLostInPrecedingBattle(Battle battle, Collection<Unit> units,
            IDelegateBridge bridge)
    {

        Collection<Unit> lost = getDependentUnits(units);

        //if all the amphibious attacking land units are lost, then we are
        //no longer a naval invasion
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
    public boolean isAmphibious()
    {
        return m_amphibious;
    }
    
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
    private final  Collection<Unit> m_attackableUnits;
    private final boolean m_canReturnFire;

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
    
    public Fire(Collection<Unit> attackableUnits, boolean canReturnFire, PlayerID firingPlayer, PlayerID hitPlayer, 
            Collection<Unit> firingUnits, String stepName, String text, MustFightBattle battle, 
            boolean defending, Map<Unit, Collection<Unit>> dependentUnits, ExecutionStack stack, boolean headless)
    {
        m_attackableUnits = attackableUnits;
        
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
    
    private void rollDice(IDelegateBridge bridge, GameData data)
    {
        if(m_dice != null)
            throw new IllegalStateException("Already rolled");
        
        List<Unit> units = new ArrayList<Unit>(m_firingUnits);
        
        String annotation;
        if(m_isHeadless)
            annotation = "";
        else
            annotation = DiceRoll.getAnnotation(units, m_firingPlayer, m_battle);
        
        
        m_dice = DiceRoll.rollDice(units, m_defending,
                m_firingPlayer, bridge, data, m_battle, annotation);

    }
    
    private void selectCasualties(IDelegateBridge bridge, GameData data)
    {
        boolean isEditMode = EditDelegate.getEditMode(data);
        if (isEditMode)
        {
            CasualtyDetails message = BattleCalculator.selectCasualties(m_stepName, m_hitPlayer, 
                    m_attackableUnits, bridge, m_text, data, m_dice,!m_defending, m_battleID, m_isHeadless, m_dice.getHits());

            m_killed = message.getKilled();
            m_damaged = message.getDamaged();
            m_confirmOwnCasualties = message.getAutoCalculated();
        }
        else
        {
            int hitCount = m_dice.getHits();

            MustFightBattle.getDisplay(bridge).notifyDice(m_battle.getBattleID(), m_dice, m_stepName);

            int countTransports = Match.countMatches(m_attackableUnits, new CompositeMatchAnd<Unit>(Matches.UnitIsTransport, Matches.UnitIsSea));

            if (countTransports > 0 && isTransportCasualtiesRestricted(data))
            {
                CasualtyDetails message;
                Collection<Unit> nonTransports = Match.getMatches(m_attackableUnits, new CompositeMatchOr<Unit>(Matches.UnitIsNotTransport, Matches.UnitIsNotSea));
                Collection<Unit> transportsOnly = Match.getMatches(m_attackableUnits, new CompositeMatchAnd<Unit>(Matches.UnitIsTransport, Matches.UnitIsSea));
                int numPossibleHits = MustFightBattle.getMaxHits(nonTransports);
                
                //more hits than combat units
                if(hitCount > numPossibleHits)
                {
                    int extraHits = hitCount - numPossibleHits;
                    Collection<Unit> remainingTargets = new ArrayList<Unit>();
                    remainingTargets.addAll(m_attackableUnits);
                    remainingTargets.removeAll(nonTransports);
                    
                    Collection<PlayerID> alliedHitPlayer = new ArrayList<PlayerID>();
                    //find the players who have transports in the attackable pile
                    for(Unit unit:transportsOnly)
                    {
                        if(!alliedHitPlayer.contains(unit.getOwner()))
                            alliedHitPlayer.add(unit.getOwner());
                    }
                    
                    Iterator<PlayerID> playerIter = alliedHitPlayer.iterator();
                    //Leave enough transports for each defender for overlfows so they can select who loses them.
                    while(playerIter.hasNext())
                    {
                        PlayerID player = playerIter.next();
                        CompositeMatch<Unit> match = new CompositeMatchAnd<Unit>();
                        match.add(Matches.UnitIsTransport);
                        match.add(Matches.unitIsOwnedBy(player));
                        Collection<Unit> playerTransports = Match.getMatches(transportsOnly, match);
                        int transportsToRemove = Math.max(0, playerTransports.size() - extraHits);
                        transportsOnly.removeAll(Match.getNMatches(playerTransports, transportsToRemove, Matches.UnitIsTransport));
                    }

                    m_killed = nonTransports;
                    m_damaged = Collections.emptyList();
                    //m_confirmOwnCasualties = true;
                    if(extraHits > transportsOnly.size())
                    	extraHits = transportsOnly.size();
                    
                    message = BattleCalculator.selectCasualties(m_stepName, m_hitPlayer, 
                        transportsOnly, bridge, m_text, data, m_dice,!m_defending, m_battleID, m_isHeadless, extraHits);

                    m_killed.addAll(message.getKilled());
                    m_confirmOwnCasualties = true;
                    
                } 
                //exact number of combat units
                else if(hitCount == numPossibleHits)
                {
                    m_killed = nonTransports;
                    m_damaged = Collections.emptyList();
                    m_confirmOwnCasualties = true;
                } 
                //less than possible number
                else
                {
                    message = BattleCalculator.selectCasualties(m_stepName, m_hitPlayer, 
                        nonTransports, bridge, m_text, data, m_dice,!m_defending, m_battleID, m_isHeadless, m_dice.getHits());

                    m_killed = message.getKilled();
                    m_damaged = message.getDamaged();
                    m_confirmOwnCasualties = message.getAutoCalculated();
                }
            }
            else  //not isTransportCasualtiesRestricted
            {
                //they all die
                if (hitCount >= MustFightBattle.getMaxHits(m_attackableUnits))
                {
                    m_killed = m_attackableUnits;
                    m_damaged = Collections.emptyList();
                    //everything died, so we need to confirm
                    m_confirmOwnCasualties = true;
                }
                //Choose casualties
                else
                {
                    CasualtyDetails message;
                    message = BattleCalculator.selectCasualties(m_stepName, m_hitPlayer, 
                        m_attackableUnits, bridge, m_text, data, m_dice,!m_defending, m_battleID, m_isHeadless, m_dice.getHits());
                    
                    m_killed = message.getKilled();
                    m_damaged = message.getDamaged();
                    m_confirmOwnCasualties = message.getAutoCalculated();
                }
            }
        }
    }
    
    private void notifyCasualties(final IDelegateBridge bridge)
    {
        
        if(m_isHeadless)
            return;
        
        MustFightBattle.getDisplay(bridge).casualtyNotification(m_battleID, m_stepName, m_dice, m_hitPlayer, new ArrayList<Unit>(m_killed), new ArrayList<Unit>(m_damaged), m_dependentUnits);


        Runnable r = new Runnable()
        {
            public void run()
            {
                try
                {
                    MustFightBattle.getRemote(m_firingPlayer, bridge).confirmEnemyCasualties(m_battleID, "Press space to continue",  m_hitPlayer);
                }
                catch(ConnectionLostException cle)
                {
                    //somone else will deal with this
                    cle.printStackTrace(System.out);
                }
            }
        };

        // execute in a seperate thread to allow either player to click continue first.
        Thread t = new Thread(r, "Click to continue waiter");
        t.start();

        if(m_confirmOwnCasualties)
            MustFightBattle.getRemote(m_hitPlayer, bridge).confirmOwnCasualties(m_battleID, "Press space to continue");

        
        try
        {
            bridge.leaveDelegateExecution();
            t.join();
        } catch (InterruptedException e)
        {
           //ignore
        }
        finally
        {
            bridge.enterDelegateExecution();
        }
        


      

        
    }


    /**
     * We must execute in atomic steps, push these steps onto the stack, and let them execute
     */
    public void execute(ExecutionStack stack, IDelegateBridge bridge, GameData data)
    {
        boolean isEditMode = EditDelegate.getEditMode(data);
        //add to the stack so we will execute,
        //we want to roll dice, select casualties, then notify in that order, so 
        //push onto the stack in reverse order
        
        IExecutable rollDice = new IExecutable()
        {        	
            public void execute(ExecutionStack stack, IDelegateBridge bridge,
                    GameData data)
            {
                rollDice(bridge, data);
            }
        };
        
        IExecutable selectCasualties = new IExecutable()
        {
        
            public void execute(ExecutionStack stack, IDelegateBridge bridge,
                    GameData data)
            {
                selectCasualties(bridge, data);
            }
        };
        
        IExecutable notifyCasualties = new IExecutable()
        {
            // compatible with 0.9.0.2 saved games
            private static final long serialVersionUID = -9173385989239225660L;

            public void execute(ExecutionStack stack, IDelegateBridge bridge,
                    GameData data)
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





