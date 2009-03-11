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

import java.io.File;

import games.strategy.engine.data.*;
import games.strategy.engine.data.events.GameStepListener;
import games.strategy.engine.display.IDisplay;
import games.strategy.engine.message.*;
import games.strategy.engine.random.IRandomSource;
import games.strategy.engine.vault.Vault;
import games.strategy.net.IMessenger;


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
  public static final RemoteName GAME_MODIFICATION_CHANNEL = new RemoteName("games.strategy.engine.framework.IGame.GAME_MODIFICATION_CHANNEL", IGameModifiedChannel.class);  
    
  public GameData getData();

  public void addGameStepListener(GameStepListener listener);
  public void removeGameStepListener(GameStepListener listener);

  public IMessenger getMessenger();
  public IChannelMessenger getChannelMessenger();
  public IRemoteMessenger getRemoteMessenger();
  
  public Vault getVault();
  

  /**
   * Should not be called outside of engine code.
   */
  public void addChange(Change aChange);

  public boolean canSave();

  public IRandomSource getRandomSource();

  /**
   * add a display that will recieve broadcasts from the IDelegateBridge.getDisplayBroadvaster
   */
  public void addDisplay(IDisplay display);
  
  
  /**
   * remove a display
   */
  public void removeDisplay(IDisplay display);
  
  /**
   * Is the game over.  Game over does not relate to the state of the game (eg check-mate in chess)
   * but to the game being shut down and all players have left.<p>
   * 
   * 
   */
  public boolean isGameOver();
  
  /**
   * 
   * @return a listing of who is playing who.
   */
  public PlayerManager getPlayerManager();
  
  /**
   * Save the game to the given directory.
   * 
   * The file should exist and be writeable.
   */
  public void saveGame(File f);
}
