package games.strategy.grid.checkers.player;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.grid.checkers.delegate.PlayDelegate;
import games.strategy.grid.delegate.remote.IGridPlayDelegate;
import games.strategy.grid.player.GridAbstractAI;
import games.strategy.grid.ui.GridPlayData;
import games.strategy.util.Tuple;

import java.util.ArrayList;
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
		// Unless the triplea.ai.pause system property is set to false,
		// pause for 0.8 seconds to give the impression of thinking
		pause();
		// Get the collection of territories from the map
		final GameData data = getGameData();
		final PlayerID me = getPlayerID();
		final List<Tuple<GridPlayData, Collection<Territory>>> validMoves = new ArrayList<Tuple<GridPlayData, Collection<Territory>>>();
		final List<GridPlayData> valid = PlayDelegate.getAllValidMoves(me, data);
		for (final GridPlayData v : valid)
		{
			final Collection<Territory> captures = PlayDelegate.checkForCaptures(v, me, data);
			validMoves.add(new Tuple<GridPlayData, Collection<Territory>>(v, captures));
		}
		Collections.shuffle(validMoves);
		Collections.sort(validMoves, getBestCaptureComparator(me, data));
		final IGridPlayDelegate playDel = (IGridPlayDelegate) this.getPlayerBridge().getRemoteDelegate();
		final Iterator<Tuple<GridPlayData, Collection<Territory>>> iter = validMoves.iterator();
		String error;
		do
		{
			error = playDel.play(iter.next().getFirst());
		} while (error != null && iter.hasNext());
	}
	
	static Comparator<Tuple<GridPlayData, Collection<Territory>>> getBestCaptureComparator(final PlayerID player, final GameData data)
	{
		return new Comparator<Tuple<GridPlayData, Collection<Territory>>>()
		{
			public int compare(final Tuple<GridPlayData, Collection<Territory>> t1, final Tuple<GridPlayData, Collection<Territory>> t2)
			{
				if ((t1 == null && t2 == null) || t1 == t2)
					return 0;
				if (t1 == null && t2 != null)
					return 1;
				if (t1 != null && t2 == null)
					return -1;
				if (t1.equals(t2))
					return 0;
				final Collection<Unit> units1 = new HashSet<Unit>();
				for (final Territory t : t1.getSecond())
				{
					units1.addAll(t.getUnits().getUnits());
				}
				final Collection<Unit> units2 = new HashSet<Unit>();
				for (final Territory t : t2.getSecond())
				{
					units2.addAll(t.getUnits().getUnits());
				}
				final int points1 = getPointsForUnits(units1);
				final int points2 = getPointsForUnits(units2);
				if (points1 == points2)
					return 0;
				if (points1 > points2)
					return -1;
				return 1;
			}
		};
	}
	
	static int getPointsForUnits(final Collection<Unit> capturedUnits)
	{
		int points = 0;
		for (final Unit u : capturedUnits)
		{
			if (PlayDelegate.UnitIsPawn.match(u))
				points += 5;
			else if (PlayDelegate.UnitIsKing.match(u))
				points += 9;
		}
		return points;
	}
}
