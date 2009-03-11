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
 * ImageScroller.java
 *
 * Created on November 1, 2001, 6:29 PM
 */

package games.strategy.ui;

import javax.swing.*;

/**
 *
 * @author  Sean Bridges
 * @version 1.0
 *
 * Overlays the small view with the large view
 */
public class ImageScroller extends JPanel
{

	/** Creates new ImageScroller
	 */
    public ImageScroller(ImageScrollerLargeView large, ImageScrollerSmallView small) 
	{
		OverlayLayout overlay = new OverlayLayout(this);
		this.setLayout(overlay);
		
		this.add(small);
		this.add(large);
		small.setAlignmentX(1);
		small.setAlignmentY(0);
    }
}
