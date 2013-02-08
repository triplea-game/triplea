package games.strategy.grid.chess.delegate;

import games.strategy.common.delegate.AbstractDelegate;
import games.strategy.engine.data.Change;
import games.strategy.engine.data.ChangeFactory;
import games.strategy.engine.data.ChangePerformer;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameMap;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.delegate.AutoSave;
import games.strategy.engine.framework.GameDataUtils;
import games.strategy.engine.message.IRemote;
import games.strategy.grid.chess.ChessUnit;
import games.strategy.grid.chess.attachments.PlayerAttachment;
import games.strategy.grid.delegate.remote.IGridPlayDelegate;
import games.strategy.grid.player.IGridGamePlayer;
import games.strategy.grid.ui.GridPlayData;
import games.strategy.grid.ui.IGridPlayData;
import games.strategy.grid.ui.display.IGridGameDisplay;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.Match;
import games.strategy.util.Quadruple;
import games.strategy.util.Tuple;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 
 * @author veqryn
 * 
 */
@AutoSave(beforeStepStart = false, afterStepEnd = true)
public class PlayDelegate extends AbstractDelegate implements IGridPlayDelegate
{
	/**
	 * Called before the delegate will run.
	 */
	@Override
	public void start()
	{
		super.start();
		final IGridGameDisplay display = (IGridGameDisplay) m_bridge.getDisplayChannelBroadcaster();
		display.setStatus(m_player.getName() + "'s turn");
	}
	
	@Override
	public void end()
	{
		super.end();
	}
	
	@Override
	public Serializable saveState()
	{
		final ChessPlayExtendedDelegateState state = new ChessPlayExtendedDelegateState();
		state.superState = super.saveState();
		// add other variables to state here:
		return state;
	}
	
	@Override
	public void loadState(final Serializable state)
	{
		final ChessPlayExtendedDelegateState s = (ChessPlayExtendedDelegateState) state;
		super.loadState(s.superState);
		// load other variables from state here:
	}
	
	public boolean stuffToDoInThisDelegate()
	{
		return true;
	}
	
	public void signalStatus(final String status)
	{
		final IGridGameDisplay display = (IGridGameDisplay) m_bridge.getDisplayChannelBroadcaster();
		display.setStatus(status);
	}
	
	public String play(final IGridPlayData play)
	{
		final Territory start = play.getStart();
		final Territory end = play.getEnd();
		if (start.getUnits().getUnitCount() > 1 || end.getUnits().getUnitCount() > 1)
			throw new IllegalStateException("Can not have more than 1 unit in any territory");
		final String error = isValidPlay(start, end, m_player, getData(), 2);
		if (error != null)
			return error;
		final Collection<Territory> captured = checkForCaptures(start, end, m_player, getData());
		performPlay(play, captured, m_player);
		if (start.getUnits().getUnitCount() > 1 || end.getUnits().getUnitCount() > 1)
			throw new IllegalStateException("Can not have more than 1 unit in any territory");
		return null;
	}
	
	/**
	 * Check to see if moving a piece from the start <code>Territory</code> to the end <code>Territory</code> is a valid play.
	 * 
	 * @param start
	 *            <code>Territory</code> where the move should start
	 * @param end
	 *            <code>Territory</code> where the move should end
	 */
	public static String isValidPlay(final Territory start, final Territory end, final PlayerID player, final GameData data, final int testForCheckTurnsAhead)
	{
		// System.out.println("Start: " + start.getX() + "," + start.getY() + "    End: " + end.getX() + "," + end.getY());
		final String basic = isValidMoveBasic(start, end, player, data);
		if (basic != null)
			return basic;
		
		final String pieceBasic = isValidPieceMoveBasic(start, end, player, data, testForCheckTurnsAhead - 1);
		if (pieceBasic != null)
			return pieceBasic;
		
		if (testForCheckTurnsAhead > 0)
		{
			if (testTerritoryForUsInCheckAfter(start, end, player, data, testForCheckTurnsAhead - 1))
				return "Illegal To Move Into Check Or Stay In Check";
		}
		
		return null;
	}
	
	/**
	 * After a move completes, look to see if any captures occur.
	 * 
	 * @param end
	 *            <code>Territory</code> where the move ended. All potential captures must involve this <code>Territory</code>.
	 * @return
	 */
	public static Collection<Territory> checkForCaptures(final Territory start, final Territory end, final PlayerID player, final GameData data)
	{
		// should only be able to capture 1 piece at most
		final Collection<Territory> captured = new HashSet<Territory>(1);
		// except for the weird pawn rule (En passant), the captured piece will always be on the end territory
		if (end.getUnits().getUnitCount() > 0)
			captured.add(end);
		// En passant
		if (start.getUnits().someMatch(UnitIsPawn) && isValidEnPassant(start, end, player, data) == null)
		{
			final boolean startsAtLowRank = PlayerBeginsAtLowestRank.match(player);
			final Territory territoryOfEnemyPawn = data.getMap().getTerritoryFromCoordinates(false, end.getX(), (startsAtLowRank ? end.getY() - 1 : end.getY() + 1));
			if (territoryOfEnemyPawn.getUnits().getUnitCount() > 0)
				captured.add(territoryOfEnemyPawn);
		}
		
		return captured;
	}
	
