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
import games.strategy.grid.delegate.remote.IGridPlayDelegate;
import games.strategy.grid.ui.display.IGridGameDisplay;
import games.strategy.util.Match;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;

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
		
		final GameMap map = data.getMap();
		
		if (UnitIsPawn.match(unit))
		{
			// TODO: fix for a lot of stuff
			if (Math.abs(end.getX() - start.getX()) <= 1 && Math.abs(end.getY() - start.getY()) <= 1)
				return null;
		}
		else if (UnitIsKnight.match(unit))
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
		}
		else if (UnitIsBishop.match(unit))
		{
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
		}
		else if (UnitIsRook.match(unit))
		{
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
		}
		else if (UnitIsQueen.match(unit))
		{
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
		}
		else if (UnitIsKing.match(unit))
		{
			// TODO: castling
			if (Math.abs(end.getX() - start.getX()) <= 1 && Math.abs(end.getY() - start.getY()) <= 1)
				return null;
		}
		else
			return "?? Unit";
		return "Invalid Move";
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
		m_bridge.addChange(change);
		final IGridGameDisplay display = (IGridGameDisplay) m_bridge.getDisplayChannelBroadcaster();
		display.performPlay(start, end, captured);
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
}
