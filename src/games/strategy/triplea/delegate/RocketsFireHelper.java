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

package games.strategy.triplea.delegate;

import games.strategy.engine.data.*;
import games.strategy.engine.delegate.*;
import games.strategy.triplea.attatchments.TerritoryAttatchment;
import games.strategy.triplea.delegate.message.*;
import games.strategy.triplea.*;
import java.util.*;
import games.strategy.util.*;
import games.strategy.engine.message.*;

/**
 * Logic to fire rockets.
 */
public class RocketsFireHelper
{

    

    public RocketsFireHelper()
    {

    }

    public void fireRockets(DelegateBridge bridge, GameData data, PlayerID player)
    {

        boolean is4thEdition = data.getProperties().get(Constants.FOURTH_EDITION, false);

        Set rocketTerritories = getTerritoriesWithRockets(data, player);
        if (rocketTerritories.isEmpty())
        {
            bridge.sendMessage(new StringMessage("No aa guns to fire rockets with"));
            return;
        }
        
        if(is4thEdition)
            fire4thEdition(data, player, rocketTerritories, bridge);
        else
            fire3rdEdition(data, player, rocketTerritories, bridge); 
       
    }
     
    private void fire4thEdition(GameData data, PlayerID player, Set rocketTerritories, DelegateBridge bridge)
    {
        Set attackedTerritories = new HashSet();
        Iterator iter = rocketTerritories.iterator();
        while(iter.hasNext())
        {
            Territory territory = (Territory) iter.next();
            Set targets = getTargetsWithinRange(territory, data, player);
            targets.removeAll(attackedTerritories);
            if(targets.isEmpty())
                continue;
            Territory target = getTarget(targets, player, bridge, territory);
            if(target != null)
            {
                attackedTerritories.add(target);
                fireRocket(player, target, bridge, data);
            }            
        }
    }
    
    private void fire3rdEdition(GameData data, PlayerID player, Set rocketTerritories, DelegateBridge bridge)
    {
        Set targets = new HashSet();
        Iterator iter = rocketTerritories.iterator();
        while(iter.hasNext())
        {
            Territory territory = (Territory) iter.next();
            targets.addAll(getTargetsWithinRange(territory, data, player));
        }
        
        if (targets.isEmpty())
        {
            bridge.sendMessage(new StringMessage("No targets to attack with rockets"));
            return;
        }

        Territory attacked = getTarget(targets, player, bridge, null);
        if (attacked != null)
            fireRocket(player, attacked, bridge, data);
    }

    private Set getTerritoriesWithRockets(GameData data, PlayerID player)
    {

        Set territories = new HashSet();

        CompositeMatch ownedAA = new CompositeMatchAnd();
        ownedAA.add(Matches.UnitIsAA);
        ownedAA.add(Matches.unitIsOwnedBy(player));

        Iterator iter = data.getMap().iterator();
        while (iter.hasNext())
        {
            Territory current = (Territory) iter.next();
            if (current.isWater())
                continue;

            if (current.getUnits().someMatch(ownedAA))
                territories.add(current);
        }
        return territories;
    }

    private Set getTargetsWithinRange(Territory territory, GameData data, PlayerID player)
    {

        Collection possible = data.getMap().getNeighbors(territory, 3);

        CompositeMatch enemyFactory = new CompositeMatchAnd();
        enemyFactory.add(Matches.UnitIsFactory);
        enemyFactory.add(Matches.enemyUnit(player, data));

        Set hasFactory = new HashSet();

        Iterator iter = possible.iterator();
        while (iter.hasNext())
        {
            Territory current = (Territory) iter.next();
            if (current.getUnits().someMatch(enemyFactory))
                hasFactory.add(current);
        }
        return hasFactory;
    }

    private Territory getTarget(Collection targets, PlayerID player, DelegateBridge bridge, Territory from)
    {
        if(targets.size() == 1)
        {
            return (Territory) targets.iterator().next();
        }

        Message response = bridge.sendMessage(new RocketAttackQuery(targets, from), player);
        if (!(response instanceof TerritoryMessage))
            throw new IllegalStateException("Message of wrong type:" + response);
        TerritoryMessage territoryMessage = (TerritoryMessage) response;
        if (territoryMessage.getTerritory() == null)
            return null;
        return territoryMessage.getTerritory();
    }

    private void fireRocket(PlayerID player, Territory attackedTerritory, DelegateBridge bridge, GameData data)
    {

        PlayerID attacked = attackedTerritory.getOwner();
        Resource ipcs = data.getResourceList().getResource(Constants.IPCS);
        //int cost = bridge.getRandom(Constants.MAX_DICE);

        int cost = bridge.getRandom(Constants.MAX_DICE, "Rocket fired by " + player.getName() + " at " + attacked.getName());

        //account for 0 base
        cost++;

        //in fourth edtion, limit rocket attack cost to
        //production value of factory.
        if (data.getProperties().get(Constants.FOURTH_EDITION, false))
        {
            int territoryProduction = TerritoryAttatchment.get(attackedTerritory).getProduction();
            if (cost > territoryProduction)
            {
                cost = territoryProduction;
            }
        }

        // Trying to remove more IPCs than the victim has is A Bad Thing[tm]
        int availForRemoval = attacked.getResources().getQuantity(ipcs);
        if (cost > availForRemoval)
            cost = availForRemoval;

        bridge.sendMessage(new StringMessage("Rocket attack in " + attackedTerritory.getName() + " costs:" + cost));

        String transcriptText = attacked.getName() + " lost " + cost + " ipcs to rocket attack by " + player.getName();
        bridge.getHistoryWriter().startEvent(transcriptText);

        Change rocketCharge = ChangeFactory.changeResourcesChange(attacked, ipcs, -cost);
        bridge.addChange(rocketCharge);

    }

}