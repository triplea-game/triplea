package games.strategy.engine.chat;

import games.strategy.net.INode;

import java.util.*;


/**
 * An interface to allow for testing.
 * 
 * @author sgb
 */
public interface IChatListener
{

    public void updatePlayerList(final Collection<INode> players);

    public void addMessage(final ChatMessage message, final boolean thirdperson);
    
    public void addStatusMessage(final String message);

}
