package games.strategy.triplea.delegate;

import games.strategy.common.delegate.BaseTripleADelegate;
import games.strategy.engine.data.Change;
import games.strategy.engine.data.ChangeFactory;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Resource;
import games.strategy.engine.random.IRandomStats.DiceType;
import games.strategy.sound.SoundPath;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attatchments.AbstractConditionsAttachment;
import games.strategy.triplea.attatchments.ICondition;
import games.strategy.triplea.attatchments.UserActionAttachment;
import games.strategy.triplea.delegate.remote.IUserActionDelegate;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.triplea.ui.UserActionText;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

public class UserActionDelegate extends BaseTripleADelegate implements IUserActionDelegate
{
	public UserActionDelegate()
	{
	}
	
	/**
	 * Called before the delegate will run.
	 */
	@Override
	public void start()
	{
		super.start();
	}
	
	@Override
	public void end()
	{
		super.end();
		resetAttempts();
	}
	
	@Override
	public Serializable saveState()
	{
		final UserActionExtendedDelegateState state = new UserActionExtendedDelegateState();
		state.superState = super.saveState();
		// state.m_testedConditions = m_testedConditions;
		// add other variables to state here:
		return state;
	}
	
	@Override
	public void loadState(final Serializable state)
	{
		final UserActionExtendedDelegateState s = (UserActionExtendedDelegateState) state;
		super.loadState(s.superState);
		// m_testedConditions = s.m_testedConditions;
		// load other variables from state here:
	}
	
	public boolean delegateCurrentlyRequiresUserInput()
	{
		// if (!m_player.amNotDeadYet(getData()))
		// return false;
		if (getValidActions().isEmpty())
			return false;
		return true;
	}
	
	public HashMap<ICondition, Boolean> getTestedConditions()
	{
		final HashSet<ICondition> allConditionsNeeded = AbstractConditionsAttachment.getAllConditionsRecursive(new HashSet<ICondition>(UserActionAttachment.getUserActionAttachments(m_player)), null);
		return AbstractConditionsAttachment.testAllConditionsRecursive(allConditionsNeeded, null, m_bridge);
	}
	
	public Collection<UserActionAttachment> getValidActions()
	{
		final GameData data = m_bridge.getData();
		final HashMap<ICondition, Boolean> testedConditions;
		data.acquireReadLock();
		try
		{
			testedConditions = getTestedConditions();
		} finally
		{
			data.releaseReadLock();
		}
		return UserActionAttachment.getValidActions(m_player, testedConditions, data);
	}
	
	public void attemptAction(final UserActionAttachment actionChoice)
	{
		if (actionChoice.canPerform(getTestedConditions()))
		{
			if (checkEnoughMoney(actionChoice))
			{ // See if the player has got enough money to pay for the action
				chargeForAction(actionChoice); // Charge for attempting the action
				actionChoice.useAttempt(getBridge()); // take one of the uses this round
				if (actionRollSucceeds(actionChoice))
				{ // See if the action is successful
					if (actionIsAccepted(actionChoice))
					{
						activateTriggers(actionChoice); // activate the triggers
						notifySuccess(actionChoice); // notify the players
					}
					else
					{
						notifyFailure(actionChoice); // notify the players of the failed attempt
					}
				}
				else
				{
					notifyFailure(actionChoice); // notify the players of the failed attempt
				}
			}
			else
			{
				notifyMoney(actionChoice, false); // notify the player he hasn't got enough money;
			}
		}
		else
		{
			notifyNoValidAction(actionChoice); // notify the player the action isn't valid anymore (shouldn't happen)
		}
	}
	
	/**
	 * @param uaa
	 *            The UserActionAttachment the player should be charged for
	 * @return false if the player can't afford the action
	 */
	private boolean checkEnoughMoney(final UserActionAttachment uaa)
	{
		final Resource PUs = getData().getResourceList().getResource(Constants.PUS);
		final int cost = uaa.getCostPU();
		final int has = m_bridge.getPlayerID().getResources().getQuantity(PUs);
		return has >= cost;
	}
	
