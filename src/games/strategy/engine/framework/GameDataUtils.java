package games.strategy.engine.framework;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

public class GameDataUtils
{
	public static GameData cloneGameData(final GameData data)
	{
		return cloneGameData(data, false);
	}
	
	/**
	 * Create a deep copy of GameData.
	 * 
	 * <Strong>You should have the game datas read or write lock before calling this method</STRONG>
	 */
	public static GameData cloneGameData(final GameData data, final boolean copyDelegates)
	{
		try
		{
			final GameDataManager manager = new GameDataManager();
			ByteArrayOutputStream sink = new ByteArrayOutputStream(10000);
			manager.saveGame(sink, data, copyDelegates);
			sink.close();
			final ByteArrayInputStream source = new ByteArrayInputStream(sink.toByteArray());
			sink = null;
			return manager.loadGame(source);
		} catch (final IOException ex)
		{
			ex.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Translate units,territories and other game data objects from one
	 * game data into another.
	 */
	public static Object translateIntoOtherGameData(final Object object, final GameData translateInto)
	{
		try
		{
			ByteArrayOutputStream sink = new ByteArrayOutputStream(1024);
			final GameObjectOutputStream out = new GameObjectOutputStream(sink);
			out.writeObject(object);
			out.flush();
			out.close();
			final ByteArrayInputStream source = new ByteArrayInputStream(sink.toByteArray());
			sink = null;
			final GameObjectStreamFactory factory = new GameObjectStreamFactory(translateInto);
			final ObjectInputStream in = factory.create(source);
			try
			{
				return in.readObject();
			} catch (final ClassNotFoundException ex)
			{
				// should never happen
				throw new RuntimeException(ex);
			}
		} catch (final IOException ioe)
		{
			throw new RuntimeException(ioe);
		}
	}
}
