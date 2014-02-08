package games.strategy.triplea.delegate;

import games.strategy.common.delegate.BaseTripleADelegate;
import games.strategy.engine.data.ChangeFactory;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.message.IRemote;
import games.strategy.engine.random.IRandomStats.DiceType;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.IntegerMap;
import games.strategy.util.Match;
import games.strategy.util.Tuple;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This delegate sets up the game according to Risk rules, with a few allowed customizations.
 * Either divide all neutral territories between players randomly, or let them pick one by one.
 * After that, any remaining units get placed one by one.
 * (Note that m_player may not be used here, because this delegate is not run by any player [it is null])
 * 
 * @author veqryn (mark christopher duncan)
 * 
 */
public class RandomStartDelegate extends BaseTripleADelegate
{
	private static final int UNITS_PER_PICK = 1;
	protected PlayerID m_currentPickingPlayer = null;
	
	@Override
	public void start()
	{
		super.start();
		setupBoard();
	}
	
	/**
	 * Called before the delegate will stop running.
	 */
	@Override
	public void end()
	{
		super.end();
		m_currentPickingPlayer = null;
	}
	
	public boolean delegateCurrentlyRequiresUserInput()
	{
		if (Match.noneMatch(getData().getMap().getTerritories(), getTerritoryPickableMatch())
					&& Match.noneMatch(getData().getPlayerList().getPlayers(), getPlayerCanPickMatch()))
			return false;
		return true;
	}
	
	@Override
	public Serializable saveState()
	{
		final RandomStartExtendedDelegateState state = new RandomStartExtendedDelegateState();
		state.superState = super.saveState();
		// add other variables to state here:
		state.m_currentPickingPlayer = this.m_currentPickingPlayer;
		return state;
	}
	
	@Override
	public void loadState(final Serializable state)
	{
		final RandomStartExtendedDelegateState s = (RandomStartExtendedDelegateState) state;
		super.loadState(s.superState);
		// load other variables from state here:
		this.m_currentPickingPlayer = s.m_currentPickingPlayer;
	}
	
