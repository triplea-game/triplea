package games.strategy.engine.framework.mapDownload;

import games.strategy.util.Version;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

public class DownloadFileParser
{
	public List<DownloadFileDescription> parse(final InputStream is, final String hostedUrl)
	{
		final List<DownloadFileDescription> rVal = new ArrayList<DownloadFileDescription>();
		try
		{
			final SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
			parser.parse(new InputSource(is), new DefaultHandler()
			{
				private StringBuilder content = new StringBuilder();
				private String url;
				private String description;
				private String mapName;
				private Version version;
				
				@Override
				public void characters(final char[] ch, final int start, final int length) throws SAXException
				{
					content.append(ch, start, length);
				}
				
				@Override
				public void endElement(final String uri, final String localName, final String qName) throws SAXException
				{
					final String elementName = qName;
					if (elementName.equals("description"))
					{
						description = content.toString().trim();
					}
					else if (elementName.equals("url"))
					{
						url = content.toString().trim();
					}
					else if (elementName.equals("mapName"))
					{
						mapName = content.toString().trim();
					}
					else if (elementName.equals("version"))
					{
						this.version = new Version(content.toString().trim());
					}
					else if (elementName.equals("game"))
					{
						rVal.add(new DownloadFileDescription(url, description, mapName, version, hostedUrl));
						// clear optional properties
						version = null;
					}
					else if (!elementName.equals("games"))
					{
						throw new IllegalStateException("unexpected tag:" + elementName);
					}
					content = new StringBuilder();
				}
			});
		} catch (final RuntimeException e)
		{
			throw e;
		} catch (final SAXParseException e)
		{
			throw new IllegalStateException("Could not parse xml error at line:" + e.getLineNumber() + " column:" + e.getColumnNumber() + " error:" + e.getMessage());
		} catch (final Exception e)
		{
			throw new IllegalStateException(e);
		}
		validate(rVal);
		return rVal;
	}
	
	private void validate(final List<DownloadFileDescription> downloads)
	{
		final Set<String> urls = new HashSet<String>();
		final Set<String> names = new HashSet<String>();
		for (final DownloadFileDescription d : downloads)
		{
			if (isEmpty(d.getUrl()))
			{
				throw new IllegalStateException("Missing game url");
			}
			// ignore urls
			if (!d.isDummyUrl())
			{
				if (isEmpty(d.getDescription()))
				{
					throw new IllegalStateException("Missing game description");
				}
				if (isEmpty(d.getMapName()))
				{
					throw new IllegalStateException("Missing map name");
				}
				if (!names.add(d.getMapName()))
				{
					throw new IllegalStateException("duplicate mapName:" + d.getMapName());
				}
				if (!urls.add(d.getUrl()))
				{
					throw new IllegalStateException("duplicate url:" + d.getUrl());
				}
				try
				{
					final URL url = new URL(d.getUrl());
					if (!url.getProtocol().toLowerCase(Locale.ENGLISH).startsWith("http"))
					{
						throw new IllegalStateException("Url must start with http:" + url);
					}
				} catch (final MalformedURLException e)
				{
					throw new IllegalStateException(e);
				}
			}
		}
	}
	
	public boolean isEmpty(final String s)
	{
		return s == null || s.trim().length() == 0;
	}
}