	private IGridGamePlayer getRemotePlayer(final PlayerID id)
	{
		return (IGridGamePlayer) m_bridge.getRemote(id);
	}
	
	/**
	 * Move a piece from the start <code>Territory</code> to the end <code>Territory</code>.
	 * 
	 * @param start
	 *            <code>Territory</code> where the move should start
	 * @param end
	 *            <code>Territory</code> where the move should end
	 */
	private void performPlay(final IGridPlayData play, final Collection<Territory> captured, final PlayerID player)
	{
		final Territory start = play.getStart();
		final Territory end = play.getEnd();
		final Collection<Unit> units = start.getUnits().getUnits();
		final Collection<Unit> promotionUnits;
		if (isValidPawnPromotion(start, end, m_player, getData()))
		{
			promotionUnits = new ArrayList<Unit>();
			final Set<UnitType> allowed = getData().getUnitTypeList().getAllUnitTypes();
			allowed.remove(getData().getUnitTypeList().getUnitType("king"));
			allowed.remove(getData().getUnitTypeList().getUnitType("pawn"));
			final UnitType selectedUnit = getRemotePlayer(player).selectUnit(units.iterator().next(), allowed, end, player, getData(),
						"Promote Pawn to what piece?");
			if (selectedUnit == null)
				promotionUnits.add(getData().getUnitTypeList().getUnitType("queen").create(player));
			else
				promotionUnits.add(getData().getUnitTypeList().getUnitType(selectedUnit.getName()).create(player));
		}
		else
			promotionUnits = null;
		m_bridge.getHistoryWriter().startEvent(play.toString(), units);
		final Change removeUnit = ChangeFactory.removeUnits(start, units);
		// final Change removeStartOwner = ChangeFactory.changeOwner(start, PlayerID.NULL_PLAYERID);
		final Change addUnit = ChangeFactory.addUnits(end, (promotionUnits == null ? units : promotionUnits));
		// final Change addEndOwner = ChangeFactory.changeOwner(end, player);
		final CompositeChange change = new CompositeChange();
		change.add(removeUnit);
		// change.add(removeStartOwner);
		change.add(addUnit);
		// change.add(addEndOwner);
		for (final Unit u : units)
		{
			final int numMoves = ((ChessUnit) u).getHasMoved();
			change.add(ChangeFactory.unitPropertyChange(u, numMoves + 1, ChessUnit.HAS_MOVED));
		}
		final Set<Unit> capturedUnitsTotal = new HashSet<Unit>();
		for (final Territory at : captured)
		{
			if (at != null)
			{
				final Collection<Unit> capturedUnits = at.getUnits().getUnits();
				if (!capturedUnits.isEmpty())
				{
					final Change capture = ChangeFactory.removeUnits(at, capturedUnits);
					change.add(capture);
					// final Change removeOwner = ChangeFactory.changeOwner(at, PlayerID.NULL_PLAYERID);
					// change.add(removeOwner);
					capturedUnitsTotal.addAll(capturedUnits);
				}
			}
		}
		if (!capturedUnitsTotal.isEmpty())
			m_bridge.getHistoryWriter().addChildToEvent(player.getName() + " captures units: " + MyFormatter.unitsToText(capturedUnitsTotal), capturedUnitsTotal);
		final Collection<Territory> refresh = new HashSet<Territory>();
		refresh.add(start);
		refresh.add(end);
		refresh.addAll(captured);
		final Collection<Unit> lastMovedPieces = new ArrayList<Unit>();
		lastMovedPieces.addAll(units);
		// castling
		if (start.getUnits().someMatch(UnitIsKing) && isValidKingCastling(start, end, player, getData(), 2) == null)
		{
			// find and move the rook
			final GameMap map = getData().getMap();
			final int lastColumn = map.getXDimension() - 1;
			if (end.getX() > start.getX())
			{
				final Territory rookTerOld = map.getTerritoryFromCoordinates(lastColumn, start.getY());
				final Territory rookTerNew = map.getTerritoryFromCoordinates(end.getX() - 1, start.getY());
				final List<Unit> rook = rookTerOld.getUnits().getMatches(new CompositeMatchAnd<Unit>(UnitIsRook, UnitIsOwnedBy(player), UnitHasNeverMovedBefore));
				final Change removeRook = ChangeFactory.removeUnits(rookTerOld, rook);
				final Change addRook = ChangeFactory.addUnits(rookTerNew, rook);
				change.add(removeRook);
				change.add(addRook);
				for (final Unit u : rook)
				{
					final int numMoves = ((ChessUnit) u).getHasMoved();
					change.add(ChangeFactory.unitPropertyChange(u, numMoves + 1, ChessUnit.HAS_MOVED));
				}
				refresh.add(rookTerOld);
				refresh.add(rookTerNew);
				lastMovedPieces.addAll(rook);
			}
			else
			{
				final Territory rookTerOld = map.getTerritoryFromCoordinates(0, start.getY());
				final Territory rookTerNew = map.getTerritoryFromCoordinates(end.getX() + 1, start.getY());
				final List<Unit> rook = rookTerOld.getUnits().getMatches(new CompositeMatchAnd<Unit>(UnitIsRook, UnitIsOwnedBy(player), UnitHasNeverMovedBefore));
				final Change removeRook = ChangeFactory.removeUnits(rookTerOld, rook);
				final Change addRook = ChangeFactory.addUnits(rookTerNew, rook);
				change.add(removeRook);
				change.add(addRook);
				for (final Unit u : rook)
				{
					final int numMoves = ((ChessUnit) u).getHasMoved();
					change.add(ChangeFactory.unitPropertyChange(u, numMoves + 1, ChessUnit.HAS_MOVED));
				}
				refresh.add(rookTerOld);
				refresh.add(rookTerNew);
				lastMovedPieces.addAll(rook);
			}
		}
		change.add(ChangeFactory.attachmentPropertyChange(PlayerAttachment.get(player), lastMovedPieces, PlayerAttachment.LAST_PIECES_MOVED));
		m_bridge.addChange(change);
		final IGridGameDisplay display = (IGridGameDisplay) m_bridge.getDisplayChannelBroadcaster();
		display.refreshTerritories(refresh);
		display.showGridPlayDataMove(new GridPlayData(start, end, player));
	}
	
