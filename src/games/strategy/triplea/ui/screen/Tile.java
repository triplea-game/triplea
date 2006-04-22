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
package games.strategy.triplea.ui.screen;

import games.strategy.engine.data.GameData;
import games.strategy.triplea.ui.MapData;
import games.strategy.triplea.util.Stopwatch;
import games.strategy.ui.Util;

import java.awt.*;
import java.lang.ref.SoftReference;
import java.util.*;
import java.util.List;
import java.util.logging.*;

/**
 * 
 *
 *
 * @author Sean Bridges
 */
public class Tile
{
    private static final boolean DRAW_DEBUG = false;
    private static final Logger s_logger = Logger.getLogger(Tile.class.getName());
    
    //allow the gc to implement memory management
    private SoftReference<Image> m_imageRef;
    private boolean m_isDirty = true;
    private final Rectangle m_bounds;
    private final int m_x;
    private final int m_y;
    
    private final Object m_mutex = new Object();
    
    private final List<IDrawable> m_contents = new ArrayList<IDrawable>();
    
    public Tile(final Rectangle bounds, int x, int y)
    {
        //s_logger.log(Level.FINER, "Tile created for:" + bounds);
        m_bounds = bounds;
        m_x = x;
        m_y = y;
        
    }
     
    public boolean isDirty()
    {
        synchronized(m_mutex)
        {
            return m_isDirty || m_imageRef == null || m_imageRef.get() == null;
        }
    }
    
    public Image getImage(GameData data, MapData mapData) 
    {
        synchronized(m_mutex)
        {
        
            if(m_imageRef == null)
            {
                m_imageRef = new SoftReference<Image>(Util.createImage((int) m_bounds.getWidth(), (int) m_bounds.getHeight(), false));
                m_isDirty = true;
            }
            
            Image image = m_imageRef.get();
            if(image == null)
            {
                image = Util.createImage((int) m_bounds.getWidth(), (int) m_bounds.getHeight(), false);
                m_imageRef = new SoftReference<Image>(image);
                m_isDirty = true;
            }
            
            if(m_isDirty)
            {
                Graphics g = image.getGraphics();
                draw((Graphics2D) g, data, mapData);
                g.dispose();
            }
            
            return image;
        }
        
    }
    
    
    /**
     * This image may be null, and it may not reflect our current drawables.  Use getImage() to get
     * a correct image
     * 
     * @return the image we currently have.
     * 
     */
    public Image getRawImage()
    {
        if(m_imageRef == null)
            return null;
        return m_imageRef.get();
    }
    
    private void draw(Graphics2D g, GameData data, MapData mapData)
    {
        Stopwatch stopWatch = new Stopwatch(s_logger, Level.FINEST, "Drawing Tile at" + m_bounds);
        
        //clear
        g.setColor(Color.BLACK);
        g.fill(new Rectangle(0,0,TileManager.TILE_SIZE, TileManager.TILE_SIZE));
     
        Collections.sort(m_contents, new DrawableComparator());
        Iterator<IDrawable> iter = m_contents.iterator();
    
        while (iter.hasNext())
        {
            IDrawable drawable = iter.next();
            drawable.draw(m_bounds, data, g, mapData);
        }
        m_isDirty = false;
        
        
        //draw debug graphics
        if(DRAW_DEBUG)
        {
            g.setColor(Color.PINK);
            Rectangle r = new Rectangle(1,1,TileManager.TILE_SIZE - 2, TileManager.TILE_SIZE -2);
            g.setStroke(new BasicStroke(1));
            g.draw(r);
            g.setFont(new Font("Ariel", Font.BOLD, 25));
            g.drawString(m_x + " " + m_y, 40,40);
        }
        
        stopWatch.done();
        
    }
    
    public void addDrawbles(Collection<IDrawable> drawables)
    {
        synchronized(m_mutex)
        {
            m_contents.addAll(drawables);
            m_isDirty = true;
        }        
    }
    
    public void addDrawable(IDrawable d)
    {
        synchronized(m_mutex)
        {
            m_contents.add(d);
            m_isDirty = true;
        }
    }
    
    public void removeDrawable(IDrawable d)
    {
        synchronized(m_mutex)
        {
            m_contents.remove(d);
            m_isDirty = true;
        }
    }
    public void removeDrawables(Collection c)
    {
        synchronized(m_mutex)
        {
            m_contents.removeAll(c);
            m_isDirty = true;
        }
    }
    
    public void clear()
    {
        synchronized(m_mutex)
        {
            m_contents.clear();
            m_isDirty = true;
        }
    }
    
    public List<IDrawable> getDrawables()
    {
        return m_contents;
    }
    
    public Rectangle getBounds()
    {
        return m_bounds;
    }
    
    public int getX()
    {
        return m_x;
    }
    
    public int getY()
    {
        return m_y;
    }
    
    public Object getMutex()
    {
        return m_mutex;
    }
    
}
