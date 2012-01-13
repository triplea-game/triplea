package games.strategy.engine.framework.ui;

import games.strategy.engine.data.EngineVersionException;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParseException;
import games.strategy.engine.data.GameParser;
import games.strategy.engine.framework.GameRunner;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import org.xml.sax.SAXException;

public class NewGameChooserEntry
{
	private final URI m_url;
	private final GameData m_data;
	
	public NewGameChooserEntry(final URI uri) throws IOException, GameParseException, SAXException, EngineVersionException
	{
		m_url = uri;
		final InputStream input = uri.toURL().openStream();
		try
		{
			m_data = new GameParser().parse(input);
		} finally
		{
			try
			{
				input.close();
			} catch (final IOException e)
			{// ignore
			}
		}
	}
	
	@Override
	public String toString()
	{
		return m_data.getGameName();
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
		final String base = GameRunner.getRootFolder().toURI().toString() + "maps";
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
}
