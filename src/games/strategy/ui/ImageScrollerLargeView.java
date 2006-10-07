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
 * Created on October 30, 2001, 6:17 PM
 */

package games.strategy.ui;

import java.awt.Dimension;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;

/**
 * 
 * @author Sean Bridges
 * @version 1.0
 * 
 * A large image that can be scrolled according to a ImageScrollModel.
 * Generally used in conjunction with a ImageScrollerSmallView.
 * 
 * 
 * We do not take care of drawing ourselves.  All we do is keep track of 
 * our location and size.  Subclasses must take care of rendering 
 *  
 */
public class ImageScrollerLargeView extends JComponent 
{
    //bit flags for determining which way we are scrolling
    final static int NONE = 0;
    final static int LEFT = 1;
    final static int RIGHT = 2;
    final static int TOP = 4;
    final static int BOTTOM = 8;

    final static int WHEEL_SCROLL_AMOUNT = 50;

    //how close to an edge we have to be before we scroll
    private final static int TOLERANCE = 25;

    //how much we scroll
    private final static int SCROLL_DISTANCE = 30;

    protected final ImageScrollModel m_model;
    
    protected double m_scale = 1;
    
    private int m_drag_scrolling_lastx;
    private int m_drag_scrolling_lasty;
    

    private ActionListener m_timerAction = new ActionListener()
    {

        public final void actionPerformed(ActionEvent e)
        {
            if (JOptionPane.getFrameForComponent(ImageScrollerLargeView.this).getFocusOwner() == null)
            {
                m_insideCount = 0;
                return;
            }
            if (m_inside && m_edge != NONE)
            {
                m_insideCount++;
                if (m_insideCount > 6)
                {
                    //we are in the timer thread, make sure the update occurs
                    // in the swing thread
                    SwingUtilities.invokeLater(new Scroller());
                }
            }
        }
    };

    //scrolling
    private javax.swing.Timer m_timer = new javax.swing.Timer(50, m_timerAction);
    private boolean m_inside = false;
    private int m_insideCount = 0;
    private int m_edge = NONE;
    private List<ScrollListener> m_scrollListeners = new ArrayList<ScrollListener>();

    /** Creates new ImageScroller 
     *  */
    public ImageScrollerLargeView(Dimension dimension, ImageScrollModel model)
    {
        super();

        m_model = model;
        m_model.setMaxBounds((int)dimension.getWidth(), (int)dimension.getHeight());
        

        setPreferredSize(getImageDimensions());
        setMaximumSize(getImageDimensions());

        addMouseWheelListener(MOUSE_WHEEL_LISTENER);
        addMouseListener(MOUSE_LISTENER);
        addMouseListener(MOUSE_LISTENER_DRAG_SCROLLING);
        addMouseMotionListener(MOUSE_MOTION_LISTENER);
        addMouseMotionListener(MOUSE_DRAG_LISTENER);
        addComponentListener(COMPONENT_LISTENER);

        m_timer.start();
        
        m_model.addObserver(new Observer()
        {
        
            public void update(Observable o, Object arg)
            {
               repaint();
               notifyScollListeners();        
            }
        
        });
    }


    /**
     * For subclasses needing to set the location of the image.
     */
    protected void setTopLeft(int x, int y)
    {
        m_model.set(x, y);
    }

    protected void setTopLeftNoWrap(int x, int y)
    {
        if(x < 0)
            x = 0;
        if(y < 0)
            y = 0;
        m_model.set(x, y);
    }

   

    public int getImageWidth()
    {
        return m_model.getMaxWidth();
    }

    public int getImageHeight()
    {
        return m_model.getMaxHeight();
    }

    
    
    public void addScrollListener(ScrollListener s)
    {
        m_scrollListeners.add(s);
    }
    
    public void removeScrollListener(ScrollListener s)
    {
        m_scrollListeners.remove(s);
    }
    
    private void notifyScollListeners()
    {
        Iterator<ScrollListener> iter = new ArrayList<ScrollListener>(m_scrollListeners).iterator();
        while (iter.hasNext())
        {
            ScrollListener element = iter.next();
            element.scrolled(m_model.getX(), m_model.getY());
            
        }
    }
    

    private void scroll()
    {

        int dy = 0;
        if ((m_edge & TOP) != 0)
            dy = -SCROLL_DISTANCE;
        else if ((m_edge & BOTTOM) != 0)
            dy = SCROLL_DISTANCE;

        int dx = 0;
        if ((m_edge & LEFT) != 0)
            dx = -SCROLL_DISTANCE;
        else if ((m_edge & RIGHT) != 0)
            dx = SCROLL_DISTANCE;

        dx = (int)  (dx / m_scale);
        dy = (int)  (dy / m_scale);
        
        int newX = (m_model.getX() + dx);
        

        int newY = m_model.getY() + dy;
        

        m_model.set(newX, newY);

    }

    public Dimension getImageDimensions()
    {
        return new Dimension(m_model.getMaxWidth(), m_model.getMaxHeight());
    }

  


    private MouseAdapter MOUSE_LISTENER = new MouseAdapter()
    {
        public void mouseEntered(MouseEvent e)
        {
            m_timer.start();
        }

        public void mouseExited(MouseEvent e)
        {
            m_inside = false;
            m_timer.stop();
        }
    };

