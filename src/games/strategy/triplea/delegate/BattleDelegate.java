/*
 * BattleDelegate.java
 *
 * Created on November 2, 2001, 12:26 PM
 */

package games.strategy.triplea.delegate;

import java.util.*;

import games.strategy.util.*;

import games.strategy.engine.data.*;
import games.strategy.engine.delegate.*;
import games.strategy.engine.message.*;

import games.strategy.triplea.Constants;
import games.strategy.triplea.attatchments.*;

import games.strategy.triplea.delegate.message.*;

/**
 *
 * @author  Sean Bridges
 * @version 1.0
 */
public class BattleDelegate implements Delegate
{

	private String m_name;
	private GameData m_data;
	private DelegateBridge m_bridge;
	private BattleTracker m_battleTracker = new BattleTracker();
	private OriginalOwnerTracker m_originalOwnerTracker = new OriginalOwnerTracker();
	
	public void initialize(String name) 
	{
		m_name = name;
	}
	
	/**
	 * Called before the delegate will run.
	 */
	public void start(DelegateBridge aBridge, GameData gameData) 
	{
		m_data = gameData;
		m_bridge = aBridge;
		if(DelegateFinder.techDelegate(gameData).getTechTracker().hasRocket(aBridge.getPlayerID()))
			fireRockets(aBridge, gameData, aBridge.getPlayerID());
	}
	
	public String getName() 
	{
		return m_name;
	}
	
	private void fireRockets(DelegateBridge bridge, GameData data, PlayerID player)
	{
		Collection targets = getTargetsWithinRange(data, player);
		if(targets.isEmpty())
		{
			bridge.sendMessage(new StringMessage("No targets to attack with rockets"));
			return;
		}
		
		PlayerID attacked = getTarget(targets, player, bridge);
		fireRocket(player, attacked, bridge, data);
	}
	
	private Collection getTargetsWithinRange(GameData data, PlayerID player)
	{
		Set targets = new HashSet();
		
		CompositeMatch ownedAA = new CompositeMatchAnd();
		ownedAA.add(Matches.UnitIsAA);
		ownedAA.add(Matches.unitIsOwnedBy(player));
		
		Iterator iter = data.getMap().iterator();
		while(iter.hasNext())
		{
			Territory current = (Territory) iter.next();
			if(!current.getOwner().equals(player))
				continue;
			if(current.getUnits().someMatch(ownedAA))
				targets.addAll(getTargetsWithinRange(current, data, player));
		}
		return targets;	
	}
	
	private Collection getTargetsWithinRange(Territory territory, GameData data, PlayerID player)
	{
		Collection possible = data.getMap().getNeighbors(territory, 3);
		
		CompositeMatch enemyFactory = new CompositeMatchAnd();
		enemyFactory.add(Matches.UnitIsFactory);
		enemyFactory.add(Matches.enemyUnit(player, data));
		
		Collection hasFactory = new ArrayList();
		
		Iterator iter = possible.iterator();
		while(iter.hasNext())
		{
			Territory current = (Territory) iter.next();
			if(current.getUnits().someMatch(enemyFactory))
				hasFactory.add(current);
		}
		return hasFactory;
	}
	
	private PlayerID getTarget(Collection targets, PlayerID player, DelegateBridge bridge)
	{
		Message response = bridge.sendMessage(new RocketAttackQuery(targets), player);
		if(!(response instanceof TerritoryMessage))
			throw new IllegalStateException("Message of wrong type:" + response);
		return ((TerritoryMessage) response).getTerritory().getOwner(); 
	}
	
	private void fireRocket(PlayerID player, PlayerID attacked,  DelegateBridge bridge, GameData data)
	{
		int cost = bridge.getRandom(Constants.MAX_DICE);
		//account for 0 base
		cost++;
		bridge.sendMessage(new StringMessage("Rocket attack costs:" + cost));
		Resource ipcs = data.getResourceList().getResource(Constants.IPCS);
		Change rocketCharge = ChangeFactory.changeResourcesChange(attacked, ipcs, -cost);
		bridge.addChange(rocketCharge);
		
		String transcriptText = attacked.getName() + " lost " + cost + " ipcs to rocket attack by " + player.getName() ;
		bridge.getTranscript().write(transcriptText);
	}
	
	/**
	 * A message from the given player.
	 */
	public Message sendMessage(Message message) 
	{
		if(message instanceof GetBattles)
			return getBattles();
		if(message instanceof FightBattleMessage)
		{
			FightBattleMessage fightMessage = (FightBattleMessage) message;
			
			Territory territory = fightMessage.getTerritory();
			boolean bombing = fightMessage.getStrategicBombingRaid();
			Battle battle = m_battleTracker.getPendingBattle(territory, bombing);
			
			//does the battle exist
			if(battle == null)
				return new StringMessage("No battle in given territory", true);
			
			//are there battles that must occur first
			Collection allMustPrecede = m_battleTracker.getDependentOn(battle); 
			if(!allMustPrecede.isEmpty())
			{
				Battle firstPrecede = (Battle) allMustPrecede.iterator().next();
				String name = firstPrecede.getTerritory().getName();
				String fightingWord = firstPrecede.isBombingRun() ? "Bombing Run" : "Battle";
				return new StringMessage("Must complete " +  fightingWord + " in " + name + " first", true);
			}
			
			//fight the battle
			battle.fight(m_bridge);
			
			//and were done
			return new StringMessage("Battle fought");
		}
		else
			throw new IllegalArgumentException("Battle delegate received message of wrong type:" + message);
	}
	
	private BattleListingMessage getBattles()
	{
		Collection battles = m_battleTracker.getPendingBattleSites(false);
		Collection bombing = m_battleTracker.getPendingBattleSites(true);
		return new BattleListingMessage(battles, bombing);
	}
	
	/**
	 * Called before the delegate will stop running.
	 */
	public void end() 
	{}
	
	public BattleTracker getBattleTracker()
	{
		return m_battleTracker;
	}
	
	public OriginalOwnerTracker getOriginalOwnerTracker()
	{
		return m_originalOwnerTracker;
	}
}

