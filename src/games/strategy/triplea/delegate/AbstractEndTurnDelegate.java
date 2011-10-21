/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public Licensec
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

/*
 * EndTurnDelegate.java
 * 
 * Created on November 2, 2001, 12:30 PM
 */

package games.strategy.triplea.delegate;

import games.strategy.common.delegate.BaseDelegate;
import games.strategy.engine.data.Change;
import games.strategy.engine.data.ChangeFactory;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.message.IRemote;
import games.strategy.engine.pbem.PBEMMessagePoster;
import games.strategy.triplea.Constants;
import games.strategy.triplea.Properties;
import games.strategy.triplea.attatchments.PlayerAttachment;
import games.strategy.triplea.attatchments.TechAttachment;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.triplea.delegate.remote.IAbstractEndTurnDelegate;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.triplea.player.ITripleaPlayer;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.Match;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * 
 * @author Sean Bridges
 * @version 1.0
 * 
 *          At the end of the turn collect income.
 */
public abstract class AbstractEndTurnDelegate extends BaseDelegate implements IAbstractEndTurnDelegate
{
	private boolean m_needToInitialize = true;
	private boolean m_hasPostedTurnSummary = false;
	
	private boolean doBattleShipsRepairEndOfTurn()
	{
		return games.strategy.triplea.Properties.getBattleships_Repair_At_End_Of_Round(getData());
	}
	
	private boolean isGiveUnitsByTerritory()
	{
		return games.strategy.triplea.Properties.getGiveUnitsByTerritory(getData());
	}
	
	/**
	 * Called before the delegate will run.
	 */
	@Override
	public void start(IDelegateBridge aBridge)
	{
		super.start(aBridge);
		if (!m_needToInitialize)
			return;
		m_hasPostedTurnSummary = false;
		GameData data = getData();
		
		// can't collect unless you own your own capital
		PlayerAttachment pa = PlayerAttachment.get(m_player);
		List<Territory> capitalsListOriginal = new ArrayList<Territory>(TerritoryAttachment.getAllCapitals(m_player, data));
		List<Territory> capitalsListOwned = new ArrayList<Territory>(TerritoryAttachment.getAllCurrentlyOwnedCapitals(m_player, data));
		if ((!capitalsListOriginal.isEmpty() && capitalsListOwned.isEmpty()) || (pa != null && pa.getRetainCapitalProduceNumber() > capitalsListOwned.size()))
			return;
		
		Resource PUs = data.getResourceList().getResource(Constants.PUS);
		// just collect resources
		Collection<Territory> territories = data.getMap().getTerritoriesOwnedBy(m_player);
		
		int toAdd = getProduction(territories);
		int blockadeLoss = getProductionLoss(m_player, data);
		toAdd -= blockadeLoss;
		toAdd *= Properties.getPU_Multiplier(data);
		int total = m_player.getResources().getQuantity(PUs) + toAdd;
		
		String transcriptText;
		if (blockadeLoss == 0)
			transcriptText = m_player.getName() + " collect " + toAdd + MyFormatter.pluralize(" PU", toAdd) + "; end with " + total + MyFormatter.pluralize(" PU", total) + " total";
		else
			transcriptText = m_player.getName() + " collect " + toAdd + MyFormatter.pluralize(" PU", toAdd) + " (" + blockadeLoss + " lost to blockades)" + "; end with " + total
						+ MyFormatter.pluralize(" PU", total) + " total";
		aBridge.getHistoryWriter().startEvent(transcriptText);
		
		if (isWarBonds(m_player))
		{
			int bonds = rollWarBonds(aBridge);
			total += bonds;
			toAdd += bonds;
			transcriptText = m_player.getName() + " collect " + bonds + MyFormatter.pluralize(" PU", bonds) + " from War Bonds; end with " + total + MyFormatter.pluralize(" PU", total) + " total";
			aBridge.getHistoryWriter().startEvent(transcriptText);
		}
		
		Change change = ChangeFactory.changeResourcesChange(m_player, PUs, toAdd);
		aBridge.addChange(change);
		
		if (data.getProperties().get(Constants.PACIFIC_THEATER, false) && pa != null)
		{
			Change changeVP = (ChangeFactory.attachmentPropertyChange(pa, (new Integer(Integer.parseInt(pa.getVps()) + (toAdd / 10 + Integer.parseInt(pa.getCaptureVps()) / 10))).toString(), "vps"));
			Change changeCapVP = ChangeFactory.attachmentPropertyChange(pa, "0", "captureVps");
			CompositeChange ccVP = new CompositeChange(changeVP, changeCapVP);
			aBridge.addChange(ccVP);
		}
		
		doNationalObjectivesAndOtherEndTurnEffects(aBridge);
		
		if (doBattleShipsRepairEndOfTurn())
		{
			MoveDelegate.repairBattleShips(aBridge, aBridge.getPlayerID(), false);
		}
		m_needToInitialize = false;
		
		if (isGiveUnitsByTerritory() && pa != null && pa.getGiveUnitControl() != null && !pa.getGiveUnitControl().isEmpty())
		{
			changeUnitOwnership(aBridge);
		}
	}
	