	protected void setupBoard()
	{
		final GameData data = getData();
		final boolean randomTerritories = games.strategy.triplea.Properties.getTerritoriesAreAssignedRandomly(data);
		final Match<Territory> pickableTerritoryMatch = getTerritoryPickableMatch();
		final Match<PlayerID> playerCanPickMatch = getPlayerCanPickMatch();
		final List<Territory> allPickableTerritories = Match.getMatches(data.getMap().getTerritories(), pickableTerritoryMatch);
		final List<PlayerID> playersCanPick = new ArrayList<PlayerID>();
		playersCanPick.addAll(Match.getMatches(data.getPlayerList().getPlayers(), playerCanPickMatch));
		// we need a main event
		if (!playersCanPick.isEmpty())
			m_bridge.getHistoryWriter().startEvent("Assigning Territories");
		// for random:
		final int[] hitRandom = (!randomTerritories ? new int[0] :
					m_bridge.getRandom(allPickableTerritories.size(), allPickableTerritories.size(), null, DiceType.ENGINE, "Picking random territories"));
		int i = 0;
		int pos = 0;
		// divvy up territories
		while (!allPickableTerritories.isEmpty() && !playersCanPick.isEmpty())
		{
			if (m_currentPickingPlayer == null || !playersCanPick.contains(m_currentPickingPlayer))
				m_currentPickingPlayer = playersCanPick.get(0);
			try
			{
				Thread.sleep(250);
			} catch (final InterruptedException e)
			{
			}
			
			Territory picked;
			if (randomTerritories)
			{
				pos += hitRandom[i];
				i++;
				final IntegerMap<UnitType> costs = BattleCalculator.getCostsForTUV(m_currentPickingPlayer, data);
				final List<Unit> units = new ArrayList<Unit>(m_currentPickingPlayer.getUnits().getUnits());
				Collections.sort(units, new UnitCostComparator(costs));
				final Set<Unit> unitsToPlace = new HashSet<Unit>();
				unitsToPlace.add(units.get(0));
				picked = allPickableTerritories.get(pos % allPickableTerritories.size());
				final CompositeChange change = new CompositeChange();
				change.add(ChangeFactory.changeOwner(picked, m_currentPickingPlayer));
				final Collection<Unit> factoryAndInfrastructure = Match.getMatches(unitsToPlace, Matches.UnitIsInfrastructure);
				if (!factoryAndInfrastructure.isEmpty())
					change.add(OriginalOwnerTracker.addOriginalOwnerChange(factoryAndInfrastructure, m_currentPickingPlayer));
				change.add(ChangeFactory.removeUnits(m_currentPickingPlayer, unitsToPlace));
				change.add(ChangeFactory.addUnits(picked, unitsToPlace));
				m_bridge.getHistoryWriter().addChildToEvent(
							m_currentPickingPlayer.getName() + " receives territory " + picked.getName() + " with units " + MyFormatter.unitsToTextNoOwner(unitsToPlace), picked);
				m_bridge.addChange(change);
			}
			else
			{
				Tuple<Territory, Set<Unit>> pick;
				Set<Unit> unitsToPlace;
				while (true)
				{
					pick = getRemotePlayer(m_currentPickingPlayer).pickTerritoryAndUnits(new ArrayList<Territory>(allPickableTerritories),
								new ArrayList<Unit>(m_currentPickingPlayer.getUnits().getUnits()), UNITS_PER_PICK);
					picked = pick.getFirst();
					unitsToPlace = pick.getSecond();
					if (!allPickableTerritories.contains(picked) || !m_currentPickingPlayer.getUnits().getUnits().containsAll(unitsToPlace) || unitsToPlace.size() > UNITS_PER_PICK
								|| (unitsToPlace.size() < UNITS_PER_PICK && unitsToPlace.size() < m_currentPickingPlayer.getUnits().getUnits().size()))
						getRemotePlayer(m_currentPickingPlayer).reportMessage("Chosen territory or units invalid!", "Chosen territory or units invalid!");
					else
						break;
				}
				final CompositeChange change = new CompositeChange();
				change.add(ChangeFactory.changeOwner(picked, m_currentPickingPlayer));
				final Collection<Unit> factoryAndInfrastructure = Match.getMatches(unitsToPlace, Matches.UnitIsInfrastructure);
				if (!factoryAndInfrastructure.isEmpty())
					change.add(OriginalOwnerTracker.addOriginalOwnerChange(factoryAndInfrastructure, m_currentPickingPlayer));
				change.add(ChangeFactory.removeUnits(m_currentPickingPlayer, unitsToPlace));
				change.add(ChangeFactory.addUnits(picked, unitsToPlace));
				m_bridge.getHistoryWriter().addChildToEvent(
							m_currentPickingPlayer.getName() + " picks territory " + picked.getName() + " and places in it " + MyFormatter.unitsToTextNoOwner(unitsToPlace), unitsToPlace);
				m_bridge.addChange(change);
			}
			allPickableTerritories.remove(picked);
			final PlayerID lastPlayer = m_currentPickingPlayer;
			m_currentPickingPlayer = getNextPlayer(playersCanPick, m_currentPickingPlayer);
			if (!playerCanPickMatch.match(lastPlayer))
				playersCanPick.remove(lastPlayer);
			if (playersCanPick.isEmpty())
				m_currentPickingPlayer = null;
		}
		// place any remaining units
		while (!playersCanPick.isEmpty())
		{
			if (m_currentPickingPlayer == null || !playersCanPick.contains(m_currentPickingPlayer))
				m_currentPickingPlayer = playersCanPick.get(0);
			
			final List<Territory> territoriesToPickFrom = data.getMap().getTerritoriesOwnedBy(m_currentPickingPlayer);
			Tuple<Territory, Set<Unit>> pick;
			Territory picked;
			Set<Unit> unitsToPlace;
			while (true)
			{
				pick = getRemotePlayer(m_currentPickingPlayer).pickTerritoryAndUnits(new ArrayList<Territory>(territoriesToPickFrom),
							new ArrayList<Unit>(m_currentPickingPlayer.getUnits().getUnits()), UNITS_PER_PICK);
				picked = pick.getFirst();
				unitsToPlace = pick.getSecond();
				if (!territoriesToPickFrom.contains(picked) || !m_currentPickingPlayer.getUnits().getUnits().containsAll(unitsToPlace) || unitsToPlace.size() > UNITS_PER_PICK
							|| (unitsToPlace.size() < UNITS_PER_PICK && unitsToPlace.size() < m_currentPickingPlayer.getUnits().getUnits().size()))
					getRemotePlayer(m_currentPickingPlayer).reportMessage("Chosen territory or units invalid!", "Chosen territory or units invalid!");
				else
					break;
			}
			final CompositeChange change = new CompositeChange();
			final Collection<Unit> factoryAndInfrastructure = Match.getMatches(unitsToPlace, Matches.UnitIsInfrastructure);
			if (!factoryAndInfrastructure.isEmpty())
				change.add(OriginalOwnerTracker.addOriginalOwnerChange(factoryAndInfrastructure, m_currentPickingPlayer));
			change.add(ChangeFactory.removeUnits(m_currentPickingPlayer, unitsToPlace));
			change.add(ChangeFactory.addUnits(picked, unitsToPlace));
			m_bridge.getHistoryWriter().addChildToEvent(m_currentPickingPlayer.getName() + " places " + MyFormatter.unitsToTextNoOwner(unitsToPlace) + " in territory " + picked.getName(),
						unitsToPlace);
			m_bridge.addChange(change);
			
			final PlayerID lastPlayer = m_currentPickingPlayer;
			m_currentPickingPlayer = getNextPlayer(playersCanPick, m_currentPickingPlayer);
			if (!playerCanPickMatch.match(lastPlayer))
				playersCanPick.remove(lastPlayer);
			if (playersCanPick.isEmpty())
				m_currentPickingPlayer = null;
		}
	}
	
