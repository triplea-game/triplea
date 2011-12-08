package games.strategy.engine.framework.startup.ui;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.properties.IEditableProperty;
import games.strategy.engine.framework.GameRunner;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * A game options cache that uses files to store the game options
 * 
 * @author Klaus Groenbaek
 */
public class FileBackedGamePropertiesCache implements IGamePropertiesCache
{
	// -----------------------------------------------------------------------
	// class constants
	// -----------------------------------------------------------------------
	// chars illegal on windows (on linux/mac anything that is allowed on windows works fine)
	final static char[] s_illegalChars = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 34, 42, 58, 60, 62, 63, 92, 124 };
	
	// -----------------------------------------------------------------------
	// instance methods
	// -----------------------------------------------------------------------
	
	/**
	 * Caches the gameOptions stored in the game data, and associates with this game. only values that are serializable
	 * (which they should all be) will be stored
	 * 
	 * @param gameData
	 *            the game which options you want to cache
	 */
	public void cacheGameProperties(final GameData gameData)
	{
		final Map<String, Object> serializableMap = new HashMap<String, Object>();
		for (final IEditableProperty property : gameData.getProperties().getEditableProperties())
		{
			if (property.getValue() instanceof Serializable)
			{
				serializableMap.put(property.getName(), property.getValue());
			}
		}
		
		final File cache = getCacheFile(gameData);
		try
		{
			// create the directory if it doesn't already exists
			if (!cache.getParentFile().exists())
			{
				cache.getParentFile().mkdirs();
			}
			final ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(cache));
			out.writeObject(serializableMap);
			out.close();
		} catch (final IOException e)
		{
			e.printStackTrace();
		}
	}
	
	/**
	 * Loads cached game options into the gameData
	 * 
	 * @param gameData
	 *            the game to load the cached game options into
	 */
	@SuppressWarnings("unchecked")
	// generics are compile time only, and lost during serialization
	public void loadCachedGamePropertiesInto(final GameData gameData)
	{
		final File cache = getCacheFile(gameData);
		try
		{
			if (cache.exists())
			{
				final ObjectInputStream in = new ObjectInputStream(new FileInputStream(cache));
				final Map<String, Serializable> serializedMap = (Map<String, Serializable>) in.readObject();
				for (final IEditableProperty property : gameData.getProperties().getEditableProperties())
				{
					final Serializable ser = serializedMap.get(property.getName());
					if (ser != null)
					{
						property.setValue(ser);
					}
				}
			}
		} catch (final IOException e)
		{
			e.printStackTrace();
		} catch (final ClassNotFoundException e)
		{
			e.printStackTrace();
		}
	}
	
	/**
	 * Calculates the cache filename and location based on the game data
	 * 
	 * @param gameData
	 *            the game data
	 * @return the File where the cached game options should be stored or read from
	 */
	private File getCacheFile(final GameData gameData)
	{
		final File cacheDir = new File(GameRunner.getUserRootFolder(), "optionCache");
		return new File(cacheDir, getFileName(gameData.getGameName()));
	}
	
	/**
	 * Removes any special characters from the file name
	 * 
	 * @param gameName
	 *            the name of the game
	 * @return the fileName on disk
	 */
	private String getFileName(final String gameName)
	{
		final StringBuilder sb = new StringBuilder();
		
		for (int i = 0, charArrayLength = gameName.length(); i < charArrayLength; i++)
		{
			final char c = gameName.charAt(i);
			if (Arrays.binarySearch(s_illegalChars, c) < 0)
			{
				sb.append(c);
			}
		}
		sb.append(".defaultOptions");
		return sb.toString();
	}
}
