package games.strategy.grid.chess.delegate;

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
import games.strategy.grid.chess.ChessUnit;
import games.strategy.grid.delegate.remote.IGridPlayDelegate;
import games.strategy.grid.ui.display.IGridGameDisplay;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.Match;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

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
		return null;
	}
	
	@Override
	public void loadState(final Serializable state)
	{
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
	
	public String play(final Territory start, final Territory end)
	{
		if (start.getUnits().getUnitCount() > 1 || end.getUnits().getUnitCount() > 1)
			throw new IllegalStateException("Can not have more than 1 unit in any territory");
		final String error = isValidPlay(start, end, m_player, getData());
		if (error != null)
			return error;
		final Collection<Territory> captured = checkForCaptures(start, end);
		performPlay(start, end, captured, m_player);
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
	public static String isValidPlay(final Territory start, final Territory end, final PlayerID player, final GameData data)
	{
		// System.out.println("Start: " + start.getX() + "," + start.getY() + "    End: " + end.getX() + "," + end.getY());
		final String basic = isValidMoveBasic(start, end, player, data);
		if (basic != null)
			return basic;
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
			return isValidKingMove(start, end, player, data);
		}
		return "?? Unit";
	}
	
	/**
	 * After a move completes, look to see if any captures occur.
	 * 
	 * @param end
	 *            <code>Territory</code> where the move ended. All potential captures must involve this <code>Territory</code>.
	 * @return
	 */
	private Collection<Territory> checkForCaptures(final Territory start, final Territory end)
	{
		// should only be able to capture 1 piece at most
		final Collection<Territory> captured = new HashSet<Territory>(1);
		// except for the weird pawn rule (En passant), the captured piece will always be on the end territory
		// TODO: En passant
		captured.add(end);
		
		return captured;
	}
	
	/**
	 * Move a piece from the start <code>Territory</code> to the end <code>Territory</code>.
	 * 
	 * @param start
	 *            <code>Territory</code> where the move should start
	 * @param end
	 *            <code>Territory</code> where the move should end
	 */
	private void performPlay(final Territory start, final Territory end, final Collection<Territory> captured, final PlayerID player)
	{
		final Collection<Unit> units = start.getUnits().getUnits();
		final String transcriptText = player.getName() + " moved from " + start.getName() + " to " + end.getName();
		m_bridge.getHistoryWriter().startEvent(transcriptText, units);
		final Change removeUnit = ChangeFactory.removeUnits(start, units);
		// final Change removeStartOwner = ChangeFactory.changeOwner(start, PlayerID.NULL_PLAYERID);
		final Change addUnit = ChangeFactory.addUnits(end, units);
		// final Change addEndOwner = ChangeFactory.changeOwner(end, player);
		final CompositeChange change = new CompositeChange();
		change.add(removeUnit);
		// change.add(removeStartOwner);
		change.add(addUnit);
		// change.add(addEndOwner);
		for (final Unit u : units)
		{
			if (!((ChessUnit) u).getHasMoved())
				change.add(ChangeFactory.unitPropertyChange(u, true, ChessUnit.HAS_MOVED));
		}
		for (final Territory at : captured)
		{
			if (at != null)
			{
				final Collection<Unit> capturedUnits = at.getUnits().getUnits();
				final Change capture = ChangeFactory.removeUnits(at, capturedUnits);
				change.add(capture);
				// final Change removeOwner = ChangeFactory.changeOwner(at, PlayerID.NULL_PLAYERID);
				// change.add(removeOwner);
			}
		}
		final Collection<Territory> refresh = new HashSet<Territory>();
		refresh.add(start);
		refresh.add(end);
		refresh.addAll(captured);
		// castling
		if (start.getUnits().someMatch(UnitIsKing) && isValidKingCastling(start, end, player, getData()) == null)
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
					if (!((ChessUnit) u).getHasMoved())
						change.add(ChangeFactory.unitPropertyChange(u, true, ChessUnit.HAS_MOVED));
				}
				refresh.add(rookTerOld);
				refresh.add(rookTerNew);
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
					if (!((ChessUnit) u).getHasMoved())
						change.add(ChangeFactory.unitPropertyChange(u, true, ChessUnit.HAS_MOVED));
				}
				refresh.add(rookTerOld);
				refresh.add(rookTerNew);
			}
		}
		m_bridge.addChange(change);
		final IGridGameDisplay display = (IGridGameDisplay) m_bridge.getDisplayChannelBroadcaster();
		display.refreshTerritories(refresh);
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
			return unit != null && !((ChessUnit) unit).getHasMoved();
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
						return "Must Capture A Piece To Move Diagonally";
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
						return "Must Capture A Piece To Move Diagonally";
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
	
	public static String isValidKingMove(final Territory start, final Territory end, final PlayerID player, final GameData data)
	{
		if (Math.abs(end.getX() - start.getX()) <= 1 && Math.abs(end.getY() - start.getY()) <= 1)
			return null;
		return isValidKingCastling(start, end, player, data);
	}
	
	public static String isValidKingCastling(final Territory start, final Territory end, final PlayerID player, final GameData data)
	{
		// castling
		final Collection<Unit> units = start.getUnits().getUnits();
		final Unit unit = units.iterator().next();
		final GameMap map = data.getMap();
		if (start.getY() == end.getY() && UnitHasNeverMovedBefore.match(unit))
		{
			final int lastRow = map.getYDimension() - 1;
			final int lastColumn = map.getXDimension() - 1;
			// TODO: The king may not currently be in check, nor may the king pass through or end up in a square that is under attack by an enemy piece (though the rook is permitted to be under attack and to pass over an attacked square)
			if (start.getY() == 0 || start.getY() == lastRow)
			{
				if (Math.abs(start.getX() - end.getX()) == 2)
				{
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
							return null;
						}
					}
				}
			}
		}
		return "Invalid Move";
	}
}
