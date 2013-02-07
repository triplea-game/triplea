package games.strategy.grid.checkers.delegate;

import games.strategy.common.delegate.AbstractDelegate;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.AutoSave;
import games.strategy.engine.message.IRemote;
import games.strategy.grid.delegate.remote.IGridPlayDelegate;
import games.strategy.grid.player.IGridGamePlayer;
import games.strategy.grid.ui.display.IGridGameDisplay;
import games.strategy.util.Match;

import java.io.Serializable;
import java.util.Collection;

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
	
	public String play(final Territory start, final Territory end)
	{
		if (start.getUnits().getUnitCount() > 1 || end.getUnits().getUnitCount() > 1)
			throw new IllegalStateException("Can not have more than 1 unit in any territory");
		final String error = isValidPlay(start, end, m_player, getData());
		if (error != null)
			return error;
		final Collection<Territory> captured = checkForCaptures(start, end, m_player, getData());
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
		
		final String pieceBasic = isValidPieceMoveBasic(start, end, player, data);
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
	public static Collection<Territory> checkForCaptures(final Territory start, final Territory end, final PlayerID player, final GameData data)
	{
		return null;
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
	private void performPlay(final Territory start, final Territory end, final Collection<Territory> captured, final PlayerID player)
	{
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
	
	public static String isValidPieceMoveBasic(final Territory start, final Territory end, final PlayerID player, final GameData data)
	{
		final Collection<Unit> units = start.getUnits().getUnits();
		final Unit unit = units.iterator().next();
		if (UnitIsPawn.match(unit))
		{
			return isValidPawnMove(start, end, player, data);
		}
		else if (UnitIsKing.match(unit))
		{
			return isValidKingMove(start, end, player, data);
		}
		return "?? Unit";
	}
	
	public static String isValidPawnMove(final Territory start, final Territory end, final PlayerID player, final GameData data)
	{
		return null;
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
	
	public static String isValidKingMove(final Territory start, final Territory end, final PlayerID player, final GameData data)
	{
		return null;
	}
}


class CheckersPlayExtendedDelegateState implements Serializable
{
	private static final long serialVersionUID = -5446004854202452332L;
	Serializable superState;
	// add other variables here:
}
