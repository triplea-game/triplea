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
 * EndTurnDelegate.java
 *
 * Created on November 2, 2001, 12:30 PM
 */

package games.strategy.triplea.delegate;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JOptionPane;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.ChangeFactory;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.IAttachment;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.AutoSave;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attatchments.PlayerAttachment;
import games.strategy.triplea.attatchments.RulesAttachment;
import games.strategy.triplea.attatchments.TechAttachment;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.util.CompositeMatch;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.Match;

/**
 *
 * @author  Sean Bridges
 * @version 1.0
 *
 * At the end of the turn collect income.
 */
@AutoSave(afterStepEnd=true)
public class EndTurnDelegate extends AbstractEndTurnDelegate
{

    protected void checkForWinner(IDelegateBridge bridge)
    {
        //only notify once
        if(m_gameOver)
            return;
        String victoryMessage = null;
        PlayerID russians = m_data.getPlayerList().getPlayerID(Constants.RUSSIANS);
        PlayerID germans = m_data.getPlayerList().getPlayerID(Constants.GERMANS);
        PlayerID british = m_data.getPlayerList().getPlayerID(Constants.BRITISH);
        PlayerID japanese = m_data.getPlayerList().getPlayerID(Constants.JAPANESE);
        PlayerID americans = m_data.getPlayerList().getPlayerID(Constants.AMERICANS);


                if(m_data.getProperties().get(Constants.PACIFIC_EDITION, false))
                {
                    PlayerAttachment pa = PlayerAttachment.get(japanese);

                    
                    if(pa != null && Integer.parseInt(pa.getVps()) >= 22)
                    {
                        m_gameOver = true;
                        victoryMessage = "Axis achieve VP victory";
                        bridge.getHistoryWriter().startEvent(victoryMessage);
                        signalGameOver(victoryMessage,bridge);
                        return;
                    } 
                } 

        		if (isNationalObjectives())
        		{
        			determineNationalObjectives(m_data, bridge);
        		}
        		
        if(isFourthEdition())
            return;

        
        if(germans == null || russians == null || british == null || japanese == null || americans == null)
            return;
        
        // Quick check to see who still owns their own capital
        boolean russia = TerritoryAttachment.getCapital(russians, m_data).getOwner().equals(russians);
        boolean germany = TerritoryAttachment.getCapital(germans, m_data).getOwner().equals(germans);
        boolean britain = TerritoryAttachment.getCapital(british, m_data).getOwner().equals(british);
        boolean japan = TerritoryAttachment.getCapital(japanese, m_data).getOwner().equals(japanese);
        boolean america = TerritoryAttachment.getCapital(americans, m_data).getOwner().equals(americans);


        int count = 0;
        if (!russia) count++;
        if (!britain) count++;
        if (!america) count++;
        victoryMessage = " achieve a military victory";

        if ( germany && japan && count >=2)
        {
            m_gameOver = true;
            bridge.getHistoryWriter().startEvent("Axis" + victoryMessage);
            signalGameOver(victoryMessage,bridge);
            //TODO We might want to find a more elegant way to end the game ala the TIC-TAC-TOE example
            //Added this to end the game on victory conditions
            //bridge.stopGameSequence();
        }

        if ( russia && !germany && britain && !japan && america)
        {
            m_gameOver = true;
            bridge.getHistoryWriter().startEvent("Allies" + victoryMessage);
            signalGameOver(victoryMessage,bridge);
            //Added this to end the game on victory conditions
            //bridge.stopGameSequence();
        }
    }

