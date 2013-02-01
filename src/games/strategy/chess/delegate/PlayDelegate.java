package games.strategy.chess.delegate;

import games.strategy.chess.delegate.remote.IPlayDelegate;
import games.strategy.common.delegate.BaseDelegate;
import games.strategy.engine.data.Territory;
import games.strategy.engine.delegate.AutoSave;
import games.strategy.engine.message.IRemote;

import java.io.Serializable;

@AutoSave(beforeStepStart = false, afterStepEnd = true)
public class PlayDelegate extends BaseDelegate implements IPlayDelegate
{
	/**
	 * Called before the delegate will run.
	 */
	@Override
	public void start()
	{
		super.start();
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
		return null;
	}
	
	/**
	 * If this class implements an interface which inherits from IRemote, returns the class of that interface.
	 * Otherwise, returns null.
	 */
	@Override
	public Class<? extends IRemote> getRemoteType()
	{
		// This class implements IPlayDelegate, which inherits from IRemote.
		return IPlayDelegate.class;
	}
}
