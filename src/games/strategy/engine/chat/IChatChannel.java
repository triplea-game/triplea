package games.strategy.engine.chat;

import games.strategy.engine.chat.IChatController.Tag;
import games.strategy.engine.message.IChannelSubscribor;
import games.strategy.net.INode;

/**
 * Chat messages occur on this channel
 * 
 * @author sgb
 */
public interface IChatChannel extends IChannelSubscribor
{
	// we get the sender from MessageContext
	public void chatOccured(final String message);
	
	public void meMessageOccured(final String message);
	
	public void slapOccured(final String playerName);
	
	public void speakerAdded(final INode node, final Tag tag, final long version);
	
	public void speakerRemoved(final INode node, final long version);
	
	public void speakerTagUpdated(final INode node, final Tag tag);
}
