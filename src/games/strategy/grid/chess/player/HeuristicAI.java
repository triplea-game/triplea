package games.strategy.grid.chess.player;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.framework.GameDataUtils;
import games.strategy.grid.chess.delegate.EndTurnDelegate;
import games.strategy.grid.chess.delegate.PlayDelegate;
import games.strategy.grid.delegate.remote.IGridPlayDelegate;
import games.strategy.grid.player.GridAbstractAI;
import games.strategy.util.Quadruple;
import games.strategy.util.Triple;
import games.strategy.util.Tuple;

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
		// triple<start, end, captures>
		final List<Triple<Territory, Territory, Collection<Territory>>> availableMoves = getAllAvailableMoves(me, data, true);
		if (availableMoves.isEmpty())
		{
			System.err.println("No available moves for " + me.getName());
			return;
		}
		// get our delegate for playing
		final IGridPlayDelegate playDel = (IGridPlayDelegate) this.getPlayerBridge().getRemote();
		final Collection<PlayerID> enemies = data.getPlayerList().getPlayers();
		enemies.remove(me);
		final PlayerID enemy = enemies.iterator().next();
		final List<Triple<Territory, Territory, Integer>> movesWithPoints = new ArrayList<Triple<Territory, Territory, Integer>>();
		for (final Triple<Territory, Territory, Collection<Territory>> move1 : availableMoves)
		{
			final Triple<Territory, Territory, Integer> move = getMoveWithPoints(move1.getFirst(), move1.getSecond(), move1.getThird(), me, enemy, data, true);
			// if null, we do the move and return
			if (move == null)
			{
				doMove(move1.getFirst(), move1.getSecond(), data, playDel);
				return;
			}
			movesWithPoints.add(move);
		}
		Collections.sort(movesWithPoints, getBestPointsComparatorInt());
		final Triple<Territory, Territory, Integer> ourMove = movesWithPoints.iterator().next();
		doMove(ourMove.getFirst(), ourMove.getSecond(), data, playDel);
		return;
	}
	
	static Triple<Territory, Territory, Integer> getMoveWithPoints(final Territory start, final Territory end, final Collection<Territory> captures, final PlayerID me, final PlayerID enemy,
				final GameData data, final boolean returnEarlyIfWin)
	{
		final Quadruple<Territory, Territory, PlayerID, GameData> temp = PlayDelegate.copyGameDataAndAttemptMove(start, end, me, data);
		final PlayerID enemyTemp = (PlayerID) GameDataUtils.translateIntoOtherGameData(enemy, temp.getForth());
		// send only temp data (except captures, which is only one that can be temp or non-temp)
		final int points = getPointsForBoardSituation(temp.getThird(), enemyTemp, captures, temp.getForth(), true);
		// return null if we should do the move right away
		if (returnEarlyIfWin && points >= 1000000000)
			return null;
		return new Triple<Territory, Territory, Integer>(start, end, points);
	}
	
	static int getPointsForBoardSituation(final PlayerID me, final PlayerID enemy, final Collection<Territory> captures, final GameData data, final boolean returnEarlyIfWin)
	{
		int points = 0;
		// can we checkmate?
		if (EndTurnDelegate.doWeWin(me, data, 1))
		{
			if (returnEarlyIfWin)
				return 1000000000;
			else
				points += 1000000000;
		}
		// can check them?
		final boolean enemyInCheck = PlayDelegate.areWeInCheck(enemy, data, 1);
		points += (enemyInCheck ? 5 : 0);
		// can our piece be captured?
		// final boolean canRecaptureUs = !PlayDelegate.getMovesThatCaptureThisTerritory(temp.getSecond(), enemyTemp, temp.getForth(), 1, true).isEmpty();
		// points += (canRecaptureUs ? 0 : 1);
		final Collection<Tuple<Territory, List<Tuple<Territory, Territory>>>> capturedPieces = PlayDelegate.whichOfOurPiecesCanBeCaptured(me, data);
		for (final Tuple<Territory, List<Tuple<Territory, Territory>>> t : capturedPieces)
		{
			points -= getPointsForUnits(t.getFirst().getUnits().getUnits());
		}
		for (final Territory t : captures)
		{
			points += getPointsForUnits(t.getUnits().getUnits());
		}
		return points;
	}
	
	static List<Triple<Territory, Territory, Collection<Territory>>> getAllAvailableMoves(final PlayerID player, final GameData data, final boolean shuffle)
	{
		final List<Territory> allTerritories1 = new ArrayList<Territory>(data.getMap().getTerritories());
		final List<Territory> allTerritories2 = new ArrayList<Territory>(allTerritories1);
		if (shuffle)
		{
			Collections.shuffle(allTerritories1);
			Collections.shuffle(allTerritories2);
		}
		// triple<start, end, captures>
		final List<Triple<Territory, Territory, Collection<Territory>>> availableMoves = new ArrayList<Triple<Territory, Territory, Collection<Territory>>>();
		for (final Territory t1 : allTerritories1)
		{
			for (final Territory t2 : allTerritories2)
			{
				if (PlayDelegate.isValidPlay(t1, t2, player, data, 2) == null)
				{
					final Collection<Territory> captures = PlayDelegate.checkForCaptures(t1, t2, player, data);
					availableMoves.add(new Triple<Territory, Territory, Collection<Territory>>(t1, t2, captures));
				}
			}
		}
		return availableMoves;
	}
	
	static Comparator<Triple<Territory, Territory, Integer>> getBestPointsComparatorInt()
	{
		return new Comparator<Triple<Territory, Territory, Integer>>()
		{
			public int compare(final Triple<Territory, Territory, Integer> t1, final Triple<Territory, Territory, Integer> t2)
			{
				if ((t1 == null && t2 == null) || t1 == t2)
					return 0;
				if (t1 == null && t2 != null)
					return 1;
				if (t1 != null && t2 == null)
					return -1;
				if (t1.equals(t2))
					return 0;
				if (t1.getThird().intValue() == t2.getThird().intValue())
					return 0;
				if (t1.getThird().intValue() > t2.getThird().intValue())
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
				points += 22;
			else if (PlayDelegate.UnitIsRook.match(u))
				points += 41;
			else if (PlayDelegate.UnitIsBishop.match(u))
				points += 42;
			else if (PlayDelegate.UnitIsKnight.match(u))
				points += 43;
			else if (PlayDelegate.UnitIsQueen.match(u))
				points += 80;
			else if (PlayDelegate.UnitIsKing.match(u))
				points += 150;
		}
		return points;
	}
	
	static Comparator<Triple<Territory, Territory, Collection<Territory>>> getBestCaptureComparator(final PlayerID player, final GameData data)
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
	
	static final void doMove(final Territory start, final Territory end, final GameData data, final IGridPlayDelegate playDel)
	{
		String error;
		error = playDel.play(start, end);
		if (error != null)
		{
			System.err.println("Attempted Move Did Not Work: start: " + start.getName() + " end: " + end.getName());
			doRandomMove(data, playDel);
		}
	}
	
	static void doRandomMove(final GameData data, final IGridPlayDelegate playDel)
	{
		// Get the collection of territories from the map
		final Collection<Territory> territories = data.getMap().getTerritories();
		final Territory[] territoryArray = territories.toArray(new Territory[territories.size()]);
		final Random generator = new Random();
		int trymeStart;
		int trymeEnd;
		String error;
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
