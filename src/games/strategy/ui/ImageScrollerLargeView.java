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

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.*;

/**
 *
 * @author  Sean Bridges
 * @version 1.0
 *
 * A large image that can be scrolled according to a ImageScrollController.
 * Generally used in conjunction with a ImageScrollerSmallView.
 *
 * Notifies the controller when the component has been resized, or the image has been
 * scrolled when the user holds the mouse near the edge of the component.
 *
 *
 * This class keeps tracks of three images, the original, an offscreen, and what is
 * currently being displayed.
 *
 * You can get the graphics context of the offscreen image by calling
 * getOffScreenGraphics().  Updates made here will not be displayed until
 * update has been called.
 *
 * The user can clear various parts of the offscreen with the original by
 * calling clearOffScreen(...). Updates are not reflected until update has been called.
 *
 * Since the above do not make any changes onscreen they are safe with respect to
 * the swing event thread.
 *
 * update() takes care of all isues related to the
 * swing event thread, and can be called from any thread.
 *
 */
public class ImageScrollerLargeView extends JComponent
{
  //bit flags for determining which way we are scrolling
  final static int NONE = 0;
  final static int LEFT = 1;
  final static int RIGHT = 2;
  final static int TOP = 4;
  final static int  BOTTOM = 8;

  //how close to an edge we have to be before we scroll
  private final static int TOLERANCE = 25;

  //how much we scroll
  private final static int SCROLL_DISTANCE = 30;

  protected ImageScrollControl m_control;

  private int m_x = 0;
  private int m_y = 0;
  private Image m_offscreenImage;

  private int m_drag_scrolling_lastx;
  private int m_drag_scrolling_lasty;

  private ActionListener mTimerAction = new ActionListener()
  {

      public final void actionPerformed(ActionEvent e)
      {
          if(JOptionPane.getFrameForComponent(ImageScrollerLargeView.this).getFocusOwner()  == null)
          {
              m_insideCount = 0;
              return;
          }

          if (m_inside && m_edge != NONE)
          {
              m_insideCount++;
              if (m_insideCount > 6)
              {
                  //we are in the timer thread, make sure the update occurs in the swing thread
                  SwingUtilities.invokeLater(new Scroller());
              }
          }
      }
  };


  //scrolling
  private javax.swing.Timer m_timer = new javax.swing.Timer(50,mTimerAction);
  private boolean m_inside = false;
  private int m_insideCount = 0;
  private int m_edge = NONE;

  /** Creates new ImageScroller */
  public ImageScrollerLargeView(Image image)
  {
    super();

    try
    {
      Util.ensureImageLoaded(image, this);
    } catch(InterruptedException ie)
    {
      ie.printStackTrace();
    }

    m_offscreenImage = image;

    setPreferredSize( Util.getDimension(m_offscreenImage, this));
    setMaximumSize( Util.getDimension(m_offscreenImage, this));

    addMouseWheelListener(MOUSE_WHEEL_LISTENER);
    addMouseListener(MOUSE_LISTENER);
	addMouseListener(MOUSE_LISTENER_DRAG_SCROLLING);
    addMouseMotionListener(MOUSE_MOTION_LISTENER);
	addMouseMotionListener(MOUSE_DRAG_LISTENER);
    addComponentListener(COMPONENT_LISTENER);

    m_timer.start();
    }

  /**
   * For subclasses needing to set the location
   * of the image.
   */
  protected void setTopLeft(int x, int y)
  {
    int newX = x;
    //newX = checkBounds(newX, m_originalImage.getWidth(this), this.getWidth(), true);

    int newY = y;
    newY = checkBounds(newY, m_offscreenImage.getHeight(this), this.getHeight(), false);

    setCoords(newX, newY);
    m_control.setLargeCoords(newX,newY);
  }

  protected void setTopLeftNoWrap(int x, int y)
  {
    int newX = x;
    newX = checkBounds(newX, m_offscreenImage.getWidth(this), this.getWidth(), false);

    int newY = y;
    newY = checkBounds(newY, m_offscreenImage.getHeight(this), this.getHeight(), false);

    setCoords(newX, newY);
    m_control.setLargeCoords(newX,newY);
  }

  public int getImageWidth()
  {
    return m_offscreenImage.getWidth(this);
  }

  public int getImageHeight()
  {
    return m_offscreenImage.getHeight(this);
  }


  void setCoords(int x, int y)
  {
    m_x = x;
    m_y = y;

    repaint();

  }

  private int checkBounds(int dim, int max, int width, boolean scrollThroughBorders)
  {
    if (!scrollThroughBorders)
    {
      if(dim < 0)
        return 0;

      if(dim + width > max)
        return max - width;
    }
    else
    {
      if (dim < 0)
        return dim + max - width;

      if (dim + width > max)
        return dim + width - max;
    }

    return dim;
  }

  private void scroll()
  {

    int dy = 0;
    if((m_edge & TOP) != 0)
      dy = -SCROLL_DISTANCE;
    else if((m_edge & BOTTOM) != 0)
      dy = SCROLL_DISTANCE;

    int dx = 0;
    if((m_edge & LEFT) != 0)
      dx = -SCROLL_DISTANCE;
    else if((m_edge & RIGHT) != 0)
      dx = SCROLL_DISTANCE;

    int newX = (m_x + dx);
    if(newX > m_offscreenImage.getWidth(this)-getWidth())
      newX -= m_offscreenImage.getWidth(this);
    if(newX < -getWidth())
      newX += m_offscreenImage.getWidth(this);
   // newX = checkBounds(newX, m_originalImage.getWidth(this), this.getWidth(), true);

    int newY = m_y + dy;
    newY = checkBounds(newY, m_offscreenImage.getHeight(this), this.getHeight(), false);

    setCoords(newX,newY);
    m_control.setLargeCoords(m_x,m_y);
  }

