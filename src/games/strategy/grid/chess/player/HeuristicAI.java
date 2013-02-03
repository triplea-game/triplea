package games.strategy.grid.chess.player;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.grid.chess.delegate.PlayDelegate;
import games.strategy.grid.delegate.remote.IGridPlayDelegate;
import games.strategy.grid.player.GridAbstractAI;
import games.strategy.util.Triple;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

/**
 * A not good AI for Chess that uses some heuristics to be slightly better than random.
 * 
 * @author veqryn
 * 
 */
public class HeuristicAI extends GridAbstractAI
{
	public HeuristicAI(final String name, final String type)
	{
		super(name, type);
	}
	
	@Override
	protected void play()
	{
		// Unless the triplea.ai.pause system property is set to false,
		// pause for 0.8 seconds to give the impression of thinking
		pause();
		final PlayerID me = getPlayerID();
		final GameData data = getGameData();
		final List<Territory> allTerritories1 = new ArrayList<Territory>(data.getMap().getTerritories());
		final List<Territory> allTerritories2 = new ArrayList<Territory>(data.getMap().getTerritories());
		Collections.shuffle(allTerritories1);
		Collections.shuffle(allTerritories2);
		// triple<start, end, captures>
		final List<Triple<Territory, Territory, Collection<Territory>>> availableMoves = new ArrayList<Triple<Territory, Territory, Collection<Territory>>>();
		final List<Triple<Territory, Territory, Collection<Territory>>> availableCaptures = new ArrayList<Triple<Territory, Territory, Collection<Territory>>>();
		for (final Territory t1 : allTerritories1)
		{
			for (final Territory t2 : allTerritories2)
			{
				if (PlayDelegate.isValidPlay(t1, t2, me, data, 2) == null)
				{
					final Collection<Territory> captures = PlayDelegate.checkForCaptures(t1, t2, me, data);
					if (!captures.isEmpty())
						availableCaptures.add(new Triple<Territory, Territory, Collection<Territory>>(t1, t2, captures));
					availableMoves.add(new Triple<Territory, Territory, Collection<Territory>>(t1, t2, captures));
				}
			}
		}
		if (availableMoves.isEmpty())
			return;
		String error;
		final IGridPlayDelegate playDel = (IGridPlayDelegate) this.getPlayerBridge().getRemote();
		if (availableCaptures.isEmpty())
		{
			final Triple<Territory, Territory, Collection<Territory>> move = availableMoves.iterator().next();
			error = playDel.play(move.getFirst(), move.getSecond());
			if (error != null)
				doRandomMove();
		}
		else
		{
			Collections.sort(availableCaptures, getBestCaptureComparator(me, data));
			final Triple<Territory, Territory, Collection<Territory>> capture = availableCaptures.iterator().next();
			error = playDel.play(capture.getFirst(), capture.getSecond());
			if (error != null)
				doRandomMove();
		}
	}
	
	private Comparator<Triple<Territory, Territory, Collection<Territory>>> getBestCaptureComparator(final PlayerID player, final GameData data)
	{
		return new Comparator<Triple<Territory, Territory, Collection<Territory>>>()
		{
			public int compare(final Triple<Territory, Territory, Collection<Territory>> t1, final Triple<Territory, Territory, Collection<Territory>> t2)
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
				for (final Territory t : t1.getThird())
				{
					units1.addAll(t.getUnits().getUnits());
				}
				final Collection<Unit> units2 = new HashSet<Unit>();
				for (final Territory t : t2.getThird())
				{
					units2.addAll(t.getUnits().getUnits());
				}
				int points1 = 0;
				for (final Unit u : units1)
				{
					if (PlayDelegate.UnitIsPawn.match(u))
						points1 += 2;
					else if (PlayDelegate.UnitIsRook.match(u))
						points1 += 4;
					else if (PlayDelegate.UnitIsBishop.match(u))
						points1 += 4;
					else if (PlayDelegate.UnitIsKnight.match(u))
						points1 += 5;
					else if (PlayDelegate.UnitIsQueen.match(u))
						points1 += 8;
					else if (PlayDelegate.UnitIsKing.match(u))
						points1 += 100;
				}
				int points2 = 0;
				for (final Unit u : units2)
				{
					if (PlayDelegate.UnitIsPawn.match(u))
						points2 += 2;
					else if (PlayDelegate.UnitIsRook.match(u))
						points2 += 4;
					else if (PlayDelegate.UnitIsBishop.match(u))
						points2 += 4;
					else if (PlayDelegate.UnitIsKnight.match(u))
						points2 += 5;
					else if (PlayDelegate.UnitIsQueen.match(u))
						points2 += 8;
					else if (PlayDelegate.UnitIsKing.match(u))
						points2 += 100;
				}
				if (points1 == points2)
					return 0;
				if (points1 > points2)
					return -1;
				return 1;
			}
		};
	}
	
	private void doRandomMove()
	{
		// Get the collection of territories from the map
		final Collection<Territory> territories = getGameData().getMap().getTerritories();
		final Territory[] territoryArray = territories.toArray(new Territory[territories.size()]);
		final Random generator = new Random();
		int trymeStart;
		int trymeEnd;
		String error;
		// Get the play delegate
		final IGridPlayDelegate playDel = (IGridPlayDelegate) this.getPlayerBridge().getRemote();
		// Randomly select a territory and try playing there
		// If that play isn't legal, try again
		do
		{
			trymeStart = generator.nextInt(territoryArray.length);
			trymeEnd = generator.nextInt(territoryArray.length);
			error = playDel.play(territoryArray[trymeStart], territoryArray[trymeEnd]);
		} while (error != null);
	}
}