	private int rollWarBonds(IDelegateBridge aBridge)
	{
		PlayerID player = aBridge.getPlayerID();
		int count = 1;
		int sides = aBridge.getPlayerID().getData().getDiceSides();
		String annotation = player.getName() + " roll to resolve War Bonds: ";
		
		DiceRoll dice;
		dice = DiceRoll.rollNDice(aBridge, count, sides, annotation);
		int total = dice.getDie(0).getValue() + 1;
		// TODO kev add dialog showing dice when built
		getRemotePlayer(player).reportMessage(annotation + total);
		return total;
	}
	
	private ITripleaPlayer getRemotePlayer(PlayerID player)
	{
		return (ITripleaPlayer) m_bridge.getRemote(player);
	}
	
	private void changeUnitOwnership(IDelegateBridge aBridge)
	{
		PlayerID Player = aBridge.getPlayerID();
		PlayerAttachment pa = PlayerAttachment.get(Player);
		Collection<PlayerID> PossibleNewOwners = pa.getGiveUnitControl();
		
		Collection<Territory> territories = aBridge.getData().getMap().getTerritories();
		Iterator<Territory> terrIter = territories.iterator();
		while (terrIter.hasNext())
		{
			Territory currTerritory = terrIter.next();
			TerritoryAttachment ta = (TerritoryAttachment) currTerritory.getAttachment(Constants.TERRITORY_ATTACHMENT_NAME);
			// if ownership should change in this territory
			if (ta != null && ta.getChangeUnitOwners() != null && !ta.getChangeUnitOwners().isEmpty())
			{
				Collection<PlayerID> terrNewOwners = ta.getChangeUnitOwners();
				for (PlayerID terrNewOwner : terrNewOwners)
				{
					if (PossibleNewOwners.contains(terrNewOwner))
					{
						// PlayerOwnerChange
						Collection<Unit> units = currTerritory.getUnits().getMatches(new CompositeMatchAnd<Unit>(Matches.unitOwnedBy(Player), Matches.UnitCanBeGivenByTerritoryTo(terrNewOwner)));
						Change changeOwner = ChangeFactory.changeOwner(units, terrNewOwner, currTerritory);
						aBridge.getHistoryWriter().addChildToEvent(changeOwner.toString());
						aBridge.addChange(changeOwner);
					}
				}
			}
		}
	}
	
	protected abstract void doNationalObjectivesAndOtherEndTurnEffects(IDelegateBridge bridge);
	
