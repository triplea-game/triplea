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

/*
 * PurchaseDelegate.java
 *
 * Created on November 2, 2001, 12:28 PM
 */

package games.strategy.triplea.delegate;

import games.strategy.engine.data.*;
import games.strategy.engine.delegate.*;
import games.strategy.triplea.delegate.remote.IPurchaseDelegate;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.util.IntegerMap;

import java.util.*;

/**
 *
 * Logic for purchasing units.
 *
 * Subclasses can override canAfford(...) to test if a purchase can be made
 *
 * Subclasses can over ride addToPlayer(...) and removeFromPlayer(...) to change how
 * the adding or removing of resources is done.
 *
 * @author  Sean Bridges
 * @version 1.0
 */
public class PurchaseDelegate implements IDelegate, IPurchaseDelegate
{
  private String m_name;
  private String m_displayName;
  private IDelegateBridge m_bridge;
  private PlayerID m_player;
  private GameData m_data;

  public void initialize(String name, String displayName)
  {
    m_name = name;
    m_displayName = displayName;
  }


  /**
   * Called before the delegate will run.
   */
  public void start(IDelegateBridge aBridge, GameData gameData)
  {
    m_bridge = aBridge;
    m_player = aBridge.getPlayerID();
    m_data = gameData;
  }

  public String getName()
  {
    return m_name;
  }

  public String getDisplayName()
  {
    return m_displayName;
  }

  protected GameData getData()
  {
    return m_data;
  }


  /**
   * subclasses can over ride this method to use different restrictions as to what a player can buy
   */
  protected boolean canAfford(IntegerMap costs, PlayerID player)
  {
    return player.getResources().has(costs);
  }

  /**
   * Returns an error code, or null if all is good.
   */
  public String purchase(IntegerMap productionRules)
  {
    IntegerMap costs = getCosts(productionRules);
    IntegerMap results = getResults(productionRules);

    if(!(canAfford(costs, m_player)))
      return "Not enough resources";

    addToPlayer(m_player, results);
    removeFromPlayer(m_player, costs);

    return null;
  }

  private IntegerMap getCosts(IntegerMap productionRules)
  {
    IntegerMap costs = new IntegerMap();

    Iterator rules = productionRules.keySet().iterator();
    while(rules.hasNext() )
    {
      ProductionRule rule = (ProductionRule) rules.next();
      costs.addMultiple(rule.getCosts(), productionRules.getInt(rule));
    }
    return costs;
  }

  private IntegerMap getResults(IntegerMap productionRules)
  {
    IntegerMap costs = new IntegerMap();

    Iterator rules = productionRules.keySet().iterator();
    while(rules.hasNext() )
    {
      ProductionRule rule = (ProductionRule) rules.next();
      costs.addMultiple(rule.getResults(), productionRules.getInt(rule));
    }
    return costs;
  }


  protected void addToPlayer(PlayerID player, IntegerMap resourcesAndUnits)
  {
    Iterator iter = resourcesAndUnits.keySet().iterator();
    Collection totalUnits = new ArrayList();
    while(iter.hasNext() )
    {
      Object next = iter.next();
      if(next instanceof Resource)
      {
        Resource resource = (Resource) next;
        int quantity = resourcesAndUnits.getInt(resource);
        Change change = ChangeFactory.changeResourcesChange(player, resource, quantity);
        m_bridge.addChange(change);
      } else
      {
        UnitType type = (UnitType) next;
        int quantity = resourcesAndUnits.getInt(type);
        Collection units = type.create(quantity, player);
        totalUnits.addAll(units);

      }
    }

    if(!totalUnits.isEmpty())
    {
      String transcriptText = player.getName() + " buys " + MyFormatter.unitsToTextNoOwner(totalUnits);
      m_bridge.getHistoryWriter().startEvent(transcriptText);
      m_bridge.getHistoryWriter().setRenderingData(totalUnits);
      Change change = ChangeFactory.addUnits(player, totalUnits);
      m_bridge.addChange(change);
    }
  }

  protected void removeFromPlayer(PlayerID player, IntegerMap resources)
  {
    Iterator iter = resources.keySet().iterator();
    while(iter.hasNext() )
    {
      Resource resource = (Resource) iter.next();
      int quantity = resources.getInt(resource);
      Change change = ChangeFactory.changeResourcesChange(player, resource, -quantity);
      m_bridge.addChange(change);
    }
  }




  /**
   * Called before the delegate will stop running.
   */
  public void end()
  {
    //this space intentionally left blank
  }


	/* 
	 * @see games.strategy.engine.delegate.IDelegate#getRemoteType()
	 */
	public Class getRemoteType()
	{
	    return IPurchaseDelegate.class;
	}
}

