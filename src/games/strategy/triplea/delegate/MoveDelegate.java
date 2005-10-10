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
 * MoveDelegate.java
 * 
 * Created on November 2, 2001, 12:24 PM
 */

package games.strategy.triplea.delegate;

import games.strategy.engine.data.*;
import games.strategy.engine.delegate.*;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.triplea.delegate.dataObjects.*;
import games.strategy.triplea.delegate.remote.IMoveDelegate;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.triplea.player.ITripleaPlayer;
import games.strategy.util.*;

import java.io.Serializable;
import java.util.*;

/**
 * 
 * Responsible for moving units on the board.
 * <p>
 * Responible for checking the validity of a move, and for moving the units.
 * <br>
 * 
 * @author Sean Bridges
 * @version 1.0
 *  
 */
public class MoveDelegate implements ISaveableDelegate, IMoveDelegate
{

    private static final String CANT_MOVE_THROUGH_IMPASSIBLE = "Can't move through impassible territories";
    private static final String TOO_POOR_TO_VIOLATE_NEUTRALITY = "Not enough money to pay for violating neutrality";
    
    private String m_name;
    private String m_displayName;
    private IDelegateBridge m_bridge;
    private GameData m_data;
    private PlayerID m_player;
    private boolean m_firstRun = true;
    private boolean m_nonCombat;
    private TransportTracker m_transportTracker = new TransportTracker();
    private IntegerMap<Unit> m_alreadyMoved = new IntegerMap<Unit>();
    private IntegerMap<Territory> m_ipcsLost = new IntegerMap<Territory>();
    private SubmergedTracker m_submergedTracker = new SubmergedTracker();

    // A collection of UndoableMoves
    private List<UndoableMove> m_movesToUndo = new ArrayList<UndoableMove>();

    //The current move
    private UndoableMove m_currentMove;

    /** Creates new MoveDelegate */
    public MoveDelegate()
    {

    }

    public void initialize(String name, String displayName)
    {
        m_name = name;
        m_displayName = displayName;
    }

    /**
     * Want to make sure that all units in the sea that can be transported are
     * marked as being transported by something.
     * 
     * We assume that all transportable units in the sea are in a transport, no
     * exceptions.
     *  
     */
    private void firstRun()
    {
        m_firstRun = false;
        //check every territory
        Iterator allTerritories = m_data.getMap().getTerritories().iterator();
        while (allTerritories.hasNext())
        {
            Territory current = (Territory) allTerritories.next();
            //only care about water
            if (!current.isWater())
                continue;

            Collection<Unit> units = current.getUnits().getUnits();
            if (units.size() == 0 || !Match.someMatch(units, Matches.UnitIsLand))
                continue;

            //map transports, try to fill
            Collection transports = Match.getMatches(units, Matches.UnitIsTransport);
            Collection land = Match.getMatches(units, Matches.UnitIsLand);
            Iterator landIter = land.iterator();
            while (landIter.hasNext())
            {
                Unit toLoad = (Unit) landIter.next();
                UnitAttachment ua = UnitAttachment.get(toLoad.getType());
                int cost = ua.getTransportCost();
                if (cost == -1)
                    throw new IllegalStateException("Non transportable unit in sea");

                //find the next transport that can hold it
                Iterator transportIter = transports.iterator();
                boolean found = false;
                while (transportIter.hasNext())
                {
                    Unit transport = (Unit) transportIter.next();
                    int capacity = m_transportTracker.getAvailableCapacity(transport);
                    if (capacity >= cost)
                    {
                        m_transportTracker.load(toLoad, transport, m_currentMove, m_player);
                        found = true;
                        break;
                    }
                }
                if (!found)
                    throw new IllegalStateException("Cannot load all units");
            }
        }
    }

    /**
     * Called before the delegate will run.
     */
    public void start(IDelegateBridge aBridge, GameData gameData)
    {
        if (aBridge.getStepName().endsWith("NonCombatMove"))
            m_nonCombat = true;
        else if (aBridge.getStepName().endsWith("CombatMove"))
            m_nonCombat = false;
        else
            throw new IllegalStateException("Cannot determine combat or not");

        m_bridge = aBridge;
        PlayerID player = aBridge.getPlayerID();

        m_data = gameData;
        m_player = player;

        if (m_firstRun)
            firstRun();
    }

    public String getName()
    {

        return m_name;
    }

    public String getDisplayName()
    {

        return m_displayName;
    }

    public List<UndoableMove> getMovesMade()
    {
        return new ArrayList<UndoableMove>(m_movesToUndo);
    }

    public String undoMove(final int moveIndex)
    {

        if (m_movesToUndo.isEmpty())
            return "No moves to undo";
        if (moveIndex >= m_movesToUndo.size())
            return "Undo move index out of range";

        UndoableMove moveToUndo = m_movesToUndo.get(moveIndex);

        if (!moveToUndo.getcanUndo())
            return moveToUndo.getReasonCantUndo();

        moveToUndo.undo(m_bridge, m_alreadyMoved, m_data);
        m_movesToUndo.remove(moveIndex);
        updateUndoableMoveIndexes();

        return null;
    }

    private void updateUndoableMoveIndexes()
    {

        for (int i = 0; i < m_movesToUndo.size(); i++)
        {
            m_movesToUndo.get(i).setIndex(i);
        }
    }

    public MustMoveWithDetails getMustMoveWith(Territory start, Collection<Unit> units)
    {
        return new MustMoveWithDetails(mustMoveWith(units, start), movementLeft(units));
    }

    private IntegerMap<Unit> movementLeft(Collection<Unit> units)
    {

        IntegerMap<Unit> movement = new IntegerMap<Unit>();

        Iterator<Unit> iter = units.iterator();
        while (iter.hasNext())
        {
            Unit current = iter.next();
            movement.put(current, MoveValidator.movementLeft(current, m_alreadyMoved));
        }

        return movement;
    }

    private Map<Unit, Collection<Unit>> mustMoveWith(Collection<Unit> units, Territory start)
    {

        List<Unit> sortedUnits = new ArrayList<Unit>(units);

        Collections.sort(sortedUnits, increasingMovement);

        Map<Unit, Collection<Unit>> mapping = new HashMap<Unit, Collection<Unit>>();
        mapping.putAll(transportsMustMoveWith(sortedUnits));
        mapping.putAll(carrierMustMoveWith(sortedUnits, start));
        return mapping;
    }

    private Map<Unit, Collection<Unit>> transportsMustMoveWith(Collection<Unit> units)
    {

        Map<Unit, Collection<Unit>> mustMoveWith = new HashMap<Unit, Collection<Unit>>();
        //map transports
        Collection<Unit> transports = Match.getMatches(units, Matches.UnitIsTransport);
        Iterator<Unit> iter = transports.iterator();
        while (iter.hasNext())
        {
            Unit transport = iter.next();
            Collection<Unit> transporting = m_transportTracker.transporting(transport);
            mustMoveWith.put(transport, transporting);
        }
        return mustMoveWith;
    }

