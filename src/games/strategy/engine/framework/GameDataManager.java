package games.strategy.engine.framework;

import games.strategy.engine.EngineVersion;
import games.strategy.engine.data.GameData;
import games.strategy.engine.delegate.IDelegate;
import games.strategy.engine.framework.headlessGameServer.HeadlessGameServer;
import games.strategy.util.Version;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Iterator;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.swing.JDialog;
import javax.swing.JOptionPane;

/**
 * <p>
 * Title: TripleA
 * </p>
 * <p>
 * Description: Responsible for loading saved games, new games from xml, and saving games
 * </p>
 * 
 * @author Sean Bridges
 */
public class GameDataManager
{
	private final static String DELEGATE_START = "<DelegateStart>";
	private final static String DELEGATE_DATA_NEXT = "<DelegateData>";
	private final static String DELEGATE_LIST_END = "<EndDelegateList>";
	
	public GameDataManager()
	{
	}
	
	public GameData loadGame(final File savedGameFile) throws IOException
	{
		InputStream input = null;
		try
		{
			input = new BufferedInputStream(new FileInputStream(savedGameFile));
			String path;
			try
			{
				path = savedGameFile.getCanonicalPath();
			} catch (final IOException e)
			{
				path = savedGameFile.getPath();
			}
			return loadGame(input, path);
		} finally
		{
			try
			{
				if (input != null)
					input.close();
			} catch (final Exception e)
			{
			}
		}
	}
	
	public GameData loadGame(final InputStream input, final String path) throws IOException
	{
		return loadGame(new ObjectInputStream(new GZIPInputStream(input)), path);
	}
	
