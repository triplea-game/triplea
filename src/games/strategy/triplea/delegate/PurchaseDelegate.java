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
import games.strategy.engine.message.IRemote;
import games.strategy.triplea.delegate.remote.IPurchaseDelegate;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.triplea.Constants;
import games.strategy.util.IntegerMap;

import java.io.Serializable;
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
  protected boolean canAfford(IntegerMap<Resource> costs, PlayerID player)
  {
    return player.getResources().has(costs);
  }

  /**
   * Returns an error code, or null if all is good.
   */
  public String purchase(IntegerMap<ProductionRule> productionRules)
  {
    IntegerMap<Resource> costs = getCosts(productionRules);
    IntegerMap<NamedAttachable> results = getResults(productionRules);

    if(!(canAfford(costs, m_player)))
      return "Not enough resources";

    // remove first, since add logs ipcs remaining
    
    Iterator<NamedAttachable> iter = results.keySet().iterator();
    Collection<Unit> totalUnits = new ArrayList<Unit>();
    CompositeChange changes = new CompositeChange();
    Resource ipcs = getData().getResourceList().getResource(Constants.IPCS);
    int ipcsRemaining = m_player.getResources().getQuantity(ipcs);

    // add changes for added resources
    //  and find all added units
    while(iter.hasNext() )
    {
      Object next = iter.next();
      if(next instanceof Resource)
      {
        Resource resource = (Resource) next;
        int quantity = results.getInt(resource);
        Change change = ChangeFactory.changeResourcesChange(m_player, resource, quantity);
        changes.add(change);
      } else
      {
        UnitType type = (UnitType) next;
        int quantity = results.getInt(type);
        Collection<Unit> units = type.create(quantity, m_player);
        totalUnits.addAll(units);

      }
    }

    // add changes for added units
    if(!totalUnits.isEmpty())
    {
      Change change = ChangeFactory.addUnits(m_player, totalUnits);
      changes.add(change);
    }

    // add changes for spent resources
    Iterator<Resource> costsIter = costs.keySet().iterator();
    while(costsIter.hasNext() )
    {
      Resource resource = costsIter.next();
      int quantity = costs.getInt(resource);
      ipcsRemaining -= quantity;
      Change change = ChangeFactory.changeResourcesChange(m_player, resource, -quantity);
      changes.add(change);
    }

    addHistoryEvent(totalUnits, ipcsRemaining);

    
    // commit changes
    m_bridge.addChange(changes);
      

    return null;
  }


private void addHistoryEvent(Collection<Unit> totalUnits, int ipcsRemaining)
{
    // add history event
    String transcriptText;
    if(!totalUnits.isEmpty())
      transcriptText = m_player.getName() + " buy " + MyFormatter.unitsToTextNoOwner(totalUnits)+"; "+ipcsRemaining+" ipcs remaining";
    else
      transcriptText = m_player.getName() + " buy nothing; "+ipcsRemaining+" ipcs remaining";
    m_bridge.getHistoryWriter().startEvent(transcriptText);
    m_bridge.getHistoryWriter().setRenderingData(totalUnits);
}

  private IntegerMap<Resource> getCosts(IntegerMap<ProductionRule> productionRules)
  {
    IntegerMap<Resource> costs = new IntegerMap<Resource>();

    Iterator<ProductionRule> rules = productionRules.keySet().iterator();
    while(rules.hasNext() )
    {
      ProductionRule rule = rules.next();
      costs.addMultiple(rule.getCosts(), productionRules.getInt(rule));
    }
    return costs;
  }

  private IntegerMap<NamedAttachable> getResults(IntegerMap<ProductionRule> productionRules)
  {
    IntegerMap<NamedAttachable> costs = new IntegerMap<NamedAttachable>();

    Iterator<ProductionRule> rules = productionRules.keySet().iterator();
    while(rules.hasNext() )
    {
      ProductionRule rule = rules.next();
      costs.addMultiple(rule.getResults(), productionRules.getInt(rule));
    }
    return costs;
  }



  /**
   * Returns the state of the Delegate.
   */
  public Serializable saveState()
  {
      return null;
  }
  
  /**
   * Loads the delegates state
   */
  public void loadState(Serializable state)
  {}


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
    public Class<? extends IRemote> getRemoteType()
    {
        return IPurchaseDelegate.class;
    }

}