    private Map<Unit, Collection<Unit>> carrierMustMoveWith(Collection<Unit> units, Territory start)
    {
        return carrierMustMoveWith(units, start.getUnits().getUnits(), m_data, m_player);
    }

    public static Map<Unit, Collection<Unit>> carrierMustMoveWith(Collection<Unit> units, Collection<Unit> startUnits, GameData data, PlayerID player)
    {

        //we want to get all air units that are owned by our allies
        //but not us that can land on a carrier
        CompositeMatch<Unit> friendlyNotOwnedAir = new CompositeMatchAnd<Unit>();
        friendlyNotOwnedAir.add(Matches.alliedUnit(player, data));
        friendlyNotOwnedAir.addInverse(Matches.unitIsOwnedBy(player));
        friendlyNotOwnedAir.add(Matches.UnitCanLandOnCarrier);

        Collection<Unit> alliedAir = Match.getMatches(startUnits, friendlyNotOwnedAir);

        if (alliedAir.isEmpty())
            return Collections.emptyMap();

        //remove air that can be carried by allied
        CompositeMatch<Unit> friendlyNotOwnedCarrier = new CompositeMatchAnd<Unit>();
        friendlyNotOwnedCarrier.add(Matches.UnitIsCarrier);
        friendlyNotOwnedCarrier.add(Matches.alliedUnit(player, data));
        friendlyNotOwnedCarrier.addInverse(Matches.unitIsOwnedBy(player));

        Collection<Unit> alliedCarrier = Match.getMatches(startUnits, friendlyNotOwnedCarrier);

        Iterator<Unit> alliedCarrierIter = alliedCarrier.iterator();
        while (alliedCarrierIter.hasNext())
        {
            Unit carrier = alliedCarrierIter.next();
            Collection<Unit> carrying = getCanCarry(carrier, alliedAir);
            alliedAir.removeAll(carrying);
        }

        if (alliedAir.isEmpty())
            return Collections.emptyMap();

        Map<Unit, Collection<Unit>> mapping = new HashMap<Unit, Collection<Unit>>();
        //get air that must be carried by our carriers
        Collection<Unit> ownedCarrier = Match.getMatches(units, Matches.UnitIsCarrier);

        Iterator<Unit> ownedCarrierIter = ownedCarrier.iterator();
        while (ownedCarrierIter.hasNext())
        {
            Unit carrier = (Unit) ownedCarrierIter.next();
            Collection<Unit> carrying = getCanCarry(carrier, alliedAir);
            alliedAir.removeAll(carrying);

            mapping.put(carrier, carrying);
        }

        return mapping;
    }

    private static Collection<Unit> getCanCarry(Unit carrier, Collection<Unit> selectFrom)
    {

        UnitAttachment ua = UnitAttachment.get(carrier.getUnitType());
        Collection<Unit> canCarry = new ArrayList<Unit>();

        int available = ua.getCarrierCapacity();
        Iterator iter = selectFrom.iterator();
        while (iter.hasNext())
        {
            Unit plane = (Unit) iter.next();
            UnitAttachment planeAttatchment = UnitAttachment.get(plane.getUnitType());
            int cost = planeAttatchment.getCarrierCost();
            if (available >= cost)
            {
                available -= cost;
                canCarry.add(plane);
            }
            if (available == 0)
                break;
        }
        return canCarry;
    }

    @SuppressWarnings("unchecked")
    public String move(Collection<Unit> units, Route route)
    {
        return move(units, route, Collections.EMPTY_LIST);
    }

    public String move(Collection<Unit> units, Route route, Collection<Unit> transportsThatCanBeLoaded)
    {
        String error = validateMove(units, route, m_player, transportsThatCanBeLoaded);
        if (error != null)
            return error;
        
        //allow user to cancel move if aa guns will fire
        Collection aaFiringTerritores = getTerritoriesWhereAAWillFire(route, units);
        if(!aaFiringTerritores.isEmpty())
        {
            if(!getRemotePlayer().confirmMoveInFaceOfAA(aaFiringTerritores))
                return null;
        }
        
        //do the move
        m_currentMove = new UndoableMove(m_data, m_alreadyMoved, units, route);

        String transcriptText = MyFormatter.unitsToTextNoOwner(units) + " moved from " + route.getStart().getName() + " to " + route.getEnd().getName();
        m_bridge.getHistoryWriter().startEvent(transcriptText);
        MoveDescription description = new MoveDescription(units, route);
        m_bridge.getHistoryWriter().setRenderingData(description);

        moveUnits(units, route, m_player, transportsThatCanBeLoaded);

        m_currentMove.markEndMovement(m_alreadyMoved);
        m_currentMove.initializeDependencies(m_movesToUndo);
        m_movesToUndo.add(m_currentMove);
        updateUndoableMoveIndexes();

        return null;
    }

    private String validateMove(Collection<Unit> units, Route route, PlayerID player, Collection<Unit> transportsToLoad)
    {

        String error;

        if (m_nonCombat)
        {
            error = validateNonCombat(units, route, player);
            if (error != null)
                return error;
        }

        if (!m_nonCombat)
        {
            error = validateCombat(units, route, player);
            if (error != null)
                return error;
        }

        error = validateNonEnemyUnitsOnPath(units, route, player);
        if (error != null)
            return error;

        error = validateBasic(units, route, player, transportsToLoad);
        if (error != null)
            return error;

        error = validateAirCanLand(units, route, player);
        if (error != null)
            return error;

        error = validateTransport(units, route, player, transportsToLoad);
        if (error != null)
            return error;

        error = validateCanal(units, route, player);
        if (error != null)
            return error;


        //dont let the user move out of a battle zone
        //the exception is air units and unloading units into a battle zone
        if (getBattleTracker().hasPendingBattle(route.getStart(), false)
                && Match.someMatch(units, Matches.UnitIsNotAir))
        {
            boolean unload = MoveValidator.isUnload(route);
            PlayerID endOwner = route.getEnd().getOwner();
            boolean attack = !m_data.getAllianceTracker().isAllied(endOwner, m_player) || getBattleTracker().wasConquered(route.getEnd());
            //unless they are unloading into another battle
            if (!(unload && attack))
                return "Cant move units out of battle zone";
        }

        //make sure we can afford to pay neutral fees
        int cost = getNeutralCharge(route);
        int resources = player.getResources().getQuantity(Constants.IPCS);
        if (resources - cost < 0)
        {
            return TOO_POOR_TO_VIOLATE_NEUTRALITY;
        }

        return null;
    }

    private String validateCanal(Collection<Unit> units, Route route, PlayerID player)
    {

        //if no sea units then we can move
        if (Match.noneMatch(units, Matches.UnitIsSea))
            return null;

        return MoveValidator.validateCanal(route, player, m_data);
    }

    private String validateCombat(Collection<Unit> units, Route route, PlayerID player)
    {

        // Don't allow aa guns to move in non-combat unless they are in a
        // transport
        if (Match.someMatch(units, Matches.UnitIsAA) && (!route.getStart().isWater() || !route.getEnd().isWater()))
            return "Cant move aa guns in combat movement phase";
        return null;
    }

