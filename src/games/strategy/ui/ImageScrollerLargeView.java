/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
/*
 * 
 * Created on October 30, 2001, 6:17 PM
 */
package games.strategy.ui;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * 
 * @author Sean Bridges
 * @version 1.0
 * 
 *          A large image that can be scrolled according to a ImageScrollModel.
 *          Generally used in conjunction with a ImageScrollerSmallView.
 * 
 * 
 *          We do not take care of drawing ourselves. All we do is keep track of
 *          our location and size. Subclasses must take care of rendering
 * 
 */
public class ImageScrollerLargeView extends JComponent
{
	// bit flags for determining which way we are scrolling
	final static int NONE = 0;
	final static int LEFT = 1;
	final static int RIGHT = 2;
	final static int TOP = 4;
	final static int BOTTOM = 8;
	final static int WHEEL_SCROLL_AMOUNT = 50;
	// how close to an edge we have to be before we scroll
	private final static int TOLERANCE = 25;
	// how much we scroll
	private final static int SCROLL_DISTANCE = 30;
	protected final ImageScrollModel m_model;
	protected double m_scale = 1;
	private int m_drag_scrolling_lastx;
	private int m_drag_scrolling_lasty;
	private final ActionListener m_timerAction = new ActionListener()
	{
		public final void actionPerformed(final ActionEvent e)
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
					// we are in the timer thread, make sure the update occurs
					// in the swing thread
					SwingUtilities.invokeLater(new Scroller());
				}
			}
		}
	};
	// scrolling
	private final javax.swing.Timer m_timer = new javax.swing.Timer(50, m_timerAction);
	private boolean m_inside = false;
	private int m_insideCount = 0;
	private int m_edge = NONE;
	private final List<ScrollListener> m_scrollListeners = new ArrayList<ScrollListener>();
	
	/**
	 * Creates new ImageScroller
	 * */
	public ImageScrollerLargeView(final Dimension dimension, final ImageScrollModel model)
	{
		super();
		m_model = model;
		m_model.setMaxBounds((int) dimension.getWidth(), (int) dimension.getHeight());
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
			public void update(final Observable o, final Object arg)
			{
				repaint();
				notifyScollListeners();
			}
		});
	}
	
	/**
	 * For subclasses needing to set the location of the image.
	 */
	protected void setTopLeft(final int x, final int y)
	{
		m_model.set(x, y);
	}
	
	protected void setTopLeftNoWrap(int x, int y)
	{
		if (x < 0)
			x = 0;
		if (y < 0)
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
	
	public void addScrollListener(final ScrollListener s)
	{
		m_scrollListeners.add(s);
	}
	
	public void removeScrollListener(final ScrollListener s)
	{
		m_scrollListeners.remove(s);
	}
	
	private void notifyScollListeners()
	{
		final Iterator<ScrollListener> iter = new ArrayList<ScrollListener>(m_scrollListeners).iterator();
		while (iter.hasNext())
		{
			final ScrollListener element = iter.next();
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
		dx = (int) (dx / m_scale);
		dy = (int) (dy / m_scale);
		final int newX = (m_model.getX() + dx);
		final int newY = m_model.getY() + dy;
		m_model.set(newX, newY);
	}
	
	public Dimension getImageDimensions()
	{
		return new Dimension(m_model.getMaxWidth(), m_model.getMaxHeight());
	}
	
	private final MouseAdapter MOUSE_LISTENER = new MouseAdapter()
	{
		@Override
		public void mouseEntered(final MouseEvent e)
		{
			m_timer.start();
		}
		
		@Override
		public void mouseExited(final MouseEvent e)
		{
			m_inside = false;
			m_timer.stop();
		}
	};
	
	private int getNewEdge(final int x, final int y, final int width, final int height)
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
	
	private final ComponentListener COMPONENT_LISTENER = new ComponentAdapter()
	{
		@Override
		public void componentResized(final ComponentEvent e)
		{
			refreshBoxSize();
		}
	};
	
	protected void refreshBoxSize()
	{
		m_model.setBoxDimensions((int) (getWidth() / m_scale), (int) (getHeight() / m_scale));
	}
	
	/**
	 * @param value
	 *            - a double between 0 and 1.
	 */
	public void setScale(double value)
	{
		if (value < 0.15)
			value = 0.15;
		if (value > 1)
			value = 1;
		// we want the ratio to be a multiple of 1/256
		// so that the tiles have integer widths and heights
		value = ((int) (value * 256)) / ((double) 256);
		m_scale = value;
		refreshBoxSize();
	}
	
	/**
	 * used for the mouse wheel
	 */
	private final MouseWheelListener MOUSE_WHEEL_LISTENER = new MouseWheelListener()
	{
		public void mouseWheelMoved(final MouseWheelEvent e)
		{
			if (!e.isAltDown())
			{
				if (m_edge == NONE)
					m_insideCount = 0;
				// compute the amount to move
				int dx = 0;
				int dy = 0;
				if ((e.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) == InputEvent.SHIFT_DOWN_MASK)
					dx = e.getWheelRotation() * WHEEL_SCROLL_AMOUNT;
				else
					dy = e.getWheelRotation() * WHEEL_SCROLL_AMOUNT;
				// move left and right and test for wrap
				int newX = (m_model.getX() + dx);
				if (newX > m_model.getMaxWidth() - getWidth())
					newX -= m_model.getMaxWidth();
				if (newX < -getWidth())
					newX += m_model.getMaxWidth();
				// move up and down and test for edges
				final int newY = m_model.getY() + dy;
				// update the map
				m_model.set(newX, newY);
			}
			else
			{
				double value = m_scale;
				int positive = 1;
				if (e.getUnitsToScroll() > 0)
					positive = -1;
				if ((positive > 0 && value == 1) || (positive < 0 && value <= .21))
					return;
				if (positive > 0)
				{
					if (value >= .79)
						value = 1.0;
					else if (value >= .59)
						value = .8;
					else if (value >= .39)
						value = .6;
					else if (value >= .19)
						value = .4;
					else
						value = .2;
				}
				else
				{
					if (value <= .41)
						value = .2;
					else if (value <= .61)
						value = .4;
					else if (value <= .81)
						value = .6;
					else if (value <= 1.0)
						value = .8;
					else
						value = 1.0;
				}
				setScale(value);
			}
		}
	};
	/**
	 * this is used to detect drag scrolling
	 */
	private final MouseMotionListener MOUSE_DRAG_LISTENER = new MouseMotionAdapter()
	{
		@Override
		public void mouseDragged(final MouseEvent e)
		{
			// the right button must be the one down
			if ((e.getModifiers() & InputEvent.BUTTON3_MASK) != 0)
			{
				m_inside = false;
				// read in location
				final int x = e.getX();
				final int y = e.getY();
				if (m_edge == NONE)
					m_insideCount = 0;
				// compute the amount to move
				final int dx = (m_drag_scrolling_lastx - x);
				final int dy = (m_drag_scrolling_lasty - y);
				// move left and right and test for wrap
				final int newX = (m_model.getX() + dx);
				// move up and down and test for edges
				final int newY = m_model.getY() + dy;
				// update the map
				m_model.set(newX, newY);
				// store the location of the mouse for the next move
				m_drag_scrolling_lastx = e.getX();
				m_drag_scrolling_lasty = e.getY();
			}
		}
	};
	private final MouseAdapter MOUSE_LISTENER_DRAG_SCROLLING = new MouseAdapter()
	{
		@Override
		public void mousePressed(final MouseEvent e)
		{
			// try to center around the click
			m_drag_scrolling_lastx = e.getX();
			m_drag_scrolling_lasty = e.getY();
		}
	};
	private final MouseMotionListener MOUSE_MOTION_LISTENER = new MouseMotionAdapter()
	{
		@Override
		public void mouseMoved(final MouseEvent e)
		{
			m_inside = true;
			final int x = e.getX();
			final int y = e.getY();
			final int height = getHeight();
			final int width = getWidth();
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
