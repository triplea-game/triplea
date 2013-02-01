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
package games.strategy.common.image;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.ResourceLoader;
import games.strategy.ui.Util;

import java.awt.Image;
import java.awt.Toolkit;
import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class to get image for a Unit.
 * <p>
 * 
 * This class is a simplified version of Sean Bridges's games.strategy.triplea.image.UnitImageFactory.
 * 
 * @author Lane Schwartz
 */
public class UnitImageFactory
{
	private static final String FILE_NAME_BASE = "units/";
	// Image cache
	private final Map<String, Image> m_images = new HashMap<String, Image>();
	private ResourceLoader m_resourceLoader;
	
	/**
	 * Creates new IconImageFactory
	 */
	public UnitImageFactory()
	{
		m_resourceLoader = ResourceLoader.getMapResourceLoader(null);
	}
	
	public void setResourceLoader(final ResourceLoader loader)
	{
		m_resourceLoader = loader;
		clearImageCache();
	}
	
	private void clearImageCache()
	{
		m_images.clear();
	}
	
	/**
	 * Return the appropriate unit image.
	 */
	public Image getImage(final UnitType type, final PlayerID player, final GameData data)
	{
		final String baseName = getBaseImageName(type, player, data);
		final String fullName = baseName + player.getName();
		if (m_images.containsKey(fullName))
		{
			return m_images.get(fullName);
		}
		final Image baseImage = getBaseImage(baseName, player);
		m_images.put(fullName, baseImage);
		return baseImage;
	}
	
	private Image getBaseImage(final String baseImageName, final PlayerID id)
	{
		final String fileName = FILE_NAME_BASE + id.getName() + File.separator + baseImageName + ".png";
		final URL url = m_resourceLoader.getResource(fileName);
		if (url == null)
			throw new IllegalStateException("Cant load: " + baseImageName + "  looking in: " + fileName);
		final Image image = Toolkit.getDefaultToolkit().getImage(url);
		try
		{
			Util.ensureImageLoaded(image);
		} catch (final InterruptedException ex)
		{
			ex.printStackTrace();
		}
		return image;
	}
	
	/*
	private static final File BASE_FOLDER = new File(GameRunner.getRootFolder(), ResourceLoader.RESOURCE_FOLDER + "/units/");
	private BufferedImage getBaseImage(final String baseImageName, final PlayerID id)
	{
		final String fileName = id.getName() + File.separator + baseImageName + ".png";
		BufferedImage image = null;
		try
		{
			image = ImageIO.read(new File(BASE_FOLDER, fileName));
			Util.ensureImageLoaded(image);
		} catch (final InterruptedException ex)
		{
			ex.printStackTrace();
		} catch (final IOException e)
		{
			e.printStackTrace();
		}
		return image;
	}*/

	private String getBaseImageName(final UnitType type, final PlayerID id, final GameData data)
	{
		final StringBuilder name = new StringBuilder(32);
		name.append(type.getName());
		return name.toString();
	}
}
