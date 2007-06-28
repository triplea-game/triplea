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

package games.puzzle.tictactoe;

import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.swing.SwingUtilities;

import games.strategy.engine.framework.IGame;
import games.strategy.engine.framework.IGameLoader;
import games.strategy.engine.gamePlayer.IGamePlayer;
import games.strategy.engine.message.IChannelSubscribor;
import games.strategy.engine.message.IRemote;

import games.puzzle.tictactoe.player.BetterAI;
import games.puzzle.tictactoe.player.ITicTacToePlayer;
import games.puzzle.tictactoe.player.RandomAI;
import games.puzzle.tictactoe.player.TicTacToePlayer;
import games.puzzle.tictactoe.ui.TicTacToeFrame;
import games.puzzle.tictactoe.ui.display.ITicTacToeDisplay;
import games.puzzle.tictactoe.ui.display.TicTacToeDisplay;


/**
 * Main class responsible for a Tic Tac Toe game.
 *
 * @author Lane Schwartz
 * @version $LastChangedDate$
 */
public class TicTacToe implements IGameLoader
{

    // When serializing, do not save transient member variables
    private transient TicTacToeDisplay m_display;
    private transient IGame m_game;
    
    private static final String HUMAN_PLAYER_TYPE = "Human";
    private static final String RANDOM_COMPUTER_PLAYER_TYPE = "Random AI";
    private static final String MINIMAX_COMPUTER_PLAYER_TYPE = "Minimax AI";
    private static final String ALPHABETA_COMPUTER_PLAYER_TYPE = "αβ AI";
    
        
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
            if (type.equals(HUMAN_PLAYER_TYPE) || type.equals(CLIENT_PLAYER_TYPE))
            {
                TicTacToePlayer player = new TicTacToePlayer(name);
                players.add(player);
            }
            else if (type.equals(RANDOM_COMPUTER_PLAYER_TYPE)) 
            {
                RandomAI ai = new RandomAI(name);
                players.add(ai);
            }
            else if (type.equals(MINIMAX_COMPUTER_PLAYER_TYPE)) 
            {
                BetterAI ai = new BetterAI(name, BetterAI.Algorithm.MINIMAX);
                players.add(ai);
            }
            else if (type.equals(ALPHABETA_COMPUTER_PLAYER_TYPE)) 
            {
                BetterAI ai = new BetterAI(name, BetterAI.Algorithm.ALPHABETA);
                players.add(ai);
            }
            else
            {
                throw new IllegalStateException("Player type not recognized:" + type);
            }
        }
        return players;
    }
    
    /**
     * Return an array of player types that can play on the server. 
     */
    public String[] getServerPlayerTypes()
    {
        return new String[]
        {HUMAN_PLAYER_TYPE, ALPHABETA_COMPUTER_PLAYER_TYPE, MINIMAX_COMPUTER_PLAYER_TYPE, RANDOM_COMPUTER_PLAYER_TYPE};
            
    }

    
    public void shutDown()
    {
        if(m_display != null) {
            m_game.removeDisplay(m_display);
            m_display.shutDown();
        }        
    }
    
    public void startGame(final IGame game, final Set<IGamePlayer> players) throws Exception
    {
        try
        {
            m_game = game;

            SwingUtilities.invokeAndWait(new Runnable()
            {
                public void run()
                {
                    final TicTacToeFrame frame = new TicTacToeFrame(game, players);

                    m_display = new TicTacToeDisplay(frame);
                    m_game.addDisplay(m_display);
                    frame.setVisible(true);
                    connectPlayers(players, frame);

                    SwingUtilities.invokeLater(
                            new Runnable()
                            {
                                public void run()
                                {
                                    //frame.setExtendedState(Frame.MAXIMIZED_BOTH);
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
            if(e.getCause() instanceof Exception)
                throw (Exception) e.getCause();
            else
            {
                e.printStackTrace();
                throw new IllegalStateException(e.getCause().getMessage());
            }

        }

    }

    private void connectPlayers(Set<IGamePlayer> players, TicTacToeFrame frame)
    {
        Iterator<IGamePlayer> iter = players.iterator();
        while (iter.hasNext())
        {
            IGamePlayer player = iter.next();
            if (player instanceof TicTacToePlayer)
                ((TicTacToePlayer) player).setFrame(frame);
        }
    }
    
    /** 
     * @see games.strategy.engine.framework.IGameLoader#getDisplayType()
     */
    public Class<? extends IChannelSubscribor> getDisplayType()
    {
        return ITicTacToeDisplay.class;
    }
    
    public Class<? extends IRemote> getRemotePlayerType()
    {
        return ITicTacToePlayer.class;
    }
}
