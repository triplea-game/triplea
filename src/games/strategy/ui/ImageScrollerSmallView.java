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
 * Notifies the controller when the selected area has been moved.
 */
public class ImageScrollerSmallView extends JComponent
{

    private ImageScrollControl m_control;

    //width of the selection box
    private int m_selectionHeight = 50;
    private int m_selectionWidth = 50;
    //location of the selection width and height
    private int m_selectionX;
    private int m_selectionY;
    private Image m_image;

    /** Creates new ImageScrollControl */
    public ImageScrollerSmallView(Image image)
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

        this.setBorder(new EtchedBorder());

        int prefWidth = getInsetsWidth() + m_image.getWidth(this);
        int prefHeight = getInsetsHeight() + m_image.getHeight(this);
        Dimension prefSize = new Dimension(prefWidth, prefHeight);

        setPreferredSize(prefSize);
        setMinimumSize(prefSize);
        setMaximumSize(prefSize);

        this.addMouseListener(MOUSE_LISTENER);
        this.addMouseMotionListener(MOUSE_MOTION_LISTENER);
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

    //called by the controller
    void setSelectionBound(int width, int height)
    {
        m_selectionWidth = width;
        m_selectionHeight = height;
        repaint();
    }

    void setController(ImageScrollControl control)
    {
        m_control = control;
    }

    void setCoords(int x, int y)
    {
        m_selectionX = x;
        m_selectionY = y;
        repaint();
    }

    public Dimension getImageDimensions()
    {
        return Util.getDimension(m_image, this);
    }

    public void paint(Graphics g)
    {
        super.paint(g);
        int xOff = getInsets().left;
        int yOff = getInsets().top;

        g.drawImage(m_image, xOff, yOff, this);
        g.setColor(Color.white);
        drawViewBox(g);

    }

    private void drawViewBox(Graphics g)
    {
        int xOff = getInsets().left;
        int yOff = getInsets().top;

        g.drawRect(m_selectionX + xOff, m_selectionY + yOff, m_selectionWidth, m_selectionHeight);
        g.drawRect(m_selectionX + xOff + getWidth(), m_selectionY + yOff, m_selectionWidth, m_selectionHeight);
        g.drawRect(m_selectionX + xOff - getWidth(), m_selectionY + yOff, m_selectionWidth, m_selectionHeight);
    }

    public Image getOffScreenImage()
    {
        return m_image;
    }

    private void setSelection(int x, int y)
    {
        if (y + m_selectionHeight > getHeight() - getInsetsHeight())
            //take off 1 more so the rectangle will be visible
            y = getHeight() - m_selectionHeight - getInsetsHeight() - 1;
        //
        //		if(x + m_selectionWidth > getWidth() - getInsetsWidth())
        //			//take off 1 more so the rectangle will be visible
        //			x = getWidth() - m_selectionWidth - getInsetsWidth() - 1;
        //
        //		if(x < 0)
        //			x = 0;
        if (y < 0)
            y = 0;

        if (!m_control.getScrollWrapX())
        {
            if (x < 0)
                x = 0;
            if (x + m_selectionWidth >= getWidth() - getInsetsWidth() - 1)
                x = getWidth() - m_selectionWidth - getInsetsWidth() - 1;
        }

        m_selectionX = x;
        m_selectionY = y;
        m_control.setSmallCoords(x, y);
        repaint();
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
            int x = e.getX() - (m_selectionWidth / 2);
            int y = e.getY() - (m_selectionHeight / 2);

            setSelection(x, y);
        }
    };

    private final MouseAdapter MOUSE_LISTENER = new MouseAdapter()
    {
        public void mouseClicked(MouseEvent e)
        {
            //try to center around the click
            int x = e.getX() - (m_selectionWidth / 2);
            int y = e.getY() - (m_selectionHeight / 2);

            setSelection(x, y);
        }
    };
    
    public double getRatioX()
    {
        return m_control.getRatioX();
    }

    public double getRatioY()
    {
        return m_control.getRatioY();
    }

    
}