	/**
	 * If this class implements an interface which inherits from IRemote, returns the class of that interface.
	 * Otherwise, returns null.
	 */
	@Override
	public Class<? extends IRemote> getRemoteType()
	{
		// This class implements IPlayDelegate, which inherits from IRemote.
		return IGridPlayDelegate.class;
	}
	
	public static Match<Territory> TerritoryHasUnitsOwnedBy(final PlayerID player)
	{
		final Match<Unit> unitOwnedBy = UnitIsOwnedBy(player);
		return new Match<Territory>()
		{
			@Override
			public boolean match(final Territory t)
			{
				return t.getUnits().someMatch(unitOwnedBy);
			}
		};
	}
	
	public static Match<Unit> UnitIsOwnedBy(final PlayerID player)
	{
		return new Match<Unit>()
		{
			@Override
			public boolean match(final Unit unit)
			{
				return unit.getOwner().equals(player);
			}
		};
	}
	
	public static final Match<PlayerID> PlayerBeginsAtLowestRank = new Match<PlayerID>()
	{
		@Override
		public boolean match(final PlayerID player)
		{
			return player != null && player.getName().equalsIgnoreCase("Black");
		}
	};
	public static final Match<Unit> UnitHasNeverMovedBefore = new Match<Unit>()
	{
		@Override
		public boolean match(final Unit unit)
		{
			return unit != null && ((ChessUnit) unit).getHasMoved() == 0;
		}
	};
	public static final Match<Unit> UnitHasOnlyMovedOnce = new Match<Unit>()
	{
		@Override
		public boolean match(final Unit unit)
		{
			return unit != null && ((ChessUnit) unit).getHasMoved() == 1;
		}
	};
	public static final Match<Unit> UnitIsPawn = new Match<Unit>()
	{
		@Override
		public boolean match(final Unit unit)
		{
			return unit != null && unit.getType().getName().equalsIgnoreCase("pawn");
		}
	};
	public static final Match<Unit> UnitIsKnight = new Match<Unit>()
	{
		@Override
		public boolean match(final Unit unit)
		{
			return unit != null && unit.getType().getName().equalsIgnoreCase("knight");
		}
	};
	public static final Match<Unit> UnitIsBishop = new Match<Unit>()
	{
		@Override
		public boolean match(final Unit unit)
		{
			return unit != null && unit.getType().getName().equalsIgnoreCase("bishop");
		}
	};
	public static final Match<Unit> UnitIsRook = new Match<Unit>()
	{
		@Override
		public boolean match(final Unit unit)
		{
			return unit != null && unit.getType().getName().equalsIgnoreCase("rook");
		}
	};
	public static final Match<Unit> UnitIsQueen = new Match<Unit>()
	{
		@Override
		public boolean match(final Unit unit)
		{
			return unit != null && unit.getType().getName().equalsIgnoreCase("queen");
		}
	};
	public static final Match<Unit> UnitIsKing = new Match<Unit>()
	{
		@Override
		public boolean match(final Unit unit)
		{
			return unit != null && unit.getType().getName().equalsIgnoreCase("king");
		}
	};
	
	public static boolean canWeMakeAValidMoveThatIsNotPuttingUsInCheck(final PlayerID player, final GameData data, final int testForCheckTurnsAhead)
	{
		final Collection<Territory> allTerritories = data.getMap().getTerritories();
		for (final Territory t1 : allTerritories)
		{
			for (final Territory t2 : allTerritories)
			{
				if (PlayDelegate.isValidPlay(t1, t2, player, data, testForCheckTurnsAhead) == null)
					return true;
			}
		}
		return false;
	}
	