    /**
     * Notify all players that the game is over.
     * 
     * @param status the "game over" text to be displayed to each user.
     */
    private void signalGameOver(String status, IDelegateBridge a_bridge)
    {
        // If the game is over, we need to be able to alert all UIs to that fact.
        //    The display object can send a message to all UIs.
        if (!m_gameOver)
        {
            m_gameOver = true;
            // Make sure the user really wants to leave the game.
            int rVal = JOptionPane.showConfirmDialog(null, status +"\nDo you want to continue?", "Continue" , JOptionPane.YES_NO_OPTION);
            if(rVal != JOptionPane.OK_OPTION)
                a_bridge.stopGameSequence();
        }
    }
    
/**
 * Determine if National Objectives have been met
 * @param data
 */
    private void determineNationalObjectives(GameData data, IDelegateBridge bridge)
    {
    	PlayerID player = data.getSequence().getStep().getPlayerID();
    	
    	//See if the player has National Objectives
    	Set<RulesAttachment> natObjs = new HashSet<RulesAttachment>();
        Map<String, IAttachment> map = player.getAttachments();
        Iterator<String> objsIter = map.keySet().iterator();
        while(objsIter.hasNext() )
        {
            IAttachment attachment = map.get(objsIter.next());
            String name = attachment.getName();
            if (name.startsWith(Constants.RULES_OBJECTIVE_PREFIX))
            {
            	natObjs.add((RulesAttachment)attachment);
            }
        }
        
        //Check whether any National Objectives are met
    	Iterator<RulesAttachment> rulesIter = natObjs.iterator();
    	while(rulesIter.hasNext())
    	{
    		RulesAttachment rule = rulesIter.next();
    		boolean objectiveMet = true;

    		//
    		//Check for allied unit exclusions
    		//
    		if(rule.getAlliedExclusionTerritories() != null)
    		{
    			//Get the listed territories
    			String[] terrs = rule.getAlliedExclusionTerritories();
    			String value = new String();    			
    			
	    		//If there's only 1, it might be a 'group' (original, controlled, all)
    			if(terrs.length == 1)
    			{    				
	    			for(String name : terrs)
	    			{
	    				if(name.equals("original"))
	    				{	//get all originally owned territories
	    					OriginalOwnerTracker origOwnerTracker = DelegateFinder.battleDelegate(data).getOriginalOwnerTracker();
	        				Collection<Territory> originalTerrs = origOwnerTracker.getOriginallyOwned(data, player);
	        				rule.setTerritoryCount(String.valueOf(originalTerrs.size()));
	        				//Colon delimit the collection as it would exist in the XML
	        				for (Territory item : originalTerrs)
	      					  	value = value + ":" + item;
	        				//Remove the leading colon
	        				value = value.replaceFirst(":", "");
	    				}
	    				else if (name.equals("controlled"))
	        			{
	        				Collection<Territory> ownedTerrs = data.getMap().getTerritoriesOwnedBy(player);
	        				rule.setTerritoryCount(String.valueOf(ownedTerrs.size()));
	        				//Colon delimit the collection as it would exist in the XML	        				
	        				  for (Territory item : ownedTerrs)
	        					  value = value + ":" + item;
	        				  //Remove the leading colon
	        				  value = value.replaceFirst(":", "");
	        			}
	    				else if (name.equals("all"))
	        			{
	        				Collection<Territory> allTerrs = data.getMap().getTerritoriesOwnedBy(player);
	        				OriginalOwnerTracker origOwnerTracker = DelegateFinder.battleDelegate(data).getOriginalOwnerTracker();
	        				allTerrs.addAll(origOwnerTracker.getOriginallyOwned(data, player));
	        				rule.setTerritoryCount(String.valueOf(allTerrs.size()));
	        				//Colon delimit the collection as it would exist in the XML	        				
	        				for (Territory item : allTerrs)
	      					  value = value + ":" + item;
	        				//Remove the leading colon
	        				value = value.replaceFirst(":", "");
	        			}
	    				else
	    				{	//The list just contained 1 territory
	    					value = name;
	    				}
	    			}
    			}
    			else
    			{
    				//Get the list of territories	
    				Collection<Territory> listedTerrs = rule.getListedTerritories(terrs);
    				//Colon delimit the collection as it exists in the XML
    				for (Territory item : listedTerrs)
  					  value = value + ":" + item;
    				//Remove the leading colon
    				value = value.replaceFirst(":", "");
    			}

    			//create the String list from the XML/gathered territories
    			terrs = value.split(":");
    			objectiveMet = checkUnitExclusions(objectiveMet, rule.getListedTerritories(terrs), "allied", rule.getTerritoryCount(), player);    			
    		}

    		//
    		//Check for enemy unit exclusions
    		//TODO Transports and Subs don't count-- perhaps list types to exclude  
    		//   		
    		if(rule.getEnemyExclusionTerritories() != null && objectiveMet == true)
    		{
    			//Get the listed territories
    			String[] terrs = rule.getEnemyExclusionTerritories();
    			String value = new String();    			
    			
	    		//If there's only 1, it might be a 'group'  (original)
    			if(terrs.length == 1)
    			{    				
	    			for(String name : terrs)
	    			{
	    				if(name.equals("original"))
	    				{	//get all originally owned territories
	    					OriginalOwnerTracker origOwnerTracker = DelegateFinder.battleDelegate(data).getOriginalOwnerTracker();
	        				Collection<Territory> enemyTerrs = origOwnerTracker.getOriginallyOwned(data, player);
	        				rule.setTerritoryCount(String.valueOf(enemyTerrs.size()));
	        				//Colon delimit the collection as it would exist in the XML
	        				for (Territory item : enemyTerrs)
	      					  	value = value + ":" + item;
	        				//Remove the leading colon
	        				value = value.replaceFirst(":", "");
	    				}
	    				else
	    				{	//The list just contained 1 territory
	    					value = name;
	    				}
	    			}
    			}
    			else
    			{
    				//Get the list of territories	
    				Collection<Territory> listedTerrs = rule.getListedTerritories(terrs);
    				//Colon delimit the collection as it exists in the XML
    				for (Territory item : listedTerrs)
  					  value = value + ":" + item;
    				//Remove the leading colon
    				value = value.replaceFirst(":", "");
    			}

    			//create the String list from the XML/gathered territories
    			terrs = value.split(":");
    			objectiveMet = checkUnitExclusions(objectiveMet, rule.getListedTerritories(terrs), "enemy", rule.getTerritoryCount(), player);
    		}

    		//
    		//Check for Territory Ownership rules
    		//
    		if(rule.getAlliedOwnershipTerritories() != null && objectiveMet == true)
    		{
    			//Get the listed territories
    			String[] terrs = rule.getAlliedOwnershipTerritories();
    			String value = new String();    			
    			
	    		//If there's only 1, it might be a 'group' (original)
    			if(terrs.length == 1)
    			{    				
	    			for(String name : terrs)
	    			{
	    				if(name.equals("original"))
	    				{	
	    					Collection<PlayerID> players = data.getPlayerList().getPlayers();
	    					Iterator<PlayerID> playersIter = players.iterator();
	    					while(playersIter.hasNext())
	    					{
	    						PlayerID currPlayer = playersIter.next();
	    						if (data.getAllianceTracker().isAllied(currPlayer, player))
	    						{
	    							//get all originally owned territories
	    	    					OriginalOwnerTracker origOwnerTracker = DelegateFinder.battleDelegate(data).getOriginalOwnerTracker();
	    	        				Collection<Territory> originalAlliedTerrs = origOwnerTracker.getOriginallyOwned(data, currPlayer);
	    	        				rule.setTerritoryCount(String.valueOf(originalAlliedTerrs.size()));
	    	        				//Colon delimit the collection as it would exist in the XML
	    	        				for (Territory item : originalAlliedTerrs)
	    	      					  	value = value + ":" + item;
	    	        				//Remove the leading colon
	    	        				value = value.replaceFirst(":", "");
	    						}
	    					}	    					
	    				}
	    				else if(name.equals("enemy"))
	    				{	//TODO Perhaps add a count to signify how many territories must be controlled- currently, it's ALL
	    					Collection<PlayerID> players = data.getPlayerList().getPlayers();
	    					Iterator<PlayerID> playersIter = players.iterator();
	    					while(playersIter.hasNext())
	    					{
	    						PlayerID currPlayer = playersIter.next();
	    						if (!data.getAllianceTracker().isAllied(currPlayer, player))
	    						{
	    							//get all originally owned territories
	    	    					OriginalOwnerTracker origOwnerTracker = DelegateFinder.battleDelegate(data).getOriginalOwnerTracker();
	    	        				Collection<Territory> originalEnemyTerrs = origOwnerTracker.getOriginallyOwned(data, currPlayer);
	    	        				rule.setTerritoryCount(String.valueOf(originalEnemyTerrs.size()));
	    	        				//Colon delimit the collection as it would exist in the XML
	    	        				for (Territory item : originalEnemyTerrs)
	    	      					  	value = value + ":" + item;
	    	        				//Remove the leading colon
	    	        				value = value.replaceFirst(":", "");
	    						}
	    					}	    					
	    				}
	    				else
	    				{	//The list just contained 1 territory
	    					value = name;
	    				}
	    			}	    			
    			}
    			else
    			{
    				//Get the list of territories	
    				Collection<Territory> listedTerrs = rule.getListedTerritories(terrs);
    				//Colon delimit the collection as it exists in the XML
    				for (Territory item : listedTerrs)
  					  value = value + ":" + item;
    				//Remove the leading colon
    				value = value.replaceFirst(":", "");
    			}

    			//create the String list from the XML/gathered territories
    			terrs = value.split(":");
    			
    			objectiveMet = checkAlliedOwnership(objectiveMet, rule.getListedTerritories(terrs), rule.getTerritoryCount(), player);			
    		}
    		
    		//
    		//If all are satisfied add the IPCs for this objective
    		//
    		if (objectiveMet)
    		{
    		    int total = player.getResources().getQuantity(Constants.IPCS) + rule.getObjectiveValue();
    		    
    		    Change change = ChangeFactory.changeResourcesChange(player, data.getResourceList().getResource(Constants.IPCS), rule.getObjectiveValue());
    			//player.getResources().addResource(data.getResourceList().getResource(Constants.IPCS), rule.getObjectiveValue());
                bridge.addChange(change);
    			
    			String ipcMessage = player.getName() + " met a national objective for an additional " + rule.getObjectiveValue() + MyFormatter.pluralize(" ipc", rule.getObjectiveValue()) +
    			"; end with " + total + MyFormatter.pluralize(" ipc", total);
    			bridge.getHistoryWriter().startEvent(ipcMessage);
    		}    		
    	} //end while        	
    } //end determineNationalObjectives

    
    /**
     * Checks for allied ownership of the collection of territories.  Once the needed number threshold is reached, the satisfied flag is set
     * to true and returned
     */
	private boolean checkAlliedOwnership(boolean satisfied, Collection<Territory> listedTerrs, int numberNeeded, PlayerID player) 
	{		
		int numberMet = 0;
		satisfied = false;
		
		Iterator<Territory> listedTerrIter = listedTerrs.iterator();
		    			
		while(listedTerrIter.hasNext())
		{
			Territory listedTerr = listedTerrIter.next();
			//if the territory owner is an ally
			if (m_data.getAllianceTracker().isAllied(listedTerr.getOwner(), player))
			{
				numberMet += 1;
				if(numberMet >= numberNeeded)
				{
					satisfied = true;
					break;
				}
			}
		}
		return satisfied;
	}

