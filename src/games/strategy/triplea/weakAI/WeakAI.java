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
 * TripleAPlayer.java
 *
 * A very weak ai
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
    
    private boolean m_pause =  Boolean.valueOf(System.getProperties().getProperty("triplea.randomai.pause", "true"));
    private void pause()
    {
        if(m_pause)
        {
            try
            {
                Thread.sleep(800);
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

    private void move(boolean nonCombat)
    {

        GameData data = m_bridge.getGameData();
        
        List<Collection<Unit>> moveUnits = new ArrayList<Collection<Unit>>();
        List<Route> moveRoutes = new ArrayList<Route>();
        
        if(!nonCombat)
        {
            populateCombatMove(data, moveUnits, moveRoutes);
        }
        else
        {
            populateNonCombat(nonCombat, data, moveUnits, moveRoutes);    
        }
        
        
        IMoveDelegate moveDel = (IMoveDelegate) m_bridge.getRemote();
        for(int i =0; i < moveRoutes.size(); i++)
        {
            pause();
            
            String result = moveDel.move(moveUnits.get(i), moveRoutes.get(i));
            if(result != null)
            {

                s_logger.info("could not move " + moveUnits.get(i) + " over " + moveRoutes.get(i) + " because : " + result);
            }
        }
        
        pause();
        
    }


    private void populateNonCombat(boolean nonCombat, GameData data, List<Collection<Unit>> moveUnits, List<Route> moveRoutes)
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
                float ourStrength = Utils.strength( m_id.getUnits().getUnits() , false);
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
            
            //find the nearest enemy capital
            for(PlayerID enemy : data.getPlayerList().getPlayers())
            {

                
                if(!data.getAllianceTracker().isAllied(enemy, m_id))
                {
                    Territory capitol =  TerritoryAttachment.getCapital(enemy, data);
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
        
        final List<Unit> unitsAlreadyMoved = new ArrayList<Unit>();
        //find the territories we can just walk into
        List<Territory> neighbors = Utils.getNeighboringEnemyLandTerritories(data, m_id);
        
        //first find the territories we can just walk into
        for(Territory enemy : neighbors)
        {
            if(Utils.strength(enemy.getUnits().getUnits(), true) == 0)
            {
                for(Territory owned : data.getMap().getNeighbors(enemy, Matches.isTerritoryOwnedBy(m_id)))
                {
                    for(Unit unit : owned.getUnits().getUnits())
                    {
                        
                        Match<Unit> match = new CompositeMatchAnd<Unit>(Matches.UnitIsLand, Matches.UnitIsNotFactory, Matches.UnitIsNotAA );
                        if(unit.getOwner().equals(m_id) && ! unitsAlreadyMoved.contains(unit) && match.match(unit))
                        {
                            moveRoutes.add(data.getMap().getRoute(owned, enemy));
                            moveUnits.add(Collections.singleton(unit));
                            unitsAlreadyMoved.add(unit);
                            break;
                        }
                    }
                }
            }
        }
        
        
        //find the territories we can reasonably expect to take
        for(Territory enemy : neighbors)
        {
            float enemyStrength = Utils.strength(enemy.getUnits().getUnits(), false);
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
                
                //find our strength that we can attack with
                int ourStrength = 0;
                Collection<Territory> attackFrom = data.getMap().getNeighbors(enemy, Matches.isTerritoryAllied(m_id, m_bridge.getGameData())); 
                for(Territory owned : attackFrom )
                {
                   ourStrength += Utils.strength(owned.getUnits().getMatches(attackable), true);
                }
                
                if(ourStrength > 1.32 * enemyStrength)
                {
                    s_logger.info("Attacking : " + enemy + " our strength:" + ourStrength + " enemy strength" + enemyStrength );
                    
                    for(Territory owned : attackFrom )
                    {
                        List<Unit> units = owned.getUnits().getMatches(attackable);
                        unitsAlreadyMoved.addAll(units);
                        moveUnits.add(units);
                        moveRoutes.add(data.getMap().getRoute(owned, enemy));
                    }
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



    private void purchase(boolean bid)
    {
        if (bid)
        {          
            return;
        }
        
        pause();

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
                if(Matches.UnitTypeIsAir.match(results) || Matches.UnitTypeIsSea.match(results) || Matches.UnitTypeIsAA.match(results) || Matches.UnitTypeIsFactory.match(results))
                {
                    continue;
                }
                
                if(Math.random() * cost < 2)
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
        
        pause();

        if (m_id.getUnits().size() == 0)
            return;

       
        GameData g = m_bridge.getGameData();

        Territory capital =  TerritoryAttachment.getCapital(m_id, g);

        Collection<Unit> toPlace;
        
        if(m_bridge.getGameData().getProperties().get(Constants.FOURTH_EDITION, false))
        {
               ArrayList<Unit> list = new ArrayList<Unit>(toPlace = m_id.getUnits().getUnits());
               
               int maxPlacement = Math.min(TerritoryAttachment.get(capital).getProduction() -1, list.size());
               
               
               toPlace = list.subList(0, maxPlacement);
        }
        else
        {
            toPlace = m_id.getUnits().getUnits();  
        }
        
        IAbstractPlaceDelegate del = (IAbstractPlaceDelegate) m_bridge
        .getRemote();
        del.placeUnits(toPlace, capital);


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
    public CasualtyDetails selectCasualties(Collection<Unit> selectFrom, Map<Unit, Collection<Unit>> dependents, int count, String message, DiceRoll dice, PlayerID hit, List<Unit> defaultCasualties)
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

    /* 
     * @see games.strategy.triplea.player.ITripleaPlayer#retreatNotificationMessage(java.util.Collection)
     */
    public void retreatNotificationMessage(Collection units)
    {
        // yeah, whatever
        
    }

    /* 
     * @see games.strategy.triplea.player.ITripleaPlayer#confirmMoveInFaceOfAA(java.util.Collection)
     */
    public boolean confirmMoveInFaceOfAA(Collection aaFiringTerritories)
    {
        return true;
    }

    /* 
     * @see games.strategy.triplea.player.ITripleaPlayer#confirm(java.util.Collection)
     */
    public boolean confirmMoveKamikaze()
    {
        return false;
    }

    /* 
     * @see games.strategy.triplea.player.ITripleaPlayer#retreatQuery(games.strategy.net.GUID, boolean, java.util.Collection, java.lang.String, java.lang.String)
     */
    public Territory retreatQuery(GUID battleID, boolean submerge, Collection<Territory> possibleTerritories, String message)
    {
        return null;
    }

    /* (non-Javadoc)
     * @see games.strategy.triplea.player.ITripleaPlayer#reportMessage(java.lang.String)
     */
    public void reportMessage(String message)
    {
       //dont bother
        
    }

    /*
     * @see games.strategy.triplea.player.ITripleaPlayer#battleInfoMessage(java.lang.String, games.strategy.triplea.delegate.DiceRoll, java.lang.String)
     */
    public void battleInfoMessage(String shortMessage, DiceRoll dice)
    {
      
        
    }

    /* (non-Javadoc)
     * @see games.strategy.triplea.player.ITripleaPlayer#confirmCasualties(games.strategy.net.GUID, java.lang.String, java.lang.String)
     */
    public void confirmOwnCasualties(GUID battleId, String message)
    {
    }

    /* (non-Javadoc)
     * @see games.strategy.triplea.player.ITripleaPlayer#confirmEnemyCasualties(games.strategy.net.GUID, java.lang.String, java.lang.String, games.strategy.engine.data.PlayerID)
     */
    public void confirmEnemyCasualties(GUID battleId, String message, PlayerID hitPlayer)
    {
    }

    /* (non-Javadoc)
     * @see games.strategy.triplea.player.ITripleaPlayer#confirmOwnCasualties(games.strategy.net.GUID, java.lang.String, java.lang.String, games.strategy.triplea.delegate.DiceRoll)
     */
    public void confirmOwnCasualties(GUID battleID, String message, String Step, DiceRoll dice)
    {
        
    }

}
