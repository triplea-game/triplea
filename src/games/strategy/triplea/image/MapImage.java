/*
 * CountryImage.java
 *
 * Created on January 8, 2002, 9:15 PM
 */

package games.strategy.triplea.image;

import java.net.URL;
import java.io.*;
import java.util.*;
import java.awt.*;
import java.awt.image.*;

import games.strategy.util.PointFileReaderWriter;
import games.strategy.engine.data.*;

/**
 *
 * @author  Sean Bridges
 */
public class MapImage 
{	
	private static final String LOCATION = "countries/location.txt";
	private final static String LARGE_IMAGE_FILENAME = "images/largeMap.gif";
	private final static String SMALL_IMAGE_FILENAME = "images/smallMap.gif";
	
	private static MapImage s_instance;
	private static Component s_observer;

	
	private static final int BRITISH_COLOUR = (153<<0) | (102<<8) | (0<<16);
	private static final int AMERICAN_COLOUR = (102<<0) | (102<<8) | (0<<16);
	private static final int RUSSIAN_COLOUR = (153<<0) | (51<<8) | (0<<16);
	private static final int GERMAN_COLOUR = (119<<0) | (119<<8) | (119<<16);
	private static final int JAPANESE_COLOUR = (255<<0) | (153<<8) | (0<<16);
	private static final int NEUTRAL_COLOUR = (204<<0) | (153<<8) | (51<<16);
	
	
	//maps playerName -> Integer
	private static Map s_playerColours = new HashMap();
	static 
	{
		s_playerColours.put("no one", new Integer(NEUTRAL_COLOUR));
		s_playerColours.put("Americans", new Integer(AMERICAN_COLOUR));
		s_playerColours.put("British", new Integer(BRITISH_COLOUR));
		s_playerColours.put("Russians", new Integer(RUSSIAN_COLOUR));
		s_playerColours.put("Japanese", new Integer(JAPANESE_COLOUR));
		s_playerColours.put("Germans", new Integer(GERMAN_COLOUR));
	}
	
	public static synchronized MapImage getInstance()
	{
		if(s_instance == null)
		{
			s_instance = new MapImage();
			s_observer = new Panel();
		}
		return s_instance;
	}

	private static Image loadImage(String name)
	{
		Image img =  Toolkit.getDefaultToolkit().createImage(MapImage.class.getResource(name));
		MediaTracker tracker = new MediaTracker(s_observer);
		tracker.addImage(img,1 );
		try
		{
			tracker.waitForAll();
			return img;
		} catch(InterruptedException ie)
		{
			ie.printStackTrace();
			return loadImage(name);
		}
	}
	
	//Maps Country name -> Point
	private Map m_topCorners = new HashMap();
	private Image m_largeMapImage;
	private Image m_smallMapImage;
	private float m_smallLargeRatio;
	
	/** Creates a new instance of CountryImage */
    public MapImage() 
	{
		initCorners();
    }

	
	public Image getSmallMapImage() 
	{
		return m_smallMapImage;
	}
	
	public void loadMaps(GameData data)
	{
		loadMaps();
		initMaps(data);
	}
	
	public Image getLargeMapImage() 
	{
		return m_largeMapImage;
	}
	
	private void loadMaps()
	{
		Image largeFromFile = loadImage(LARGE_IMAGE_FILENAME);
		Image smallFromFile = loadImage(SMALL_IMAGE_FILENAME);
		
		//create from a component to make screen drawing faster
		//if you create an image from a component then no operations
		//have to be done when drawing the image to the screen, just a simple 
		//byte copy
		Frame frame = new Frame();
		frame.addNotify();
		m_largeMapImage = frame.createImage(largeFromFile.getWidth(s_observer), largeFromFile.getHeight(s_observer));
		m_smallMapImage = frame.createImage(smallFromFile.getWidth(s_observer), smallFromFile.getHeight(s_observer));
		frame.removeNotify();
		frame.dispose();
		frame = null;
		
		m_largeMapImage.getGraphics().drawImage(largeFromFile, 0,0,s_observer);
		m_smallMapImage.getGraphics().drawImage(smallFromFile, 0,0,s_observer);
		
		largeFromFile = null;
		smallFromFile = null;
		System.gc();
	}
	
	private void initMaps(GameData data)
	{
		m_smallLargeRatio = ((float) m_largeMapImage.getHeight(s_observer)) / ((float) m_smallMapImage.getHeight(s_observer));
		
		Iterator territories = data.getMap().iterator();
		while(territories.hasNext())
		{
			Territory current = (Territory) territories.next();
			PlayerID id = current.getOwner();
			
			if(!current.isWater())
				setOwner(current, id);
		}
	}
		
	private void initCorners() 
	{	
		try
		{
			URL centers = MapImage.class.getResource(LOCATION);		
			InputStream stream = centers.openStream();
			m_topCorners = new PointFileReaderWriter().readOneToOne(stream);
		} catch(IOException ioe)
		{
			System.err.println("Error reading " + LOCATION + "  file");
			ioe.printStackTrace();
			System.exit(0);
		}
	}	
		
	public void setOwner(Territory territory, PlayerID id)
	{
		if(territory.isWater())
			return;
		
		String name = territory.getName();
		
		String fileName = "countries/" + name.replace(' ', '_')+  ".png";
		Image country = loadImage(fileName);
		
		BufferedImage newImage = new BufferedImage(country.getWidth(s_observer), country.getHeight(s_observer), BufferedImage.TYPE_INT_ARGB);
		newImage.getGraphics().drawImage(country, 0,0, s_observer);
		
		LookupOp filter = getLookupOp(id);
		filter.filter(newImage, newImage);
		
		Point p = (Point) m_topCorners.get(name);
		if(p == null)
			throw new IllegalStateException("No top corner could be found for:" + name);
		
		m_largeMapImage.getGraphics().drawImage(newImage, p.x, p.y, s_observer);
		
		int smallHeight = (int) (newImage.getHeight() / m_smallLargeRatio) + 3;
		int smallWidth = (int) (newImage.getWidth() / m_smallLargeRatio) + 3;
		Image small  = newImage.getScaledInstance(smallWidth, smallHeight , Image.SCALE_FAST);
		
		Point smallPoint = new Point( (int)( p.x / m_smallLargeRatio), (int) (p.y / m_smallLargeRatio));
		m_smallMapImage.getGraphics().drawImage(small, smallPoint.x, smallPoint.y,  s_observer);
		
	}
	
	
	private LookupOp getLookupOp(PlayerID id)
	{
		String playerName = id.getName();
		if(!s_playerColours.containsKey(playerName))
			throw new IllegalStateException("player name not found:" + playerName);
		
		int colour = ((Integer) s_playerColours.get(playerName)).intValue();
		byte red = (byte) colour;
		byte green = (byte) (colour>>8);
		byte blue = (byte) (colour>>16);
		
		byte[] rBytes = new byte[256];
		byte[] gBytes = new byte[256];
		byte[] bBytes = new byte[256];
		byte[] alpha = new byte[256];
		
		for(int i = 0; i < 256; i++)
		{
			rBytes[i] = red;
			gBytes[i] = green;
			bBytes[i] = blue;
			alpha[i] = (byte) i;
		}
		
		byte[][] bytes = new byte[][] {rBytes, gBytes, bBytes, alpha};
		LookupOp op = new LookupOp( new ByteLookupTable(0,bytes), null );
		
		return op;
	}
}