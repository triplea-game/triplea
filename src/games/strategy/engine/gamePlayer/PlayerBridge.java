/*
 * PlayerBridge.java
 *
 * Created on October 27, 2001, 5:23 PM
 */

package games.strategy.engine.gamePlayer;

import java.util.*;

import games.strategy.engine.message.Message;
import games.strategy.engine.data.GameData;

/**
 *
 * @author  Sean Bridges
 * @version 1.0
 *
 * Communication with the GamePlayer goes through the PlayerBridge to 
 * make the game network transparent.
 *
 *
 */
public interface PlayerBridge 
{	
	/**
	 * Return the game data
	 */
	public GameData getGameData();
	
	/**
	 * Send a message to the current delegate.
	 */
	public Message sendMessage(Message aMessage);
	
	/** 
	 * Get the name of the current step being exectured.
	 */
	public String getStepName();
}