    private String validateNonCombat(Collection<Unit> units, Route route, PlayerID player)
    {

        if (route.someMatch(Matches.TerritoryIsImpassible))
        {
          return CANT_MOVE_THROUGH_IMPASSIBLE;
        }

        CompositeMatch<Territory> battle = new CompositeMatchOr<Territory>();
        battle.add(Matches.TerritoryIsNuetral);
        battle.add(Matches.isTerritoryEnemyAndNotNeutral(player, m_data));

        if (battle.match(route.getEnd()))
        {
            return "Cant advance units to battle in non combat";
        }

        if (route.getEnd().getUnits().someMatch(Matches.enemyUnit(player, m_data)))
        {
            CompositeMatch<Unit> friendlyOrSubmerged = new CompositeMatchOr<Unit>();
            friendlyOrSubmerged.add(Matches.alliedUnit(m_player, m_data));
            friendlyOrSubmerged.add(Matches.unitIsSubmerged(m_data));
            if (!route.getEnd().getUnits().allMatch(friendlyOrSubmerged))
            {
                return "Cant advance to battle in non combat";
            }
        }

        if (Match.allMatch(units, Matches.UnitIsAir))
        {
            if (route.someMatch(Matches.TerritoryIsNuetral))
            {
                    return "Air units cannot fly over neutral territories in non combat";
            }
        } else
        {
            CompositeMatch<Territory> neutralOrEnemy = new CompositeMatchOr<Territory>(Matches.TerritoryIsNuetral, Matches.isTerritoryEnemyAndNotNeutral(player, m_data));
            if (route.someMatch(neutralOrEnemy))
                return "Cant move units to neutral or enemy territories in non combat";
        }
        return null;
    }

    private String validateNonEnemyUnitsOnPath(Collection<Unit> units, Route route, PlayerID player)
    {
        //check to see no enemy units on path
        if (MoveValidator.onlyAlliedUnitsOnPath(route, player, m_data))
            return null;

        //if we are all air, then its ok
        if (Match.allMatch(units, Matches.UnitIsAir))
            return null;

        boolean submersibleSubsAllowed = m_data.getProperties().get(Constants.SUBMERSIBLE_SUBS, false);

        if (submersibleSubsAllowed && Match.allMatch(units, Matches.UnitIsSub))
        {
            //this is ok unless there are destroyer on the path
            if (MoveValidator.enemyDestroyerOnPath(route, player, m_data))
            {
                return "Cant move submarines under destroyers";
            } else
                return null;
        }

        return "Enemy units on path";
    }

    private String validateBasic(Collection<Unit> units, Route route, PlayerID player, Collection<Unit> transportsToLoad)
    {

        if (m_submergedTracker.areAnySubmerged(units))
        {
            return "You cannot move submerged units";
        }

        //make sure all units are actually in the start territory
        if (!route.getStart().getUnits().containsAll(units))
        {
            return "Not enough units in starting territory";
        }

        //make sure transports in the destination
        if (!route.getEnd().getUnits().containsAll(transportsToLoad))
        {
            return "Transports not found in route end";
        }

        //make sure all units are at least friendly
        if (!Match.allMatch(units, Matches.alliedUnit(m_player, m_data)))
            return "Can only move friendly units";

        //check we have enough movement
        //exclude transported units
        Collection<Unit> moveTest;
        if (route.getStart().isWater())
        {
            moveTest = MoveValidator.getNonLand(units);
        } else
        {
            moveTest = units;
        }
        if (!MoveValidator.hasEnoughMovement(moveTest, m_alreadyMoved, route.getLength()))
            return "Units do not enough movement";

        //if there is a nuetral in the middle must stop unless all are air
        if (MoveValidator.hasNuetralBeforeEnd(route))
        {
            if (!Match.allMatch(units, Matches.UnitIsAir))
                return "Must stop land units when passing through nuetral territories";
        }

        if (!m_nonCombat && Match.someMatch(units, Matches.UnitIsLand) && route.getLength() >= 1)
        {
            //check all the territories but the end,
            //if there are enemy territories, make sure they are blitzable
            //if they are not blitzable, or we arent all blit units
            //fail
            int enemyCount = 0;
            boolean allEnemyBlitzable = true;

            for (int i = 0; i < route.getLength() - 1; i++)
            {
                Territory current = route.at(i);

                if (current.isWater())
                    continue;

                if (!m_data.getAllianceTracker().isAllied(current.getOwner(), m_player)
                        || getBattleTracker().wasConquered(current))
                {
                    enemyCount++;
                    allEnemyBlitzable &= MoveValidator.isBlitzable(current, m_data, m_player);
                }
            }

            if (enemyCount > 0 && !allEnemyBlitzable)
            {
                return "Cant blitz on that route";
            } else if (enemyCount > 0 && allEnemyBlitzable)
            {
                Match<Unit> blitzingUnit = new CompositeMatchOr<Unit>(Matches.UnitCanBlitz, Matches.UnitIsAir);
                if (!Match.allMatch(units, blitzingUnit))
                    return "Not all units can blitz";
            }

        }

        //make sure no conquered territories on route
        if (MoveValidator.hasConqueredNonBlitzedOnRoute(route, m_data))
        {
            //unless we are all air or we are in non combat
            if (!Match.allMatch(units, Matches.UnitIsAir) && !m_nonCombat)
                return "Cannot move through newly captured territories";
        }

        //make sure that no non sea non transportable no carriable units
        //end at sea
        if (route.getEnd().isWater())
        {
            if (MoveValidator.hasUnitsThatCantGoOnWater(units))
                return "Those units cannot end at water";
        }

        //if we are water make sure no land
        if (Match.someMatch(units, Matches.UnitIsSea))
        {
            if (MoveValidator.hasLand(route))
                return "Sea units cant go on land";
        }

        //make sure that we dont send aa guns to attack
        if (Match.someMatch(units, Matches.UnitIsAA))
        {
            //TODO
            //dont move if some were conquered

            for (int i = 0; i < route.getLength(); i++)
            {
                Territory current = route.at(i);
                if (!(current.isWater() || current.getOwner().equals(m_player) || m_data.getAllianceTracker().isAllied(m_player, current.getOwner())))

                    return "AA units cant advance to battle";

            }
        }

        //only allow aa into a terland territory if one already present.
        if (!isFourEdition() && Match.someMatch(units, Matches.UnitIsAA) && route.getEnd().getUnits().someMatch(Matches.UnitIsAA)
                && !route.getEnd().isWater())
        {
            return "Only one AA gun allowed in a territroy";
        }

        //only allow 1 aa to unload
        if (route.getStart().isWater() && !route.getEnd().isWater() && Match.countMatches(units, Matches.UnitIsAA) > 1)
        {
            return "Only 1 AA gun allowed in a territory";
        }

        // don't allow move through impassible territories
        if (route.someMatch(Matches.TerritoryIsImpassible))
        {
          return CANT_MOVE_THROUGH_IMPASSIBLE;
        }

        String errorMsg = canCrossNeutralTerritory(route, player);
        if (errorMsg != null)
        {
            return errorMsg;
        }

        return null;
    }

