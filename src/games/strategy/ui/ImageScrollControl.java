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
 * ImageScrollControl.java
 *
 * Created on October 30, 2001, 7:04 PM
 */

package games.strategy.ui;

import javax.swing.*;
import java.awt.*;
import java.util.*;

/**
 *
 * @author  Sean Bridges
 * @version 1.0
 *
 * Co-ordinates the actions of an ImageScrollerLargeView and an
 * ImageScrollerSmallView.  Keeps the selection area of the small view and
 * the displayed area of the large view in sync.
 */
public class ImageScrollControl
{

	private ImageScrollerLargeView m_large;
	private ImageScrollerSmallView m_small;
	private double m_ratioX = 0;
	private double m_ratioY = 0;

	public static void main(String[] args)
	{
		if(args.length != 2)
		{
			System.out.println("Usage: first arg name of large image, second arg name of small image");
			System.exit(0);
		}

		Image smallImage = Toolkit.getDefaultToolkit().getImage( args[1]);
		Image largeImage = Toolkit.getDefaultToolkit().getImage( args[0]);

		ImageScrollerLargeView large = new ImageScrollerLargeView(largeImage);
		ImageScrollerSmallView small = new ImageScrollerSmallView(smallImage);
		new ImageScrollControl(large, small);

		ImageScroller scroller= new ImageScroller(large, small);

		JFrame l = new JFrame("large");
		l.setSize(900,600);
		l.addWindowListener(Util.EXIT_ON_CLOSE_WINDOW_LISTENER);
		l.getContentPane().add(scroller);
		l.show();
	}

	/** Creates new ImageScrollControl */
    public ImageScrollControl(ImageScrollerLargeView large, ImageScrollerSmallView small)
	{
		m_large = large;
		m_small = small;

		m_large.setController(this);
		m_small.setController(this);

		Dimension largeSize = m_large.getImageDimensions();
		Dimension smallSize = m_small.getImageDimensions();
		m_ratioX = largeSize.getHeight() / smallSize.getHeight();
		m_ratioY = largeSize.getWidth() / smallSize.getWidth();
		resetSmallSelectionArea();
    }

	public double getRatioX()
	{
		return m_ratioX;
	}

	public double getRatioY()
	{
		return m_ratioY;
	}

	void setSmallCoords(int x, int y)
	{
		m_large.setCoords( (int) (x * m_ratioX) ,(int) (y * m_ratioY));
	}

	void setLargeCoords(int x, int y)
	{
		m_small.setCoords( (int) (x / m_ratioX) ,(int) (y / m_ratioY));
	}

	private void resetSmallSelectionArea()
	{
		int x = m_large.getWidth();
		int y = m_large.getHeight();
		m_small.setSelectionBound((int) (x / m_ratioX) ,(int) (y / m_ratioY));
	}

	void largeViewChangedSize()
	{
		resetSmallSelectionArea();
	}
}
