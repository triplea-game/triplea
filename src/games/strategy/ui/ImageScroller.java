/*
 * ImageScroller.java
 *
 * Created on November 1, 2001, 6:29 PM
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
