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
 * FlagIconImageFactory.java
 *
 * Created on November 26, 2001, 8:27 PM
 */

package games.strategy.triplea.image;

import games.strategy.engine.data.PlayerID;

import java.awt.*;
import java.io.IOException;
import java.util.*;

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
    private FlagIconImageFactory() { };


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
		Image japanese = Toolkit.getDefaultToolkit().getImage( this.getClass().getResource("images/flags/jap.gif"));
		Image japaneseSmall = Toolkit.getDefaultToolkit().getImage( this.getClass().getResource("images/flags/jap_small.gif"));
		Image british = Toolkit.getDefaultToolkit().getImage( this.getClass().getResource("images/flags/uk.gif"));
		Image britishSmall = Toolkit.getDefaultToolkit().getImage( this.getClass().getResource("images/flags/uk_small.gif"));
		Image american = Toolkit.getDefaultToolkit().getImage( this.getClass().getResource("images/flags/us.gif"));
		Image americanSmall = Toolkit.getDefaultToolkit().getImage( this.getClass().getResource("images/flags/us_small.gif"));
		Image german = Toolkit.getDefaultToolkit().getImage( this.getClass().getResource("images/flags/ger.gif"));
		Image germanSmall = Toolkit.getDefaultToolkit().getImage( this.getClass().getResource("images/flags/ger_small.gif"));
		Image russian = Toolkit.getDefaultToolkit().getImage( this.getClass().getResource("images/flags/rus.gif"));
		Image russianSmall = Toolkit.getDefaultToolkit().getImage( this.getClass().getResource("images/flags/rus_small.gif"));
		Image neutralSmall = Toolkit.getDefaultToolkit().getImage( this.getClass().getResource("images/flags/neutral_small.gif"));
                Image italian = Toolkit.getDefaultToolkit().getImage( this.getClass().getResource("images/flags/it.gif"));
                Image italianSmall = Toolkit.getDefaultToolkit().getImage( this.getClass().getResource("images/flags/it_small.gif"));

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
                tracker.addImage(italian, 1);
                tracker.addImage(italianSmall, 1);
		try
		{
			tracker.waitForAll();
		} catch(InterruptedException e)
		{
			e.printStackTrace(System.out);
			System.out.println("try again");
			load(observer);
      return;
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
                m_images.put("Italians", italian);
                m_images.put("ItaliansSmall", italianSmall);
		m_images.put(PlayerID.NULL_PLAYERID.getName() + "Small", neutralSmall);

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
    if(id == null)
      throw new IllegalArgumentException("Null id");

		if(m_loaded == false)
			throw new IllegalArgumentException("Images not loaded");

		Image img = (Image) m_images.get(id.getName() + "Small");
		if(img == null)
			throw new IllegalArgumentException("Small flag not found:" + id.getName());
		return img;
	}
}