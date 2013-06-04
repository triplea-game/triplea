package games.strategy.grid.chess.player;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.framework.GameDataUtils;
import games.strategy.grid.chess.Chess;
import games.strategy.grid.chess.delegate.EndTurnDelegate;
import games.strategy.grid.chess.delegate.PlayDelegate;
import games.strategy.grid.delegate.remote.IGridPlayDelegate;
import games.strategy.util.Quadruple;
import games.strategy.util.Triple;
import games.strategy.util.Tuple;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A simple alpha-beta pruning AI.
 * 
 * @author veqryn
 * 
 */
public class AlphaBeta extends HeuristicAI
{
	// private static final double PRUNE_PERCENT = 0.5d; // remove this % of the worst nodes
	// private static final int SKIP_PRUNING_START_DEPTH = 1; // skip this number of turns before we start pruning
	// private static final int SEARCH_DEPTH = 3; // how many turns to search out
	
	public AlphaBeta(final String name, final String type)
	{
		super(name, type);
	}
	
	@Override
	protected void play()
	{
		pause();
		final PlayerID me = getPlayerID();
		final GameData data = getGameData();
		// get our delegate for playing
		final IGridPlayDelegate playDel = (IGridPlayDelegate) this.getPlayerBridge().getRemoteDelegate();
		final Collection<PlayerID> enemies = data.getPlayerList().getPlayers();
		enemies.remove(me);
		final PlayerID enemy = enemies.iterator().next();
		final int searchDepth = getAISearchDepthProperty(data);
		final List<Triple<Territory, Territory, Long>> movesWithPoints = getPointsForBoardSituationStartBranches(me, enemy, data, searchDepth);
		if (movesWithPoints.isEmpty())
		{
			System.err.println("No available moves for " + me.getName());
			return;
		}
		Collections.sort(movesWithPoints, getBestPointsComparatorLong());
		// printMovesSet(movesWithPoints);
		final Triple<Territory, Territory, Long> ourMove = movesWithPoints.iterator().next();
		doMove(ourMove.getFirst(), ourMove.getSecond(), data, playDel, me);
		return;
	}
	
	static List<Triple<Territory, Territory, Long>> getPointsForBoardSituationStartBranches(final PlayerID theAI, final PlayerID enemy, final GameData data, final int branchesLeftToDo)
	{
		final List<Thread> threads = new ArrayList<Thread>();
		final List<Triple<Territory, Territory, AtomicReference<Tuple<Long, Integer>>>> movesWithPointsReferences = new ArrayList<Triple<Territory, Territory, AtomicReference<Tuple<Long, Integer>>>>();
		final List<Triple<Territory, Territory, Collection<Territory>>> available = getAllAvailableMoves(theAI, data, true);
		for (final Triple<Territory, Territory, Collection<Territory>> move1 : available)
		{
			final Quadruple<Territory, Territory, PlayerID, GameData> temp = PlayDelegate.copyGameDataAndAttemptMove(move1.getFirst(), move1.getSecond(), theAI, data);
			final PlayerID theAITemp = (PlayerID) GameDataUtils.translateIntoOtherGameData(theAI, temp.getForth());
			final PlayerID currentEnemyTemp = (PlayerID) GameDataUtils.translateIntoOtherGameData(enemy, temp.getForth());
			final AtomicReference<Tuple<Long, Integer>> reference = new AtomicReference<Tuple<Long, Integer>>();
			movesWithPointsReferences.add(new Triple<Territory, Territory, AtomicReference<Tuple<Long, Integer>>>(move1.getFirst(), move1.getSecond(), reference));
			final Thread startBranches = new Thread(new Runnable()
			{
				public void run()
				{
					final Tuple<Long, Integer> pointsForThisMove = getPointsForBoardSituationBranch(theAITemp, currentEnemyTemp, theAITemp, temp.getForth(), branchesLeftToDo - 1);
					reference.set(pointsForThisMove);
				}
			});
			// System.out.println("Starting Thread: " + startBranches.getName());
			threads.add(startBranches);
			startBranches.start();
		}
		for (final Thread th : threads)
		{
			try
			{
				th.join();
				// System.out.println("Joining Thread: " + th.getName());
			} catch (final InterruptedException e)
			{
				e.printStackTrace();
			}
		}
		final List<Triple<Territory, Territory, Long>> movesWithPoints = new ArrayList<Triple<Territory, Territory, Long>>();
		for (final Triple<Territory, Territory, AtomicReference<Tuple<Long, Integer>>> ref : movesWithPointsReferences)
		{
			movesWithPoints.add(new Triple<Territory, Territory, Long>(ref.getFirst(), ref.getSecond(), (ref.getThird().get().getFirst() / ref.getThird().get().getSecond())));
		}
		return movesWithPoints;
	}
	
