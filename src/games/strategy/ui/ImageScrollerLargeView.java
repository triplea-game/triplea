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

import games.strategy.triplea.ui.Active;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;
import java.util.List;

/**
 * 
 * @author Sean Bridges
 * @version 1.0
 * 
 * A large image that can be scrolled according to a ImageScrollController.
 * Generally used in conjunction with a ImageScrollerSmallView.
 * 
 * Notifies the controller when the component has been resized, or the image has
 * been scrolled when the user holds the mouse near the edge of the component.
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

    protected ImageScrollControl m_control;

    protected int m_x = 0;
    protected int m_y = 0;
    

    private int m_drag_scrolling_lastx;
    private int m_drag_scrolling_lasty;
    
    protected Dimension m_dimensions;

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

    /** Creates new ImageScroller */
    public ImageScrollerLargeView(Dimension dimensions)
    {
        super();

        
        m_dimensions = dimensions;

        setPreferredSize(m_dimensions);
        setMaximumSize(m_dimensions);

        addMouseWheelListener(MOUSE_WHEEL_LISTENER);
        addMouseListener(MOUSE_LISTENER);
        addMouseListener(MOUSE_LISTENER_DRAG_SCROLLING);
        addMouseMotionListener(MOUSE_MOTION_LISTENER);
        addMouseMotionListener(MOUSE_DRAG_LISTENER);
        addComponentListener(COMPONENT_LISTENER);

        m_timer.start();
    }

    // Beagle Code used to chnage map skin
    public void setDimensions(Dimension dimensions)
    {

        m_dimensions = new Dimension(dimensions);
    }

    /**
     * For subclasses needing to set the location of the image.
     */
    protected void setTopLeft(int x, int y)
    {
        int newX = x;
        //newX = checkBounds(newX, m_originalImage.getWidth(this),
        // this.getWidth(), true);

        int newY = y;
        newY = checkBounds(newY, (int)  m_dimensions.getHeight(), this.getHeight());

        setCoordsInternal(newX, newY);
    }

    protected void setTopLeftNoWrap(int x, int y)
    {
        int newX = x;
        newX = checkBounds(newX, (int)  m_dimensions.getWidth(), this.getWidth());

        int newY = y;
        newY = checkBounds(newY, (int)  m_dimensions.getHeight(), this.getHeight());

        setCoordsInternal(newX, newY);
    }

    /**
     * @param newX
     * @param newY
     */
    private void setCoordsInternal(int newX, int newY)
    {
        if(!m_control.getScrollWrapX())
        {
            if(newX < -getWidth() / 2)
            {
                newX = (int)  m_dimensions.getWidth() - getWidth() ;
            }
            else if(newX < 0)
            {
                newX =0;
            }
        }
        
        
        setCoords(newX, newY);
        m_control.setLargeCoords(newX, newY);
    }

    public int getImageWidth()
    {
        return (int) m_dimensions.getWidth();
    }

    public int getImageHeight()
    {
        return (int) m_dimensions.getHeight();
    }

    void setCoords(int x, int y)
    {
        m_x = x;
        m_y = y;

        notifyScollListeners();
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
            element.scrolled(m_x, m_y);
            
        }
    }
    

    private int checkBounds(int dim, int max, int width)
    {
        if (dim < 0)
            return 0;

        if (dim + width > max)
            return max - width;
        return dim;
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

        int newX = (m_x + dx);
        if (newX > (int) m_dimensions.getWidth() - getWidth())
            newX -= (int) m_dimensions.getWidth();
        if (newX < -getWidth())
            newX += (int) m_dimensions.getWidth();
        // newX = checkBounds(newX, m_originalImage.getWidth(this),
        // this.getWidth(), true);

        int newY = m_y + dy;
        newY = checkBounds(newY, (int) m_dimensions.getHeight(), this.getHeight());

        setCoordsInternal(newX, newY);

    }

    public Dimension getImageDimensions()
    {
        return new Dimension(m_dimensions);
    }

    void setController(ImageScrollControl control)
    {
        m_control = control;
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
            m_control.largeViewChangedSize();
        }
    };

    /**
     * used for the mouse wheel
     */
    private MouseWheelListener MOUSE_WHEEL_LISTENER = new MouseWheelListener()
    {

        public void mouseWheelMoved(MouseWheelEvent e)
        {
            int height = getHeight();
            //int width = getWidth();

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
            int newX = (m_x + dx);
            if (newX > (int)  m_dimensions.getWidth() - getWidth())
                newX -= (int)  m_dimensions.getWidth();
            if (newX < -getWidth())
                newX += (int)  m_dimensions.getWidth();

            //move up and down and test for edges
            int newY = m_y + dy;
            newY = checkBounds(newY,(int)  m_dimensions.getHeight(), height);

            //update the map
            setCoordsInternal(newX, newY);

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
                int dx = -(m_drag_scrolling_lastx - x) * ((int)  m_dimensions.getWidth()- width) / width;
                int dy = -(m_drag_scrolling_lasty - y) * ((int)  m_dimensions.getHeight() - height) / height;

                //move left and right and test for wrap
                int newX = (m_x + dx);
                if (newX > (int)  m_dimensions.getWidth() - getWidth())
                    newX -= (int)  m_dimensions.getWidth();
                if (newX < -getWidth())
                    newX += (int)  m_dimensions.getWidth();

                // newX = checkBounds(newX, m_originalImage.getWidth(this),
                // this.getWidth(), true);

                //move up and down and test for edges
                int newY = m_y + dy;
                newY = checkBounds(newY, (int)  m_dimensions.getHeight(), height);

                //update the map
                setCoordsInternal(newX, newY);

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
        return m_x;
    }

    public int getYOffset()
    {
        return m_y;
    }

    private class Scroller implements Runnable
    {
        public void run()
        {
            scroll();
        }
    }

    public void deactivate()
    {
        m_timer.stop();
        m_timer.removeActionListener(m_timerAction);
        
    }
}