	public static List<Tuple<Territory, Territory>> getMovesThatCaptureThisTerritory(final Territory end, final PlayerID player, final GameData data, final int testForCheckTurnsAhead,
				final boolean endAsSoonAsFindOne)
	{
		final List<Tuple<Territory, Territory>> available = new ArrayList<Tuple<Territory, Territory>>();
		final Collection<Territory> allTerritories = data.getMap().getTerritories();
		final Collection<Territory> allOur = Match.getMatches(allTerritories, TerritoryHasUnitsOwnedBy(player));
		for (final Territory t1 : allOur)
		{
			for (final Territory t2 : allTerritories)
			{
				final Collection<Territory> captured = PlayDelegate.checkForCaptures(t1, t2, player, data);
				if (captured.contains(end))
				{
					if (PlayDelegate.isValidPlay(t1, t2, player, data, testForCheckTurnsAhead) == null)
					{
						available.add(new Tuple<Territory, Territory>(t1, t2));
						if (endAsSoonAsFindOne)
							return available;
					}
				}
			}
		}
		return available;
	}
	
	/**
	 * How many of our pieces can be captured by the enemy?
	 */
	public static Collection<Tuple<Territory, List<Tuple<Territory, Territory>>>> whichOfOurPiecesCanBeCaptured(final PlayerID player, final GameData data)
	{
		final Collection<Tuple<Territory, List<Tuple<Territory, Territory>>>> capturedPieces = new ArrayList<Tuple<Territory, List<Tuple<Territory, Territory>>>>();
		// check if any of our opponents are in check
		final Collection<Territory> allOur = Match.getMatches(data.getMap().getTerritories(), TerritoryHasUnitsOwnedBy(player));
		for (final PlayerID enemy : data.getPlayerList().getPlayers())
		{
			if (enemy.equals(player))
				continue;
			for (final Territory t : allOur)
			{
				final List<Tuple<Territory, Territory>> captures = getMovesThatCaptureThisTerritory(t, enemy, data, 1, true);
				if (!captures.isEmpty())
					capturedPieces.add(new Tuple<Territory, List<Tuple<Territory, Territory>>>(t, captures));
			}
		}
		return capturedPieces;
	}
	
	/**
	 * If we move our piece from start to end, can an opponent capture it?
	 */
	public static boolean testTerritoryForEnemyCaptureUsAfter(final Territory start, final Territory end, final PlayerID player, final GameData data, final int testForCheckTurnsAhead)
	{
		final Quadruple<Territory, Territory, PlayerID, GameData> temp = copyGameDataAndAttemptMove(start, end, player, data);
		// final Territory newTempStart = temp.getFirst();
		final Territory newTempEnd = temp.getSecond();
		final PlayerID newTempPlayer = temp.getThird();
		final GameData newTempData = temp.getForth();
		
		// check if any of our opponents are in check
		for (final PlayerID enemy : newTempData.getPlayerList().getPlayers())
		{
			if (enemy.equals(newTempPlayer))
				continue;
			if (!getMovesThatCaptureThisTerritory(newTempEnd, enemy, newTempData, testForCheckTurnsAhead, true).isEmpty())
				return true;
		}
		return false;
	}
	
	/**
	 * If we move our piece from start to end, does that put any of our opponents into check?
	 */
	public static boolean testTerritoryForEnemyInCheckAfter(final Territory start, final Territory end, final PlayerID player, final GameData data, final int testForCheckTurnsAhead)
	{
		final Quadruple<Territory, Territory, PlayerID, GameData> temp = copyGameDataAndAttemptMove(start, end, player, data);
		// final Territory newTempStart = temp.getFirst();
		// final Territory newTempEnd = temp.getSecond();
		final PlayerID newTempPlayer = temp.getThird();
		final GameData newTempData = temp.getForth();
		
		// check if any of our opponents are in check
		for (final PlayerID enemy : newTempData.getPlayerList().getPlayers())
		{
			if (enemy.equals(newTempPlayer))
				continue;
			if (areWeInCheck(enemy, newTempData, testForCheckTurnsAhead))
				return true;
		}
		return false;
	}
	
	/**
	 * If we move our piece from start to end, does that put us in check?
	 */
	public static boolean testTerritoryForUsInCheckAfter(final Territory start, final Territory end, final PlayerID player, final GameData data, final int testForCheckTurnsAhead)
	{
		final Quadruple<Territory, Territory, PlayerID, GameData> temp = copyGameDataAndAttemptMove(start, end, player, data);
		// final Territory newTempStart = temp.getFirst();
		// final Territory newTempEnd = temp.getSecond();
		final PlayerID newTempPlayer = temp.getThird();
		final GameData newTempData = temp.getForth();
		if (EndTurnDelegate.doWeWin(newTempPlayer, newTempData, testForCheckTurnsAhead))
			return false;
		return areWeInCheck(newTempPlayer, newTempData, testForCheckTurnsAhead);
	}
	
