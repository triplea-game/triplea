package games.strategy.triplea.weakAI;

import games.strategy.engine.data.*;
import games.strategy.triplea.attatchments.*;
import games.strategy.triplea.baseAI.AIUtils;
import games.strategy.triplea.delegate.Matches;
import games.strategy.util.*;

import java.util.*;

public class Utils
{

    /**
     * All the territories that border one of our territories 
     */
    public static List<Territory> getNeighboringEnemyLandTerritories(GameData data, PlayerID player)
    {
        ArrayList<Territory> rVal = new ArrayList<Territory>();
        for(Territory t : data.getMap())
        {
            if(Matches.isTerritoryEnemy(player, data).match(t) && !t.getOwner().isNull() )
            {
            
                if(!data.getMap().getNeighbors(t, Matches.isTerritoryOwnedBy(player)).isEmpty())
                {
                    rVal.add(t);
                }
            }
        }
        return rVal;
    }
    
    public static List<Unit> getUnitsUpToStrength(double maxStrength, Collection<Unit> units, boolean attacking, boolean sea)
    {
        if(AIUtils.strength(units, attacking, sea) < maxStrength)
            return new ArrayList<Unit>(units);
        
        ArrayList<Unit> rVal = new ArrayList<Unit>();
        
        for(Unit u : units)
        {
            rVal.add(u);
            if(AIUtils.strength(rVal, attacking, sea) > maxStrength)
                return rVal;
        }
        
        return rVal;
        
        
    }
    

    

    
    public static float getStrengthOfPotentialAttackers(Territory location, GameData data)
    {
        float strength = 0;
        for(Territory t : data.getMap().getNeighbors(location,  location.isWater() ? Matches.TerritoryIsWater :  Matches.TerritoryIsLand))
        {
            List<Unit> enemies = t.getUnits().getMatches(Matches.enemyUnit(location.getOwner(), data));
            strength+= AIUtils.strength(enemies, true, location.isWater());
            
        }
        return strength;
    }
    
    
    public static Route findNearest(Territory start, Match<Territory> endCondition, Match<Territory> routeCondition, GameData data)
    {
        Route shortestRoute = null;
        
        
        
        for(Territory t : data.getMap().getTerritories())
        {
            if(endCondition.match(t))
            {
                CompositeMatchOr<Territory> routeOrEnd = new CompositeMatchOr<Territory>(routeCondition, Matches.territoryIs(t));
                
                Route r = data.getMap().getRoute(start, t, routeOrEnd);
                if(r != null)
                {
                    if(shortestRoute == null || r.getLength() < shortestRoute.getLength())
                       shortestRoute = r;
                }
            }
        }
        
        return shortestRoute;
        
        
    }
    
    
    public  static boolean hasLandRouteToEnemyOwnedCapitol(Territory t, PlayerID us, GameData data)
    {
        for(PlayerID player : data.getPlayerList())
        {
            Territory capitol = TerritoryAttachment.getCapital(player, data);
            //optional players will return null- set them to false
            if (capitol == null)
            	return false;
                        	
            if(data.getAllianceTracker().isAllied(us, capitol.getOwner()))
                continue;
            
            if(data.getMap().getDistance(t, capitol, Matches.TerritoryIsLand) != -1)
            {
                return true;
            }
        
        }
        return false;
        
    }
}

