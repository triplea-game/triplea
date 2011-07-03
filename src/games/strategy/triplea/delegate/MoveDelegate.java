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

import games.strategy.engine.data.Change;
import games.strategy.engine.data.ChangeFactory;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegate;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.Constants;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.attatchments.TriggerAttachment;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.triplea.delegate.dataObjects.MoveDescription;
import games.strategy.triplea.delegate.dataObjects.MoveValidationResult;
import games.strategy.triplea.delegate.remote.IMoveDelegate;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.triplea.player.ITripleaPlayer;
import games.strategy.triplea.util.UnitCategory;
import games.strategy.triplea.util.UnitSeperator;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.IntegerMap;
import games.strategy.util.Match;
import games.strategy.util.Util;

import java.io.Serializable;
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
    
    private String m_name;
    private String m_displayName;
    private IDelegateBridge m_bridge;
    private GameData m_data;
    private PlayerID m_player;
    private boolean m_firstRun = true;
    private boolean m_nonCombat;
    private final TransportTracker m_transportTracker = new TransportTracker();
    private IntegerMap<Territory> m_PUsLost = new IntegerMap<Territory>();
    
    
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
        Iterator<Territory> allTerritories = m_data.getMap().getTerritories().iterator();
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
            Collection<Unit> transports = Match.getMatches(units, Matches.UnitIsTransport);
            Collection<Unit> land = Match.getMatches(units, Matches.UnitIsLand);
            Iterator<Unit> landIter = land.iterator();
            
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

        m_bridge = new TripleADelegateBridge(aBridge, gameData);
        PlayerID player = aBridge.getPlayerID();

        m_data = gameData;
        m_player = player;

        if (m_firstRun)
            firstRun();
        
        // repair 2-hit units at beginning of turn (some maps have combat move before purchase, so i think it is better to do this at beginning of combat move)
        if(!m_nonCombat && games.strategy.triplea.Properties.getBattleships_Repair_At_Beginning_Of_Round(m_data))
        	repairBattleShips(m_bridge, m_data, m_player);
        
        // give movement to units which begin the turn in the same territory as units with giveMovement (like air and naval bases)
        if (!m_nonCombat && games.strategy.triplea.Properties.getUnitsMayGiveBonusMovement(m_data))
        	giveBonusMovement(m_bridge, m_data, m_player);
        
        // placing triggered units at beginning of combat move.
        if(!m_nonCombat && games.strategy.triplea.Properties.getTriggers(m_data))
        	TriggerAttachment.triggerUnitPlacement(player,m_bridge,gameData);
        
        if(m_tempMovePerformer != null)
        {
            m_tempMovePerformer.initialize(this, m_data, aBridge);
            m_tempMovePerformer.resume();
            m_tempMovePerformer = null;
        }
    }
    
    /**
     * This entire method relies on the fact that "TripleAUnit.ALREADY_MOVED" can be a negative value.
     * Normally TripleAUnit.ALREADY_MOVED is positive, and so it is increased each time the unit moves.  
     * But since we make it a negative value here, a unit is temporarily gaining movement for this turn.  
     * Thankfully the movement validator takes this into account.
     * 
     * changing ALREADY_MOVED means that a unit will not be able to match certain things like 'has not moved' and 'has moved' correctly... we should change to a separate bonus somehow
     * 
     * (veqryn)
     */
    private void giveBonusMovement(IDelegateBridge aBridge, GameData data, PlayerID player)
    {
    	CompositeChange change = new CompositeChange();
    	for(Territory t : data.getMap().getTerritories())
    	{
    		for(Unit u : t.getUnits().getUnits())
    		{
    			if (Matches.UnitCanBeGivenBonusMovementByFacilitiesInItsTerritory(t, player, data).match(u))
    			{
    				if (!Matches.isUnitAllied(player, data).match(u))
    					continue;
    				
    				int bonusMovement = Integer.MIN_VALUE;
    				Collection<Unit> givesBonusUnits = new ArrayList<Unit>();
    				Match<Unit> givesBonusUnit = new CompositeMatchAnd<Unit>(Matches.alliedUnit(player, data), Matches.UnitCanGiveBonusMovementToThisUnit(u));
                	
                	givesBonusUnits.addAll(Match.getMatches(t.getUnits().getUnits(), givesBonusUnit));
                	
                	if (Matches.UnitIsSea.match(u))
                	{
                		Match<Unit> givesBonusUnitLand = new CompositeMatchAnd<Unit>(givesBonusUnit, Matches.UnitIsLand);
                		List<Territory> neighbors = new ArrayList<Territory>(data.getMap().getNeighbors(t, Matches.TerritoryIsLand));
                		Iterator iter = neighbors.iterator();
                		while (iter.hasNext())
                		{
                			Territory current = (Territory) iter.next();
                			givesBonusUnits.addAll(Match.getMatches(current.getUnits().getUnits(), givesBonusUnitLand));
                		}
                	}
                	
                	for (Unit bonusGiver : givesBonusUnits)
                	{
                		int tempBonus = UnitAttachment.get(bonusGiver.getType()).getGivesMovement().getInt(u.getType());
                		if (tempBonus > bonusMovement)
                			bonusMovement = tempBonus;
                	}
                	
                	if (bonusMovement != Integer.MIN_VALUE)
                	{
                		// changing ALREADY_MOVED means that a unit will not be able to match certain things like 'has not moved' and 'has moved' correctly... we should change to a separate bonus somehow
                		bonusMovement = bonusMovement * -1;
                		bonusMovement = Math.min(bonusMovement, UnitAttachment.get(u.getType()).getMovement(player));
                		change.add(ChangeFactory.unitPropertyChange(u,bonusMovement, TripleAUnit.ALREADY_MOVED));
                	}
    			}
    		}
    	}
    	if (!change.isEmpty())
    	{
    		aBridge.getHistoryWriter().startEvent("Giving bonus movement to units");
            aBridge.addChange(change);
    	}
    }
    
    private void repairBattleShips(IDelegateBridge aBridge, GameData data, PlayerID player)
    {
       Match<Unit> damagedBattleship = new CompositeMatchAnd<Unit>(Matches.UnitIsTwoHit, Matches.UnitIsDamaged, Matches.unitIsOwnedBy(player));
        
       Collection<Unit> damaged = new ArrayList<Unit>();
       Iterator iter = data.getMap().getTerritories().iterator();
       while(iter.hasNext())
       {
           Territory current = (Territory) iter.next();
           if (!games.strategy.triplea.Properties.getTwoHitPointUnitsRequireRepairFacilities(data))
        	   damaged.addAll(current.getUnits().getMatches(damagedBattleship));
           else
        	   damaged.addAll(current.getUnits().getMatches(new CompositeMatchAnd<Unit>(damagedBattleship, Matches.UnitCanBeRepairedByFacilitiesInItsTerritory(current, player, data))));
       }

       if(damaged.size() == 0)
           return;
       
       IntegerMap<Unit> hits = new IntegerMap<Unit>();
       iter = damaged.iterator();
       while(iter.hasNext())
       {
           Unit unit = (Unit) iter.next();
           hits.put(unit,0);
       }
       aBridge.addChange(ChangeFactory.unitsHit(hits));
       aBridge.getHistoryWriter().startEvent(damaged.size() + " " +  MyFormatter.pluralize("unit", damaged.size()) + " repaired.");
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

        moveToUndo.undo(m_data,m_bridge);
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

    private PlayerID getUnitOwner(Collection<Unit> units)
    {
        if (!units.isEmpty())
            return units.iterator().next().getOwner();
        else
            return m_player;
    }

    public String move(Collection<Unit> units, Route route)
    {
        return move(units, route, Collections.<Unit>emptyList());
    }

    public String move(Collection<Unit> units, Route route, Collection<Unit> transportsThatCanBeLoaded)
    {
        PlayerID player = getUnitOwner(units);
        
        MoveValidationResult result = MoveValidator.validateMove(units, 
                                                                 route, 
                                                                 player, 
                                                                 transportsThatCanBeLoaded,
                                                                 m_nonCombat,
                                                                 m_movesToUndo,
                                                                 m_data);

        StringBuilder errorMsg = new StringBuilder(100);

        int numProblems = result.getTotalWarningCount() - (result.hasError() ? 0 : 1);

        String numErrorsMsg = numProblems > 0 ? ("; "+ numProblems + " " + MyFormatter.pluralize("error", numProblems) + " not shown") : "";

        if (result.hasError())
            return errorMsg.append(result.getError()).append(numErrorsMsg).toString();

        if (result.hasDisallowedUnits())
            return errorMsg.append(result.getDisallowedUnitWarning(0)).append(numErrorsMsg).toString();

        boolean isKamikaze = false;
        boolean getKamikazeAir = games.strategy.triplea.Properties.getKamikaze_Airplanes(m_data);
        Collection<Unit> kamikazeUnits = new ArrayList<Unit>();
        //boolean isHariKari = false;
        // confirm kamikaze moves, and remove them from unresolved units
        //if(m_data.getProperties().get(Constants.KAMIKAZE, false))
        if(getKamikazeAir || Match.someMatch(units, Matches.UnitIsKamikaze))
        {
            kamikazeUnits = result.getUnresolvedUnits(MoveValidator.NOT_ALL_AIR_UNITS_CAN_LAND);
            if (kamikazeUnits.size() > 0 && getRemotePlayer().confirmMoveKamikaze())
            {
                for (Unit unit : kamikazeUnits) 
                {
                	if (getKamikazeAir || Matches.UnitIsKamikaze.match(unit))
                	{
                		result.removeUnresolvedUnit(MoveValidator.NOT_ALL_AIR_UNITS_CAN_LAND, unit);
                        isKamikaze = true;
                	}
                }
            }
        }
        
        // confirm HariKari moves, and remove them from unresolved units
        /*if(m_data.getProperties().get(Constants.HARI_KARI, false))
        {
            Collection<Unit> hariKariUnits = result.getUnresolvedUnits(MoveValidator.UNESCORTED_UNITS_WILL_DIE_IN_COMBAT);
            if (hariKariUnits.size() > 0 && getRemotePlayer().confirmMoveHariKari())
            {
                for (Unit unit : hariKariUnits) 
                {
                    result.removeUnresolvedUnit(MoveValidator.UNESCORTED_UNITS_WILL_DIE_IN_COMBAT, unit);
                    isHariKari = true;
                }
            }
        }        */
        

        if (result.hasUnresolvedUnits())
            return errorMsg.append(result.getUnresolvedUnitWarning(0)).append(numErrorsMsg).toString();

        // allow user to cancel move if aa guns will fire
        AAInMoveUtil aaInMoveUtil = new AAInMoveUtil();
        aaInMoveUtil.initialize(m_bridge, m_data);
        Collection<Territory> aaFiringTerritores = aaInMoveUtil.getTerritoriesWhereAAWillFire(route, units);
        if(!aaFiringTerritores.isEmpty())
        {
            if(!getRemotePlayer().confirmMoveInFaceOfAA(aaFiringTerritores))
                return null;
        }
        
        //do the move
        UndoableMove currentMove = new UndoableMove(m_data, units, route);

        String transcriptText = MyFormatter.unitsToTextNoOwner(units) + " moved from " + route.getStart().getName() + " to " + route.getEnd().getName();
        m_bridge.getHistoryWriter().startEvent(transcriptText);
        if(isKamikaze)
        {
        	m_bridge.getHistoryWriter().addChildToEvent("This was a kamikaze move, for at least some of the units", kamikazeUnits);
        }
        /*if(isHariKari)
        {
            m_bridge.getHistoryWriter().addChildToEvent("This was a Hari-Kari move");
        }*/
        MoveDescription description = new MoveDescription(units, route);
        m_bridge.getHistoryWriter().setRenderingData(description);

        
        m_tempMovePerformer = new MovePerformer();
        m_tempMovePerformer.initialize(this, m_data, m_bridge);
        m_tempMovePerformer.moveUnits(units, route, player, transportsThatCanBeLoaded, currentMove);
        m_tempMovePerformer = null;

        return null;
    }
    
    void updateUndoableMoves(UndoableMove currentMove)
    {
        currentMove.initializeDependencies(m_movesToUndo);
        m_movesToUndo.add(currentMove);
        updateUndoableMoveIndexes();
    }

    public static BattleTracker getBattleTracker(GameData data)
    {
        return DelegateFinder.battleDelegate(data).getBattleTracker();
    }

    private boolean isWW2V2()
    {
    	return games.strategy.triplea.Properties.getWW2V2(m_data);
    }

    private boolean isPreviousUnitsFight()
    {
    	return games.strategy.triplea.Properties.getPreviousUnitsFight(m_data);
    }

    private boolean isWW2V3()
    {
        return games.strategy.triplea.Properties.getWW2V3(m_data);
    }
    
    private ITripleaPlayer getRemotePlayer()
    {
        return getRemotePlayer(m_player);
    }

    private ITripleaPlayer getRemotePlayer(PlayerID id)
    {
        return (ITripleaPlayer) m_bridge.getRemote(id);
    }


    public static Collection<Territory> getEmptyNeutral(Route route)
    {

        Match<Territory> emptyNeutral = new CompositeMatchAnd<Territory>(Matches.TerritoryIsEmpty, Matches.TerritoryIsNeutral);
        Collection<Territory> neutral = route.getMatches(emptyNeutral);
        return neutral;
    }


    /**
     * Mark units as having no movement.
     */
    public Change markNoMovementChange(Collection<Unit> units)
    {
        if(units.isEmpty()) 
            return ChangeFactory.EMPTY_CHANGE;
        
        CompositeChange change = new CompositeChange();
        Iterator<Unit> iter = units.iterator();
        while (iter.hasNext())
        {
            change.add(markNoMovementChange(iter.next()));
        }
        return change;
    }

    
    public Change ensureCanMoveOneSpaceChange(Unit unit)
    {
        int alreadyMoved = TripleAUnit.get(unit).getAlreadyMoved();
        int maxMovement = UnitAttachment.get(unit.getType()).getMovement(unit.getOwner());
        return ChangeFactory.unitPropertyChange(unit, Math.min(alreadyMoved, maxMovement - 1), TripleAUnit.ALREADY_MOVED);
    }

    private Change markNoMovementChange(Unit unit)
    {        
        UnitAttachment ua = UnitAttachment.get(unit.getType());
        return ChangeFactory.unitPropertyChange(unit, ua.getMovement(unit.getOwner()), TripleAUnit.ALREADY_MOVED);
    }

    /**
     * Called before the delegate will stop running.
     */
    public void end()
    {
    	

        if (m_nonCombat)
            removeAirThatCantLand();
        
        m_movesToUndo.clear();

        //WW2V2, fires at end of combat move
        //WW2V1, fires at end of non combat move
        if ((!m_nonCombat && isWW2V3()) || (m_nonCombat && (!isWW2V2() && !isWW2V3())) || (!m_nonCombat && isWW2V2()))
        {
            if (TechTracker.hasRocket(m_bridge.getPlayerID()))
            {
                RocketsFireHelper helper = new RocketsFireHelper();
                helper.fireRockets(m_bridge, m_data, m_bridge.getPlayerID());
            }
        }
        CompositeChange change = new CompositeChange();

        if(!m_nonCombat && (isWW2V3() || isWW2V2() || isPreviousUnitsFight())) 
        {
            change.add(addLingeringSeaUnitsToBattles());
        }
        
        //do at the end of the round
        //if we do it at the start of non combat, then
        //we may do it in the middle of the round, while loading.
        if (m_nonCombat)
        {
            for(Unit u : m_data.getUnits()) 
            {
                if(TripleAUnit.get(u).getAlreadyMoved() != 0 ) 
                {
                    change.add(ChangeFactory.unitPropertyChange(u,0, TripleAUnit.ALREADY_MOVED));
                }
                if(TripleAUnit.get(u).getSubmerged()) 
                {
                    change.add(ChangeFactory.unitPropertyChange(u,false, TripleAUnit.SUBMERGED));
                }
            }
            
            change.add(m_transportTracker.endOfRoundClearStateChange(m_data));
            m_PUsLost.clear();                        
        }

        if(!change.isEmpty()) 
        {
            // if no non-combat occurred, we may have cleanup left from combat
            // that we need to spawn an event for
            m_bridge.getHistoryWriter().startEvent("Cleaning up after movement phases");
            m_bridge.addChange(change);
        }
    }
    
    private Change addLingeringSeaUnitsToBattles() 
    {
        //if an enemy placed units in a hostile sea zone
        //and then during combat move, we attacked the newly placed units
        //our units in the sea zone need to join the battle
        CompositeChange change = new CompositeChange();
        BattleTracker tracker = getBattleTracker(m_data);
        for(Territory t : tracker.getPendingBattleSites(false)) 
        {
            if(!t.isWater()) 
            {
                continue;
            }
            if(!(tracker.getPendingBattle(t, false) instanceof MustFightBattle)) 
            {
                continue;
            }
            MustFightBattle mfb = (MustFightBattle) tracker.getPendingBattle(t,false);
            Set<Unit> ownedSeaUnits = new HashSet<Unit>(t.getUnits().getMatches(new CompositeMatchAnd<Unit>(
                Matches.UnitIsLand.invert(),
                Matches.unitIsOwnedBy(m_player))));
             ownedSeaUnits.removeAll(mfb.getAttackingUnits());
             if(!ownedSeaUnits.isEmpty()) 
             {
                 change.add(mfb.addAttackChange(new Route(t), ownedSeaUnits));
             }
            
        }
        return change;
        
    }

    private void removeAirThatCantLand()
    {
        boolean lhtrCarrierProd = AirThatCantLandUtil.isLHTRCarrierProduction(m_data) || AirThatCantLandUtil.isLandExistingFightersOnNewCarriers(m_data);
        boolean hasProducedCarriers = m_player.getUnits().someMatch(Matches.UnitIsCarrier);
        AirThatCantLandUtil util = new AirThatCantLandUtil(m_data, m_bridge);
        util.removeAirThatCantLand(m_player, lhtrCarrierProd && hasProducedCarriers);
        // if edit mode has been on, we need to clean up after all players
        Iterator<PlayerID> iter = m_data.getPlayerList().iterator();
        while (iter.hasNext())
        {
            PlayerID player = iter.next();
            //Check if player still has units to place
            if (!player.equals(m_player)) // && !player.getUnits().isEmpty()
                util.removeAirThatCantLand(player, (player.getUnits().someMatch(Matches.UnitIsCarrier) && lhtrCarrierProd));
        }
    }
   
    /**
     * returns a map of unit -> transport. returns null if no mapping can be
     * done either because there is not sufficient transport capacity or because
     * a unit is not with its transport
     * This method is static so it can be called from the client side.
     */
    public static Map<Unit, Unit> mapTransports(Route route, Collection<Unit> units, Collection<Unit> transportsToLoad)
    {
        if (MoveValidator.isLoad(route))
            return mapTransportsToLoad(units, transportsToLoad);
        if (MoveValidator.isUnload(route))
            return mapTransportsAlreadyLoaded(units, route.getStart().getUnits().getUnits());
        return mapTransportsAlreadyLoaded(units, units);
    }
    
    /**
     * returns a map of unit -> transport. returns null if no mapping can be
     * done either because there is not sufficient transport capacity or because
     * a unit is not with its transport
     * This method is static so it can be called from the client side.
     */
    public static Map<Unit, Unit> mapTransports(Route route, Collection<Unit> units, Collection<Unit> transportsToLoad, boolean isload, PlayerID player)
    {    	
        if (isload)
        	return mapTransportsToLoad(units, transportsToLoad);
        if (MoveValidator.isUnload(route))
            return mapTransportsAlreadyLoaded(units, route.getStart().getUnits().getUnits());
        return mapTransportsAlreadyLoaded(units, units);
    }
    
    /**
     * Returns a list of max number of each type of unit that may be loaded
     * This method is static so it can be called from the client side.
     */
    public static List<Unit> mapAirTransportPossibilities(Route route, Collection<Unit> units, Collection<Unit> transportsToLoad, boolean isload, PlayerID player)
    {
    		//return mapAirTransportsToLoad(units, Match.getMatches(transportsToLoad, Matches.UnitIsAirTransport));
    	return mapAirTransportsToLoad2(units, Match.getMatches(transportsToLoad, Matches.UnitIsAirTransport));
    }

    /**
     * returns a map of unit -> transport. returns null if no mapping can be
     * done either because there is not sufficient transport capacity or because
     * a unit is not with its transport
     * This method is static so it can be called from the client side.
     */
    public static Map<Unit, Unit> mapAirTransports(Route route, Collection<Unit> units, Collection<Unit> transportsToLoad, boolean isload, PlayerID player)
    {
    	return mapTransports(route, units, transportsToLoad, isload, player);
    		//return mapUnitsToAirTransports(units, Match.getMatches(transportsToLoad, Matches.UnitIsAirTransport));
    }
    
    /**
     * Returns a map of unit -> transport. Unit must already be loaded in the
     * transport.  If no units are loaded in the transports then an empty Map will
     * be returned.
     */
    private static Map<Unit, Unit> mapTransportsAlreadyLoaded(Collection<Unit> units, Collection<Unit> transports)
    {

        Collection<Unit> canBeTransported = Match.getMatches(units, Matches.UnitCanBeTransported);
        Collection<Unit> canTransport = Match.getMatches(transports, Matches.UnitCanTransport);
        TransportTracker transportTracker = new TransportTracker();

        Map<Unit, Unit> mapping = new HashMap<Unit, Unit>();
        Iterator<Unit> land = canBeTransported.iterator();
        while (land.hasNext())
        {
            Unit currentTransported = (Unit) land.next();
            Unit transport = transportTracker.transportedBy(currentTransported);
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
     * units. If it can't succeed returns an empty Map.
     *  
     */
    private static Map<Unit, Unit> mapTransportsToLoad(Collection<Unit> units, Collection<Unit> transports)
    {

        List<Unit> canBeTransported = Match.getMatches(units, Matches.UnitCanBeTransported);
        int transportIndex = 0;
        TransportTracker transportTracker = new TransportTracker();

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
        //in WW2V2 rules.
        Collections.sort(canBeTransported, c);

        List<Unit> canTransport = Match.getMatches(transports, Matches.UnitCanTransport);

        Map<Unit, Unit> mapping = new HashMap<Unit, Unit>();
        IntegerMap<Unit> addedLoad = new IntegerMap<Unit>();

        Iterator<Unit> landIter = canBeTransported.iterator();
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
            //TODO review the following loop in light of bug ticket 2827064- previously unloaded trns perhaps shouldn't be included.
            Iterator<Unit> transportIter = Util.shiftElementsToEnd(canTransport, transportIndex).iterator();
            while (transportIter.hasNext() && !loaded)
            {
                transportIndex++;
                if(transportIndex >= canTransport.size())
                    transportIndex = 0;
                
                Unit transport = (Unit) transportIter.next();
                int capacity = transportTracker.getAvailableCapacity(transport);
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

    /**
     * Returns a list of the maximum number of each type of unit that can be loaded on the transports
     * If it can't succeed returns an empty Map.
     *  
     */
    private static List<Unit> mapAirTransportsToLoad(Collection<Unit> units, Collection<Unit> transports)
    {
    	Comparator<Unit> c = new Comparator<Unit>()
        {
            public int compare(Unit o1, Unit o2)
            {
                int cost1 = UnitAttachment.get(((Unit) o1).getUnitType()).getTransportCost();
                int cost2 = UnitAttachment.get(((Unit) o2).getUnitType()).getTransportCost();
                return cost2 - cost1; //descending transportCost
            }
        };
        Collections.sort((List<Unit>) units, c);
        
        Iterator<Unit> trnIter = transports.iterator();
    	//Spin through each transport and find the possible loads
        List<Unit> totalLoad = new ArrayList<Unit>();
    	while(trnIter.hasNext())
    	{
    		//(re)set the initial and current capacity of the air transport
    		Unit transport = trnIter.next();
    		UnitAttachment trnA = (UnitAttachment) transport.getType().getAttachment(Constants.UNIT_ATTATCHMENT_NAME);
    		int initCapacity = trnA.getTransportCapacity();
    		int currCapacity = initCapacity;

    		//set up a list for a single potential load
            List<Unit> aLoad = new ArrayList<Unit>();
        	Iterator<Unit> unitIter = units.iterator();
            IntegerMap<Unit> addedLoad = new IntegerMap<Unit>();
        	while (unitIter.hasNext())
        	{
        		//For each potential unit, get transport cost
        		Unit unit = unitIter.next();
        		UnitAttachment ua = (UnitAttachment) unit.getType().getAttachment(Constants.UNIT_ATTATCHMENT_NAME);
        		int cost = ua.getTransportCost();
        		//Check the cost against the air transport's current capacity (including previously loaded units)
        		currCapacity -= addedLoad.getInt(transport);
        		if(currCapacity >= cost )
        		{
        			addedLoad.add(transport, cost);
        			aLoad.add(unit);
        		}
        		else
        		{
        			//If there's no available capacity, consider the load full and add to total
        			totalLoad.addAll(aLoad);
        			addedLoad.clear();
        			aLoad.clear();
        			//see if any units like the current unit were previously loaded
        			Iterator<Unit> ttlIter = totalLoad.listIterator();
        			List<Integer> indices = new ArrayList<Integer>();
        			while (ttlIter.hasNext())
        			{
        				Unit ttlUnit = ttlIter.next();
        				if(unit != ttlUnit && unit.getType().equals(ttlUnit.getType()))
        				{
        					indices.add(totalLoad.indexOf(ttlUnit));
        				}
        			}
        			//If there are any, add up their transportCosts and see if there is room for another.
        			currCapacity = initCapacity;
        			if(indices.isEmpty())
        			{
        				if(currCapacity >= cost )
        				{
        					addedLoad.add(transport, cost);
        					aLoad.add(unit);
        				}
        			}
        			else
        			{
        				//reload aLoad with any units of the same type & check capacity vs aLoad cost
        				//this eliminates too many duplicate units in the list
        				Iterator<Integer> indCosts = indices.listIterator();
        				while(indCosts.hasNext())
        				{
        					Integer index = indCosts.next();
        					Unit indexedUnit = totalLoad.get(index);
        					UnitAttachment indexedUnitAtt = (UnitAttachment) indexedUnit.getType().getAttachment(Constants.UNIT_ATTATCHMENT_NAME);
        					currCapacity -= indexedUnitAtt.getTransportCost();
        				}
        				if(currCapacity >= cost )
        				{
        					addedLoad.add(transport, cost);
        					aLoad.add(unit);
        				}
        			}
        		}    		
            }
			//If there's no available capacity, consider the load full and add to total
			totalLoad.addAll(aLoad);
    	}

        return totalLoad;
    }
    
    private static List<Unit> mapAirTransportsToLoad2(Collection<Unit> units, Collection<Unit> transports)
    {
    	Comparator<Unit> c = new Comparator<Unit>()
        {
            public int compare(Unit o1, Unit o2)
            {
                int cost1 = UnitAttachment.get(((Unit) o1).getUnitType()).getTransportCost();
                int cost2 = UnitAttachment.get(((Unit) o2).getUnitType()).getTransportCost();
                return cost2 - cost1; //descending transportCost
            }
        };
        Collections.sort((List<Unit>) units, c);
        
        //Define the max of all units that could be loaded
        List<Unit> totalLoad = new ArrayList<Unit>();
        
		//Get a list of the unit categories
        Collection<UnitCategory> unitTypes = UnitSeperator.categorize(units, null, false, true);
        Collection<UnitCategory> transportTypes = UnitSeperator.categorize(transports, null, false, false);
		
        for(UnitCategory unitType : unitTypes)
        {
        	int transportCost = unitType.getTransportCost();
        	
        	for(UnitCategory transportType : transportTypes)
        	{
        		int transportCapacity = UnitAttachment.get(transportType.getType()).getTransportCapacity();
        		if(transportCost > 0 && transportCapacity >= transportCost)
        		{
        			int transportCount = Match.countMatches(transports, Matches.unitIsOfType(transportType.getType()));
        			int ttlTransportCapacity = transportCount * (int) Math.floor(transportCapacity/transportCost);
        			totalLoad.addAll(Match.getNMatches(units, ttlTransportCapacity, Matches.unitIsOfType(unitType.getType())));
        		}
        	}
        }

        return totalLoad;
    }
    
    public Collection<Territory> getTerritoriesWhereAirCantLand(PlayerID player)
    {
        return new AirThatCantLandUtil(m_data, m_bridge).getTerritoriesWhereAirCantLand(player);
    }
    
    public Collection<Territory> getTerritoriesWhereAirCantLand()
    {
        return new AirThatCantLandUtil(m_data, m_bridge).getTerritoriesWhereAirCantLand(m_player);
    }


    public Collection<Territory> getTerritoriesWhereUnitsCantFight()
    {
        return new UnitsThatCantFightUtil(m_data).getTerritoriesWhereUnitsCantFight(m_player);
    }

 

    /**
     * Find the route that a unit used to move into the given territory.
     */
    public Route getRouteUsedToMoveInto(Unit unit, Territory end)
    {
        return MoveDelegate.getRouteUsedToMoveInto(m_movesToUndo, unit, end);
    }

    /**
     * Find the route that a unit used to move into the given territory.
     * This method is static so it can be called from the client side.
     */
    public static Route getRouteUsedToMoveInto(final List<UndoableMove> undoableMoves, Unit unit, Territory end)
    {
        for (int i = undoableMoves.size() - 1; i >= 0; i--)
        {
            UndoableMove move = undoableMoves.get(i);
            if (!move.getUnits().contains(unit))
                continue;
            if (move.getRoute().getEnd().equals(end))
                return move.getRoute();
        }
        return null;
    }


    /**
     * Return the number of PUs that have been lost by bombing, rockets, etc.
     */
    public int PUsAlreadyLost(Territory t)
    {
        return m_PUsLost.getInt(t);
    }

    /**
     * Add more PUs lost to a territory due to bombing, rockets, etc.
     */
    public void PUsLost(Territory t, int amt)
    {
        m_PUsLost.add(t, amt);
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
        if (saveUndo)
            state.m_movesToUndo = m_movesToUndo;
        state.m_PUsLost = m_PUsLost;
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
        //if the undo state wasnt saved, then dont load it
        //prevents overwriting undo state when we restore from an undo move
        if (state.m_movesToUndo != null)
            m_movesToUndo = state.m_movesToUndo;
        m_PUsLost = state.m_PUsLost;
        m_tempMovePerformer = state.m_tempMovePerformer;
    }
}

class MoveState implements Serializable
{
    public boolean m_firstRun = true;
    public boolean m_nonCombat;
    public IntegerMap<Territory> m_PUsLost;
    public List<UndoableMove> m_movesToUndo;
    public MovePerformer m_tempMovePerformer;

}