	public GameData loadGame(final ObjectInputStream input, final String savegamePath) throws IOException
	{
		try
		{
			final Version readVersion = (Version) input.readObject();
			final boolean headless = HeadlessGameServer.headless();
			if (!readVersion.equals(EngineVersion.VERSION, true))
			{
				// a hack for now, but a headless server should not try to open any savegame that is not its version
				if (headless)
				{
					final String message = "Incompatible game save, we are: " + EngineVersion.VERSION + "  Trying to load game created with: " + readVersion;
					HeadlessGameServer.sendChat(message);
					System.out.println(message);
					return null;
				}
				final String error = "<html>Incompatible engine versions, and no old engine found. We are: " + EngineVersion.VERSION + " . Trying to load game created with: " + readVersion
							+ "<br>To download the latest version of TripleA, Please visit http://triplea.sourceforge.net/</html>";
				if (savegamePath == null)
					throw new IOException(error);
				
				// so, what we do here is try to see if our installed copy of triplea includes older jars with it that are the same engine as was used for this savegame, and if so try to run it
				try
				{
					final String newClassPath = TripleAProcessRunner.findOldJar(readVersion, true);
					// ask user if we really want to do this?
					final String messageString = "<html>This TripleA engine is version "
								+ EngineVersion.VERSION.toString()
								+ " and you are trying to open a savegame made with version "
								+ readVersion.toString()
								+ "<br>However, this TripleA can not open any savegame made by any engine other than engines with the same first three version numbers as it (x_x_x_x)."
								+ "<br><br>TripleA now comes with older engines included with it, and has found the engine to run this savegame. This is a new feature and is in 'beta' stage."
								+ "<br>It will attempt to run a new instance of TripleA using the older engine jar file, and this instance will only be able to play this savegame."
								+ "<br><b>You may choose to either Close or Keep the current instance of TripleA!</b> (If hosting, you must close it). Please report any bugs or issues."
								+ "<br><br>Do you wish to continue?</html>";
					
					final String yesClose = "Yes & Close Current";
					final String yesOpen = "Yes & Do Not Close";
					final String cancel = "Cancel";
					final Object[] options = new Object[] { yesClose, yesOpen, cancel };
					final JOptionPane pane = new JOptionPane(messageString, JOptionPane.PLAIN_MESSAGE, JOptionPane.YES_NO_CANCEL_OPTION, null, options, yesClose);
					final JDialog window = pane.createDialog(null, "Run old jar to open old Save Game?");
					window.setVisible(true);
					final Object buttonPressed = pane.getValue();
					if (buttonPressed == null || buttonPressed.equals(cancel))
					{
						return null;
					}
					final boolean closeCurrentInstance = buttonPressed.equals(yesClose);
					TripleAProcessRunner.startGame(savegamePath, newClassPath, null);
					if (closeCurrentInstance)
					{
						try
						{
							Thread.sleep(1000);
						} catch (final InterruptedException e)
						{
						}
						System.exit(0);
					}
				} catch (final IOException e)
				{
					if (GameRunner2.areWeOldExtraJar())
					{
						throw new IOException("<html>Please run the default TripleA and try to open this game again. " +
									"<br>This TripleA engine is old and kept only for backwards compatibility and can only open savegames created by engines with these first 3 version digits: " +
									EngineVersion.VERSION.toStringFull("_", true) + "</html>");
					}
					else
						throw new IOException(error);
				}
				return null;
			}
			else if (!headless && readVersion.isGreaterThan(EngineVersion.VERSION, false))
			{
				// we can still load it because first 3 numbers of the version are the same, however this save was made by a newer engine, so prompt the user to upgrade
				final String messageString = "<html>Your TripleA engine is OUT OF DATE.  This save was made by a newer version of TripleA."
							+ "<br>However, because the first 3 version numbers are the same as your current version, we can still open the savegame."
							+ "<br><br>This TripleA engine is version "
							+ EngineVersion.VERSION.toStringFull("_")
							+ " and you are trying to open a savegame made with version "
							+ readVersion.toStringFull("_")
							+ "<br><br>To download the latest version of TripleA, Please visit http://triplea.sourceforge.net/"
							+ "<br><br>It is recommended that you upgrade to the latest version of TripleA before playing this savegame."
							+ "<br><br>Do you wish to continue and open this save with your current 'old' version?</html>";
				final int answer = JOptionPane.showConfirmDialog(null, messageString, "Open Newer Save Game?", JOptionPane.YES_NO_OPTION);
				if (answer != JOptionPane.YES_OPTION)
					return null;
			}
			final GameData data = (GameData) input.readObject();
			updateDataToBeCompatibleWithNewEngine(readVersion, data); // TODO: expand this functionality (and keep it updated)
			loadDelegates(input, data);
			data.postDeSerialize();
			return data;
		} catch (final ClassNotFoundException cnfe)
		{
			throw new IOException(cnfe.getMessage());
		}
	}
	
	/**
	 * Use this to keep compatibility between savegames when it is easy to do so.
	 * When it is not easy to do so, just make sure to include the last release's .jar file in the "old" folder for triplea.
	 * 
	 * FYI: Engine version numbers work like this with regards to savegames:
	 * Any changes to the first 3 digits means that the savegame is not compatible between different engines.
	 * While any change only to the 4th (last) digit means that the savegame must be compatible between different engines.
	 * 
	 * @param originalEngineVersion
	 * @param data
	 */
	private void updateDataToBeCompatibleWithNewEngine(final Version originalEngineVersion, final GameData data)
	{
		// whenever this gets out of date, just comment out (but keep as an example, by commenting out)
		/* example1:
		final Version v1610 = new Version(1, 6, 1, 0);
		final Version v1620 = new Version(1, 6, 2, 0);
		if (originalEngineVersion.equals(v1610, false) && EngineVersion.VERSION.isGreaterThan(v1610, false) && EngineVersion.VERSION.isLessThan(v1620, true))
		{
			// if original save was done under 1.6.1.0, and new engine is greater than 1.6.1.0 and less than 1.6.2.0
			try
			{
				if (TechAdvance.getTechAdvances(data).isEmpty())
				{
					System.out.println("Adding tech to be compatible with 1.6.1.x");
					TechAdvance.createDefaultTechAdvances(data);
					TechAbilityAttachment.setDefaultTechnologyAttachments(data);
				}
			} catch (final Exception e)
			{
				e.printStackTrace();
			}
		}*/
	}
	
