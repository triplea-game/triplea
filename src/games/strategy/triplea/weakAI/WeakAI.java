package games.strategy.triplea.weakAI;

/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

import games.strategy.engine.data.*;
import games.strategy.engine.gamePlayer.*;
import games.strategy.net.GUID;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attatchments.*;
import games.strategy.triplea.delegate.*;
import games.strategy.triplea.delegate.dataObjects.*;
import games.strategy.triplea.delegate.remote.*;
import games.strategy.triplea.player.ITripleaPlayer;
import games.strategy.util.*;

import java.util.*;
import java.util.logging.Logger;




/*
 *
 * A very weak ai, based on some simple rules.<p>
 * 
 * run with -Dtriplea.randomai.pause=false to eliminate the human friendly pauses 
 *
 * @author Sean Bridges
 */
public class WeakAI implements IGamePlayer, ITripleaPlayer
{
    private final static Logger s_logger = Logger.getLogger(WeakAI.class.getName());
    
    private final String m_name;
    private PlayerID m_id;
    private IPlayerBridge m_bridge;

   
    /** Creates new TripleAPlayer */
    public WeakAI(String name)
    {
        m_name = name;
    }
    
    private boolean m_pause =  Boolean.valueOf(System.getProperties().getProperty("triplea.weakai.pause", "true"));
    private void pause()
    {
        if(m_pause)
        {
            try
            {
                Thread.sleep(700);
            } catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }
    }
    

    public String getName()
    {
        return m_name;
    }

    public PlayerID getID()
    {
        return m_id;
    }

    public void initialize(IPlayerBridge bridge, PlayerID id)
    {
        m_bridge = bridge;
        m_id = id;
    }
    
    

    public void start(String name)
    {
        if (name.endsWith("Bid"))
            purchase(true);
        else if (name.endsWith("Tech"))
            tech();
        else if (name.endsWith("Purchase"))
            purchase(false);
        else if (name.endsWith("Move"))
            move(name.endsWith("NonCombatMove"));
        else if (name.endsWith("Battle"))
            battle();
        else if (name.endsWith("Place"))
            place(name.indexOf("Bid") != -1);
        else if (name.endsWith("EndTurn"))
        {}
    }



    private void tech()
    {}

    private Route getAmphibRoute()
    {
        if(!isAmphibAttack())
            return null;
        
        final GameData data = m_bridge.getGameData();
        
        Territory ourCapitol = TerritoryAttachment.getCapital(m_id, data);
        Match<Territory> endMatch = new Match<Territory>()
        {
            @Override
            public boolean match(Territory o)
            {
                boolean impassable = TerritoryAttachment.get(o) != null &&  TerritoryAttachment.get(o) .isImpassible();
                return  !impassable && !o.isWater() && Utils.hasLandRouteToEnemyOwnedCapitol(o, m_id, data);
            }
        
        };
        
        Match<Territory> routeCond = new CompositeMatchAnd<Territory>(Matches.TerritoryIsWater, Matches.territoryHasNoEnemyUnits(m_id, data));
        
        Route withNoEnemy = Utils.findNearest(ourCapitol, endMatch, routeCond,
                m_bridge.getGameData());
        if(withNoEnemy != null)
            return withNoEnemy;

        //this will fail if our capitol is not next to water, c'est la vie.
        return  Utils.findNearest(ourCapitol, endMatch, Matches.TerritoryIsWater,
                m_bridge.getGameData());
    }
    
    private boolean isAmphibAttack()
    {
        Territory capitol = TerritoryAttachment.getCapital(m_id, m_bridge.getGameData());
        //we dont own our own capitol
        if(capitol == null || !capitol.getOwner().equals(m_id))
            return false;
        
        //find a land route to an enemy territory from our capitol
        Route invasionRoute = Utils.findNearest(capitol, Matches.isTerritoryEnemyAndNotNeutral(m_id, m_bridge.getGameData()),
                Matches.TerritoryIsLand, m_bridge.getGameData());
        
        return invasionRoute == null;
    }
    
    private void move(boolean nonCombat)
    {


        
        if(nonCombat)
        {
            doNonCombatMove();
        }
        else
        {
            doCombatMove();
        }
        
        
        pause();
    }


    private void doNonCombatMove()
    {
        GameData data = m_bridge.getGameData();
        
        List<Collection<Unit>> moveUnits = new ArrayList<Collection<Unit>>();
        List<Route> moveRoutes = new ArrayList<Route>();
        List<Collection<Unit>> transportsToLoad = new ArrayList<Collection<Unit>>();
        
        //load the transports first
        //they may be able to move farther
        populateTransportLoad(false, data, moveUnits, moveRoutes, transportsToLoad);
        doMove(moveUnits, moveRoutes, transportsToLoad);
        
        moveRoutes.clear();
        moveUnits.clear();
        transportsToLoad.clear();
        
        //do the rest of the moves
        populateNonCombat(data, moveUnits, moveRoutes);    
        populateNonCombatSea(true, data, moveUnits, moveRoutes);
        
        doMove(moveUnits, moveRoutes, null);
        
        moveUnits.clear();
        moveRoutes.clear();
        transportsToLoad.clear();
        
        //load the transports again if we can
        //they may be able to move farther
        populateTransportLoad(false, data, moveUnits, moveRoutes, transportsToLoad);
        doMove(moveUnits, moveRoutes, transportsToLoad);
        
        moveRoutes.clear();
        moveUnits.clear();
        transportsToLoad.clear();
        
        
        //unload the transports that can be unloaded
        populateTransportUnloadNonCom( true, data, moveUnits, moveRoutes);
        doMove(moveUnits, moveRoutes, null);
    }


