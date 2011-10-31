/*
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version. This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details. You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

/*
 * PoliticsDelegate.java
 * 
 * Created on July 16, 2011
 */

package games.strategy.triplea.delegate;

import games.strategy.common.delegate.BaseDelegate;
import games.strategy.engine.data.Change;
import games.strategy.engine.data.ChangeFactory;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.RelationshipType;
import games.strategy.engine.data.Resource;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attatchments.PoliticalActionAttachment;
import games.strategy.triplea.attatchments.TriggerAttachment;
import games.strategy.triplea.delegate.remote.IPoliticsDelegate;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.triplea.player.ITripleaPlayer;
import games.strategy.triplea.ui.PoliticsText;

import java.io.Serializable;

/**
 * 
 * Responsible allowing players to perform politicalActions
 * 
 * @author Edwin van der Wal
 * @version 1.0
 * 
 */
public class PoliticsDelegate extends BaseDelegate implements IPoliticsDelegate
{
	/** Creates new PoliticsDelegate */
	public PoliticsDelegate()
	{
		
	}
	
	/**
	 * Called before the delegate will run.
	 */
	
	@Override
	public void start(IDelegateBridge aBridge)
	{
		super.start(aBridge);
	}
	
	@Override
	public void end()
	{
		super.end();
		resetAttempts();
		if (games.strategy.triplea.Properties.getTriggers(getData()))
		{
			TriggerAttachment.triggerRelationshipChange(m_player, m_bridge, null, null);
		}
	}

	@Override
	public Serializable saveState()
	{
		PoliticsExtendedDelegateState state = new PoliticsExtendedDelegateState();
		state.superState = super.saveState();
		// add other variables to state here:
		return state;
	}

	@Override
	public void loadState(Serializable state)
	{
		PoliticsExtendedDelegateState s = (PoliticsExtendedDelegateState) state;
		super.loadState(s.superState);
		// load other variables from state here:
	}
	
	/*
	 * @see games.strategy.engine.delegate.IDelegate#getRemoteType()
	 */

	@Override
	public Class<IPoliticsDelegate> getRemoteType()
	{
		return IPoliticsDelegate.class;
	}
	
	public void attemptAction(PoliticalActionAttachment paa)
	{
		if (!games.strategy.triplea.Properties.getUsePolitics(getData()))
		{
			notifyPoliticsTurnedOff();
			return;
		}
		if (paa.canPerform())
		{
			if (checkEnoughMoney(paa))
			{ // See if the player has got enough money to pay for the action
				chargeForAction(paa); // Charge for attempting the action
				paa.useAttempt(getBridge()); // take one of the uses this round
				if (actionSucceeds(paa))
				{ // See if the action is successful
					if (actionIsAccepted(paa))
					{
						changeRelationships(paa); // change the relationships
						notifySuccess(paa); // notify the players
					}
					else
					{
						notifyFailure(paa); // notify the players of the failed attempt
					}
				}
				else
				{
					notifyFailure(paa); // notify the players of the failed attempt
				}
			}
			else
			{
				notifyMoney(paa, false); // notify the player he hasn't got enough money;
			}
		}
		else
		{
			notifyNoValidAction(paa); // notify the player the action isn't valid anymore (shouldn't happen)
		}
	}
	
	/**
	 * Get a list of players that should accept this action and then ask each
	 * player if it accepts this action.
	 * 
	 * @param paa
	 *            the politicalActionAttachment that should be accepted
	 * @return
	 */
	private boolean actionIsAccepted(PoliticalActionAttachment paa)
	{
		for (PlayerID player : paa.getActionAccept())
		{
			if (!((ITripleaPlayer) m_bridge.getRemote(player)).acceptPoliticalAction(PoliticsText.getInstance().getAcceptanceQuestion(paa.getText())))
				return false;
		}
		return true;
	}
	
	/**
	 * Let the player know this action isn't valid anymore, this shouldn't
	 * happen as the player shouldn't get an option to push the button on
	 * non-valid actions.
	 * 
	 * @param paa
	 */
	private void notifyNoValidAction(PoliticalActionAttachment paa)
	{
		sendNotification(m_player, "This action isn't available anymore (this shouldn't happen!?!)");
	}
	
	private void notifyPoliticsTurnedOff()
	{
		sendNotification(m_player, "Politics is turned off in the game options");
	}
	
	/**
	 * Let the player know he is being charged for money or that he hasn't got
	 * ennough money
	 * 
	 * @param paa
	 *            the actionattachment the player is notified about
	 * @param ennough
	 *            is this a notificaiton about ennough or not ennough money.
	 */
	private void notifyMoney(PoliticalActionAttachment paa, boolean ennough)
	{
		if (ennough)
		{
			sendNotification(m_player, "Charging " + paa.getCostPU() + " PU's to perform this action");
		}
		else
		{
			sendNotification(m_player, "You don't have ennough money, you need " + paa.getCostPU() + " PU's to perform this action");
		}
		
	}
	
	/**
	 * Subtract money from the players wallet
	 * 
	 * @param paa
	 *            the politicalactionattachment this the money is charged for.
	 */
	private void chargeForAction(PoliticalActionAttachment paa)
	{
		Resource PUs = getData().getResourceList().getResource(Constants.PUS);
		int cost = paa.getCostPU();
		if (cost > 0)
		{
			// don't notify user of spending money anymore
			// notifyMoney(paa, true);
			String transcriptText = m_bridge.getPlayerID().getName() + " spend " + cost + " PU on Political Action: " + paa.getName();
			m_bridge.getHistoryWriter().startEvent(transcriptText);
			
			Change charge = ChangeFactory.changeResourcesChange(m_bridge
						.getPlayerID(), PUs, -cost);
			m_bridge.addChange(charge);
		}
	}
	
