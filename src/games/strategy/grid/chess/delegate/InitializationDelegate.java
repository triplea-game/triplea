package games.strategy.grid.chess.delegate;

import games.strategy.common.delegate.AbstractDelegate;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.message.IRemote;
import games.strategy.grid.chess.attachments.PlayerAttachment;

import java.io.Serializable;

public class InitializationDelegate extends AbstractDelegate
{
	/**
	 * Called before the delegate will run.
	 */
	@Override
	public void start()
	{
		super.start();
		// make sure all players have a player attachment
		for (final PlayerID player : getData().getPlayerList().getPlayers())
		{
			PlayerAttachment pattachment = PlayerAttachment.get(player);
			if (pattachment == null)
			{
				pattachment = new PlayerAttachment(PlayerAttachment.ATTACHMENT_NAME, player, getData());
				player.addAttachment(PlayerAttachment.ATTACHMENT_NAME, pattachment);
			}
		}
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
		return false;
	}
	
	/**
	 * If this class implements an interface which inherits from IRemote, returns the class of that interface.
	 * Otherwise, returns null.
	 */
	@Override
	public Class<? extends IRemote> getRemoteType()
	{
		// This class does not implement the IRemote interface, so return null.
		return null;
	}
}
