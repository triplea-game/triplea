/*
 * UnitIconImageFactory.java
 *
 * Created on November 25, 2001, 8:27 PM
 */

package games.strategy.triplea.image;

import java.io.*;
import java.util.*;
import java.awt.*;
import java.awt.image.*;

import javax.swing.ImageIcon;

import games.strategy.util.*;
import games.strategy.ui.Util;
import games.strategy.engine.data.UnitType;

/**
 *
 * @author  Sean Bridges
 * @version 1.0
 */
public class UnitIconImageFactory 
{
	
	private static UnitIconImageFactory s_instance = new UnitIconImageFactory();
	
	public static UnitIconImageFactory instance()
	{
		return s_instance;
	}
	
	boolean m_loaded = false;
	/**
	 * Width of all icons.
	 */
	public static final int UNIT_ICON_WIDTH = 30;
	/**
	 * Height of all icons.
	 **/
	public static final int UNIT_ICON_HEIGHT = 28;
	
	private static final String FILE_NAME = "images/units.gif";
	
	//maps name -> image
	private final Map m_images = new HashMap();
	//maps image -> ICon
	private final Map m_icons = new HashMap();

	/** Creates new IconImageFactory */
    private UnitIconImageFactory() 
	{
		
    }
	
	private void copyImage(String name, int row, int column, Component comp, Image source)
	{
		Image image = Util.createImage(UNIT_ICON_WIDTH, UNIT_ICON_HEIGHT);
		Graphics g = image.getGraphics();
		int sx = column * UNIT_ICON_WIDTH;
		int sy = row * UNIT_ICON_HEIGHT;
		g.drawImage(source, 0,0, UNIT_ICON_WIDTH, UNIT_ICON_HEIGHT, sx, sy, sx + UNIT_ICON_WIDTH, sy + UNIT_ICON_HEIGHT, comp);
		m_images.put(name, image);
	}
	
	/**
	 * Loads the images, does not return till all images
	 * have finished loading.
	 */
	public synchronized void load(Component observer) throws IOException
	{
		if(m_loaded)
			throw new IllegalStateException("Already loaded");
		
		Image image = Toolkit.getDefaultToolkit().getImage( this.getClass().getResource(FILE_NAME));
		//wait for the image to load
		MediaTracker tracker = new MediaTracker(observer);
		tracker.addImage(image, 1);
		try
		{
			tracker.waitForAll();
		} catch(InterruptedException e)
		{
			e.printStackTrace(System.out);
			System.out.println("try again");
			load(observer);
		}
		
		//load the individual images
		copyImage("armour", 0,0, observer, image);
		copyImage("infantry", 0,1, observer, image);
		copyImage("fighter", 0,2, observer, image);
		copyImage("bomber", 0,3, observer, image);
		copyImage("carrier", 0,4, observer, image);
		copyImage("transport", 0,5, observer, image);
		
		copyImage("battleship", 1,0, observer, image);
		copyImage("aaGun", 1,1, observer, image);
		copyImage("submarine", 1,2, observer, image);
		copyImage("heavyBomber", 1,3, observer, image);
		copyImage("jetFighter", 1,4, observer, image);
		
		
		copyImage("factory", 1,5, observer, image);
		
		m_loaded = true;
	}
	
	public Image getImage(UnitType type)
	{
		if(m_loaded == false)
			throw new IllegalArgumentException("Images not loaded");
		
		Image img = (Image) m_images.get(type.getName());
		if(img == null)
			throw new IllegalArgumentException("Image not found:" + type.getName());
		return img;
	}
	
	public ImageIcon getIcon(UnitType type)
	{
		if(m_loaded == false)
			throw new IllegalArgumentException("Images not loaded");
		
		Image img = getImage(type);
		
		ImageIcon icon = (ImageIcon) m_icons.get(img);
		if(icon == null)
		{
			icon = new ImageIcon(img);
			m_icons.put(img, icon);
		}
		
		return icon;
	}
}