	/**
	 * Subtract money from the players wallet
	 * 
	 * @param uaa
	 *            the UserActionAttachment this the money is charged for.
	 */
	private void chargeForAction(final UserActionAttachment uaa)
	{
		final Resource PUs = getData().getResourceList().getResource(Constants.PUS);
		final int cost = uaa.getCostPU();
		if (cost > 0)
		{
			// don't notify user of spending money anymore
			// notifyMoney(uaa, true);
			final String transcriptText = m_bridge.getPlayerID().getName() + " spend " + cost + " PU on User Action: " + MyFormatter.attachmentNameToText(uaa.getName());
			m_bridge.getHistoryWriter().startEvent(transcriptText);
			final Change charge = ChangeFactory.changeResourcesChange(m_bridge.getPlayerID(), PUs, -cost);
			m_bridge.addChange(charge);
		}
		else
		{
			final String transcriptText = m_bridge.getPlayerID().getName() + " takes action: " + MyFormatter.attachmentNameToText(uaa.getName());
			m_bridge.getHistoryWriter().startEvent(transcriptText); // we must start an event anyway
		}
	}
	
	/**
	 * @param uaa
	 *            the action to check if it succeeds
	 * @return true if the action succeeds, usually because the die-roll succeeded.
	 */
	private boolean actionRollSucceeds(final UserActionAttachment uaa)
	{
		final int hitTarget = uaa.getChanceToHit();
		final int diceSides = uaa.getChanceDiceSides();
		if (diceSides <= 0 || hitTarget >= diceSides)
		{
			uaa.changeChanceDecrementOrIncrementOnSuccessOrFailure(m_bridge, true, true);
			return true;
		}
		else if (hitTarget <= 0)
		{
			uaa.changeChanceDecrementOrIncrementOnSuccessOrFailure(m_bridge, false, true);
			return false;
		}
		final int rollResult = m_bridge.getRandom(diceSides, m_player, DiceType.NONCOMBAT, "Attempting the User Action: " + MyFormatter.attachmentNameToText(uaa.getName())) + 1;
		final boolean success = rollResult <= hitTarget;
		final String notificationMessage = "rolling (" + hitTarget + " out of " + diceSides + ") result: " + rollResult + " = " + (success ? "Success!" : "Failure!");
		m_bridge.getHistoryWriter().addChildToEvent(MyFormatter.attachmentNameToText(uaa.getName()) + " : " + notificationMessage);
		uaa.changeChanceDecrementOrIncrementOnSuccessOrFailure(m_bridge, success, true);
		sendNotification(notificationMessage);
		return success;
	}
	
	/**
	 * Get a list of players that should accept this action and then ask each
	 * player if it accepts this action.
	 * 
	 * @param uaa
	 *            the UserActionAttachment that should be accepted
	 * @return
	 */
	private boolean actionIsAccepted(final UserActionAttachment uaa)
	{
		for (final PlayerID player : uaa.getActionAccept())
		{
			if (!(getRemotePlayer(player)).acceptAction(m_player, UserActionText.getInstance().getAcceptanceQuestion(uaa.getText()), false))
				return false;
		}
		return true;
	}
	
	/**
	 * Fire triggers
	 * 
	 * @param uaa
	 *            the UserActionAttachment to activate triggers for
	 */
	private void activateTriggers(final UserActionAttachment uaa)
	{
		UserActionAttachment.fireTriggers(uaa, getTestedConditions(), m_bridge);
	}
	
	/**
	 * Let all players involved in this action know the action was successful
	 * 
	 * @param uaa
	 *            the UserActionAttachment that just succeeded.
	 */
	private void notifySuccess(final UserActionAttachment uaa)
	{
		// play a sound
		getSoundChannel().playSoundForAll(SoundPath.CLIP_USER_ACTION_SUCCESSFUL, m_player.getName());
		sendNotification(UserActionText.getInstance().getNotificationSucccess(uaa.getText()));
		notifyOtherPlayers(uaa, UserActionText.getInstance().getNotificationSuccessOthers(uaa.getText()));
	}
	
