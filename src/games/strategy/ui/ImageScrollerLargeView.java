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
public class ImageScrollerLargeView extends JComponent implements ActionListener
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

	private ImageScrollControl m_control;
	
	private int m_x = 0;
	private int m_y = 0;
	private Image m_image;
	private Image m_originalImage;
	private Image m_offscreenImage;

	//scrolling
	private javax.swing.Timer m_timer = new javax.swing.Timer(50,this);
	private boolean m_inside = false;
	private int m_insideCount = 0;
	private int m_edge = NONE;
	
	/** Creates new ImageScroller */
    public ImageScrollerLargeView(Image image) 
	{
		super();
		this.setDoubleBuffered(false);
		try
		{
			Util.ensureImageLoaded(image, this);
		} catch(InterruptedException ie)
		{
			ie.printStackTrace();
		}
				
		m_originalImage = image;
		m_offscreenImage = Util.copyImage(image, this);
		
		setPreferredSize( Util.getDimension(m_originalImage, this));
		setMaximumSize( Util.getDimension(m_originalImage, this));
		
		addMouseListener(MOUSE_LISTENER);
		addMouseMotionListener(MOUSE_MOTION_LISTENER);
		addComponentListener(COMPONENT_LISTENER);
		
		m_timer.start();
    }

	/**
	 * For subclasses needing to set the loaction
	 * of the image.
	 */
	protected void setTopLeft(int x, int y)
	{
		int newX = x;
		newX = checkBounds(newX, m_originalImage.getWidth(this), this.getWidth(), true);
		
		int newY = y;
		newY = checkBounds(newY, m_originalImage.getHeight(this), this.getHeight(), false);
		
		setCoords(newX, newY);
		m_control.setLargeCoords(newX,newY);		
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

		int newX = m_x + dx;
		newX = checkBounds(newX, m_originalImage.getWidth(this), this.getWidth(), true);
		
		int newY = m_y + dy;
		newY = checkBounds(newY, m_originalImage.getHeight(this), this.getHeight(), false);
		
		setCoords(newX,newY);
		m_control.setLargeCoords(m_x,m_y);		
	}
	
	public Dimension getImageDimensions()
	{
		return Util.getDimension(m_originalImage,this);
	}
	
	void setController(ImageScrollControl control)
	{
		m_control = control;
	}
	
	public void paint(Graphics g)
	{
		super.paint(g);
		
		//TODO what if the graphics is the same size as the image
		Rectangle screen = g.getClipBounds();
		
		g.drawImage(m_offscreenImage, 0,0,  getWidth(),getHeight(),
		            m_x,m_y, m_x + getWidth(), m_y + getHeight(), this);
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
	
	public final void actionPerformed(ActionEvent e) 
	{		
		if(m_inside && m_edge != NONE)
		{
			m_insideCount++;
			if(m_insideCount > 6)
			{
				//we are in the timer thread, make sure the update occurs in the swing thread
				SwingUtilities.invokeLater(new Scroller());	
			}
		}			
	}
	
	//for subclasses
	/**
	 * Replaces a section of the offscreen image with the original.
	 */
	public void clearOffscreen(int x, int y, int height, int width)
	{
		m_offscreenImage.getGraphics().drawImage(m_originalImage, x,y,height, width, x,y,width,height,this);
	}
	
	/**
	 * Update will not be seen until update is called.
	 * Resets the offscreen image to the original.
	 */
	public void clearOffscreen()
	{
		m_offscreenImage.getGraphics().drawImage(m_originalImage, 0,0,this);
	}
	
	/**
	 * Updates will not appear until update has been called. 
	 * Updates made here can be made outside the swing event thread.
	 */
	public Graphics getOffscreenGraphics()
	{
		return m_offscreenImage.getGraphics();
	}
	
	/**
	 * Copies the offscreen graphics to the screen.
	 * Thread safe.  Can be called outside the swing event thread.
	 */
	public synchronized void update()
	{
		//copying the offscreen image onscreen must occur within the 
		//swing event thread, otherwise m_image
		//could be altered while it was being drawn
		if(SwingUtilities.isEventDispatchThread())
			updateOnScreen();
		else
			SwingUtilities.invokeLater(
				new Runnable() 
					{
						public void run()
						{
							updateOnScreen();
						}
					}
			);
	}
	
	/**
	 * Must be called from the event thread.
	 */
	private void updateOnScreen()
	{
		//int height = m_originalImage.getHeight(this);
		//int width = m_originalImage.getWidth(this);
		
		//m_image.getGraphics().drawImage(m_offscreenImage, 0,0,this);
		
		//m_image.getGraphics().drawImage(m_offscreenImage, 0,0,height, width, 
		  //                              0,0,width,height,this);
		repaint();
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