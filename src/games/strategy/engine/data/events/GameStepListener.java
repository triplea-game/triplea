/*
 * GameStepListener.java
 *
 * Created on November 6, 2001, 9:00 PM
 */

package games.strategy.engine.data.events;

import games.strategy.engine.data.PlayerID;
/**
 *
 * @author  Sean Bridges
 * @version 1.0
 */
public interface GameStepListener 
{
	public void gameStepChanged(String stepName, String delegateName, PlayerID player);
}
