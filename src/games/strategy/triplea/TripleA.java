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
import games.strategy.triplea.ui.TerritoryData;
import games.strategy.triplea.ui.TripleAFrame;
import java.awt.*;
import games.strategy.engine.random.*;
import games.strategy.engine.gamePlayer.*;
import games.strategy.triplea.image.MapImage;
import games.strategy.triplea.image.TerritoryImageFactory;
import games.strategy.triplea.sound.*;

/**
 * @author Sean Bridges
 * @version 1.0
 */
public class TripleA implements IGameLoader
{
    private static final String HUMAN_PLAYER_TYPE = "Human";
    private static final String COMPUTER_PLAYER_TYPE = "Computer";


    /**
     * @see IGameLoader.createPlayers(playerNames)
     */
    public Set createPlayers(Map playerNames)
    {
        Set players = new HashSet();
        Iterator iter = playerNames.keySet().iterator();
        while (iter.hasNext())
        {
            String name = (String) iter.next();
            String type = (String) playerNames.get(name);
            if (type.equals(COMPUTER_PLAYER_TYPE))
            {
                throw new IllegalStateException("TODO - create a GamePlayer instance for computer players here");
            } else if (type.equals(HUMAN_PLAYER_TYPE) || type.equals(CLIENT_PLAYER_TYPE))
            {
                TripleAPlayer player = new TripleAPlayer(name);
                players.add(player);
            } else
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
            boolean fourthEdition = game.getData().getProperties().get(Constants.FOURTH_EDITION, false);
            String mapDir = game.getData().getProperties().get(Constants.MAP_NAME).toString();
            TerritoryData.setFourthEdition(fourthEdition);
            MapImage.setFourthEdition(fourthEdition);
            TerritoryImageFactory.setFourthEdition(fourthEdition);
            TerritoryImageFactory.setMapDir(mapDir);

            TripleAFrame frame = new TripleAFrame(game, players);

            //the pbem roller needs to know about the ui.
            /*if (game.getRandomSource() != null && game.getRandomSource() instanceof IronyGamesDiceRollerRandomSource)
            {
                ((IronyGamesDiceRollerRandomSource) game.getRandomSource()).setUI(frame);
            }*/


            frame.setVisible(true);

            while (!frame.isVisible())
            {
                Thread.yield();
            }
            //size when minimized
            frame.setSize(700, 400);

            frame.setExtendedState(Frame.MAXIMIZED_BOTH);

            connectPlayers(players, frame);

            //load the sounds in a background thread,
            //avoids the pause where sounds dont load right away
            Runnable loadSounds = new Runnable()
            {
                public void run()
                {
                    SoundPath.preLoadSounds();
                }
            };
            new Thread(loadSounds).start();

        } catch (IOException ioe)
        {
            ioe.printStackTrace();
            System.exit(0);
        }
    }

    private void connectPlayers(Set players, TripleAFrame frame)
    {
        Iterator iter = players.iterator();
        while (iter.hasNext())
        {
            GamePlayer player = (GamePlayer) iter.next();
            if (player instanceof TripleAPlayer)
                ((TripleAPlayer) player).setFrame(frame);
        }
    }

    /**
     * Return an array of player types that can play on the server. This array must not contain any entries that could play on the client.
     */
    public String[] getServerPlayerTypes()
    {
        if (System.getProperties().getProperty("triplea.ai") != null)
            return new String[]
            {HUMAN_PLAYER_TYPE, COMPUTER_PLAYER_TYPE};
        else
            return new String[]
            {HUMAN_PLAYER_TYPE};
    }

}
