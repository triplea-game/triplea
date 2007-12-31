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

import games.puzzle.slidingtiles.attachments.Tile;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.gamePlayer.IPlayerBridge;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;

/**
 * Custom component for displaying a n-puzzle gameboard.
 * 
 * @author Lane Schwartz
 * @version $LastChangedDate: 2007-06-27 19:20:21 -0500 (Wed, 27 Jun 2007) $
 */
public class BoardPanel extends JComponent implements MouseListener
{
    private BoardData m_mapData;
    
    private Territory m_clickedAt = null;
    private Territory m_releasedAt = null;
    
    private CountDownLatch m_waiting;
    
    private BufferedImage m_image;
    
    public BoardPanel(BoardData mapData, File file)
    {
    	m_waiting = null;
    	
        m_mapData = mapData;
        
        Dimension mapDimension = m_mapData.getMapDimensions();
        
        this.setMinimumSize(mapDimension);
        this.setPreferredSize(mapDimension);
        this.setSize(mapDimension);

        this.addMouseListener(this);
        
        this.setOpaque(true);

        if (file!=null) 
        {
            try {
                BufferedImage bigimage = ImageIO.read(file);
                AffineTransform trans = new AffineTransform();
                
                double scalex = mapDimension.getWidth() / bigimage.getWidth();
                double scaley = mapDimension.getHeight() / bigimage.getHeight();
                trans.scale(scalex, scaley);
                AffineTransformOp scale = new AffineTransformOp(trans, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
                
                m_image = new BufferedImage((int)mapDimension.getWidth(), (int)mapDimension.getHeight(), bigimage.getType());
                scale.filter(bigimage, m_image);

            } catch (IOException e)
            {
                m_image = null;
            }    
        }
        else
        {
            m_image = null;
        }

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
     * Update the user interface to reflect a game play.
     */
    protected void performPlay()
    {   
        // Ask Swing to repaint this panel when it's convenient
        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                repaint();
            }
        });
    }

    
    /**
     * Draw the current map and pieces.
     */
    protected void paintComponent(Graphics g) 
    {   
        g.setColor(Color.white);
        
        Dimension mapDimension = m_mapData.getMapDimensions();
        g.fillRect(0, 0, mapDimension.width, mapDimension.height);

        //g.fillRect(0, 0, getWidth(), getHeight());

        for (Map.Entry<Territory,Polygon> entry : m_mapData.getPolygons().entrySet())
        {   
            Polygon p = entry.getValue();
            Territory at = entry.getKey();
            Tile tile = (Tile) at.getAttachment("tile");
            if (tile!=null)
            {
                int value = tile.getValue();

                if (value != 0)
                {
                    Rectangle square = p.getBounds(); 
                    
                    Rectangle tileData = m_mapData.getLocation(value);
                    
                    if (m_image==null)
                    {
                        g.setColor(Color.black);
                        g.drawString(Integer.toString(value), square.x+(square.width*5/12), square.y+(square.height*7/12));
                    }
                    else if (tileData!=null)
                    {
                        g.drawImage(m_image, square.x, square.y, square.x+square.width, square.y+square.height, tileData.x, tileData.y, tileData.x+tileData.width, tileData.y+tileData.height, this);
                    }
                    else
                    {
                        g.setColor(Color.white);
                        g.fillRect(square.x, square.y, square.width, square.height);
                    }

                }
            }

            g.setColor(Color.black);
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
    }
    
    /**
     * Process the mouse button being released.
     */
    public void mouseReleased(MouseEvent e) 
    { 
        // After this method has been called, 
        //    the Territory corresponding to the cursor location when the mouse was released 
        //    will be stored in the private member variable m_releasedAt.
        m_releasedAt = m_mapData.getTerritoryAt(e.getX(), e.getY());

        // The waitForPlay method is waiting for mouse input.
        //    Let it know that we have processed mouse input.
        if (m_waiting!=null)
            m_waiting.countDown();      
    }
    
    
    /**
     * Wait for a player to play.
     * 
     * @param player the player to wait on
     * @param bridge the bridge for player
     * @param waiting a <code>CountDownLatch</code> used to wait for user input - must be non-null and have and have <code>getCount()==1</code>
     * @return PlayData representing a play, or <code>null</code> if the play started and stopped on the same <code>Territory</code>
     * @throws InterruptedException if the play was interrupted
     */
    public PlayData waitForPlay(final PlayerID player, final IPlayerBridge bridge, CountDownLatch waiting) throws InterruptedException 
    {
        // Make sure we have a valid CountDownLatch.
        if (waiting==null || waiting.getCount()!=1)
            throw new IllegalArgumentException("CountDownLatch must be non-null and have getCount()==1");
            
        // The mouse listeners need access to the CountDownLatch, so store as a member variable.
        m_waiting = waiting;

        // Wait for a play or an attempt to leave the game
        m_waiting.await();

        if (m_clickedAt==null || m_releasedAt==null)
        {   
            // If either m_clickedAt==null or m_releasedAt==null,
            //    the play is invalid and must have been interrupted.
            //    So, reset the member variables, and throw an exception.
            m_clickedAt = null;
            m_releasedAt = null;
            throw new InterruptedException("Interrupted while waiting for play.");
        }
        else
        {
            // We have a valid play!
            //    Reset the member variables, and return the play.
            PlayData play = new PlayData(m_clickedAt, m_releasedAt);
            m_clickedAt = null;
            m_releasedAt = null;
            return play;
        }

    }

}
