/*
 * TechnolgoyDelegate.java 
 *
 *
 * Created on November 25, 2001, 4:16 PM
 */

package games.strategy.triplea.delegate;

import java.util.*;

import games.strategy.util.*;
import games.strategy.engine.data.*;
import games.strategy.engine.message.Message;
import games.strategy.engine.delegate.*;

import games.strategy.triplea.Constants;
import games.strategy.triplea.delegate.message.*;

/**
 * Logic for dealing with player tech rolls.
 *
 * @author  Sean Bridges
 * @version 1.0
 */
public class TechnologyDelegate implements Delegate
{
	
	private String m_name;
	private GameData m_data;
	private DelegateBridge m_bridge;
	private TechTracker m_techTracker = new TechTracker();
	private PlayerID m_player;

	/** Creates new TechnolgoyDelegate */
    public TechnologyDelegate() 
	{
    }

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
		m_data = gameData;
		m_player = aBridge.getPlayerID();
	}
	
	private Message rollTech(IntegerMessage msg)
	{
		int techRolls = msg.getMessage();
		boolean canPay = checkEnoughMoney(techRolls);
		if(!canPay)
			return new StringMessage("Not enough money to pay for that many tech rolls", true);
		
		chargeForTechRolls(techRolls);
		int techHits = getTechHits(techRolls);
		m_bridge.sendMessage( new StringMessage("You got " + techHits + " hits"));
		if(techHits == 0)
			return null;
		
		Collection advances = getTechAdvances(techHits);
		
		Iterator iter = advances.iterator();
		StringBuffer text = new StringBuffer();
		while(iter.hasNext())
		{
			TechAdvance advance = (TechAdvance) iter.next();
			advance.perform(m_bridge.getPlayerID(),m_bridge, m_data );
			text.append(advance.getName());
			if(iter.hasNext())
				text.append(" and ");
			m_techTracker.addAdvance(m_bridge.getPlayerID(), m_data, m_bridge, advance);
		}
		
		String transcriptText =  m_bridge.getPlayerID().getName() + " discovers " + text.toString();
		m_bridge.getTranscript().write(transcriptText);
		
		return new StringMessage("Youre scientists have discovered:" + text);
		
		
	}
	
	boolean checkEnoughMoney(int rolls)
	{
		Resource ipcs = m_data.getResourceList().getResource(Constants.IPCS);
		int cost = rolls * Constants.TECH_ROLL_COST;
		int has = m_bridge.getPlayerID().getResources().getQuantity(ipcs);
		return has >= cost;
	}
	
	private void chargeForTechRolls(int rolls)
	{
		Resource ipcs = m_data.getResourceList().getResource(Constants.IPCS);
		int cost = rolls * Constants.TECH_ROLL_COST;
		Change charge = ChangeFactory.changeResourcesChange(m_bridge.getPlayerID(), ipcs, -cost);
		m_bridge.addChange(charge);
		
		String transcriptText = m_bridge.getPlayerID().getName() + " spends " + cost + " on tech rolls";
		m_bridge.getTranscript().write(transcriptText);
	}
	
	private int getTechHits(int rolls)
	{
		int[] random = m_bridge.getRandom(Constants.MAX_DICE, rolls);
		int count = 0;
		for(int i = 0; i < rolls; i++)
		{
			if(random[i] == Constants.MAX_DICE - 1)
				count++;
		}
		return count;
	}
	
	private Collection getTechAdvances(int hits)
	{
		//too many
		Collection allAdvances = TechAdvance.getTechAdvances();
		Collection playersAdvances = m_techTracker.getAdvances(m_bridge.getPlayerID());
		
		List available = Util.difference(allAdvances, playersAdvances);
		if(available.isEmpty())
			return Collections.EMPTY_LIST;
		if(hits >= available.size())
			return available;
		
		Collection newAdvances = new ArrayList(hits);
		while(hits > 0)
		{
			int random = m_bridge.getRandom(available.size());
			
			newAdvances.add(available.get(random));
			available.remove(random);
			hits--;
		}
		return newAdvances;
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
		if((aMessage instanceof IntegerMessage))
			return rollTech((IntegerMessage) aMessage);
		else 
			throw new IllegalStateException("Message of wrong type:" + aMessage);
		
	}
	
	public TechTracker getTechTracker()
	{
		return m_techTracker;
	}
	
	/**
	 * Called before the delegate will stop running.
	 */
	public void end() 
	{
	}	
}