    private int getNewEdge(int x, int y, int width, int height)
    {
        int newEdge = NONE;

        if (x < TOLERANCE)
            newEdge += LEFT;
        else if (width - x < TOLERANCE)
            newEdge += RIGHT;

        if (y < TOLERANCE)
            newEdge += TOP;
        else if (height - y < TOLERANCE)
            newEdge += BOTTOM;

        return newEdge;
    }

    private ComponentListener COMPONENT_LISTENER = new ComponentAdapter()
    {
        public void componentResized(ComponentEvent e)
        {
            refreshBoxSize();
        }

    };
    
    protected void refreshBoxSize()
    {
        m_model.setBoxDimensions( (int) (getWidth() / m_scale), (int) (getHeight() / m_scale));
    }

    public void setScale(double scale)
    {
        m_scale = scale;
        refreshBoxSize();
    }

    /**
     * used for the mouse wheel
     */
    private MouseWheelListener MOUSE_WHEEL_LISTENER = new MouseWheelListener()
    {

        public void mouseWheelMoved(MouseWheelEvent e)
        {

            if (m_edge == NONE)
                m_insideCount = 0;

            //compute the amount to move
            int dx = 0;
            int dy = 0;

            if ((e.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) == InputEvent.SHIFT_DOWN_MASK)
                dx = e.getWheelRotation() * WHEEL_SCROLL_AMOUNT;
            else
                dy = e.getWheelRotation() * WHEEL_SCROLL_AMOUNT;

            //move left and right and test for wrap
            int newX = (m_model.getX() + dx);
            if (newX > (int)  m_model.getMaxWidth() - getWidth())
                newX -= (int)  m_model.getMaxWidth();
            if (newX < -getWidth())
                newX += (int)  m_model.getMaxWidth();

            //move up and down and test for edges
            int newY = m_model.getY() + dy;
            

            //update the map
            m_model.set(newX, newY);

        }
    };

    /**
     * this is used to detect dragscrolling
     */
    private MouseMotionListener MOUSE_DRAG_LISTENER = new MouseMotionAdapter()
    {
        private long m_LastUpdate = 0; //time since the last update
        private long MIN_UPDATE_DELAY = 10; //the fastest we allow it to update

        public void mouseDragged(MouseEvent e)
        {
            //this is to make sure we don't update too soon
            long now = System.currentTimeMillis();
            if (now < m_LastUpdate + MIN_UPDATE_DELAY)
                return;

            m_LastUpdate = now;

            //the right button must be the one down
            if ((e.getModifiers() & InputEvent.BUTTON3_MASK) != 0)
            {
                m_inside = false;

                //read in location
                int x = e.getX();
                int y = e.getY();

                int height = getHeight();
                int width = getWidth();

                if (m_edge == NONE)
                    m_insideCount = 0;

                //compute the amount to move
                int dx = -(m_drag_scrolling_lastx - x) * ((int)  m_model.getMaxWidth()- width) / width;
                int dy = -(m_drag_scrolling_lasty - y) * ((int)  m_model.getMaxHeight() - height) / height;

                //move left and right and test for wrap
                int newX = (m_model.getX() + dx);
                if (newX > (int)  m_model.getMaxWidth() - getWidth())
                    newX -= (int)  m_model.getMaxHeight();
                if (newX < -getWidth())
                    newX += (int)  m_model.getMaxHeight();

                // newX = checkBounds(newX, m_originalImage.getWidth(this),
                // this.getWidth(), true);

                //move up and down and test for edges
                int newY = m_model.getY() + dy;
                

                //update the map
                m_model.set(newX, newY);

                //store the location of the mouse for the next move
                m_drag_scrolling_lastx = e.getX();
                m_drag_scrolling_lasty = e.getY();
            }
        }
    };

    private final MouseAdapter MOUSE_LISTENER_DRAG_SCROLLING = new MouseAdapter()
    {
        public void mousePressed(MouseEvent e)
        {
            //try to center around the click
            m_drag_scrolling_lastx = e.getX();
            m_drag_scrolling_lasty = e.getY();
        }
    };

    private MouseMotionListener MOUSE_MOTION_LISTENER = new MouseMotionAdapter()
    {
        public void mouseMoved(MouseEvent e)
        {
            m_inside = true;

            int x = e.getX();
            int y = e.getY();

            int height = getHeight();
            int width = getWidth();

            m_edge = getNewEdge(x, y, width, height);

            if (m_edge == NONE)
                m_insideCount = 0;
        }
    };

    /**
     * Update will not be seen until update is called. Resets the offscreen
     * image to the original.
     */





    public int getXOffset()
    {
        return m_model.getX();
    }

    public int getYOffset()
    {
        return m_model.getY();
    }

    private class Scroller implements Runnable
    {
        public void run()
        {
            scroll();
        }
    }
    
    protected double getScaledWidth()
    {
        return getWidth() / m_scale;
    }
    
    protected double getScaledHeight()
    {
        return getHeight() / m_scale;
    }


    public void deactivate()
    {
        m_timer.stop();
        m_timer.removeActionListener(m_timerAction);
        
    }
}