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
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.util.*;
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
public class MoveDelegate implements IDelegate, IMoveDelegate
{

    public static final String CANNOT_LOAD_AND_UNLOAD_AN_ALLIED_TRANSPORT_IN_THE_SAME_ROUND = "Cannot load and unload an allied transport in the same round";
    private static final String CANT_MOVE_THROUGH_IMPASSIBLE = "Can't move through impassible territories";
    private static final String TOO_POOR_TO_VIOLATE_NEUTRALITY = "Not enough money to pay for violating neutrality";
    private static final String NOT_ALL_AIR_UNITS_CAN_LAND = "Not all air units can land";
    
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
    
    //if we are in the process of doing a move
    //this instance will allow us to resume the move
    private MovePerformer m_tempMovePerformer;
    

    // A collection of UndoableMoves
    private List<UndoableMove> m_movesToUndo = new ArrayList<UndoableMove>();

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
                        m_bridge.addChange(m_transportTracker.loadTransportChange((TripleAUnit) transport, toLoad, m_player));                        
                        found = true;
                        break;
                    }
                }
                if (!found)
                    throw new IllegalStateException("Cannot load all units");
            }
            
        }
    }

    GameData getGameData()
    {
        return m_data;
    }
    
    public static boolean isNonCombat(IDelegateBridge aBridge)
    {
        if (aBridge.getStepName().endsWith("NonCombatMove"))
            return true;
        else if (aBridge.getStepName().endsWith("CombatMove"))
            return false;
        else
            throw new IllegalStateException("Cannot determine combat or not");
    }
    
    /**
     * Called before the delegate will run.
     */
    public void start(IDelegateBridge aBridge, GameData gameData)
    {
        m_nonCombat = isNonCombat(aBridge);

        m_bridge = aBridge;
        PlayerID player = aBridge.getPlayerID();

        m_data = gameData;
        m_player = player;

        if (m_firstRun)
            firstRun();
        
        
        if(m_tempMovePerformer != null)
        {
            m_tempMovePerformer.initialize(this, m_data, aBridge);
            m_tempMovePerformer.resume();
            m_tempMovePerformer = null;
        }
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
        Collection<Unit> ownedCarrier = Match.getMatches(units, new CompositeMatchAnd<Unit>(Matches.UnitIsCarrier, Matches.unitIsOwnedBy(player)));

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

    public String move(Collection<Unit> units, Route route)
    {
        return move(units, route, Collections.<Unit>emptyList());
    }

    public String move(Collection<Unit> units, Route route, Collection<Unit> transportsThatCanBeLoaded)
    {

        MoveValidationResult result = validateMove(units, route, m_player, transportsThatCanBeLoaded);

        StringBuilder errorMsg = new StringBuilder(100);

        int numProblems = result.getTotalWarningCount() - (result.hasError() ? 0 : 1);

        String numErrorsMsg = numProblems > 0 ? ("; "+ numProblems + " errors" + (numProblems==1 ? "" : "s") + " not shown") : "";

        if (result.hasError())
            return errorMsg.append(result.getError()).append(numErrorsMsg).toString();

        if (result.hasDisallowedUnits())
            return errorMsg.append(result.getDisallowedUnitWarning(0)).append(numErrorsMsg).toString();

        // confirm kamikaze moves, and remove them from unresolved units
        if(m_data.getProperties().get(Constants.KAMIKAZE, false))
        {
            Collection<Unit> kamikazeUnits = result.getUnresolvedUnits(NOT_ALL_AIR_UNITS_CAN_LAND);
            if (kamikazeUnits.size() > 0 && getRemotePlayer().confirmMoveKamikaze())  
                for (Unit unit : kamikazeUnits)
                    result.removeUnresolvedUnit(NOT_ALL_AIR_UNITS_CAN_LAND, unit);
        }

        if (result.hasUnresolvedUnits())
            return errorMsg.append(result.getUnresolvedUnitWarning(0)).append(numErrorsMsg).toString();

        // allow user to cancel move if aa guns will fire
        AAInMoveUtil aaInMoveUtil = new AAInMoveUtil();
        aaInMoveUtil.initialize(m_bridge, m_data);
        Collection aaFiringTerritores = aaInMoveUtil.getTerritoriesWhereAAWillFire(route, units);
        if(!aaFiringTerritores.isEmpty())
        {
            if(!getRemotePlayer().confirmMoveInFaceOfAA(aaFiringTerritores))
                return null;
        }
        
        //do the move
        UndoableMove currentMove = new UndoableMove(m_data, m_alreadyMoved, units, route);

        String transcriptText = MyFormatter.unitsToTextNoOwner(units) + " moved from " + route.getStart().getName() + " to " + route.getEnd().getName();
        m_bridge.getHistoryWriter().startEvent(transcriptText);
        MoveDescription description = new MoveDescription(units, route);
        m_bridge.getHistoryWriter().setRenderingData(description);

        
        m_tempMovePerformer = new MovePerformer();
        m_tempMovePerformer.initialize(this, m_data, m_bridge);
        m_tempMovePerformer.moveUnits(units, route, m_player, transportsThatCanBeLoaded, currentMove);
        m_tempMovePerformer = null;


        return null;
    }
    
    void updateUndoableMoves(UndoableMove currentMove)
    {
        currentMove.markEndMovement(m_alreadyMoved);
        currentMove.initializeDependencies(m_movesToUndo);
        m_movesToUndo.add(currentMove);
        updateUndoableMoveIndexes();
    }

    public MoveValidationResult validateMove(Collection<Unit> units, Route route, PlayerID player, Collection<Unit> transportsToLoad)
    {

        MoveValidationResult result = new MoveValidationResult();

        if (m_nonCombat)
        {
            if (validateNonCombat(units, route, player, result).getError() != null)
                return result;
        }

        if (!m_nonCombat)
        {
            if (validateCombat(units, route, player, result).getError() != null)
                return result;
        }

        if (validateNonEnemyUnitsOnPath(units, route, player, result).getError() != null)
            return result;

        if (validateBasic(units, route, player, transportsToLoad, result).getError() != null)
            return result;

        if (validateAirCanLand(units, route, player, result).getError() != null)
            return result;

        if (validateTransport(units, route, player, transportsToLoad, result).getError() != null)
            return result;

        if (validateCanal(units, route, player, result).getError() != null)
            return result;


        //dont let the user move out of a battle zone
        //the exception is air units and unloading units into a battle zone
        if (getBattleTracker().hasPendingBattle(route.getStart(), false)
                && Match.someMatch(units, Matches.UnitIsNotAir))
        {
            //if the units did not move into the territory, then they can move out
            //this will happen if there is a submerged sub in the area, and 
            //a different unit moved into the sea zone setting up a battle
            //but the original unit can still remain
            boolean unitsStartedInTerritory = true;
            for(Unit unit : units) 
            {
                if(getRouteUsedToMoveInto(unit, route.getEnd()) != null)
                {
                    unitsStartedInTerritory = false;
                    break;
                }
            }
            
            if(!unitsStartedInTerritory)
            {
            
                boolean unload = MoveValidator.isUnload(route);
                PlayerID endOwner = route.getEnd().getOwner();
                boolean attack = !m_data.getAllianceTracker().isAllied(endOwner, m_player) || getBattleTracker().wasConquered(route.getEnd());
                //unless they are unloading into another battle
                if (!(unload && attack))
                    return result.setErrorReturnResult("Cannot move units out of battle zone");
            }
        }

        //make sure we can afford to pay neutral fees
        int cost = getNeutralCharge(route);
        int resources = player.getResources().getQuantity(Constants.IPCS);
        if (resources - cost < 0)
            return result.setErrorReturnResult(TOO_POOR_TO_VIOLATE_NEUTRALITY);

        return result;
    }

    private MoveValidationResult validateCanal(Collection<Unit> units, Route route, PlayerID player, MoveValidationResult result)
    {

        //if no sea units then we can move
        if (Match.noneMatch(units, Matches.UnitIsSea))
            return result;

        //TODO: merge validateCanal here and provide granular unit warnings
        return result.setErrorReturnResult(MoveValidator.validateCanal(route, player, m_data));
    }

    private MoveValidationResult validateCombat(Collection<Unit> units, Route route, PlayerID player, MoveValidationResult result)
    {

        // Don't allow aa guns to move in combat unless they are in a
        // transport
        if (Match.someMatch(units, Matches.UnitIsAA) && (!route.getStart().isWater() || !route.getEnd().isWater()))
            for (Unit unit : Match.getMatches(units, Matches.UnitIsAA))
                result.addDisallowedUnit("Cannot move AA guns in combat movement phase", unit);

        return result;
    }

    private MoveValidationResult validateNonCombat(Collection<Unit> units, Route route, PlayerID player, MoveValidationResult result)
    {

        if (route.someMatch(Matches.TerritoryIsImpassible))
            return result.setErrorReturnResult(CANT_MOVE_THROUGH_IMPASSIBLE);

        CompositeMatch<Territory> battle = new CompositeMatchOr<Territory>();
        battle.add(Matches.TerritoryIsNeutral);
        battle.add(Matches.isTerritoryEnemyAndNotNeutral(player, m_data));

        if (battle.match(route.getEnd()))
            return result.setErrorReturnResult("Cannot advance units to battle in non combat");

        if (route.getEnd().getUnits().someMatch(Matches.enemyUnit(player, m_data)))
        {
            CompositeMatch<Unit> friendlyOrSubmerged = new CompositeMatchOr<Unit>();
            friendlyOrSubmerged.add(Matches.alliedUnit(m_player, m_data));
            friendlyOrSubmerged.add(Matches.unitIsSubmerged(m_data));
            if (!route.getEnd().getUnits().allMatch(friendlyOrSubmerged))
                return result.setErrorReturnResult("Cannot advance to battle in non combat");
        }

        if (Match.allMatch(units, Matches.UnitIsAir))
        {
            if (route.someMatch(Matches.TerritoryIsNeutral))
                return result.setErrorReturnResult("Air units cannot fly over neutral territories in non combat");
        } else
        {
            CompositeMatch<Territory> neutralOrEnemy = new CompositeMatchOr<Territory>(Matches.TerritoryIsNeutral, Matches.isTerritoryEnemyAndNotNeutral(player, m_data));
            if (route.someMatch(neutralOrEnemy))
                return result.setErrorReturnResult("Cannot move units to neutral or enemy territories in non combat");
        }
        return result;
    }

    private MoveValidationResult validateNonEnemyUnitsOnPath(Collection<Unit> units, Route route, PlayerID player, MoveValidationResult result)
    {
        //check to see no enemy units on path
        if (MoveValidator.onlyAlliedUnitsOnPath(route, player, m_data))
            return result;

        //if we are all air, then its ok
        if (Match.allMatch(units, Matches.UnitIsAir))
            return result;

        boolean submersibleSubsAllowed = m_data.getProperties().get(Constants.SUBMERSIBLE_SUBS, false);

        if (submersibleSubsAllowed && Match.allMatch(units, Matches.UnitIsSub))
        {
            //this is ok unless there are destroyer on the path
            if (MoveValidator.enemyDestroyerOnPath(route, player, m_data))
                return result.setErrorReturnResult("Cannot move submarines under destroyers");
            else
                return result;
        }

        return result.setErrorReturnResult("Enemy units on path");
    }

    private MoveValidationResult validateBasic(Collection<Unit> units, Route route, PlayerID player, Collection<Unit> transportsToLoad, MoveValidationResult result)
    {

        if(units.size() == 0)
            return result.setErrorReturnResult("No units");
        
        
    	for (Unit unit : units)
    	{
    	    if (m_submergedTracker.isSubmerged(unit))
    	        result.addDisallowedUnit("Cannot move submerged units", unit);
    	}

        //make sure all units are actually in the start territory
        if (!route.getStart().getUnits().containsAll(units))
            return result.setErrorReturnResult("Not enough units in starting territory");

        //make sure transports in the destination
        if (!route.getEnd().getUnits().containsAll(transportsToLoad))
            return result.setErrorReturnResult("Transports not found in route end");

        //make sure all units are at least friendly
        for (Unit unit : Match.getMatches(units, Matches.enemyUnit(m_player,m_data)))
            result.addDisallowedUnit("Can only move friendly units", unit);

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
	// check units individually
	for (Unit unit : moveTest)
	{
	    if (!MoveValidator.hasEnoughMovement(unit, m_alreadyMoved, route.getLength()))
                result.addDisallowedUnit("Not all units have enough movement",unit);
	}

        //if there is a neutral in the middle must stop unless all are air
        if (MoveValidator.hasNeutralBeforeEnd(route))
        {
            if (!Match.allMatch(units, Matches.UnitIsAir))
                return result.setErrorReturnResult("Must stop land units when passing through neutral territories");
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
                return result.setErrorReturnResult("Cannot blitz on that route");
            } else if (enemyCount > 0 && allEnemyBlitzable)
            {
                Match<Unit> blitzingUnit = new CompositeMatchOr<Unit>(Matches.UnitCanBlitz, Matches.UnitIsAir);
                Match<Unit> nonBlitzing = new InverseMatch<Unit>(blitzingUnit);
                for (Unit unit : Match.getMatches(units, nonBlitzing))
                    result.addDisallowedUnit("Not all units can blitz",unit);
            }

        }

        //make sure no conquered territories on route
        if (MoveValidator.hasConqueredNonBlitzedOnRoute(route, m_data))
        {
            //unless we are all air or we are in non combat
            if (!Match.allMatch(units, Matches.UnitIsAir) && !m_nonCombat)
                return result.setErrorReturnResult("Cannot move through newly captured territories");
        }

        //make sure that no non sea non transportable no carriable units
        //end at sea
        if (route.getEnd().isWater())
        {
            for (Unit unit : MoveValidator.getUnitsThatCantGoOnWater(units))
                result.addDisallowedUnit("Not all units can end at water",unit);
        }

        //if we are water make sure no land
        if (Match.someMatch(units, Matches.UnitIsSea))
        {
            if (MoveValidator.hasLand(route))
                for (Unit unit : Match.getMatches(units, Matches.UnitIsSea))
                    result.addDisallowedUnit("Sea units cannot go on land",unit);
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
                {
                    for (Unit unit : Match.getMatches(units, Matches.UnitIsAA))
                        result.addDisallowedUnit("AA units cannot advance to battle", unit);

                    break;
                }
            }
        }

        //only allow aa into a land territory if one already present.
        if (!isFourEdition() && Match.someMatch(units, Matches.UnitIsAA) && route.getEnd().getUnits().someMatch(Matches.UnitIsAA)
                && !route.getEnd().isWater())
        {
            for (Unit unit : Match.getMatches(units, Matches.UnitIsAA))
                result.addDisallowedUnit("Only one AA gun allowed in a territory",unit);
        }

        //only allow 1 aa to unload
        if (route.getStart().isWater() && !route.getEnd().isWater() && Match.countMatches(units, Matches.UnitIsAA) > 1)
        {
            Collection<Unit> aaGuns = Match.getMatches(units, Matches.UnitIsAA);
            Iterator<Unit> aaIter = aaGuns.iterator();
            aaIter.next(); // skip first unit
            for (; aaIter.hasNext(); )
                result.addUnresolvedUnit("Only one AA gun can unload in a territory",aaIter.next());
        }

        // don't allow move through impassible territories
        if (route.someMatch(Matches.TerritoryIsImpassible))
          return result.setErrorReturnResult(CANT_MOVE_THROUGH_IMPASSIBLE);

        if (canCrossNeutralTerritory(route, player, result).getError() != null)
            return result;
        
        return result;
    }

    BattleTracker getBattleTracker()
    {
        return DelegateFinder.battleDelegate(m_data).getBattleTracker();
    }

    private boolean isFourEdition()
    {
        return m_data.getProperties().get(Constants.FOURTH_EDITION, false);
    }

    private MoveValidationResult validateAirCanLand(Collection<Unit> units, Route route, PlayerID player, MoveValidationResult result)
    {
        boolean allowKamikaze =  m_data.getProperties().get(Constants.KAMIKAZE, false);

        //nothing to check
        if (!Match.someMatch(units, Matches.UnitIsAir))
            return result;

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
            return result;
        

        //this is the farthese we need to look for places to land
        //the fighters cant move farther than this
        //note that this doesnt take into account the movement used to move the
        //units along the route
        int maxMovement = MoveValidator.getMaxMovement(units, m_alreadyMoved);
        
        Match<Territory> canMoveThrough = new InverseMatch<Territory>(Matches.TerritoryIsImpassible);
        Match<Territory> notNeutral = new InverseMatch<Territory>(Matches.TerritoryIsNeutral);
        
        Match<Territory> notNeutralAndNotImpassible = new CompositeMatchAnd<Territory>(canMoveThrough, notNeutral);
        
        //find the closest land territory where everyone can land
        int closestLandTerritory = Integer.MAX_VALUE;
        
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
                closestLandTerritory = Math.min(closestLandTerritory, noNeutralRoute.getLength());
            }
            //can we find a path with neutrals?
            //can we afford this path?
            Route neutralViolatingRoute = m_data.getMap().getRoute(route.getEnd(), territory, notNeutral);
            if((neutralViolatingRoute != null) && getNeutralCharge(neutralViolatingRoute) <= m_player.getResources().getQuantity(Constants.IPCS))
            {
                closestLandTerritory = Math.min(closestLandTerritory, neutralViolatingRoute.getLength());                    
            }
        }
        
        //these are the units we have to be sure that can land somewhere
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
            if(movementLeft.getInt(unit) < closestLandTerritory)
                airThatMustLandOnCarriers.add(unit);
        }
        
        //we are done, everything can find a place to land
        if(airThatMustLandOnCarriers.isEmpty())
            return result;
        
        //not everything can land on a carrier
        Match<Unit> cantLandMatch = new InverseMatch<Unit>(Matches.UnitCanLandOnCarrier);
        for (Unit unit : Match.getMatches(airThatMustLandOnCarriers, cantLandMatch))
        {
            if (allowKamikaze)
                result.addUnresolvedUnit(NOT_ALL_AIR_UNITS_CAN_LAND, unit);
            else
                result.addDisallowedUnit(NOT_ALL_AIR_UNITS_CAN_LAND, unit);
        }
        
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

        
        for (Unit unit : Match.getMatches(airThatMustLandOnCarriers, Matches.UnitCanLandOnCarrier))
        {
            int carrierCost = UnitAttachment.get(unit.getType()).getCarrierCost();
            int movement = movementLeft.getInt(unit);
            for(int i = movement; i >=-1; i--)
            {
                if(i == -1)
                {
                    if (allowKamikaze)
                        result.addUnresolvedUnit(NOT_ALL_AIR_UNITS_CAN_LAND, unit);
                    else
                        result.addDisallowedUnit(NOT_ALL_AIR_UNITS_CAN_LAND, unit);
                    break;
                }

                Integer current = new Integer(i);
                if(carrierCapacity.getInt(current) >= carrierCost && carrierCost != -1 )
                {
                    carrierCapacity.put(current,carrierCapacity.getInt(current) - carrierCost);
                    break;
                }
            }
            
        }
        
        return result;
    }

    // Determines whether we can pay the neutral territory charge for a
    // given route for air units. We can't cross neutral territories
    // in 4th Edition.
    private MoveValidationResult canCrossNeutralTerritory(Route route, PlayerID player, MoveValidationResult result)
    {
        //neutrals we will overfly in the first place
        Collection neutrals = getEmptyNeutral(route);
        int ipcs = player.getResources().getQuantity(Constants.IPCS);

        if (ipcs < getNeutralCharge(neutrals.size()))
            return result.setErrorReturnResult(TOO_POOR_TO_VIOLATE_NEUTRALITY);

        return result;
    }


    private MoveValidationResult validateTransport(Collection<Unit> units, Route route, PlayerID player, Collection<Unit> transportsToLoad, MoveValidationResult result)
    {

        if (Match.allMatch(units, Matches.UnitIsAir))
            return result;

        if (!MoveValidator.hasWater(route))
            return result;

        //if unloading make sure length of route is only 1
        if (MoveValidator.isUnload(route))
        {
            if (route.getLength() > 1)
                result.setErrorReturnResult("Unloading units must stop where they are unloaded");

            for (Unit unit : m_transportTracker.getUnitsLoadedOnAlliedTransportsThisTurn(units))
                result.addDisallowedUnit(CANNOT_LOAD_AND_UNLOAD_AN_ALLIED_TRANSPORT_IN_THE_SAME_ROUND,unit);
            
            Collection<Unit> transports = mapTransportsAlreadyLoaded(units, route.getStart().getUnits().getUnits()).values();
            for(Unit transport : transports)
            {
                Territory alreadyUnloadedTo = getTerritoryTransportHasUnloadedTo(transport);
                if(alreadyUnloadedTo != null && ! alreadyUnloadedTo.equals(route.getEnd()))
                    for (Unit unit : m_transportTracker.transporting(transport))
                        result.addDisallowedUnit("Transport has already unloaded units to " + alreadyUnloadedTo.getName(), unit);
            }
            
        }

        //if we are land make sure no water in route except for transport
        // situations
        Collection<Unit> land = Match.getMatches(units, Matches.UnitIsLand);

        //make sure we can be transported
        Match<Unit> cantBeTransported = new InverseMatch<Unit>(Matches.UnitCanBeTransported);
        for (Unit unit : Match.getMatches(land, cantBeTransported))
            result.addDisallowedUnit("Not all units can be transported",unit);

        //make sure that the only the first or last territory is land
        //dont want situation where they go sea land sea
        if (MoveValidator.hasLand(route) && !(route.getStart().isWater() || route.getEnd().isWater()))
            result.setErrorReturnResult("Invalid move, only start or end can be land when route has water.");

        //simply because I dont want to handle it yet
        //checks are done at the start and end, dont want to worry about just
        //using a transport as a bridge yet
        //TODO handle this
        if (!route.getEnd().isWater() && !route.getStart().isWater())
            return result.setErrorReturnResult("Must stop units at a transport on route");

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
                        result.addDisallowedUnit("Transports cannot leave their units",unit);
                }
                //make sure units dont leave their transports behind
                if (ua.getTransportCost() != -1)
                {
                    Unit transport = m_transportTracker.transportedBy(unit);
                    if (transport != null && !units.contains(transport))
                        result.addDisallowedUnit("Unit must stay with its transport while moving",unit);
                }
            }
        } //end if end is water

        if (MoveValidator.isLoad(route))
        {
            Map<Unit,Unit> unitsToTransports = mapTransports(route, land, transportsToLoad);
            if (! unitsToTransports.keySet().containsAll(land))
            {
                // some units didn't get mapped to a transport
                Collection<UnitCategory> unitsToLoadCategories = UnitSeperator.categorize(land);

                if (unitsToTransports.size() == 0 || unitsToLoadCategories.size() == 1)
                {
                    // set all unmapped units as disallowed if there are no transports
                    //   or only one unit category
                    for (Unit unit : land)
                    {
                        if (unitsToTransports.containsKey(unit))
                            continue;
                        UnitAttachment ua = UnitAttachment.get(unit.getType());
                        if (ua.getTransportCost() != -1)
                            result.addDisallowedUnit("Not enough transports", unit);
                    }
                }
                else
                {
                    // set all units as unresolved if there is at least one transport 
                    //   and mixed unit categories
                    for (Unit unit : land)
                    {
                        UnitAttachment ua = UnitAttachment.get(unit.getType());
                        if (ua.getTransportCost() != -1)
                            result.addUnresolvedUnit("Not enough transports", unit);
                    }
                }
            }

            if (route.getLength() != 1)
                return result.setErrorReturnResult("Units cannot move before loading onto transports");

            Iterator iter = units.iterator();
            while (iter.hasNext())
            {
                Unit unit = (Unit) iter.next();
                if (m_alreadyMoved.getInt(unit) != 0)
                    result.addDisallowedUnit("Units cannot move before loading onto transports",unit);
            }

            CompositeMatch<Unit> enemyNonSubmerged = new CompositeMatchAnd<Unit>(Matches.enemyUnit(m_player, m_data), new InverseMatch<Unit>(Matches
                    .unitIsSubmerged(m_data)));
            if (route.getEnd().getUnits().someMatch(enemyNonSubmerged))
                return result.setErrorReturnResult("Cannot load when enemy sea units are present");
        }

        return result;
    }

    private ITripleaPlayer getRemotePlayer()
    {
        return getRemotePlayer(m_player);
    }

    private ITripleaPlayer getRemotePlayer(PlayerID id)
    {
        return (ITripleaPlayer) m_bridge.getRemote(id);
    }

 



    private int getNeutralCharge(Route route)
    {

        return getNeutralCharge(getEmptyNeutral(route).size());
    }

    private int getNeutralCharge(int numberOfTerritories)
    {

        return numberOfTerritories * games.strategy.triplea.Properties.getNeutralCharge(m_data);
    }


    public static Collection getEmptyNeutral(Route route)
    {

        Match<Territory> emptyNeutral = new CompositeMatchAnd<Territory>(Matches.TerritoryIsEmpty, Matches.TerritoryIsNeutral);
        Collection neutral = route.getMatches(emptyNeutral);
        return neutral;
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
        CompositeChange change = new CompositeChange();

        //do at the end of the round
        //if we do it at the start of non combat, then
        //we may do it in the middle of the round, while loading.
        if (m_nonCombat)
        {
            m_alreadyMoved.clear();
            change.add(m_transportTracker.endOfRoundClearStateChange(m_data));
            m_ipcsLost.clear();
            m_submergedTracker.clear();
        }

        if(!change.isEmpty()) 
        {
            m_bridge.addChange(change);
        }
    }

    private void removeAirThatCantLand()
    {
        boolean lhtrCarrierProd = AirThatCantLandUtil.isLHTRCarrierProdcution(m_data);
        boolean hasProducedCarriers = m_player.getUnits().someMatch(Matches.UnitIsCarrier);
        new AirThatCantLandUtil(m_data, m_bridge).removeAirThatCantLand(lhtrCarrierProd && hasProducedCarriers);
        
    }

    /**
     * returns a map of unit -> transport. returns null if no mapping can be
     * done either because there is not sufficient transport capacity or because
     * a unit is not with its transport
     */
    public Map<Unit, Unit> mapTransports(Route route, Collection<Unit> units, Collection<Unit> transportsToLoad)
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
                continue;

            if (!canTransport.contains(transport))
                continue;
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
            
        }
        return mapping;
    }

    public int compareAccordingToMovementLeft(Unit u1, Unit u2)
    {
        return decreasingMovement.compare(u1,u2);
    }
    
    Comparator<Unit> getDecreasingMovement()
    {
        return decreasingMovement;
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
        return new AirThatCantLandUtil(m_data, m_bridge).getTerritoriesWhereAirCantLand();
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
        state.m_tempMovePerformer = this.m_tempMovePerformer;
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
        m_tempMovePerformer = state.m_tempMovePerformer;
    }

    public SubmergedTracker getSubmergedTracker()
    {
        return m_submergedTracker;
    }

    IntegerMap<Unit> getAlreadyMoved()
    {
        return m_alreadyMoved;
    }

    public List<Unit> getUnitsAlreadyMoved()
    {
        List<Unit> rVal = new ArrayList<Unit>();
        for(Unit u : m_alreadyMoved.keySet())
        {
            if(m_alreadyMoved.getInt(u) > 0 )
            {
                rVal.add(u);
            }
        }
        return rVal;
        
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
    public MovePerformer m_tempMovePerformer;

}
