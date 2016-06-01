package games.strategy.engine.gamePlayer;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Properties;

import games.strategy.debug.ClientLogger;
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

/**
 * Default implementation of PlayerBridge.
 */
public class DefaultPlayerBridge implements IPlayerBridge {
  private final IGame m_game;
  private String m_currentStep;
  private String m_currentDelegate;

  /** Creates new DefaultPlayerBridge */
  public DefaultPlayerBridge(final IGame aGame) {
    m_game = aGame;
    m_game.addGameStepListener(m_gameStepListener);
  }

  /**
   * Get the name of the current step being executed.
   */
  @Override
  public String getStepName() {
    return m_currentStep;
  }

  @Override
  public boolean isGameOver() {
    return m_game.isGameOver();
  }

  /**
   * Return the game data
   */
  @Override
  public GameData getGameData() {
    return m_game.getData();
  }

  private final GameStepListener m_gameStepListener = new GameStepListener() {
    @Override
    public void gameStepChanged(final String stepName, final String delegateName, final PlayerID player,
        final int round, final String displayName) {
      if (stepName == null) {
        throw new IllegalArgumentException("Null step");
      }
      if (delegateName == null) {
        throw new IllegalArgumentException("Null delegate");
      }
      m_currentStep = stepName;
      m_currentDelegate = delegateName;
    }
  };

  @Override
  public IRemote getRemoteDelegate() {
    if (m_game.isGameOver()) {
      throw new GameOverException("Game Over");
    }
    try {
      m_game.getData().acquireReadLock();
      try {
        final IDelegate delegate = m_game.getData().getDelegateList().getDelegate(m_currentDelegate);
        if (delegate == null) {
          final String errorMessage = "IDelegate in DefaultPlayerBridge.getRemote() cannot be null. CurrentStep: "
              + m_currentStep + ", and CurrentDelegate: " + m_currentDelegate;
          // for some reason, client isn't getting or seeing the errors, so make sure we print it to err
          // too
          System.err.println(errorMessage);
          // Veqryn: hope that this suffices...?
          throw new IllegalStateException(errorMessage);
        }
        final RemoteName remoteName;
        try {
          remoteName = ServerGame.getRemoteName(delegate);
        } catch (final Exception e) {
          ClientLogger.logQuietly(e);
          final String errorMessage =
              "IDelegate IRemote interface class returned null or was not correct interface. CurrentStep: "
                  + m_currentStep + ", and CurrentDelegate: " + m_currentDelegate;
          // for some reason, client isn't getting or seeing the errors, so make sure we print it to err
          // too
          System.err.println(errorMessage);
          throw new IllegalStateException(errorMessage, e);
        }
        return getRemoteThatChecksForGameOver(m_game.getRemoteMessenger().getRemote(remoteName));
      } finally {
        m_game.getData().releaseReadLock();
      }
    } catch (final MessengerException me) {
      throw new GameOverException("Game Over!");
    }
  }

  @Override
  public IRemote getRemotePersistentDelegate(final String name) {
    if (m_game.isGameOver()) {
      throw new GameOverException("Game Over");
    }
    try {
      m_game.getData().acquireReadLock();
      try {
        final IDelegate delegate = m_game.getData().getDelegateList().getDelegate(name);
        if (delegate == null) {
          final String errorMessage =
              "IDelegate in DefaultPlayerBridge.getRemote() cannot be null. Looking for delegate named: " + name;
          // for some reason, client isn't getting or seeing the errors, so make sure we print it to err
          System.err.println(errorMessage);
          // too
          throw new IllegalStateException(errorMessage);
        }
        if (!(delegate instanceof IPersistentDelegate)) {
          return null;
        }
        return getRemoteThatChecksForGameOver(
            m_game.getRemoteMessenger().getRemote(ServerGame.getRemoteName(delegate)));
      } finally {
        m_game.getData().releaseReadLock();
      }
    } catch (final MessengerException me) {
      throw new GameOverException("Game Over!");
    }
  }

  @Override
  public Properties getStepProperties() {
    return m_game.getData().getSequence().getStep().getProperties();
  }

  private IRemote getRemoteThatChecksForGameOver(final IRemote implementor) {
    final Class<?>[] classes = implementor.getClass().getInterfaces();
    final GameOverInvocationHandler goih = new GameOverInvocationHandler(implementor, m_game);
    return (IRemote) Proxy.newProxyInstance(implementor.getClass().getClassLoader(), classes, goih);
  }
}


class GameOverInvocationHandler implements InvocationHandler {
  private final Object m_delegate;
  private final IGame m_game;

  public GameOverInvocationHandler(final Object delegate, final IGame game) {
    m_delegate = delegate;
    m_game = game;
  }

  @Override
  public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
    try {
      return method.invoke(m_delegate, args);
    } catch (final InvocationTargetException ite) {
      if (!m_game.isGameOver()) {
        throw ite.getCause();
      } else {
        throw new GameOverException("Game Over Exception!");
      }
    } catch (final RemoteNotFoundException rnfe) {
      throw new GameOverException("Game Over!");
    }
  }
}