    private BattleTracker getBattleTracker()
    {
        return DelegateFinder.battleDelegate(m_data).getBattleTracker();
    }

    private boolean isFourEdition()
    {
        return m_data.getProperties().get(Constants.FOURTH_EDITION, false);
    }

    private String validateAirCanLand(Collection<Unit> units, Route route, PlayerID player)
    {
        //nothing to check
        if (!Match.someMatch(units, Matches.UnitIsAir))
            return null;

        //these is a place where we can land
        //must be friendly and non conqueuerd land
        CompositeMatch<Territory> friendlyGround = new CompositeMatchAnd<Territory>();
        friendlyGround.add(Matches.isTerritoryAllied(m_player, m_data));
        friendlyGround.add(new Match<Territory>() 
                {
            		public boolean match(Territory o)
            		{
            		    return !getBattleTracker().wasConquered((Territory) o);
            		}
                }
        );
        friendlyGround.add(new Match<Territory>() 
                {
            		public boolean match(Territory o)
            		{
            		    return !getBattleTracker().hasPendingBattle((Territory) o, false);
            		}
                }
        );
        friendlyGround.add(Matches.TerritoryIsLand);
        
        
        //we can land at the end, nothing left to check
        if(friendlyGround.match(route.getEnd()))
            return null;
        

        //this is the farthese we need to look for places to land
        //the fighters cant move farther than this
        //note that this doesnt take into account the movement used to move the
        //units along the route
        int maxMovement = MoveValidator.getMaxMovement(units, m_alreadyMoved);
        
        Match<Territory> canMoveThrough = new InverseMatch<Territory>(Matches.TerritoryIsImpassible);
        Match<Territory> notNeutral = new InverseMatch<Territory>(Matches.TerritoryIsNuetral);
        
        Match<Territory> notNeutralAndNotImpassible = new CompositeMatchAnd<Territory>(canMoveThrough, notNeutral);
        
        //find the closest land territory where everyone can land
        int closesetLandTerritory = Integer.MAX_VALUE;
        
        Iterator iter = m_data.getMap().getNeighbors(route.getEnd(), maxMovement).iterator();
    
        while (iter.hasNext())
        {

            Territory territory = (Territory) iter.next();
        
            //can we land there?
            if(!friendlyGround.match(territory))
                continue;
            
            //do we have an easy path to get there
            //can we do it without violating neutrals
            Route noNeutralRoute =m_data.getMap().getRoute(route.getEnd(), territory, notNeutralAndNotImpassible); 
            if(noNeutralRoute != null)
            {
                closesetLandTerritory = Math.min(closesetLandTerritory, noNeutralRoute.getLength());
            }
            //can we find a path with neutrals?
            //can we afford this path?
            Route neutralViolatingRoute = m_data.getMap().getRoute(route.getEnd(), territory, notNeutral);
            if((neutralViolatingRoute != null) && getNeutralCharge(neutralViolatingRoute) <= m_player.getResources().getQuantity(Constants.IPCS))
            {
                closesetLandTerritory = Math.min(closesetLandTerritory, neutralViolatingRoute.getLength());                    
            }
        }
        
        //these rae the units we have to be sure that can land somewhere
        Match<Unit> ownedAirMatch = new CompositeMatchAnd<Unit>(Matches.UnitIsAir, Matches.unitOwnedBy(m_player) );
        Collection<Unit> ownedAir = new ArrayList<Unit>();
        ownedAir.addAll( Match.getMatches(units, ownedAirMatch ));
        ownedAir.addAll(Match.getMatches( route.getEnd().getUnits().getUnits(), ownedAirMatch ));

        
        //find out how much movement we have left  
        IntegerMap<Unit> movementLeft = new IntegerMap<Unit>();
        Iterator ownedAirIter = ownedAir.iterator();
        while (ownedAirIter.hasNext())
        {
            Unit unit = (Unit) ownedAirIter.next();
            int movement = MoveValidator.movementLeft(unit, m_alreadyMoved);
            
            if(units.contains(unit))
                movement -= route.getLength();
            
            movementLeft.put(unit, movement);
        }
        
        //find the air units that cant make it to land
        Collection<Unit> airThatMustLandOnCarriers = new ArrayList<Unit>();
        Iterator ownedAirIter2 = ownedAir.iterator();
        while (ownedAirIter2.hasNext())
        {
            Unit unit = (Unit) ownedAirIter2.next();
            if(movementLeft.getInt(unit) < closesetLandTerritory)
                airThatMustLandOnCarriers.add(unit);
        }
        
        //we are done, everything can find a place to land
        if(airThatMustLandOnCarriers.isEmpty())
            return null;
        
        //not everything can land on a carrier and not kamikaze, fail    
        if(!m_data.getProperties().get(Constants.KAMIKAZE, false))
            if(!Match.allMatch(airThatMustLandOnCarriers, Matches.UnitCanLandOnCarrier))
                return "No where for the air unit to land";
        
        //now, find out where we can land on carriers
        IntegerMap<Integer> carrierCapacity = new IntegerMap<Integer>();
        
        Iterator candidates = m_data.getMap().getNeighbors(route.getEnd(), maxMovement).iterator();
        while (candidates.hasNext())
        {
            Territory territory = (Territory) candidates.next();
            Route candidateRoute = m_data.getMap().getRoute(route.getEnd(), territory, canMoveThrough);
            if(candidateRoute == null)
                continue;
            Integer distance = new Integer(candidateRoute.getLength());
            
            //we dont want to count untis that moved with us
            Collection<Unit> unitsAtLocation = territory.getUnits().getMatches(Matches.alliedUnit(m_player, m_data));
            unitsAtLocation.removeAll(units);
            
            //how much spare capacity do they have?
            int extraCapacity = MoveValidator.carrierCapacity(unitsAtLocation) - MoveValidator.carrierCost(unitsAtLocation);
            extraCapacity = Math.max(0, extraCapacity);
            
            carrierCapacity.put(distance, carrierCapacity.getInt(distance) + extraCapacity);
            
        }
        
        Collection<Unit> unitsAtEnd = route.getEnd().getUnits().getMatches(Matches.alliedUnit(m_player, m_data));
        unitsAtEnd.addAll(units);
        carrierCapacity.put(new Integer(0), MoveValidator.carrierCapacity(unitsAtEnd));

        
        Iterator airThatMustLandOnCarriersIterator = airThatMustLandOnCarriers.iterator();
        while (airThatMustLandOnCarriersIterator.hasNext())
        {
         
            Unit unit = (Unit) airThatMustLandOnCarriersIterator.next();
            int carrierCost = UnitAttachment.get(unit.getType()).getCarrierCost();
            int movement = movementLeft.getInt(unit);
            for(int i = movement; i >=-1; i--)
            {
                if(i == -1)
                {
                    if(m_data.getProperties().get(Constants.KAMIKAZE, false))
                    {
                        if(!getRemotePlayer().confirmMoveKamikaze())
                            return "Move Aborted";
                    }
                    else                        
                        return "No place for unit to land";
                }

                Integer current = new Integer(i);
                if(carrierCapacity.getInt(current) >= carrierCost && carrierCost != -1 )
                {
                    carrierCapacity.put(current,carrierCapacity.getInt(current) - carrierCost);
                    break;
                }
            }
            
        }
        
        return null;        
    }

