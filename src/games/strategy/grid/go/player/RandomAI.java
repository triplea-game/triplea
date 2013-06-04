package games.strategy.grid.go.player;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.grid.go.delegate.PlayDelegate;
import games.strategy.grid.go.delegate.remote.IGoEndTurnDelegate;
import games.strategy.grid.go.delegate.remote.IGoPlayDelegate;
import games.strategy.grid.player.GridAbstractAI;
import games.strategy.grid.ui.GridEndTurnData;
import games.strategy.grid.ui.GridPlayData;
import games.strategy.grid.ui.IGridEndTurnData;
import games.strategy.grid.ui.IGridPlayData;
import games.strategy.util.Triple;
import games.strategy.util.Tuple;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

public class RandomAI extends GridAbstractAI
{
	public RandomAI(final String name, final String type)
	{
		super(name, type);
	}
	
	@Override
	protected void play()
	{
		final IGoPlayDelegate playDel = (IGoPlayDelegate) this.getPlayerBridge().getRemoteDelegate();
		final PlayerID me = getPlayerID();
		final GameData data = getGameData();
		// if (playDel.haveTwoPassedInARow())
		// return;
		
		pause();
		String error;
		final Triple<List<Territory>, List<Tuple<Territory, Collection<Territory>>>, List<Territory>> totalMoves;
		try
		{
			data.acquireReadLock();
			totalMoves = PlayDelegate.getAllValidMovesCaptureMovesAndInvalidMoves(me, data);
		} finally
		{
			data.releaseReadLock();
		}
		final List<Territory> validNonCaptureMoves = totalMoves.getFirst();
		final List<Tuple<Territory, Collection<Territory>>> validCaptureMoves = totalMoves.getSecond();
		if (!validCaptureMoves.isEmpty())
		{
			Collections.shuffle(validCaptureMoves);
			Collections.sort(validCaptureMoves, getBestCaptureComparator(getPlayerID(), getGameData()));
			final Iterator<Tuple<Territory, Collection<Territory>>> iter = validCaptureMoves.iterator();
			do
			{
				final IGridPlayData play = new GridPlayData(iter.next().getFirst(), me);
				error = playDel.play(play);
			} while (error != null && iter.hasNext());
			if (error == null)
				return;
		}
		// pass if there are very few moves left
		if (validNonCaptureMoves.size() > (2 + ((data.getMap().getXDimension() * data.getMap().getYDimension()) / 5)))
		{
			Collections.shuffle(validNonCaptureMoves);
			final Iterator<Territory> iter = validNonCaptureMoves.iterator();
			do
			{
				final IGridPlayData play = new GridPlayData(iter.next(), me);
				error = playDel.play(play);
			} while (error != null && iter.hasNext());
			if (error == null)
				return;
		}
		// pass
		final IGridPlayData pass = new GridPlayData(true, me);
		playDel.play(pass);
	}
	
	@Override
	protected void endTurn()
	{
		final IGoEndTurnDelegate endTurnDel = (IGoEndTurnDelegate) getPlayerBridge().getRemoteDelegate();
		// if (!endTurnDel.haveTwoPassedInARow())
		// return;
		pause();
		final IGridEndTurnData lastPlayersEndTurn = endTurnDel.getTerritoryAdjustment();
		if (lastPlayersEndTurn == null)
		{
			final IGridEndTurnData endPlay = new GridEndTurnData(new HashSet<Territory>(), false, getPlayerID());
			endTurnDel.territoryAdjustment(endPlay);
		}
		else
		{
			endTurnDel.territoryAdjustment(lastPlayersEndTurn);
		}
	}
	
	static Comparator<Tuple<Territory, Collection<Territory>>> getBestCaptureComparator(final PlayerID player, final GameData data)
	{
		return new Comparator<Tuple<Territory, Collection<Territory>>>()
		{
			public int compare(final Tuple<Territory, Collection<Territory>> t1, final Tuple<Territory, Collection<Territory>> t2)
			{
				if ((t1 == null && t2 == null) || t1 == t2)
					return 0;
				if (t1 == null && t2 != null)
					return 1;
				if (t1 != null && t2 == null)
					return -1;
				if (t1.equals(t2))
					return 0;
				final int points1 = t1.getSecond().size();
				final int points2 = t2.getSecond().size();
				if (points1 == points2)
					return 0;
				if (points1 > points2)
					return -1;
				return 1;
			}
		};
	}
}