  public Dimension getImageDimensions()
  {
    return Util.getDimension(m_offscreenImage,this);
  }

  void setController(ImageScrollControl control)
  {
    m_control = control;
  }

  public void paint(Graphics g)
  {
    super.paint(g);

    //TODO what if the graphics is the same size as the image

    Rectangle center = new Rectangle(m_x,m_y, getWidth(),  getHeight());
    Rectangle left = new Rectangle(m_x - m_offscreenImage.getWidth(this) ,m_y, getWidth(),  getHeight());
    Rectangle right = new Rectangle(m_x + m_offscreenImage.getWidth(this) ,m_y,  getWidth(),  getHeight());



    drawVisible(g, center);
    drawVisible(g, left);
    drawVisible(g, right);
  }

  private void drawVisible(Graphics g, Rectangle center)
  {
    Rectangle visible = new Rectangle(0,0, m_offscreenImage.getWidth(this), m_offscreenImage.getHeight(this));
    Rectangle intersection = center.intersection(visible);


    if(intersection.getWidth() == 0)
      return;

    int x = intersection.x == center.x ? 0 : (int) center.getWidth() - (int) intersection.getWidth();

    g.drawImage(m_offscreenImage,
                x,
                0,
                x + (int) intersection.getWidth(),
                0 + (int) intersection.getHeight(),

                intersection.x,
                intersection.y,
                intersection.x + (int) intersection.getWidth(),
                intersection.y + (int) intersection.getHeight(),
                this);
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

  private int getNewEdge(int x, int y , int width, int height)
  {
    int newEdge = NONE;

    if(x < TOLERANCE)
      newEdge += LEFT;
    else if(width - x < TOLERANCE)
      newEdge += RIGHT;

    if(y < TOLERANCE)
      newEdge += TOP;
    else if(height - y < TOLERANCE)
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
			m_inside = true;

			int height = getHeight();
			int width = getWidth();

			if(m_edge == NONE)
			  m_insideCount = 0;

			//compute the amount to move
			int dx = 0;
			int dy = e.getWheelRotation()*50;

			//move left and right and test for wrap
			int newX = (m_x + dx);
			if(newX > m_offscreenImage.getWidth(ImageScrollerLargeView.this)-getWidth())
			  newX -= m_offscreenImage.getWidth(ImageScrollerLargeView.this);
			if(newX < -getWidth())
			  newX += m_offscreenImage.getWidth(ImageScrollerLargeView.this);

			//move up and down and test for edges
			int newY = m_y + dy;
			newY = checkBounds(newY, m_offscreenImage.getHeight(ImageScrollerLargeView.this), height, false);

			//update the map
			setCoords(newX,newY);
			m_control.setLargeCoords(m_x,m_y);

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
		if(now < m_LastUpdate + MIN_UPDATE_DELAY)
		  return;

		m_LastUpdate = now;

		//the right button must be the one down
		if ( (e.getModifiers() & MouseEvent.BUTTON3_MASK) != 0) {
			m_inside = true;

			//read in location
			int x = e.getX();
			int y = e.getY();

			int height = getHeight();
			int width = getWidth();

			if(m_edge == NONE)
			  m_insideCount = 0;

			//compute the amount to move
			int dx =-( m_drag_scrolling_lastx-x)*(m_offscreenImage.getWidth(ImageScrollerLargeView.this)-width)/width;
			int dy =-( m_drag_scrolling_lasty-y)*(m_offscreenImage.getHeight(ImageScrollerLargeView.this)-height)/height;

			//move left and right and test for wrap
			int newX = (m_x + dx);
			if(newX > m_offscreenImage.getWidth(ImageScrollerLargeView.this)-getWidth())
			  newX -= m_offscreenImage.getWidth(ImageScrollerLargeView.this);
			if(newX < -getWidth())
			  newX += m_offscreenImage.getWidth(ImageScrollerLargeView.this);

			  
		   // newX = checkBounds(newX, m_originalImage.getWidth(this), this.getWidth(), true);

			//move up and down and test for edges
			int newY = m_y + dy;
			newY = checkBounds(newY, m_offscreenImage.getHeight(ImageScrollerLargeView.this), height, false);

			//update the map
			setCoords(newX,newY);
			m_control.setLargeCoords(m_x,m_y);

			//store the location of the mouse for the next move
			m_drag_scrolling_lastx=e.getX();
			m_drag_scrolling_lasty=e.getY();
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

      m_edge = getNewEdge(x,y,width,height);

      if(m_edge == NONE)
        m_insideCount = 0;
    }
  };


  /**
   * Update will not be seen until update is called.
   * Resets the offscreen image to the original.
   */


  /**
   * Updates will not appear until update has been called.
   * Updates made here can be made outside the swing event thread.
   */
  public Graphics getOffscreenGraphics()
  {
    return m_offscreenImage.getGraphics();
  }

  public Image getOffscreenImage()
  {
      return m_offscreenImage;
  }


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
}
