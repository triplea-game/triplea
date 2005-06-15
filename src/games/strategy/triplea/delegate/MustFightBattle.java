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
 */

package games.strategy.triplea.delegate;

import games.strategy.engine.data.*;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.net.GUID;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attatchments.UnitAttatchment;
import games.strategy.triplea.delegate.dataObjects.CasualtyDetails;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.triplea.player.ITripleaPlayer;
import games.strategy.triplea.ui.display.ITripleaDisplay;
import games.strategy.util.*;

import java.lang.reflect.*;
import java.lang.reflect.InvocationHandler;
import java.util.*;

/**
 * 
 * Handles logic for battles in which fighting actually occurs.
 * 
 * @author Sean Bridges
 * 
 */
public class MustFightBattle implements Battle, BattleStepStrings
{
    public static final int DEFAULT_RETREAT_TYPE = 0;
    public static final int SUBS_RETREAT_TYPE = 1;
    public static final int PLANES_RETREAT_TYPE = 2;

    private final Territory m_battleSite;

    //maps Territory-> units
    //stores a collection of who is attacking from where, needed
    //for undoing moves
    private Map m_attackingFromMap = new HashMap();
    private List m_attackingUnits = new LinkedList();
    private Collection m_attackingWaitingToDie = new ArrayList();
    private Set m_attackingFrom = new HashSet();
    private Collection m_amphibiousAttackFrom = new ArrayList();
    private Collection m_amphibiousLandAttackers = new ArrayList();
    private List m_defendingUnits = new LinkedList();
    private Collection m_defendingWaitingToDie = new ArrayList();
    private Collection m_bombardingUnits = new ArrayList();
    private boolean m_amphibious = false;
    private boolean m_over = false;
    private BattleTracker m_tracker;

    private TransportTracker m_transportTracker;

    private PlayerID m_defender;
    private PlayerID m_attacker;

    private GameData m_data;
    
    private final GUID m_battleID = new GUID();

    //dependent units
    //maps unit -> Collection of units
    //if unit is lost in a battle we are dependent on
    //then we lose the corresponding collection of units
    private Map m_dependentUnits = new HashMap();

    //keep track of all the units that die in the battle to show in the history
    // window
    private Collection m_killed = new ArrayList();
    
    private int m_round = 0;

    public MustFightBattle(Territory battleSite, PlayerID attacker,
            GameData data, BattleTracker tracker,
            TransportTracker transportTracker)
    {

        m_data = data;
        m_tracker = tracker;
        m_battleSite = battleSite;
        m_attacker = attacker;
        m_transportTracker = transportTracker;

        m_defendingUnits.addAll(m_battleSite.getUnits().getMatches(
                Matches.enemyUnit(attacker, data)));
        m_defender = findDefender(battleSite);
    }

    private boolean canSubsSubmerge()
    {

        return m_data.getProperties().get(Constants.SUBMERSIBLE_SUBS, false);
    }

    public boolean isOver()
    {
        return m_over;
    }

    public void removeAttack(Route route, Collection units)
    {
        m_attackingUnits.removeAll(units);
        
        //the route could be null, in the case of a unit in a territory where a sub is submerged.
        if(route == null)
            return;
        Territory attackingFrom = getAttackFrom(route);

        Collection attackingFromMapUnits = (Collection) m_attackingFromMap
                .get(attackingFrom);
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

        Iterator dependentHolders = m_dependentUnits.keySet().iterator();
        while (dependentHolders.hasNext())
        {
            Object holder = dependentHolders.next();
            Collection dependents = (Collection) m_dependentUnits.get(holder);
            dependents.removeAll(units);

        }

    }

    public boolean isEmpty()
    {

        return m_attackingUnits.isEmpty() && m_attackingWaitingToDie.isEmpty();
    }

