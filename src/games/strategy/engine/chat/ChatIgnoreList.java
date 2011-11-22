package games.strategy.engine.chat;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class ChatIgnoreList
{
	private static final Logger log = Logger.getLogger(ChatIgnoreList.class.getName());
	private final Object m_lock = new Object();
	private final Set<String> m_ignore = new HashSet<String>();
	
	public ChatIgnoreList()
	{
		final Preferences prefs = getPrefNode();
		try
		{
			for (final String key : prefs.keys())
			{
				m_ignore.add(key);
			}
		} catch (final BackingStoreException e)
		{
			log.log(Level.FINE, e.getMessage(), e);
		}
	}
	
	public void add(final String name)
	{
		synchronized (m_lock)
		{
			m_ignore.add(name);
			final Preferences prefs = getPrefNode();
			prefs.put(name, "true");
			try
			{
				prefs.flush();
			} catch (final BackingStoreException e)
			{
				log.log(Level.FINE, e.getMessage(), e);
			}
		}
	}
	
	protected static Preferences getPrefNode()
	{
		return Preferences.userNodeForPackage(ChatIgnoreList.class);
	}
	
	public void remove(final String name)
	{
		synchronized (m_lock)
		{
			m_ignore.remove(name);
			final Preferences prefs = getPrefNode();
			prefs.remove(name);
			try
			{
				prefs.flush();
			} catch (final BackingStoreException e)
			{
				log.log(Level.FINE, e.getMessage(), e);
			}
		}
	}
	
	public boolean shouldIgnore(final String name)
	{
		synchronized (m_lock)
		{
			return m_ignore.contains(name);
		}
	}
}