	protected PlayerID getNextPlayer(final List<PlayerID> playersCanPick, final PlayerID currentPlayer)
	{
		int index = playersCanPick.indexOf(currentPlayer);
		if (index == -1)
			return null;
		index++;
		if (index >= playersCanPick.size())
			index = 0;
		return playersCanPick.get(index);
	}
	
	public Match<Territory> getTerritoryPickableMatch()
	{
		return new CompositeMatchAnd<Territory>(Matches.TerritoryIsLand, Matches.TerritoryIsNotImpassable, Matches.isTerritoryOwnedBy(PlayerID.NULL_PLAYERID), Matches.TerritoryIsEmpty);
	}
	
	public Match<PlayerID> getPlayerCanPickMatch()
	{
		return new Match<PlayerID>()
		{
			@Override
			public boolean match(final PlayerID player)
			{
				if (player == null || player.equals(PlayerID.NULL_PLAYERID))
					return false;
				if (player.getUnits().isEmpty())
					return false;
				if (player.getIsDisabled())
					return false;
				return true;
			}
		};
	}
	
	@Override
	public Class<? extends IRemote> getRemoteType()
	{
		return null;
	}
}


class RandomStartExtendedDelegateState implements Serializable
{
	private static final long serialVersionUID = 607794506772555083L;
	Serializable superState;
	// add other variables here:
	public PlayerID m_currentPickingPlayer;
}


class UnitCostComparator implements Comparator<Unit>
{
	private final IntegerMap<UnitType> m_costs;
	
	public UnitCostComparator(final IntegerMap<UnitType> costs)
	{
		m_costs = costs;
	}
	
	public UnitCostComparator(final PlayerID player, final GameData data)
	{
		m_costs = BattleCalculator.getCostsForTUV(player, data);
	}
	
	public int compare(final Unit u1, final Unit u2)
	{
		return m_costs.getInt(u1.getType()) - m_costs.getInt(u2.getType());
	}
}


class UnitTypeCostComparator implements Comparator<UnitType>
{
	private final IntegerMap<UnitType> m_costs;
	
	public UnitTypeCostComparator(final IntegerMap<UnitType> costs)
	{
		m_costs = costs;
	}
	
	public UnitTypeCostComparator(final PlayerID player, final GameData data)
	{
		m_costs = BattleCalculator.getCostsForTUV(player, data);
	}
	
	public int compare(final UnitType u1, final UnitType u2)
	{
		return m_costs.getInt(u1) - m_costs.getInt(u2);
	}
}
