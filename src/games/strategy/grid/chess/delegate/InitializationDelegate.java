package games.strategy.grid.chess.delegate;

import games.strategy.common.delegate.AbstractDelegate;
import games.strategy.engine.data.ChangeFactory;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.message.IRemote;
import games.strategy.grid.chess.attachments.PlayerAttachment;

import java.io.Serializable;

/**
 * 
 * @author veqryn
 * 
 */
public class InitializationDelegate extends AbstractDelegate
{
	/**
	 * Called before the delegate will run.
	 */
	@Override
	public void start()
	{
		super.start();
		final CompositeChange initializingBoard = new CompositeChange();
		// make sure all players have a player attachment
		for (final PlayerID player : getData().getPlayerList().getPlayers())
		{
			PlayerAttachment pattachment = PlayerAttachment.get(player);
			if (pattachment == null)
			{
				pattachment = new PlayerAttachment(PlayerAttachment.ATTACHMENT_NAME, player, getData());
				// player.addAttachment(PlayerAttachment.ATTACHMENT_NAME, pattachment);
				initializingBoard.add(ChangeFactory.addAttachmentChange(pattachment, player, PlayerAttachment.ATTACHMENT_NAME));
			}
		}
		if (!initializingBoard.isEmpty())
		{
			m_bridge.getHistoryWriter().startEvent("Initializing board");
			m_bridge.addChange(initializingBoard);
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
