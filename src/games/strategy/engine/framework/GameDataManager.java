package games.strategy.engine.framework;

import games.strategy.engine.EngineVersion;
import games.strategy.engine.data.GameData;
import games.strategy.engine.delegate.IDelegate;
import games.strategy.engine.lobby.client.ui.LobbyGamePanel;
import games.strategy.triplea.attatchments.TechAbilityAttachment;
import games.strategy.triplea.delegate.TechAdvance;
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
			if (!readVersion.equals(EngineVersion.VERSION))
			{
				/*if (GameRunner.areWeOldExtraJar())
				{
					throw new IOException("<html>Please run the default TripleA and try to open this game again. " +
								"<br>This TripleA engine is old and kept only for backwards compatibility and can only open savegames created by engines with these first 3 version digits: " +
								EngineVersion.VERSION.toStringFull("_", true) + "</html>");
				}*/
				final String error = "Incompatible engine versions, and no old engine found. We are: " + EngineVersion.VERSION + " . Trying to load game created with: " + readVersion;
				if (savegamePath == null)
					throw new IOException(error);
				
				// so, what we do here is try to see if our installed copy of triplea includes older jars with it that are the same engine as was used for this savegame, and if so try to run it
				try
				{
					final String newClassPath = LobbyGamePanel.findOldJar(readVersion, true);
					// ask user if we really want to do this?
					final String messageString = "<html>This TripleA engine is version "
								+ EngineVersion.VERSION.toString()
								+ " and you are trying to open a savegame made with version "
								+ readVersion.toString()
								+ "<br>However, this TripleA can not open any savegame made by any engine other than engines with the same first three version numbers as it (x_x_x_x)."
								+ "<br><br>TripleA now comes with older engines included with it, and has found the engine to run this savegame. This is a new feature and is in 'beta' stage."
								+ "<br>It will attempt to run a new instance of TripleA using the older engine jar file, and this instance will only be able to play this savegame."
								+ "<br>Your current instance will not be closed. Please report any bugs or issues."
								+ "<br><br>Do you wish to continue?</html>";
					final int answer = JOptionPane.showConfirmDialog(null, messageString, "Run old jar to open old Save Game?", JOptionPane.YES_NO_OPTION);
					if (answer != JOptionPane.YES_OPTION)
						return null;
					LobbyGamePanel.startGame(savegamePath, newClassPath);
				} catch (final IOException e)
				{
					throw new IOException(error);
				}
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
		}
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
}
