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

package games.strategy.common.delegate;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.delegate.IDelegate;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.message.IRemote;
import games.strategy.triplea.attatchments.TriggerAttachment;
import games.strategy.triplea.delegate.EndRoundDelegate;
import games.strategy.triplea.player.ITripleaPlayer;
import games.strategy.triplea.ui.NotificationMessages;
import games.strategy.util.Tuple;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;

/**
 * Base class designed to make writing custom delegates simpler.
 * Code common to all delegates is implemented here.
 * 
 * @author Lane Schwartz
 */
public abstract class BaseDelegate implements IDelegate
{
	protected String m_name;
	protected String m_displayName;
	protected PlayerID m_player;
	protected IDelegateBridge m_bridge;
	
	/**
	 * Creates a new instance of the Delegate
	 */
	public BaseDelegate()
	{
	}
	
	public void initialize(String name, String displayName)
	{
		m_name = name;
		m_displayName = displayName;
	}
	
	/**
	 * Called before the delegate will run.
	 */
	
	public void start(IDelegateBridge bridge)
	{
		m_bridge = bridge;
		m_player = bridge.getPlayerID();
		triggerWhenTriggerAttachments(TriggerAttachment.BEFORE);
	}
	
	public String getName()
	{
		return m_name;
	}
	
	public String getDisplayName()
	{
		return m_displayName;
	}
	
	/**
	 * Called before the delegate will stop running.
	 */
	
	public void end()
	{
		// normally nothing to do here
		// we are firing triggers, for maps that include them
		triggerWhenTriggerAttachments(TriggerAttachment.AFTER);
	}
	
	/**
	 * Returns the state of the Delegate.
	 */
	
	public Serializable saveState()
	{
		// This delegate does not maintain internal state
		return null;
	}
	
	/**
	 * Loads the delegates state
	 */
	
	public void loadState(Serializable state)
	{
		// This delegate does not maintain internal state
	}
	
	/**
	 * If this class implements an interface which inherits from IRemote, returns the class of that interface.
	 * Otherwise, returns null.
	 */
	
	public abstract Class<? extends IRemote> getRemoteType();
	
	public IDelegateBridge getBridge()
	{
		return m_bridge;
	}
	
	protected GameData getData()
	{
		return m_bridge.getData();
	}
	
	private void triggerWhenTriggerAttachments(String beforeOrAfter)
	{
		GameData data = getData();
		if (games.strategy.triplea.Properties.getTriggers(data))
		{
			String stepName = data.getSequence().getStep().getName();
			for (PlayerID aPlayer : data.getPlayerList())
			{
				// TODO: add all possible triggers here (in addition to their default locations)
				TriggerAttachment.triggerPlayerPropertyChange(aPlayer, m_bridge, beforeOrAfter, stepName);
				TriggerAttachment.triggerRelationshipTypePropertyChange(aPlayer, m_bridge, beforeOrAfter, stepName);
				TriggerAttachment.triggerTerritoryPropertyChange(aPlayer, m_bridge, beforeOrAfter, stepName);
				TriggerAttachment.triggerUnitPropertyChange(aPlayer, m_bridge, beforeOrAfter, stepName);
				TriggerAttachment.triggerTerritoryEffectPropertyChange(aPlayer, m_bridge, beforeOrAfter, stepName);
				
				TriggerAttachment.triggerRelationshipChange(aPlayer, m_bridge, beforeOrAfter, stepName);
				TriggerAttachment.triggerAvailableTechChange(aPlayer, m_bridge, beforeOrAfter, stepName);
				TriggerAttachment.triggerTechChange(aPlayer, m_bridge, beforeOrAfter, stepName);
				TriggerAttachment.triggerProductionFrontierEditChange(aPlayer, m_bridge, beforeOrAfter, stepName);
				TriggerAttachment.triggerProductionChange(aPlayer, m_bridge, beforeOrAfter, stepName);
				TriggerAttachment.triggerPurchase(aPlayer, m_bridge, beforeOrAfter, stepName);
				TriggerAttachment.triggerSupportChange(aPlayer, m_bridge, beforeOrAfter, stepName);
				TriggerAttachment.triggerUnitPlacement(aPlayer, m_bridge, beforeOrAfter, stepName);
				TriggerAttachment.triggerResourceChange(aPlayer, m_bridge, beforeOrAfter, stepName);
				
				// now do notifications:
				Iterator<String> notificationMessages = TriggerAttachment.triggerNotifications(aPlayer, m_bridge, beforeOrAfter, stepName).iterator();
				while (notificationMessages.hasNext())
				{
					String notificationMessageKey = notificationMessages.next();
					String message = NotificationMessages.getInstance().getMessage(notificationMessageKey);
					message = "<html>" + message + "</html>";
					((ITripleaPlayer) m_bridge.getRemote(m_player)).reportMessage(message);
					;
				}
				
				// now do victory messages:
				Tuple<String, Collection<PlayerID>> winnersMessage = TriggerAttachment.triggerVictory(aPlayer, m_bridge, beforeOrAfter, stepName);
				if (winnersMessage != null && winnersMessage.getFirst() != null)
				{
					String victoryMessage = winnersMessage.getFirst();
					victoryMessage = NotificationMessages.getInstance().getMessage(victoryMessage);
					victoryMessage = "<html>" + victoryMessage + "</html>";
					IDelegate delegateEndRound = data.getDelegateList().getDelegate("endRound");
					((EndRoundDelegate) delegateEndRound).signalGameOver(victoryMessage, winnersMessage.getSecond(), m_bridge);
				}
			}
		}
	}
}
