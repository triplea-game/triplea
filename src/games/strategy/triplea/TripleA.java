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

import games.strategy.engine.framework.*;
import games.strategy.engine.gamePlayer.IGamePlayer;
import games.strategy.triplea.image.TileImageFactory;
import games.strategy.triplea.player.ITripleaPlayer;
import games.strategy.triplea.sound.SoundPath;
import games.strategy.triplea.troxAI.TroxAIPlayer;
import games.strategy.triplea.ui.*;
import games.strategy.triplea.ui.display.*;
import games.strategy.triplea.ui.display.ITripleaDisplay;

import java.awt.Frame;
import java.io.IOException;
import java.util.*;

import javax.swing.SwingUtilities;

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
                players.add(new TroxAIPlayer(name));
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
            
	    /*
	       Retreive the map name from xml file
	       This is the key for triplea to find the maps
	    */
            String mapDir = game.getData().getProperties().get(Constants.MAP_NAME).toString();

            MapData.setMapDir(mapDir);                //tells TerritoryData where the txt files are
            TileImageFactory.setMapDir(mapDir);  //tells the image factory where the images are

            final TripleAFrame frame = new TripleAFrame(game, players);
           
            TripleaDisplay display = new TripleaDisplay(frame);
            game.addDisplay(display);

            frame.setVisible(true);

            while (!frame.isVisible())
            {
                Thread.yield();
            }
            //size when minimized
            
            SwingUtilities.invokeLater(
                new Runnable()
                {
                    public void run()
                    {
                        frame.setSize(700, 400);
                        frame.setExtendedState(Frame.MAXIMIZED_BOTH);
                    }
                }
            );

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
            IGamePlayer player = (IGamePlayer) iter.next();
            if (player instanceof TripleAPlayer)
                ((TripleAPlayer) player).setFrame(frame);
        }
    }

    /**
     * Return an array of player types that can play on the server. 
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

    /* 
     * @see games.strategy.engine.framework.IGameLoader#getDisplayType()
     */
    public Class getDisplayType()
    {
        return ITripleaDisplay.class;
    }

    
    public Class getRemotePlayerType()
    {
        return ITripleaPlayer.class;
    }
    
    
    
}