	protected int getProduction(Collection<Territory> territories)
	{
		int value = 0;
		Iterator<Territory> iter = territories.iterator();
		while (iter.hasNext())
		{
			Territory current = iter.next();
			TerritoryAttachment attatchment = (TerritoryAttachment) current.getAttachment(Constants.TERRITORY_ATTACHMENT_NAME);
			
			if (attatchment == null)
				throw new IllegalStateException("No attachment for owned territory:" + current.getName());
			
			// Check if territory is originally owned convoy center
			if (current.isWater())
			{
				// Preset the original owner
				PlayerID origOwner = attatchment.getOccupiedTerrOf();
				if (origOwner == null)
					origOwner = DelegateFinder.battleDelegate(getData()).getOriginalOwnerTracker().getOriginalOwner(current);
				
				if (origOwner != PlayerID.NULL_PLAYERID && origOwner == current.getOwner())
					value += attatchment.getProduction();
			}
			else
			{
				// if it's a convoy route
				if (TerritoryAttachment.get(current).isConvoyRoute())
				{
					// Determine if both parts of the convoy route are owned by the attacker or allies
					boolean ownedConvoyRoute = getData().getMap().getNeighbors(current, Matches.territoryHasConvoyOwnedBy(current.getOwner(), getData(), current)).size() > 0;
					if (ownedConvoyRoute)
						value += attatchment.getProduction();
					
				}
				else
				// just add the normal land territories
				{
					value += attatchment.getProduction();
				}
			}
			
		}
		return value;
	}
	
	// finds losses due to blockades etc, positive value returned.
	protected int getProductionLoss(PlayerID player, GameData data)
	{
		Collection<Territory> blockable = Match.getMatches(data.getMap().getTerritories(), Matches.territoryIsBlockadeZone);
		Match<Unit> enemyUnits = new CompositeMatchAnd<Unit>(Matches.enemyUnit(player, data));
		int totalLoss = 0;
		for (Territory b : blockable)
		{
			int maxLoss = 0;
			for (Territory m : data.getMap().getNeighbors(b))
			{
				if (m.getOwner().equals(player))
					maxLoss += TerritoryAttachment.get(m).getProduction();
			}
			int loss = 0;
			Collection<Unit> enemies = Match.getMatches(b.getUnits().getUnits(), enemyUnits);
			for (Unit u : enemies)
				loss += UnitAttachment.get(u.getType()).getBlockade();
			totalLoss += Math.min(maxLoss, loss);
		}
		return totalLoss;
	}
	
	private boolean isWarBonds(PlayerID player)
	{
		TechAttachment ta = (TechAttachment) player.getAttachment(Constants.TECH_ATTACHMENT_NAME);
		if (ta != null)
			return ta.hasWarBonds();
		
		return false;
	}
	
	@Override
	public void setHasPostedTurnSummary(boolean hasPostedTurnSummary)
	{
		m_hasPostedTurnSummary = hasPostedTurnSummary;
	}
	
	@Override
	public boolean getHasPostedTurnSummary()
	{
		return m_hasPostedTurnSummary;
	}
	
	@Override
	public boolean postTurnSummary(PBEMMessagePoster poster)
	{
		m_hasPostedTurnSummary = poster.post(m_bridge.getHistoryWriter());
		return m_hasPostedTurnSummary;
	}
	
	@Override
	public String getName()
	{
		return m_name;
	}
	
	@Override
	public String getDisplayName()
	{
		return m_displayName;
	}
	
	/**
	 * Called before the delegate will stop running.
	 */
	@Override
	public void end()
	{
		m_needToInitialize = true;
		DelegateFinder.battleDelegate(getData()).getBattleTracker().clear();
	}
	
	/* 
	 * @see games.strategy.engine.delegate.IDelegate#getRemoteType()
	 */
	@Override
	public Class<? extends IRemote> getRemoteType()
	{
		return IAbstractEndTurnDelegate.class;
	}
	
	/**
	 * Returns the state of the Delegate.
	 */
	@Override
	public Serializable saveState()
	{
		EndTurnState state = new EndTurnState();
		state.m_needToInitialize = m_needToInitialize;
		state.m_hasPostedTurnSummary = m_hasPostedTurnSummary;
		return state;
	}
	
	/**
	 * Loads the delegates state
	 */
	@Override
	public void loadState(Serializable aState)
	{
		if (aState != null)
		{
			EndTurnState state = (EndTurnState) aState;
			m_needToInitialize = state.m_needToInitialize;
			m_hasPostedTurnSummary = state.m_hasPostedTurnSummary;
		}
	}
}


@SuppressWarnings("serial")
class EndTurnState
			implements Serializable
{
	
	EndTurnState()
	{
	}
	
	public boolean m_needToInitialize;
	public boolean m_hasPostedTurnSummary;
}