    private void doCombatMove()
    {
        GameData data = m_bridge.getGameData();
        
        List<Collection<Unit>> moveUnits = new ArrayList<Collection<Unit>>();
        List<Route> moveRoutes = new ArrayList<Route>();
        List<Collection<Unit>> transportsToLoad = new ArrayList<Collection<Unit>>();
        
        //load the transports first
        //they may be able to take part in a battle
        populateTransportLoad(true, data, moveUnits, moveRoutes, transportsToLoad);
        doMove(moveUnits, moveRoutes, transportsToLoad);
        
        moveRoutes.clear();
        moveUnits.clear();
        
        //we want to move loaded transports before we try to fight our battles 
        populateNonCombatSea(false, data, moveUnits, moveRoutes);
        doMove(moveUnits, moveRoutes, null);
        
        moveUnits.clear();
        moveRoutes.clear();
        transportsToLoad.clear();

        
        
        //fight
        populateCombatMove(data, moveUnits, moveRoutes);
        populateCombatMoveSea(data, moveUnits, moveRoutes);
        
        doMove(moveUnits, moveRoutes, null);
    }


    private void populateTransportLoad(boolean nonCombat, GameData data, List<Collection<Unit>> moveUnits, List<Route> moveRoutes, List<Collection<Unit>> transportsToLoad)
    {
        
        TransportTracker tracker = DelegateFinder.moveDelegate(data).getTransportTracker();
        
        if(!isAmphibAttack())
            return;
        Territory capitol = TerritoryAttachment.getCapital(m_id, data);
        if(capitol == null ||!capitol.getOwner().equals(m_id))
            return;
        List<Unit> unitsToLoad = capitol.getUnits().getMatches( Matches.UnitIsAAOrFactory.invert());
        
        for(Territory neighbor : data.getMap().getNeighbors(capitol))
        {
            if(!neighbor.isWater())
                continue;
            
            List<Unit> units = new ArrayList<Unit>();
            for(Unit transport : neighbor.getUnits().getMatches(Matches.unitIsOwnedBy(m_id)))
            {
                int free = tracker.getAvailableCapacity(transport);
                if(free <= 0)
                    continue;
                
                Iterator<Unit> iter = unitsToLoad.iterator();
                while(iter.hasNext() && free > 0)
                {
                    Unit current = iter.next();
                    UnitAttachment ua = UnitAttachment.get(current.getType());
                    if(ua.isAir())
                        continue;
                    if(ua.getTransportCost() <= free)
                    {
                        iter.remove();
                        free -= ua.getTransportCost();
                        units.add(current);
                    }
                }
                
            }
           
            if(units.size() > 0)
            {
                Route route = new Route();
                route.setStart(capitol);
                route.add(neighbor);
                moveUnits.add(units );
                moveRoutes.add(route);
                transportsToLoad.add( neighbor.getUnits().getMatches(Matches.UnitIsTransport));
            }
            
        }
    }
    
    private void populateTransportUnloadNonCom(boolean nonCombat, GameData data, List<Collection<Unit>> moveUnits, List<Route> moveRoutes)
    {
        Route amphibRoute = getAmphibRoute();
        if(amphibRoute == null)
            return;
        
        Territory lastSeaZoneOnAmphib = amphibRoute.getTerritories().get(amphibRoute.getLength() -1);
        Territory landOn = amphibRoute.getEnd();
        
        CompositeMatch<Unit> landAndOwned = new CompositeMatchAnd<Unit>(Matches.UnitIsLand, Matches.unitIsOwnedBy(m_id));
        List<Unit> units = lastSeaZoneOnAmphib.getUnits().getMatches(landAndOwned);
        if(units.size() > 0)
        {
            //just try to make the move, the engine will stop us if it doesnt work
            Route route = new Route();
            route.setStart(lastSeaZoneOnAmphib);
            route.add(landOn);
            moveUnits.add(units);
            moveRoutes.add(route);
        }
        
        
    }


