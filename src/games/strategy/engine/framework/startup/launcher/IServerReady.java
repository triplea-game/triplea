package games.strategy.engine.framework.startup.launcher;

import games.strategy.engine.message.IRemote;

/**
 * 
 * Allows for the server to wait for all clients to finish initialization before
 * starting the game
 * 
 * @author Sean Bridges
 */

public interface IServerReady extends IRemote
{
    public void clientReady();
}