    // Determines whether we can pay the neutral territory charge for a
    // given route for air units. We can't cross neutral territories
    // in 4th Edition.
    private String canCrossNeutralTerritory(Route route, PlayerID player)
    {
        //neutrals we will overfly in the first place
        Collection neutrals = getEmptyNeutral(route);
        int ipcs = player.getResources().getQuantity(Constants.IPCS);

        if (ipcs < getNeutralCharge(neutrals.size()))
        {
            return TOO_POOR_TO_VIOLATE_NEUTRALITY;
        }
        return null;
    }


    private String validateTransport(Collection<Unit> units, Route route, PlayerID player, Collection<Unit> transportsToLoad)
    {

        if (Match.allMatch(units, Matches.UnitIsAir))
            return null;

        if (!MoveValidator.hasWater(route))
            return null;

        //if unloading make sure length of route is only 1
        if (MoveValidator.isUnload(route))
        {
            if (route.getLength() > 1)
                return "Unloading units must stop where they are unloaded";

            if (m_transportTracker.wereAnyOfTheseLoadedOnAlliedTransportsThisTurn(units))
                return "Cannot load and unload an allied transport in the same round";
            
            Collection<Unit> transports = mapTransportsAlreadyLoaded(units, route.getStart().getUnits().getUnits()).values();
            for(Unit transport : transports)
            {
                Territory alreadyUnloadedTo = getTerritoryTransportHasUnloadedTo(transport);
                if(alreadyUnloadedTo != null && ! alreadyUnloadedTo.equals(route.getEnd()))
                    return "Transport has already unloaded units to " + alreadyUnloadedTo.getName();
            }
            
        }

        //if we are land make sure no water in route except for transport
        // situations
        Collection<Unit> land = Match.getMatches(units, Matches.UnitIsLand);

        //make sure we can be transported
        if (!Match.allMatch(land, Matches.UnitCanBeTransported))
            return "Unit cannot be transported";

        //make sure that the only the first or last territory is land
        //dont want situation where they go sea land sea
        if (MoveValidator.hasLand(route) && !(route.getStart().isWater() || route.getEnd().isWater()))
            return "Invalid move, only start or end can be land when route has water.";

        //simply because I dont want to handle it yet
        //checks are done at the start and end, dont want to worry about just
        //using a transport as a bridge yet
        //TODO handle this
        if (!route.getEnd().isWater() && !route.getStart().isWater())
            return "Must stop units at a transport on route";

        if (route.getEnd().isWater() && route.getStart().isWater())
        {
            //make sure units and transports stick together
            Iterator iter = units.iterator();
            while (iter.hasNext())
            {
                Unit unit = (Unit) iter.next();
                UnitAttachment ua = UnitAttachment.get(unit.getType());
                //make sure transports dont leave their units behind
                if (ua.getTransportCapacity() != -1)
                {
                    Collection holding = m_transportTracker.transporting(unit);
                    if (holding != null && !units.containsAll(holding))
                    {
                        return "Transport cannot leave their units";
                    }
                }
                //make sure units dont leave their transports behind
                if (ua.getTransportCost() != -1)
                {
                    Unit transport = m_transportTracker.transportedBy(unit);
                    if (transport != null && !units.contains(transport))
                    {
                        return "Unit must stay with its transport while moving";
                    }
                }
            }
        } //end if end is water

        if (MoveValidator.isLoad(route))
        {
            if (mapTransports(route, land, transportsToLoad) == null)
                return "Not enough transports";

            if (route.getLength() != 1)
                return "Units cannot move before loading onto transports";

            Iterator iter = units.iterator();
            while (iter.hasNext())
            {
                Unit unit = (Unit) iter.next();
                if (m_alreadyMoved.getInt(unit) != 0)
                    return "Units cannot move before loading onto transports";
            }

            CompositeMatch<Unit> enemyNonSubmerged = new CompositeMatchAnd<Unit>(Matches.enemyUnit(m_player, m_data), new InverseMatch<Unit>(Matches
                    .unitIsSubmerged(m_data)));
            if (route.getEnd().getUnits().someMatch(enemyNonSubmerged))
            {
                return "Cant load when enemy sea units are present";
            }
        }

        return null;
    }

    private boolean isAlwaysONAAEnabled()
    {
        return m_data.getProperties().get(Constants.ALWAYS_ON_AA_PROPERTY, false);
    }

    private ITripleaPlayer getRemotePlayer()
    {
        return getRemotePlayer(m_player);
    }

    private ITripleaPlayer getRemotePlayer(PlayerID id)
    {
        return (ITripleaPlayer) m_bridge.getRemote(id);
    }

    /**
     * We assume that the move is valid
     */
    private void moveUnits(Collection<Unit> units, Route route, PlayerID id, Collection<Unit> transportsToLoad)
    {


        //if we are moving out of a battle zone, mark it
        //this can happen for air units moving out of a battle zone
        Battle nonBombingBattle = getBattleTracker().getPendingBattle(route.getStart(), false);
        Battle bombingBattle = getBattleTracker().getPendingBattle(route.getStart(), true);
        if (nonBombingBattle != null || bombingBattle != null)
        {
            Iterator iter = units.iterator();
            while (iter.hasNext())
            {
                Unit unit = (Unit) iter.next();
                Route routeUnitUsedToMove = getRouteUsedToMoveInto(unit, route.getStart());
                if (nonBombingBattle != null)
                {
                    nonBombingBattle.removeAttack(routeUnitUsedToMove, Collections.singleton(unit));
                }
                if (bombingBattle != null)
                {
                    bombingBattle.removeAttack(routeUnitUsedToMove, Collections.singleton(unit));
                }
            }
        }

         
        Collection<Unit> aaCasualties = fireAA(route, units);
        Collection<Unit> arrivingUnits = Util.difference(units, aaCasualties);

        //if any non enemy territories on route
        //or if any enemy units on route the
        //battles on (note water could have enemy but its
        //not owned)
        CompositeMatch<Territory> mustFightThrough = new CompositeMatchOr<Territory>();
        mustFightThrough.add(Matches.isTerritoryEnemy(id, m_data));
        mustFightThrough.add(Matches.territoryHasNonSubmergedEnemyUnits(id, m_data));

        Collection<Unit> moved = Util.intersection(units, arrivingUnits);

        if (route.someMatch(mustFightThrough) && arrivingUnits.size() != 0)
        {
            boolean bombing = false;
            //could it be a bombuing raid
            boolean allCanBomb = Match.allMatch(units, Matches.UnitIsStrategicBomber);

            CompositeMatch<Unit> enemyFactory = new CompositeMatchAnd<Unit>();
            enemyFactory.add(Matches.UnitIsFactory);
            enemyFactory.add(Matches.enemyUnit(id, m_data));
            boolean targetToBomb = route.getEnd().getUnits().someMatch(enemyFactory);

            if (allCanBomb && targetToBomb)
            {
                bombing = getRemotePlayer().shouldBomberBomb(route.getEnd());
            }

            getBattleTracker().addBattle(route, arrivingUnits, m_transportTracker, bombing, id, m_data,
                    m_bridge, m_currentMove);
        }

        //mark movement
        markMovement(units, route);
        
        //TODO, put units in owned transports first
        Map<Unit, Unit> transporting = mapTransports(route, units, transportsToLoad);
        markTransportsMovement(transporting, route);
        
        
        //actually move the units
        Change remove = ChangeFactory.removeUnits(route.getStart(), units);
        Change add = ChangeFactory.addUnits(route.getEnd(), arrivingUnits);
        CompositeChange change = new CompositeChange(add, remove);
        m_bridge.addChange(change);

        m_currentMove.addChange(change);

        m_currentMove.setDescription(MyFormatter.unitsToTextNoOwner(moved) + " moved from " + route.getStart().getName() + " to "
                + route.getEnd().getName());

    }



