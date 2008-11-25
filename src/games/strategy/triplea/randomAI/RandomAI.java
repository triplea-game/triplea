package games.strategy.triplea.randomAI;

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

/*
 *
 * More useful for testing and for an example of how an ai could be written rather than as an actual ai.
 * 
 * run with -Dtriplea.randomai.pause=false to eliminate the human friendly pauses 
 *
 * @author Sean Bridges
 */
public class RandomAI implements IGamePlayer, ITripleaPlayer
{
    private final String m_name;
    private PlayerID m_id;
    private IPlayerBridge m_bridge;

    /** Creates new TripleAPlayer */
    public RandomAI(String name)
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
            {}//intentionally blank
        

    }

    private void tech()
    {}

    private void move(boolean nonCombat)
    {
        
        

        GameData data = m_bridge.getGameData();
        Collection<Territory> territories = data.getMap().getTerritories();
        
        
        List<Collection<Unit>> moveUnits = new ArrayList<Collection<Unit>>();
        List<Route> moveRouets = new ArrayList<Route>();
        
        for(Territory t : territories)
        {
            //keep it interesting
            if(Math.random() < 0.3)
                continue;
            
            //these are the territories we can move to
            CompositeMatchAnd<Territory> moveToIf = new CompositeMatchAnd<Territory>();
            //these are the units we can move
            CompositeMatch<Unit> moveOfType = new CompositeMatchAnd<Unit>();
            
            moveOfType.add(Matches.unitIsOwnedBy(m_id));
            
            //aa guns cant move in non combat
            if(!nonCombat)
                moveOfType.add(Matches.UnitIsNotAA);
            
            //we can never move factories
            moveOfType.add(Matches.UnitIsNotFactory);
            
            //keep to land/water
            if(t.isWater())
            {
                moveToIf.add(Matches.TerritoryIsWater);
                moveOfType.add(Matches.UnitIsSea);
            }
            else
            {
                moveToIf.add(Matches.TerritoryIsLand);
                moveOfType.add(Matches.UnitIsLand);
                
                if(nonCombat)
                {
                    //non combat only move to friendly
                    moveToIf.add(Matches.isTerritoryFriendly(m_id, data));
                }
                
                // can enter non-neutral (or free neutral) spaces
                CompositeMatchOr<Territory> walkInto = new CompositeMatchOr<Territory>();
                walkInto.add(Matches.isTerritoryAllied(m_id, data));
                walkInto.add(Matches.isTerritoryEnemyAndNotNeutral(m_id, data));
                walkInto.add(Matches.isTerritoryFreeNeutral(data));
                                 
                // stay out of neutrals
                //moveToIf.add(new InverseMatch<Territory>(Matches.TerritoryIsNuetral));
                moveToIf.add(walkInto);
                
            }
            
            //attack or not depending on type
            if(!nonCombat)
            {
                Matches.territoryHasEnemyUnits(m_id, data);
            }
            else
            {
                Matches.territoryHasNoEnemyUnits(m_id, data);
            }
                
            Collection<Territory> neighbors = data.getMap().getNeighbors(t, moveToIf);
            
            if(neighbors.isEmpty())
            {
                continue;
            }
            
            Territory to = new ArrayList<Territory>(neighbors).get((int) (Math.random() * neighbors.size()));
            Collection<Unit> units = t.getUnits().getMatches(moveOfType);
            
            if(units.size() > 0)
            {
                moveUnits.add(units);
                Route route = new Route();
                route.setStart(t);
                route.add(to);
                moveRouets.add(route);
            }
            
        }
        
        
        IMoveDelegate moveDel = (IMoveDelegate) m_bridge.getRemote();
        for(int i =0; i < moveRouets.size(); i++)
        {
            pause();
            
            String result = moveDel.move(moveUnits.get(i), moveRouets.get(i));
            if(result != null)
            {
                System.out.println(result);
            }
        }
        
        pause();
        
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
                if(Matches.UnitTypeIsSea.match(results) || Matches.UnitTypeIsAA.match(results) || Matches.UnitTypeIsFactory.match(results))
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

       
        GameData data = m_bridge.getGameData();

        Territory capital =  TerritoryAttachment.getCapital(m_id, data);

        Collection<Unit> toPlace;
        
        if(games.strategy.triplea.Properties.getFourthEdition(data) || games.strategy.triplea.Properties.getPlacementRestrictedByFactory(data))
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
        del.placeUnits(new ArrayList<Unit>(toPlace), capital);


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
        //just a warning to our user
        System.out.println(error);
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
        return Math.random() < 0.2;
            
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
     * @see games.strategy.triplea.player.ITripleaPlayer#confirm(java.util.Collection)
     */
    public boolean confirmMoveHariKari()
    {
        return false;
    }

    /* 
     * @see games.strategy.triplea.player.ITripleaPlayer#retreatQuery(games.strategy.net.GUID, boolean, java.util.Collection, java.lang.String, java.lang.String)
     */
    public Territory retreatQuery(GUID battleID, boolean submerge, Collection<Territory> possibleTerritories, String message)
    {
        if(Math.random() < 0.1)
            return possibleTerritories.iterator().next();
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

    /* (non-Javadoc)
     * @see games.strategy.triplea.player.ITripleaPlayer#selectFixedDice(int, java.lang.String)
     */
    public int[] selectFixedDice(int numRolls, int hitAt, boolean hitOnlyIfEquals, String message)
    {
        int[] dice = new int[numRolls];
        for (int i=0; i<numRolls; i++)
        {
            dice[i] = (int)Math.ceil(Math.random() * 6);
        }
        return dice;
    }
}
