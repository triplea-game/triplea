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
package games.puzzle.slidingtiles.ui;

import games.strategy.common.ui.MacWrapper;
import games.strategy.common.ui.MainGameFrame;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.framework.ClientGame;
import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.framework.IGame;
import games.strategy.engine.framework.ServerGame;
import games.strategy.engine.framework.startup.ui.MainFrame;
import games.strategy.engine.gamePlayer.IGamePlayer;
import games.strategy.engine.gamePlayer.IPlayerBridge;

import java.awt.Component;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

/**
 * Represents the puzzle board component.
 * 
 * @author Lane Schwartz
 * @version $LastChangedDate$
 */
public class NPuzzleFrame extends MainGameFrame
{
    private GameData m_data;
    private IGame m_game;
    
    private BoardData m_mapData;
    private BoardPanel m_mapPanel;
    private JLabel m_status;
    private JLabel m_error;

    private boolean m_gameOver;
    
    private CountDownLatch m_waiting;

    /**
     * Construct a new user interface for an N-Puzzle game.
     * 
     * @param game
     * @param players
     */
    public NPuzzleFrame(IGame game, Set<IGamePlayer> players)
    {
        m_gameOver = false;
        m_waiting = null;
        
        m_game = game;
        m_data = game.getData();

        // Get the dimension of the gameboard - specified in the game's xml file.
        int x_dim = m_data.getMap().getXDimension();
        int y_dim = m_data.getMap().getYDimension();

        // The MapData holds info for the map, 
        //    including the dimensions (x_dim and y_dim)
        //    and the size of each square (50 by 50)
        m_mapData = new BoardData(m_data, x_dim, y_dim, 50, 50);
        
        // MapPanel is the Swing component that actually displays the gameboard.
        m_mapPanel = new BoardPanel(m_mapData, (File) m_data.getProperties().get("Background"));

        // This label will display whose turn it is
        m_status = new JLabel(" ");
        m_status.setAlignmentX(Component.CENTER_ALIGNMENT);
        m_status.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));
        
        // This label will display any error messages
        m_error = new JLabel(" ");
        m_error.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // We need somewhere to put the map panel, status label, and error label
        JPanel mainPanel = new JPanel();
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.add(m_mapPanel);
        mainPanel.add(m_status);
        mainPanel.add(m_error);        
        this.setContentPane(mainPanel);

        // Set up the menu bar and window title
        this.setJMenuBar(new NPuzzleMenu(this));
        this.setTitle(m_game.getData().getGameName());
        
        // If a user tries to close this frame, treat it as if they have asked to leave the game
        this.addWindowListener(new WindowAdapter()
        {
            public void windowClosing(WindowEvent e)
            {
                leaveGame();
            }
        });
        
        // Resize the window, then make it visible
        this.pack();
        this.setResizable(false);
        this.setVisible(true);
    }   
    /**
     * Wait for a player to play.
     * 
     * @param player the player to wait on
     * @param bridge the bridge for player
     * @return PlayData representing a play, or <code>null</code> if the play was interrupted
     */
    public PlayData waitForPlay(final PlayerID player, final IPlayerBridge bridge)
    {
        PlayData play = null;
        //player.getName().endsWith("AI");
        try {
            while (play==null)
            {
                m_waiting = new CountDownLatch(1);

                play = m_mapPanel.waitForPlay(player,bridge,m_waiting);
            }
        }
        catch (InterruptedException e)
        {
            return null;
        }
        
        return play;
    }
    
    /**
     * Update the user interface based on a game play.
     */
    public void performPlay()
    {
        m_mapPanel.performPlay();
    }
    
    /**
     * Set up the tiles.
     */
    public void initializeTiles()
    {
        m_mapData.initializeTiles();
    }
    
    /**
     * Get the <code>IGame</code> for the current game.
     * @return the <code>IGame</code> for the current game
     */
    public IGame getGame()
    {
        return m_game;
    }
    /**
     * Process a user request to leave the game.
     */
    public void leaveGame() 
    {
        if (!m_gameOver)
        {
            // Make sure the user really wants to leave the game.
            int rVal = JOptionPane.showConfirmDialog(this, "Are you sure you want to leave?\nUnsaved game data will be lost.", "Exit" , JOptionPane.YES_NO_OPTION);
            if(rVal != JOptionPane.OK_OPTION)
                return;
        }
        
        // We need to let the MapPanel know that we're leaving the game.
        //    Once the CountDownLatch has counted down to zero,
        //    the MapPanel will stop listening for mouse clicks,
        //    and its thread will be able to terminate.
        if (m_waiting!=null)
        {
            synchronized(m_waiting)
            {
                while (m_waiting.getCount() > 0)
                    m_waiting.countDown();
            }
        }
       
        // Exit the game.
        if(m_game instanceof ServerGame)
        {
            ((ServerGame) m_game).stopGame();
        }
        else
        {   
            m_game.getMessenger().shutDown();
            ((ClientGame) m_game).shutDown();
            
            //an ugly hack, we need a better
            //way to get the main frame
            MainFrame.getInstance().clientLeftGame();
        }
    }
    
    
    /**
     * Process a user request to stop the game.
     * 
     * This method is responsible for de-activating this frame.
     */    
    public void stopGame()
    {        

        if(GameRunner.isMac()) 
        {
            //this frame should not handle shutdowns anymore
            MacWrapper.unregisterShutdownHandler();
        }
        this.setVisible(false);
        this.dispose();
    
        
        m_game = null;

        if(m_data != null)
            m_data.clearAllListeners();
        m_data = null;
              
        m_mapPanel = null;

        m_status = null;

        for (WindowListener l : this.getWindowListeners())
            this.removeWindowListener(l);
    }
   
    
    /**
     * Process a user request to exit the program.
     */
    public void shutdown()
    {   
        if (!m_gameOver)
        {
            int rVal = JOptionPane.showConfirmDialog(this, "Are you sure you want to exit?\nUnsaved game data will be lost.", "Exit" , JOptionPane.YES_NO_OPTION);
            if(rVal != JOptionPane.OK_OPTION)
                return;
        }
        System.exit(0);
    }
    
    
    /**
     * Set the game over status for this frame to <code>true</code>.
     */
    public void setGameOver()
    {
        m_gameOver = true;
    }
    
    
    /**
     * Determine whether the game is over.
     * @return <code>true</code> if the game is over, <code>false</code> otherwise
     */
    public boolean isGameOver()
    {
        return m_gameOver;
    }
    
    
    /**
     * Graphically notify the user of an error.
     * @param error the error message to display
     */
    public void notifyError(String error)
    {
        m_error.setText(error);
    }
    
    
    /**
     * Graphically notify the user of the current game status.
     * @param error the status message to display
     */    
    public void setStatus(String status)
    {
        m_error.setText(" ");
        m_status.setText(status);
    }
}
