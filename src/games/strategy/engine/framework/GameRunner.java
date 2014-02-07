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
		final boolean v15 = version.indexOf("1.5") != -1;
		if (v15 || v14 || v13 || v12)
		{
			if (isMac())
			{
				JOptionPane.showMessageDialog(
							null,
							"TripleA requires a java runtime greater than or equal to 6 [ie: Java 6]. (Note, this requires Mac OS X >= 10.5 or 10.6 depending.) \nPlease download a newer version of java from http://www.java.com/",
							"ERROR", JOptionPane.ERROR_MESSAGE);
				System.exit(-1);
			}
			else
			{
				JOptionPane.showMessageDialog(null, "TripleA requires a java runtime greater than or equal to 6 [ie: Java 6]. \nPlease download a newer version of java from http://www.java.com/",
							"ERROR",
							JOptionPane.ERROR_MESSAGE);
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
}
