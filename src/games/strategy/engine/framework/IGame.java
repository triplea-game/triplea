/*
 * IGame.java
 *
 * Created on December 31, 2001, 11:26 AM
 */

package games.strategy.engine.framework;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.events.GameStepListener;
import games.strategy.engine.message.IMessageManager;
import games.strategy.net.IMessenger;
import games.strategy.engine.data.Change;
import games.strategy.engine.transcript.*;

/**
 * Represents a running game. <p>
 * Allows access to the games communication interfaces, and to listen to the 
 * current game step.
 *
 * 
 *
 * @author  Sean Bridges
 */
public interface IGame 
{
	public GameData getData();
	
	public void addGameStepListener(GameStepListener listener);
	public void removeGameStepListener(GameStepListener listener);
	
	public IMessageManager getMessageManager();
	public IMessenger getMessenger();
	
	/**
	 * Should not be called outside of engine code.
	 */
	public void addChange(Change aChange);
	
	/**
	 * Get the ganes transcript.
	 */
	public Transcript getTranscript();
}