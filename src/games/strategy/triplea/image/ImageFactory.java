package games.strategy.triplea.image;

import games.strategy.triplea.ResourceLoader;

import java.awt.Image;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

public class ImageFactory
{
	private final Map<String, Image> m_images = new HashMap<String, Image>();
	private ResourceLoader m_resourceLoader;
	
	public void setResourceLoader(final ResourceLoader loader)
	{
		m_resourceLoader = loader;
		m_images.clear();
	}
	
	protected Image getImage(final String key1, final String key2, final boolean throwIfNotFound)
	{
		final Image i1 = getImage(key1, false);
		if (i1 != null)
		{
			return i1;
		}
		return getImage(key2, throwIfNotFound);
	}
	
	protected Image getImage(final String key, final boolean throwIfNotFound)
	{
		if (!m_images.containsKey(key))
		{
			final URL url = m_resourceLoader.getResource(key);
			if (url == null && throwIfNotFound)
			{
				throw new IllegalStateException("Image Not Found:" + key);
			}
			else if (url == null)
			{
				m_images.put(key, null);
				return null;
			}
			Image image;
			try
			{
				image = ImageIO.read(url);
			} catch (final IOException e)
			{
				e.printStackTrace();
				throw new IllegalStateException(e.getMessage());
			}
			m_images.put(key, image);
		}
		return m_images.get(key);
	}
}
