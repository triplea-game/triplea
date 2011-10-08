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
 * EndRoundDelegate.java
 *
 * Created on January 18, 2002, 9:50 PM
 */

package games.strategy.triplea.delegate;

import games.strategy.common.delegate.BaseDelegate;
import games.strategy.engine.data.*;
import games.strategy.engine.delegate.*;
import games.strategy.engine.message.IRemote;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attatchments.PlayerAttachment;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.attatchments.TriggerAttachment;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.EventThreadJOptionPane;
import games.strategy.util.Match;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import javax.swing.JOptionPane;


/**
 *
 *  A delegate used to check for end of game conditions.
 *
 * @author  Sean Bridges
 */
public class EndRoundDelegate extends BaseDelegate
{

	private boolean m_gameOver = false;

	/** Creates a new instance of EndRoundDelegate */
    public EndRoundDelegate()
	{
    }



	/**
	 * Called before the delegate will run.
	 */
	public void start(IDelegateBridge aBridge, GameData gameData)
	{
		super.start(aBridge, gameData);
		
		if(m_gameOver)
			return;

		String victoryMessage = null;
		
        if(isPacificTheater())
        {
            PlayerID japanese = m_data.getPlayerList().getPlayerID(Constants.JAPANESE);
            PlayerAttachment pa = PlayerAttachment.get(japanese);
            if(pa != null && Integer.parseInt(pa.getVps()) >= 22)
            {
                victoryMessage = "Axis achieve VP victory";
                aBridge.getHistoryWriter().startEvent(victoryMessage);
                signalGameOver(victoryMessage,aBridge);
            }
        }

		//Check for Winning conditions        
        if(isTotalVictory()) //Check for Win by Victory Cities
		{
        	victoryMessage = " achieve TOTAL VICTORY with ";
		    checkVictoryCities(aBridge, m_data, victoryMessage, " Total Victory VCs");            
		} 

        if(isHonorableSurrender())
		{
			victoryMessage = " achieve an HONORABLE VICTORY with ";
		    checkVictoryCities(aBridge, m_data, victoryMessage, " Honorable Victory VCs");    
		}

        if (isProjectionOfPower())
		{
			victoryMessage = " achieve victory through a PROJECTION OF POWER with ";
		    checkVictoryCities(aBridge, m_data, victoryMessage, " Projection of Power VCs");           
		}

        if (isEconomicVictory()) //Check for regular economic victory
		{
			Iterator<String> allianceIter = m_data.getAllianceTracker().getAlliances().iterator();
			String allianceName = null;
			while (allianceIter.hasNext())
			{
				allianceName = (String) allianceIter.next();
				
				int victoryAmount = getEconomicVictoryAmount(m_data, allianceName);
				
				Set<PlayerID> teamMembers = m_data.getAllianceTracker().getPlayersInAlliance(allianceName);
				
				Iterator<PlayerID> teamIter = teamMembers.iterator();
				int teamProd = 0;
				while(teamIter.hasNext())
				{				
				    PlayerID player = teamIter.next();
				     teamProd += getProduction(player);

				    if(teamProd >= victoryAmount)
				    {
				    	victoryMessage = allianceName + " achieve economic victory";
				    	aBridge.getHistoryWriter().startEvent(victoryMessage);
				        //Added this to end the game on victory conditions
				        signalGameOver(victoryMessage,aBridge);
				    }
				}
			}
		}
        
        // now check for generic trigger based victories
        if (isTriggeredVictory())
        {
        	// it is end of round, so loop through all players
        	Collection<PlayerID> playerList = m_data.getPlayerList().getPlayers();
        	for (PlayerID p : playerList)
        	{
            	String vMessage = TriggerAttachment.triggerVictory(p, aBridge, m_data, null, null);
            	if (vMessage != null)
            	{
                	//vMessage = NotificationMessages.getInstance(m_ui.getUIContext()).getMessage(victoryMessage);
            		vMessage = "<html>" + vMessage + "</html>";
            		signalGameOver(vMessage,aBridge);
            	}
        	}
        }
        
        if(isWW2V2() || isWW2V3())
            return;
        
        // now test older maps that only use these 5 players, to see if someone has won
        PlayerID russians = m_data.getPlayerList().getPlayerID(Constants.RUSSIANS);
        PlayerID germans = m_data.getPlayerList().getPlayerID(Constants.GERMANS);
        PlayerID british = m_data.getPlayerList().getPlayerID(Constants.BRITISH);
        PlayerID japanese = m_data.getPlayerList().getPlayerID(Constants.JAPANESE);
        PlayerID americans = m_data.getPlayerList().getPlayerID(Constants.AMERICANS);
        
        if(germans == null || russians == null || british == null || japanese == null || americans == null || m_data.getPlayerList().size() > 5)
            return;
        
        // Quick check to see who still owns their own capital
        boolean russia = TerritoryAttachment.getCapital(russians, m_data).getOwner().equals(russians);
        boolean germany = TerritoryAttachment.getCapital(germans, m_data).getOwner().equals(germans);
        boolean britain = TerritoryAttachment.getCapital(british, m_data).getOwner().equals(british);
        boolean japan = TerritoryAttachment.getCapital(japanese, m_data).getOwner().equals(japanese);
        boolean america = TerritoryAttachment.getCapital(americans, m_data).getOwner().equals(americans);
        
        int count = 0;
        if (!russia)
        	count++;
        if (!britain)
        	count++;
        if (!america)
        	count++;
        
        victoryMessage = " achieve a military victory";
        
        if (germany && japan && count >=2)
        {
        	aBridge.getHistoryWriter().startEvent("Axis" + victoryMessage);
            signalGameOver(victoryMessage,aBridge);
        }

        if (russia && !germany && britain && !japan && america)
        {
            aBridge.getHistoryWriter().startEvent("Allies" + victoryMessage);
            signalGameOver(victoryMessage,aBridge);
        }
	}


