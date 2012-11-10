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
package games.strategy.ui;

import java.util.Observable;

/**
 * 
 * Model for an ImageScroller. Generally one large view and one small view will be
 * connected to the same model.
 * <p>
 * 
 * notifies its observers when changes occur.
 * <p>
 * 
 * 
 * 
 * @author sgb
 */
public class ImageScrollModel extends Observable
{
	private int m_x;
	private int m_y;
	private int m_boxWidth = 5;
	private int m_boxHeight = 5;
	private int m_maxWidth;
	private int m_maxHeight;
	private boolean m_scrollX;
	private boolean m_scrollY;
	
	public void setMaxBounds(final int maxWidth, final int maxHeight)
	{
		m_maxWidth = maxWidth;
		m_maxHeight = maxHeight;
		enforceBounds();
		updateListeners();
	}
	
	public void setBoxDimensions(final int maxX, final int maxy)
	{
		m_boxWidth = maxX;
		m_boxHeight = maxy;
		enforceBounds();
		updateListeners();
	}
	
	private void updateListeners()
	{
		super.setChanged();
		super.notifyObservers();
	}
	
	public void setScrollX(final boolean aBool)
	{
		m_scrollX = aBool;
		enforceBounds();
		updateListeners();
	}
	
	public void setScrollY(final boolean aBool)
	{
		m_scrollY = aBool;
		enforceBounds();
		updateListeners();
	}
	
	private void enforceBounds()
	{
		if (!m_scrollY)
		{
			if (m_y < 0)
				m_y = 0;
			if (m_y + m_boxHeight > m_maxHeight)
			{
				m_y = m_maxHeight - m_boxHeight;
			}
		}
		else
		{
			// don't let the map scroll infinitely,
			// when it gets to be twice the height to the up or down, move it back one length
			while (m_y > m_maxHeight)
			{
				m_y -= m_maxHeight;
			}
			while (m_y < -m_maxHeight)
			{
				m_y += m_maxHeight;
			}
		}
		// if the box is bigger than the map
		// put us at 0,0
		if (m_boxHeight > m_maxHeight)
		{
			m_y = 0;
		}
		
		if (!m_scrollX)
		{
			if (m_x < 0)
				m_x = 0;
			if (m_x + m_boxWidth > m_maxWidth)
			{
				m_x = m_maxWidth - m_boxWidth;
			}
		}
		else
		{
			// don't let the map scroll infinitely,
			// when it gets to be twice the length to the left or right, move it back one length
			while (m_x > m_maxWidth)
			{
				m_x -= m_maxWidth;
			}
			while (m_x < -m_maxWidth)
			{
				m_x += m_maxWidth;
			}
		}
		// if the box is bigger than the map
		// put us at 0,0
		if (m_boxWidth > m_maxWidth)
		{
			m_x = 0;
		}
	}
	
	public boolean getScrollX()
	{
		return m_scrollX;
	}
	
	public int getX()
	{
		return m_x;
	}
	
	public int getY()
	{
		return m_y;
	}
	
	public int getBoxWidth()
	{
		return m_boxWidth;
	}
	
	public int getBoxHeight()
	{
		return m_boxHeight;
	}
	
	public int getMaxWidth()
	{
		return m_maxWidth;
	}
	
	public int getMaxHeight()
	{
		return m_maxHeight;
	}
	
	public void set(final int x, final int y)
	{
		m_x = x;
		m_y = y;
		enforceBounds();
		updateListeners();
	}
}
