/*
 * DefaultPlayerBridge.java
 *
 * Created on October 27, 2001, 8:55 PM
 */

package games.strategy.engine.gamePlayer;

import java.util.*;

import games.strategy.engine.data.*;
import games.strategy.engine.data.events.GameStepListener;
import games.strategy.engine.delegate.*;
import games.strategy.engine.framework.*;
import games.strategy.engine.message.Message;

/**
 * Default implementation of PlayerBridge. 
 *
 * @author  Sean Bridges
 * @version 1.0
 *
 */
public class DefaultPlayerBridge implements PlayerBridge 
{

	private final IGame m_game;
	private final GamePlayer m_player;
	private String m_currentStep;
	private String m_currentDelegate;
	
	/** Creates new DefaultPlayerBridge */
    public DefaultPlayerBridge(IGame aGame, GamePlayer aPlayer) 
	{
		m_player = aPlayer;
		m_game = aGame;
		m_game.addGameStepListener(m_gameStepListener);
    }
		
	/**
	 * Send a message to the current delegate
	 * @returnVal null if the action performed successfuly, otherwise an error message.
	 */
	public Message sendMessage(Message message) 
	{
		return m_game.getMessageManager().send(message, m_currentDelegate);
	}
	
	/**
	 * Get the name of the current step being exectured.
	 */
	public String getStepName() 
	{
		return m_currentStep;
	}
		
	/**
	 * Return the game data
	 */
	public GameData getGameData() 
	{
		return m_game.getData();
	}
	
	private GameStepListener m_gameStepListener = new GameStepListener()
	{
		public void gameStepChanged(String stepName, String delegateName, PlayerID player)
		{
			m_currentStep = stepName;
			m_currentDelegate = delegateName;
		}
	};
	
}