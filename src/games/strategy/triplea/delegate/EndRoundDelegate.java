/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

/*
 * EndRoundDelegate.java
 * 
 * Created on January 18, 2002, 9:50 PM
 */

package games.strategy.triplea.delegate;

import games.strategy.common.delegate.BaseDelegate;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.PlayerList;
import games.strategy.engine.data.Territory;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.message.IRemote;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attatchments.PlayerAttachment;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.attatchments.TriggerAttachment;
import games.strategy.triplea.ui.NotificationMessages;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.EventThreadJOptionPane;
import games.strategy.util.Match;
import games.strategy.util.Tuple;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import javax.swing.JOptionPane;

/**
 * 
 * A delegate used to check for end of game conditions.
 * 
 * @author Sean Bridges
 */
public class EndRoundDelegate extends BaseDelegate
{
	
	private boolean m_gameOver = false;
	private Collection<PlayerID> m_winners = new ArrayList<PlayerID>();
	
	/** Creates a new instance of EndRoundDelegate */
	public EndRoundDelegate()
	{
	}
	
	/**
	 * Called before the delegate will run.
	 */
	@Override
	public void start(IDelegateBridge aBridge)
	{
		super.start(aBridge);
		
		if (m_gameOver)
			return;
		
		String victoryMessage = null;
		
		GameData data = getData();
		if (isPacificTheater())
		{
			PlayerID japanese = data.getPlayerList().getPlayerID(Constants.JAPANESE);
			PlayerAttachment pa = PlayerAttachment.get(japanese);
			if (pa != null && Integer.parseInt(pa.getVps()) >= 22)
			{
				victoryMessage = "Axis achieve VP victory";
				aBridge.getHistoryWriter().startEvent(victoryMessage);
				Collection<PlayerID> winners = data.getAllianceTracker().getPlayersCollectionInAlliance(
							data.getAllianceTracker().getAlliancesPlayerIsIn(japanese).iterator().next());
				signalGameOver(victoryMessage, winners, aBridge);
			}
		}
		
		// Check for Winning conditions
		if (isTotalVictory()) // Check for Win by Victory Cities
		{
			victoryMessage = " achieve TOTAL VICTORY with ";
			checkVictoryCities(aBridge, victoryMessage, " Total Victory VCs");
		}
		
		if (isHonorableSurrender())
		{
			victoryMessage = " achieve an HONORABLE VICTORY with ";
			checkVictoryCities(aBridge, victoryMessage, " Honorable Victory VCs");
		}
		
		if (isProjectionOfPower())
		{
			victoryMessage = " achieve victory through a PROJECTION OF POWER with ";
			checkVictoryCities(aBridge, victoryMessage, " Projection of Power VCs");
		}
		
		if (isEconomicVictory()) // Check for regular economic victory
		{
			Iterator<String> allianceIter = data.getAllianceTracker().getAlliances().iterator();
			String allianceName = null;
			while (allianceIter.hasNext())
			{
				allianceName = allianceIter.next();
				
				int victoryAmount = getEconomicVictoryAmount(data, allianceName);
				
				Set<PlayerID> teamMembers = data.getAllianceTracker().getPlayersInAlliance(allianceName);
				
				Iterator<PlayerID> teamIter = teamMembers.iterator();
				int teamProd = 0;
				while (teamIter.hasNext())
				{
					PlayerID player = teamIter.next();
					teamProd += getProduction(player);
					
					if (teamProd >= victoryAmount)
					{
						victoryMessage = allianceName + " achieve economic victory";
						aBridge.getHistoryWriter().startEvent(victoryMessage);
						Collection<PlayerID> winners = data.getAllianceTracker().getPlayersCollectionInAlliance(allianceName);
						// Added this to end the game on victory conditions
						signalGameOver(victoryMessage, winners, aBridge);
					}
				}
			}
		}
		
		// now check for generic trigger based victories
		if (isTriggeredVictory())
		{
			// it is end of round, so loop through all players
			Collection<PlayerID> playerList = data.getPlayerList().getPlayers();
			for (PlayerID p : playerList)
			{
				Tuple<String, Collection<PlayerID>> winnersMessage = TriggerAttachment.triggerVictory(p, aBridge, null, null);
				if (winnersMessage != null && winnersMessage.getFirst() != null)
				{
					victoryMessage = winnersMessage.getFirst();
					victoryMessage = NotificationMessages.getInstance().getMessage(victoryMessage);
					victoryMessage = "<html>" + victoryMessage + "</html>";
					signalGameOver(victoryMessage, winnersMessage.getSecond(), aBridge);
				}
			}
		}
		
		if (isWW2V2() || isWW2V3())
			return;
		
		PlayerList playerList = data.getPlayerList();
		// now test older maps that only use these 5 players, to see if someone has won
		PlayerID russians = playerList.getPlayerID(Constants.RUSSIANS);
		PlayerID germans = playerList.getPlayerID(Constants.GERMANS);
		PlayerID british = playerList.getPlayerID(Constants.BRITISH);
		PlayerID japanese = playerList.getPlayerID(Constants.JAPANESE);
		PlayerID americans = playerList.getPlayerID(Constants.AMERICANS);
		
		if (germans == null || russians == null || british == null || japanese == null || americans == null || playerList.size() > 5)
			return;
		
		// Quick check to see who still owns their own capital
		boolean russia = TerritoryAttachment.getCapital(russians, data).getOwner().equals(russians);
		boolean germany = TerritoryAttachment.getCapital(germans, data).getOwner().equals(germans);
		boolean britain = TerritoryAttachment.getCapital(british, data).getOwner().equals(british);
		boolean japan = TerritoryAttachment.getCapital(japanese, data).getOwner().equals(japanese);
		boolean america = TerritoryAttachment.getCapital(americans, data).getOwner().equals(americans);
		
		int count = 0;
		if (!russia)
			count++;
		if (!britain)
			count++;
		if (!america)
			count++;
		
		victoryMessage = " achieve a military victory";
		
		if (germany && japan && count >= 2)
		{
			aBridge.getHistoryWriter().startEvent("Axis" + victoryMessage);
			Collection<PlayerID> winners = data.getAllianceTracker().getPlayersCollectionInAlliance("Axis");
			signalGameOver(victoryMessage, winners, aBridge);
		}
		
		if (russia && !germany && britain && !japan && america)
		{
			aBridge.getHistoryWriter().startEvent("Allies" + victoryMessage);
			Collection<PlayerID> winners = data.getAllianceTracker().getPlayersCollectionInAlliance("Allies");
			signalGameOver(victoryMessage, winners, aBridge);
		}
	}

