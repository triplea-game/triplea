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
 *
 * Created on October 30, 2001, 6:57 PM
 */

package games.strategy.ui;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;
import java.util.*;

import javax.swing.JComponent;
import javax.swing.border.EtchedBorder;

/**
 * 
 * @author Sean Bridges
 * @version 1.0
 * 
 * A small image that tracks a selection area within a small image. Generally
 * used in conjunction with a ImageScrollerLarrgeView.
 * 
 */
public class ImageScrollerSmallView extends JComponent
{

    private final ImageScrollModel m_model;
    
    private Image m_image;

        
    public ImageScrollerSmallView(Image image, ImageScrollModel model)
    {
        m_model = model;
        try
        {
            Util.ensureImageLoaded(image, this);
            setDoubleBuffered(false);
        } catch (InterruptedException ie)
        {
            ie.printStackTrace();
        }
        m_image = image;

        this.setBorder(new EtchedBorder());

        int prefWidth = getInsetsWidth() + m_image.getWidth(this);
        int prefHeight = getInsetsHeight() + m_image.getHeight(this);
        Dimension prefSize = new Dimension(prefWidth, prefHeight);

        setPreferredSize(prefSize);
        setMinimumSize(prefSize);
        setMaximumSize(prefSize);

        this.addMouseListener(MOUSE_LISTENER);
        this.addMouseMotionListener(MOUSE_MOTION_LISTENER);
        model.addObserver(new Observer()
        {
        
            public void update(Observable o, Object arg)
            {
              repaint();          
            }
        
        });
    }

    public void changeImage(Image image)
    {
        try
        {
            Util.ensureImageLoaded(image, this);
            setDoubleBuffered(false);
        } catch (InterruptedException ie)
        {
            ie.printStackTrace();
        }
        m_image = image;
    }
    
    
  
    private int getInsetsWidth()
    {
        return getInsets().left + getInsets().right;
    }

    private int getInsetsHeight()
    {
        return getInsets().top + getInsets().bottom;
    }


    void setCoords(int x, int y)
    {
       m_model.set(x, y);
        
    }

    public Dimension getImageDimensions()
    {
        return Util.getDimension(m_image, this);
    }


    public void paintComponent(Graphics g)
    {
        g.drawImage(m_image, 0, 0, this);
        g.setColor(Color.white);
        
        drawViewBox((Graphics2D) g);

    }
    
    private void drawViewBox(Graphics2D g)
    {
        
        
        double ratioX = getRatioX();
        double ratioY = getRatioY();
        
        double x = m_model.getX() * ratioX;
        double y = m_model.getY() * ratioY;
        
        double width = m_model.getBoxWidth() * ratioX;
        double height = m_model.getBoxHeight() * ratioY;
        
        Rectangle2D.Double rect = new Rectangle2D.Double(x,y,width,height);
        g.draw(rect);
        
        if(m_model.getScrollX())
        {
            double mapWidth = m_model.getMaxWidth() * ratioX;
            
            rect.x += mapWidth;
            g.draw(rect);
            rect.x -= 2 * mapWidth;
            g.draw(rect);
        }
    }

    public Image getOffScreenImage()
    {
        return m_image;
    }

    private void setSelection(int x, int y)
    {
      m_model.set(x, y);
    }

    private long mLastUpdate = 0;
    private long MIN_UPDATE_DELAY = 30;

    private final MouseMotionListener MOUSE_MOTION_LISTENER = new MouseMotionAdapter()
    {
        public void mouseDragged(MouseEvent e)
        {

            long now = System.currentTimeMillis();
            if (now < mLastUpdate + MIN_UPDATE_DELAY)
                return;

            mLastUpdate = now;

            Rectangle bounds = (Rectangle) getBounds().clone();
            //if the mouse is a little off the screen, allow it to still scroll
            // the screen
            bounds.grow(30, 0);

            if (!bounds.contains(e.getPoint()))
                return;

            //try to center around the click
            int x = (int) (e.getX() / getRatioX()) - (m_model.getBoxWidth() / 2);
            int y = (int) (e.getY() / getRatioY()) - (m_model.getBoxHeight() / 2);

            setSelection(x, y);
        }
    };
    
   


    private final MouseAdapter MOUSE_LISTENER = new MouseAdapter()
    {
        public void mouseClicked(MouseEvent e)
        {
            //try to center around the click
            int x = (int) (e.getX() / getRatioX()) - (m_model.getBoxWidth() / 2);
            int y = (int) (e.getY() / getRatioY()) - (m_model.getBoxHeight() / 2);
            
            m_model.set(x, y);
        }
    };


    public double getRatioY()
    {
        return m_image.getHeight(null) / (double) m_model.getMaxHeight();
    }
    
    public double getRatioX()
    {
        return m_image.getWidth(null) / (double) m_model.getMaxWidth();
    }

    
    
}