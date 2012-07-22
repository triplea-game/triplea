/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
/*
 * GameRunner.java
 * 
 * Created on December 14, 2001, 12:05 PM
 */
package games.strategy.engine.framework;

import games.strategy.engine.EngineVersion;

import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Window;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;

import javax.swing.JOptionPane;

/**
 * 
 * @author Sean Bridges
 * 
 *         This class starts and runs the game.
 *         <p>
 * 
 *         This class is compiled to run under older jdks (1.3 at least), and should not do anything more than check the java version number, and then delegate to GameRunner2
 *         <p>
 */
public class GameRunner
{
	public final static int PORT = 3300;
	
	public static Image getGameIcon(final Window frame)
	{
		Image img = null;
		try
		{
			img = frame.getToolkit().getImage(GameRunner.class.getResource("ta_icon.png"));
		} catch (final Exception ex)
		{
			System.out.println("icon not loaded");
		}
		final MediaTracker tracker = new MediaTracker(frame);
		tracker.addImage(img, 0);
		try
		{
			tracker.waitForAll();
		} catch (final InterruptedException ex)
		{
			ex.printStackTrace();
		}
		return img;
	}
	
	public static boolean isWindows()
	{
		return System.getProperties().getProperty("os.name").toLowerCase().indexOf("windows") != -1;
	}
	
	public static boolean isMac()
	{
		return System.getProperties().getProperty("os.name").toLowerCase().indexOf("mac") != -1;
	}
	
	/**
	 * Get version number of Java VM.
	 * 
	 * @author NeKromancer
	 */
	private static void checkJavaVersion()
	{
		// note - this method should not use any new language features (this includes string concatention using +
		// since this method must run on older vms.
		final String version = System.getProperties().getProperty("java.version");
		final boolean v12 = version.indexOf("1.2") != -1;
		final boolean v13 = version.indexOf("1.3") != -1;
		final boolean v14 = version.indexOf("1.4") != -1;
		if (v14 || v13 || v12)
		{
			if (!isMac())
			{
				JOptionPane.showMessageDialog(null, "TripleA requires a java runtime greater than or equal to 5.0.\nPlease download a newer version of java from http://java.sun.com/", "ERROR",
							JOptionPane.ERROR_MESSAGE);
				System.exit(-1);
			}
			else if (isMac())
			{
				JOptionPane.showMessageDialog(
							null,
							"TripleA requires a java runtime greater than or equal to 5.0 (Note, this requires Mac OS X >= 10.4)\nPlease download a newer version of java from http://www.apple.com/java/",
							"ERROR", JOptionPane.ERROR_MESSAGE);
				System.exit(-1);
			}
		}
	}// end checkJavaVersion()
	
	public static void main(final String[] args)
	{
		// we want this class to be executable in older jvm's
		// since we require jdk 1.5, this class delegates to GameRunner2
		// and all we do is check the java version
		checkJavaVersion();
		// do the other interesting stuff here
		GameRunner2.main(args);
	}
	
	public static File getUserRootFolder()
	{
		final File userHome = new File(System.getProperties().getProperty("user.home"));
		// the default
		File rootDir;
		if (GameRunner.isMac())
			rootDir = new File(new File(userHome, "Documents"), "triplea");
		else
			rootDir = new File(userHome, "triplea");
		return rootDir;
	}
	
	public static File getUserMapsFolder()
	{
		final File f = new File(getUserRootFolder(), "maps");
		if (!f.exists())
		{
			f.mkdirs();
		}
		return f;
	}
	
	/**
	 * Our jar is named with engine number and we are in "old" folder.
	 * 
	 * @return
	 */
	public static boolean areWeOldExtraJar()
	{
		final URL url = GameRunner.class.getResource("GameRunner.class");
		String fileName = url.getFile();
		try
		{
			fileName = URLDecoder.decode(fileName, "UTF-8");
		} catch (final UnsupportedEncodingException e)
		{
			e.printStackTrace();
		}
		final String tripleaJarNameWithEngineVersion = getTripleaJarWithEngineVersionStringPath();
		if (fileName.indexOf(tripleaJarNameWithEngineVersion) != -1)
		{
			final String subString = fileName.substring("file:/".length() - (isWindows() ? 0 : 1), fileName.indexOf(tripleaJarNameWithEngineVersion) - 1);
			final File f = new File(subString);
			if (!f.exists())
			{
				throw new IllegalStateException("File not found:" + f);
			}
			String path;
			try
			{
				path = f.getCanonicalPath();
			} catch (final IOException e)
			{
				path = f.getPath();
			}
			return path.indexOf("old") != -1;
		}
		return false;
	}
	
	private static String getTripleaJarWithEngineVersionStringPath()
	{
		return "triplea_" + EngineVersion.VERSION.toStringFull("_") + ".jar!";
	}
	
	/**
	 * Get the root folder for the application
	 */
	public static File getRootFolder()
	{
		// we know that the class file is in a directory one above the games root folder
		// so navigate up from the class file, and we have root.
		// find the url of our class
		final URL url = GameRunner.class.getResource("GameRunner.class");
		// we want to move up 1 directory for each
		// package
		final int moveUpCount = GameRunner.class.getName().split("\\.").length + 1;
		String fileName = url.getFile();
		try
		{
			// deal with spaces in the file name which would be url encoded
			fileName = URLDecoder.decode(fileName, "UTF-8");
		} catch (final UnsupportedEncodingException e)
		{
			e.printStackTrace();
		}
		final String tripleaJarName = "triplea.jar!";
		final String tripleaJarNameWithEngineVersion = getTripleaJarWithEngineVersionStringPath();
		// we are in a jar file
		if (fileName.indexOf(tripleaJarName) != -1)
		{
			final String subString = fileName.substring("file:/".length() - (isWindows() ? 0 : 1), fileName.indexOf(tripleaJarName) - 1);
			final File f = new File(subString).getParentFile();
			if (!f.exists())
			{
				throw new IllegalStateException("File not found:" + f);
			}
			return f;
		}
		else if (fileName.indexOf(tripleaJarNameWithEngineVersion) != -1)
		{
			final String subString = fileName.substring("file:/".length() - (isWindows() ? 0 : 1), fileName.indexOf(tripleaJarNameWithEngineVersion) - 1);
			final File f = new File(subString).getParentFile();
			if (!f.exists())
			{
				throw new IllegalStateException("File not found:" + f);
			}
			return f;
		}
		else
		{
			File f = new File(fileName);
			for (int i = 0; i < moveUpCount; i++)
			{
				f = f.getParentFile();
			}
			if (!f.exists())
			{
				System.err.println("Could not find root folder, does  not exist:" + f);
				return new File(System.getProperties().getProperty("user.dir"));
			}
			return f;
		}
	}
}