	static Tuple<Long, Integer> getPointsForBoardSituationBranch(final PlayerID theAI, final PlayerID currentPlayer, final PlayerID currentEnemy, final GameData data, final int branchesLeftToDo)
	{
		if (branchesLeftToDo > 0)
		{
			final List<Triple<Territory, Territory, Collection<Territory>>> available = getAllAvailableMoves(currentPlayer, data, true);
			if (available.isEmpty())
				return getPointsForBoardSituationBranch(theAI, currentPlayer, currentEnemy, data, -1);
			Tuple<Long, Integer> totalPointsForThisBoardSituation = new Tuple<Long, Integer>((long) 0, 0);
			for (final Triple<Territory, Territory, Collection<Territory>> move1 : available)
			{
				final Quadruple<Territory, Territory, PlayerID, GameData> temp = PlayDelegate.copyGameDataAndAttemptMove(move1.getFirst(), move1.getSecond(), currentPlayer, data);
				final PlayerID theAITemp = (PlayerID) GameDataUtils.translateIntoOtherGameData(theAI, temp.getForth());
				final PlayerID currentPlayerTemp = (PlayerID) GameDataUtils.translateIntoOtherGameData(currentPlayer, temp.getForth());
				final PlayerID currentEnemyTemp = (PlayerID) GameDataUtils.translateIntoOtherGameData(currentEnemy, temp.getForth());
				final Tuple<Long, Integer> totalForThisMove = getPointsForBoardSituationBranch(theAITemp, currentEnemyTemp, currentPlayerTemp, temp.getForth(), branchesLeftToDo - 1);
				totalPointsForThisBoardSituation = new Tuple<Long, Integer>(totalPointsForThisBoardSituation.getFirst() + totalForThisMove.getFirst(),
							totalPointsForThisBoardSituation.getSecond() + totalForThisMove.getSecond());
			}
			return totalPointsForThisBoardSituation;
		}
		// can we checkmate?
		if (EndTurnDelegate.doWeWin(theAI, data, 1))
			return new Tuple<Long, Integer>((400 + getPointsForBoardPieces(theAI, data)), 1);
		return new Tuple<Long, Integer>(getPointsForBoardSituationTotal(theAI, data), 1);
	}
	
	/*
	
	static List<Triple<Territory, Territory, Long>> getPointsForBoardSituationStartBranches(final PlayerID theAI, final PlayerID enemy, final GameData data, final int branchesLeftToDo)
	{
		final List<Triple<Territory, Territory, Long>> movesWithPoints = new ArrayList<Triple<Territory, Territory, Long>>();
		final List<Triple<Territory, Territory, Collection<Territory>>> available = getAllAvailableMoves(theAI, data, true);
		for (final Triple<Territory, Territory, Collection<Territory>> move1 : available)
		{
			final Quadruple<Territory, Territory, PlayerID, GameData> temp = PlayDelegate.copyGameDataAndAttemptMove(move1.getFirst(), move1.getSecond(), theAI, data);
			final PlayerID theAITemp = (PlayerID) GameDataUtils.translateIntoOtherGameData(theAI, temp.getForth());
			final PlayerID currentEnemyTemp = (PlayerID) GameDataUtils.translateIntoOtherGameData(enemy, temp.getForth());
			final Tuple<Long, Integer> pointsForThisMove = getPointsForBoardSituationBranch(theAITemp, currentEnemyTemp, theAITemp, temp.getForth(), branchesLeftToDo - 1);
			movesWithPoints.add(new Triple<Territory, Territory, Long>(move1.getFirst(), move1.getSecond(), (pointsForThisMove.getFirst() / pointsForThisMove.getSecond())));
		}
		return movesWithPoints;
	}
	
	static Tuple<Long, Integer> getPointsForBoardSituationBranch(final PlayerID theAI, final PlayerID currentPlayer, final PlayerID currentEnemy, final GameData data, final int branchesLeftToDo)
	{
		if (branchesLeftToDo > 0)
		{
			final List<Triple<Territory, Territory, Collection<Territory>>> available = getAllAvailableMoves(currentPlayer, data, true);
			Tuple<Long, Integer> totalPointsForThisBoardSituation = new Tuple<Long, Integer>((long) 0, available.size());
			for (final Triple<Territory, Territory, Collection<Territory>> move1 : available)
			{
				final Quadruple<Territory, Territory, PlayerID, GameData> temp = PlayDelegate.copyGameDataAndAttemptMove(move1.getFirst(), move1.getSecond(), currentPlayer, data);
				final PlayerID theAITemp = (PlayerID) GameDataUtils.translateIntoOtherGameData(theAI, temp.getForth());
				final PlayerID currentPlayerTemp = (PlayerID) GameDataUtils.translateIntoOtherGameData(currentPlayer, temp.getForth());
				final PlayerID currentEnemyTemp = (PlayerID) GameDataUtils.translateIntoOtherGameData(currentEnemy, temp.getForth());
				final Tuple<Long, Integer> totalForThisMove = getPointsForBoardSituationBranch(theAITemp, currentEnemyTemp, currentPlayerTemp, temp.getForth(), branchesLeftToDo - 1);
				totalPointsForThisBoardSituation = new Tuple<Long, Integer>(totalPointsForThisBoardSituation.getFirst() + totalForThisMove.getFirst(),
							totalPointsForThisBoardSituation.getSecond() + totalForThisMove.getSecond());
			}
			return totalPointsForThisBoardSituation;
		}
		// can we checkmate?
		if (EndTurnDelegate.doWeWin(theAI, data, 1))
			return new Tuple<Long, Integer>((400 + getPointsForBoardPieces(theAI, data)), 0);
		return new Tuple<Long, Integer>(getPointsForBoardSituationTotal(theAI, data), 0);
	}
	*/
	
