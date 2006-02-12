package games.strategy.engine.chat;

import java.util.*;


/**
 * An interface to allow for testing.
 * 
 * @author sgb
 */
public interface IChatListener
{

    public void updatePlayerList(final Collection<String> players);

    public void addMessage(final String message, final String from, final boolean thirdperson);

}
