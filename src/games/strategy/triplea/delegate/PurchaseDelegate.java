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

import java.util.*;

import games.strategy.util.*;
import games.strategy.engine.data.*;
import games.strategy.engine.delegate.*;
import games.strategy.engine.message.*;

import games.strategy.triplea.delegate.message.*;
import games.strategy.triplea.formatter.Formatter;

/**
 * 
 * Logic for purchasing units.
 *
 * @author  Sean Bridges
 * @version 1.0
 */
public class PurchaseDelegate implements Delegate
{
	private String m_name;
	private DelegateBridge m_bridge;
	private PlayerID m_player;
	
	public void initialize(String name) 
	{
		m_name = name;
	}
	
	/**
	 * Called before the delegate will run.
	 */
	public void start(DelegateBridge aBridge, GameData gameData) 
	{
		m_bridge = aBridge;
		m_player = aBridge.getPlayerID();
	}
	
	public String getName() 
	{
		return m_name;
	}
	
	/**
	 * A message from the given player.
	 */
	public Message sendMessage(Message aMessage) 
	{
		if(aMessage instanceof BuyMessage)
		{	
			return buy( (BuyMessage) aMessage, m_player); 
		} else
		{
			throw new IllegalArgumentException("Purchase delegate received message of wrong type:" + aMessage);
		}
	}
	
	/** 
	 * Returns an error code, or null if all is good.
	 */
	private Message buy(BuyMessage buy, PlayerID player)
	{
		IntegerMap costs = getCosts(buy);
		IntegerMap results = getResults(buy);
		if(!(player.getResources().has(costs)))
			return new StringMessage("Not enough resources", true);
		
		addToPlayer(player, results);
		removeFromPlayer(player, costs);
		
		
		
		return new StringMessage("done");
	}
	
	private IntegerMap getCosts(BuyMessage buy)
	{
		IntegerMap costs = new IntegerMap();
		
		Iterator rules = buy.getPurchase().keySet().iterator();
		while(rules.hasNext() )
		{
			ProductionRule rule = (ProductionRule) rules.next();
			costs.addMultiple(rule.getCosts(), buy.getPurchase().getInt(rule));
		}
		return costs;
	}

	private IntegerMap getResults(BuyMessage buy)
	{
		IntegerMap costs = new IntegerMap();
		
		Iterator rules = buy.getPurchase().keySet().iterator();
		while(rules.hasNext() )
		{
			ProductionRule rule = (ProductionRule) rules.next();
			costs.addMultiple(rule.getResults(), buy.getPurchase().getInt(rule));
		}
		return costs;
	}
	
	
	private void addToPlayer(PlayerID player, IntegerMap resourcesAndUnits)
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
			Change change = ChangeFactory.addUnits(player, totalUnits);
			m_bridge.addChange(change);
			
			String transcriptText = player.getName() + " buys " + Formatter.unitsToTextNoOwner(totalUnits);
			m_bridge.getTranscript().write(transcriptText);
		}
	}
	
	private void removeFromPlayer(PlayerID player, IntegerMap resources)
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
}