	@Override
	public void end()
	{
		super.end();
	}
	
	private void checkVictoryCities(IDelegateBridge aBridge, String victoryMessage, String victoryType)
	{
		GameData data = aBridge.getData();
		Iterator<String> allianceIter = data.getAllianceTracker().getAlliances().iterator();
		String allianceName = null;
		while (allianceIter.hasNext())
		{
			allianceName = allianceIter.next();
			
			int vcAmount = getVCAmount(data, allianceName, victoryType);
			
			Set<PlayerID> teamMembers = data.getAllianceTracker().getPlayersInAlliance(allianceName);
			
			Iterator<PlayerID> teamIter = teamMembers.iterator();
			int teamVCs = Match.countMatches(data.getMap().getTerritories(),
						new CompositeMatchAnd<Territory>(Matches.TerritoryIsVictoryCity, Matches.isTerritoryAllied(teamIter.next(), data)));
			
			if (teamVCs >= vcAmount)
			{
				aBridge.getHistoryWriter().startEvent(allianceName + victoryMessage + vcAmount + " Victory Cities!");
				Collection<PlayerID> winners = data.getAllianceTracker().getPlayersCollectionInAlliance(allianceName);
				// Added this to end the game on victory conditions
				signalGameOver(allianceName + victoryMessage + vcAmount + " Victory Cities!", winners, aBridge);
			}
		}
	}
	
	private int getEconomicVictoryAmount(GameData data, String alliance)
	{
		try
		{
			return Integer.parseInt((String) data.getProperties().get(alliance + " Economic Victory"));
		} catch (NumberFormatException e)
		{
			return 1000;
		}
	}
	
	private int getVCAmount(GameData data, String alliance, String type)
	{
		try
		{
			return Integer.parseInt((String) data.getProperties().get(alliance + type));
		} catch (NumberFormatException e)
		{
			if (type.equals(" Total Victory VCs"))
			{
				return 18;
			}
			else if (type.equals(" Honorable Victory VCs"))
			{
				return 15;
			}
			else if (type.equals(" Projection of Power VCs"))
			{
				return 13;
			}
			
			return 1000;
		}
	}
	
	/**
	 * Notify all players that the game is over.
	 * 
	 * @param status
	 *            the "game over" text to be displayed to each user.
	 */
	public void signalGameOver(String status, Collection<PlayerID> winners, IDelegateBridge a_bridge)
	{
		// If the game is over, we need to be able to alert all UIs to that fact.
		// The display object can send a message to all UIs.
		if (!m_gameOver)
		{
			m_gameOver = true;
			m_winners = winners;
			// Make sure the user really wants to leave the game.
			int rVal = EventThreadJOptionPane.showConfirmDialog(null, status + "\nDo you want to continue?", "Continue", JOptionPane.YES_NO_OPTION);
			if (rVal != JOptionPane.OK_OPTION)
				a_bridge.stopGameSequence();
		}
	}
	
	/**
	 * if null, the game is not over yet
	 * 
	 * @return
	 */
	public Collection<PlayerID> getWinners()
	{
		if (!m_gameOver)
			return null;
		return m_winners;
	}
	
	private boolean isWW2V2()
	{
		return games.strategy.triplea.Properties.getWW2V2(getData());
	}
	
	private boolean isWW2V3()
	{
		return games.strategy.triplea.Properties.getWW2V3(getData());
	}
	
	private boolean isPacificTheater()
	{
		return games.strategy.triplea.Properties.getPacificTheater(getData());
	}
	
	private boolean isTotalVictory()
	{
		return games.strategy.triplea.Properties.getTotalVictory(getData());
	}
	
	private boolean isHonorableSurrender()
	{
		return games.strategy.triplea.Properties.getHonorableSurrender(getData());
	}
	
	private boolean isProjectionOfPower()
	{
		return games.strategy.triplea.Properties.getProjectionOfPower(getData());
	}
	
	private boolean isEconomicVictory()
	{
		return games.strategy.triplea.Properties.getEconomicVictory(getData());
	}
	
	private boolean isTriggeredVictory()
	{
		return games.strategy.triplea.Properties.getTriggeredVictory(getData());
	}
	
	public int getProduction(PlayerID id)
	{
		int sum = 0;
		
		Iterator<Territory> territories = getData().getMap().iterator();
		while (territories.hasNext())
		{
			Territory current = territories.next();
			if (current.getOwner().equals(id))
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
	@Override
	public Serializable saveState()
	{
		return Boolean.valueOf(m_gameOver);
	}
	
	/**
	 * Loads the delegates state
	 */
	@Override
	public void loadState(Serializable state)
	{
		m_gameOver = ((Boolean) state).booleanValue();
	}
	
	/*
	 * @see games.strategy.engine.delegate.IDelegate#getRemoteType()
	 */
	@Override
	public Class<? extends IRemote> getRemoteType()
	{
		return null;
	}
	
}
