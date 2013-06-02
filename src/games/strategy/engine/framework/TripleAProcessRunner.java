package games.strategy.engine.framework;

import games.strategy.engine.EngineVersion;
import games.strategy.engine.lobby.server.GameDescription;
import games.strategy.engine.lobby.server.GameDescription.GameStatus;
import games.strategy.net.Messengers;
import games.strategy.util.Version;

import java.awt.Container;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JOptionPane;

public class TripleAProcessRunner
{
	public static void startNewTripleA(final Long maxMemory)
	{
		startGame(System.getProperty(GameRunner2.TRIPLEA_GAME_PROPERTY), null, maxMemory);
	}
	
	public static void startGame(final String savegamePath, final String classpath, final Long maxMemory)
	{
		final List<String> commands = new ArrayList<String>();
		if (maxMemory != null && maxMemory > (32 * 1024 * 1024))
			ProcessRunnerUtil.populateBasicJavaArgs(commands, classpath, maxMemory);
		else
			ProcessRunnerUtil.populateBasicJavaArgs(commands, classpath);
		if (savegamePath != null && savegamePath.length() > 0)
			commands.add("-D" + GameRunner2.TRIPLEA_GAME_PROPERTY + "=" + savegamePath);
		// add in any existing command line items
		for (final String property : GameRunner2.getProperties())
		{
			// we add game property above, and we add version bin in the populateBasicJavaArgs
			if (GameRunner2.TRIPLEA_GAME_PROPERTY.equals(property) || GameRunner2.TRIPLEA_ENGINE_VERSION_BIN.equals(property))
				continue;
			final String value = System.getProperty(property);
			if (value != null)
			{
				commands.add("-D" + property + "=" + value);
			}
			else if (GameRunner2.LOBBY_HOST.equals(property) || GameRunner2.LOBBY_PORT.equals(property) || GameRunner2.LOBBY_GAME_HOSTED_BY.equals(property))
			{
				// for these 3 properties, we clear them after hosting, but back them up.
				final String oldValue = System.getProperty(property + GameRunner2.OLD_EXTENSION);
				if (oldValue != null)
				{
					commands.add("-D" + property + "=" + oldValue);
				}
			}
		}
		// classpath for main
		final String javaClass = "games.strategy.engine.framework.GameRunner";
		commands.add(javaClass);
		// System.out.println("Commands: " + commands);
		ProcessRunnerUtil.exec(commands);
	}
	
	public static void hostGame(final int port, final String playerName, final String comments, final String password, final Messengers messengers)
	{
		final List<String> commands = new ArrayList<String>();
		ProcessRunnerUtil.populateBasicJavaArgs(commands);
		commands.add("-D" + GameRunner2.TRIPLEA_SERVER_PROPERTY + "=true");
		commands.add("-D" + GameRunner2.TRIPLEA_PORT_PROPERTY + "=" + port);
		commands.add("-D" + GameRunner2.TRIPLEA_NAME_PROPERTY + "=" + playerName);
		commands.add("-D" + GameRunner2.LOBBY_HOST + "=" + messengers.getMessenger().getRemoteServerSocketAddress().getAddress().getHostAddress());
		commands.add("-D" + GameRunner2.LOBBY_PORT + "=" + messengers.getMessenger().getRemoteServerSocketAddress().getPort());
		commands.add("-D" + GameRunner2.LOBBY_GAME_COMMENTS + "=" + comments);
		commands.add("-D" + GameRunner2.LOBBY_GAME_HOSTED_BY + "=" + messengers.getMessenger().getLocalNode().getName());
		if (password != null && password.length() > 0)
			commands.add("-D" + GameRunner2.TRIPLEA_SERVER_PASSWORD_PROPERTY + "=" + password);
		final String fileName = System.getProperty(GameRunner2.TRIPLEA_GAME_PROPERTY, "");
		if (fileName.length() > 0)
			commands.add("-D" + GameRunner2.TRIPLEA_GAME_PROPERTY + "=" + fileName);
		final String javaClass = "games.strategy.engine.framework.GameRunner";
		commands.add(javaClass);
		ProcessRunnerUtil.exec(commands);
	}
	
	public static void joinGame(final GameDescription description, final Messengers messengers, final Container parent)
	{
		final GameStatus status = description.getStatus();
		if (GameStatus.LAUNCHING.equals(status))
			return;
		final Version engineVersionOfGameToJoin = new Version(description.getEngineVersion());
		String newClassPath = null;
		if (!EngineVersion.VERSION.equals(engineVersionOfGameToJoin))
		{
			try
			{
				newClassPath = findOldJar(engineVersionOfGameToJoin, false);
			} catch (final Exception e)
			{
				if (GameRunner2.areWeOldExtraJar())
				{
					JOptionPane.showMessageDialog(parent, "<html>Please run the default TripleA and try joining the online lobby for it instead. " +
								"<br>This TripleA engine is old and kept only for backwards compatibility and can only play with people using the exact same version as this one. " +
								"<br><br>Host is using a different engine than you, and can not find correct engine: " + engineVersionOfGameToJoin.toStringFull("_") + "</html>",
								"Correct TripleA Engine Not Found", JOptionPane.WARNING_MESSAGE);
				}
				else
				{
					JOptionPane.showMessageDialog(parent, "Host is using a different engine than you, and can not find correct engine: " + engineVersionOfGameToJoin.toStringFull("_"),
								"Correct TripleA Engine Not Found", JOptionPane.WARNING_MESSAGE);
				}
				return;
			}
			// ask user if we really want to do this?
			final String messageString = "<html>This TripleA engine is version "
						+ EngineVersion.VERSION.toString()
						+ " and you are trying to join a game made with version "
						+ engineVersionOfGameToJoin.toString()
						+ "<br>However, this TripleA can only play with engines that are the exact same version as itself (x_x_x_x)."
						+ "<br><br>TripleA now comes with older engines included with it, and has found the engine used by the host. This is a new feature and is in 'beta' stage."
						+ "<br>It will attempt to run a new instance of TripleA using the older engine jar file, and this instance will join the host's game."
						+ "<br>Your current instance will not be closed. Please report any bugs or issues."
						+ "<br><br>Do you wish to continue?</html>";
			final int answer = JOptionPane.showConfirmDialog(null, messageString, "Run old jar to join hosted game?", JOptionPane.YES_NO_OPTION);
			if (answer != JOptionPane.YES_OPTION)
				return;
		}
		joinGame(description.getPort(), description.getHostedBy().getAddress().getHostAddress(), newClassPath, messengers);
	}
	
