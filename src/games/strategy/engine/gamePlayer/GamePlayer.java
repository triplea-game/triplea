/*
 * GamePlayer.java
 *
 * Created on October 27, 2001, 5:15 PM
 */

package games.strategy.engine.gamePlayer;

import java.util.*;

import games.strategy.engine.data.PlayerID;
import games.strategy.engine.message.*;

/**
 *
 * A player of the game.  <p>
 * Game players communicate to the game through a PlayerBridge.
 *
 * @author  Sean Bridges
 * @version 1.0
 *
 */
public interface GamePlayer extends IDestination
{
	public void initialize(PlayerBridge bridge, PlayerID id);
	public Message sendMessage(Message message);
	public String getName();
	public PlayerID getID();
	public void start(String stepName);
}