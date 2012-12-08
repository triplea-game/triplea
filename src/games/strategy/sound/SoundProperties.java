package games.strategy.sound;

import games.strategy.triplea.ResourceLoader;

import java.io.IOException;
import java.net.URL;
import java.util.Calendar;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

/**
 * sounds.properties file helper class
 * 
 * @author veqryn
 * 
 */
public class SoundProperties
{
	// Filename
	private static final String PROPERTY_FILE = "sounds.properties";
	static final String PROPERTY_DEFAULT_FOLDER = "Sound.Default.Folder";
	static final String DEFAULT_ERA_FOLDER = "ww2";
	static final String GENERIC_FOLDER = "generic";
	static final String OBJECTIVES_PANEL_NAME = "Objectives.Panel.Name";
	private static SoundProperties s_op = null;
	private static long s_timestamp = 0;
	private final Properties m_properties = new Properties();
	
	protected SoundProperties(final ResourceLoader loader)
	{
		final URL url = loader.getResource(PROPERTY_FILE);
		if (url == null)
		{
			// no property file found
		}
		else
		{
			try
			{
				m_properties.load(url.openStream());
			} catch (final IOException e)
			{
				System.out.println("Error reading " + PROPERTY_FILE + " : " + e);
			}
		}
	}
	
	public static SoundProperties getInstance(final ResourceLoader loader)
	{
		if (s_op == null || Calendar.getInstance().getTimeInMillis() > s_timestamp + 1000)
		{ // cache properties for 1 second
			s_op = new SoundProperties(loader);
			s_timestamp = Calendar.getInstance().getTimeInMillis();
		}
		return s_op;
	}
	
	public String getDefaultEraFolder()
	{
		return getProperty(PROPERTY_DEFAULT_FOLDER, DEFAULT_ERA_FOLDER);
	}
	
	/**
	 * 
	 * @param objectiveKey
	 * @return the string property, or null if not found
	 */
	public String getProperty(final String key)
	{
		return m_properties.getProperty(key);
	}
	
	public String getProperty(final String key, final String defaultValue)
	{
		return m_properties.getProperty(key, defaultValue);
	}
	
	public Set<Entry<Object, Object>> entrySet()
	{
		return m_properties.entrySet();
	}
}