	public static Quadruple<Territory, Territory, PlayerID, GameData> copyGameDataAndAttemptMove(final Territory start, final Territory end, final PlayerID player, final GameData data)
	{
		if (start == null || end == null || player == null || data == null)
			throw new IllegalArgumentException("copyGameDataAndAttemptMove can not accept null arguments");
		final Territory newTempStart;
		final Territory newTempEnd;
		final PlayerID newTempPlayer;
		final GameData newTempData;
		data.acquireReadLock();
		try
		{
			newTempData = GameDataUtils.cloneGameData(data, false);
			newTempStart = (Territory) GameDataUtils.translateIntoOtherGameData(start, newTempData);
			newTempEnd = (Territory) GameDataUtils.translateIntoOtherGameData(end, newTempData);
			newTempPlayer = (PlayerID) GameDataUtils.translateIntoOtherGameData(player, newTempData);
		} finally
		{
			data.releaseReadLock();
		}
		if (newTempData == null || newTempStart == null || newTempEnd == null || newTempPlayer == null)
			throw new IllegalStateException("Game Data translation did not work");
		// assume we have already checked the move is valid, and that any units in the end territory are captured/removed
		final ChangePerformer changePerformer = new ChangePerformer(newTempData);
		final Collection<Unit> unitsToMove = newTempStart.getUnits().getUnits();
		for (final Territory t : checkForCaptures(newTempStart, newTempEnd, newTempPlayer, newTempData))
		{
			changePerformer.perform(ChangeFactory.removeUnits(t, t.getUnits().getUnits()));
		}
		changePerformer.perform(ChangeFactory.removeUnits(newTempStart, unitsToMove));
		changePerformer.perform(ChangeFactory.addUnits(newTempEnd, unitsToMove));
		return new Quadruple<Territory, Territory, PlayerID, GameData>(newTempStart, newTempEnd, newTempPlayer, newTempData);
	}
	
	public static Collection<Territory> getKingTerritories(final PlayerID player, final GameData data)
	{
		// find where our king is
		final Collection<Territory> kingTerritories = new ArrayList<Territory>();
		for (final Territory t : data.getMap().getTerritories())
		{
			if (t.getUnits().someMatch(new CompositeMatchAnd<Unit>(UnitIsOwnedBy(player), UnitIsKing)))
			{
				kingTerritories.add(t);
			}
		}
		return kingTerritories;
	}
	
	public static boolean areWeInCheck(final PlayerID player, final GameData data, final int testForCheckTurnsAhead)
	{
		final Collection<Territory> kingTerritories = getKingTerritories(player, data);
		if (kingTerritories.isEmpty())
			return false;
		// check if any of our opponents can reach these territories
		for (final PlayerID enemy : data.getPlayerList().getPlayers())
		{
			if (enemy.equals(player))
				continue;
			for (final Territory kingT : kingTerritories)
			{
				if (canSomePieceMoveHere(kingT, enemy, data, testForCheckTurnsAhead))
					return true;
			}
		}
		return false;
	}
	
	public static boolean canSomePieceMoveHere(final Territory territory, final PlayerID player, final GameData data, final int testForCheckTurnsAhead)
	{
		for (final Territory t : data.getMap().getTerritories())
		{
			if (t.getUnits().someMatch(UnitIsOwnedBy(player)))
			{
				if (isValidPlay(t, territory, player, data, testForCheckTurnsAhead) == null)
					return true;
			}
		}
		return false;
	}
	
	public static String isValidMoveBasic(final Territory start, final Territory end, final PlayerID player, final GameData data)
	{
		if (start == null || end == null)
			return "Can Not Move Off Board";
		final Collection<Unit> units = start.getUnits().getUnits();
		if (units == null || units.isEmpty())
			return "No Piece Selected";
		if (start.equals(end))
			return "Must Move Piece To New Position";
		final Unit unit = units.iterator().next();
		if (!UnitIsOwnedBy(player).match(unit))
			return "You Do Not Own This Piece";
		if (end.getUnits().someMatch(UnitIsOwnedBy(player)))
			return "A Piece You Own Is In That Position";
		return null;
	}
	
	public static String isValidPieceMoveBasic(final Territory start, final Territory end, final PlayerID player, final GameData data, final int testForCheckTurnsAhead)
	{
		final Collection<Unit> units = start.getUnits().getUnits();
		final Unit unit = units.iterator().next();
		if (UnitIsPawn.match(unit))
		{
			return isValidPawnMove(start, end, player, data);
		}
		else if (UnitIsKnight.match(unit))
		{
			return isValidKnightMove(start, end, player, data);
		}
		else if (UnitIsBishop.match(unit))
		{
			return isValidBishopMove(start, end, player, data);
		}
		else if (UnitIsRook.match(unit))
		{
			return isValidRookMove(start, end, player, data);
		}
		else if (UnitIsQueen.match(unit))
		{
			return isValidQueenMove(start, end, player, data);
		}
		else if (UnitIsKing.match(unit))
		{
			return isValidKingMove(start, end, player, data, testForCheckTurnsAhead);
		}
		return "?? Unit";
	}
	
