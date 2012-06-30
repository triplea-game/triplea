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
import games.strategy.engine.data.GameMap;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.RelationshipTracker.Relationship;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.message.IRemote;
import games.strategy.engine.pbem.PBEMMessagePoster;
import games.strategy.triplea.Constants;
import games.strategy.triplea.Properties;
import games.strategy.triplea.attatchments.PlayerAttachment;
import games.strategy.triplea.attatchments.RelationshipTypeAttachment;
import games.strategy.triplea.attatchments.TechAbilityAttachment;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.triplea.delegate.remote.IAbstractEndTurnDelegate;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.triplea.player.ITripleaPlayer;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.IntegerMap;
import games.strategy.util.Match;
import games.strategy.util.Tuple;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
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
	private static int CONVOY_BLOCKADE_DICE_SIDES = 6;
	
	private boolean doBattleShipsRepairEndOfTurn()
	{
		return games.strategy.triplea.Properties.getBattleshipsRepairAtEndOfRound(getData());
	}
	
	private boolean isGiveUnitsByTerritory()
	{
		return games.strategy.triplea.Properties.getGiveUnitsByTerritory(getData());
	}
	
	public boolean canPlayerCollectIncome(final PlayerID player, final GameData data)
	{
		final PlayerAttachment pa = PlayerAttachment.get(player);
		final List<Territory> capitalsListOriginal = new ArrayList<Territory>(TerritoryAttachment.getAllCapitals(player, data));
		final List<Territory> capitalsListOwned = new ArrayList<Territory>(TerritoryAttachment.getAllCurrentlyOwnedCapitals(player, data));
		if ((!capitalsListOriginal.isEmpty() && capitalsListOwned.isEmpty()) || (pa != null && pa.getRetainCapitalProduceNumber() > capitalsListOwned.size()))
			return false;
		return true;
	}
	
	/**
	 * Called before the delegate will run.
	 */
	@Override
	public void start(final IDelegateBridge aBridge)
	{
		// figure out our current PUs before we do anything else, including super methods
		final GameData data = aBridge.getData();
		final Resource PUs = data.getResourceList().getResource(Constants.PUS);
		final int leftOverPUs = aBridge.getPlayerID().getResources().getQuantity(PUs);
		super.start(aBridge);
		if (!m_needToInitialize)
			return;
		m_hasPostedTurnSummary = false;
		// can't collect unless you own your own capital
		if (!canPlayerCollectIncome(m_player, data))
		{
			rollWarBondsForFriends(aBridge, m_player, data);
			return; // we do not collect any income this turn
		}
		// just collect resources
		final Collection<Territory> territories = data.getMap().getTerritoriesOwnedBy(m_player);
		int toAdd = getProduction(territories);
		final int blockadeLoss = getProductionLoss(m_player, data, aBridge);
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
		
		// do war bonds
		final int bonds = rollWarBonds(aBridge, m_player, data);
		if (bonds > 0)
		{
			total += bonds;
			toAdd += bonds;
			transcriptText = m_player.getName() + " collect " + bonds + MyFormatter.pluralize(" PU", bonds) + " from War Bonds; end with " + total + MyFormatter.pluralize(" PU", total) + " total";
			aBridge.getHistoryWriter().startEvent(transcriptText);
		}
		
		if (total < 0)
		{
			toAdd -= total;
			total = 0;
		}
		final Change change = ChangeFactory.changeResourcesChange(m_player, PUs, toAdd);
		aBridge.addChange(change);
		final PlayerAttachment pa = PlayerAttachment.get(m_player);
		if (data.getProperties().get(Constants.PACIFIC_THEATER, false) && pa != null)
		{
			final Change changeVP = (ChangeFactory.attachmentPropertyChange(pa, (pa.getVps() + (toAdd / 10) + (pa.getCaptureVps() / 10)), "vps"));
			final Change changeCapVP = ChangeFactory.attachmentPropertyChange(pa, "0", "captureVps");
			final CompositeChange ccVP = new CompositeChange(changeVP, changeCapVP);
			aBridge.addChange(ccVP);
		}
		
		addOtherResources(aBridge);
		
		doNationalObjectivesAndOtherEndTurnEffects(aBridge);
		
		if (doBattleShipsRepairEndOfTurn())
		{
			MoveDelegate.repairBattleShips(aBridge, aBridge.getPlayerID(), false);
		}
		if (isGiveUnitsByTerritory() && pa != null && pa.getGiveUnitControl() != null && !pa.getGiveUnitControl().isEmpty())
		{
			changeUnitOwnership(aBridge);
		}
		// now we do upkeep costs, including upkeep cost as a percentage of our entire income for this turn (including NOs)
		final int currentPUs = m_player.getResources().getQuantity(PUs);
		final float gainedPUS = Math.max(0, currentPUs - leftOverPUs);
		int relationshipUpkeepCostFlat = 0;
		int relationshipUpkeepCostPercentage = 0;
		int relationshipUpkeepTotalCost = 0;
		for (final Relationship r : data.getRelationshipTracker().getRelationships(m_player))
		{
			final String[] upkeep = r.getRelationshipType().getRelationshipTypeAttachment().getUpkeepCost().split(":");
			if (upkeep.length == 1 || upkeep[1].equals(RelationshipTypeAttachment.UPKEEP_FLAT))
				relationshipUpkeepCostFlat += Integer.parseInt(upkeep[0]);
			else if (upkeep[1].equals(RelationshipTypeAttachment.UPKEEP_PERCENTAGE))
				relationshipUpkeepCostPercentage += Integer.parseInt(upkeep[0]);
		}
		relationshipUpkeepCostPercentage = Math.min(100, relationshipUpkeepCostPercentage);
		if (relationshipUpkeepCostPercentage != 0)
		{
			relationshipUpkeepTotalCost += Math.round(gainedPUS * (relationshipUpkeepCostPercentage) / 100f);
		}
		if (relationshipUpkeepCostFlat != 0)
		{
			relationshipUpkeepTotalCost += relationshipUpkeepCostFlat;
		}
		// we can't remove more than we have, and we also must flip the sign
		relationshipUpkeepTotalCost = Math.min(currentPUs, relationshipUpkeepTotalCost);
		relationshipUpkeepTotalCost = -1 * relationshipUpkeepTotalCost;
		if (relationshipUpkeepTotalCost != 0)
		{
			final int newTotal = currentPUs + relationshipUpkeepTotalCost;
			transcriptText = m_player.getName() + (relationshipUpkeepTotalCost < 0 ? " pays " : " taxes ") + (-1 * relationshipUpkeepTotalCost)
						+ MyFormatter.pluralize(" PU", relationshipUpkeepTotalCost) + " in order to maintain current relationships with other players, and ends the turn with " + newTotal
						+ MyFormatter.pluralize(" PU", newTotal);
			aBridge.getHistoryWriter().startEvent(transcriptText);
			final Change upkeep = ChangeFactory.changeResourcesChange(m_player, PUs, relationshipUpkeepTotalCost);
			aBridge.addChange(upkeep);
		}
		m_needToInitialize = false;
	}
	
	/**
	 * Called before the delegate will stop running.
	 */
	@Override
	public void end()
	{
		super.end();
		m_needToInitialize = true;
		DelegateFinder.battleDelegate(getData()).getBattleTracker().clear();
	}
	
	@Override
	public Serializable saveState()
	{
		final EndTurnExtendedDelegateState state = new EndTurnExtendedDelegateState();
		state.superState = super.saveState();
		// add other variables to state here:
		state.m_needToInitialize = m_needToInitialize;
		state.m_hasPostedTurnSummary = m_hasPostedTurnSummary;
		return state;
	}
	
	@Override
	public void loadState(final Serializable state)
	{
		final EndTurnExtendedDelegateState s = (EndTurnExtendedDelegateState) state;
		super.loadState(s.superState);
		// load other variables from state here:
		m_needToInitialize = s.m_needToInitialize;
		m_hasPostedTurnSummary = s.m_hasPostedTurnSummary;
	}
	
	private int rollWarBonds(final IDelegateBridge aBridge, final PlayerID player, final GameData data)
	{
		final int count = TechAbilityAttachment.getWarBondDiceNumber(player, data);
		final int sides = TechAbilityAttachment.getWarBondDiceSides(player, data);
		if (sides <= 0 || count <= 0)
			return 0;
		final String annotation = player.getName() + " rolling to resolve War Bonds: ";
		DiceRoll dice;
		dice = DiceRoll.rollNDice(aBridge, count, sides, annotation);
		int total = 0;
		for (int i = 0; i < dice.size(); i++)
		{
			total += dice.getDie(i).getValue() + 1;
		}
		getRemotePlayer(player).reportMessage(annotation + MyFormatter.asDice(dice), annotation + MyFormatter.asDice(dice));
		return total;
	}
	
	private void rollWarBondsForFriends(final IDelegateBridge aBridge, final PlayerID player, final GameData data)
	{
		final int count = TechAbilityAttachment.getWarBondDiceNumber(player, data);
		final int sides = TechAbilityAttachment.getWarBondDiceSides(player, data);
		if (sides <= 0 || count <= 0)
			return;
		// basically, if we are sharing our technology with someone, and we have warbonds but they do not, then we roll our warbonds and give them the proceeds (Global 1940)
		final PlayerAttachment playerattachment = PlayerAttachment.get(player);
		if (playerattachment == null)
			return;
		final Collection<PlayerID> shareWith = playerattachment.getShareTechnology();
		if (shareWith == null || shareWith.isEmpty())
			return;
		PlayerID giveWarBondsTo = null; // take first one
		for (final PlayerID p : shareWith)
		{
			final int pCount = TechAbilityAttachment.getWarBondDiceNumber(p, data);
			final int pSides = TechAbilityAttachment.getWarBondDiceSides(p, data);
			if (pSides <= 0 && pCount <= 0)
			{
				// if both are zero, then it must mean we did not share our war bonds tech with them, even though we are sharing all tech (because they can not have this tech)
				if (canPlayerCollectIncome(p, data))
				{
					giveWarBondsTo = p;
					break;
				}
			}
		}
		if (giveWarBondsTo == null)
			return;
		final String annotation = player.getName() + " rolling to resolve War Bonds, and giving results to " + giveWarBondsTo.getName() + ": ";
		final DiceRoll dice = DiceRoll.rollNDice(aBridge, count, sides, annotation);
		int totalWarBonds = 0;
		for (int i = 0; i < dice.size(); i++)
		{
			totalWarBonds += dice.getDie(i).getValue() + 1;
		}
		final Resource PUs = data.getResourceList().getResource(Constants.PUS);
		final int currentPUs = giveWarBondsTo.getResources().getQuantity(PUs);
		final String transcriptText = player.getName() + " rolls " + totalWarBonds + MyFormatter.pluralize(" PU", totalWarBonds) + " from War Bonds, giving the total to " + giveWarBondsTo.getName()
					+ ", who ends with " + (currentPUs + totalWarBonds) + MyFormatter.pluralize(" PU", (currentPUs + totalWarBonds)) + " total";
		aBridge.getHistoryWriter().startEvent(transcriptText);
		final Change change = ChangeFactory.changeResourcesChange(giveWarBondsTo, PUs, totalWarBonds);
		aBridge.addChange(change);
		getRemotePlayer(player).reportMessage(annotation + MyFormatter.asDice(dice), annotation + MyFormatter.asDice(dice));
		return;
	}
	
	private ITripleaPlayer getRemotePlayer(final PlayerID player)
	{
		return (ITripleaPlayer) m_bridge.getRemote(player);
	}
	
	private void changeUnitOwnership(final IDelegateBridge aBridge)
	{
		final PlayerID Player = aBridge.getPlayerID();
		final PlayerAttachment pa = PlayerAttachment.get(Player);
		final Collection<PlayerID> PossibleNewOwners = pa.getGiveUnitControl();
		final Collection<Territory> territories = aBridge.getData().getMap().getTerritories();
		final CompositeChange change = new CompositeChange();
		final Collection<Tuple<Territory, Collection<Unit>>> changeList = new ArrayList<Tuple<Territory, Collection<Unit>>>();
		for (final Territory currTerritory : territories)
		{
			final TerritoryAttachment ta = (TerritoryAttachment) currTerritory.getAttachment(Constants.TERRITORY_ATTACHMENT_NAME);
			// if ownership should change in this territory
			if (ta != null && ta.getChangeUnitOwners() != null && !ta.getChangeUnitOwners().isEmpty())
			{
				final Collection<PlayerID> terrNewOwners = ta.getChangeUnitOwners();
				for (final PlayerID terrNewOwner : terrNewOwners)
				{
					if (PossibleNewOwners.contains(terrNewOwner))
					{
						// PlayerOwnerChange
						final Collection<Unit> units = currTerritory.getUnits().getMatches(new CompositeMatchAnd<Unit>(Matches.unitOwnedBy(Player), Matches.UnitCanBeGivenByTerritoryTo(terrNewOwner)));
						if (!units.isEmpty())
						{
							change.add(ChangeFactory.changeOwner(units, terrNewOwner, currTerritory));
							changeList.add(new Tuple<Territory, Collection<Unit>>(currTerritory, units));
						}
					}
				}
			}
		}
		if (!change.isEmpty() && !changeList.isEmpty())
		{
			if (changeList.size() == 1)
			{
				final Tuple<Territory, Collection<Unit>> tuple = changeList.iterator().next();
				aBridge.getHistoryWriter().startEvent("Some Units in " + tuple.getFirst().getName() + " change ownership: " + MyFormatter.unitsToTextNoOwner(tuple.getSecond()), tuple.getSecond());
			}
			else
			{
				aBridge.getHistoryWriter().startEvent("Units Change Ownership");
				for (final Tuple<Territory, Collection<Unit>> tuple : changeList)
				{
					aBridge.getHistoryWriter().addChildToEvent("Some Units in " + tuple.getFirst().getName() + " change ownership: " + MyFormatter.unitsToTextNoOwner(tuple.getSecond()),
								tuple.getSecond());
				}
			}
			aBridge.addChange(change);
		}
	}
	
	protected abstract void addOtherResources(IDelegateBridge bridge);
	
	protected abstract void doNationalObjectivesAndOtherEndTurnEffects(IDelegateBridge bridge);
	
	protected int getProduction(final Collection<Territory> territories)
	{
		return getProduction(territories, getData());
	}
	
	public static int getProduction(final Collection<Territory> territories, final GameData data)
	{
		int value = 0;
		for (final Territory current : territories)
		{
			final TerritoryAttachment attachment = (TerritoryAttachment) current.getAttachment(Constants.TERRITORY_ATTACHMENT_NAME);
			if (attachment == null)
				throw new IllegalStateException("No attachment for owned territory:" + current.getName());
			// Check if territory is originally owned convoy center
			if (Matches.territoryCanCollectIncomeFrom(current.getOwner(), data).match(current))
				value += attachment.getProduction();
		}
		return value;
	}
	
	// finds losses due to blockades etc, positive value returned.
	protected int getProductionLoss(final PlayerID player, final GameData data, final IDelegateBridge aBridge)
	{
		final GameMap map = data.getMap();
		final Collection<Territory> blockable = Match.getMatches(map.getTerritories(), Matches.territoryIsBlockadeZone);
		if (blockable.isEmpty())
			return 0;
		final Match<Unit> enemyUnits = new CompositeMatchAnd<Unit>(Matches.enemyUnit(player, data));
		int totalLoss = 0;
		final boolean rollDiceForBlockadeDamage = games.strategy.triplea.Properties.getConvoyBlockadesRollDiceForCost(data);
		final Collection<String> transcripts = new ArrayList<String>();
		final HashMap<Territory, Tuple<Integer, List<Territory>>> damagePerBlockadeZone = new HashMap<Territory, Tuple<Integer, List<Territory>>>();
		for (final Territory b : blockable)
		{
			final List<Territory> viableNeighbors = Match.getMatches(map.getNeighbors(b),
						new CompositeMatchAnd<Territory>(Matches.isTerritoryOwnedBy(player), Matches.territoryCanCollectIncomeFrom(player, data)));
			final int maxLoss = getProduction(viableNeighbors);
			if (maxLoss <= 0)
				continue;
			int loss = 0;
			final Collection<Unit> enemies = Match.getMatches(b.getUnits().getUnits(), enemyUnits);
			if (rollDiceForBlockadeDamage)
			{
				int numberOfDice = 0;
				for (final Unit u : enemies)
				{
					numberOfDice += UnitAttachment.get(u.getType()).getBlockade();
				}
				if (numberOfDice > 0)
				{
					final String transcript = "Rolling for Convoy Blockade Damage in " + b.getName();
					final int[] dice = aBridge.getRandom(CONVOY_BLOCKADE_DICE_SIDES, numberOfDice, transcript);
					transcripts.add(transcript + ". Rolls: " + MyFormatter.asDice(dice));
					for (final int d : dice)
					{
						final int roll = d + 1; // we are zero based
						loss += (roll <= 3 ? roll : 0);
					}
				}
			}
			else
			{
				for (final Unit u : enemies)
				{
					loss += UnitAttachment.get(u.getType()).getBlockade();
				}
			}
			if (loss <= 0)
				continue;
			final int lossForBlockade = Math.min(maxLoss, loss);
			damagePerBlockadeZone.put(b, new Tuple<Integer, List<Territory>>(lossForBlockade, viableNeighbors));
			totalLoss += lossForBlockade;
		}
		if (totalLoss <= 0)
			return 0;
		// now we need to make sure that we didn't deal more damage than the territories are worth, in the case of having multiple sea zones touching the same land zone.
		final List<Territory> blockadeZonesSorted = new ArrayList<Territory>(damagePerBlockadeZone.keySet());
		Collections.sort(blockadeZonesSorted, getSingleBlockadeThenHighestToLowestBlockadeDamage(damagePerBlockadeZone));
		// we want to match highest damage to largest producer first, that is why we sort twice
		final IntegerMap<Territory> totalDamageTracker = new IntegerMap<Territory>();
		for (final Territory b : blockadeZonesSorted)
		{
			final Tuple<Integer, List<Territory>> tuple = damagePerBlockadeZone.get(b);
			int damageForZone = tuple.getFirst();
			final List<Territory> terrsLosingIncome = new ArrayList<Territory>(tuple.getSecond());
			Collections.sort(terrsLosingIncome, getSingleNeighborBlockadesThenHighestToLowestProduction(blockadeZonesSorted, map));
			final Iterator<Territory> iter = terrsLosingIncome.iterator();
			while (damageForZone > 0 && iter.hasNext())
			{
				final Territory t = iter.next();
				final int maxProductionLessPreviousDamage = TerritoryAttachment.get(t).getProduction() - totalDamageTracker.getInt(t);
				final int damageToTerr = Math.min(damageForZone, maxProductionLessPreviousDamage);
				damageForZone -= damageToTerr;
				totalDamageTracker.put(t, damageToTerr + totalDamageTracker.getInt(t));
			}
		}
		final int realTotalLoss = Math.max(0, totalDamageTracker.totalValues());
		if (rollDiceForBlockadeDamage && realTotalLoss > 0 && !transcripts.isEmpty())
		{
			aBridge.getHistoryWriter().startEvent("Total Cost from Convoy Blockades: " + realTotalLoss);
			for (final String t : transcripts)
			{
				aBridge.getHistoryWriter().addChildToEvent(t);
			}
		}
		return realTotalLoss;
	}
	
	public void setHasPostedTurnSummary(final boolean hasPostedTurnSummary)
	{
		m_hasPostedTurnSummary = hasPostedTurnSummary;
	}
	
	public boolean getHasPostedTurnSummary()
	{
		return m_hasPostedTurnSummary;
	}
	
	public boolean postTurnSummary(final PBEMMessagePoster poster, final String title)
	{
		m_hasPostedTurnSummary = poster.post(m_bridge.getHistoryWriter(), title);
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
	
	/*
	 * @see games.strategy.engine.delegate.IDelegate#getRemoteType()
	 */
	@Override
	public Class<? extends IRemote> getRemoteType()
	{
		return IAbstractEndTurnDelegate.class;
	}
	
	/*private static Comparator<Territory> getHighestToLowestProduction()
	{
		return new Comparator<Territory>()
		{
			public int compare(final Territory t1, final Territory t2)
			{
				if (t1 == t2 || t1.equals(t2) || (t1 == null && t2 == null))
					return 0;
				if (t1 == null)
					return 1;
				if (t2 == null)
					return -1;
				final TerritoryAttachment ta1 = TerritoryAttachment.get(t1);
				final TerritoryAttachment ta2 = TerritoryAttachment.get(t2);
				if (ta1 == null && ta2 == null)
					return 0;
				if (ta1 == null)
					return 1;
				if (ta2 == null)
					return -1;
				final int p1 = ta1.getProduction();
				final int p2 = ta2.getProduction();
				if (p1 == p2)
					return 0;
				if (p1 > p2)
					return -1;
				return 1;
			}
		};
	}*/

	private static Comparator<Territory> getSingleNeighborBlockadesThenHighestToLowestProduction(final Collection<Territory> blockadeZones, final GameMap map)
	{
		return new Comparator<Territory>()
		{
			public int compare(final Territory t1, final Territory t2)
			{
				if (t1 == t2 || t1.equals(t2) || (t1 == null && t2 == null))
					return 0;
				if (t1 == null)
					return 1;
				if (t2 == null)
					return -1;
				// if a territory is only touching 1 blockadeZone, we must take it first
				final Collection<Territory> neighborBlockades1 = new ArrayList<Territory>(map.getNeighbors(t1));
				neighborBlockades1.retainAll(blockadeZones);
				final int n1 = neighborBlockades1.size();
				final Collection<Territory> neighborBlockades2 = new ArrayList<Territory>(map.getNeighbors(t2));
				neighborBlockades2.retainAll(blockadeZones);
				final int n2 = neighborBlockades2.size();
				if (n1 == 1 && n2 != 1)
					return -1;
				if (n2 == 1 && n1 != 1)
					return 1;
				final TerritoryAttachment ta1 = TerritoryAttachment.get(t1);
				final TerritoryAttachment ta2 = TerritoryAttachment.get(t2);
				if (ta1 == null && ta2 == null)
					return 0;
				if (ta1 == null)
					return 1;
				if (ta2 == null)
					return -1;
				final int p1 = ta1.getProduction();
				final int p2 = ta2.getProduction();
				if (p1 == p2)
					return 0;
				if (p1 > p2)
					return -1;
				return 1;
			}
		};
	}
	
	private static Comparator<Territory> getSingleBlockadeThenHighestToLowestBlockadeDamage(final HashMap<Territory, Tuple<Integer, List<Territory>>> damagePerBlockadeZone)
	{
		return new Comparator<Territory>()
		{
			public int compare(final Territory t1, final Territory t2)
			{
				if (t1 == t2 || t1.equals(t2) || (t1 == null && t2 == null))
					return 0;
				if (t1 == null)
					return 1;
				if (t2 == null)
					return -1;
				final Tuple<Integer, List<Territory>> tuple1 = damagePerBlockadeZone.get(t1);
				final Tuple<Integer, List<Territory>> tuple2 = damagePerBlockadeZone.get(t2);
				final int num1 = tuple1.getSecond().size();
				final int num2 = tuple2.getSecond().size();
				if (num1 == 1 && num2 != 1)
					return -1;
				if (num2 == 1 && num1 != 1)
					return 1;
				final int d1 = tuple1.getFirst();
				final int d2 = tuple2.getFirst();
				if (d1 == d2)
					return 0;
				if (d1 > d2)
					return -1;
				return 1;
			}
		};
	}
}


class EndTurnExtendedDelegateState implements Serializable
{
	private static final long serialVersionUID = -3939461840835898284L;
	Serializable superState;
	// add other variables here:
	public boolean m_needToInitialize;
	public boolean m_hasPostedTurnSummary;
}