    public void addAttack(Route route, Collection units)
    {
        // Filter out allied units if fourth edition
        Match ownedBy = Matches.unitIsOwnedBy(m_attacker);
        Collection attackingUnits = isFourthEdition() ? Match.getMatches(units,
                ownedBy) : units;

        Territory attackingFrom = getAttackFrom(route);

        m_attackingFrom.add(attackingFrom);

        m_attackingUnits.addAll(attackingUnits);

        if (m_attackingFromMap.get(attackingFrom) == null)
        {
            m_attackingFromMap.put(attackingFrom, new ArrayList());
        }
        {
            Collection attackingFromMapUnits = (Collection) m_attackingFromMap
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

        //mark units with no movement
        //for all but air
        Collection nonAir = Match.getMatches(attackingUnits,
                Matches.UnitIsNotAir);
        DelegateFinder.moveDelegate(m_data).markNoMovement(nonAir);

        //dependencies
        MoveDelegate moveDelegate = DelegateFinder.moveDelegate(m_data);
        // transports
        Map dependencies = transporting(units);
        // If fourth edition, allied air on our carriers are also dependents
        if (isFourthEdition())
        {
            dependencies.putAll(moveDelegate.carrierMustMoveWith(units, units));
        }

        addDependentUnits(dependencies);
    }

    private void addDependentUnits(Map dependencies)
    {
        Iterator iter = dependencies.keySet().iterator();
        while (iter.hasNext())
        {
            Unit holder = (Unit) iter.next();
            Collection transporting = (Collection) dependencies.get(holder);
            if (m_dependentUnits.get(holder) != null)
                ((Collection) m_dependentUnits.get(holder))
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

        return m_attacker.getName() + " attacks " + m_defender.getName()
                + " in " + m_battleSite.getName();
    }

    private PlayerID findDefender(Territory battleSite)
    {

        if (!battleSite.isWater())
            return battleSite.getOwner();
        //if water find the defender based on who has the most units in the
        // territory
        IntegerMap players = battleSite.getUnits().getPlayerUnitCounts();
        int max = -1;
        PlayerID defender = null;
        Iterator iter = players.keySet().iterator();
        while (iter.hasNext())
        {
            PlayerID current = (PlayerID) iter.next();
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
            throw new IllegalStateException("No defender found");

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

        bridge.getHistoryWriter().startEvent("Battle in " + m_battleSite);
        removeAirNoLongerInTerritory();

        //it is possible that no attacking units are present, if so
        //end now
        if (m_attackingUnits.size() == 0)
        {
            endBattle(bridge);
            defenderWins(bridge);
            return;
        }

        //if is possible that no defending units exist
        if (m_defendingUnits.size() == 0)
        {
            endBattle(bridge);
            attackerWins(bridge);
            return;
        }

        // Add dependent defending units to dependent unit map
        Map dependencies = transporting(m_defendingUnits);
        addDependentUnits(dependencies);

        //list the steps
        List steps = determineStepStrings(true);

        ITripleaDisplay display = getDisplay(bridge);
        display.showBattle(m_battleID, m_battleSite, getBattleTitle(), removeNonCombatants(m_attackingUnits), removeNonCombatants(m_defendingUnits), m_dependentUnits, m_attacker, m_defender);

        display.listBattleSteps(m_battleID, steps);

        //take the casualties with least movement first
        BattleCalculator.sortPreBattle(m_attackingUnits, m_data);
        //System.out.print(m_attackingUnits);
        BattleCalculator.sortPreBattle(m_defendingUnits, m_data);
        //System.out.print(m_defendingUnits);

        fightStart(bridge);
        fightLoop(bridge);
    }

    private void removeAirNoLongerInTerritory()
    {

        //remove any air units that were once in this attack, but have now
        // moved out of the territory
        //this is an ilegant way to handle this bug
        CompositeMatch airNotInTerritory = new CompositeMatchAnd();
        airNotInTerritory.add(new InverseMatch(Matches
                .unitIsInTerritory(m_battleSite)));

        m_attackingUnits.removeAll(Match.getMatches(m_attackingUnits,
                airNotInTerritory));

    }

    public List determineStepStrings(boolean showFirstRun)
    {

        List steps = new ArrayList();
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
        }

        //attacker subs
        if (m_battleSite.isWater())
        {
            if (Match.someMatch(m_attackingUnits, Matches.UnitIsSub))
            {
                steps.add(ATTACKER_SUBS_FIRE);
                steps.add(DEFENDER_SELECT_SUB_CASUALTIES);
                steps.add(DEFENDER_REMOVE_SUB_CASUALTIES);
            }
        }

        if (isFourthEdition() && m_battleSite.isWater())
        {
            if (Match.someMatch(m_defendingUnits, Matches.UnitIsSub))
            {
                steps.add(m_defender.getName() + DEFENDER_FIRES_SUBS);
                steps
                        .add(m_attacker.getName()
                                + ATTACKER_SELECT_SUB_CASUALTIES);
            }
        }

        //attacker fire
        if (Match.someMatch(m_attackingUnits, Matches.UnitIsNotSub))
        {
            steps.add(m_attacker.getName() + ATTACKER_FIRES);
            steps.add(m_defender.getName() + DEFENDER_SELECT_CASUALTIES);
        }

        //defender subs, note this happens earlier for fourth edition
        if (!isFourthEdition() && m_battleSite.isWater())
        {
            if (Match.someMatch(m_defendingUnits, Matches.UnitIsSub))
            {
                steps.add(m_defender.getName() + DEFENDER_FIRES_SUBS);
                steps
                        .add(m_attacker.getName()
                                + ATTACKER_SELECT_SUB_CASUALTIES);
            }
        }

        if (Match.someMatch(m_defendingUnits, Matches.UnitIsNotSub))
        {
            //defender fire
            steps.add(m_defender.getName() + DEFENDER_FIRES);
            steps.add(m_attacker.getName() + ATTACKER_SELECT_CASUALTIES);
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
            //not water
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
        if (canAttackerRetreat())
        {
            steps.add(m_attacker.getName() + ATTACKER_WITHDRAW);
        } else if (canAttackerRetreatPlanes())
        {
            steps.add(m_attacker.getName() + PLANES_WITHDRAW);
        }

        return steps;

    }

    private void fightStart(IDelegateBridge bridge)
    {

        fireAAGuns(bridge);
        fireNavalBombardment(bridge);
        removeNonCombatants();
    }

    private void fightLoop(IDelegateBridge bridge)
    {

        if (m_over)
            return;

        //for 4th edition we need to fire the defending subs before the
        //attacking subs fire
        //this allows the dead subs to return fire, even if they are selected
        // as casualties
        List defendingSubs = Match.getMatches(m_defendingUnits,
                Matches.UnitIsSub);

        attackSubs(bridge);

        if (isFourthEdition())
            defendSubs(bridge, defendingSubs);

        attackNonSubs(bridge);

        if (!isFourthEdition())
        {
            Collection units = new ArrayList(m_defendingUnits.size()
                    + m_defendingWaitingToDie.size());
            units.addAll(m_defendingUnits);
            units.addAll(m_defendingWaitingToDie);
            units = Match.getMatches(units, Matches.UnitIsSub);

            defendSubs(bridge, units);

        }
        defendNonSubs(bridge);

        clearWaitingToDie(bridge);

        if (m_attackingUnits.size() == 0)
        {
            endBattle(bridge);
            defenderWins(bridge);
            return;
        } else if (m_defendingUnits.size() == 0)
        {
            endBattle(bridge);
            attackerWins(bridge);
            return;
        }

        attackerRetreatSubs(bridge);
        defenderRetreatSubs(bridge);
        if (canAttackerRetreatPlanes())
            attackerRetreatPlanes(bridge);
        attackerRetreat(bridge);

        if (!m_over)
        {
            List steps = determineStepStrings(false);
            ITripleaDisplay display = getDisplay(bridge);
            display.listBattleSteps(m_battleID, steps);
        }

        m_round++;
        fightLoop(bridge);
        return;
    }

    /**
     * @param bridge
     * @return
     */
    private ITripleaDisplay getDisplay(IDelegateBridge bridge)
    {
        return (ITripleaDisplay) bridge.getDisplayChannelBroadcaster();
    }

    /**
     * @return
     */
    private boolean canAttackerRetreatPlanes()
    {
        return isFourthEdition() && m_amphibious
                && Match.someMatch(m_attackingUnits, Matches.UnitIsAir);
    }

    private Collection getAttackerRetreatTerritories()
    {
        // If attacker is all planes, just return collection of current
        // territory
        if (Match.allMatch(m_attackingUnits, Matches.UnitIsAir))
        {
            Collection oneTerritory = new ArrayList(2);
            oneTerritory.add(m_battleSite);
            return oneTerritory;
        }

        //its possible that a sub retreated to a territory we came from,
        //if so we can no longer retreat there
        Collection possible = Match.getMatches(m_attackingFrom, Matches
                .territoryHasNoEnemyUnits(m_attacker, m_data));

        // In 4th edition we need to filter out territories where only planes
        // came from since planes cannot define retreat paths
        if (isFourthEdition())
        {
            possible = Match.getMatches(possible, new Match()
            {
                public boolean match(Object obj)
                {
                    Collection units = (Collection) m_attackingFromMap.get(obj);
                    return !Match.allMatch(units, Matches.UnitIsAir);
                }
            });
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
        if (m_amphibious)
            return false;

        Collection options = getAttackerRetreatTerritories();

        if (options.size() == 0)
            return false;

        return true;
    }

    private boolean canAttackerRetreatSubs()
    {
        if (Match.someMatch(m_defendingUnits, Matches.UnitIsDestroyer))
            return false;

        return canAttackerRetreat() || canSubsSubmerge();
    }

    private void attackerRetreat(IDelegateBridge bridge)
    {
        if (!canAttackerRetreat())
            return;

        Collection possible = getAttackerRetreatTerritories();

        if (!m_over)
            queryRetreat(false, DEFAULT_RETREAT_TYPE, bridge, possible);
    }

    private void attackerRetreatSubs(IDelegateBridge bridge)
    {

        if (!canAttackerRetreatSubs())
            return;

        Collection possible = getAttackerRetreatTerritories();

        //retreat subs
        if (Match.someMatch(m_attackingUnits, Matches.UnitIsSub))
            queryRetreat(false, SUBS_RETREAT_TYPE, bridge, possible);
    }

    private void attackerRetreatPlanes(IDelegateBridge bridge)
    {
        //planes retreat to the same square the battle is in, and then should
        //move during non combat to their landing site, or be scrapped if they
        //can't find one.
        Collection possible = new ArrayList(2);
        possible.add(m_battleSite);

        //retreat planes
        if (Match.someMatch(m_attackingUnits, Matches.UnitIsAir))
            queryRetreat(false, PLANES_RETREAT_TYPE, bridge, possible);
    }

    private boolean canDefenderRetreatSubs()
    {
        if (Match.someMatch(m_attackingUnits, Matches.UnitIsDestroyer))
            return false;

        return getEmptyOrFriendlySeaNeighbors(m_defender).size() != 0
                || canSubsSubmerge();
    }

    private void defenderRetreatSubs(IDelegateBridge bridge)
    {
        if (!canDefenderRetreatSubs())
            return;

        if (!m_over)
            queryRetreat(true, SUBS_RETREAT_TYPE, bridge,
                    getEmptyOrFriendlySeaNeighbors(m_defender));
    }

    private Collection getEmptyOrFriendlySeaNeighbors(PlayerID player)
    {
        Collection possible = m_data.getMap().getNeighbors(m_battleSite);
        CompositeMatch match = new CompositeMatchAnd(Matches.TerritoryIsWater,
                Matches.territoryHasNoEnemyUnits(player, m_data));

        //make sure we can move through the any canals
        Match canalMatch = new Match()
        {
            public boolean match(Object o)
            {

                Route r = new Route();
                r.setStart(m_battleSite);
                r.add((Territory) o);
                return null == MoveValidator.validateCanal(r, m_defender,
                        m_data);
            }
        };
        match.add(canalMatch);

        possible = Match.getMatches(possible, match);
        return possible;
    }

    private void queryRetreat(boolean defender, int retreatType,
            IDelegateBridge bridge, Collection availableTerritories)
    {
        boolean subs;
        boolean planes;
        planes = retreatType == PLANES_RETREAT_TYPE;
        subs = retreatType == SUBS_RETREAT_TYPE;
        if (availableTerritories.isEmpty() && !(subs && canSubsSubmerge()))
            return;

        Collection units = defender ? m_defendingUnits : m_attackingUnits;
        if (subs)
        {
            units = Match.getMatches(units, Matches.UnitIsSub);
        } else if (planes)
        {
            units = Match.getMatches(units, Matches.UnitIsAir);
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
            if (!defender && !subs && !planes)
                m_over = true;

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
                else
                    messageLong = retreatingPlayer.getName()
                            + " retreats all units to " + retreatTo.getName();
                getDisplay(bridge).notifyRetreat(messageShort, messageLong, step, retreatingPlayer);

            }

        }
    }

    private Change retreatFromDependents(Collection units,
            IDelegateBridge bridge, Territory retreatTo)
    {
        CompositeChange change = new CompositeChange();
        Collection dependents = m_tracker.getBlocked(this);
        Iterator iter = dependents.iterator();
        while (iter.hasNext())
        {
            Battle dependent = (Battle) iter.next();
            Route route = new Route();
            route.setStart(m_battleSite);
            route.add(dependent.getTerritory());

            Collection retreatedUnits = dependent.getDependentUnits(units);

            dependent.removeAttack(route, retreatedUnits);

            Collection transports = Match.getMatches(units,
                    Matches.UnitCanTransport);

            // Put units back on their transports
            Iterator transportsIter = transports.iterator();
            while (transportsIter.hasNext())
            {

                Unit transport = (Unit) transportsIter.next();
                Collection unloaded = m_transportTracker.unloaded(transport);
                Iterator unloadedIter = unloaded.iterator();
                while (unloadedIter.hasNext())
                {

                    Unit load = (Unit) unloadedIter.next();
                    m_transportTracker.undoUnload(load, transport, m_attacker);
                }

                change.add(ChangeFactory.moveUnits(dependent.getTerritory(),
                        retreatTo, retreatedUnits));
            }
        }
        return change;
    }

    private void retreatPlanes(Collection retreating, boolean defender,
            IDelegateBridge bridge)
    {
        String transcriptText = MyFormatter.unitsToTextNoOwner(retreating)
                + " retreated";

        Collection units = defender ? m_defendingUnits : m_attackingUnits;
        /** @todo Does this need to happen with planes retreating too? */
        //DelegateFinder.moveDelegate(m_data).getSubmergedTracker().submerge(retreating);
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

    private void submergeUnits(Collection submerging, boolean defender,
            IDelegateBridge bridge)
    {
        String transcriptText = MyFormatter.unitsToTextNoOwner(submerging)
                + " Submerged";

        Collection units = defender ? m_defendingUnits : m_attackingUnits;
        DelegateFinder.moveDelegate(m_data).getSubmergedTracker().submerge(
                submerging);

        units.removeAll(submerging);
        if (units.isEmpty() || m_over)
        {
            endBattle(bridge);
            if (defender)
                attackerWins(bridge);
            else
                defenderWins(bridge);
        } else
        {
            getDisplay(bridge).notifyRetreat(m_battleID, submerging);

         }

        bridge.getHistoryWriter().addChildToEvent(transcriptText, submerging);

    }

    private void retreatUnits(Collection retreating, Territory to,
            boolean defender, IDelegateBridge bridge)
    {

        retreating.addAll(getDependentUnits(retreating));
        //our own air units dont retreat with land units
        Match notMyAir = new CompositeMatchOr(Matches.UnitIsNotAir,
                new InverseMatch(Matches.unitIsOwnedBy(m_attacker)));
        retreating = Match.getMatches(retreating, notMyAir);

        String transcriptText = MyFormatter.unitsToTextNoOwner(retreating)
                + " retreated to " + to.getName();
        bridge.getHistoryWriter().addChildToEvent(transcriptText,
                new ArrayList(retreating));

        CompositeChange change = new CompositeChange();
        change.add(ChangeFactory.moveUnits(m_battleSite, to, retreating));

        if (m_over)
        {
            change.add(retreatFromDependents(retreating, bridge, to));
        }

        bridge.addChange(change);

        Collection units = defender ? m_defendingUnits : m_attackingUnits;

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

    //the maximum number of hits that this collection of units can sustain
    //takes into account units with two hits
    public int getMaxHits(Collection units)
    {

        int count = 0;
        Iterator unitIter = units.iterator();
        while (unitIter.hasNext())
        {
            Unit unit = (Unit) unitIter.next();
            if (UnitAttatchment.get(unit.getUnitType()).isTwoHit())
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

    private void fire(final String stepName, Collection firingUnits,
            Collection attackableUnits, boolean defender,
            boolean canReturnFire, final IDelegateBridge bridge, String text)
    {

        final PlayerID firingPlayer = defender ? m_defender : m_attacker;
        final PlayerID hitPlayer = defender ? m_attacker : m_defender;

        DiceRoll dice = DiceRoll.rollDice(new ArrayList(firingUnits), defender,
                firingPlayer, bridge, m_data, this);

        int hitCount = dice.getHits();
        Collection killed;
        Collection damaged = Collections.EMPTY_LIST;
        
        getDisplay(bridge).notifyDice(m_battleID, dice, stepName);

        //they all die
        if (hitCount >= getMaxHits(attackableUnits))
        {
            killed = attackableUnits;
            getDisplay(bridge).casualtyNotification(m_battleID, stepName, dice, hitPlayer, killed, damaged, m_dependentUnits);
            getRemote(hitPlayer, bridge).confirmOwnCasualties(m_battleID, "Click to continue");
        } else
        {            
            CasualtyDetails message = selectCasualties(stepName, bridge,
                    attackableUnits, !defender, text, dice);

            killed = message.getKilled();
            damaged = message.getDamaged();

            getDisplay(bridge).casualtyNotification(m_battleID, stepName, dice, hitPlayer, killed, damaged, m_dependentUnits);
            
            //the user hasnt had a chance to see these yet
            if(message.getAutoCalculated())
                getRemote(hitPlayer, bridge).confirmOwnCasualties(m_battleID, "Click to continue");
        }

        
	    Runnable r = new Runnable()
        {
            public void run()
            {
                getRemote(firingPlayer, bridge).confirmEnemyCasualties(m_battleID, "Click to continue",  hitPlayer);
            }
        };
	    
        //execute in a seperate thread to allow either player to click continue first.
        Thread t = new Thread(r, "Click to continue waiter");
        t.start();
        try
        {
            t.join();
        } catch (InterruptedException e)
        {
           //ignore
        }
        
        
        if (damaged != null)
            markDamaged(damaged, bridge);

        removeCasualties(killed, canReturnFire, !defender, bridge);
    }

    private void defendNonSubs(IDelegateBridge bridge)
    {

        if (m_attackingUnits.size() == 0)
            return;
        Collection units = new ArrayList(m_defendingUnits.size()
                + m_defendingWaitingToDie.size());
        units.addAll(m_defendingUnits);
        units.addAll(m_defendingWaitingToDie);
        units = Match.getMatches(units, Matches.UnitIsNotSub);

        if (units.isEmpty())
            return;

        fire(m_attacker.getName() + ATTACKER_SELECT_CASUALTIES, units,
                m_attackingUnits, true, true, bridge, "Defenders fire, ");
    }

    private void attackNonSubs(IDelegateBridge bridge)
    {

        if (m_defendingUnits.size() == 0)
            return;
        Collection units = Match.getMatches(m_attackingUnits,
                Matches.UnitIsNotSub);
        units.addAll(Match.getMatches(m_attackingWaitingToDie,
                Matches.UnitIsNotSub));

        if (units.isEmpty())
            return;

        fire(m_defender.getName() + DEFENDER_SELECT_CASUALTIES, units,
                m_defendingUnits, false, true, bridge, "Attackers fire,");
    }

    private void attackSubs(IDelegateBridge bridge)
    {

        Collection firing = Match.getMatches(m_attackingUnits,
                Matches.UnitIsSub);
        if (firing.isEmpty())
            return;
        Collection attacked = Match.getMatches(m_defendingUnits,
                Matches.UnitIsNotAir);
        //if there are destroyers in the attacked units, we can return fire.
        boolean destroyersPresent = Match.someMatch(attacked,
                Matches.UnitIsDestroyer);
        fire(DEFENDER_SELECT_SUB_CASUALTIES, firing, attacked, false,
                destroyersPresent, bridge, "Subs fire,");
    }

    private void defendSubs(IDelegateBridge bridge, Collection units)
    {
        if (m_attackingUnits.size() == 0)
            return;

        if (units.isEmpty())
            return;

        Collection attacked = Match.getMatches(m_attackingUnits,
                Matches.UnitIsNotAir);
        if (attacked.isEmpty())
            return;

        boolean destroyersPresent = Match.someMatch(attacked,
                Matches.UnitIsDestroyer);
        fire(m_attacker.getName() + ATTACKER_SELECT_SUB_CASUALTIES, units,
                attacked, true, destroyersPresent, bridge, "Subs defend, ");
    }

    private CasualtyDetails selectCasualties(String step,
            IDelegateBridge bridge, Collection attackableUnits,
            boolean defender, String text, DiceRoll dice)
    {

        PlayerID hit = defender ? m_defender : m_attacker;
        return BattleCalculator.selectCasualties(step, hit, attackableUnits,
                bridge, text, m_data, dice, defender);
    }

    private void removeCasualties(Collection killed, boolean canReturnFire,
            boolean defender, IDelegateBridge bridge)
    {

        if (canReturnFire)
        {
            //move to waiting to die
            if (defender)
                m_defendingWaitingToDie.addAll(killed);
            else
                m_attackingWaitingToDie.addAll(killed);
        } else

            //remove immediately
            remove(killed, bridge);

        //remove from the active fighting
        if (defender)
            m_defendingUnits.removeAll(killed);
        else
            m_attackingUnits.removeAll(killed);
    }

    private void fireNavalBombardment(IDelegateBridge bridge)
    {

        Collection bombard = getBombardingUnits();
        Collection attacked = Match.getMatches(m_defendingUnits,
                Matches.UnitIsDestructible);

        //bombarding units cant move after bombarding
        DelegateFinder.moveDelegate(m_data).markNoMovement(bombard);

        //4th edition, bombardment casualties cant return fire
        boolean canReturnFire = !isFourthEdition();

        if (bombard.size() > 0 && attacked.size() > 0)
        {
            fire(SELECT_NAVAL_BOMBARDMENT_CASUALTIES, bombard, attacked, false,
                    canReturnFire, bridge, "Bombard");
        }

    }

    /**
     * @return
     */
    private boolean isFourthEdition()
    {
        return m_data.getProperties().get(Constants.FOURTH_EDITION, false);
    }

    /**
     * Return the territories where there are amphibious attacks.
     */
    public Collection getAmphibiousAttackTerritories()
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
    private Collection getBombardingUnits()
    {
        return m_bombardingUnits;
    }

    private void fireAAGuns(final IDelegateBridge bridge)
    {

        final String step = SELECT_AA_CASUALTIES;
        if (!canFireAA())
            return;

        int attackingAirCount = Match.countMatches(m_attackingUnits,
                Matches.UnitIsAir);
        //DiceRoll dice = DiceRoll.rollAA(attackingAirCount, bridge);
        // NEW VERSION
        DiceRoll dice = DiceRoll.rollAA(attackingAirCount, bridge,
                m_battleSite, m_data);

        //send attacker the dice roll so he can see what the dice are while he
        // waits for
        //attacker to select casualties
        getDisplay(bridge).notifyDice(m_battleID,  dice, step);

        Collection casualties = null;
        Collection attackable = Match.getMatches(m_attackingUnits,
                Matches.UnitIsAir);
        
        // if 4th edition choose casualties randomnly
        // we can do that by removing planes at positions in the list where
        // there was a corresponding hit in the dice roll.
        if (isFourthEdition())
        {
            casualties = BattleCalculator.fourthEditionAACasualties(attackable,
                    dice, bridge);
           
        } else
        {
            casualties = selectCasualties(step, bridge, attackable, false,
                    "AA guns fire,", dice).getKilled();
        }

        getDisplay(bridge).casualtyNotification(m_battleID, step,dice, m_attacker, casualties, Collections.EMPTY_LIST, m_dependentUnits);
        
        getRemote(m_attacker, bridge).confirmOwnCasualties(m_battleID, "Click to continue");
        Runnable r = new Runnable()
        {
            public void run()
            {
                getRemote(m_defender, bridge).confirmEnemyCasualties(m_battleID, "Click to continue", m_attacker);        
            }
        };
        Thread t = new Thread(r, "click to continue waiter");
        t.start();
        try
        {
            t.join();
        } catch (InterruptedException e)
        {
          //ignore
        }
        
        
        removeCasualties(casualties, false, false, bridge);

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
    private List removeNonCombatants(Collection units)
    {

        CompositeMatch combat = new CompositeMatchAnd();
        combat.add(new InverseMatch(Matches.UnitIsAAOrFactory));

        if (m_battleSite.isWater())
            combat.add(new InverseMatch(Matches.UnitIsLand));

        return Match.getMatches(units, combat);

    }

    private void removeNonCombatants()
    {

        m_defendingUnits = removeNonCombatants(m_defendingUnits);
        m_attackingUnits = removeNonCombatants(m_attackingUnits);
    }

    public Collection getDependentUnits(Collection units)
    {

        Iterator iter = units.iterator();
        Collection dependents = new ArrayList();
        while (iter.hasNext())
        {
            Collection depending = (Collection) m_dependentUnits.get(iter
                    .next());
            if (depending != null)
            {
                dependents.addAll(depending);
            }
        }
        return dependents;
    }

    private void markDamaged(Collection damaged, IDelegateBridge bridge)
    {

        if (damaged.size() == 0)
            return;
        Change damagedChange = null;
        IntegerMap damagedMap = new IntegerMap();
        damagedMap.putAll(damaged, 1);
        damagedChange = ChangeFactory.unitsHit(damagedMap);
        bridge.getHistoryWriter().addChildToEvent(
                "Units damaged:" + MyFormatter.unitsToTextNoOwner(damaged),
                damaged);
        bridge.addChange(damagedChange);

    }

    private void remove(Collection killed, IDelegateBridge bridge)
    {
        if (killed.size() == 0)
            return;

        //get the transported units
        if (m_battleSite.isWater())
        {
            Collection dependent = getDependentUnits(killed);
            killed.addAll(dependent);
        }
        Change killedChange = ChangeFactory.removeUnits(m_battleSite, killed);
        m_killed.addAll(killed);

        String transcriptText = MyFormatter.unitsToText(killed) + " lost in "
                + m_battleSite.getName();
        bridge.getHistoryWriter().addChildToEvent(transcriptText, killed);

        bridge.addChange(killedChange);
        removeFromDependents(killed, bridge);

    }

    private void removeFromDependents(Collection units, IDelegateBridge bridge)
    {

        Collection dependents = m_tracker.getBlocked(this);
        Iterator iter = dependents.iterator();
        while (iter.hasNext())
        {
            Battle dependent = (Battle) iter.next();
            dependent.unitsLost(this, units, bridge);
        }
    }

    private void clearWaitingToDie(IDelegateBridge bridge)
    {

        Collection units = new ArrayList();
        units.addAll(m_attackingWaitingToDie);
        units.addAll(m_defendingWaitingToDie);
        remove(units, bridge);
        m_defendingWaitingToDie.clear();
        m_attackingWaitingToDie.clear();
    }

    private void defenderWins(IDelegateBridge bridge)
    {
        getDisplay(bridge).battleEnd(m_battleID, m_defender.getName()+ " win");
        
        bridge.getHistoryWriter()
                .addChildToEvent(m_defender.getName() + " win");
        showCasualties(bridge);

        checkDefendingPlanesCanLand(bridge, m_defender);

    }
    
    private ITripleaPlayer getRemote(PlayerID player, IDelegateBridge bridge)
    {
        //if its the null player, return a do nothing proxy
        if(player.isNull())
            return (ITripleaPlayer) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class[] {ITripleaPlayer.class}, new NullInvocationHandler());
        return (ITripleaPlayer) bridge.getRemote(player);
    }

    /**
     * The defender has won, but there may be defending fighters that cant stay
     * in the sea zone due to insufficient carriers.
     */
    private void checkDefendingPlanesCanLand(IDelegateBridge bridge,
            PlayerID defender)
    {

        //not water, not relevant.
        if (!m_battleSite.isWater())
            return;

        CompositeMatch alliedDefendingAir = new CompositeMatchAnd(
                Matches.UnitIsAir, Matches.isUnitAllied(m_defender, m_data));
        Collection defendingAir = Match.getMatches(m_defendingUnits,
                alliedDefendingAir);
        //ne defending air
        if (defendingAir.isEmpty())
            return;

        int carrierCost = MoveValidator.carrierCost(defendingAir);
        int carrierCapacity = MoveValidator.carrierCapacity(m_defendingUnits);

        if (carrierCapacity >= carrierCost)
            return;

        //find out what we must remove
        //remove all the air that can land on carriers from defendingAir
        carrierCost = 0;
        Iterator defendingAirIter = new ArrayList(defendingAir).iterator();
        while (defendingAirIter.hasNext() && carrierCapacity >= carrierCost)
        {
            Unit currentUnit = (Unit) defendingAirIter.next();
            carrierCost += UnitAttatchment.get(currentUnit.getType())
                    .getCarrierCost();
            if (carrierCapacity >= carrierCost)
            {
                defendingAir.remove(currentUnit);
            }
        }

        // Get land territories where air can land
        Set neighbors = m_data.getMap().getNeighbors(m_battleSite);
        CompositeMatch alliedLandTerritories = new CompositeMatchAnd(
                Matches.TerritoryIsLand, Matches.isTerritoryAllied(m_defender,
                        m_data));
        Collection canLandHere = Match.getMatches(neighbors,
                alliedLandTerritories);

        // If fourth edition we need an adjacent land, while classic requires
        // an island inside the seazone.
        if (isFourthEdition() && canLandHere.size() > 0)
        {
            Territory territory = null;
            if (canLandHere.size() > 1)
            {
                territory = getRemote(m_defender, bridge).selectTerritoryForAirToLand(canLandHere);
            } else
            {
                territory = (Territory) canLandHere.iterator().next();
            }
            bridge.getHistoryWriter().addChildToEvent(
                    MyFormatter.unitsToText(defendingAir) + " forced to land in "
                            + territory.getName(), defendingAir);
            Change change = ChangeFactory.moveUnits(m_battleSite, territory,
                    defendingAir);
            bridge.addChange(change);
            return;
        } else if (canLandHere.size() > 0)
        {   // 2nd edition
            //now defending air has what cant stay, is there a place we can go?
            //check for an island in this sea zone
            Iterator neighborsIter = canLandHere.iterator();
            while (neighborsIter.hasNext())
            {
                Territory currentTerritory = (Territory) neighborsIter.next();
                //only one neighbor, its an island.
                if (m_data.getMap().getNeighbors(currentTerritory).size() == 1)
                {
                    bridge.getHistoryWriter().addChildToEvent(
                            MyFormatter.unitsToText(defendingAir)
                                    + " forced to land in "
                                    + currentTerritory.getName(), defendingAir);
                    Change change = ChangeFactory.moveUnits(m_battleSite,
                            currentTerritory, defendingAir);
                    bridge.addChange(change);
                    return;
                }
            }
        }

        //no were to go, they must die
        bridge.getHistoryWriter().addChildToEvent(
                MyFormatter.unitsToText(defendingAir)
                        + " could not land and were killed", defendingAir);
        Change change = ChangeFactory.removeUnits(m_battleSite, defendingAir);
        bridge.addChange(change);
    }

    private void attackerWins(IDelegateBridge bridge)
    {
        getDisplay(bridge).battleEnd(m_battleID, m_attacker.getName() + " win");

        //do we need to change ownership
        if (!m_battleSite.isWater())
        {

            if (Match.someMatch(m_attackingUnits, Matches.UnitIsNotAir))
            {
                m_tracker.addToConquered(m_battleSite);
                m_tracker.takeOver(m_battleSite, m_attacker, bridge, m_data,
                        null);
            }
        }

        bridge.getHistoryWriter()
                .addChildToEvent(m_attacker.getName() + " win");
        showCasualties(bridge);
    }

    private void showCasualties(IDelegateBridge bridge)
    {
        if (m_killed.isEmpty())
            return;
        //a handy summary of all the units killed
        IntegerMap costs = BattleCalculator.getCosts(m_attacker, m_data);
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

    public String toString()
    {

        return "Battle in:" + m_battleSite + " attacked by:" + m_attackingUnits
                + " from:" + m_attackingFrom + " defender:"
                + m_defender.getName() + " bombing:" + isBombingRun();
    }

    public Collection getAttackingUnits()
    {
        return m_attackingUnits;
    }

    public void unitsLost(Battle battle, Collection units,
            IDelegateBridge bridge)
    {

        Collection lost = getDependentUnits(units);

        //if all the amphibious attacking land units are lost, then we are
        //no longer a naval invasion
        m_amphibiousLandAttackers.removeAll(lost);
        if (m_amphibiousLandAttackers.isEmpty())
        {
            m_amphibious = false;
            m_bombardingUnits.clear();
        }

        m_attackingUnits.removeAll(lost);
        remove(lost, bridge);

        if (m_attackingUnits.isEmpty())
            m_tracker.removeBattle(this);
    }

    /**
     * Returns a map of transport -> collection of transported units.
     */
    private Map transporting(Collection units)
    {

        return m_transportTracker.transporting(units);
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


class NullInvocationHandler implements InvocationHandler
{

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
    {
        return null;
    }
}