    private int getNeutralCharge(Route route)
    {

        return getNeutralCharge(getEmptyNeutral(route).size());
    }

    private int getNeutralCharge(int numberOfTerritories)
    {

        return numberOfTerritories * games.strategy.triplea.Properties.getNeutralCharge(m_data);
    }

    private boolean hasConqueredNonBlitzed(Route route)
    {

        BattleTracker tracker = getBattleTracker();

        for (int i = 0; i < route.getLength(); i++)
        {
            Territory current = route.at(i);
            if (tracker.wasConquered(current) && !tracker.wasBlitzed(current))
                return true;
        }
        return false;
    }

    private Collection getEmptyNeutral(Route route)
    {

        Match<Territory> emptyNeutral = new CompositeMatchAnd<Territory>(Matches.TerritoryIsEmpty, Matches.TerritoryIsNuetral);
        Collection neutral = route.getMatches(emptyNeutral);
        return neutral;
    }

    private void markMovement(Collection<Unit> units, Route route)
    {

        int moved = route.getLength();
        Iterator iter = units.iterator();
        while (iter.hasNext())
        {
            Unit unit = (Unit) iter.next();
            m_alreadyMoved.add(unit, moved);
        }

        //if neutrals were taken over mark land units with 0 movement
        //if weve entered a non blitzed conquered territory, mark with 0
        // movement
        if (!m_nonCombat && (getEmptyNeutral(route).size() != 0 || hasConqueredNonBlitzed(route)))
        {
            Collection land = Match.getMatches(units, Matches.UnitIsLand);
            iter = land.iterator();
            while (iter.hasNext())
            {
                Unit unit = (Unit) iter.next();
                markNoMovement(unit);
            }
        }
    }

    /**
     * Marks transports and units involved in unloading with no movement left.
     */
    private void markTransportsMovement(Map<Unit, Unit> transporting, Route route)
    {

        if (transporting == null)
            return;

        if (MoveValidator.isUnload(route))
        {

            Collection<Unit> units = new ArrayList<Unit>();
            units.addAll(transporting.values());
            units.addAll(transporting.keySet());
            Iterator<Unit> iter = units.iterator();
            while (iter.hasNext())
            {
                Unit unit = iter.next();
                markNoMovement(unit);
            }

            //unload the transports
            Iterator<Unit> unitIter = transporting.keySet().iterator();
            while (unitIter.hasNext())
            {
                Unit load = unitIter.next();
                m_transportTracker.unload(load, m_currentMove);
            }
        }

        //load the transports
        if (MoveValidator.isLoad(route))
        {
            //mark transports as having transported
            Iterator<Unit> units = transporting.keySet().iterator();
            while (units.hasNext())
            {

                Unit load = units.next();
                Unit transport = transporting.get(load);
                m_transportTracker.load(load, transport, m_currentMove, m_player);
            }
        }
    }

    /**
     * Mark units as having no movement.
     */
    public void markNoMovement(Collection units)
    {

        Iterator iter = units.iterator();
        while (iter.hasNext())
        {
            Unit unit = (Unit) iter.next();
            markNoMovement(unit);
        }
    }

    /**
     * Return whether unit has not moved.
     */
    public boolean hasNotMoved(Unit unit)
    {
        return m_alreadyMoved.getInt(unit) == 0;
    }
    
    public void ensureCanMoveOneSpace(Unit unit)
    {
        int alreadyMoved = m_alreadyMoved.getInt(unit);
        int maxMovement = UnitAttachment.get(unit.getType()).getMovement(unit.getOwner());
        m_alreadyMoved.put(unit, Math.min(alreadyMoved, maxMovement - 1)  );
    }

    private void markNoMovement(Unit unit)
    {

        UnitAttachment ua = UnitAttachment.get(unit.getType());
        m_alreadyMoved.put(unit, ua.getMovement(unit.getOwner()));
    }

    /**
     * Called before the delegate will stop running.
     */
    public void end()
    {

        if (m_nonCombat)
            removeAirThatCantLand();
        m_movesToUndo.clear();

        //fourth edition, fires at end of combat move
        //3rd edition, fires at end of non combat move
        if ((m_nonCombat && !isFourEdition()) || (!m_nonCombat && isFourEdition()))
        {
            if (TechTracker.hasRocket(m_bridge.getPlayerID()))
            {
                RocketsFireHelper helper = new RocketsFireHelper();
                helper.fireRockets(m_bridge, m_data, m_bridge.getPlayerID());
            }
        }

        //do at the end of the round
        //if we do it at the start of non combat, then
        //we may do it in the middle of the round, while loading.
        if (m_nonCombat)
        {
            m_alreadyMoved.clear();
            m_transportTracker.endOfRoundClearState();
            m_ipcsLost.clear();
            m_submergedTracker.clear();
        }

    }

    /**
     * returns a map of unit -> transport. returns null if no mapping can be
     * done either because there is not sufficient transport capacity or because
     * a unit is not with its transport
     */
    private Map<Unit, Unit> mapTransports(Route route, Collection<Unit> units, Collection<Unit> transportsToLoad)
    {

        if (MoveValidator.isLoad(route))
            return mapTransportsToLoad(units, transportsToLoad);
        if (MoveValidator.isUnload(route))
            return mapTransportsAlreadyLoaded(units, route.getStart().getUnits().getUnits());
        return mapTransportsAlreadyLoaded(units, units);
    }

