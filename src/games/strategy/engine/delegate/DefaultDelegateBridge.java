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

package games.strategy.engine.delegate;


import games.strategy.engine.data.*;
import games.strategy.engine.framework.IGame;
import games.strategy.engine.message.*;
import games.strategy.engine.framework.*;
import games.strategy.engine.message.Message;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.history.*;
import games.strategy.engine.random.*;

/**
 *
 * Default implementation of DelegateBridge
 *
 * @author  Sean Bridges
 */
public class DefaultDelegateBridge implements DelegateBridge
{

  private final GameStep m_step;
  private PlayerID m_player;
  private final IGame m_game;
  private final DelegateHistoryWriter m_historyWriter;

  private IRandomSource m_randomSource = new PlainRandomSource();


  /** Creates new DefaultDelegateBridge */
  public DefaultDelegateBridge(GameData data, GameStep step, IGame game, DelegateHistoryWriter historyWriter)
  {
    m_step = step;
    m_game = game;
    m_player = m_step.getPlayerID();
    m_historyWriter = historyWriter;

  }

  public PlayerID getPlayerID()
  {
    return m_player;
  }

  public void setRandomSource(IRandomSource randomSource)
  {
    m_randomSource = randomSource;
  }

  /**
   * All delegates should use random data that comes from both players
   * so that neither player cheats.
   */
  public int getRandom(int max, String annotation)
  {
    return m_randomSource.getRandom(max, annotation);
  }

  /**
   * Delegates should not use random data that comes from any other source.
   */
  public int[] getRandom(int max, int count, String annotation)
  {
    return m_randomSource.getRandom(max, count, annotation);
  }


  public void addChange(Change aChange)
  {
    m_game.addChange(aChange);
  }

  /**
   * Messages are sent to the current player
   */
  public Message sendMessage(Message message)
  {
    return m_game.getMessageManager().send(message, m_player.getName());
  }

  public void setPlayerID(PlayerID aPlayer)
  {
    m_player = aPlayer;
  }

  /**
   * Sends a message to the given player.
   */
  public Message sendMessage(Message message, PlayerID player)
  {
    return m_game.getMessageManager().send(message, player.getName());
  }

  /**
   * Sends a message to the given player.
   */
  public Message sendMessage(Message message, String dest_name)
  {
    return m_game.getMessageManager().send(message, dest_name);
  }

  /**
   * Returns the current step name
   */
  public String getStepName()
  {
    return m_step.getName();
  }


  public void sendMessageNoResponse(Message message)
  {
    sendMessageNoResponse(message, getPlayerID());
  }

  public void sendMessageNoResponse(Message message, PlayerID player)
  {
    m_game.getMessageManager().sendNoResponse(message, player.getName());
  }

  public DelegateHistoryWriter getHistoryWriter()
  {
      return m_historyWriter;
  }
}
