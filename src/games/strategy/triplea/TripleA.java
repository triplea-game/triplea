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
 * TripleA.java
 *
 *
 * Created on November 2, 2001, 8:56 PM
 */

package games.strategy.triplea;

import java.io.IOException;
import java.util.*;

import games.strategy.engine.framework.IGame;
import games.strategy.engine.framework.IGameLoader;
import games.strategy.engine.random.IronyGamesDiceRollerRandomSource;
import games.strategy.triplea.ui.TripleAFrame;
import java.awt.*;
import games.strategy.engine.random.*;


/**
 *
 * @author  Sean Bridges
 * @version 1.0
 */
public class TripleA implements IGameLoader
{
  private static final String HUMAN_PLAYER_TYPE = "Human";
  private static final String COMPUTER_PLAYER_TYPE = "Computer";



	public Set createPlayers(Map playerNames)
	{
		Set players = new HashSet();
		Iterator iter = playerNames.keySet().iterator();
		while(iter.hasNext())
		{
			String name = (String) iter.next();
      String type = (String) playerNames.get(name);
      if(type.equals(COMPUTER_PLAYER_TYPE))
      {
        throw new IllegalStateException("TODO - create a GamePlayer instance for computer players here");
      }
      else if (type.equals(HUMAN_PLAYER_TYPE) )
      {
        TripleAPlayer player = new TripleAPlayer(name);
        players.add(player);
      }
      else
      {
        throw new IllegalStateException("Player type not recognized:" + type);
      }
		}
		return players;
	}

	public void startGame(final IGame game, Set players)
	{
		try
		{
			TripleAFrame frame = new TripleAFrame(game, players);

      //the pbem roller needs to know about the ui.
      if(game.getRandomSource() != null && game.getRandomSource() instanceof IronyGamesDiceRollerRandomSource)
      {
        ((IronyGamesDiceRollerRandomSource) game.getRandomSource()).setUI(frame);
      }
      if(game.getRandomSource() != null && game.getRandomSource() instanceof CryptoRandomSource)
      {
        //the first roll takes a while, initialize
        //here in the background so that the user doesnt notice
        Thread t = new Thread()
        {
          public void run()
          {
            game.getRandomSource().getRandom(Constants.MAX_DICE,2,"Warming up crpyto random source");
          }
        };
        t.start();


      }

			frame.setVisible(true);

			while(!frame.isVisible())
			{
				Thread.currentThread().yield();
			}
      //size when minimized
      frame.setSize(700,400);

      frame.setExtendedState(Frame.MAXIMIZED_BOTH);

			connectPlayers(players, frame);
		} catch(IOException ioe)
		{
			ioe.printStackTrace();
			System.exit(0);
		}
	}

	private void connectPlayers(Set players, TripleAFrame frame)
	{
		Iterator iter = players.iterator();
		while(iter.hasNext())
		{
			TripleAPlayer player = (TripleAPlayer) iter.next();
			player.setFrame(frame);
		}
	}

	/**
	 * Return an array of player types that can play on the server.
	 * This array must not contain any entries that could play on the client.
	 */
	public String[] getServerPlayerTypes()
	{
    if(System.getProperties().getProperty("triplea.ai") != null)
      return new String[] {HUMAN_PLAYER_TYPE, COMPUTER_PLAYER_TYPE};
    else
      return new String[] {HUMAN_PLAYER_TYPE};
	}

	/**
	 * Return an array of player types that can play on the client.
	 * This array must not contain any entries that could play on the server.
	 */
	public String[] getClientPlayerTypes()
	{
		String[] players = {"Client"};
		return players;
	}
}
