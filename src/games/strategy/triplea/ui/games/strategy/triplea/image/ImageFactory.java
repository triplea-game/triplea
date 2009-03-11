package games.strategy.triplea.image;

import games.strategy.triplea.ResourceLoader;

import java.awt.Image;
import java.io.IOException;
import java.net.URL;
import java.util.*;

import javax.imageio.ImageIO;

public class ImageFactory
{
    private final Map<String, Image> m_images = new HashMap<String, Image>();
    private ResourceLoader m_resourceLoader;
    
    
    public void setResourceLoader(ResourceLoader loader)
    {
        m_resourceLoader = loader;
        m_images.clear();
    }

    protected Image getImage(String key, boolean throwIfNotFound)
    {

        if (!m_images.containsKey(key))
        {
            URL url = m_resourceLoader.getResource(key);
            if (url == null && throwIfNotFound)
            {
                throw new IllegalStateException("Image Not Found:" + key);
            }
            else if(url == null)
            {
                m_images.put(key, null);
                return null;
            }

            Image image;
            try
            {
                image = ImageIO.read(url);
            } catch (IOException e)
            {
                e.printStackTrace();
                throw new IllegalStateException(e.getMessage());
            }
            m_images.put(key, image);
        }
        
        return m_images.get(key);
    }
    
    
    
}