	// newClassPath can be null
	public static void joinGame(final int port, final String hostAddressIP, final String newClassPath, final Messengers messengers)
	{
		final List<String> commands = new ArrayList<String>();
		ProcessRunnerUtil.populateBasicJavaArgs(commands, newClassPath);
		commands.add("-D" + GameRunner2.TRIPLEA_CLIENT_PROPERTY + "=true");
		commands.add("-D" + GameRunner2.TRIPLEA_PORT_PROPERTY + "=" + port);
		commands.add("-D" + GameRunner2.TRIPLEA_HOST_PROPERTY + "=" + hostAddressIP);
		commands.add("-D" + GameRunner2.TRIPLEA_NAME_PROPERTY + "=" + messengers.getMessenger().getLocalNode().getName());
		final String javaClass = "games.strategy.engine.framework.GameRunner";
		commands.add(javaClass);
		ProcessRunnerUtil.exec(commands);
	}
	
	public static String findOldJar(final Version oldVersionNeeded, final boolean ignoreMicro) throws IOException
	{
		if (EngineVersion.VERSION.equals(oldVersionNeeded, ignoreMicro))
			return System.getProperty("java.class.path");
		// first, see if the default/main triplea can run it
		if (GameRunner2.areWeOldExtraJar())
		{
			final String version = System.getProperty(GameRunner2.TRIPLEA_ENGINE_VERSION_BIN);
			if (version != null && version.length() > 0)
			{
				Version defaultVersion = null;
				try
				{
					defaultVersion = new Version(version);
				} catch (final Exception e)
				{
					// nothing, just continue
				}
				if (defaultVersion != null)
				{
					if (defaultVersion.equals(oldVersionNeeded, ignoreMicro))
					{
						final String jarName = "triplea.jar";
						// windows is in 'bin' folder, mac is in 'Java' folder.
						File binFolder = new File(GameRunner2.getRootFolder(), "bin/");
						if (!binFolder.exists())
							binFolder = new File(GameRunner2.getRootFolder(), "Java/");
						if (binFolder.exists())
						{
							final File[] files = binFolder.listFiles();
							if (files == null)
								throw new IOException("Can not find 'bin' engine jars folder");
							File ourBinJar = null;
							for (final File f : Arrays.asList(files))
							{
								if (!f.exists())
									continue;
								final String jarPath = f.getCanonicalPath();
								if (jarPath.indexOf(jarName) != -1)
								{
									ourBinJar = f;
									break;
								}
							}
							if (ourBinJar == null)
								throw new IOException("Can not find 'bin' engine jar for version: " + oldVersionNeeded.toStringFull("_"));
							final String newClassPath = ourBinJar.getCanonicalPath();
							if (newClassPath == null || newClassPath.length() <= 0)
								throw new IOException("Can not find 'bin' engine jar for version: " + oldVersionNeeded.toStringFull("_"));
							return newClassPath;
						}
						else
							System.err.println("Can not find 'bin' or 'Java' folder, where main triplea.jar should be.");
					}
				}
			}
		}
		// so, what we do here is try to see if our installed copy of triplea includes older jars with it that are the same engine as was used for this savegame, and if so try to run it
		// System.out.println("System classpath: " + System.getProperty("java.class.path"));
		// we don't care what the last (micro) number is of the version number. example: triplea 1.5.2.1 can open 1.5.2.0 savegames.
		final String jarName = "triplea_" + oldVersionNeeded.toStringFull("_", ignoreMicro);
		final File oldJarsFolder = new File(GameRunner2.getRootFolder(), "old/");
		if (!oldJarsFolder.exists())
			throw new IOException("Can not find 'old' engine jars folder");
		final File[] files = oldJarsFolder.listFiles();
		if (files == null)
			throw new IOException("Can not find 'old' engine jars folder");
		File ourOldJar = null;
		for (final File f : Arrays.asList(files))
		{
			if (!f.exists())
				continue;
			// final String jarPath = f.getCanonicalPath();
			final String name = f.getName();
			if (name.indexOf(jarName) != -1 && name.indexOf(".jar") != -1)
			{
				ourOldJar = f;
				break;
			}
		}
		if (ourOldJar == null)
			throw new IOException("Can not find 'old' engine jar for version: " + oldVersionNeeded.toStringFull("_"));
		final String newClassPath = ourOldJar.getCanonicalPath();
		if (newClassPath == null || newClassPath.length() <= 0)
			throw new IOException("Can not find 'old' engine jar for version: " + oldVersionNeeded.toStringFull("_"));
		return newClassPath;
	}
}