    /**
     * Checks for the collection of territories to see if they have units onwned by the exclType alliance.  
     * It doesn't yet threshold the data
     */
	private boolean checkUnitExclusions(boolean satisfied, Collection<Territory> Territories, String exclType, int numberNeeded, PlayerID player) 
	{
		int numberMet = 0;
		satisfied = false;
		
		Iterator<Territory> ownedTerrIter = Territories.iterator();
		//Go through the owned territories and see if there are any units owned by allied/enemy based on exclType
		while (ownedTerrIter.hasNext())
		{
			//get all the units in the territory
			Territory terr = ownedTerrIter.next();
			Collection<Unit> allUnits =  terr.getUnits().getUnits();
			
			if (exclType == "allied")
			{	//any allied units in the territory
				allUnits.removeAll(Match.getMatches(allUnits, Matches.unitIsOwnedBy(player)));
				Collection<Unit> playerUnits = Match.getMatches(allUnits, Matches.alliedUnit(player, m_data));
				if (playerUnits.size() < 1)
				{
					numberMet += 1;
					if(numberMet >= numberNeeded)
					{
						satisfied = true;
						break;
					}
				}
			}
			else 
			{	//any enemy units in the territory
				Collection<Unit> enemyUnits = Match.getMatches(allUnits, Matches.enemyUnit(player, m_data));
				if (enemyUnits.size() < 1)
				{
					//TODO check the sub/trn rule (they don't count)
					numberMet += 1;
					if(numberMet >= numberNeeded)
					{
						satisfied = true;
						break;
					}
				}
			}
		}		
		return satisfied;
	}
    
	private boolean isNationalObjectives()
    {
    	return games.strategy.triplea.Properties.getNationalObjectives(m_data);
    }

    private boolean isFourthEdition()
    {
    	return games.strategy.triplea.Properties.getFourthEdition(m_data);
    }
}