    private void doMove(List<Collection<Unit>> moveUnits, List<Route> moveRoutes, List<Collection<Unit>> transportsToLoad)
    {
        IMoveDelegate moveDel = (IMoveDelegate) m_bridge.getRemote();
        for(int i =0; i < moveRoutes.size(); i++)
        {
            pause();
            
            
            if(moveRoutes.get(i) == null || moveRoutes.get(i).getEnd() == null || moveRoutes.get(i).getStart() == null)
            {
                s_logger.fine("Route not valid" + moveRoutes.get(i) + " units:" + moveUnits.get(i));
                continue;
            }
            
            String result;
            
            if(transportsToLoad == null)
                result = moveDel.move(moveUnits.get(i), moveRoutes.get(i)  );
            else
                result = moveDel.move(moveUnits.get(i), moveRoutes.get(i) , transportsToLoad.get(i) );
            
            if(result != null)
            {
                s_logger.fine("could not move " + moveUnits.get(i) + " over " + moveRoutes.get(i) + " because : " + result);
            }
        }
    }

    private void populateNonCombatSea(boolean nonCombat, final GameData data, List<Collection<Unit>> moveUnits, List<Route> moveRoutes)
    {
        
       Route amphibRoute = getAmphibRoute();
       
       Territory firstSeaZoneOnAmphib = null;
       Territory lastSeaZoneOnAmphib = null;
       if(amphibRoute != null)
       {
           firstSeaZoneOnAmphib = amphibRoute.getTerritories().get(1);
           lastSeaZoneOnAmphib = amphibRoute.getTerritories().get(amphibRoute.getLength() -1);
       }
       
       final Collection<Unit> alreadyMoved = new HashSet<Unit>();
       alreadyMoved.addAll(DelegateFinder.moveDelegate(data).getUnitsAlreadyMoved());
       
       Match<Unit> ownedAndNotMoved = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(m_id), new Match<Unit>()
        {
            @Override
            public boolean match(Unit o)
            {
                return !alreadyMoved.contains(o);
            }
        
        }
//       ,
//        new Match<Unit>()
//        {
//            @Override
//            public boolean match(Unit o)
//            {
//                return !DelegateFinder.moveDelegate(data).getTransportTracker().isTransporting(o);
//            }
//        
//        }
       );
       
       
       
       
       for(Territory t : data.getMap())
       {
           //move sea units to the capitol, unless they are loaded transports
           if(t.isWater())
           {
               //land units, move all towards the end point
               if(t.getUnits().someMatch(Matches.UnitIsLand))
               {
                   //move along amphi route
                   if(lastSeaZoneOnAmphib != null)
                   {
                       Route r = getMaxSeaRoute(data, t, lastSeaZoneOnAmphib);
                       
                       if(r != null && r.getLength() > 0)
                       {
                           moveRoutes.add(r);
                           List<Unit> unitsToMove = t.getUnits().getMatches(Matches.unitIsOwnedBy(m_id)); 
                           moveUnits.add( unitsToMove);
                           alreadyMoved.addAll(unitsToMove);
                       }
                   }
               }
               if(nonCombat &&  t.getUnits().someMatch(ownedAndNotMoved))
               {
                   //move toward the start of the amphib route
                   if(firstSeaZoneOnAmphib != null)
                   {
                       Route r = getMaxSeaRoute(data, t, firstSeaZoneOnAmphib);
                       moveRoutes.add(r);
                       moveUnits.add( t.getUnits().getMatches(ownedAndNotMoved));
                   }
               }
           }
           
           
           
       }
       
    }


    private Route getMaxSeaRoute(final GameData data, Territory start, Territory destination)
    {
        Match<Territory> routeCond = new CompositeMatchAnd<Territory>(
                Matches.TerritoryIsWater,
                Matches.territoryHasEnemyUnits(m_id, data).invert(),
                new Match<Territory>()
             {
                 public boolean match(Territory o)
                 {
                     if(CanalAttachment.get(o) == null)
                         return true;
                     return Match.allMatch( CanalAttachment.get(o).getLandTerritories(), Matches.isTerritoryAllied(m_id, data));
                     
                 }
             }
        );
        Route r = data.getMap().getRoute(start, destination, routeCond);
        if(r == null)
            return null;
        if(r.getLength() > 2)
        {
           Route newRoute = new Route();
           newRoute.setStart(start);
           newRoute.add( r.getTerritories().get(1) );
           newRoute.add( r.getTerritories().get(2) );
           r = newRoute;
        }
        return r;
    }


    private void populateCombatMoveSea(GameData data, List<Collection<Unit>> moveUnits, List<Route> moveRoutes)
    {
        final Collection<Unit> unitsAlreadyMoved = new HashSet<Unit>();
        
        for(Territory t : data.getMap())
        {
            if(!t.isWater())
                continue;
            if(!t.getUnits().someMatch(Matches.enemyUnit(m_id, data) ) )
            {
                continue;
            }
  
            Territory enemy = t;
            
            float enemyStrength = Utils.strength(enemy.getUnits().getUnits(), false, true);
            if(enemyStrength > 0)
            {
                CompositeMatch<Unit> attackable = new CompositeMatchAnd<Unit>(
                        Matches.unitIsOwnedBy(m_id), 
                        new Match<Unit>()
                        {
                            public boolean match(Unit o)
                            {
                                return !unitsAlreadyMoved.contains(o);
                            }
                        }
                        );
                
                Set<Territory> dontMoveFrom = new HashSet<Territory>();
                
                //find our strength that we can attack with
                int ourStrength = 0;
                Collection<Territory> attackFrom = data.getMap().getNeighbors(enemy, Matches.TerritoryIsWater); 
                for(Territory owned : attackFrom )
                {
                   //dont risk units we are carrying
                   if( owned.getUnits().someMatch(Matches.UnitIsLand) )
                   {
                       dontMoveFrom.add(owned);
                       continue;
                   }
                    
                   ourStrength += Utils.strength(owned.getUnits().getMatches(attackable), true, true);
                }
                
                
                if(ourStrength > 1.32 * enemyStrength)
                {
                    s_logger.fine("Attacking : " + enemy + " our strength:" + ourStrength + " enemy strength" + enemyStrength );
                    
                    for(Territory owned : attackFrom )
                    {
                        if(dontMoveFrom.contains(owned))
                            continue;
                        
                        List<Unit> units = owned.getUnits().getMatches(attackable);
                        unitsAlreadyMoved.addAll(units);
                        moveUnits.add(units);
                        moveRoutes.add(data.getMap().getRoute(owned, enemy));
                    }
                }
            }
            
  
            
            
        }
    }


    private void populateNonCombat(GameData data, List<Collection<Unit>> moveUnits, List<Route> moveRoutes)
    {
        Collection<Territory> territories = data.getMap().getTerritories();
        
        movePlanesHomeNonCom(moveUnits, moveRoutes);
        
        //move our units toward the nearest enemy capitol
        
        for(Territory t : territories)
        {
            if(t.isWater())
                continue;

            if(TerritoryAttachment.get(t).isCapital())
            {
                //if they are a threat to take our capitol, dont move
                //compare the strength of units we can place
                float ourStrength = Utils.strength( m_id.getUnits().getUnits() , false, false);
                float attackerStrength = Utils.getStrengthOfPotentialAttackers( t, data );
                
                if(attackerStrength > ourStrength)
                    continue;
            }
            
            //these are the units we can move
            CompositeMatch<Unit> moveOfType = new CompositeMatchAnd<Unit>();
            
            moveOfType.add(Matches.unitIsOwnedBy(m_id));            
            moveOfType.add(Matches.UnitIsNotAA);
            
            //we can never move factories
            moveOfType.add(Matches.UnitIsNotFactory);
            moveOfType.add(Matches.UnitIsLand);
            
            CompositeMatchAnd<Territory> moveThrough = new CompositeMatchAnd<Territory>(new InverseMatch<Territory>(Matches.TerritoryIsImpassible), 
                    new InverseMatch<Territory>(Matches.TerritoryIsNuetral),
                    Matches.TerritoryIsLand);
            
            List<Unit> units = t.getUnits().getMatches(moveOfType);
            if(units.size() == 0)
                continue;

            int minDistance = Integer.MAX_VALUE;
            Territory to = null;
            
            //find the nearest enemy owned capital
            for(PlayerID player : data.getPlayerList().getPlayers())
            {
                Territory capitol =  TerritoryAttachment.getCapital(player, data);
                if(capitol != null && !data.getAllianceTracker().isAllied(m_id, capitol.getOwner()))
                {
                    Route route = data.getMap().getRoute(t, capitol, moveThrough);
                    if(route != null)
                    {
                        int distance = route.getLength();
                        if(distance != 0 && distance < minDistance)
                        {
                            minDistance = distance;
                            to = capitol;
                        }
                    }
                }

            }
                
            if(to != null)
            {
                
                if(units.size() > 0)
                {
                    moveUnits.add(units);
                    Route routeToCapitol = data.getMap().getRoute(t, to, moveThrough);
                    Territory firstStep = routeToCapitol.getTerritories().get(1);
                    Route route = new Route();
                    route.setStart(t);
                    route.add(firstStep);
                    moveRoutes.add(route);
                }

            }
            //if we cant move to a capitol, move towards the enemy
            else
            {
                CompositeMatchAnd<Territory> routeCondition = new CompositeMatchAnd<Territory>(Matches.TerritoryIsLand, Matches.TerritoryIsImpassible.invert());
                Route newRoute = Utils.findNearest(t, Matches.territoryHasEnemyLandUnits(m_id, data ), routeCondition, data);
                if(newRoute != null)
                {
                    moveUnits.add(units);
                    Territory firstStep = newRoute.getTerritories().get(1);
                    Route route = new Route();
                    route.setStart(t);
                    route.add(firstStep);
                    moveRoutes.add(route);
                }
                
            }
        }
        
    
    }


    @SuppressWarnings("unchecked")
    private void movePlanesHomeNonCom(List<Collection<Unit>> moveUnits, List<Route> moveRoutes)
    {
        //the preferred way to get the delegate
        IMoveDelegate delegateRemote = (IMoveDelegate) m_bridge.getRemote();
        
        //this works because we are on the server
        final BattleDelegate delegate = DelegateFinder.battleDelegate(m_bridge.getGameData());
        
        Match<Territory> canLand = new CompositeMatchAnd<Territory>(
                
                Matches.isTerritoryAllied(m_id, m_bridge.getGameData()),
                new Match<Territory>()
                {
                    @Override
                    public boolean match(Territory o)
                    {
                        return !delegate.getBattleTracker().wasConquered(o);
                    }
                }
                );
        
        Match<Territory> routeCondition = new CompositeMatchAnd<Territory>(
                Matches.territoryHasEnemyAA(m_id, m_bridge.getGameData()).invert(),
                Matches.TerritoryIsImpassible.invert()
        );
        
        for(Territory t : delegateRemote.getTerritoriesWhereAirCantLand())
        {
            Route noAARoute = Utils.findNearest(t, canLand, routeCondition, m_bridge.getGameData());
            
            Route aaRoute = Utils.findNearest(t, canLand, Matches.TerritoryIsImpassible.invert() , m_bridge.getGameData());
            
            Collection<Unit> airToLand = t.getUnits().getMatches( new CompositeMatchAnd<Unit>(Matches.UnitIsAir, Matches.unitIsOwnedBy(m_id)));
            
            //dont bother to see if all the air units have enough movement points
            //to move without aa guns firing
            //simply move first over no aa, then with aa
            //one (but hopefully not both) will be rejected
            
            moveUnits.add(airToLand);
            moveRoutes.add(noAARoute);
            
            moveUnits.add(airToLand);
            moveRoutes.add(aaRoute);
        }
        
    }


    private void populateCombatMove(GameData data, List<Collection<Unit>> moveUnits, List<Route> moveRoutes)
    {
        populateBomberCombat(data, moveUnits, moveRoutes);
        
        final Collection<Unit> unitsAlreadyMoved = new HashSet<Unit>();
        //find the territories we can just walk into
        
        List<Territory> enemyOwned = Match.getMatches(data.getMap().getTerritories(), Matches.isTerritoryEnemyAndNotNeutral(m_id, data));

        
        Collections.sort(enemyOwned, new Comparator<Territory>()
        {        
            private Map<Territory, Integer> randomInts = new HashMap<Territory, Integer>();
            
            public int compare(Territory o1, Territory o2)
            {
                
                TerritoryAttachment ta1 = TerritoryAttachment.get(o1);
                TerritoryAttachment ta2 = TerritoryAttachment.get(o2);
                
                //take capitols first if we can
                if(ta1 != null && ta2 != null)
                {
                    if(ta1.isCapital() && !ta2.isCapital())
                        return 1;
                    if(!ta1.isCapital() && ta2.isCapital())
                        return -1;
                }
                
                boolean factoryInT1 = o1.getUnits().someMatch(Matches.UnitIsFactory);
                boolean factoryInT2 = o2.getUnits().someMatch(Matches.UnitIsFactory);
                
                //next take territories with factories
                if(factoryInT1 && !factoryInT2)
                    return 1;
                if(!factoryInT1 && factoryInT2)
                    return -1;

                //randomness is a better guide than any other metric
                //sort the remaining randomly
                if(!randomInts.containsKey(o1))
                    randomInts.put(o1,(int) Math.random() * 1000);
                if(!randomInts.containsKey(o2))
                    randomInts.put(o2,(int) Math.random() * 1000);

                return randomInts.get(o1) - randomInts.get(o2);
            }
        
        }  );
        
        
        //first find the territories we can just walk into
        for(Territory enemy : enemyOwned)
        {
            if(Utils.strength(enemy.getUnits().getUnits(), true, false) == 0)
            {
                //only take it with 1 unit
                boolean taken = false;
                for(Territory attackFrom : data.getMap().getNeighbors(enemy, Matches.territoryHasLandUnitsOwnedBy(m_id)))
                {
                    if(taken)
                        break;
                    for(Unit unit : attackFrom.getUnits().getUnits())
                    {
                        
                        Match<Unit> match = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(m_id), Matches.UnitIsLand, Matches.UnitIsNotFactory, Matches.UnitIsNotAA );
                        if(!unitsAlreadyMoved.contains(unit) && match.match(unit))
                        {
                            moveRoutes.add(data.getMap().getRoute(attackFrom, enemy));
                            
                            //if unloading units, unload all of them,
                            //otherwise we wont be able to unload them
                            //in non com, for land moves we want to move the minimal
                            //number of units, to leave units free to move elsewhere
                            if(attackFrom.isWater())
                            {
                                List<Unit> units = attackFrom.getUnits().getMatches(Matches.unitIsLandAndOwnedBy(m_id)); 
                                moveUnits.add( Util.difference(units, unitsAlreadyMoved) );
                                unitsAlreadyMoved.addAll(units);
                            }
                            else
                            {
                                moveUnits.add(Collections.singleton(unit));
                            }
                            
                            unitsAlreadyMoved.add(unit);
                            taken = true;
                            break;
                        }
                    }
                }
            }
        }
        
        
        
        
        //find the territories we can reasonably expect to take
        for(Territory enemy : enemyOwned)
        {
            
            float enemyStrength = Utils.strength(enemy.getUnits().getUnits(), false, false);
            if(enemyStrength > 0)
            {
                CompositeMatch<Unit> attackable = new CompositeMatchAnd<Unit>(
                        Matches.unitIsOwnedBy(m_id),
                        Matches.UnitIsStrategicBomber.invert(), 
                        new Match<Unit>()
                        {
                            public boolean match(Unit o)
                            {
                                return !unitsAlreadyMoved.contains(o);
                            }
                            
                        }
                        );
                attackable.add(Matches.UnitIsNotAA);
                attackable.add(Matches.UnitIsNotFactory);
                attackable.add(Matches.UnitIsNotSea);
                
                Set<Territory> dontMoveFrom = new HashSet<Territory>();
                
                //find our strength that we can attack with
                int ourStrength = 0;
                Collection<Territory> attackFrom = data.getMap().getNeighbors(enemy, Matches.territoryHasLandUnitsOwnedBy(m_id)); 
                for(Territory owned : attackFrom )
                {
                    
                   if( TerritoryAttachment.get(owned) != null &&
                       TerritoryAttachment.get(owned).isCapital() && 
                       (Utils.getStrengthOfPotentialAttackers(owned, m_bridge.getGameData()) >
                       Utils.strength(owned.getUnits().getUnits(), false, false))
                   )
                   {
                       dontMoveFrom.add(owned);
                       continue;
                   }
                    
                   ourStrength += Utils.strength(owned.getUnits().getMatches(attackable), true, false);
                }
                
                //prevents 2 infantry from attacking 1 infantry
                if(ourStrength > 1.37 * enemyStrength)
                {
                    
                    //this is all we need to take it, dont go overboard, since we may be able to use the units to attack somewhere else
                    double remainingStrengthNeeded = (2.5 * enemyStrength) + 4;
                    for(Territory owned : attackFrom )
                    {
                        if(dontMoveFrom.contains(owned))
                            continue;
                        
                        List<Unit> units = owned.getUnits().getMatches(attackable);
                        
                        //only take the units we need if 
                        //1) we are not an amphibious attack
                        //2) we can potentially attack another territory
                        if(!owned.isWater() && data.getMap().getNeighbors(owned, Matches.territoryHasEnemyLandUnits(m_id, data)).size() > 1  )
                            units = Utils.getUnitsUpToStrength(remainingStrengthNeeded, units, true, false);
                        
                        remainingStrengthNeeded -= Utils.strength(units, true, false);
                        
                        if(units.size() > 0)
                        {
                            unitsAlreadyMoved.addAll(units);
                            moveUnits.add(units);
                            moveRoutes.add(data.getMap().getRoute(owned, enemy));
                        }
                    }
                    
                    s_logger.fine("Attacking : " + enemy + " our strength:" + ourStrength + " enemy strength" + enemyStrength + " remaining strength needed " + remainingStrengthNeeded);
                }
                
            }
        }
        
    }

    private void populateBomberCombat(GameData data, List<Collection<Unit>> moveUnits, List<Route> moveRoutes)
    {
        Match<Territory> enemyFactory = Matches.territoryHasEnemyFactory(m_bridge.getGameData(), m_id);
        Match<Unit> ownBomber = new CompositeMatchAnd<Unit>(Matches.UnitIsStrategicBomber, Matches.unitIsOwnedBy(m_id));
        
        for(Territory t: data.getMap().getTerritories())
        {
            Collection<Unit> bombers = t.getUnits().getMatches(ownBomber);
            if(bombers.isEmpty())
                continue;
            Match<Territory> routeCond = new InverseMatch<Territory>(Matches.territoryHasEnemyAA(m_id, m_bridge.getGameData()));
            Route bombRoute = Utils.findNearest(t, enemyFactory, routeCond, m_bridge.getGameData());
           
            moveUnits.add(bombers);
            moveRoutes.add(bombRoute);
       
        }
        
    }


    private int countTransports(GameData data)
    {
        CompositeMatchAnd<Unit> ownedTransport = new CompositeMatchAnd<Unit>(Matches.UnitIsTransport, Matches.unitIsOwnedBy(m_id));
        int sum = 0;
        for(Territory t : data.getMap())
        {
            sum += t.getUnits().countMatches(ownedTransport );
        }
        return sum;
    }

    private void purchase(boolean bid)
    {
        if (bid)
        {          
            return;
        }
        
        pause();

        boolean isAmphib = isAmphibAttack(); 
        Route amphibRoute = getAmphibRoute();
        int transportCount =  countTransports(m_bridge.getGameData());
        
        m_id.getProductionFrontier();
        
        GameData data = m_bridge.getGameData();
        Resource ipcs = data.getResourceList().getResource(Constants.IPCS);
        

        int leftToSpend = m_id.getResources().getQuantity(ipcs );
        
        
        List<ProductionRule> rules = m_id.getProductionFrontier().getRules();
        IntegerMap<ProductionRule> purchase = new IntegerMap<ProductionRule>();

        
        int minCost = Integer.MAX_VALUE;
        while(minCost == Integer.MAX_VALUE ||  leftToSpend > minCost)
        {
            for(ProductionRule rule : rules)
            {
                int cost = rule.getCosts().getInt(ipcs);
                
                if(minCost == Integer.MAX_VALUE)
                {
                    minCost = cost;
                }
                if(minCost > cost)
                {
                    minCost = cost;
                }
                
                
                UnitType results = (UnitType) rule.getResults().keySet().iterator().next();
                if(Matches.UnitTypeIsAir.match(results) ||  Matches.UnitTypeIsAA.match(results) || Matches.UnitTypeIsFactory.match(results))
                {
                    continue;
                }
                
                int transportCapacity = UnitAttachment.get(results).getTransportCapacity();
                //buy transports if we can be amphibious
                if(Matches.UnitTypeIsSea.match(results))
                if(!isAmphib || transportCapacity <= 0)
                {
                    continue;
                }
                
                
                //give a preferene to cheap units, and to transports
                //but dont go overboard with buying transports
                int goodNumberOfTransports = 0;
                if(amphibRoute != null)
                {
                    goodNumberOfTransports = ((int) (amphibRoute.getTerritories().size() * 2.6)) + 1;
                }
                
                boolean isTransport = transportCapacity > 0;
                boolean buyBecauseTransport = (Math.random() < 0.7 && transportCount < goodNumberOfTransports) || Math.random() < 0.10 ;
                boolean dontBuyBecauseTooManyTransports = transportCount > 2 * goodNumberOfTransports;
                
                if(  (!isTransport && Math.random() * cost < 2)  ||
                     (isTransport && buyBecauseTransport && !dontBuyBecauseTooManyTransports)  )
                {
                    if(cost <= leftToSpend)
                    {
                        leftToSpend-= cost;
                        purchase.add(rule, 1);
                    }
                }
            }
        }
        
        IPurchaseDelegate delegate = (IPurchaseDelegate) m_bridge.getRemote();
        delegate.purchase(purchase);
        
        
       
    }

    private void battle()
    {
        while (true)
        {

            IBattleDelegate battleDel = (IBattleDelegate) m_bridge.getRemote();
            
            BattleListing listing = battleDel.getBattles();

            //all fought
            if(listing.getBattles().isEmpty() && listing.getStrategicRaids().isEmpty())
                return;
            
            Iterator<Territory> raidBattles = listing.getStrategicRaids().iterator();

            //fight strategic bombing raids
            while(raidBattles.hasNext())
            {
                
                pause();
                
                Territory current = raidBattles.next();
                String error = battleDel.fightBattle(current, true);
                if(error != null)
                    System.out.println(error);
            }
            
            
            Iterator<Territory> nonRaidBattles = listing.getBattles().iterator();

            //fight normal battles
            while(nonRaidBattles.hasNext())
            {
                pause();
                
                Territory current = nonRaidBattles.next();
                String error = battleDel.fightBattle(current, false);
                if(error != null)
                    System.out.println(error);
            }
        }
    }

    private void place(boolean bid)
    {
        
        

        if (m_id.getUnits().size() == 0)
            return;

       
        GameData data = m_bridge.getGameData();
        Territory capitol =  TerritoryAttachment.getCapital(m_id, data);
        
        //place in capitol first
        placeAllWeCanOn(data, capitol);
        
        List<Territory> randomTerritories = new ArrayList<Territory>(data.getMap().getTerritories());
        Collections.shuffle(randomTerritories);
        for(Territory t : randomTerritories)
        {
            if(t != capitol && t.getOwner().equals(m_id) && t.getUnits().someMatch(Matches.UnitIsFactory))
            {
                placeAllWeCanOn(data, t);
            }
        }

    }


    private void placeAllWeCanOn(GameData data, Territory placeAt)
    {
        IAbstractPlaceDelegate del = (IAbstractPlaceDelegate) m_bridge
        .getRemote();
        
        PlaceableUnits pu = del.getPlaceableUnits(m_id.getUnits().getUnits() , placeAt);
        if(pu.getErrorMessage() != null)
            return;
        
        int placementLeft =  pu.getMaxUnits();
        if(placementLeft == -1)
            placementLeft = Integer.MAX_VALUE;
        
        List<Unit> seaUnits = new ArrayList<Unit>(m_id.getUnits().getMatches(Matches.UnitIsSea));
        
        if(seaUnits.size() > 0)
        {
            Route amphibRoute = getAmphibRoute();
            Territory seaPlaceAt = null;
            
            if(amphibRoute != null)
            {
                seaPlaceAt = amphibRoute.getTerritories().get(1);
            }
            else
            {
                Set<Territory> seaNeighbors =  data.getMap().getNeighbors(placeAt, Matches.TerritoryIsWater);
                
                if(!seaNeighbors.isEmpty())
                    seaPlaceAt = seaNeighbors.iterator().next();
            }
            
            if(seaPlaceAt != null)
            {
                int seaPlacement = Math.min(placementLeft, seaUnits.size());
                placementLeft -= seaPlacement;
                Collection<Unit> toPlace = seaUnits.subList(0, seaPlacement);
                doPlace(seaPlaceAt, toPlace, del);
            }
        }
        
        
        List<Unit> landUnits = new ArrayList<Unit>(m_id.getUnits().getMatches(Matches.UnitIsLand));
        if(!landUnits.isEmpty())
        {   
            int landPlaceCount = Math.min(placementLeft, landUnits.size());
            placementLeft -= landPlaceCount;
            Collection<Unit> toPlace = landUnits.subList(0, landPlaceCount);
        
            
            doPlace(placeAt, toPlace, del);
        }
        
    }


    private void doPlace(Territory where, Collection<Unit> toPlace, IAbstractPlaceDelegate del)
    {
        String message = del.placeUnits(toPlace, where);
        if(message != null)
        {
            s_logger.fine(message);
            s_logger.fine("Attempt was at:" + where + " with:" + toPlace);
        }
        pause();
    }

    /*
     * @see games.strategy.engine.framework.IGameLoader#getRemotePlayerType()
     */
    public Class<ITripleaPlayer> getRemotePlayerType()
    {
        return ITripleaPlayer.class;
    }

    /*
     * 
     * 
     * @see games.strategy.triplea.player.ITripleaPlayer#selectCasualties(java.lang.String,
     *      java.util.Collection, java.util.Map, int, java.lang.String,
     *      games.strategy.triplea.delegate.DiceRoll,
     *      games.strategy.engine.data.PlayerID, java.util.List)
     */
    public CasualtyDetails selectCasualties(Collection<Unit> selectFrom, Map<Unit, Collection<Unit>> dependents, int count, String message, DiceRoll dice, PlayerID hit, List<Unit> defaultCasualties, GUID battleID)
    {
        List<Unit> rDamaged = new ArrayList<Unit>();
        List<Unit> rKilled = new ArrayList<Unit>();
        
        for(Unit unit : defaultCasualties)
        {
            boolean twoHit = UnitAttachment.get(unit.getType()).isTwoHit();
            //if it appears twice it then it both damaged and killed
            if(unit.getHits() == 0 && twoHit && !rDamaged.contains(unit))
                rDamaged.add(unit);
            else 
                rKilled.add(unit);
        }
        
        
        CasualtyDetails m2 = new CasualtyDetails(rKilled, rDamaged, false);
        return m2;

    }

    /*
     * (non-Javadoc)
     * 
     * @see games.strategy.triplea.player.ITripleaPlayer#reportError(java.lang.String)
     */
    public void reportError(String error)
    {
        
    }

    /* 
     * @see games.strategy.triplea.player.ITripleaPlayer#selectBombardingTerritory(games.strategy.engine.data.Unit, games.strategy.engine.data.Territory, java.util.Collection, boolean)
     */
    public Territory selectBombardingTerritory(Unit unit, Territory unitTerritory, Collection territories, boolean noneAvailable)
    {       
        //return the first one
        return (Territory) territories.iterator().next();
    }

    /* 
     * @see games.strategy.triplea.player.ITripleaPlayer#shouldBomberBomb(games.strategy.engine.data.Territory)
     */
    public boolean shouldBomberBomb(Territory territory)
    {
        return true;
            
    }

    /* 
     * @see games.strategy.triplea.player.ITripleaPlayer#whereShouldRocketsAttach(java.util.Collection, games.strategy.engine.data.Territory)
     */
    public Territory whereShouldRocketsAttack(Collection candidates, Territory from)
    {   
        //just use the first one
        return (Territory) candidates.iterator().next();
    }

    /* 
     * @see games.strategy.triplea.player.ITripleaPlayer#getNumberOfFightersToMoveToNewCarrier(java.util.Collection, games.strategy.engine.data.Territory)
     */
    public Collection<Unit> getNumberOfFightersToMoveToNewCarrier(Collection<Unit> fightersThatCanBeMoved, Territory from)
    {
        List<Unit> rVal = new ArrayList<Unit>();
        for(Unit fighter : fightersThatCanBeMoved)
        {
            if(Math.random() < 0.8)
                rVal.add(fighter);
        }
        return rVal;
    }

    /* 
     * @see games.strategy.triplea.player.ITripleaPlayer#selectTerritoryForAirToLand(java.util.Collection, java.lang.String)
     */
    public Territory selectTerritoryForAirToLand(Collection candidates)
    {
       return (Territory) candidates.iterator().next();
    }

    public void retreatNotificationMessage(Collection units)
    {
        // yeah, whatever
        
    }

    public boolean confirmMoveInFaceOfAA(Collection aaFiringTerritories)
    {
        return true;
    }

    public boolean confirmMoveKamikaze()
    {
        return false;
    }

    public Territory retreatQuery(GUID battleID, boolean submerge, Collection<Territory> possibleTerritories, String message)
    {
        return null;
    }

    public void reportMessage(String message)
    {
       //dont bother
        
    }

    public void battleInfoMessage(String shortMessage, DiceRoll dice)
    {
      
        
    }

    public void confirmOwnCasualties(GUID battleId, String message)
    {
        pause();
    }

    public void confirmEnemyCasualties(GUID battleId, String message, PlayerID hitPlayer)
    {
    }


}
