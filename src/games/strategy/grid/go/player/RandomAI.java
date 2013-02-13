package games.strategy.grid.go.player;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.grid.delegate.remote.IGridPlayDelegate;
import games.strategy.grid.go.delegate.PlayDelegate;
import games.strategy.grid.player.GridAbstractAI;
import games.strategy.grid.ui.GridPlayData;
import games.strategy.grid.ui.IGridPlayData;
import games.strategy.util.Triple;
import games.strategy.util.Tuple;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
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
		final IGridPlayDelegate playDel = (IGridPlayDelegate) this.getPlayerBridge().getRemote();
		final PlayerID me = getPlayerID();
		final GameData data = getGameData();
		String error;
		final Triple<List<Territory>, List<Tuple<Territory, Collection<Territory>>>, List<Territory>> totalMoves = PlayDelegate.getAllValidMovesCaptureMovesAndInvalidMoves(me, data);
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
