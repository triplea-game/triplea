package games.strategy.triplea;

import games.strategy.engine.framework.GameRunner;
import games.strategy.util.ClassLoaderUtil;
import games.strategy.util.Match;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.StringTokenizer;

/**
 * 
 * Utility for managing where images and property files for maps and units should be loaded from.
 * 
 * Based on java Classloaders.
 * 
 */
public class ResourceLoader
{
	private final URLClassLoader m_loader;
	public static String RESOURCE_FOLDER = "assets";
	
	public static ResourceLoader getMapResourceLoader(final String mapName)
	{
		final List<String> dirs = getPaths(mapName);
		dirs.add(new File(GameRunner.getRootFolder(), RESOURCE_FOLDER).getAbsolutePath());
		return new ResourceLoader(dirs.toArray(new String[0]));
	}
	
	private static List<String> getPaths(final String mapName)
	{
		if (mapName == null)
			return new ArrayList<String>();
		// find the primary directory/file
		final String dirName = File.separator + mapName;
		final String zipName = dirName + ".zip";
		final List<File> candidates = new ArrayList<File>();
		// prioritize user maps folder over root folder
		candidates.add(new File(GameRunner.getUserMapsFolder(), dirName));
		candidates.add(new File(GameRunner.getUserMapsFolder(), zipName));
		candidates.add(new File(GameRunner.getRootFolder() + File.separator + "maps", dirName));
		candidates.add(new File(GameRunner.getRootFolder() + File.separator + "maps", zipName));
		final Collection<File> existing = Match.getMatches(candidates, new Match<File>()
		{
			@Override
			public boolean match(final File f)
			{
				return f.exists();
			}
		});
		if (existing.size() > 1)
		{
			System.out.println("INFO: Found too many files for: " + mapName + "  found: " + existing);
			// we no longer throw this error message, instead we simply use the first one we find (prioritizing the user maps folder over the root folder)
			// throw new IllegalStateException("Found too many files for: " + mapName + "  found: " + existing);
		}
		// At least one must exist
		if (existing.isEmpty())
		{
			throw new IllegalStateException("Could not find file folder or zip for map: " + mapName + "\r\n" + "Please DOWNLOAD THIS MAP if you do not have it." + "\r\n"
						+ "If you are making a map or mod, make sure the mapName property within the xml game file exactly matches the map zip or folder name." + "\r\n" + "\r\n");
		}
		final File match = existing.iterator().next();
		String fileName = match.getName();
		if (fileName.indexOf('.') > 0)
		{
			fileName = fileName.substring(0, fileName.lastIndexOf('.'));
		}
		if (!fileName.equals(mapName))
		{
			throw new IllegalStateException("Map case is incorrect, xml: " + mapName + " file: " + match.getName() + "\r\n"
						+ "Make sure the mapName property within the xml game file exactly matches the map zip or folder name." + "\r\n");
		}
		final List<String> rVal = new ArrayList<String>();
		rVal.add(match.getAbsolutePath());
		// find dependencies
		try
		{
			final URLClassLoader url = new URLClassLoader(new URL[] { match.toURI().toURL() });
			final URL dependencesURL = url.getResource("dependencies.txt");
			ClassLoaderUtil.closeLoader(url);
			if (dependencesURL != null)
			{
				final java.util.Properties dependenciesFile = new java.util.Properties();
				final InputStream stream = dependencesURL.openStream();
				try
				{
					dependenciesFile.load(stream);
					final String dependencies = dependenciesFile.getProperty("dependencies");
					final StringTokenizer tokens = new StringTokenizer(dependencies, ",", false);
					while (tokens.hasMoreTokens())
					{
						// add the dependencies recursivly
						rVal.addAll(getPaths(tokens.nextToken()));
					}
				} finally
				{
					stream.close();
				}
			}
		} catch (final Exception e)
		{
			e.printStackTrace();
			throw new IllegalStateException(e.getMessage());
		}
		return rVal;
	}
	
	public void close()
	{
		ClassLoaderUtil.closeLoader(m_loader);
	}
	
	private ResourceLoader(final String[] paths)
	{
		final URL[] urls = new URL[paths.length];
		for (int i = 0; i < paths.length; i++)
		{
			final File f = new File(paths[i]);
			if (!f.exists())
			{
				System.err.println(f + " does not exist");
			}
			if (!f.isDirectory() && !f.getName().endsWith(".zip"))
			{
				System.err.println(f + " is not a directory or a zip file");
			}
			try
			{
				urls[i] = f.toURI().toURL();
			} catch (final MalformedURLException e)
			{
				e.printStackTrace();
				throw new IllegalStateException(e.getMessage());
			}
		}
		m_loader = new URLClassLoader(urls);
	}
	
	public boolean hasPath(final String path)
	{
		final URL rVal = m_loader.getResource(path);
		if (rVal == null)
			return false;
		return true;
	}
	
	public URL getResource(final String path)
	{
		final URL rVal = m_loader.getResource(path);
		if (rVal == null)
		{
			return null;
		}
		String fileName;
		try
		{
			fileName = URLDecoder.decode(rVal.getFile(), "utf-8");
		} catch (final IOException e)
		{
			throw new IllegalStateException(e);
		}
		if (!fileName.endsWith(path))
		{
			throw new IllegalStateException("The file:" + fileName + "  does not have the correct case.  It must match the case declared in the xml:" + path);
		}
		return rVal;
	}
	
	public InputStream getResourceAsStream(final String path)
	{
		return m_loader.getResourceAsStream(path);
	}
}