    /**
     * Returns a map of unit -> transport. Unit must already be loaded in the
     * transport, if the unit is in a transport not in transports then null will
     * be returned.
     */
    private Map<Unit, Unit> mapTransportsAlreadyLoaded(Collection<Unit> units, Collection<Unit> transports)
    {

        Collection<Unit> canBeTransported = Match.getMatches(units, Matches.UnitCanBeTransported);
        Collection canTransport = Match.getMatches(transports, Matches.UnitCanTransport);

        Map<Unit, Unit> mapping = new HashMap<Unit, Unit>();
        Iterator land = canBeTransported.iterator();
        while (land.hasNext())
        {
            Unit currentTransported = (Unit) land.next();
            Unit transport = m_transportTracker.transportedBy(currentTransported);
            //already being transported, make sure it is in transports
            if (transport == null)
                return null;

            if (!canTransport.contains(transport))
                return null;
            mapping.put(currentTransported, transport);
        }
        return mapping;
    }

    /**
     * Returns a map of unit -> transport. Tries to find transports to load all
     * units. If it cant suceed returns null.
     *  
     */
    private Map<Unit, Unit> mapTransportsToLoad(Collection<Unit> units, Collection<Unit> transports)
    {

        List<Unit> canBeTransported = Match.getMatches(units, Matches.UnitCanBeTransported);
        int transportIndex = 0;
        Comparator<Unit> c = new Comparator<Unit>()
        {
            public int compare(Unit o1, Unit o2)
            {
                int cost1 = UnitAttachment.get(((Unit) o1).getUnitType()).getTransportCost();
                int cost2 = UnitAttachment.get(((Unit) o2).getUnitType()).getTransportCost();
                return cost2 - cost1;
            }
        };
        //fill the units with the highest cost first.
        //allows easy loading of 2 infantry and 2 tanks on 2 transports
        //in 4th edition rules.
        Collections.sort(canBeTransported, c);

        List<Unit> canTransport = Match.getMatches(transports, Matches.UnitCanTransport);

        Map<Unit, Unit> mapping = new HashMap<Unit, Unit>();
        IntegerMap<Unit> addedLoad = new IntegerMap<Unit>();

        Iterator landIter = canBeTransported.iterator();
        while (landIter.hasNext())
        {
            Unit land = (Unit) landIter.next();
            UnitAttachment landUA = UnitAttachment.get(land.getType());
            int cost = landUA.getTransportCost();
            boolean loaded = false;

            //we want to try to distribute units evenly to all the transports
            //if the user has 2 infantry, and selects two transports to load
            //we should put 1 infantry in each transport.
            //the algorithm below does not guarantee even distribution in all cases
            //but it solves most of the cases
            Iterator transportIter = Util.shiftElementsToEnd(canTransport, transportIndex).iterator();
            while (transportIter.hasNext() && !loaded)
            {
                transportIndex++;
                if(transportIndex >= canTransport.size())
                    transportIndex = 0;
                
                Unit transport = (Unit) transportIter.next();
                int capacity = m_transportTracker.getAvailableCapacity(transport);
                capacity -= addedLoad.getInt(transport);
                if (capacity >= cost)
                {
                    addedLoad.add(transport, cost);
                    mapping.put(land, transport);
                    loaded = true;
                }
            }
            
            if (!loaded)
                return null;
        }
        return mapping;
    }

    public int compareAccordingToMovementLeft(Unit u1, Unit u2)
    {
        return decreasingMovement.compare(u1,u2);
    }
    
    private Comparator<Unit> decreasingMovement = new Comparator<Unit>()
    {

        public int compare(Unit u1, Unit u2)
        {


            int left1 = MoveValidator.movementLeft(u1, m_alreadyMoved);
            int left2 = MoveValidator.movementLeft(u2, m_alreadyMoved);

            if (left1 == left2)
                return 0;
            if (left1 > left2)
                return 1;
            return -1;
        }
    };

    private Comparator<Unit> increasingMovement = new Comparator<Unit>()
    {

        public int compare(Unit o1, Unit o2)
        {

            //reverse the order, clever huh
            return decreasingMovement.compare(o2, o1);
        }
    };

    

    public Collection<Territory> getTerritoriesWhereAirCantLand()
    {
        Collection<Territory> cantLand = new ArrayList<Territory>();
        Iterator territories = m_data.getMap().getTerritories().iterator();
        while (territories.hasNext())
        {
            Territory current = (Territory) territories.next();
            CompositeMatch<Unit> ownedAir = new CompositeMatchAnd<Unit>();
            ownedAir.add(Matches.UnitIsAir);
            ownedAir.add(Matches.alliedUnit(m_player, m_data));
            Collection<Unit> air = current.getUnits().getMatches(ownedAir);
            if (air.size() != 0 && !MoveValidator.canLand(air, current, m_player, m_data))
            {
                cantLand.add(current);
            }
        }
        return cantLand;
    }

    private void removeAirThatCantLand()
    {
        Iterator<Territory> territories = getTerritoriesWhereAirCantLand().iterator();
        while (territories.hasNext())
        {
            Territory current = territories.next();
            CompositeMatch<Unit> ownedAir = new CompositeMatchAnd<Unit>();
            ownedAir.add(Matches.UnitIsAir);
            ownedAir.add(Matches.alliedUnit(m_player, m_data));
            Collection<Unit> air = current.getUnits().getMatches(ownedAir);

            removeAirThatCantLand(current, air);
        }
    }

    private void removeAirThatCantLand(Territory territory, Collection<Unit> airUnits)
    {

        Collection<Unit> toRemove = new ArrayList<Unit>(airUnits.size());
        //if we cant land on land then none can
        if (!territory.isWater())
        {
            toRemove.addAll(airUnits);
        } else
        //on water we may just no have enough carriers
        {
            //find the carrier capacity
            Collection carriers = territory.getUnits().getMatches(Matches.alliedUnit(m_player, m_data));
            int capacity = MoveValidator.carrierCapacity(carriers);

            Iterator iter = airUnits.iterator();
            while (iter.hasNext())
            {
                Unit unit = (Unit) iter.next();
                UnitAttachment ua = UnitAttachment.get(unit.getType());
                int cost = ua.getCarrierCost();
                if (cost == -1 || cost > capacity)
                    toRemove.add(unit);
                else
                    capacity -= cost;
            }
        }

        Change remove = ChangeFactory.removeUnits(territory, toRemove);

        String transcriptText = MyFormatter.unitsToTextNoOwner(toRemove) + " could not land in " + territory.getName() + " and "
                + (toRemove.size() > 1 ? "were" : "was") + " removed";
        m_bridge.getHistoryWriter().startEvent(transcriptText);

        m_bridge.addChange(remove);

    }

    /**
     * Fire aa guns. Returns units to remove.
     */
    private Collection<Unit> fireAA(Route route, Collection<Unit> units)
    {
        List<Unit> targets = Match.getMatches(units, Matches.UnitIsAir);

        //select units with lowest movement first
        Collections.sort(targets, decreasingMovement);
        Collection<Unit> originalTargets = new ArrayList<Unit>(targets);
        
        Iterator iter = getTerritoriesWhereAAWillFire(route, units).iterator();
        while (iter.hasNext())
        {
            Territory location = (Territory) iter.next();
            fireAA(location, targets);
        }

        return Util.difference(originalTargets, targets);

    }

