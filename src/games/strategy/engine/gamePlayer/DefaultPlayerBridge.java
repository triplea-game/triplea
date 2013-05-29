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
 * DefaultPlayerBridge.java
 * 
 * Created on October 27, 2001, 8:55 PM
 */
package games.strategy.engine.gamePlayer;

import games.strategy.engine.GameOverException;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.events.GameStepListener;
import games.strategy.engine.delegate.IDelegate;
import games.strategy.engine.delegate.IPersistentDelegate;
import games.strategy.engine.framework.IGame;
import games.strategy.engine.framework.ServerGame;
import games.strategy.engine.message.IRemote;
import games.strategy.engine.message.MessengerException;
import games.strategy.engine.message.RemoteName;
import games.strategy.engine.message.RemoteNotFoundException;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Properties;

/**
 * Default implementation of PlayerBridge.
 * 
 * @author Sean Bridges
 * 
 */
public class DefaultPlayerBridge implements IPlayerBridge
{
	private final IGame m_game;
	private String m_currentStep;
	private String m_currentDelegate;
	
	/** Creates new DefaultPlayerBridge */
	public DefaultPlayerBridge(final IGame aGame)
	{
		m_game = aGame;
		m_game.addGameStepListener(m_gameStepListener);
	}
	
	/**
	 * Get the name of the current step being executed.
	 */
	public String getStepName()
	{
		return m_currentStep;
	}
	
	/* TODO: add this into next release, along with interface method.
	 * Get the name of the current delegate being executed.
	public String getDelegateName()
	{
		return m_currentDelegate;
	}*/

	/* TODO: add this into next release
	public void printErrorStatus()
	{
		if (m_game == null)
		{
			System.err.println("WTF?? IGame == null");
		}
		else
		{
			String error = "IGame Status: Player Manager: " + m_game.getPlayerManager() + ", GameOver: " + m_game.isGameOver();
			final IMessenger messenger = m_game.getMessenger();
			if (messenger == null)
			{
				error += ", IMessenger == null (WTF?)";
			}
			else
			{
				error += ", Connected: " + messenger.isConnected() + ", Is Server: " + messenger.isServer() + ", Local Node: "
							+ messenger.getLocalNode() + ", Server Node: " + messenger.getServerNode() + ", Messenger ShutDown: " + messenger.isShutDown();
				if (m_game.getMessenger() instanceof IServerMessenger)
					error += ((IServerMessenger) m_game.getMessenger()).getNodes();
			}
			System.err.println(error);
		}
	}*/

	public boolean isGameOver()
	{
		return m_game.isGameOver();
	}
	
	/**
	 * Return the game data
	 */
	public GameData getGameData()
	{
		return m_game.getData();
	}
	
	private final GameStepListener m_gameStepListener = new GameStepListener()
	{
		public void gameStepChanged(final String stepName, final String delegateName, final PlayerID player, final int round, final String displayName)
		{
			if (stepName == null)
				throw new IllegalArgumentException("Null step");
			if (delegateName == null)
				throw new IllegalArgumentException("Null delegate");
			m_currentStep = stepName;
			m_currentDelegate = delegateName;
		}
	};
	
	/* 
	 * @see games.strategy.engine.gamePlayer.PlayerBridge#getRemote()
	 */
	public IRemote getRemote()
	{
		if (m_game.isGameOver())
			throw new GameOverException("Game Over");
		try
		{
			m_game.getData().acquireReadLock();
			try
			{
				final IDelegate delegate = m_game.getData().getDelegateList().getDelegate(m_currentDelegate);
				if (delegate == null)
				{
					final String errorMessage = "IDelegate in DefaultPlayerBridge.getRemote() can not be null. CurrentStep: " + m_currentStep + ", and CurrentDelegate: " + m_currentDelegate;
					System.err.println(errorMessage);
					throw new GameOverException(errorMessage); // Veqryn: hope that this suffices...?
				}
				final RemoteName remoteName;
				try
				{
					remoteName = ServerGame.getRemoteName(delegate);
				} catch (final Exception e)
				{
					e.printStackTrace();
					// TODO: Veqryn: We are getting a IllegalArgumentException (formerly a NullPointerException) here occasionally for hosts, because the 'class' variable is null.
					// This should be impossible, and indeed all the classes it has occurred for have a non-null IRemote delegate interface class for their getRemote().
					// On top of this, it is also occassionally occurring for HeadlessGameServer hostbots, which are not playing any players (therefore we should never be in TripleAPlayer -> start() -> deleget.getRemote())
					// My only guess is that someone disconnects at some very sensitive point, and then the classes are destroyed or set to null or something, resulting in a null for the interface class.
					// I am hoping that we are in the middle of getting a connection lost error, and therefore we can just print the stack trace and ignore, letting the connection lost error do the work.
					final String errorMessage = "IDelegate IRemote interface class returned null or was not correct interface. CurrentStep: " + m_currentStep + ", and CurrentDelegate: "
								+ m_currentDelegate;
					throw new GameOverException(errorMessage);
				}
				return getRemoteThatChecksForGameOver(m_game.getRemoteMessenger().getRemote(remoteName));
			} finally
			{
				m_game.getData().releaseReadLock();
			}
		} catch (final MessengerException me)
		{
			throw new GameOverException("Game Over!");
		}
	}
	
	public IRemote getRemote(final String name)
	{
		if (m_game.isGameOver())
			throw new GameOverException("Game Over");
		try
		{
			m_game.getData().acquireReadLock();
			try
			{
				final IDelegate delegate = m_game.getData().getDelegateList().getDelegate(name);
				if (!(delegate instanceof IPersistentDelegate))
					return null;
				return getRemoteThatChecksForGameOver(m_game.getRemoteMessenger().getRemote(ServerGame.getRemoteName(delegate)));
			} finally
			{
				m_game.getData().releaseReadLock();
			}
		} catch (final MessengerException me)
		{
			throw new GameOverException("Game Over!");
		}
	}
	
	public Properties getStepProperties()
	{
		return m_game.getData().getSequence().getStep().getProperties();
	}
	
	private IRemote getRemoteThatChecksForGameOver(final IRemote implementor)
	{
		final Class<?>[] classes = implementor.getClass().getInterfaces();
		final GameOverInvocationHandler goih = new GameOverInvocationHandler(implementor, m_game);
		return (IRemote) Proxy.newProxyInstance(implementor.getClass().getClassLoader(), classes, goih);
	}
}


class GameOverInvocationHandler implements InvocationHandler
{
	private final Object m_delegate;
	private final IGame m_game;
	
	public GameOverInvocationHandler(final Object delegate, final IGame game)
	{
		m_delegate = delegate;
		m_game = game;
	}
	
	public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable
	{
		try
		{
			return method.invoke(m_delegate, args);
		} catch (final InvocationTargetException ite)
		{
			if (!m_game.isGameOver())
				throw ite.getCause();
			else
				throw new GameOverException("Game Over Exception!");
		} catch (final RemoteNotFoundException rnfe)
		{
			throw new GameOverException("Game Over!");
		}
	}
}