	public static String isValidPawnMove(final Territory start, final Territory end, final PlayerID player, final GameData data)
	{
		final GameMap map = data.getMap();
		final boolean startsAtLowRank = PlayerBeginsAtLowestRank.match(player);
		if (startsAtLowRank)
		{
			if (end.getY() - start.getY() == 1)
			{
				if (end.getX() == start.getX())
				{
					if (map.getTerritoryFromCoordinates(false, end.getX(), end.getY()).getUnits().getUnitCount() > 0)
						return "A Piece Is In The Way";
					return null;
				}
				else if (Math.abs(end.getX() - start.getX()) == 1)
				{
					if (map.getTerritoryFromCoordinates(false, end.getX(), end.getY()).getUnits().getUnitCount() == 0)
					{
						// en passant
						if (isValidEnPassant(start, end, player, data) != null)
							return "Must Capture A Piece To Move Diagonally";
					}
					return null;
				}
			}
			else if (end.getY() - start.getY() == 2 && end.getX() == start.getX() && start.getUnits().someMatch(UnitHasNeverMovedBefore))
			{
				if (map.getTerritoryFromCoordinates(false, end.getX(), end.getY() - 1).getUnits().getUnitCount() > 0)
					return "A Piece Is In The Way";
				if (map.getTerritoryFromCoordinates(false, end.getX(), end.getY()).getUnits().getUnitCount() > 0)
					return "A Piece Is In The Way";
				return null;
			}
		}
		else
		{
			if (start.getY() - end.getY() == 1)
			{
				if (end.getX() == start.getX())
				{
					if (map.getTerritoryFromCoordinates(false, end.getX(), end.getY()).getUnits().getUnitCount() > 0)
						return "A Piece Is In The Way";
					return null;
				}
				else if (Math.abs(end.getX() - start.getX()) == 1)
				{
					if (map.getTerritoryFromCoordinates(false, end.getX(), end.getY()).getUnits().getUnitCount() == 0)
					{
						// en passant
						if (isValidEnPassant(start, end, player, data) != null)
							return "Must Capture A Piece To Move Diagonally";
					}
					return null;
				}
			}
			else if (start.getY() - end.getY() == 2 && end.getX() == start.getX() && start.getUnits().someMatch(UnitHasNeverMovedBefore))
			{
				if (map.getTerritoryFromCoordinates(false, end.getX(), end.getY() + 1).getUnits().getUnitCount() > 0)
					return "A Piece Is In The Way";
				if (map.getTerritoryFromCoordinates(false, end.getX(), end.getY()).getUnits().getUnitCount() > 0)
					return "A Piece Is In The Way";
				return null;
			}
		}
		return "Invalid Move";
	}
	
	public static String isValidEnPassant(final Territory start, final Territory end, final PlayerID player, final GameData data)
	{
		final boolean startsAtLowRank = PlayerBeginsAtLowestRank.match(player);
		final int yLevelOfPawnShouldBe = (startsAtLowRank ? data.getMap().getYDimension() - 4 : 3);
		final int yLevelOfEnemyPawn = (startsAtLowRank ? end.getY() - 1 : end.getY() + 1);
		if (yLevelOfEnemyPawn != yLevelOfPawnShouldBe)
			return "Invalid Move";
		final Territory territoryOfEnemyPawn = data.getMap().getTerritoryFromCoordinates(false, end.getX(), yLevelOfEnemyPawn);
		if (!territoryOfEnemyPawn.getUnits().getMatches(new CompositeMatchAnd<Unit>(UnitIsPawn, UnitHasOnlyMovedOnce, UnitIsOwnedBy(player).invert())).isEmpty())
		{
			final Collection<Unit> lastMovedPiecesNotByCurrentPlayer = new ArrayList<Unit>();
			for (final PlayerID enemy : data.getPlayerList().getPlayers())
			{
				if (enemy.equals(player))
					continue;
				lastMovedPiecesNotByCurrentPlayer.addAll(PlayerAttachment.get(enemy).getLastPiecesMoved());
			}
			if (lastMovedPiecesNotByCurrentPlayer.containsAll(territoryOfEnemyPawn.getUnits().getMatches(UnitIsOwnedBy(player).invert())))
			{
				return null;
			}
		}
		return "Invalid Move";
	}
	
	public static boolean isValidPawnPromotion(final Territory start, final Territory end, final PlayerID player, final GameData data)
	{
		final boolean startsAtLowRank = PlayerBeginsAtLowestRank.match(player);
		if (!start.getUnits().someMatch(UnitIsPawn))
			return false;
		if (startsAtLowRank && end.getY() == data.getMap().getYDimension() - 1)
			return true;
		if (!startsAtLowRank && end.getY() == 0)
			return true;
		return false;
	}
	
	public static String isValidKnightMove(final Territory start, final Territory end, final PlayerID player, final GameData data)
	{
		if (end.getX() - 2 == start.getX())
		{
			if (end.getY() - 1 == start.getY() || end.getY() + 1 == start.getY())
				return null;
		}
		else if (end.getX() + 2 == start.getX())
		{
			if (end.getY() - 1 == start.getY() || end.getY() + 1 == start.getY())
				return null;
		}
		else if (end.getX() - 1 == start.getX())
		{
			if (end.getY() - 2 == start.getY() || end.getY() + 2 == start.getY())
				return null;
		}
		else if (end.getX() + 1 == start.getX())
		{
			if (end.getY() - 2 == start.getY() || end.getY() + 2 == start.getY())
				return null;
		}
		return "Invalid Move";
	}
	
