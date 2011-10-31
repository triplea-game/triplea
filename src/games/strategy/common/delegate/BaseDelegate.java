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
	private boolean m_startBaseStepsFinished = false;
	private boolean m_endBaseStepsFinished = false;
	
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
	 * All classes should call super.start if they override this.
	 */
	public void start(IDelegateBridge bridge)
	{
		m_bridge = bridge;
		m_player = bridge.getPlayerID();
		if (!m_startBaseStepsFinished)
		{
			m_startBaseStepsFinished = true;
			triggerWhenTriggerAttachments(TriggerAttachment.BEFORE);
		}
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
	 * All classes should call super.end if they override this.
	 */
	public void end()
	{
		// normally nothing to do here
		// we are firing triggers, for maps that include them
		if (!m_endBaseStepsFinished)
		{
			m_endBaseStepsFinished = true;
			triggerWhenTriggerAttachments(TriggerAttachment.AFTER);
		}
		
		// these should probably be somewhere else, but we are relying on the fact that reloading a save go into the start step,
		// but nothing goes into the end step, and therefore there is no way to save then have the end step repeat itself
		m_startBaseStepsFinished = false;
		m_endBaseStepsFinished = false;
	}
	
	/**
	 * Returns the state of the Delegate.
	 * All classes should super.saveState if they override this.
	 */
	public Serializable saveState()
	{
		BaseDelegateState state = new BaseDelegateState();
		state.m_startBaseStepsFinished = m_startBaseStepsFinished;
		state.m_endBaseStepsFinished = m_endBaseStepsFinished;
		return state;
	}
	
	/**
	 * Loads the delegates state
	 */
	
	public void loadState(Serializable state)
	{
		BaseDelegateState s = (BaseDelegateState) state;
		m_startBaseStepsFinished = s.m_startBaseStepsFinished;
		m_endBaseStepsFinished = s.m_endBaseStepsFinished;
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
				// first do notifications, since we want the condition to still be true (after the non-notification trigger fires, the notification might not be true anymore)
				Iterator<String> notificationMessages = TriggerAttachment.triggerNotifications(aPlayer, m_bridge, beforeOrAfter, stepName).iterator();
				while (notificationMessages.hasNext())
				{
					String notificationMessageKey = notificationMessages.next();
					String message = NotificationMessages.getInstance().getMessage(notificationMessageKey);
					message = "<html>" + message + "</html>";
					((ITripleaPlayer) m_bridge.getRemote(m_player)).reportMessage(message, "Notification");
					;
				}
				
				// now do all non-notification, non-victory triggers
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

@SuppressWarnings("serial")
class BaseDelegateState implements Serializable
{
	public boolean m_startBaseStepsFinished = false;
	public boolean m_endBaseStepsFinished = false;
}

/*
All overriding classes should use the following format for saveState and loadState, in order to save and load the superstate

@SuppressWarnings("serial")
class ExtendedDelegateState implements Serializable
{
	Serializable superState;
	// add other variables here:
}

@Override
public Serializable saveState()
{
	ExtendedDelegateState state = new ExtendedDelegateState();
	state.superState = super.saveState();
	// add other variables to state here:
	return state;
}

@Override
public void loadState(Serializable state)
{
	ExtendedDelegateState s = (ExtendedDelegateState) state;
	super.loadState(s.superState);
	// load other variables from state here:
}
*/
