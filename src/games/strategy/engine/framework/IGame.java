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
 * IGame.java
 *
 * Created on December 31, 2001, 11:26 AM
 */

package games.strategy.engine.framework;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.events.GameStepListener;
import games.strategy.engine.message.IMessageManager;
import games.strategy.net.*;
import games.strategy.net.IMessenger;
import games.strategy.engine.data.Change;
import games.strategy.engine.random.*;


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
  public IChannelMessenger getChannelMessenger();
  public IRemoteMessenger getRemoteMessenger();

  

  /**
   * Should not be called outside of engine code.
   */
  public void addChange(Change aChange);

  public boolean canSave();

  public void shutdown();

  public IRandomSource getRandomSource();
}
