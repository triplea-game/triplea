/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

/*
 * DefaultPlayerBridge.java
 *
 * Created on October 27, 2001, 8:55 PM
 */

package games.strategy.engine.gamePlayer;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.events.GameStepListener;
import games.strategy.engine.framework.IGame;
import games.strategy.engine.message.Message;

/**
 * Default implementation of PlayerBridge.
 *
 * @author  Sean Bridges
 *
 */
public class DefaultPlayerBridge implements PlayerBridge
{

	private final IGame m_game;
	private String m_currentStep;
	private String m_currentDelegate;


  /** Creates new DefaultPlayerBridge */
  public DefaultPlayerBridge(IGame aGame)
  {
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
		public void gameStepChanged(String stepName, String delegateName, PlayerID player, int round, String displayName)
		{
      if(stepName == null)
        throw new IllegalArgumentException("Null step");
      if(delegateName == null)
        throw new IllegalArgumentException("Null delegate");


			m_currentStep = stepName;
			m_currentDelegate = delegateName;
		}
	};

}
