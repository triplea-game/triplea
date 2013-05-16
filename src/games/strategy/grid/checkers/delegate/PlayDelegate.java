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
import games.strategy.util.Tuple;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
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
	public static String ALLOW_JUMPING_OVER_YOUR_OWN_PIECES = "Allow Jumping Over Your Own Pieces";
	public static String ALLOW_UNCROWNED_PIECES_TO_CAPTURE_BACKWARDS = "Allow Uncrowned Pieces To Capture Backwards";
	public static boolean ALLOW_JUMPING_SAME_PIECE_TWICE_IF_OWNED = false;
	
	
	public enum PIECES
	{
		PAWN, KING
	}
	
	public static boolean getPropertyAllowJumpingOverYourOwnPieces(final GameData data)
	{
		return data.getProperties().get(ALLOW_JUMPING_OVER_YOUR_OWN_PIECES, false);
	}
	
	public static boolean getPropertyAllowUncrownedPiecesToCaptureBackwards(final GameData data)
	{
		return data.getProperties().get(ALLOW_UNCROWNED_PIECES_TO_CAPTURE_BACKWARDS, false);
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
	
	public boolean delegateCurrentlyRequiresUserInput()
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
		final String error = isValidPlayOverall(play, m_player, getData());
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
	public static String isValidPlay(final IGridPlayData play, final PlayerID player, final GameData data, final boolean uncrownedCanCaptureBackwards, final boolean allowJumpingOwnPieces)
	{
		// System.out.println("Start: " + start.getX() + "," + start.getY() + "    End: " + end.getX() + "," + end.getY());
		final String basic = isValidMoveBasic(play, player, data);
		if (basic != null)
			return basic;
		
		final String pieceBasic = isValidPieceMoveBasic(play, player, data, uncrownedCanCaptureBackwards, allowJumpingOwnPieces);
		if (pieceBasic != null)
			return pieceBasic;
		
		final String formedLoop = isValidMoveNoLoops(play, player, data);
		if (formedLoop != null)
			return formedLoop;
		
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
		m_bridge.getHistoryWriter().startEvent(play.toString(), units);
		final Change removeUnit = ChangeFactory.removeUnits(play.getStart(), units);
		final Change addUnit = ChangeFactory.addUnits(play.getEnd(), (promotionUnits == null ? units : promotionUnits));
		final CompositeChange change = new CompositeChange();
		change.add(removeUnit);
		change.add(addUnit);
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
					capturedUnitsTotal.addAll(capturedUnits);
				}
			}
		}
		if (!capturedUnitsTotal.isEmpty())
			m_bridge.getHistoryWriter().addChildToEvent(player.getName() + " captures units: " + MyFormatter.unitsToText(capturedUnitsTotal), capturedUnitsTotal);
		final Collection<Territory> refresh = new HashSet<Territory>(play.getAllSteps());
		refresh.addAll(captured);
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
	public static final Match<Unit> UnitIsUnit = new Match<Unit>()
	{
		@Override
		public boolean match(final Unit unit)
		{
			return unit != null;
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
	
	public static String isValidPieceMoveBasic(final IGridPlayData play, final PlayerID player, final GameData data, final boolean uncrownedCanCaptureBackwards, final boolean allowJumpingOwnPieces)
	{
		final Collection<Unit> units = play.getStart().getUnits().getUnits();
		final Unit unit = units.iterator().next();
		if (UnitIsPawn.match(unit))
		{
			return isValidPawnMove(play, player, data, uncrownedCanCaptureBackwards, allowJumpingOwnPieces);
		}
		else if (UnitIsKing.match(unit))
		{
			return isValidKingMove(play, player, data, allowJumpingOwnPieces);
		}
		return "?? Unit";
	}
	
	public static boolean canNotMakeMoves(final PlayerID player, final GameData data)
	{
		Territory tWithUnits = null;
		for (final Territory t : data.getMap().getTerritories())
		{
			if (t.getUnits().getUnitCount() > 0)
			{
				tWithUnits = t;
				break;
			}
		}
		final Collection<Territory> allTerritories = PlayDelegate.getAllTerritoriesOnMapWhichCanHaveUnits(tWithUnits, data);
		final Collection<Territory> myTerritories = Match.getMatches(allTerritories, PlayDelegate.TerritoryHasUnitsOwnedBy(player));
		for (final Territory t : myTerritories)
		{
			if (!PlayDelegate.getAllValidMovesFromHere(t, player, data).isEmpty())
				return false;
		}
		return true;
	}
	
	public static String isValidPlayOverall(final IGridPlayData play, final PlayerID player, final GameData data)
	{
		final String validNormal = isValidPlay(play, player, data, getPropertyAllowUncrownedPiecesToCaptureBackwards(data), getPropertyAllowJumpingOverYourOwnPieces(data));
		if (validNormal != null)
			return validNormal;
		if (getAllValidMoves(player, data).contains(play))
			return null;
		else
			return "Must Capture Pieces Or Finish Sequence";
	}
	
	public static List<GridPlayData> getAllValidMoves(final PlayerID player, final GameData data)
	{
		Territory tWithUnits = null;
		for (final Territory t : data.getMap().getTerritories())
		{
			if (t.getUnits().getUnitCount() > 0)
			{
				tWithUnits = t;
				break;
			}
		}
		final Collection<Territory> allTerritories = PlayDelegate.getAllTerritoriesOnMapWhichCanHaveUnits(tWithUnits, data);
		final Collection<Territory> myTerritories = Match.getMatches(allTerritories, PlayDelegate.TerritoryHasUnitsOwnedBy(player));
		final List<GridPlayData> validMoves = new ArrayList<GridPlayData>();
		for (final Territory t : myTerritories)
		{
			validMoves.addAll(PlayDelegate.getAllValidMovesFromHere(t, player, data));
		}
		// And here is where we check to make sure that the player is taking a capture, if they can, or capturing all the way to the end of a sequence
		final List<GridPlayData> captureMoves = Match.getMatches(validMoves, GridPlayDataCaptures(data));
		if (captureMoves.isEmpty())
			return validMoves;
		return PlayDelegate.getPlaysWithShortVersionsRemoved(captureMoves, player, data);
	}
	
	public static Match<GridPlayData> GridPlayDataCaptures(final GameData data)
	{
		return new Match<GridPlayData>()
		{
			@Override
			public boolean match(final GridPlayData play)
			{
				return play != null && !PlayDelegate.checkForCaptures(play, play.getPlayerID(), data).isEmpty();
			}
		};
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
		return getAllValidMovesFromHere(validMoves, new HashSet<GridPlayData>(), start, start, new ArrayList<Territory>(), piece, startsAtLowRank, player, data, true,
					getPropertyAllowUncrownedPiecesToCaptureBackwards(data), getPropertyAllowJumpingOverYourOwnPieces(data));
		
	}
	
	public static Set<GridPlayData> getAllValidMovesFromHere(final Set<GridPlayData> validMovesSoFar, final Set<GridPlayData> triedMovesSoFar, final Territory originalStart,
				final Territory thisStart, final List<Territory> middleStepsSoFar, final PIECES unit, final boolean startsAtLowRank, final PlayerID player, final GameData data,
				final boolean checkInitialMove, final boolean uncrownedCanCaptureBackwards, final boolean allowJumpingOwnPieces)
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
						if (isValidPlay(play, player, data, uncrownedCanCaptureBackwards, allowJumpingOwnPieces) == null)
							validMovesSoFar.add(play);
					}
				}
				{
					final Territory right = map.getTerritoryFromCoordinates(thisStart.getX() + 1, newY);
					if (right != null)
					{
						final GridPlayData play = new GridPlayData(thisStart, right, player);
						if (isValidPlay(play, player, data, uncrownedCanCaptureBackwards, allowJumpingOwnPieces) == null)
							validMovesSoFar.add(play);
					}
				}
			}
			final int newY = thisStart.getY() + (startsAtLowRank ? 2 : -2);
			final int otherY = thisStart.getY() + (startsAtLowRank ? -2 : 2);
			{
				final Territory left = map.getTerritoryFromCoordinates(thisStart.getX() - 2, newY);
				if (left != null)
				{
					final GridPlayData play = new GridPlayData(originalStart, middleStepsSoFar, left, player);
					if (!triedMovesSoFar.contains(play))
					{
						final boolean valid = isValidPlay(play, player, data, uncrownedCanCaptureBackwards, allowJumpingOwnPieces) == null;
						if (valid)
						{
							validMovesSoFar.add(play);
							final List<Territory> newMiddleSteps = new ArrayList<Territory>(middleStepsSoFar);
							newMiddleSteps.add(left);
							triedMovesSoFar.add(play);
							validMovesSoFar.addAll(getAllValidMovesFromHere(validMovesSoFar, triedMovesSoFar, originalStart, left, newMiddleSteps, unit, startsAtLowRank, player, data, false,
										uncrownedCanCaptureBackwards, allowJumpingOwnPieces));
						}
					}
				}
			}
			{
				final Territory right = map.getTerritoryFromCoordinates(thisStart.getX() + 2, newY);
				if (right != null)
				{
					final GridPlayData play = new GridPlayData(originalStart, middleStepsSoFar, right, player);
					if (!triedMovesSoFar.contains(play))
					{
						final boolean valid = isValidPlay(play, player, data, uncrownedCanCaptureBackwards, allowJumpingOwnPieces) == null;
						if (valid)
						{
							validMovesSoFar.add(play);
							final List<Territory> newMiddleSteps = new ArrayList<Territory>(middleStepsSoFar);
							newMiddleSteps.add(right);
							triedMovesSoFar.add(play);
							validMovesSoFar.addAll(getAllValidMovesFromHere(validMovesSoFar, triedMovesSoFar, originalStart, right, newMiddleSteps, unit, startsAtLowRank, player, data, false,
										uncrownedCanCaptureBackwards, allowJumpingOwnPieces));
						}
					}
				}
			}
			if (uncrownedCanCaptureBackwards)
			{
				{
					final Territory left = map.getTerritoryFromCoordinates(thisStart.getX() - 2, otherY);
					if (left != null)
					{
						final GridPlayData play = new GridPlayData(originalStart, middleStepsSoFar, left, player);
						if (!triedMovesSoFar.contains(play))
						{
							final boolean valid = isValidPlay(play, player, data, uncrownedCanCaptureBackwards, allowJumpingOwnPieces) == null;
							if (valid)
							{
								validMovesSoFar.add(play);
								final List<Territory> newMiddleSteps = new ArrayList<Territory>(middleStepsSoFar);
								newMiddleSteps.add(left);
								triedMovesSoFar.add(play);
								validMovesSoFar.addAll(getAllValidMovesFromHere(validMovesSoFar, triedMovesSoFar, originalStart, left, newMiddleSteps, unit, startsAtLowRank, player, data, false,
											uncrownedCanCaptureBackwards, allowJumpingOwnPieces));
							}
						}
					}
				}
				{
					final Territory right = map.getTerritoryFromCoordinates(thisStart.getX() + 2, otherY);
					if (right != null)
					{
						final GridPlayData play = new GridPlayData(originalStart, middleStepsSoFar, right, player);
						if (!triedMovesSoFar.contains(play))
						{
							final boolean valid = isValidPlay(play, player, data, uncrownedCanCaptureBackwards, allowJumpingOwnPieces) == null;
							if (valid)
							{
								validMovesSoFar.add(play);
								final List<Territory> newMiddleSteps = new ArrayList<Territory>(middleStepsSoFar);
								newMiddleSteps.add(right);
								triedMovesSoFar.add(play);
								validMovesSoFar.addAll(getAllValidMovesFromHere(validMovesSoFar, triedMovesSoFar, originalStart, right, newMiddleSteps, unit, startsAtLowRank, player, data, false,
											uncrownedCanCaptureBackwards, allowJumpingOwnPieces));
							}
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
						if (isValidPlay(play, player, data, uncrownedCanCaptureBackwards, allowJumpingOwnPieces) == null)
							validMovesSoFar.add(play);
					}
				}
				{
					final Territory rightUp = map.getTerritoryFromCoordinates(thisStart.getX() + 1, thisStart.getY() - 1);
					if (rightUp != null)
					{
						final GridPlayData play = new GridPlayData(thisStart, rightUp, player);
						if (isValidPlay(play, player, data, uncrownedCanCaptureBackwards, allowJumpingOwnPieces) == null)
							validMovesSoFar.add(play);
					}
				}
				{
					final Territory leftDown = map.getTerritoryFromCoordinates(thisStart.getX() - 1, thisStart.getY() + 1);
					if (leftDown != null)
					{
						final GridPlayData play = new GridPlayData(thisStart, leftDown, player);
						if (isValidPlay(play, player, data, uncrownedCanCaptureBackwards, allowJumpingOwnPieces) == null)
							validMovesSoFar.add(play);
					}
				}
				{
					final Territory rightDown = map.getTerritoryFromCoordinates(thisStart.getX() + 1, thisStart.getY() + 1);
					if (rightDown != null)
					{
						final GridPlayData play = new GridPlayData(thisStart, rightDown, player);
						if (isValidPlay(play, player, data, uncrownedCanCaptureBackwards, allowJumpingOwnPieces) == null)
							validMovesSoFar.add(play);
					}
				}
			}
			{
				final Territory leftUp = map.getTerritoryFromCoordinates(thisStart.getX() - 2, thisStart.getY() - 2);
				if (leftUp != null)
				{
					final GridPlayData play = new GridPlayData(originalStart, middleStepsSoFar, leftUp, player);
					if (!triedMovesSoFar.contains(play))
					{
						final boolean valid = isValidPlay(play, player, data, uncrownedCanCaptureBackwards, allowJumpingOwnPieces) == null;
						if (valid)
						{
							validMovesSoFar.add(play);
							final List<Territory> newMiddleSteps = new ArrayList<Territory>(middleStepsSoFar);
							newMiddleSteps.add(leftUp);
							triedMovesSoFar.add(play);
							validMovesSoFar.addAll(getAllValidMovesFromHere(validMovesSoFar, triedMovesSoFar, originalStart, leftUp, newMiddleSteps, unit, startsAtLowRank, player, data, false,
										uncrownedCanCaptureBackwards, allowJumpingOwnPieces));
						}
					}
				}
			}
			{
				final Territory rightUp = map.getTerritoryFromCoordinates(thisStart.getX() + 2, thisStart.getY() - 2);
				if (rightUp != null)
				{
					final GridPlayData play = new GridPlayData(originalStart, middleStepsSoFar, rightUp, player);
					if (!triedMovesSoFar.contains(play))
					{
						final boolean valid = isValidPlay(play, player, data, uncrownedCanCaptureBackwards, allowJumpingOwnPieces) == null;
						if (valid)
						{
							validMovesSoFar.add(play);
							final List<Territory> newMiddleSteps = new ArrayList<Territory>(middleStepsSoFar);
							newMiddleSteps.add(rightUp);
							triedMovesSoFar.add(play);
							validMovesSoFar.addAll(getAllValidMovesFromHere(validMovesSoFar, triedMovesSoFar, originalStart, rightUp, newMiddleSteps, unit, startsAtLowRank, player, data, false,
										uncrownedCanCaptureBackwards, allowJumpingOwnPieces));
						}
					}
				}
			}
			{
				final Territory leftDown = map.getTerritoryFromCoordinates(thisStart.getX() - 2, thisStart.getY() + 2);
				if (leftDown != null)
				{
					final GridPlayData play = new GridPlayData(originalStart, middleStepsSoFar, leftDown, player);
					if (!triedMovesSoFar.contains(play))
					{
						final boolean valid = isValidPlay(play, player, data, uncrownedCanCaptureBackwards, allowJumpingOwnPieces) == null;
						if (valid)
						{
							validMovesSoFar.add(play);
							final List<Territory> newMiddleSteps = new ArrayList<Territory>(middleStepsSoFar);
							newMiddleSteps.add(leftDown);
							triedMovesSoFar.add(play);
							validMovesSoFar.addAll(getAllValidMovesFromHere(validMovesSoFar, triedMovesSoFar, originalStart, leftDown, newMiddleSteps, unit, startsAtLowRank, player, data, false,
										uncrownedCanCaptureBackwards, allowJumpingOwnPieces));
						}
					}
				}
			}
			{
				final Territory rightDown = map.getTerritoryFromCoordinates(thisStart.getX() + 2, thisStart.getY() + 2);
				if (rightDown != null)
				{
					final GridPlayData play = new GridPlayData(originalStart, middleStepsSoFar, rightDown, player);
					if (!triedMovesSoFar.contains(play))
					{
						final boolean valid = isValidPlay(play, player, data, uncrownedCanCaptureBackwards, allowJumpingOwnPieces) == null;
						if (valid)
						{
							validMovesSoFar.add(play);
							final List<Territory> newMiddleSteps = new ArrayList<Territory>(middleStepsSoFar);
							newMiddleSteps.add(rightDown);
							triedMovesSoFar.add(play);
							validMovesSoFar.addAll(getAllValidMovesFromHere(validMovesSoFar, triedMovesSoFar, originalStart, rightDown, newMiddleSteps, unit, startsAtLowRank, player, data, false,
										uncrownedCanCaptureBackwards, allowJumpingOwnPieces));
						}
					}
				}
			}
		}
		return validMovesSoFar;
	}
	
	public static String isValidPawnMove(final IGridPlayData play, final PlayerID player, final GameData data, final boolean uncrownedCanCaptureBackwards, final boolean allowJumpingOwnPieces)
	{
		final boolean startsAtLowRank = PlayerBeginsAtLowestRank.match(player);
		// all territories should be further than their previous territory
		// all must skip over other units, or move forward to unoccupied territories
		final int lastY = play.getStart().getY();
		final int lastX = play.getStart().getX();
		final int numSteps = play.getAllStepsExceptStart().size();
		// simple move
		if (numSteps == 1)
		{
			final int diffX = Math.abs(lastX - play.getEnd().getX());
			final int diffY = Math.abs(lastY - play.getEnd().getY());
			if (diffX == 1 && diffY == 1)
			{
				// move forward
				if (startsAtLowRank)
				{
					if (lastY > play.getEnd().getY())
						return "Pawns Must Move Forward Diagonally";
				}
				else
				{
					if (lastY < play.getEnd().getY())
						return "Pawns Must Move Forward Diagonally";
				}
				// we already check in basic check that the square is empty, so no need to check for that again
				return null;
			}
		}
		return isValidCaptureMove(play, player, data, uncrownedCanCaptureBackwards, allowJumpingOwnPieces);
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
	
	public static String isValidKingMove(final IGridPlayData play, final PlayerID player, final GameData data, final boolean allowJumpingOwnPieces)
	{
		// all must skip over other units, or move forward to unoccupied territories
		final int lastY = play.getStart().getY();
		final int lastX = play.getStart().getX();
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
		return isValidCaptureMove(play, player, data, true, allowJumpingOwnPieces);
	}
	
	public static String isValidCaptureMove(final IGridPlayData play, final PlayerID player, final GameData data, final boolean canCaptureBackwards, final boolean allowJumpingOwnPieces)
	{
		// all must skip over other units, or move forward to unoccupied territories
		final boolean startsAtLowRank = PlayerBeginsAtLowestRank.match(player);
		int lastY = play.getStart().getY();
		int lastX = play.getStart().getX();
		// final int numSteps = play.getAllStepsExceptStart().size();
		
		// because kings can form loops, we must make sure not to capture the same piece twice
		final Set<Territory> capturedAlready = new HashSet<Territory>();
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
				if (!startsAtLowRank && !canCaptureBackwards)
					return "Must Capture Moving Forward";
				// move left or right
				if (lastX == t.getX())
					return "Must Jump Over Pieces To Get There Or Must Move Right Or Left";
				final int diffX = Math.abs(lastX - t.getX());
				final int diffY = Math.abs(lastY - t.getY());
				if (diffX != 2 || diffY != 2)
					return "Must Either Move A Single Space, Or Jump Over Pieces";
				if (lastX < t.getX())
				{
					final Territory jumped = map.getTerritoryFromCoordinates(false, lastX + 1, lastY + 1);
					if (jumped.getUnits().getMatches((allowJumpingOwnPieces ? UnitIsUnit : UnitIsOwnedBy(player).invert())).isEmpty())
						return "Must Jump Over A Piece";
					else if (capturedAlready.contains(jumped))
						return "Can Not Form Loops";
					else
					{
						if (!ALLOW_JUMPING_SAME_PIECE_TWICE_IF_OWNED || jumped.getUnits().someMatch(UnitIsOwnedBy(player).invert()))
							capturedAlready.add(jumped);
					}
				}
				else
				{
					final Territory jumped = map.getTerritoryFromCoordinates(false, lastX - 1, lastY + 1);
					if (jumped.getUnits().getMatches((allowJumpingOwnPieces ? UnitIsUnit : UnitIsOwnedBy(player).invert())).isEmpty())
						return "Must Jump Over A Piece";
					else if (capturedAlready.contains(jumped))
						return "Can Not Form Loops";
					else
					{
						if (!ALLOW_JUMPING_SAME_PIECE_TWICE_IF_OWNED || jumped.getUnits().someMatch(UnitIsOwnedBy(player).invert()))
							capturedAlready.add(jumped);
					}
				}
			}
			else
			{
				if (startsAtLowRank && !canCaptureBackwards)
					return "Must Capture Moving Forward";
				// move left or right
				if (lastX == t.getX())
					return "Must Jump Over Pieces To Get There Or Must Move Right Or Left";
				final int diffX = Math.abs(lastX - t.getX());
				final int diffY = Math.abs(lastY - t.getY());
				if (diffX != 2 || diffY != 2)
					return "Must Either Move A Single Space, Or Jump Over Pieces";
				if (lastX < t.getX())
				{
					final Territory jumped = map.getTerritoryFromCoordinates(false, lastX + 1, lastY - 1);
					if (jumped.getUnits().getMatches((allowJumpingOwnPieces ? UnitIsUnit : UnitIsOwnedBy(player).invert())).isEmpty())
						return "Must Jump Over A Piece";
					else if (capturedAlready.contains(jumped))
						return "Can Not Form Loops";
					else
					{
						if (!ALLOW_JUMPING_SAME_PIECE_TWICE_IF_OWNED || jumped.getUnits().someMatch(UnitIsOwnedBy(player).invert()))
							capturedAlready.add(jumped);
					}
				}
				else
				{
					final Territory jumped = map.getTerritoryFromCoordinates(false, lastX - 1, lastY - 1);
					if (jumped.getUnits().getMatches((allowJumpingOwnPieces ? UnitIsUnit : UnitIsOwnedBy(player).invert())).isEmpty())
						return "Must Jump Over A Piece";
					else if (capturedAlready.contains(jumped))
						return "Can Not Form Loops";
					else
					{
						if (!ALLOW_JUMPING_SAME_PIECE_TWICE_IF_OWNED || jumped.getUnits().someMatch(UnitIsOwnedBy(player).invert()))
							capturedAlready.add(jumped);
					}
				}
			}
			lastY = t.getY();
			lastX = t.getX();
		}
		return null;
	}
	
	public static String isValidMoveNoLoops(final IGridPlayData play, final PlayerID player, final GameData data)
	{
		// a loop is not just a circle, but a circle that ends going in the same direction as it start in
		// the only way i can think of to test for this, is to make tuples of territories and their directions (the next territory after them),
		// and then make sure that none of the tuples occur more than once
		final List<Territory> territories = play.getAllSteps();
		final int size = territories.size();
		if (size <= 2)
			return null;
		final List<Tuple<Territory, Territory>> directions = new ArrayList<Tuple<Territory, Territory>>();
		// create tuples
		for (int i = 0; i < size - 1; i++)
		{
			directions.add(new Tuple<Territory, Territory>(territories.get(i), territories.get(i + 1)));
		}
		// inefficient loop
		// test for if any of them occur twice or more
		final Iterator<Tuple<Territory, Territory>> iter = directions.iterator();
		while (iter.hasNext())
		{
			final Tuple<Territory, Territory> d1 = iter.next();
			for (int i = 1; i < directions.size(); i++)
			{
				final Tuple<Territory, Territory> d2 = directions.get(i);
				if (d1.equals(d2))
					return "No Loops Allowed";
			}
			iter.remove();
		}
		return null;
	}
	
	public static List<GridPlayData> getPlaysWithShortVersionsRemoved(final List<GridPlayData> plays, final PlayerID player, final GameData data)
	{
		final List<GridPlayData> validLongPlays = new ArrayList<GridPlayData>();
		final List<GridPlayData> allPlays = new ArrayList<GridPlayData>(plays);
		Collections.sort(allPlays, GridPlayData.SmallestToLargestPlays);
		final Iterator<GridPlayData> iter = allPlays.iterator();
		while (iter.hasNext())
		{
			final GridPlayData play = iter.next();
			boolean isNotContained = true;
			for (final IGridPlayData other : allPlays)
			{
				if (other.isBiggerThanAndContains(play))
				{
					isNotContained = false;
					break;
				}
			}
			if (isNotContained)
				validLongPlays.add(play);
			iter.remove();
		}
		return validLongPlays;
	}
}


class CheckersPlayExtendedDelegateState implements Serializable
{
	private static final long serialVersionUID = -5446004854202452332L;
	Serializable superState;
	// add other variables here:
}
