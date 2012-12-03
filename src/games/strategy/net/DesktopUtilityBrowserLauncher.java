package games.strategy.net;

import java.io.File;
import java.net.URI;
import java.util.Arrays;

import javax.swing.JOptionPane;

/**
 * <b>Bare Bones Browser Launch for Java</b><br>
 * Utility class to open a web page from a Swing application
 * in the user's default browser.<br>
 * Supports: Mac OS X, GNU/Linux, Unix, Windows XP/Vista/7<br>
 * Example Usage:<code><br> &nbsp; &nbsp;
 *    String url = "http://www.google.com/";<br> &nbsp; &nbsp;
 *    BareBonesBrowserLaunch.openURL(url);<br></code> Latest Version: <a href="http://www.centerkey.com/java/browser/">www.centerkey.com/java/browser</a><br>
 * Author: Dem Pilafian<br>
 * Public Domain Software -- Free to Use as You Like
 * 
 * @version 3.1, June 6, 2010
 */
public class DesktopUtilityBrowserLauncher
{
	static final String[] browsers = { "firefox", "google-chrome", "opera", "epiphany", "konqueror", "conkeror", "midori", "kazehakase", "mozilla", "netscape" };
	static final String errMsg = "Error attempting to launch web browser";
	
	/**
	 * Opens a specific file on the user's computer, using whatever default program is used to open such files, using the local computer's file associations. (veqryn)
	 * 
	 * @param file
	 */
	public static void openFile(final File file)
	{
		// openURL(file.toURI().toString());
		final URI uri = file.toURI();
		try
		{ // attempt to use Desktop library from JDK 1.6+
			final Class<?> d = Class.forName("java.awt.Desktop");
			d.getDeclaredMethod("browse", new Class[] { java.net.URI.class }).invoke(d.getDeclaredMethod("getDesktop").invoke(null), new Object[] { uri });
			// above code mimicks: java.awt.Desktop.getDesktop().browse()
		} catch (final Exception ignore)
		{ // library not available or failed
			final String url = uri.toString(); // we use "toString()" or to "toASCIIString()" because "getPath()" sometimes does not work.
			final String osName = System.getProperty("os.name");
			try
			{
				if (osName != null && osName.startsWith("Mac OS"))
				{
					try
					{
						Class.forName("com.apple.eio.FileManager").getDeclaredMethod("open", new Class[] { String.class }).invoke(null, new Object[] { url });
					} catch (final Exception e)
					{
						Runtime.getRuntime().exec("open " + url);
					}
				}
				else if (osName != null && osName.startsWith("Windows"))
				{
					Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + url);
				}
				else
				{ // assume Unix or Linux
					try
					{
						final String[] cmd = { "xdg-open", url };
						Runtime.getRuntime().exec(cmd);
					} catch (final Exception e)
					{
						try
						{
							final String[] cmdGnome = { "gnome-open", url };
							Runtime.getRuntime().exec(cmdGnome);
						} catch (final Exception e2)
						{
							final String[] cmdKDE = { "kfmclient", url };
							Runtime.getRuntime().exec(cmdKDE);
						}
					}
				}
			} catch (final Exception e)
			{
				JOptionPane.showMessageDialog(null, errMsg + "\n" + e.toString());
			}
		}
	}
	
	/**
	 * Opens the specified web page in the user's default browser
	 * 
	 * @param url
	 *            A web address (URL) of a web page (ex: "http://www.google.com/")
	 */
	public static void openURL(final String url)
	{
		try
		{ // attempt to use Desktop library from JDK 1.6+
			final Class<?> d = Class.forName("java.awt.Desktop");
			d.getDeclaredMethod("browse", new Class[] { java.net.URI.class }).invoke(d.getDeclaredMethod("getDesktop").invoke(null), new Object[] { java.net.URI.create(url) });
			// above code mimicks: java.awt.Desktop.getDesktop().browse()
		} catch (final Exception ignore)
		{ // library not available or failed
			final String osName = System.getProperty("os.name");
			try
			{
				if (osName != null && osName.startsWith("Mac OS"))
				{
					Class.forName("com.apple.eio.FileManager").getDeclaredMethod("openURL", new Class[] { String.class }).invoke(null, new Object[] { url });
				}
				else if (osName != null && osName.startsWith("Windows"))
				{
					Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + url);
				}
				else
				{ // assume Unix or Linux
					String browser = null;
					for (final String b : browsers)
						if (browser == null && Runtime.getRuntime().exec(new String[] { "which", b }).getInputStream().read() != -1)
							Runtime.getRuntime().exec(new String[] { browser = b, url });
					if (browser == null)
					{
						// Under Unix, Firefox has to be running for the "-remote"
						// command to work. So, we try sending the command and
						// check for an exit value. If the exit command is 0,
						// it worked, otherwise we need to start the browser.
						// cmd = 'firefox -remote openURL(http://www.javaworld.com)'
						String cmd = "firefox" + " " + "-remote openURL" + "(" + url + ")";
						try
						{
							Process p = Runtime.getRuntime().exec(cmd);
							// wait for exit code -- if it's 0, command worked,
							// otherwise we need to start the browser up.
							final int exitCode = p.waitFor();
							if (exitCode != 0)
							{
								// Command failed, start up the browser
								// cmd = 'firefox http://www.javaworld.com'
								cmd = "firefox" + " " + url;
								p = Runtime.getRuntime().exec(cmd);
							}
						} catch (final Exception x)
						{
							cmd = "netscape" + " " + "-remote openURL" + "(" + url + ")";
							try
							{
								Process p = Runtime.getRuntime().exec(cmd);
								// wait for exit code -- if it's 0, command worked,
								// otherwise we need to start the browser up.
								final int exitCode = p.waitFor();
								if (exitCode != 0)
								{
									// Command failed, start up the browser
									// cmd = 'firefox http://www.javaworld.com'
									cmd = "netscape" + " " + url;
									p = Runtime.getRuntime().exec(cmd);
								}
							} catch (final Exception x2)
							{
								throw new Exception(Arrays.toString(browsers));
							}
						}
					}
				}
			} catch (final Exception e)
			{
				JOptionPane.showMessageDialog(null, errMsg + "\n" + e.toString());
			}
		}
	}
}
