package games.strategy.grid.checkers.delegate;

import games.strategy.common.delegate.AbstractDelegate;
import games.strategy.engine.data.Change;
import games.strategy.engine.data.ChangeFactory;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameMap;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.AutoSave;
import games.strategy.engine.message.IRemote;
import games.strategy.grid.delegate.remote.IGridPlayDelegate;
import games.strategy.grid.ui.GridPlayData;
import games.strategy.grid.ui.IGridPlayData;
import games.strategy.grid.ui.display.IGridGameDisplay;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.util.Match;

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
	public enum PIECES
	{
		PAWN, KING
	}
	
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
		final CheckersPlayExtendedDelegateState state = new CheckersPlayExtendedDelegateState();
		state.superState = super.saveState();
		// add other variables to state here:
		return state;
	}
	
	@Override
	public void loadState(final Serializable state)
	{
		final CheckersPlayExtendedDelegateState s = (CheckersPlayExtendedDelegateState) state;
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
		for (final Territory t : play.getAllSteps())
		{
			if (t.getUnits().getUnitCount() > 1)
				throw new IllegalStateException("Can not have more than 1 unit in any territory");
		}
		final String error = isValidPlay(play, m_player, getData(), true);
		if (error != null)
			return error;
		final Collection<Territory> captured = checkForCaptures(play, m_player, getData());
		performPlay(play, captured, m_player);
		for (final Territory t : play.getAllSteps())
		{
			if (t.getUnits().getUnitCount() > 1)
				throw new IllegalStateException("Can not have more than 1 unit in any territory");
		}
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
	public static String isValidPlay(final IGridPlayData play, final PlayerID player, final GameData data, final boolean checkForceCapture)
	{
		// System.out.println("Start: " + start.getX() + "," + start.getY() + "    End: " + end.getX() + "," + end.getY());
		final String basic = isValidMoveBasic(play, player, data);
		if (basic != null)
			return basic;
		
		final String pieceBasic = isValidPieceMoveBasic(play, player, data, checkForceCapture);
		if (pieceBasic != null)
			return pieceBasic;
		
		return null;
	}
	
	/**
	 * After a move completes, look to see if any captures occur.
	 * 
	 * @param end
	 *            <code>Territory</code> where the move ended. All potential captures must involve this <code>Territory</code>.
	 * @return
	 */
	public static Collection<Territory> checkForCaptures(final IGridPlayData play, final PlayerID player, final GameData data)
	{
		// assume it is a legal move
		final Collection<Territory> captured = new HashSet<Territory>();
		int lastY = play.getStart().getY();
		int lastX = play.getStart().getX();
		final int numSteps = play.getAllStepsExceptStart().size();
		// simple move
		if (numSteps == 1)
		{
			final int diffX = Math.abs(lastX - play.getEnd().getX());
			final int diffY = Math.abs(lastY - play.getEnd().getY());
			if (diffX == 1 && diffY == 1)
			{
				return captured;
			}
		}
		final GameMap map = data.getMap();
		for (final Territory t : play.getAllStepsExceptStart())
		{
			if (lastY < t.getY())
			{
				if (lastX < t.getX())
				{
					final Territory jumped = map.getTerritoryFromCoordinates(false, lastX + 1, lastY + 1);
					if (jumped.getUnits().someMatch(UnitIsOwnedBy(player).invert()))
						captured.add(jumped);
				}
				else
				{
					final Territory jumped = map.getTerritoryFromCoordinates(false, lastX - 1, lastY + 1);
					if (jumped.getUnits().someMatch(UnitIsOwnedBy(player).invert()))
						captured.add(jumped);
				}
			}
			else
			{
				if (lastX < t.getX())
				{
					final Territory jumped = map.getTerritoryFromCoordinates(false, lastX + 1, lastY - 1);
					if (jumped.getUnits().someMatch(UnitIsOwnedBy(player).invert()))
						captured.add(jumped);
				}
				else
				{
					final Territory jumped = map.getTerritoryFromCoordinates(false, lastX - 1, lastY - 1);
					if (jumped.getUnits().someMatch(UnitIsOwnedBy(player).invert()))
						captured.add(jumped);
				}
			}
			lastY = t.getY();
			lastX = t.getX();
		}
		return captured;
	}
	
	/*
	private IGridGamePlayer getRemotePlayer(final PlayerID id)
	{
		return (IGridGamePlayer) m_bridge.getRemote(id);
	}
	*/

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
		final Collection<Unit> units = play.getStart().getUnits().getUnits();
		final Collection<Unit> promotionUnits;
		if (isValidPawnPromotion(play, m_player, getData()))
		{
			promotionUnits = new ArrayList<Unit>();
			promotionUnits.add(getData().getUnitTypeList().getUnitType("king").create(player));
		}
		else
			promotionUnits = null;
		final String transcriptText = player.getName() + " moved " + MyFormatter.unitsToTextNoOwner(units) + " from " + play.getStart().getName() + " to " + play.getEnd().getName();
		m_bridge.getHistoryWriter().startEvent(transcriptText, units);
		final Change removeUnit = ChangeFactory.removeUnits(play.getStart(), units);
		final Change addUnit = ChangeFactory.addUnits(play.getEnd(), (promotionUnits == null ? units : promotionUnits));
		final CompositeChange change = new CompositeChange();
		change.add(removeUnit);
		change.add(addUnit);
		for (final Territory at : captured)
		{
			if (at != null)
			{
				final Collection<Unit> capturedUnits = at.getUnits().getUnits();
				if (!capturedUnits.isEmpty())
				{
					final Change capture = ChangeFactory.removeUnits(at, capturedUnits);
					change.add(capture);
				}
			}
		}
		final Collection<Territory> refresh = new HashSet<Territory>(play.getAllSteps());
		m_bridge.addChange(change);
		final IGridGameDisplay display = (IGridGameDisplay) m_bridge.getDisplayChannelBroadcaster();
		display.refreshTerritories(refresh);
		display.showGridPlayDataMove(play);
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
	public static final Match<Unit> UnitIsPawn = new Match<Unit>()
	{
		@Override
		public boolean match(final Unit unit)
		{
			return unit != null && unit.getType().getName().equalsIgnoreCase("pawn");
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
	
	public static String isValidMoveBasic(final IGridPlayData play, final PlayerID player, final GameData data)
	{
		if (play.getStart() == null || play.getEnd() == null)
			return "Can Not Move Off Board";
		final int even = (play.getStart().getX() + play.getStart().getY()) % 2;
		for (final Territory t : play.getAllStepsExceptStart())
		{
			if ((t.getX() + t.getY()) % 2 != even)
				return "Must Stay On Diagonal Tiles";
		}
		final Collection<Unit> units = play.getStart().getUnits().getUnits();
		if (units == null || units.isEmpty())
			return "No Piece Selected";
		if (play.getStart().equals(play.getEnd()))
			return "Must Move Piece To New Position";
		final Unit unit = units.iterator().next();
		if (!UnitIsOwnedBy(player).match(unit))
			return "You Do Not Own This Piece";
		for (final Territory t : play.getAllStepsExceptStart())
		{
			if (t.getUnits().getUnitCount() > 0)
				return "A Piece Is In That Position";
		}
		return null;
	}
	
	public static String isValidPieceMoveBasic(final IGridPlayData play, final PlayerID player, final GameData data, final boolean checkForceCapture)
	{
		final Collection<Unit> units = play.getStart().getUnits().getUnits();
		final Unit unit = units.iterator().next();
		if (UnitIsPawn.match(unit))
		{
			return isValidPawnMove(play, player, data, checkForceCapture);
		}
		else if (UnitIsKing.match(unit))
		{
			return isValidKingMove(play, player, data, checkForceCapture);
		}
		return "?? Unit";
	}
	
	public static Set<Territory> getAllTerritoriesOnMapWhichCanHaveUnits(final Territory start, final GameData data)
	{
		final Set<Territory> allTerritories = new HashSet<Territory>();
		final int even = (start.getX() + start.getY()) % 2;
		for (final Territory t : data.getMap().getTerritories())
		{
			if ((t.getX() + t.getY()) % 2 == even)
				allTerritories.add(t);
		}
		return allTerritories;
	}
	
	public static Set<GridPlayData> getAllValidMovesFromHere(final Territory start, final PlayerID player, final GameData data)
	{
		final Collection<Unit> units = start.getUnits().getMatches(UnitIsOwnedBy(player));
		if (units.isEmpty())
			return new HashSet<GridPlayData>();
		final Unit unit = units.iterator().next();
		PIECES piece;
		if (UnitIsPawn.match(unit))
			piece = PIECES.PAWN;
		else if (UnitIsKing.match(unit))
			piece = PIECES.KING;
		else
			return new HashSet<GridPlayData>();
		final boolean startsAtLowRank = PlayerBeginsAtLowestRank.match(player);
		final Set<GridPlayData> validMoves = new HashSet<GridPlayData>();
		return getAllValidMovesFromHere(validMoves, start, start, new ArrayList<Territory>(), piece, startsAtLowRank, player, data, true);
		
	}
	
	public static Set<GridPlayData> getAllValidMovesFromHere(final Set<GridPlayData> validMovesSoFar, final Territory originalStart, final Territory thisStart, final List<Territory> middleStepsSoFar,
				final PIECES unit, final boolean startsAtLowRank, final PlayerID player, final GameData data, final boolean checkInitialMove)
	{
		final GameMap map = data.getMap();
		// check 1 move ahead, then start checking 2 moves ahead each time
		if (unit == PIECES.PAWN)
		{
			if (checkInitialMove)
			{
				final int newY = thisStart.getY() + (startsAtLowRank ? 1 : -1);
				{
					final Territory left = map.getTerritoryFromCoordinates(thisStart.getX() - 1, newY);
					if (left != null)
					{
						final GridPlayData play = new GridPlayData(thisStart, left, player);
						if (isValidPlay(play, player, data, true) == null)
							validMovesSoFar.add(play);
					}
				}
				{
					final Territory right = map.getTerritoryFromCoordinates(thisStart.getX() + 1, newY);
					if (right != null)
					{
						final GridPlayData play = new GridPlayData(thisStart, right, player);
						if (isValidPlay(play, player, data, true) == null)
							validMovesSoFar.add(play);
					}
				}
			}
			final int newY = thisStart.getY() + (startsAtLowRank ? 2 : -2);
			{
				final Territory left = map.getTerritoryFromCoordinates(thisStart.getX() - 2, newY);
				if (left != null)
				{
					final GridPlayData play = new GridPlayData(originalStart, middleStepsSoFar, left, player);
					if (!validMovesSoFar.contains(play))
					{
						final boolean valid = isValidPlay(play, player, data, true) == null;
						if (valid)
							validMovesSoFar.add(play);
						if (valid || isValidPlay(play, player, data, false) == null)
						{
							final List<Territory> newMiddleSteps = new ArrayList<Territory>(middleStepsSoFar);
							newMiddleSteps.add(left);
							validMovesSoFar.addAll(getAllValidMovesFromHere(validMovesSoFar, originalStart, left, newMiddleSteps, unit, startsAtLowRank, player, data, false));
						}
					}
				}
			}
			{
				final Territory right = map.getTerritoryFromCoordinates(thisStart.getX() + 2, newY);
				if (right != null)
				{
					final GridPlayData play = new GridPlayData(originalStart, middleStepsSoFar, right, player);
					if (!validMovesSoFar.contains(play))
					{
						final boolean valid = isValidPlay(play, player, data, true) == null;
						if (valid)
							validMovesSoFar.add(play);
						if (valid || isValidPlay(play, player, data, false) == null)
						{
							final List<Territory> newMiddleSteps = new ArrayList<Territory>(middleStepsSoFar);
							newMiddleSteps.add(right);
							validMovesSoFar.addAll(getAllValidMovesFromHere(validMovesSoFar, originalStart, right, newMiddleSteps, unit, startsAtLowRank, player, data, false));
						}
					}
				}
			}
		}
		else if (unit == PIECES.KING)
		{
			if (checkInitialMove)
			{
				{
					final Territory leftUp = map.getTerritoryFromCoordinates(thisStart.getX() - 1, thisStart.getY() - 1);
					if (leftUp != null)
					{
						final GridPlayData play = new GridPlayData(thisStart, leftUp, player);
						if (isValidPlay(play, player, data, true) == null)
							validMovesSoFar.add(play);
					}
				}
				{
					final Territory rightUp = map.getTerritoryFromCoordinates(thisStart.getX() + 1, thisStart.getY() - 1);
					if (rightUp != null)
					{
						final GridPlayData play = new GridPlayData(thisStart, rightUp, player);
						if (isValidPlay(play, player, data, true) == null)
							validMovesSoFar.add(play);
					}
				}
				{
					final Territory leftDown = map.getTerritoryFromCoordinates(thisStart.getX() - 1, thisStart.getY() + 1);
					if (leftDown != null)
					{
						final GridPlayData play = new GridPlayData(thisStart, leftDown, player);
						if (isValidPlay(play, player, data, true) == null)
							validMovesSoFar.add(play);
					}
				}
				{
					final Territory rightDown = map.getTerritoryFromCoordinates(thisStart.getX() + 1, thisStart.getY() + 1);
					if (rightDown != null)
					{
						final GridPlayData play = new GridPlayData(thisStart, rightDown, player);
						if (isValidPlay(play, player, data, true) == null)
							validMovesSoFar.add(play);
					}
				}
			}
			{
				final Territory leftUp = map.getTerritoryFromCoordinates(thisStart.getX() - 2, thisStart.getY() - 2);
				if (leftUp != null)
				{
					final GridPlayData play = new GridPlayData(originalStart, middleStepsSoFar, leftUp, player);
					if (!validMovesSoFar.contains(play))
					{
						final boolean valid = isValidPlay(play, player, data, true) == null;
						if (valid)
							validMovesSoFar.add(play);
						if (valid || isValidPlay(play, player, data, false) == null)
						{
							final List<Territory> newMiddleSteps = new ArrayList<Territory>(middleStepsSoFar);
							newMiddleSteps.add(leftUp);
							validMovesSoFar.addAll(getAllValidMovesFromHere(validMovesSoFar, originalStart, leftUp, newMiddleSteps, unit, startsAtLowRank, player, data, false));
						}
					}
				}
			}
			{
				final Territory rightUp = map.getTerritoryFromCoordinates(thisStart.getX() + 2, thisStart.getY() - 2);
				if (rightUp != null)
				{
					final GridPlayData play = new GridPlayData(originalStart, middleStepsSoFar, rightUp, player);
					if (!validMovesSoFar.contains(play))
					{
						final boolean valid = isValidPlay(play, player, data, true) == null;
						if (valid)
							validMovesSoFar.add(play);
						if (valid || isValidPlay(play, player, data, false) == null)
						{
							final List<Territory> newMiddleSteps = new ArrayList<Territory>(middleStepsSoFar);
							newMiddleSteps.add(rightUp);
							validMovesSoFar.addAll(getAllValidMovesFromHere(validMovesSoFar, originalStart, rightUp, newMiddleSteps, unit, startsAtLowRank, player, data, false));
						}
					}
				}
			}
			{
				final Territory leftDown = map.getTerritoryFromCoordinates(thisStart.getX() - 2, thisStart.getY() + 2);
				if (leftDown != null)
				{
					final GridPlayData play = new GridPlayData(originalStart, middleStepsSoFar, leftDown, player);
					if (!validMovesSoFar.contains(play))
					{
						final boolean valid = isValidPlay(play, player, data, true) == null;
						if (valid)
							validMovesSoFar.add(play);
						if (valid || isValidPlay(play, player, data, false) == null)
						{
							final List<Territory> newMiddleSteps = new ArrayList<Territory>(middleStepsSoFar);
							newMiddleSteps.add(leftDown);
							validMovesSoFar.addAll(getAllValidMovesFromHere(validMovesSoFar, originalStart, leftDown, newMiddleSteps, unit, startsAtLowRank, player, data, false));
						}
					}
				}
			}
			{
				final Territory rightDown = map.getTerritoryFromCoordinates(thisStart.getX() + 2, thisStart.getY() + 2);
				if (rightDown != null)
				{
					final GridPlayData play = new GridPlayData(originalStart, middleStepsSoFar, rightDown, player);
					if (!validMovesSoFar.contains(play))
					{
						final boolean valid = isValidPlay(play, player, data, true) == null;
						if (valid)
							validMovesSoFar.add(play);
						if (valid || isValidPlay(play, player, data, false) == null)
						{
							final List<Territory> newMiddleSteps = new ArrayList<Territory>(middleStepsSoFar);
							newMiddleSteps.add(rightDown);
							validMovesSoFar.addAll(getAllValidMovesFromHere(validMovesSoFar, originalStart, rightDown, newMiddleSteps, unit, startsAtLowRank, player, data, false));
						}
					}
				}
			}
		}
		return validMovesSoFar;
	}
	
	public static String isValidPawnMove(final IGridPlayData play, final PlayerID player, final GameData data, final boolean forceCapture)
	{
		final boolean startsAtLowRank = PlayerBeginsAtLowestRank.match(player);
		// all territories should be further than their previous territory
		// all must skip over other units, or move forward to unoccupied territories
		int lastY = play.getStart().getY();
		int lastX = play.getStart().getX();
		final int numSteps = play.getAllStepsExceptStart().size();
		// simple move
		if (numSteps == 1)
		{
			final int diffX = Math.abs(lastX - play.getEnd().getX());
			final int diffY = Math.abs(lastY - play.getEnd().getY());
			if (diffX == 1 && diffY == 1)
			{
				// we already check in basic check that the square is empty, so no need to check for that again
				return null;
			}
		}
		final GameMap map = data.getMap();
		// jump move
		for (final Territory t : play.getAllStepsExceptStart())
		{
			if (startsAtLowRank)
			{
				// move forward
				if (lastY > t.getY())
					return "Pawns Must Move Forward Diagonally";
				// move left or right
				if (lastX == t.getX())
					return "Must Move Right Or Left";
				final int diffX = Math.abs(lastX - t.getX());
				final int diffY = Math.abs(lastY - t.getY());
				if (diffX != 2 || diffY != 2)
					return "Must Either Move A Single Space, Or Jump Over Pieces";
				if (lastX < t.getX())
				{
					final Territory jumped = map.getTerritoryFromCoordinates(false, lastX + 1, lastY + 1);
					if (jumped.getUnits().getUnitCount() < 1)
						return "Must Jump Over A Piece";
				}
				else
				{
					final Territory jumped = map.getTerritoryFromCoordinates(false, lastX - 1, lastY + 1);
					if (jumped.getUnits().getUnitCount() < 1)
						return "Must Jump Over A Piece";
				}
			}
			else
			{
				// move forward
				if (lastY < t.getY())
					return "Pawns Must Move Forward Diagonally";
				// move left or right
				if (lastX == t.getX())
					return "Must Move Right Or Left";
				final int diffX = Math.abs(lastX - t.getX());
				final int diffY = Math.abs(lastY - t.getY());
				if (diffX != 2 || diffY != 2)
					return "Must Either Move A Single Space, Or Jump Over Pieces";
				if (lastX < t.getX())
				{
					final Territory jumped = map.getTerritoryFromCoordinates(false, lastX + 1, lastY - 1);
					if (jumped.getUnits().getUnitCount() < 1)
						return "Must Jump Over A Piece";
				}
				else
				{
					final Territory jumped = map.getTerritoryFromCoordinates(false, lastX - 1, lastY - 1);
					if (jumped.getUnits().getUnitCount() < 1)
						return "Must Jump Over A Piece";
				}
			}
			lastY = t.getY();
			lastX = t.getX();
		}
		// TODO: must capture all pieces in a series
		return null;
	}
	
	public static boolean isValidPawnPromotion(final IGridPlayData play, final PlayerID player, final GameData data)
	{
		final boolean startsAtLowRank = PlayerBeginsAtLowestRank.match(player);
		if (!play.getStart().getUnits().someMatch(UnitIsPawn))
			return false;
		if (startsAtLowRank && play.getEnd().getY() == data.getMap().getYDimension() - 1)
			return true;
		if (!startsAtLowRank && play.getEnd().getY() == 0)
			return true;
		return false;
	}
	
	public static String isValidKingMove(final IGridPlayData play, final PlayerID player, final GameData data, final boolean forceCapture)
	{
		// all must skip over other units, or move forward to unoccupied territories
		int lastY = play.getStart().getY();
		int lastX = play.getStart().getX();
		final int numSteps = play.getAllStepsExceptStart().size();
		// simple move
		if (numSteps == 1)
		{
			final int diffX = Math.abs(lastX - play.getEnd().getX());
			final int diffY = Math.abs(lastY - play.getEnd().getY());
			if (diffX == 1 && diffY == 1)
			{
				// we already check in basic check that the square is empty, so no need to check for that again
				return null;
			}
		}
		final GameMap map = data.getMap();
		// jump move
		for (final Territory t : play.getAllStepsExceptStart())
		{
			if (lastY == t.getY())
			{
				return "Must Move Forward Or Backward Diagonally";
			}
			else if (lastY < t.getY())
			{
				// move left or right
				if (lastX == t.getX())
					return "Must Move Right Or Left";
				final int diffX = Math.abs(lastX - t.getX());
				final int diffY = Math.abs(lastY - t.getY());
				if (diffX != 2 || diffY != 2)
					return "Must Either Move A Single Space, Or Jump Over Pieces";
				if (lastX < t.getX())
				{
					final Territory jumped = map.getTerritoryFromCoordinates(false, lastX + 1, lastY + 1);
					if (jumped.getUnits().getUnitCount() < 1)
						return "Must Jump Over A Piece";
				}
				else
				{
					final Territory jumped = map.getTerritoryFromCoordinates(false, lastX - 1, lastY + 1);
					if (jumped.getUnits().getUnitCount() < 1)
						return "Must Jump Over A Piece";
				}
			}
			else
			{
				// move forward
				if (lastY >= t.getY())
					return "Pawns Must Move Forward Diagonally";
				// move left or right
				if (lastX == t.getX())
					return "Must Move Right Or Left";
				final int diffX = Math.abs(lastX - t.getX());
				final int diffY = Math.abs(lastY - t.getY());
				if (diffX != 2 || diffY != 2)
					return "Must Either Move A Single Space, Or Jump Over Pieces";
				if (lastX < t.getX())
				{
					final Territory jumped = map.getTerritoryFromCoordinates(false, lastX + 1, lastY - 1);
					if (jumped.getUnits().getUnitCount() < 1)
						return "Must Jump Over A Piece";
				}
				else
				{
					final Territory jumped = map.getTerritoryFromCoordinates(false, lastX - 1, lastY - 1);
					if (jumped.getUnits().getUnitCount() < 1)
						return "Must Jump Over A Piece";
				}
			}
			lastY = t.getY();
			lastX = t.getX();
		}
		return null;
	}
}


class CheckersPlayExtendedDelegateState implements Serializable
{
	private static final long serialVersionUID = -5446004854202452332L;
	Serializable superState;
	// add other variables here:
}