    private Collection<Territory> getTerritoriesWhereAAWillFire(Route route, Collection<Unit> units)
    {
        if (m_nonCombat && !isAlwaysONAAEnabled())
            return Collections.emptyList();

        if (Match.noneMatch(units, Matches.UnitIsAir))
            return Collections.emptyList();

        //dont iteratate over the end
        //that will be a battle
        //and handled else where in this tangled mess
        CompositeMatch<Unit> hasAA = new CompositeMatchAnd<Unit>();
        hasAA.add(Matches.UnitIsAA);
        hasAA.add(Matches.enemyUnit(m_player, m_data));

        List<Territory> territoriesWhereAAWillFire = new ArrayList<Territory>();

        for (int i = 0; i < route.getLength() - 1; i++)
        {
            Territory current = route.at(i);

            //aa guns in transports shouldnt be able to fire
            if (current.getUnits().someMatch(hasAA) && !current.isWater())
            {
                territoriesWhereAAWillFire.add(current);
            }
        }

        //check start as well, prevent user from moving to and from aa sites
        // one at a time
        //if there was a battle fought there then dont fire
        //this covers the case where we fight, and always on aa wants to fire
        //after the battle.
        //TODO
        //there is a bug in which if you move an air unit to a battle site
        //in the middle of non combat, it wont fire
        if (route.getStart().getUnits().someMatch(hasAA)
                && !getBattleTracker().wasBattleFought(route.getStart()))
            territoriesWhereAAWillFire.add(route.getStart());
 
        return territoriesWhereAAWillFire;
    }
    
    /**
     * Fire the aa units in the given territory, hits are removed from units
     */
    private void fireAA(Territory territory, Collection<Unit> units)
    {
        
        if(units.isEmpty())
            return;

        //once we fire the aa guns, we cant undo
        //otherwise you could keep undoing and redoing
        //until you got the roll you wanted
        m_currentMove.setCantUndo("Move cannot be undone after AA has fired.");
        DiceRoll dice = DiceRoll.rollAA(units.size(), m_bridge, territory, m_data);
        int hitCount = dice.getHits();

        if (hitCount == 0)
        {
            getRemotePlayer().reportMessage("No aa hits in " + territory.getName());
        } else
            selectCasualties(dice, units, territory);
    }

    /**
     * hits are removed from units. Note that units are removed in the order
     * that the iterator will move through them.
     */
    private void selectCasualties(DiceRoll dice, Collection<Unit> units, Territory territory)
    {

        String text = "Select " + dice.getHits() + " casualties from aa fire in " + territory.getName();
        // If fourth edition, select casualties randomnly
        Collection<Unit> casualties = null;
        if (isFourEdition())
        {
            casualties = BattleCalculator.fourthEditionAACasualties(units, dice, m_bridge);
        } else
        {
            CasualtyDetails casualtyMsg = BattleCalculator.selectCasualties(m_player, units, m_bridge, text, m_data, dice, false);
            casualties = casualtyMsg.getKilled();
        }

        getRemotePlayer().reportMessage(dice.getHits() + " AA hits in " + territory.getName());
        
        m_bridge.getHistoryWriter().addChildToEvent(MyFormatter.unitsToTextNoOwner(casualties) + " lost in " + territory.getName(), casualties);
        units.removeAll(casualties);
    }

    /**
     * Find the route that a unit used to move into the given territory.
     */
    public Route getRouteUsedToMoveInto(Unit unit, Territory end)
    {
        for (int i = m_movesToUndo.size() - 1; i >= 0; i--)
        {
            UndoableMove move = m_movesToUndo.get(i);
            if (!move.getUnits().contains(unit))
                continue;
            if (move.getRoute().getEnd().equals(end))
                return move.getRoute();
        }
        return null;

    }

    /**
     * Return the number of ipcs that have been lost by bombing, rockets, etc.
     */
    public int ipcsAlreadyLost(Territory t)
    {
        return m_ipcsLost.getInt(t);
    }

    /**
     * Add more ipcs lost to a territory due to bombing, rockets, etc.
     */
    public void ipcsLost(Territory t, int amt)
    {
        m_ipcsLost.add(t, amt);
    }

    public TransportTracker getTransportTracker()
    {
        return m_transportTracker;
    }

    /**
     * Can the delegate be saved at the current time.
     * 
     * @arg message, a String[] of size 1, hack to pass an error message back.
     */
    public boolean canSave(String[] message)
    {

        return true;
    }

    public Serializable saveState()
    {

        return saveState(true);
    }

    /*
     * @see games.strategy.engine.delegate.IDelegate#getRemoteType()
     */
    public Class<IMoveDelegate> getRemoteType()
    {
        return IMoveDelegate.class;
    }
    
    private Territory getTerritoryTransportHasUnloadedTo(Unit transport)
    {
        
        for(UndoableMove undoableMove : m_movesToUndo)
        {
            if(undoableMove.wasTransportUnloaded(transport))
            {
                return undoableMove.getRoute().getEnd();
            }
        }
        return null;
    }

    /**
     * Returns the state of the Delegate. We dont want to save the undoState if
     * we are saving the state for an undo move (we dont need it, it will just
     * take up extra space).
     */
    Serializable saveState(boolean saveUndo)
    {

        MoveState state = new MoveState();
        state.m_firstRun = m_firstRun;
        state.m_nonCombat = m_nonCombat;
        state.m_transportTracker = m_transportTracker;
        state.m_alreadyMoved = m_alreadyMoved;
        if (saveUndo)
            state.m_movesToUndo = m_movesToUndo;
        state.m_submergedTracker = m_submergedTracker;
        state.m_ipcsLost = m_ipcsLost;
        return state;
    }

    /**
     * Loads the delegates state
     *  
     */
    public void loadState(Serializable aState)
    {

        MoveState state = (MoveState) aState;
        m_firstRun = state.m_firstRun;
        m_nonCombat = state.m_nonCombat;
        m_transportTracker = state.m_transportTracker;
        m_alreadyMoved = state.m_alreadyMoved;
        //if the undo state wasnt saved, then dont load it
        //prevents overwriting undo state when we restore from an undo move
        if (state.m_movesToUndo != null)
            m_movesToUndo = state.m_movesToUndo;
        m_submergedTracker = state.m_submergedTracker;
        m_ipcsLost = state.m_ipcsLost;
    }

    public SubmergedTracker getSubmergedTracker()
    {
        return m_submergedTracker;
    }
}

class MoveState implements Serializable
{
    public boolean m_firstRun = true;
    public boolean m_nonCombat;
    public TransportTracker m_transportTracker;
    public IntegerMap<Unit> m_alreadyMoved;
    public IntegerMap<Territory> m_ipcsLost;
    public List<UndoableMove> m_movesToUndo;
    public SubmergedTracker m_submergedTracker;

}