	public static String isValidBishopMove(final Territory start, final Territory end, final PlayerID player, final GameData data)
	{
		final GameMap map = data.getMap();
		if (Math.abs(end.getX() - start.getX()) == Math.abs(end.getY() - start.getY()))
		{
			if (end.getX() > start.getX())
			{
				if (end.getY() > start.getY())
				{
					for (int i = 1; start.getX() + i < end.getX(); i++)
					{
						if (map.getTerritoryFromCoordinates(false, start.getX() + i, start.getY() + i).getUnits().getUnitCount() > 0)
							return "A Piece Is In The Way";
					}
					return null;
				}
				else
				{
					for (int i = 1; start.getX() + i < end.getX(); i++)
					{
						if (map.getTerritoryFromCoordinates(false, start.getX() + i, start.getY() - i).getUnits().getUnitCount() > 0)
							return "A Piece Is In The Way";
					}
					return null;
				}
			}
			else
			{
				if (end.getY() > start.getY())
				{
					for (int i = 1; start.getX() - i > end.getX(); i++)
					{
						if (map.getTerritoryFromCoordinates(false, start.getX() - i, start.getY() + i).getUnits().getUnitCount() > 0)
							return "A Piece Is In The Way";
					}
					return null;
				}
				else
				{
					for (int i = 1; start.getX() - i > end.getX(); i++)
					{
						if (map.getTerritoryFromCoordinates(false, start.getX() - i, start.getY() - i).getUnits().getUnitCount() > 0)
							return "A Piece Is In The Way";
					}
					return null;
				}
			}
		}
		return "Invalid Move";
	}
	
	public static String isValidRookMove(final Territory start, final Territory end, final PlayerID player, final GameData data)
	{
		final GameMap map = data.getMap();
		if (Math.abs(end.getX() - start.getX()) > 0 && Math.abs(end.getY() - start.getY()) == 0)
		{
			if (end.getX() > start.getX())
			{
				for (int i = 1; start.getX() + i < end.getX(); i++)
				{
					if (map.getTerritoryFromCoordinates(false, start.getX() + i, start.getY()).getUnits().getUnitCount() > 0)
						return "A Piece Is In The Way";
				}
				return null;
			}
			else
			{
				for (int i = 1; start.getX() - i > end.getX(); i++)
				{
					if (map.getTerritoryFromCoordinates(false, start.getX() - i, start.getY()).getUnits().getUnitCount() > 0)
						return "A Piece Is In The Way";
				}
				return null;
			}
		}
		else if (Math.abs(end.getX() - start.getX()) == 0 && Math.abs(end.getY() - start.getY()) > 0)
		{
			if (end.getY() > start.getY())
			{
				for (int i = 1; start.getY() + i < end.getY(); i++)
				{
					if (map.getTerritoryFromCoordinates(false, start.getX(), start.getY() + i).getUnits().getUnitCount() > 0)
						return "A Piece Is In The Way";
				}
				return null;
			}
			else
			{
				for (int i = 1; start.getY() - i > end.getY(); i++)
				{
					if (map.getTerritoryFromCoordinates(false, start.getX(), start.getY() - i).getUnits().getUnitCount() > 0)
						return "A Piece Is In The Way";
				}
				return null;
			}
		}
		return "Invalid Move";
	}
	
