package games.strategy.engine.framework;

import games.strategy.util.Version;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * To hold various static utility methods for running a java program.
 * 
 * @author veqryn
 * 
 */
public class ProcessRunnerUtil
{
	public static void populateBasicJavaArgs(final List<String> commands)
	{
		populateBasicJavaArgs(commands, System.getProperty("java.class.path"));
	}
	
	public static void populateBasicJavaArgs(final List<String> commands, final long maxMemory)
	{
		populateBasicJavaArgs(commands, System.getProperty("java.class.path"), maxMemory);
	}
	
	public static void populateBasicJavaArgs(final List<String> commands, final String newClasspath)
	{
		// for whatever reason, .maxMemory() returns a value about 12% smaller than the real Xmx value, so we are going to add 64m to that to compensate
		final long maxMemory = ((long) (Runtime.getRuntime().maxMemory() * 1.15) + 67108864);
		populateBasicJavaArgs(commands, newClasspath, maxMemory);
	}
	
	public static void populateBasicJavaArgs(final List<String> commands, final String classpath, final long maxMemory)
	{
		final String javaCommand = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
		commands.add(javaCommand);
		commands.add("-classpath");
		if (classpath != null && classpath.length() > 0)
			commands.add(classpath);
		else
			commands.add(System.getProperty("java.class.path"));
		commands.add("-Xmx" + maxMemory);
		// commands.add("-Xmx768m"); //TODO: This may need updating to something higher, like 896m or 1024m ((started out at 128m, then 256m, then 384m, then 512m, then 640m, now 768m).
		// preserve noddraw to fix 1742775
		final String[] preservedSystemProperties = { "sun.java2d.noddraw" };
		for (final String key : preservedSystemProperties)
		{
			if (System.getProperties().getProperty(key) != null)
			{
				final String value = System.getProperties().getProperty(key);
				if (value.matches("[a-zA-Z0-9.]+"))
				{
					commands.add("-D" + key + "=" + value);
				}
			}
		}
		if (GameRunner.isMac())
		{
			commands.add("-Dapple.laf.useScreenMenuBar=true");
			commands.add("-Xdock:name=\"TripleA\"");
			final File icons = new File(GameRunner2.getRootFolder(), "icons/triplea_icon.png");
			if (!icons.exists())
				throw new IllegalStateException("Icon file not found");
			commands.add("-Xdock:icon=" + icons.getAbsolutePath() + "");
		}
		final String version = System.getProperty(GameRunner2.TRIPLEA_ENGINE_VERSION_BIN);
		if (version != null && version.length() > 0)
		{
			final Version testVersion;
			try
			{
				testVersion = new Version(version);
				commands.add("-D" + GameRunner2.TRIPLEA_ENGINE_VERSION_BIN + "=" + testVersion.toString());
			} catch (final Exception e)
			{
				// nothing
			}
		}
	}
	
	public static void exec(final List<String> commands)
	{
		final ProcessBuilder builder = new ProcessBuilder(commands);
		// merge the streams, so we only have to start one reader thread
		builder.redirectErrorStream(true);
		try
		{
			final Process p = builder.start();
			final InputStream s = p.getInputStream();
			// we need to read the input stream to prevent possible
			// deadlocks
			final Thread t = new Thread(new Runnable()
			{
				public void run()
				{
					try
					{
						while (s.read() >= 0)
						{
							// just read
						}
					} catch (final IOException e)
					{
						e.printStackTrace();
					}
				}
			}, "Process ouput gobbler");
			t.setDaemon(true);
			t.start();
		} catch (final IOException ioe)
		{
			ioe.printStackTrace();
		}
	}
	
}
