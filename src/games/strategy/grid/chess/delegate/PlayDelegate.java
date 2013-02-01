package games.strategy.grid.chess.delegate;

import games.strategy.common.delegate.AbstractDelegate;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.delegate.AutoSave;
import games.strategy.engine.message.IRemote;
import games.strategy.grid.delegate.remote.IGridPlayDelegate;
import games.strategy.grid.ui.display.IGridGameDisplay;

import java.io.Serializable;
import java.util.Collection;

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
		final String error = isValidPlay(start, end);
		if (error != null)
			return error;
		final Collection<Territory> captured = checkForCaptures(start, end);
		performPlay(start, end, captured, m_player);
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
	private String isValidPlay(final Territory start, final Territory end)
	{
		// TODO:
		return null;
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
		// TODO:
		return null;
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
		// TODO:
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
}