	private void checkVictoryCities(IDelegateBridge aBridge, GameData m_data, String victoryMessage, String victoryType) 
	{
		Iterator<String> allianceIter = m_data.getAllianceTracker().getAlliances().iterator();
		String allianceName = null;
		while (allianceIter.hasNext())
		{
			allianceName = (String) allianceIter.next();
			
			int vcAmount = getVCAmount(m_data, allianceName, victoryType);
			
			Set<PlayerID> teamMembers = m_data.getAllianceTracker().getPlayersInAlliance(allianceName);
			
			Iterator<PlayerID> teamIter = teamMembers.iterator();
			int teamVCs = Match.countMatches(m_data.getMap().getTerritories(), 
			        new CompositeMatchAnd<Territory>(Matches.TerritoryIsVictoryCity,Matches.isTerritoryAllied(teamIter.next(), m_data)));

			if(teamVCs >= vcAmount)
			{				
				aBridge.getHistoryWriter().startEvent(allianceName + victoryMessage + vcAmount + " Victory Cities!");
				//Added this to end the game on victory conditions
				signalGameOver(allianceName + victoryMessage + vcAmount + " Victory Cities!",aBridge);
			}
		}
	}

	private int getEconomicVictoryAmount(GameData data, String alliance)
	{
		try {
			return Integer.parseInt((String) data.getProperties().get(alliance + " Economic Victory"));
		} catch (NumberFormatException e) {
			return 1000;
		}
	}
	
	private int getVCAmount(GameData data, String alliance, String type)
	{
		try {
			return Integer.parseInt((String) data.getProperties().get(alliance + type));
		} catch (NumberFormatException e) {
			if(type == " Total Victory VCs")
			{
				return 18;
			}
			else if(type == " Honorable Victory VCs")
			{
				return 15;
			}
			else if(type == " Projection of Power VCs")
			{
				return 13;
			}
				
			return 1000;
		}
	}
    
    /**
     * Notify all players that the game is over.
     * 
     * @param status the "game over" text to be displayed to each user.
     */
    public void signalGameOver(String status, IDelegateBridge a_bridge)
    {
        // If the game is over, we need to be able to alert all UIs to that fact.
        //    The display object can send a message to all UIs.
        if (!m_gameOver)
        {
            m_gameOver = true;
            // Make sure the user really wants to leave the game.
            int rVal =  EventThreadJOptionPane.showConfirmDialog(null, status +"\nDo you want to continue?", "Continue" , JOptionPane.YES_NO_OPTION);
            if(rVal != JOptionPane.OK_OPTION)
                a_bridge.stopGameSequence();
        }
    }
    
    private boolean isWW2V2()
    {
        return games.strategy.triplea.Properties.getWW2V2(m_data);
    } 
    
    private boolean isWW2V3()
    {
        return games.strategy.triplea.Properties.getWW2V3(m_data);
    } 
    
    private boolean isPacificTheater()
    {
        return games.strategy.triplea.Properties.getPacificTheater(m_data);
    } 
    
    private boolean isTotalVictory()
    {
        return games.strategy.triplea.Properties.getTotalVictory(m_data);
    }   
    
    private boolean isHonorableSurrender()
    {
        return games.strategy.triplea.Properties.getHonorableSurrender(m_data);
    }   
    
    private boolean isProjectionOfPower()
    {
        return games.strategy.triplea.Properties.getProjectionOfPower(m_data);
    }   
	
    private boolean isEconomicVictory()
    {
        return games.strategy.triplea.Properties.getEconomicVictory(m_data);
    }   
	
    private boolean isTriggeredVictory()
    {
        return games.strategy.triplea.Properties.getTriggeredVictory(m_data);
    }  
	
	public int getProduction(PlayerID id)
	{
		int sum = 0;

		Iterator<Territory> territories = m_data.getMap().iterator();
		while(territories.hasNext())
		{
			Territory current = (Territory) territories.next();
			if(current.getOwner().equals(id))
			{
				TerritoryAttachment ta = TerritoryAttachment.get(current);
				sum += ta.getProduction();
			}
		}
                
		return sum;
	}


	/**
	 * Returns the state of the Delegate.
	 */
	public Serializable saveState()
	{
		return Boolean.valueOf(m_gameOver);
	}

	/**
	 * Loads the delegates state
	 */
	public void loadState(Serializable state)
	{
		m_gameOver = ((Boolean) state).booleanValue();
	}

	
    /* 
     * @see games.strategy.engine.delegate.IDelegate#getRemoteType()
     */
    public Class<? extends IRemote> getRemoteType()
    {
        return null;
    }

}
