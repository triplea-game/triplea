/*
 * FlagIconImageFactory.java
 *
 * Created on November 26, 2001, 8:27 PM
 */

package games.strategy.triplea.image;

import java.io.*;
import java.util.*;
import java.awt.*;
import java.awt.image.*;

import games.strategy.util.*;
import games.strategy.ui.Util;
import games.strategy.engine.data.PlayerID;

/**
 *
 * @author  Sean Bridges
 * @version 1.0
 */
public class FlagIconImageFactory 
{
	
	private static FlagIconImageFactory s_instance = new FlagIconImageFactory();
	
	public static FlagIconImageFactory instance()
	{
		return s_instance;
	}
	
	boolean m_loaded = false;
	
	public static final int FLAG_ICON_WIDTH = 30;
	public static final int FLAG_ICON_HEIGHT = 15;
	
	public static final int SMALL_FLAG_ICON_WIDTH = 12;
	public static final int SMALL_FLAG_ICON_HEIGHT = 7;
	
	//maps name -> image
	private final Map m_images = new HashMap();

	/** Creates new IconImageFactory */
    private FlagIconImageFactory() 
	{}
	
	
	/**
	 * Loads the images, does not return till all images
	 * have finished loading.
	 */
	public synchronized void load(Component observer) throws IOException
	{
		if(m_loaded)
			throw new IllegalStateException("Already loaded");
		
		//TODO 
		//put these io one image
		Image japanese = Toolkit.getDefaultToolkit().getImage( this.getClass().getResource("images/jap.gif"));
		Image japaneseSmall = Toolkit.getDefaultToolkit().getImage( this.getClass().getResource("images/jap_small.gif"));
		Image british = Toolkit.getDefaultToolkit().getImage( this.getClass().getResource("images/uk.gif"));
		Image britishSmall = Toolkit.getDefaultToolkit().getImage( this.getClass().getResource("images/uk_small.gif"));
		Image american = Toolkit.getDefaultToolkit().getImage( this.getClass().getResource("images/us.gif"));
		Image americanSmall = Toolkit.getDefaultToolkit().getImage( this.getClass().getResource("images/us_small.gif"));
		Image german = Toolkit.getDefaultToolkit().getImage( this.getClass().getResource("images/ger.gif"));
		Image germanSmall = Toolkit.getDefaultToolkit().getImage( this.getClass().getResource("images/ger_small.gif"));
		Image russian = Toolkit.getDefaultToolkit().getImage( this.getClass().getResource("images/rus.gif"));
		Image russianSmall = Toolkit.getDefaultToolkit().getImage( this.getClass().getResource("images/rus_small.gif"));

		MediaTracker tracker = new MediaTracker(observer);
		tracker.addImage(japanese, 1);
		tracker.addImage(japaneseSmall, 1);
		tracker.addImage(british, 1);
		tracker.addImage(britishSmall, 1);
		tracker.addImage(american, 1);
		tracker.addImage(americanSmall, 1);
		tracker.addImage(german, 1);
		tracker.addImage(germanSmall, 1);
		tracker.addImage(russian, 1);
		tracker.addImage(russianSmall, 1);
		try
		{
			tracker.waitForAll();
		} catch(InterruptedException e)
		{
			e.printStackTrace(System.out);
			System.out.println("try again");
			load(observer);
		}
		
		m_images.put("JapaneseSmall", japaneseSmall);
		m_images.put("Japanese", japanese);
		m_images.put("BritishSmall", britishSmall);
		m_images.put("British", british);
		m_images.put("AmericansSmall", americanSmall);
		m_images.put("Americans", american);
		m_images.put("GermansSmall", germanSmall);
		m_images.put("Germans", german);
		m_images.put("RussiansSmall", russianSmall);
		m_images.put("Russians", russian);
		
		m_loaded = true;		
	}
	
	public Image getFlag(PlayerID id)
	{
		if(m_loaded == false)
			throw new IllegalArgumentException("Images not loaded");
		
		Image img = (Image) m_images.get(id.getName());
		if(img == null)
			throw new IllegalArgumentException("Flag not found:" + id.getName());
		return img;
	}
	
	public Image getSmallFlag(PlayerID id)
	{
		if(m_loaded == false)
			throw new IllegalArgumentException("Images not loaded");
		
		Image img = (Image) m_images.get(id.getName() + "Small");
		if(img == null)
			throw new IllegalArgumentException("Small flag not found:" + id.getName());
		return img;
	}
}