package games.strategy.engine.chat;

import games.strategy.engine.message.IChannelSubscribor;
import games.strategy.net.INode;

/**
 * Chat messages occur on this channel
 * 
 * @author sgb
 */
interface IChatChannel extends IChannelSubscribor
{
    //we get the sender from MessageContext
    public void chatOccured(String message);
    public void meMessageOccured(String message);
    public void slapOccured(INode to);
    

    public void speakerAdded(INode node, long version);
    public void speakerRemoved(INode node, long version);    
}