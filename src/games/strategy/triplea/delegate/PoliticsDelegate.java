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
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.RelationshipType;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Territory;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attatchments.PoliticalActionAttachment;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.attatchments.TriggerAttachment;
import games.strategy.triplea.delegate.remote.IPoliticsDelegate;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.triplea.player.ITripleaPlayer;
import games.strategy.triplea.ui.PoliticsText;
import games.strategy.util.CompositeMatchOr;
import games.strategy.util.Match;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;

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
	public void start(final IDelegateBridge aBridge)
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
			final Match<TriggerAttachment> politicsDelegateTriggerMatch = new CompositeMatchOr<TriggerAttachment>(
						TriggerAttachment.relationshipChangeMatch(null, null));
			TriggerAttachment.collectAndFireTriggers(new HashSet<PlayerID>(Collections.singleton(m_player)), politicsDelegateTriggerMatch, m_bridge);
		}
		chainAlliancesTogether(m_bridge);
		givesBackOriginalTerritories(m_bridge);
	}
	
	@Override
	public Serializable saveState()
	{
		final PoliticsExtendedDelegateState state = new PoliticsExtendedDelegateState();
		state.superState = super.saveState();
		// add other variables to state here:
		return state;
	}
	
	@Override
	public void loadState(final Serializable state)
	{
		final PoliticsExtendedDelegateState s = (PoliticsExtendedDelegateState) state;
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
	
	public void attemptAction(final PoliticalActionAttachment paa)
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
	private boolean actionIsAccepted(final PoliticalActionAttachment paa)
	{
		final GameData data = getData();
		final CompositeMatchOr<PoliticalActionAttachment> intoAlliedChainOrIntoOrOutOfWar = new CompositeMatchOr<PoliticalActionAttachment>(Matches.politicalActionIsRelationshipChangeOf(null,
					Matches.RelationshipTypeIsAlliedAndAlliancesCanChainTogether.invert(), Matches.RelationshipTypeIsAlliedAndAlliancesCanChainTogether, data),
					Matches.politicalActionIsRelationshipChangeOf(null,
								Matches.RelationshipTypeIsAtWar.invert(), Matches.RelationshipTypeIsAtWar, data), Matches.politicalActionIsRelationshipChangeOf(null, Matches.RelationshipTypeIsAtWar,
								Matches.RelationshipTypeIsAtWar.invert(), data));
		if (!games.strategy.triplea.Properties.getAlliancesCanChainTogether(data) || !intoAlliedChainOrIntoOrOutOfWar.match(paa))
		{
			for (final PlayerID player : paa.getActionAccept())
			{
				if (!((ITripleaPlayer) m_bridge.getRemote(player)).acceptPoliticalAction(PoliticsText.getInstance().getAcceptanceQuestion(paa.getText())))
					return false;
			}
		}
		else
		{
			// if alliances chain together, then our allies must have a say in anyone becoming a new ally/enemy
			final LinkedHashSet<PlayerID> playersWhoNeedToAccept = new LinkedHashSet<PlayerID>();
			playersWhoNeedToAccept.addAll(paa.getActionAccept());
			playersWhoNeedToAccept.addAll(Match.getMatches(data.getPlayerList().getPlayers(), Matches.isAlliedAndAlliancesCanChainTogether(m_player, data)));
			for (final PlayerID player : paa.getActionAccept())
			{
				playersWhoNeedToAccept.addAll(Match.getMatches(data.getPlayerList().getPlayers(), Matches.isAlliedAndAlliancesCanChainTogether(player, data)));
			}
			final HashSet<PlayerID> alliesWhoMustAccept = playersWhoNeedToAccept;
			alliesWhoMustAccept.removeAll(paa.getActionAccept());
			for (final PlayerID player : playersWhoNeedToAccept)
			{
				String actionText = PoliticsText.getInstance().getAcceptanceQuestion(paa.getText());
				if (actionText.equals("NONE"))
				{
					actionText = m_player.getName() + " wants to take the following action: " + MyFormatter.attachmentNameToText(paa.getName()) + " \r\n Do you approve?";
				}
				else
				{
					actionText = m_player.getName() + " wants to take the following action: " + MyFormatter.attachmentNameToText(paa.getName()) + ".  Do you approve? \r\n\r\n " + m_player.getName()
								+ " will ask " + MyFormatter.asList(paa.getActionAccept()) + ", the following question: \r\n " + actionText;
				}
				if (!((ITripleaPlayer) m_bridge.getRemote(player)).acceptPoliticalAction(actionText))
					return false;
			}
			for (final PlayerID player : paa.getActionAccept())
			{
				if (!((ITripleaPlayer) m_bridge.getRemote(player)).acceptPoliticalAction(PoliticsText.getInstance().getAcceptanceQuestion(paa.getText())))
					return false;
			}
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
	private void notifyNoValidAction(final PoliticalActionAttachment paa)
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
	private void notifyMoney(final PoliticalActionAttachment paa, final boolean ennough)
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
	private void chargeForAction(final PoliticalActionAttachment paa)
	{
		final Resource PUs = getData().getResourceList().getResource(Constants.PUS);
		final int cost = paa.getCostPU();
		if (cost > 0)
		{
			// don't notify user of spending money anymore
			// notifyMoney(paa, true);
			final String transcriptText = m_bridge.getPlayerID().getName() + " spend " + cost + " PU on Political Action: " + MyFormatter.attachmentNameToText(paa.getName());
			m_bridge.getHistoryWriter().startEvent(transcriptText);
			final Change charge = ChangeFactory.changeResourcesChange(m_bridge.getPlayerID(), PUs, -cost);
			m_bridge.addChange(charge);
		}
	}
	
	/**
	 * @param paa
	 *            The Political Action the player should be charged for
	 * @return false if the player can't afford the action
	 */
	private boolean checkEnoughMoney(final PoliticalActionAttachment paa)
	{
		final Resource PUs = getData().getResourceList().getResource(Constants.PUS);
		final int cost = paa.getCostPU();
		final int has = m_bridge.getPlayerID().getResources().getQuantity(PUs);
		return has >= cost;
	}
	
	/**
	 * Let all players involved in this action know the action has failed.
	 * 
	 * @param paa
	 *            the political action attachment that just failed.
	 */
	private void notifyFailure(final PoliticalActionAttachment paa)
	{
		final String transcriptText = m_bridge.getPlayerID().getName() + " fails on action: " + MyFormatter.attachmentNameToText(paa.getName());
		m_bridge.getHistoryWriter().startEvent(transcriptText);
		sendNotification(m_player, PoliticsText.getInstance().getNotificationFailure(paa.getText()));
		notifyOtherPlayers(paa, PoliticsText.getInstance().getNotificationFailureOthers(paa.getText()));
	}
	
	/**
	 * Let all players involved in this action know the action was successful
	 * 
	 * @param paa
	 *            the political action attachment that just failed.
	 */
	private void notifySuccess(final PoliticalActionAttachment paa)
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
	private void notifyOtherPlayers(final PoliticalActionAttachment paa, final String notification)
	{
		for (final PlayerID otherPlayer : paa.getOtherPlayers())
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
	private void sendNotification(final PlayerID player, final String text)
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
	private void changeRelationships(final PoliticalActionAttachment paa)
	{
		getMyselfOutOfAlliance(paa, m_player, m_bridge);
		getNeutralOutOfWarWithAllies(paa, m_player, m_bridge);
		final CompositeChange change = new CompositeChange();
		for (final String relationshipChange : paa.getRelationshipChange())
		{
			final String[] s = relationshipChange.split(":");
			final PlayerID player1 = getData().getPlayerList().getPlayerID(s[0]);
			final PlayerID player2 = getData().getPlayerList().getPlayerID(s[1]);
			final RelationshipType oldRelation = getData().getRelationshipTracker().getRelationshipType(player1, player2);
			final RelationshipType newRelation = getData().getRelationshipTypeList().getRelationshipType(s[2]);
			if (oldRelation.equals(newRelation))
				continue;
			change.add(ChangeFactory.relationshipChange(player1, player2, oldRelation, newRelation));
			m_bridge.getHistoryWriter().startEvent(
						m_bridge.getPlayerID().getName() + " succeeds on action: " + MyFormatter.attachmentNameToText(paa.getName()) + ": Changing Relationship for " + player1.getName() + " and "
									+ player2.getName() + " from " + oldRelation.getName() + " to " + newRelation.getName());
			/* creation of new battles is handled at the beginning of the battle delegate, in "setupUnitsInSameTerritoryBattles", not here.
			if (Matches.RelationshipTypeIsAtWar.match(newRelation))
				TriggerAttachment.triggerMustFightBattle(player1, player2, m_bridge);*/
		}
		if (!change.isEmpty())
			m_bridge.addChange(change);
		chainAlliancesTogether(m_bridge);
	}
	
	/**
	 * @param paa
	 *            the action to check if it succeeds
	 * @return true if the action succeeds, usually because the die-roll succeeded.
	 */
	private boolean actionSucceeds(final PoliticalActionAttachment paa)
	{
		if (paa.diceSides() == paa.toHit())
			return true;
		final int rollResult = m_bridge.getRandom(paa.diceSides(), "Attempting the PoliticalAction: " + MyFormatter.attachmentNameToText(paa.getName())) + 1;
		final boolean success = rollResult <= paa.toHit();
		final String notificationMessage = "rolling (" + paa.toHit() + " out of " + paa.diceSides() + ") result: " + rollResult + " " + (success ? "Success!" : "Failure!");
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
		for (final PoliticalActionAttachment paa : PoliticalActionAttachment.getPoliticalActionAttachments(m_player))
		{
			paa.resetAttempts(getBridge());
		}
	}
	
	public static void getMyselfOutOfAlliance(final PoliticalActionAttachment paa, final PlayerID player, final IDelegateBridge aBridge)
	{
		final GameData data = aBridge.getData();
		if (!games.strategy.triplea.Properties.getAlliancesCanChainTogether(data))
			return;
		final Collection<PlayerID> players = data.getPlayerList().getPlayers();
		final Collection<PlayerID> p1AlliedWith = Match.getMatches(players, Matches.isAlliedAndAlliancesCanChainTogether(player, data));
		p1AlliedWith.remove(player);
		final CompositeChange change = new CompositeChange();
		for (final String relationshipChangeString : paa.getRelationshipChange())
		{
			final String[] relationshipChange = relationshipChangeString.split(":");
			final PlayerID p1 = data.getPlayerList().getPlayerID(relationshipChange[0]);
			final PlayerID p2 = data.getPlayerList().getPlayerID(relationshipChange[1]);
			if (!(p1.equals(player) || p2.equals(player)))
				continue;
			final PlayerID pOther = (p1.equals(player) ? p2 : p1);
			if (!p1AlliedWith.contains(pOther))
				continue;
			final RelationshipType currentType = data.getRelationshipTracker().getRelationshipType(p1, p2);
			final RelationshipType newType = data.getRelationshipTypeList().getRelationshipType(relationshipChange[2]);
			if (Matches.RelationshipTypeIsAlliedAndAlliancesCanChainTogether.match(currentType) && Matches.RelationshipTypeIsAlliedAndAlliancesCanChainTogether.invert().match(newType))
			{
				for (final PlayerID p3 : p1AlliedWith)
				{
					final RelationshipType currentOther = data.getRelationshipTracker().getRelationshipType(p3, player);
					if (!currentOther.equals(newType))
					{
						change.add(ChangeFactory.relationshipChange(p3, player, currentOther, newType));
						aBridge.getHistoryWriter().startEvent(player.getName() + " and " + p3.getName() + " sign a " + newType.getName() + " treaty");
					}
				}
			}
		}
		if (!change.isEmpty())
			aBridge.addChange(change);
	}
	
	public static void getNeutralOutOfWarWithAllies(final PoliticalActionAttachment paa, final PlayerID player, final IDelegateBridge aBridge)
	{
		final GameData data = aBridge.getData();
		if (!games.strategy.triplea.Properties.getAlliancesCanChainTogether(data))
			return;
		// if (!Matches.isRelationshipChangeOf(null, Matches.RelationshipTypeIsAtWar, Matches.RelationshipTypeIsAtWar.invert(), data).match(paa))
		// return;
		final Collection<PlayerID> players = data.getPlayerList().getPlayers();
		final Collection<PlayerID> p1AlliedWith = Match.getMatches(players, Matches.isAlliedAndAlliancesCanChainTogether(player, data));
		final CompositeChange change = new CompositeChange();
		for (final String relationshipChangeString : paa.getRelationshipChange())
		{
			final String[] relationshipChange = relationshipChangeString.split(":");
			final PlayerID p1 = data.getPlayerList().getPlayerID(relationshipChange[0]);
			final PlayerID p2 = data.getPlayerList().getPlayerID(relationshipChange[1]);
			if (!(p1.equals(player) || p2.equals(player)))
				continue;
			final PlayerID pOther = (p1.equals(player) ? p2 : p1);
			final RelationshipType currentType = data.getRelationshipTracker().getRelationshipType(p1, p2);
			final RelationshipType newType = data.getRelationshipTypeList().getRelationshipType(relationshipChange[2]);
			if (Matches.RelationshipTypeIsAtWar.match(currentType) && Matches.RelationshipTypeIsAtWar.invert().match(newType))
			{
				final Collection<PlayerID> pOtherAlliedWith = Match.getMatches(players, Matches.isAlliedAndAlliancesCanChainTogether(pOther, data));
				if (!pOtherAlliedWith.contains(pOther))
					pOtherAlliedWith.add(pOther);
				if (!p1AlliedWith.contains(player))
					p1AlliedWith.add(player);
				for (final PlayerID p3 : p1AlliedWith)
				{
					for (final PlayerID p4 : pOtherAlliedWith)
					{
						final RelationshipType currentOther = data.getRelationshipTracker().getRelationshipType(p3, p4);
						if (!currentOther.equals(newType) && Matches.RelationshipTypeIsAtWar.match(currentOther))
						{
							change.add(ChangeFactory.relationshipChange(p3, p4, currentOther, newType));
							aBridge.getHistoryWriter().startEvent(p3.getName() + " and " + p4.getName() + " sign a " + newType.getName() + " treaty");
						}
					}
				}
			}
		}
		if (!change.isEmpty())
			aBridge.addChange(change);
	}
	
	public static void chainAlliancesTogether(final IDelegateBridge aBridge)
	{
		final GameData data = aBridge.getData();
		if (!games.strategy.triplea.Properties.getAlliancesCanChainTogether(data))
			return;
		final Collection<RelationshipType> allTypes = data.getRelationshipTypeList().getAllRelationshipTypes();
		RelationshipType alliedType = null;
		RelationshipType warType = null;
		for (final RelationshipType type : allTypes)
		{
			if (type.getRelationshipTypeAttachment().getIsDefaultWarPosition())
				warType = type;
			else if (type.getRelationshipTypeAttachment().getAlliancesCanChainTogether())
				alliedType = type;
		}
		if (alliedType == null)
			return;
		// first do alliances. then, do war (since we don't want to declare war on a potential ally).
		final Collection<PlayerID> players = data.getPlayerList().getPlayers();
		for (final PlayerID p1 : players)
		{
			final HashSet<PlayerID> p1NewAllies = new HashSet<PlayerID>();
			final Collection<PlayerID> p1AlliedWith = Match.getMatches(players, Matches.isAlliedAndAlliancesCanChainTogether(p1, data));
			for (final PlayerID p2 : p1AlliedWith)
			{
				p1NewAllies.addAll(Match.getMatches(players, Matches.isAlliedAndAlliancesCanChainTogether(p2, data)));
			}
			p1NewAllies.removeAll(p1AlliedWith);
			p1NewAllies.remove(p1);
			for (final PlayerID p3 : p1NewAllies)
			{
				if (!data.getRelationshipTracker().getRelationshipType(p1, p3).equals(alliedType))
				{
					aBridge.addChange(ChangeFactory.relationshipChange(p1, p3, data.getRelationshipTracker().getRelationshipType(p1, p3), alliedType));
					aBridge.getHistoryWriter().startEvent(p1.getName() + " and " + p3.getName() + " are joined together in an " + alliedType.getName() + " treaty");
				}
			}
		}
		// now war
		if (warType == null)
			return;
		for (final PlayerID p1 : players)
		{
			final HashSet<PlayerID> p1NewWar = new HashSet<PlayerID>();
			final Collection<PlayerID> p1WarWith = Match.getMatches(players, Matches.isAtWar(p1, data));
			final Collection<PlayerID> p1AlliedWith = Match.getMatches(players, Matches.isAlliedAndAlliancesCanChainTogether(p1, data));
			for (final PlayerID p2 : p1AlliedWith)
			{
				p1NewWar.addAll(Match.getMatches(players, Matches.isAtWar(p2, data)));
			}
			p1NewWar.removeAll(p1WarWith);
			p1NewWar.remove(p1);
			for (final PlayerID p3 : p1NewWar)
			{
				if (!data.getRelationshipTracker().getRelationshipType(p1, p3).equals(warType))
				{
					aBridge.addChange(ChangeFactory.relationshipChange(p1, p3, data.getRelationshipTracker().getRelationshipType(p1, p3), warType));
					aBridge.getHistoryWriter().startEvent(p1.getName() + " and " + p3.getName() + " declare " + warType.getName() + " on each other");
				}
			}
		}
	}
	
	public static void givesBackOriginalTerritories(final IDelegateBridge aBridge)
	{
		final GameData data = aBridge.getData();
		final OriginalOwnerTracker origOwnerTracker = DelegateFinder.battleDelegate(data).getOriginalOwnerTracker();
		final CompositeChange change = new CompositeChange();
		final Collection<PlayerID> players = data.getPlayerList().getPlayers();
		for (final PlayerID p1 : players)
		{
			for (final PlayerID p2 : players)
			{
				if (!data.getRelationshipTracker().givesBackOriginalTerritories(p1, p2))
					continue;
				for (final Territory t : data.getMap().getTerritoriesOwnedBy(p1))
				{
					final TerritoryAttachment ta = TerritoryAttachment.get(t);
					PlayerID original = ta.getOccupiedTerrOf();
					if (original == null)
						original = origOwnerTracker.getOriginalOwner(t);
					if (original == null)
						continue;
					if (original.equals(p2))
					{
						change.add(ChangeFactory.changeOwner(t, original));
					}
				}
			}
		}
		if (!change.isEmpty())
		{
			aBridge.getHistoryWriter().startEvent("Giving back territories to original owners");
			aBridge.addChange(change);
		}
	}
}


@SuppressWarnings("serial")
class PoliticsExtendedDelegateState implements Serializable
{
	Serializable superState;
	// add other variables here:
}