	/**
	 * Send a notification to the current player
	 * 
	 * @param text
	 *            if NONE don't send a notification
	 */
	private void sendNotification(final String text)
	{
		if (!"NONE".equals(text))
			this.getRemotePlayer().reportMessage(text, text); // "To " + m_player.getName() + ": " +
	}
	
	/**
	 * Send a notification to the other players involved in this action (all
	 * players except the player starting the action)
	 * 
	 * @param uaa
	 * @param notification
	 */
	private void notifyOtherPlayers(final UserActionAttachment uaa, final String notification)
	{
		if (!"NONE".equals(notification))
		{
			// we can send it to just uaa.getOtherPlayers(), or we can send it to all players. both are good options.
			final Collection<PlayerID> currentPlayer = new ArrayList<PlayerID>();
			currentPlayer.add(m_player);
			final Collection<PlayerID> otherPlayers = getData().getPlayerList().getPlayers();
			otherPlayers.removeAll(currentPlayer);
			this.getDisplay().reportMessageToPlayers(otherPlayers, currentPlayer, notification, notification);
		}
	}
	
	/**
	 * Let all players involved in this action know the action has failed.
	 * 
	 * @param uaa
	 *            the UserActionAttachment that just failed.
	 */
	private void notifyFailure(final UserActionAttachment uaa)
	{
		// play a sound
		getSoundChannel().playSoundForAll(SoundPath.CLIP_USER_ACTION_FAILURE, m_player.getName());
		final String transcriptText = m_bridge.getPlayerID().getName() + " fails on action: " + MyFormatter.attachmentNameToText(uaa.getName());
		m_bridge.getHistoryWriter().addChildToEvent(transcriptText);
		sendNotification(UserActionText.getInstance().getNotificationFailure(uaa.getText()));
		notifyOtherPlayers(uaa, UserActionText.getInstance().getNotificationFailureOthers(uaa.getText()));
	}
	
	/**
	 * Let the player know he is being charged for money or that he hasn't got
	 * enough money
	 * 
	 * @param uaa
	 *            the UserActionAttachment the player is notified about
	 * @param enough
	 *            is this a notification about enough or not enough money.
	 */
	private void notifyMoney(final UserActionAttachment uaa, final boolean enough)
	{
		if (enough)
		{
			sendNotification("Charging " + uaa.getCostPU() + " PU's to perform this action");
		}
		else
		{
			sendNotification("You don't have ennough money, you need " + uaa.getCostPU() + " PU's to perform this action");
		}
	}
	
	/**
	 * Let the player know this action isn't valid anymore, this shouldn't
	 * happen as the player shouldn't get an option to push the button on
	 * non-valid actions.
	 * 
	 * @param uaa
	 */
	private void notifyNoValidAction(final UserActionAttachment uaa)
	{
		sendNotification("This action isn't available anymore (this shouldn't happen!?!)");
	}
	
	/**
	 * Reset the attempts-counter for this action, so next round the player can
	 * try again for a number of attempts.
	 * 
	 */
	private void resetAttempts()
	{
		for (final UserActionAttachment uaa : UserActionAttachment.getUserActionAttachments(m_player))
		{
			uaa.resetAttempts(getBridge());
		}
	}
	
	/*
	 * @see games.strategy.engine.delegate.IDelegate#getRemoteType()
	 */
	@Override
	public Class<IUserActionDelegate> getRemoteType()
	{
		return IUserActionDelegate.class;
	}
}


class UserActionExtendedDelegateState implements Serializable
{
	private static final long serialVersionUID = -7521031770074984272L;
	Serializable superState;
	// add other variables here:
	// public HashMap<ICondition, Boolean> m_testedConditions = null;
}
