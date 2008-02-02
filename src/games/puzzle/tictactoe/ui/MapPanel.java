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

package games.puzzle.tictactoe.ui;

import games.strategy.common.image.UnitImageFactory;
import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.events.GameDataChangeListener;
import games.strategy.engine.gamePlayer.IPlayerBridge;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import javax.swing.JComponent;

/**
 * Custom component for displaying a Tic Tac Toe gameboard and pieces.
 * 
 * @author Lane Schwartz
 * @version $LastChangedDate$
 */
public class MapPanel extends JComponent implements MouseListener
{
    private MapData m_mapData;
    private GameData m_gameData;
    
    private Territory m_clickedAt = null;
    
    private Map<Territory, Image> m_images;
    
    private CountDownLatch m_waiting;
    
    public MapPanel(MapData mapData)
    {
    	m_waiting = null;
    	
        m_mapData = mapData;
        m_gameData = m_mapData.getGameData();
        
        Dimension mapDimension = m_mapData.getMapDimensions();
        
        this.setMinimumSize(mapDimension);
        this.setPreferredSize(mapDimension);
        this.setSize(mapDimension);

        m_images = new HashMap<Territory, Image>();
        updateAllImages();
        
        this.addMouseListener(this);
        
        this.setOpaque(true);
        
        m_gameData.addDataChangeListener(new GameDataChangeListener()
        {
        
            public void gameDataChanged(Change change)
            {
                updateAllImages();
        
            }
        
        });
    }


    private void updateAllImages()
    {
        for (Territory at : m_mapData.getPolygons().keySet())
        {
            updateImage(at);
        }
        repaint();
    }
    
    
    /**
     * Get the size of the map.
     * 
     * @return the size of the map
     */
    public Dimension getMapDimensions()
    {
        return m_mapData.getMapDimensions();
    }
    
    
    /**
     * Update the user interface based on a game play.
     * 
     * @param start <code>Territory</code> where the moving piece began
     * @param end <code>Territory</code> where the moving piece ended
     * @param captured <code>Collection</code> of <code>Territory</code>s whose pieces were captured during the play
     */
    protected void performPlay(Territory at)
    {   

    }
    
    /**
     * Update the image for a <code>Territory</code> based on the contents of that <code>Territory</code>.
     * 
     * @param at the <code>Territory</code> to update
     */
    private void updateImage(Territory at)
    {
        if (at != null)
        {
            if (at.getUnits().size() == 1)
            {  
                UnitImageFactory f = new UnitImageFactory();
                // Get image for exactly one unit
                Unit u = (Unit) at.getUnits().getUnits().toArray()[0];
                Image image = f.getImage(u.getType(), u.getOwner(), m_gameData);
                m_images.put(at, image); 
            } else
            {
                m_images.remove(at);
            }
        }
    }
    
    
    /**
     * Draw the current map and pieces.
     */
    protected void paintComponent(Graphics g) 
    {   
        g.setColor(Color.white);
        g.fillRect(0, 0, getWidth(), getHeight());

        for (Map.Entry<Territory,Polygon> entry : m_mapData.getPolygons().entrySet())
        {   
            Polygon p = entry.getValue();
            Territory at = entry.getKey();
            Color backgroundColor = Color.WHITE;

            g.setColor(Color.black);
            Image image = m_images.get(at);

            if (image != null)
            {
                Rectangle square = p.getBounds(); 
                g.drawImage(image, square.x, square.y, square.width, square.height, backgroundColor, null);
            }

            g.drawPolygon(p);                

        }
    }
    

    public void mouseClicked(MouseEvent e) { }
    public void mouseEntered(MouseEvent e) { }
    public void mouseExited(MouseEvent e) { }
  
    /**
     * Process the mouse button being pressed.
     */
    public void mousePressed(MouseEvent e) 
    { 
    	// After this method has been called, 
        //    the Territory corresponding to the cursor location when the mouse was pressed 
        //    will be stored in the private member variable m_clickedAt.
    	m_clickedAt = m_mapData.getTerritoryAt(e.getX(), e.getY());
        
    	// The waitForPlay method is waiting for mouse input.
        //    Let it know that we have processed mouse input.
        if (m_waiting!=null)
            m_waiting.countDown(); 
    }
    
    /**
     * Process the mouse button being released.
     */
    public void mouseReleased(MouseEvent e) 
    {}
    
    
    /**
     * Wait for a player to play.
     * 
     * @param player the player to wait on
     * @param bridge the bridge for player
     * @param waiting a <code>CountDownLatch</code> used to wait for user input - must be non-null and have and have <code>getCount()==1</code>
     * @return PlayData representing a play, or <code>null</code> if the play started and stopped on the same <code>Territory</code>
     * @throws InterruptedException if the play was interrupted
     */
    public Territory waitForPlay(final PlayerID player, final IPlayerBridge bridge, CountDownLatch waiting) throws InterruptedException 
    {
    	// Make sure we have a valid CountDownLatch.
    	if (waiting==null || waiting.getCount()!=1)
    		throw new IllegalArgumentException("CountDownLatch must be non-null and have getCount()==1");
    		
    	// The mouse listeners need access to the CountDownLatch, so store as a member variable.
    	m_waiting = waiting;

    	// Wait for a play or an attempt to leave the game
    	m_waiting.await();

    	if (m_clickedAt==null)
    	{	
    		// If m_clickedAt==null,
    		//    the play is invalid and must have been interrupted.
    		//    So, reset the member variable, and throw an exception.
    		m_clickedAt = null;
    		throw new InterruptedException("Interrupted while waiting for play.");
    	}
    	else
    	{
    		// We have a valid play!
    		//    Reset the member variables, and return the play.
    		Territory play = m_clickedAt;
    		m_clickedAt = null;
    		return play;
    	}

    }

}
