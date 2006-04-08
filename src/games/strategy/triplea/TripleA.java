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
import games.strategy.engine.message.*;
import games.strategy.triplea.player.ITripleaPlayer;
import games.strategy.triplea.randomAI.RandomAI;
import games.strategy.triplea.sound.SoundPath;
import games.strategy.triplea.ui.TripleAFrame;
import games.strategy.triplea.ui.display.*;
import games.strategy.triplea.weakAI.WeakAI;

import java.awt.Frame;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

import javax.swing.SwingUtilities;

/**
 * @author Sean Bridges
 * @version 1.0
 */
public class TripleA implements IGameLoader
{
    private static final String HUMAN_PLAYER_TYPE = "Human";
    private static final String RANDOM_COMPUTER_PLAYER_TYPE = "Random AI";
    private static final String WEAK_COMPUTER_PLAYER_TYPE = "Easy AI";

    
    private transient TripleaDisplay m_display;
    private transient IGame m_game;

    /**
     * @see IGameLoader.createPlayers(playerNames)
     */
    public Set<IGamePlayer> createPlayers(Map playerNames)
    {
        Set<IGamePlayer> players = new HashSet<IGamePlayer>();
        Iterator iter = playerNames.keySet().iterator();
        while (iter.hasNext())
        {
            String name = (String) iter.next();
            String type = (String) playerNames.get(name);
            if (type.equals(RANDOM_COMPUTER_PLAYER_TYPE))
            {
                players.add(new RandomAI(name));
            }
            else if (type.equals(WEAK_COMPUTER_PLAYER_TYPE))
            {
                players.add(new WeakAI(name));
            }
            else if (type.equals(HUMAN_PLAYER_TYPE) || type.equals(CLIENT_PLAYER_TYPE))
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

    public void shutDown()
    {
        m_game.removeDisplay(m_display);
        m_display.shutDown();
        
    }
    
    public void startGame(final IGame game, final Set<IGamePlayer> players)
    {
        try
        {
            
	    /*
	       Retreive the map name from xml file
	       This is the key for triplea to find the maps
	    */
            m_game = game;
            final String mapDir = game.getData().getProperties().get(Constants.MAP_NAME).toString();



                SwingUtilities.invokeAndWait(new Runnable()
                {
                
                    public void run()
                    {
                        final TripleAFrame frame;
                        try
                        {
                            frame = new TripleAFrame(game, players);
                        } catch (IOException e)
                        {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                            System.exit(-1);
                            return;
                        }
                        
                        m_display = new TripleaDisplay(frame);
                        game.addDisplay(m_display);
                        frame.setSize(700, 400);
                        frame.setVisible(true);
                        connectPlayers(players, frame);
                        
                        SwingUtilities.invokeLater(
                            new Runnable()
                            {
                                public void run()
                                {
                                    frame.setExtendedState(Frame.MAXIMIZED_BOTH);
                                    frame.toFront();
                                }
                            }
                        );
                        
                    }
                
                });
            } catch (InterruptedException e)
            {
                e.printStackTrace();
            } catch (InvocationTargetException e)
            {
                e.printStackTrace();
            }
            

            

            //load the sounds in a background thread,
            //avoids the pause where sounds dont load right away
            Runnable loadSounds = new Runnable()
            {
                public void run()
                {
                    SoundPath.preLoadSounds();
                }
            };
            new Thread(loadSounds, "Triplea sound loader").start();

    }

    private void connectPlayers(Set<IGamePlayer> players, TripleAFrame frame)
    {
        Iterator<IGamePlayer> iter = players.iterator();
        while (iter.hasNext())
        {
            IGamePlayer player = iter.next();
            if (player instanceof TripleAPlayer)
                ((TripleAPlayer) player).setFrame(frame);
        }
    }

    /**
     * Return an array of player types that can play on the server. 
     */
    public String[] getServerPlayerTypes()
    {
        
        return new String[]
        {HUMAN_PLAYER_TYPE, WEAK_COMPUTER_PLAYER_TYPE};
            
    }

    /* 
     * @see games.strategy.engine.framework.IGameLoader#getDisplayType()
     */
    public Class<? extends IChannelSubscribor> getDisplayType()
    {
        return ITripleaDisplay.class;
    }

    
    public Class<? extends IRemote> getRemotePlayerType()
    {
        return ITripleaPlayer.class;
    }
    
    
    
}
