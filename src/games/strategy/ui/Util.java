/*
 * Util.java
 *
 * Created on October 30, 2001, 6:29 PM
 */

package games.strategy.ui;

import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.*;

/**
 *
 * @author  Sean Bridges
 * @version 1.0
 */
public class Util 
{
	//all we have is static methods
	private Util() {}
	
	public static void ensureImageLoaded(Image anImage, Component comp) throws InterruptedException
	{
		MediaTracker tracker = new MediaTracker(comp);
		tracker.addImage(anImage, 1);
		tracker.waitForAll();
	}
	
	public static Image copyImage(Image img, JComponent comp)
	{		
		Image copy = createImage(img.getWidth(comp), img.getHeight(comp));
		copy.getGraphics().drawImage(img, 0,0, comp);
		return copy;
	}
	
	public static Image createImage(int width, int height)
	{
		return new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
	}
	
	public static Dimension getDimension(Image anImage, ImageObserver obs)
	{
		return new Dimension(anImage.getWidth(obs), anImage.getHeight(obs) );
	}
	
	public static final WindowListener EXIT_ON_CLOSE_WINDOW_LISTENER = new WindowAdapter()
	{
		public void windowClosing(WindowEvent e) 
		{
			System.exit(0);
		}
	};	
}