	public static String isValidQueenMove(final Territory start, final Territory end, final PlayerID player, final GameData data)
	{
		final GameMap map = data.getMap();
		if (Math.abs(end.getX() - start.getX()) == Math.abs(end.getY() - start.getY()))
		{
			if (end.getX() > start.getX())
			{
				if (end.getY() > start.getY())
				{
					for (int i = 1; start.getX() + i < end.getX(); i++)
					{
						if (map.getTerritoryFromCoordinates(false, start.getX() + i, start.getY() + i).getUnits().getUnitCount() > 0)
							return "A Piece Is In The Way";
					}
					return null;
				}
				else
				{
					for (int i = 1; start.getX() + i < end.getX(); i++)
					{
						if (map.getTerritoryFromCoordinates(false, start.getX() + i, start.getY() - i).getUnits().getUnitCount() > 0)
							return "A Piece Is In The Way";
					}
					return null;
				}
			}
			else
			{
				if (end.getY() > start.getY())
				{
					for (int i = 1; start.getX() - i > end.getX(); i++)
					{
						if (map.getTerritoryFromCoordinates(false, start.getX() - i, start.getY() + i).getUnits().getUnitCount() > 0)
							return "A Piece Is In The Way";
					}
					return null;
				}
				else
				{
					for (int i = 1; start.getX() - i > end.getX(); i++)
					{
						if (map.getTerritoryFromCoordinates(false, start.getX() - i, start.getY() - i).getUnits().getUnitCount() > 0)
							return "A Piece Is In The Way";
					}
					return null;
				}
			}
		}
		else if (Math.abs(end.getX() - start.getX()) > 0 && Math.abs(end.getY() - start.getY()) == 0)
		{
			if (end.getX() > start.getX())
			{
				for (int i = 1; start.getX() + i < end.getX(); i++)
				{
					if (map.getTerritoryFromCoordinates(false, start.getX() + i, start.getY()).getUnits().getUnitCount() > 0)
						return "A Piece Is In The Way";
				}
				return null;
			}
			else
			{
				for (int i = 1; start.getX() - i > end.getX(); i++)
				{
					if (map.getTerritoryFromCoordinates(false, start.getX() - i, start.getY()).getUnits().getUnitCount() > 0)
						return "A Piece Is In The Way";
				}
				return null;
			}
		}
		else if (Math.abs(end.getX() - start.getX()) == 0 && Math.abs(end.getY() - start.getY()) > 0)
		{
			if (end.getY() > start.getY())
			{
				for (int i = 1; start.getY() + i < end.getY(); i++)
				{
					if (map.getTerritoryFromCoordinates(false, start.getX(), start.getY() + i).getUnits().getUnitCount() > 0)
						return "A Piece Is In The Way";
				}
				return null;
			}
			else
			{
				for (int i = 1; start.getY() - i > end.getY(); i++)
				{
					if (map.getTerritoryFromCoordinates(false, start.getX(), start.getY() - i).getUnits().getUnitCount() > 0)
						return "A Piece Is In The Way";
				}
				return null;
			}
		}
		return "Invalid Move";
	}
	
	public static String isValidKingMove(final Territory start, final Territory end, final PlayerID player, final GameData data, final int testForCheckTurnsAhead)
	{
		if (Math.abs(end.getX() - start.getX()) <= 1 && Math.abs(end.getY() - start.getY()) <= 1)
			return null;
		return isValidKingCastling(start, end, player, data, testForCheckTurnsAhead);
	}
	
	public static String isValidKingCastling(final Territory start, final Territory end, final PlayerID player, final GameData data, final int testForCheckTurnsAhead)
	{
		// castling
		final Collection<Unit> units = start.getUnits().getUnits();
		final Unit unit = units.iterator().next();
		final GameMap map = data.getMap();
		if (start.getY() == end.getY() && UnitHasNeverMovedBefore.match(unit))
		{
			final int lastRow = map.getYDimension() - 1;
			final int lastColumn = map.getXDimension() - 1;
			if (start.getY() == 0 || start.getY() == lastRow)
			{
				if (Math.abs(start.getX() - end.getX()) == 2)
				{
					// The king may not currently be in check, nor may the king pass through or end up in a square that is under attack by an enemy piece (though the rook is permitted to be under attack and to pass over an attacked square)
					if (areWeInCheck(player, data, testForCheckTurnsAhead))
						return "May Not Castle While In Check";
					if (end.getX() > start.getX())
					{
						final List<Unit> rook = map.getTerritoryFromCoordinates(lastColumn, start.getY()).getUnits().getMatches(
												new CompositeMatchAnd<Unit>(UnitIsRook, UnitIsOwnedBy(player), UnitHasNeverMovedBefore));
						if (!rook.isEmpty())
						{
							for (int i = start.getX() + 1; i < lastColumn; i++)
							{
								if (map.getTerritoryFromCoordinates(false, i, start.getY()).getUnits().getUnitCount() > 0)
									return "Can Not Castle King With Pieces In The Way";
							}
							final Territory moveThroughTerritoryForKing = map.getTerritoryFromCoordinates(false, end.getX() - 1, start.getY());
							if (testTerritoryForUsInCheckAfter(start, moveThroughTerritoryForKing, player, data, testForCheckTurnsAhead))
								return "Illegal To Move Through Check To Castle";
							return null;
						}
					}
					else
					{
						final List<Unit> rook = map.getTerritoryFromCoordinates(0, start.getY()).getUnits().getMatches(
									new CompositeMatchAnd<Unit>(UnitIsRook, UnitIsOwnedBy(player), UnitHasNeverMovedBefore));
						if (!rook.isEmpty())
						{
							for (int i = 1; i < start.getX(); i++)
							{
								if (map.getTerritoryFromCoordinates(false, i, start.getY()).getUnits().getUnitCount() > 0)
									return "Can Not Castle King With Pieces In The Way";
							}
							final Territory moveThroughTerritoryForKing = map.getTerritoryFromCoordinates(false, end.getX() + 1, start.getY());
							if (testTerritoryForUsInCheckAfter(start, moveThroughTerritoryForKing, player, data, testForCheckTurnsAhead))
								return "Illegal To Move Through Check To Castle";
							return null;
						}
					}
				}
			}
		}
		return "Invalid Move";
	}
}


class ChessPlayExtendedDelegateState implements Serializable
{
	private static final long serialVersionUID = 2861231666229378075L;
	Serializable superState;
	// add other variables here:
}
