package games.strategy.engine.framework.ui;

import games.strategy.engine.data.EngineVersionException;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParseException;
import games.strategy.engine.data.GameParser;
import games.strategy.engine.framework.GameRunner2;
import games.strategy.triplea.Constants;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.concurrent.atomic.AtomicReference;

import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class NewGameChooserEntry
{
	private final URI m_url;
	private GameData m_data;
	private boolean m_gameDataFullyLoaded = false;
	private final String m_gameNameAndMapNameProperty;
	
	public NewGameChooserEntry(final URI uri) throws IOException, GameParseException, SAXException, EngineVersionException
	{
		m_url = uri;
		InputStream input = null;
		final AtomicReference<String> gameName = new AtomicReference<String>();
		try
		{
			input = uri.toURL().openStream();
			final boolean delayParsing = GameRunner2.getDelayedParsing();
			m_data = new GameParser().parse(input, gameName, delayParsing);
			m_gameDataFullyLoaded = !delayParsing;
			m_gameNameAndMapNameProperty = getGameName() + ":" + getMapNameProperty();
		} finally
		{
			try
			{
				if (input != null)
				{
					input.close();
				}
			} catch (final IOException e)
			{// ignore
			}
		}
	}
	
	public void fullyParseGameData() throws GameParseException
	{
		m_data = null;
		InputStream input = null;
		String error = null;
		final AtomicReference<String> gameName = new AtomicReference<String>();
		try
		{
			input = m_url.toURL().openStream();
			try
			{
				m_data = new GameParser().parse(input, gameName, false);
				m_gameDataFullyLoaded = true;
			} catch (final EngineVersionException e)
			{
				System.out.println(e.getMessage());
				error = e.getMessage();
			} catch (final SAXParseException e)
			{
				System.err.println("Could not parse:" + m_url + " error at line:" + e.getLineNumber() + " column:" + e.getColumnNumber());
				e.printStackTrace();
				error = e.getMessage();
			} catch (final Exception e)
			{
				System.err.println("Could not parse:" + m_url);
				e.printStackTrace();
				error = e.getMessage();
			}
		} catch (final MalformedURLException e1)
		{
			e1.printStackTrace();
			error = e1.getMessage();
		} catch (final IOException e1)
		{
			e1.printStackTrace();
			error = e1.getMessage();
		} finally
		{
			try
			{
				if (input != null)
				{
					input.close();
				}
			} catch (final IOException e)
			{// ignore
			}
		}
		if (error != null)
		{
			throw new GameParseException(error);
		}
	}
	
	/**
	 * Do not use this if possible. Instead try to remove the bad map from the GameChooserModel.
	 * If that fails, then do a short parse so the user doesn't get a null pointer error.
	 */
	public void delayParseGameData()
	{
		m_data = null;
		InputStream input = null;
		final AtomicReference<String> gameName = new AtomicReference<String>();
		try
		{
			input = m_url.toURL().openStream();
			try
			{
				m_data = new GameParser().parse(input, gameName, true);
				m_gameDataFullyLoaded = false;
			} catch (final EngineVersionException e)
			{
				System.out.println(e.getMessage());
			} catch (final SAXParseException e)
			{
				System.err.println("Could not parse:" + m_url + " error at line:" + e.getLineNumber() + " column:" + e.getColumnNumber());
				e.printStackTrace();
			} catch (final Exception e)
			{
				System.err.println("Could not parse:" + m_url);
				e.printStackTrace();
			}
		} catch (final MalformedURLException e1)
		{
			e1.printStackTrace();
		} catch (final IOException e1)
		{
			e1.printStackTrace();
		} finally
		{
			try
			{
				if (input != null)
				{
					input.close();
				}
			} catch (final IOException e)
			{// ignore
			}
		}
	}
	
	public boolean isGameDataLoaded()
	{
		return m_gameDataFullyLoaded;
	}
	
	public String getGameName()
	{
		return m_data.getGameName();
	}
	
	// the user may have selected a map skin instead of this map folder, so don't use this for anything except our equals/hashcode below
	private String getMapNameProperty()
	{
		final String mapName = (String) m_data.getProperties().get(Constants.MAP_NAME);
		if (mapName == null || mapName.trim().length() == 0)
		{
			throw new IllegalStateException("Map name property not set on game");
		}
		return mapName;
	}
	
	public String getGameNameAndMapNameProperty()
	{
		return m_gameNameAndMapNameProperty;
	}
	
	@Override
	public String toString()
	{
		return getGameName();
	}
	
	public GameData getGameData()
	{
		return m_data;
	}
	
	public URI getURI()
	{
		return m_url;
	}
	
	public String getLocation()
	{
		final String raw = m_url.toString();
		final String base = GameRunner2.getRootFolder().toURI().toString() + "maps";
		if (raw.startsWith(base))
		{
			return raw.substring(base.length());
		}
		if (raw.startsWith("jar:" + base))
		{
			return raw.substring("jar:".length() + base.length());
		}
		return raw;
	}
	
	@Override
	public int hashCode()
	{
		return getGameNameAndMapNameProperty().hashCode();
	}
	
	@Override
	public boolean equals(final Object obj)
	{
		if (this == obj)
		{
			return true;
		}
		if (obj == null)
		{
			return false;
		}
		if (getClass() != obj.getClass())
		{
			return false;
		}
		final NewGameChooserEntry other = (NewGameChooserEntry) obj;
		if (m_data == null)
		{
			if (other.m_data != null)
			{
				return false;
			}
		}
		else
		{
			if (other.m_data == null)
			{
				return false;
			}
		}
		return this.getGameNameAndMapNameProperty().equals(other.getGameNameAndMapNameProperty());
	}
}
