package games.strategy.engine.framework.startup;

import games.strategy.engine.framework.GameRunner2;
import games.strategy.engine.framework.startup.ui.editors.IBean;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * A cache for serialized beans that should be stored locally.
 * This is used to store settings which are not game related, and should therefore not go into the options cache
 * This is often used by editors to remember previous values
 * 
 * @author Klaus Groenbaek
 */
public class LocalBeanCache
{
	// -----------------------------------------------------------------------
	// class fields
	// -----------------------------------------------------------------------
	private static final LocalBeanCache s_INSTANCE = new LocalBeanCache();
	private final File m_file;
	private final Object m_mutex = new Object();
	
	// -----------------------------------------------------------------------
	// class methods
	// -----------------------------------------------------------------------
	public static LocalBeanCache getInstance()
	{
		return s_INSTANCE;
	}
	
	// -----------------------------------------------------------------------
	// instance fields
	// -----------------------------------------------------------------------
	Map<String, IBean> m_map = new HashMap<String, IBean>();
	
	// -----------------------------------------------------------------------
	// constructors
	// -----------------------------------------------------------------------
	
	private LocalBeanCache()
	{
		m_file = new File(GameRunner2.getUserRootFolder(), "local.cache");
		m_map = loadMap();
		
		// add a shutdown, just in case someone forgets to call writeToDisk
		final Thread shutdown = new Thread(new Runnable()
		{
			public void run()
			{
				writeToDisk();
			}
		});
		Runtime.getRuntime().addShutdownHook(shutdown);
		
	}
	
	@SuppressWarnings("unchecked")
	private Map<String, IBean> loadMap()
	{
		
		if (m_file.exists())
		{
			try
			{
				FileInputStream fin = null;
				ObjectInput oin = null;
				try
				{
					fin = new FileInputStream(m_file);
					oin = new ObjectInputStream(fin);
					final Object o = oin.readObject();
					if (o instanceof Map)
					{
						final Map m = (Map) o;
						for (final Object o1 : m.keySet())
						{
							if (!(o1 instanceof String))
							{
								throw new Exception("Map is corrupt");
							}
						}
					}
					else
					{
						throw new Exception("File is corrupt");
					}
					// we know that the map has proper type key/value
					return (HashMap<String, IBean>) o;
				} finally
				{
					if (fin != null)
					{
						try
						{
							fin.close();// close stream, or we can delete the file (on windows)
						} catch (final IOException e)
						{
							// ignore
						}
					}
					if (oin != null)
					{
						try
						{
							oin.close();// close stream, or we can delete the file (on windows)
						} catch (final IOException e)
						{
							// ignore
						}
					}
				}
			} catch (final Exception e)
			{
				// on error we delete the cache file, if we can
				m_file.delete();
				System.err.println("Serialization cache invalid");
			}
		}
		return new HashMap<String, IBean>();
		
	}
	
	// -----------------------------------------------------------------------
	// instance methods
	// -----------------------------------------------------------------------
	
	/**
	 * adds a new Serializable to the cache
	 * 
	 * @param key
	 *            the key the serializable should be stored under. Take care not to override a serializable stored by other code
	 *            it is generally a good ide to use fully qualified class names, getClass().getCanonicalName() as key
	 * @param bean
	 *            the bean
	 */
	public void storeSerializable(final String key, final IBean bean)
	{
		m_map.put(key, bean);
	}
	
	/**
	 * Call to have the cache written to disk
	 */
	public void writeToDisk()
	{
		synchronized (m_mutex)
		{
			
			ObjectOutputStream out = null;
			FileOutputStream fout = null;
			try
			{
				fout = new FileOutputStream(m_file, false);
				out = new ObjectOutputStream(fout);
				out.writeObject(m_map);
				
			} catch (final IOException e)
			{
				// ignore
			} finally
			{
				if (fout != null)
				{
					try
					{
						fout.close();
					} catch (final IOException e)
					{
						// ignore
					}
				}
				if (out != null)
				{
					try
					{
						out.close();
					} catch (final IOException e)
					{
						// ignore
					}
				}
			}
		}
	}
	
	/**
	 * Get a serializable from the cache
	 * 
	 * @param key
	 *            the key ot was stored under
	 * @return the serializable or null if one doesn't exists under the given key
	 */
	public IBean getSerializable(final String key)
	{
		return m_map.get(key);
	}
}