	static long getPointsForBoardSituationTotal(final PlayerID theAI, final GameData data)
	{
		long points = 0;
		// can check them?
		for (final PlayerID enemy : data.getPlayerList().getPlayers())
		{
			if (enemy.equals(theAI))
				continue;
			final boolean enemyInCheck = PlayDelegate.areWeInCheck(enemy, data, 1);
			points += (enemyInCheck ? 5 : 0);
		}
		// can our piece be captured?
		final Collection<Tuple<Territory, List<Tuple<Territory, Territory>>>> capturedPieces = PlayDelegate.whichOfOurPiecesCanBeCaptured(theAI, data);
		for (final Tuple<Territory, List<Tuple<Territory, Territory>>> t : capturedPieces)
		{
			points -= getPointsForUnits(t.getFirst().getUnits().getUnits()) * 0.8;
		}
		points += getPointsForBoardPieces(theAI, data);
		return points;
	}
	
	static long getPointsForBoardPieces(final PlayerID theAI, final GameData data)
	{
		long points = 0;
		for (final Territory t : data.getMap().getTerritories())
		{
			final Collection<Unit> unitsOwnedByUs = t.getUnits().getMatches(PlayDelegate.UnitIsOwnedBy(theAI));
			final Collection<Unit> unitsNotOwnedByUs = t.getUnits().getMatches(PlayDelegate.UnitIsOwnedBy(theAI).invert());
			points += getPointsForUnits(unitsOwnedByUs);
			points -= getPointsForUnits(unitsNotOwnedByUs);
		}
		return points;
	}
	
	static Comparator<Triple<Territory, Territory, Long>> getBestPointsComparatorLong()
	{
		return new Comparator<Triple<Territory, Territory, Long>>()
		{
			public int compare(final Triple<Territory, Territory, Long> t1, final Triple<Territory, Territory, Long> t2)
			{
				if ((t1 == null && t2 == null) || t1 == t2)
					return 0;
				if (t1 == null && t2 != null)
					return 1;
				if (t1 != null && t2 == null)
					return -1;
				if (t1.equals(t2))
					return 0;
				if (t1.getThird().longValue() == t2.getThird().longValue())
					return 0;
				if (t1.getThird().longValue() > t2.getThird().longValue())
					return -1;
				return 1;
			}
		};
	}
	
	static void printMovesSet(final List<Triple<Territory, Territory, Long>> movesWithPoints)
	{
		System.out.println("\n\rMoves Available:");
		for (final Triple<Territory, Territory, Long> move1 : movesWithPoints)
		{
			System.out.println(move1.getFirst().getX() + "," + move1.getFirst().getY() + " -> " + move1.getSecond().getX() + "," + move1.getSecond().getY() + "  == " + move1.getThird());
		}
	}
	
	public static int getAISearchDepthProperty(final GameData data)
	{
		return data.getProperties().get(Chess.AI_SEARCH_DEPTH_PROPERTY, 3);
	}
}