	/**
	 * @param paa
	 *            The Political Action the player should be charged for
	 * @return false if the player can't afford the action
	 */
	private boolean checkEnoughMoney(PoliticalActionAttachment paa)
	{
		Resource PUs = getData().getResourceList().getResource(Constants.PUS);
		int cost = paa.getCostPU();
		int has = m_bridge.getPlayerID().getResources().getQuantity(PUs);
		return has >= cost;
	}
	
	/**
	 * Let all players involved in this action know the action has failed.
	 * 
	 * @param paa
	 *            the political action attachment that just failed.
	 */
	private void notifyFailure(PoliticalActionAttachment paa)
	{
		sendNotification(m_player, PoliticsText.getInstance().getNotificationFailure(paa.getText()));
		notifyOtherPlayers(paa, PoliticsText.getInstance().getNotificationFailureOthers(paa.getText()));
	}
	
	/**
	 * Let all players involved in this action know the action was successful
	 * 
	 * @param paa
	 *            the political action attachment that just failed.
	 */
	private void notifySuccess(PoliticalActionAttachment paa)
	{
		sendNotification(m_player, PoliticsText.getInstance().getNotificationSucccess(paa.getText()));
		notifyOtherPlayers(paa, PoliticsText.getInstance().getNotificationSuccessOthers(paa.getText()));
	}
	
	/**
	 * Send a notification to the other players involved in this action (all
	 * players except the player starting teh action)
	 * 
	 * @param paa
	 * @param notification
	 */
	private void notifyOtherPlayers(PoliticalActionAttachment paa, String notification)
	{
		for (PlayerID otherPlayer : paa.getOtherPlayers())
		{
			sendNotification(otherPlayer, notification);
		}
	}
	
	/**
	 * Send a notification to the players
	 * 
	 * @param player
	 * @param text
	 *            if NONE don't send a notification
	 */
	private void sendNotification(PlayerID player, String text)
	{
		if (!"NONE".equals(text))
			((ITripleaPlayer) m_bridge.getRemote(player)).reportMessage("To " + player.getName() + ": " + text, "To " + player.getName() + ": " + text);
	}
	
	/**
	 * Changes all relationships
	 * 
	 * @param paa
	 *            the political action to change the relationships for
	 */
	private void changeRelationships(PoliticalActionAttachment paa)
	{
		CompositeChange change = new CompositeChange();
		for (String relationshipChange : paa.getRelationshipChange())
		{
			String[] s = relationshipChange.split(":");
			PlayerID player1 = getData().getPlayerList().getPlayerID(s[0]);
			PlayerID player2 = getData().getPlayerList().getPlayerID(s[1]);
			RelationshipType oldRelation = getData().getRelationshipTracker().getRelationshipType(player1, player2);
			RelationshipType newRelation = getData().getRelationshipTypeList().getRelationshipType(s[2]);
			if (oldRelation.equals(newRelation))
				continue;
			
			change.add(ChangeFactory.relationshipChange(player1, player2, oldRelation, newRelation));
			
			m_bridge.getHistoryWriter().startEvent(
						MyFormatter.attachmentNameToText(paa.getName()) + ": Changing Relationship for " + player1.getName() + " and " + player2.getName() + " from " + oldRelation.getName() + " to "
									+ newRelation.getName());
			
			if (Matches.RelationshipIsAtWar.match(newRelation))
				TriggerAttachment.triggerMustFightBattle(player1, player2, m_bridge); // TODO: see if this causes problems to do with savestate, or relations that don't cause battles. better to leave this in the battle delegate i think.
		}
		if (!change.isEmpty())
			m_bridge.addChange(change);
	}
	
	/**
	 * @param paa
	 *            the action to check if it succeeds
	 * @return true if the action succeeds, usually because the die-roll succeeded.
	 */
	private boolean actionSucceeds(PoliticalActionAttachment paa)
	{
		if (paa.diceSides() == paa.toHit())
			return true;
		
		int rollResult = m_bridge.getRandom(paa.diceSides(), "Attempting the PoliticalAction: " + paa.getName()) + 1;
		boolean success = rollResult <= paa.toHit();
		String notificationMessage = "rolling (" + paa.toHit() + " out of " + paa.diceSides() + ") result: " + rollResult + " " + (success ? "Success!" : "Failure!");
		sendNotification(m_player, notificationMessage);
		m_bridge.getHistoryWriter().startEvent(MyFormatter.attachmentNameToText(paa.getName()) + " : " + notificationMessage);
		return success;
	}
	
	/**
	 * Reset the attemptscounter for this action, so next round the player can
	 * try again for a number of attempts.
	 * 
	 */
	private void resetAttempts()
	{
		for (PoliticalActionAttachment paa : PoliticalActionAttachment.getPoliticalActionAttachments(m_player))
		{
			paa.resetAttempts(getBridge());
		}
	}
}


@SuppressWarnings("serial")
class PoliticsExtendedDelegateState implements Serializable
{
	Serializable superState;
	// add other variables here:
}
