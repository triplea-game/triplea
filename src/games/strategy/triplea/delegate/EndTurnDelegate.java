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

import org.w3c.dom.Element;
import org.w3c.dom.Node;

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
import games.strategy.triplea.attatchments.TerritoryAttachment;
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
                        bridge.getHistoryWriter().startEvent("Axis achieve VP victory");
                        return;
                    } 
                } 

        		if (isNationalObjectives())
        		{
        			determineNationalObjectives(m_data);
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

        if ( germany && japan && count >=2)
        {
            m_gameOver = true;
            bridge.getHistoryWriter().startEvent("Axis achieve a military victory");
            //TODO We might want to find a more elegant way to end the game ala the TIC-TAC-TOE example
            //Added this to end the game on victory conditions
            bridge.stopGameSequence();
        }

        if ( russia && !germany && britain && !japan && america)
        {
            m_gameOver = true;
            bridge.getHistoryWriter().startEvent("Allies achieve a military victory");
            //Added this to end the game on victory conditions
            bridge.stopGameSequence();
        }

    }
    
    //Comco new method
    private void determineNationalObjectives(GameData data)
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
    		boolean satisfied = true;

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
	        				//Colon delimit the collection as it would exist in the XML
	        				for (Territory item : originalTerrs)
	      					  	value = value + ":" + item;
	        				//Remove the leading colon
	        				value = value.replaceFirst(":", "");
	    				}
	    				else if (name.equals("controlled"))
	        			{
	        				Collection<Territory> ownedTerrs = data.getMap().getTerritoriesOwnedBy(player);
	        				
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
    			}

    			//create the String list from the XML/gathered territories
    			terrs = value.split(":");
    			satisfied = checkUnitExclusions(satisfied, rule.getListedTerritories(terrs), "allied", player);    			
    		}

    		//
    		//Check for enemy unit exclusions
    		//TODO Transports and Subs don't count-- perhaps list types to exclude  
    		//   		
    		if(rule.getEnemyExclusionTerritories() != null && satisfied == true)
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
	        				Collection<Territory> originalTerrs = origOwnerTracker.getOriginallyOwned(data, player);
	        				//Colon delimit the collection as it would exist in the XML
	        				for (Territory item : originalTerrs)
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
    			}

    			//create the String list from the XML/gathered territories
    			terrs = value.split(":");
    			satisfied = checkUnitExclusions(satisfied, rule.getListedTerritories(terrs), "enemy", player);
    		}

    		//
    		//Check for Territory Ownership rules
    		//
    		if(rule.getAlliedOwnershipTerritoryCount() != -1 && satisfied == true)
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
	    				{	//TODO Spin through allied players and get all their originally owned terrs.
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
	    	        				//Colon delimit the collection as it would exist in the XML
	    	        				for (Territory item : originalAlliedTerrs)
	    	      					  	value = value + ":" + item;
	    	        				//Remove the leading colon
	    	        				value = value.replaceFirst(":", "");
	    						}
	    					}	    					
	    				}
	    				else if(name.equals("enemy"))
	    				{	//TODO Spin through enemy players and get all their originally owned terrs.
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
    			}

    			//create the String list from the XML/gathered territories
    			terrs = value.split(":");
    			
    			satisfied = checkAlliedOwnership(satisfied, rule.getListedTerritories(terrs), rule.getAlliedOwnershipTerritoryCount(), player);			
    		}
    		
    		//
    		//If all are satisfied add the IPCs for this objective
    		//
    		if (satisfied)
    		{
    			//Also log a message
    			player.getResources().addResource(data.getResourceList().getResource(Constants.IPCS), rule.getObjectiveValue());
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
		
		Iterator<Territory> listedTerrIter = listedTerrs.iterator();
		    			
		while(listedTerrIter.hasNext())
		{
			Territory listedTerr = listedTerrIter.next();
			//if the territory owner is an ally
			if (!m_data.getAllianceTracker().isAllied(listedTerr.getOwner(), player))
			{
				numberMet += 1;
				if(numberMet >= numberNeeded)
				{
					satisfied = true;
					break;
				}
			}    				
			satisfied = false;
		}
		return satisfied;
	}

    /**
     * Checks for the collection of territories to see if they have units onwned by the exclType alliance.  
     * It doesn't yet threshold the data
     */
	private boolean checkUnitExclusions(boolean satisfied, Collection<Territory> Territories, String exclType, PlayerID player) 
	{
		//TODO check the threshold number
		Iterator<Territory> ownedTerrIter = Territories.iterator();
		//Go through the owned territories and see if there are any units owned by allied/enemy based on exclType
		while (ownedTerrIter.hasNext())
		{
			//get all the units in the territory
			Territory terr = ownedTerrIter.next();
			Collection<Unit> allUnits =  terr.getUnits().getUnits();
			
			if (exclType == "allied")
			{	//any allied units in the territory
				Collection<Unit> playerUnits = Match.getMatches(allUnits, Matches.alliedUnit(player, m_data));
				if (playerUnits.size() > 0)
				{
					satisfied = false;
					break;
				}
			}
			else 
			{	//any enemy units in the territory
				Collection<Unit> playerUnits = Match.getMatches(allUnits, Matches.enemyUnit(player, m_data));
				if (playerUnits.size() > 0)
				{
					//TODO check the sub/trn rule (they don't count)
					satisfied = false;
					break;
				}
			}
		}		
		return satisfied;
	}
    
	private boolean isNationalObjectives()
    {
    	return games.strategy.triplea.Properties.getNationalObjectives(m_data);
    }

	private boolean isAnniversaryEditionLandProduction()
    {
    	return games.strategy.triplea.Properties.getAnniversaryEditionLandProduction(m_data);
    }
	

    private boolean isFourthEdition()
    {
    	return games.strategy.triplea.Properties.getFourthEdition(m_data);
    }
}
