package games.strategy.engine.chat;

import games.strategy.engine.message.IRemote;
import games.strategy.net.INode;
import games.strategy.util.Tuple;

import java.util.List;

/**
 * A central controller of who is in the chat.<p>  
 * 
 * When joining you get a list of all the players currently in the chat<p>
 * 
 * To handle un-ordered comings and going into the chat, we send a version number
 * with each chat change. The init method is the sum of all changes &lt; the version number returned
 * in the init message.<p>
 * 
 * @author sgb
 */
interface IChatController extends IRemote
{
    /**
     * Join the chat, returns the chatters currently in the chat.
     */
    public Tuple<List<INode>, Long> joinChat();
    
    /**
     * Leave the chat, and ask that everyone stops bothering me.
     */
    public void leaveChat();
    
}