	private void loadDelegates(final ObjectInputStream input, final GameData data) throws ClassNotFoundException, IOException
	{
		for (Object endMarker = input.readObject(); !endMarker.equals(DELEGATE_LIST_END); endMarker = input.readObject())
		{
			final String name = (String) input.readObject();
			final String displayName = (String) input.readObject();
			final String className = (String) input.readObject();
			IDelegate instance;
			try
			{
				instance = (IDelegate) Class.forName(className).newInstance();
				instance.initialize(name, displayName);
				data.getDelegateList().addDelegate(instance);
			} catch (final Exception e)
			{
				e.printStackTrace();
				throw new IOException(e.getMessage());
			}
			final String next = (String) input.readObject();
			if (next.equals(DELEGATE_DATA_NEXT))
			{
				instance.loadState((Serializable) input.readObject());
			}
		}
	}
	
	public void saveGame(final File destination, final GameData data) throws IOException
	{
		BufferedOutputStream out = null;
		try
		{
			final OutputStream fileStream = new FileOutputStream(destination);
			out = new BufferedOutputStream(fileStream);
			saveGame(fileStream, data);
		} finally
		{
			try
			{
				if (out != null)
					out.close();
			} catch (final Exception e)
			{
				e.printStackTrace();
			}
		}
	}
	
	public void saveGame(final OutputStream sink, final GameData data) throws IOException
	{
		saveGame(sink, data, true);
	}
	
	public void saveGame(final OutputStream sink, final GameData data, final boolean saveDelegateInfo) throws IOException
	{
		// write internally first in case of error
		final ByteArrayOutputStream bytes = new ByteArrayOutputStream(25000);
		final ObjectOutputStream outStream = new ObjectOutputStream(bytes);
		outStream.writeObject(games.strategy.engine.EngineVersion.VERSION);
		data.acquireReadLock();
		try
		{
			outStream.writeObject(data);
			if (saveDelegateInfo)
				writeDelegates(data, outStream);
			else
				outStream.writeObject(DELEGATE_LIST_END);
		} finally
		{
			data.releaseReadLock();
		}
		final GZIPOutputStream zippedOut = new GZIPOutputStream(sink);
		// now write to file
		zippedOut.write(bytes.toByteArray());
		zippedOut.flush();
		zippedOut.close();
	}
	
	private void writeDelegates(final GameData data, final ObjectOutputStream out) throws IOException
	{
		final Iterator<IDelegate> iter = data.getDelegateList().iterator();
		while (iter.hasNext())
		{
			out.writeObject(DELEGATE_START);
			final IDelegate delegate = iter.next();
			// write out the delegate info
			out.writeObject(delegate.getName());
			out.writeObject(delegate.getDisplayName());
			out.writeObject(delegate.getClass().getName());
			out.writeObject(DELEGATE_DATA_NEXT);
			out.writeObject(delegate.saveState());
		}
		// mark end of delegate section
		out.writeObject(DELEGATE_LIST_END);
	}
	
	/**
	 * Test if a game save works. Also a good way to dump a gamesave in memory to a hprof file, without all the background stuff.
	 */
	public static void main(final String[] args)
	{
		if (args == null || args.length == 0 || args[0] == null || args[0].length() == 0)
		{
			System.out.println("Usage: Please provide an argument that a valid path to a savegame file");
			return;
		}
		GameData data;
		{
			final File save = new File(args[0]);
			if (!save.exists() || save.isDirectory() || !save.canRead())
			{
				System.out.println("Usage: Please provide an argument that a valid path to a savegame file");
				return;
			}
			final GameDataManager manager = new GameDataManager();
			try
			{
				data = manager.loadGame(save);
			} catch (final IOException e)
			{
				e.printStackTrace();
				return;
			}
		}
		if (data == null)
		{
			System.out.println("GameData null, exiting.");
		}
		else
		{
			System.out.println("GameData loaded successfully for game: " + data.getGameName()
						+ " (round " + data.getSequence().getRound() + "). Press any key to exit.");
			try
			{
				System.in.read();
			} catch (final IOException e)
			{
				e.printStackTrace();
			}
		}
	}
}
