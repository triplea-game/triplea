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

import java.util.*;
import java.io.Serializable;

import games.strategy.util.*;

import games.strategy.engine.data.*;
import games.strategy.engine.delegate.*;
import games.strategy.engine.message.*;

import games.strategy.net.IRemote;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attatchments.*;
import games.strategy.triplea.delegate.message.*;
import games.strategy.triplea.delegate.remote.IMoveDelegate;
import games.strategy.triplea.formatter.Formatter;
import java.io.*;

import org.omg.CORBA.COMM_FAILURE;

import sun.security.provider.MD5;

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

    private String m_name;
    private String m_displayName;
    private DelegateBridge m_bridge;
    private GameData m_data;
    private PlayerID m_player;
    private boolean m_firstRun = true;
    private boolean m_nonCombat;
    private TransportTracker m_transportTracker = new TransportTracker();
    private IntegerMap m_alreadyMoved = new IntegerMap();
    private IntegerMap m_ipcsLost = new IntegerMap();
    private SubmergedTracker m_submergedTracker = new SubmergedTracker();
    
    // A collection of UndoableMoves
    private List m_movesToUndo = new ArrayList();

    //The current move
    private UndoableMove m_currentMove;

    private static final String CANT_VIOLATE_NEUTRALITY  =
        "Can't violate neutrality";
    private static final String TOO_POOR_TO_VIOLATE_NEUTRALITY = 
        "Not enough money to pay for violating neutrality";

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

            Collection units = current.getUnits().getUnits();
            if (units.size() == 0 || !Match.someMatch(units, Matches.UnitIsLand))
                continue;

            //map transports, try to fill
            Collection transports = Match.getMatches(units, Matches.UnitIsTransport);
            Collection land = Match.getMatches(units, Matches.UnitIsLand);
            Iterator landIter = land.iterator();
            while (landIter.hasNext())
            {
                Unit toLoad = (Unit) landIter.next();
                UnitAttatchment ua = UnitAttatchment.get(toLoad.getType());
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
    public void start(DelegateBridge aBridge, GameData gameData)
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

    /**
     * A message from the given player.
     */
    public Message sendMessage(Message aMessage)
    {

        if (aMessage instanceof MoveMessage)
            return move((MoveMessage) aMessage, m_player);
        else if (aMessage instanceof MustMoveAirQueryMessage)
            return new TerritoryCollectionMessage(getTerritoriesWhereAirCantLand());
        else if (aMessage instanceof MustMoveWithQuery)
            return mustMoveWith((MustMoveWithQuery) aMessage);
        else if (aMessage instanceof UndoMoveMessage)
            return undoMove((UndoMoveMessage) aMessage);
        else if (aMessage instanceof MoveCountRequestMessage)
            return getMoveCount();
        else
            throw new IllegalArgumentException("Move delegate received message of wrong type:" + aMessage);
    }

    private MoveCountReplyMessage getMoveCount()
    {

        return new MoveCountReplyMessage(m_movesToUndo);

    }

    private StringMessage undoMove(UndoMoveMessage message)
    {

        if (m_movesToUndo.isEmpty())
            return new StringMessage("No moves to undo", true);
        if (message.getIndex() >= m_movesToUndo.size())
            return new StringMessage("Undo move index out of range", true);

        UndoableMove moveToUndo = (UndoableMove) m_movesToUndo.get(message.getIndex());

        if (!moveToUndo.getcanUndo())
            return new StringMessage(moveToUndo.getReasonCantUndo(), true);

        moveToUndo.undo(m_bridge, m_alreadyMoved, m_data);
        m_movesToUndo.remove(message.getIndex());
        updateUndoableMoveIndexes();

        return null;
    }

    private void updateUndoableMoveIndexes()
    {

        for (int i = 0; i < m_movesToUndo.size(); i++)
        {
            ((UndoableMove) m_movesToUndo.get(i)).setIndex(i);
        }
    }

    private MustMoveWithReply mustMoveWith(MustMoveWithQuery query)
    {

        return new MustMoveWithReply(mustMoveWith(query.getUnits(), query.getStart()), movementLeft(query.getUnits()));
    }

    private IntegerMap movementLeft(Collection units)
    {

        IntegerMap movement = new IntegerMap();

        Iterator iter = units.iterator();
        while (iter.hasNext())
        {
            Unit current = (Unit) iter.next();
            movement.put(current, MoveValidator.movementLeft(current, m_alreadyMoved));
        }

        return movement;
    }

    private Map mustMoveWith(Collection units, Territory start)
    {

        List sortedUnits = new ArrayList(units);

        Collections.sort(sortedUnits, increasingMovement);

        Map mapping = new HashMap();
        mapping.putAll(transportsMustMoveWith(sortedUnits));
        mapping.putAll(carrierMustMoveWith(sortedUnits, start));
        return mapping;
    }

    private Map transportsMustMoveWith(Collection units)
    {

        Map mustMoveWith = new HashMap();
        //map transports
        Collection transports = Match.getMatches(units, Matches.UnitIsTransport);
        Iterator iter = transports.iterator();
        while (iter.hasNext())
        {
            Unit transport = (Unit) iter.next();
            Collection transporting = m_transportTracker.transporting(transport);
            mustMoveWith.put(transport, transporting);
        }
        return mustMoveWith;
    }

    private Map carrierMustMoveWith(Collection units, Territory start) {
      return carrierMustMoveWith(units, start.getUnits().getUnits());
    }

    public Map carrierMustMoveWith(Collection units, Collection startUnits)
    {

        //we want to get all air units that are owned by our allies
        //but not us that can land on a carrier
        CompositeMatch friendlyNotOwnedAir = new CompositeMatchAnd();
        friendlyNotOwnedAir.add(Matches.alliedUnit(m_player, m_data));
        friendlyNotOwnedAir.addInverse(Matches.unitIsOwnedBy(m_player));
        friendlyNotOwnedAir.add(Matches.UnitCanLandOnCarrier);

        Collection alliedAir = Match.getMatches(startUnits, friendlyNotOwnedAir);

        if (alliedAir.isEmpty())
            return Collections.EMPTY_MAP;

        //remove air that can be carried by allied
        CompositeMatch friendlyNotOwnedCarrier = new CompositeMatchAnd();
        friendlyNotOwnedCarrier.add(Matches.UnitIsCarrier);
        friendlyNotOwnedCarrier.add(Matches.alliedUnit(m_player, m_data));
        friendlyNotOwnedCarrier.addInverse(Matches.unitIsOwnedBy(m_player));

        Collection alliedCarrier = Match.getMatches(startUnits, friendlyNotOwnedCarrier);

        Iterator alliedCarrierIter = alliedCarrier.iterator();
        while (alliedCarrierIter.hasNext())
        {
            Unit carrier = (Unit) alliedCarrierIter.next();
            Collection carrying = getCanCarry(carrier, alliedAir);
            alliedAir.removeAll(carrying);
        }

        if (alliedAir.isEmpty())
            return Collections.EMPTY_MAP;

        Map mapping = new HashMap();
        //get air that must be carried by our carriers
        Collection ownedCarrier = Match.getMatches(units, Matches.UnitIsCarrier);

        Iterator ownedCarrierIter = ownedCarrier.iterator();
        while (ownedCarrierIter.hasNext())
        {
            Unit carrier = (Unit) ownedCarrierIter.next();
            Collection carrying = getCanCarry(carrier, alliedAir);
            alliedAir.removeAll(carrying);

            mapping.put(carrier, carrying);
        }

        return mapping;
    }

    private Collection getCanCarry(Unit carrier, Collection selectFrom)
    {

        UnitAttatchment ua = UnitAttatchment.get(carrier.getUnitType());
        Collection canCarry = new ArrayList();

        int available = ua.getCarrierCapacity();
        Iterator iter = selectFrom.iterator();
        while (iter.hasNext())
        {
            Unit plane = (Unit) iter.next();
            UnitAttatchment planeAttatchment = UnitAttatchment.get(plane.getUnitType());
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

    private StringMessage move(MoveMessage message, PlayerID id)
    {

        Route route = message.getRoute();
        Collection units = message.getUnits();

        String error = validateMove(units, route, id, message.getTransportsToLoad());
        if (error != null)
            return new StringMessage(error, true);
        //do the move
        m_currentMove = new UndoableMove(m_data, m_alreadyMoved, units, route);

        String transcriptText = Formatter.unitsToTextNoOwner(units) + " moved from " + route.getStart().getName() + " to " + route.getEnd().getName();
        m_bridge.getHistoryWriter().startEvent(transcriptText);
        m_bridge.getHistoryWriter().setRenderingData(message);

        StringMessage rVal = moveUnits(units, route, id, message.getTransportsToLoad());
        if (!rVal.isError())
        {
            m_currentMove.markEndMovement(m_alreadyMoved);
            m_currentMove.initializeDependencies(m_movesToUndo);
            m_movesToUndo.add(m_currentMove);
            updateUndoableMoveIndexes();
        }
        m_currentMove = null;
        return rVal;
    }

    private String validateMove(Collection units, Route route, PlayerID player, Collection transportsToLoad)
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
        //the exception is air units
        if (DelegateFinder.battleDelegate(m_data).getBattleTracker().hasPendingBattle(route.getStart(), false) &&
            Match.someMatch(units, Matches.UnitIsNotAir)    
                )
        {
            boolean unload = MoveValidator.isUnload(route);
            PlayerID endOwner = route.getEnd().getOwner();
            boolean attack = !m_data.getAllianceTracker().isAllied(endOwner, m_player);
            //unless they are unloading into another battle
            if (!(unload && attack))
                return "Cant move units out of battle zone";
        }

        //make sure we can afford to pay neutral fees
        int cost = getNeutralCharge(route);
        int resources = player.getResources().getQuantity(Constants.IPCS);
        if (resources - cost < 0)
        {
            if(isFourEdition())
	        return CANT_VIOLATE_NEUTRALITY;
            else
                return TOO_POOR_TO_VIOLATE_NEUTRALITY;
        }


        return null;
    }

    private String validateCanal(Collection units, Route route, PlayerID player)
    {

        //if no sea units then we can move
        if (Match.noneMatch(units, Matches.UnitIsSea))
            return null;

        return MoveValidator.validateCanal(route, player, m_data);
    }

    private String validateCombat(Collection units, Route route, PlayerID player)
    {

      // Don't allow aa guns to move in non-combat unless they are in a transport
        if (Match.someMatch(units, Matches.UnitIsAA)
	    && (!route.getStart().isWater() || !route.getEnd().isWater()))
            return "Cant move aa guns in combat movement phase";
        return null;
    }

    private String validateNonCombat(Collection units, Route route, PlayerID player)
    {

        CompositeMatch battle = new CompositeMatchOr();
        battle.add(Matches.TerritoryIsNuetral);
        battle.add(Matches.isTerritoryEnemy(player, m_data));
        
        if (battle.match(route.getEnd()))
        {
            return "Cant advance units to battle in non combat";
        }

        
        if(route.getEnd().getUnits().someMatch(Matches.enemyUnit(player, m_data)))
        {
            CompositeMatch friendlyOrSubmerged = new CompositeMatchOr();
            friendlyOrSubmerged.add(Matches.alliedUnit(m_player, m_data));
            friendlyOrSubmerged.add(Matches.unitIsSubmerged(m_data));
            if(!route.getEnd().getUnits().allMatch(friendlyOrSubmerged))
            {
                return "Cant advance to battle in non combat";
            }
        }
        
        if (Match.allMatch(units, Matches.UnitIsAir))
        {
  	    if (route.someMatch(Matches.TerritoryIsNuetral))
            {
	        if (isFourEdition())
                {
 		    return CANT_VIOLATE_NEUTRALITY;
		} else
                {
                    return "Air units cannot fly over neutral territories in non combat";
		}
	    }
        } else
        {
            CompositeMatch neutralOrEnemy = new CompositeMatchOr(Matches.TerritoryIsNuetral, Matches.isTerritoryEnemy(player, m_data));
            if (route.someMatch(neutralOrEnemy))
                return "Cant move units to neutral or enemy territories in non combat";
        }
        return null;
    }

    private String validateNonEnemyUnitsOnPath(Collection units, Route route, PlayerID player)
    {
        //check to see no enemy units on path
        if (MoveValidator.onlyAlliedUnitsOnPath(route, player, m_data))
            return null;

        //if we are all air, then its ok
        if(MoveValidator.isAir(units))
            return null;
        
        boolean submersibleSubsAllowed = m_data.getProperties().get(Constants.SUBMERSIBLE_SUBS, false);
        
        if(submersibleSubsAllowed && Match.allMatch(units, Matches.UnitIsSub))
        {
           //this is ok unless there are destroyer on the path
           if(MoveValidator.enemyDestroyerOnPath(route, player, m_data))
           {
               return "Cant move submarines under destroyers";
           }
           else
               return null;
        }
        

        return "Enemy units on path";
    }
    
    private String validateBasic(Collection units, Route route, PlayerID player, Collection transportsToLoad)
    {

        if(m_submergedTracker.areAnySubmerged(units))
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
        Collection moveTest;
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
            if (!MoveValidator.isAir(units))
                return "Must stop land units when passing through nuetral territories";
        }

        if(!m_nonCombat &&  Match.someMatch(units, Matches.UnitIsLand) && route.getLength() >=1)
        {
        	//check all the territories but the end, 
        	//if there are enemy territories, make sure they are blitzable
        	//if they are not blitzable, or we arent all blit units
        	//fail
            int enemyCount = 0;
            boolean allEnemyBlitzable = true;
            
            for(int i = 0; i < route.getLength() - 1; i++)
            {
                Territory current = route.at(i);
                
                if(current.isWater())
                	continue;
                	
                if(!m_data.getAllianceTracker().isAllied(current.getOwner(), m_player)  ||
                        DelegateFinder.battleDelegate(m_data).getBattleTracker().wasConquered(current ))
                        {
                    		enemyCount++;
                    		allEnemyBlitzable &= MoveValidator.isBlitzable(current, m_data, m_player);
                        }
            }
            
            if(enemyCount > 0 && ! allEnemyBlitzable)
            {
            	return "Cant blitz on that route";
           }
           else if(enemyCount > 0 && allEnemyBlitzable)
           {
           	    Match blitzingUnit = new CompositeMatchOr(Matches.UnitCanBlitz, Matches.UnitIsAir);
            	if(!Match.allMatch(units, blitzingUnit))
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
        if (MoveValidator.hasSea(units))
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
        if (  !isFourEdition() &&
                Match.someMatch(units, Matches.UnitIsAA) && route.getEnd().getUnits().someMatch(Matches.UnitIsAA) && !route.getEnd().isWater())
        {
            return "Only one AA gun allowed in a territroy";
        }

        //only allow 1 aa to unload
        if (route.getStart().isWater() && !route.getEnd().isWater() && Match.countMatches(units, Matches.UnitIsAA) > 1)
        {
            return "Only 1 AA gun allowed in a territory";
        }


        String errorMsg = canCrossNeutralTerritory(route, player);
        if ( errorMsg != null) {
	  return errorMsg;
	}

        return null;
    }

    private boolean isFourEdition()
    {
        return m_data.getProperties().get(Constants.FOURTH_EDITION, false);
    }
    
    private String validateAirCanLand(Collection units, Route route, PlayerID player)
    {

        //TODO,
        //if they cant all land in one territory then
        //they an error will be returned, even
        //if they could land in multiple territories

        //make sure air units have enough movement to land

        if (!MoveValidator.hasAir(units))
            return null;

        //could be a war zone, make sure we only look at
        //friendly units that will be in the territory after
        //the move
        Collection friendly = MoveValidator.getFriendly(route.getEnd(), player, m_data);
        friendly.addAll(units);
        Collection friendlyAir = Match.getMatches(friendly, Matches.UnitIsAir);

        if (!MoveValidator.canLand(friendlyAir, route.getEnd(), player, m_data))
        {
            //get movement left of units that are moving
            //not enough carrier capacity at end
            Collection air = Match.getMatches(units, Matches.UnitIsAir);
            int distance = MoveValidator.getLeastMovement(air, m_alreadyMoved);
            distance = distance - route.getLength();

            boolean canLand = canFindAPlaceToLand(route, player, air, distance);

            //this is a hack
            //if the air that we are moving and cant find another place to land,
            //see if the air we are moving can land in the given territory
            //and the air already there can land somewhere else
            if(!canLand && MoveValidator.canLand( Match.getMatches(units, Matches.UnitIsAir), route.getEnd(), m_player, m_data))
            {
                Collection airAlreadyThere = Match.getMatches( MoveValidator.getFriendly(route.getEnd(), player, m_data), Matches.UnitIsAir);
                distance = MoveValidator.getLeastMovement(airAlreadyThere, m_alreadyMoved);
                canLand = canFindAPlaceToLand(route, player, airAlreadyThere, distance);
                    
            }
            
            //TODO - may be able to split air units up and land in different
            // groups
            //eg a bomber and a fighter move to a carrier. Bomber can land on
            //carrier but has enough movment to make it to land,
            //fighter can land, but does not have enough movement to
            //make it to land.
            //Both can land somewhere, but there is no place where all can land
            if (!canLand)
            {
                if (air.size() == 1)
                    return "No place for the air unit to land.";
                else
                    return "There is no place where all the air units can land. You may be able to move these units in smaller groups.";
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
        if (isFourEdition() && (neutrals.size() > 0))
        {
  	    return CANT_VIOLATE_NEUTRALITY;
        }
	if (ipcs < getNeutralCharge(neutrals.size()))
        {
  	    return TOO_POOR_TO_VIOLATE_NEUTRALITY;
	}
	return null;
    }

    private boolean canFindAPlaceToLand(Route route, PlayerID player, Collection air, int distance)
    {
        Set neighbors = m_data.getMap().getNeighbors(route.getEnd(), distance);

        boolean canLand = false;
        Iterator iter = neighbors.iterator();

        //neutrals we will overfly in the first place
        Collection neutrals = getEmptyNeutral(route);
        int ipcs = player.getResources().getQuantity(Constants.IPCS);
        while (iter.hasNext())
        {
            Territory possible = (Territory) iter.next();
            if (MoveValidator.canLand(air, possible, player, m_data))
            {
                //make sure we can pay for the neutrals we will
                //overfly when we go to land
                Set overflownNeutrals = new HashSet();
                overflownNeutrals.addAll(neutrals);
                overflownNeutrals.addAll(getBestNeutralEmptyCollection(route.getEnd(), possible, distance));
                if (ipcs >= getNeutralCharge(overflownNeutrals.size())) {
                    canLand = true;
		}
            }
        }
        return canLand;
    }

    private String validateTransport(Collection units, Route route, PlayerID player, Collection transportsToLoad)
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
        }

        //if we are land make sure no water in route except for transport
        // situations
        Collection land = Match.getMatches(units, Matches.UnitIsLand);

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
                UnitAttatchment ua = UnitAttatchment.get(unit.getType());
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
            
            CompositeMatch enemyNonSubmerged = new CompositeMatchAnd(
                    Matches.enemyUnit(m_player, m_data),
                    new InverseMatch( Matches.unitIsSubmerged(m_data))
            );
            if(route.getEnd().getUnits().someMatch(enemyNonSubmerged))
            {
                return "Cant load when enemy sea units are present";
            }
        }

        return null;
    }

    private boolean isAlwaysONAAEnabled()
    {

        Boolean property = (Boolean) m_data.getProperties().get(Constants.ALWAYS_ON_AA_PROPERTY);
        if (property == null)
            return false;
        return property.booleanValue();
    }

    /**
     * We assume that the move is valid
     */
    private StringMessage moveUnits(Collection units, Route route, PlayerID id, Collection transportsToLoad)
    {

        //mark movement
        markMovement(units, route);
        
        
        //if we are moving out of a battle zone, mark it
        //this can happen for air units moving out of a battle zone
        Battle battleLand =DelegateFinder.battleDelegate(m_data).getBattleTracker().getPendingBattle(route.getStart(), false);
        Battle battleAir =DelegateFinder.battleDelegate(m_data).getBattleTracker().getPendingBattle(route.getStart(), true);
        if(battleLand != null || battleAir != null)
        {
            Iterator iter = units.iterator();
            while(iter.hasNext())
            {
                Unit unit = (Unit) iter.next();
                Route routeUnitUsedToMove = getRouteUsedToMoveInto(unit, route.getStart());
                if(battleLand != null)
                {
                    battleLand.removeAttack(routeUnitUsedToMove, Collections.singleton(unit));
                }
                if(battleAir != null)
                {
                    battleAir.removeAttack(routeUnitUsedToMove, Collections.singleton(unit));
                }
            }
        }
        

        Collection arrivingUnits = units;
        if (!m_nonCombat || isAlwaysONAAEnabled())
        {
            Collection aaCasualties = fireAA(route, units);
            arrivingUnits = Util.difference(units, aaCasualties);
        }

        //if any non enemy territories on route
        //or if any enemy units on route the
        //battles on (note water could have enemy but its
        //not owned)
        CompositeMatch mustFightThrough = new CompositeMatchOr();
        mustFightThrough.add(Matches.isTerritoryEnemy(id, m_data));
        mustFightThrough.add(Matches.territoryHasNonSubmergedEnemyUnits(id, m_data));

        Collection moved = Util.intersection(units, arrivingUnits);

        if (route.someMatch(mustFightThrough) && arrivingUnits.size() != 0)
        {
            boolean bombing = false;
            //could it be a bombuing raid
            boolean allCanBomb = Match.allMatch(units, Matches.UnitIsStrategicBomber);

            CompositeMatch enemyFactory = new CompositeMatchAnd();
            enemyFactory.add(Matches.UnitIsFactory);
            enemyFactory.add(Matches.enemyUnit(id, m_data));
            boolean targetToBomb = route.getEnd().getUnits().someMatch(enemyFactory);

            if (allCanBomb && targetToBomb)
            {
                StrategicBombQuery query = new StrategicBombQuery(route.getEnd());
                Message response = m_bridge.sendMessage(query);
                if (!(response instanceof BooleanMessage))
                {
                    throw new IllegalStateException("Received message of wrong type. Message:" + response);
                }
                bombing = ((BooleanMessage) response).getBoolean();
            }

            DelegateFinder.battleDelegate(m_data).getBattleTracker().addBattle(route, arrivingUnits, m_transportTracker, bombing, id, m_data, m_bridge, m_currentMove);
        }

        //TODO, put units in owned transports first
        Map transporting = mapTransports(route, units, transportsToLoad);
        markTransportsMovement(transporting, route);

        //actually move the units
        Change remove = ChangeFactory.removeUnits(route.getStart(), units);
        Change add = ChangeFactory.addUnits(route.getEnd(), arrivingUnits);
        CompositeChange change = new CompositeChange(add, remove);
        m_bridge.addChange(change);
        
        m_currentMove.addChange(change);

        m_currentMove.setDescription(Formatter.unitsToTextNoOwner(moved) + " moved from " + route.getStart().getName() + " to " + route.getEnd().getName());

        return new StringMessage("done");
    }

    private Collection getBestNeutralEmptyCollection(Territory start, Territory end, int maxDistance)
    {

        //TODO fix this. If there are two neutral territories
        //on the route, we may be able to find
        //a route with only one, currently its either
        //take the obvious unless a perfect route
        //with no neutrals can be found

        //get the obvious route
        Route route = m_data.getMap().getRoute(start, end);
        if (route.getLength() > maxDistance)
            throw new IllegalStateException("No route short enough." + "route:" + route + " maxDistance:" + maxDistance);

        Collection neutral = getEmptyNeutral(route);
        if (neutral.size() == 0)
        {
            return neutral;
        }

        //see if we can do better
        Match emptyNeutral = new CompositeMatchAnd(Matches.TerritoryIsNuetral, Matches.TerritoryIsEmpty);

        Route alternate = m_data.getMap().getRoute(start, end, new InverseMatch(emptyNeutral));
        if (alternate == null)
            return neutral;
        if (alternate.getLength() > maxDistance)
            return neutral;
        //route has no empty neutral states in path, no charge
        return new ArrayList();
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

        BattleTracker tracker = DelegateFinder.battleDelegate(m_data).getBattleTracker();

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

        Match emptyNeutral = new CompositeMatchAnd(Matches.TerritoryIsEmpty, Matches.TerritoryIsNuetral);
        Collection neutral = route.getMatches(emptyNeutral);
        return neutral;
    }

    private void markMovement(Collection units, Route route)
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
    private void markTransportsMovement(Map transporting, Route route)
    {

        if (transporting == null)
            return;

        if (MoveValidator.isUnload(route))
        {

            Collection units = new ArrayList();
            units.addAll(transporting.values());
            units.addAll(transporting.keySet());
            Iterator iter = units.iterator();
            while (iter.hasNext())
            {
                Unit unit = (Unit) iter.next();
                markNoMovement(unit);
            }

            //unload the transports
            Iterator unitIter = transporting.keySet().iterator();
            while (unitIter.hasNext())
            {
                Unit load = (Unit) unitIter.next();
                m_transportTracker.unload(load, m_currentMove);
            }
        }

        //load the transports
        if (MoveValidator.isLoad(route))
        {
            //mark transports as having transported
            Iterator units = transporting.keySet().iterator();
            while (units.hasNext())
            {

                Unit load = (Unit) units.next();
                Unit transport = (Unit) transporting.get(load);
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

    private void markNoMovement(Unit unit)
    {

        UnitAttatchment ua = UnitAttatchment.get(unit.getType());
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
        if( (m_nonCombat && !isFourEdition()) ||
                (!m_nonCombat && isFourEdition() ) ) 
        {
            if (  TechTracker.hasRocket(m_bridge.getPlayerID()))
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
    private Map mapTransports(Route route, Collection units, Collection transportsToLoad)
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
    private Map mapTransportsAlreadyLoaded(Collection units, Collection transports)
    {

        Collection canBeTransported = Match.getMatches(units, Matches.UnitCanBeTransported);
        Collection canTransport = Match.getMatches(transports, Matches.UnitCanTransport);

        Map mapping = new HashMap();
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
    private Map mapTransportsToLoad(Collection units, Collection transports)
    {

        List canBeTransported = Match.getMatches(units, Matches.UnitCanBeTransported);
        Comparator c = new Comparator()
        {
            public int compare(Object o1, Object o2)
            {
                int cost1 = UnitAttatchment.get( ((Unit) o1).getUnitType()).getTransportCost();
                int cost2 = UnitAttatchment.get( ((Unit) o2).getUnitType()).getTransportCost();
                return cost2 - cost1;
             }
        };
        //fill the units with the highest cost first.
        //allows easy loading of 2 infantry and 2 tanks on 2 transports
        //in 4th edition rules.
        Collections.sort(canBeTransported, c);
        
        Collection canTransport = Match.getMatches(transports, Matches.UnitCanTransport);
        Collection ownedTransport = Match.getMatches(transports, Matches.unitIsOwnedBy(m_player));
        canTransport = Util.difference(canTransport, ownedTransport);

        Map mapping = new HashMap();
        IntegerMap addedLoad = new IntegerMap();

        Iterator landIter = canBeTransported.iterator();
        while (landIter.hasNext())
        {
            Unit land = (Unit) landIter.next();
            UnitAttatchment landUA = UnitAttatchment.get(land.getType());
            int cost = landUA.getTransportCost();
            boolean loaded = false;

            //check owned first
            Iterator transportIter = ownedTransport.iterator();
            while (transportIter.hasNext() && !loaded)
            {
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
            //check allied
            transportIter = canTransport.iterator();
            while (transportIter.hasNext() && !loaded)
            {
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

    public void sortAccordingToMovementLeft(List units, boolean ascending)
    {

        Collections.sort(units, ascending ? increasingMovement : decreasingMovement);
    }

    private Comparator decreasingMovement = new Comparator()
    {

        public int compare(Object o1, Object o2)
        {

            Unit u1 = (Unit) o1;
            Unit u2 = (Unit) o2;

            int left1 = MoveValidator.movementLeft(u1, m_alreadyMoved);
            int left2 = MoveValidator.movementLeft(u2, m_alreadyMoved);

            if (left1 == left2)
                return 0;
            if (left1 > left2)
                return 1;
            return -1;
        }
    };

    private Comparator increasingMovement = new Comparator()
    {

        public int compare(Object o1, Object o2)
        {

            //reverse the order, clever huh
            return decreasingMovement.compare(o2, o1);
        }
    };

    private Collection getTerritoriesWhereAirCantLand()
    {

        Collection cantLand = new ArrayList();
        Iterator territories = m_data.getMap().getTerritories().iterator();
        while (territories.hasNext())
        {
            Territory current = (Territory) territories.next();
            CompositeMatch ownedAir = new CompositeMatchAnd();
            ownedAir.add(Matches.UnitIsAir);
            ownedAir.add(Matches.alliedUnit(m_player, m_data));
            Collection air = current.getUnits().getMatches(ownedAir);
            if (air.size() != 0 && !MoveValidator.canLand(air, current, m_player, m_data))
            {
                cantLand.add(current);
            }
        }
        return cantLand;
    }

    private void removeAirThatCantLand()
    {

        Iterator territories = getTerritoriesWhereAirCantLand().iterator();
        while (territories.hasNext())
        {
            Territory current = (Territory) territories.next();
            CompositeMatch ownedAir = new CompositeMatchAnd();
            ownedAir.add(Matches.UnitIsAir);
            ownedAir.add(Matches.alliedUnit(m_player, m_data));
            Collection air = current.getUnits().getMatches(ownedAir);

            removeAirThatCantLand(current, air);
        }
    }

    private void removeAirThatCantLand(Territory territory, Collection airUnits)
    {

        Collection toRemove = new ArrayList(airUnits.size());
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
                UnitAttatchment ua = UnitAttatchment.get(unit.getType());
                int cost = ua.getCarrierCost();
                if (cost == -1 || cost > capacity)
                    toRemove.add(unit);
                else
                    capacity -= cost;
            }
        }

        Change remove = ChangeFactory.removeUnits(territory, toRemove);

        String transcriptText = Formatter.unitsToTextNoOwner(toRemove) + " could not land in " + territory.getName() + " and " + (toRemove.size() > 1 ? "were" : "was") + " removed";
        m_bridge.getHistoryWriter().startEvent(transcriptText);

        m_bridge.addChange(remove);

    }

    /**
     * Fire aa guns. Returns units to remove.
     */
    private Collection fireAA(Route route, Collection units)
    {

        if (Match.noneMatch(units, Matches.UnitIsAir))
            return Collections.EMPTY_LIST;

        List targets = Match.getMatches(units, Matches.UnitIsAir);

        //select units with lowest movement first
        Collections.sort(targets, decreasingMovement);
        Collection originalTargets = new ArrayList(targets);

        //dont iteratate over the end
        //that will be a battle
        //and handled else where in this tangled mess
        CompositeMatch hasAA = new CompositeMatchAnd();
        hasAA.add(Matches.UnitIsAA);
        hasAA.add(Matches.enemyUnit(m_player, m_data));

        for (int i = 0; i < route.getLength() - 1; i++)
        {
            Territory current = route.at(i);

            //aa guns in transports shouldnt be able to fire
            if (current.getUnits().someMatch(hasAA) && !current.isWater())
            {
                fireAA(current, targets);
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
        if (route.getStart().getUnits().someMatch(hasAA) && 
            !DelegateFinder.battleDelegate(m_data).getBattleTracker().wasBattleFought(route.getStart())        
        )
            fireAA(route.getStart(), targets);

        return Util.difference(originalTargets, targets);
    }

    /**
     * Fire the aa units in the given territory, hits are removed from units
     */
    private void fireAA(Territory territory, Collection units)
    {

        //once we fire the aa guns, we cant undo
        //otherwise you could keep undoing and redoing
        //until you got the roll you wanted
        m_currentMove.setCantUndo("Move cannot be undone after AA has fired.");
        DiceRoll dice = DiceRoll.rollAA(units.size(), m_bridge, territory, m_data);
        int hitCount = dice.getHits();

        if (hitCount == 0)
        {
            m_bridge.sendMessage(new StringMessage("No aa hits in " + territory.getName()));
        } else
            selectCasualties(dice, units, territory);
    }

    /**
     * hits are removed from units. Note that units are removed in the order
     * that the iterator will move through them.
     */
    private void selectCasualties(DiceRoll dice, Collection units, Territory territory)
    {

        String text = "Select " + dice.getHits() + " casualties from aa fire in " + territory.getName();
	// If fourth edition, select casualties randomnly
	Collection casualties = null;
	if (isFourEdition()) {
          casualties = BattleCalculator.fourthEditionAACasualties(units, dice, m_bridge);
	} else {
	  SelectCasualtyMessage casualtyMsg = BattleCalculator.selectCasualties(m_player, units, m_bridge, text, m_data, dice, false);
	  casualties = casualtyMsg.getKilled();
	}

	m_bridge.sendMessage(new StringMessage(dice.getHits() + " AA hits in " + territory.getName()));
        m_bridge.getHistoryWriter().addChildToEvent(Formatter.unitsToTextNoOwner(casualties) + " lost in " + territory.getName(), casualties);
        units.removeAll(casualties);
    }
    
    /**
     * Find the route that a unit used to move into the given territory. 
     */
    public Route getRouteUsedToMoveInto(Unit unit, Territory end)
    {
        for(int i = m_movesToUndo.size() -1; i >= 0; i--)
        {
            UndoableMove move = (UndoableMove) m_movesToUndo.get(i);
            if(!move.getUnits().contains(unit))
                continue;
            if(move.getRoute().getEnd().equals(end))
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
    public Class getRemoteType()
    {
        return  IMoveDelegate.class;
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
    public IntegerMap m_alreadyMoved;
    public IntegerMap m_ipcsLost;
    public List m_movesToUndo;
    public SubmergedTracker m_submergedTracker;
    
}
