package games.strategy.triplea.weakAI;

import games.strategy.engine.data.*;
import games.strategy.triplea.attatchments.*;
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
        if(strength(units, attacking, sea) < maxStrength)
            return new ArrayList<Unit>(units);
        
        ArrayList<Unit> rVal = new ArrayList<Unit>();
        
        for(Unit u : units)
        {
            rVal.add(u);
            if(strength(rVal, attacking, sea) > maxStrength)
                return rVal;
        }
        
        return rVal;
        
        
    }
    
    /**
     * get a quick and dirty estimate of the strenght of the units 
     * @param sea TODO
     */
    public static float strength(Collection<Unit> units, boolean attacking, boolean sea)
    {
        int strength = 0;
        
        for(Unit u : units)
        {
            UnitAttachment unitAttatchment = UnitAttachment.get(u.getType());
            if(unitAttatchment.isAA() || unitAttatchment.isFactory())
            {
                //nothing
            }
            else if(unitAttatchment.isSea() == sea)
            {
                //2 points since we can absorb a hit
                strength +=  2;
                
                //two hit
                if(unitAttatchment.isTwoHit())
                    strength +=1.5;
                
                //the number of pips on the dice
                if(attacking)
                    strength += unitAttatchment.getAttack(u.getOwner());
                else
                    strength += unitAttatchment.getDefense(u.getOwner());    
                
                if(attacking)
                {
                    //a unit with attack of 0 isnt worth much
                    //we dont want transports to try and gang up on subs
                    if(unitAttatchment.getAttack(u.getOwner()) == 0)
                    {
                        strength -= 1.2;
                    }
                }
     
            }
        }
        
        if(attacking)
        {
            int art = Match.countMatches(units, Matches.UnitIsArtillery);
            int artSupport = Match.countMatches(units, Matches.UnitIsArtillerySupportable);
            strength += Math.min(art, artSupport);
        }
        
        return strength;
    }
    

    
    public static float getStrengthOfPotentialAttackers(Territory location, GameData data)
    {
        float strength = 0;
        for(Territory t : data.getMap().getNeighbors(location,  location.isWater() ? Matches.TerritoryIsWater :  Matches.TerritoryIsLand))
        {
            List<Unit> enemies = t.getUnits().getMatches(Matches.enemyUnit(location.getOwner(), data));
            strength+= strength(enemies, true, location.isWater());
            
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
            
            if(data.getAllianceTracker().isAllied(us, capitol.getOwner()))
                continue;
            
            
            if(capitol != null)
            {
                if(data.getMap().getDistance(t, capitol, Matches.TerritoryIsLand) != -1)
                {
                    return true;
                }
            }
        }
        return false;
        
    }
    
}

