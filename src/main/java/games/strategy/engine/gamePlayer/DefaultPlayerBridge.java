package games.strategy.engine.gamePlayer;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Properties;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.GameOverException;
import games.strategy.engine.data.GameData;
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
  private final IGame game;
  private String currentStep;
  private String currentDelegate;

  /** Creates new DefaultPlayerBridge. */
  public DefaultPlayerBridge(final IGame game) {
    this.game = game;
    final GameStepListener gameStepListener = (stepName, delegateName, player, round, displayName) -> {
      if (stepName == null) {
        throw new IllegalArgumentException("Null step");
      }
      if (delegateName == null) {
        throw new IllegalArgumentException("Null delegate");
      }
      currentStep = stepName;
      currentDelegate = delegateName;
    };
    this.game.addGameStepListener(gameStepListener);
  }

  @Override
  public String getStepName() {
    return currentStep;
  }

  @Override
  public boolean isGameOver() {
    return game.isGameOver();
  }

  @Override
  public GameData getGameData() {
    return game.getData();
  }

  @Override
  public IRemote getRemoteDelegate() {
    if (game.isGameOver()) {
      throw new GameOverException("Game Over");
    }
    try {
      game.getData().acquireReadLock();
      try {
        final IDelegate delegate = game.getData().getDelegateList().getDelegate(currentDelegate);
        if (delegate == null) {
          final String errorMessage = "IDelegate in DefaultPlayerBridge.getRemote() cannot be null. CurrentStep: "
              + currentStep + ", and CurrentDelegate: " + currentDelegate;
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
          final String errorMessage =
              "IDelegate IRemote interface class returned null or was not correct interface. CurrentStep: "
                  + currentStep + ", and CurrentDelegate: " + currentDelegate;
          // for some reason, client isn't getting or seeing the errors, so make sure we print it to err
          // too
          System.err.println(errorMessage);
          ClientLogger.logQuietly(errorMessage, e);
          throw new IllegalStateException(errorMessage, e);
        }
        return getRemoteThatChecksForGameOver(game.getRemoteMessenger().getRemote(remoteName));
      } finally {
        game.getData().releaseReadLock();
      }
    } catch (final MessengerException me) {
      throw new GameOverException("Game Over!");
    }
  }

  @Override
  public IRemote getRemotePersistentDelegate(final String name) {
    if (game.isGameOver()) {
      throw new GameOverException("Game Over");
    }
    try {
      game.getData().acquireReadLock();
      try {
        final IDelegate delegate = game.getData().getDelegateList().getDelegate(name);
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
            game.getRemoteMessenger().getRemote(ServerGame.getRemoteName(delegate)));
      } finally {
        game.getData().releaseReadLock();
      }
    } catch (final MessengerException me) {
      throw new GameOverException("Game Over!");
    }
  }

  @Override
  public Properties getStepProperties() {
    return game.getData().getSequence().getStep().getProperties();
  }

  private IRemote getRemoteThatChecksForGameOver(final IRemote implementor) {
    final Class<?>[] classes = implementor.getClass().getInterfaces();
    final GameOverInvocationHandler goih = new GameOverInvocationHandler(implementor, game);
    return (IRemote) Proxy.newProxyInstance(implementor.getClass().getClassLoader(), classes, goih);
  }

  static class GameOverInvocationHandler implements InvocationHandler {
    private final Object delegate;
    private final IGame game;

    public GameOverInvocationHandler(final Object delegate, final IGame game) {
      this.delegate = delegate;
      this.game = game;
    }

    @Override
    public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
      try {
        return method.invoke(delegate, args);
      } catch (final InvocationTargetException ite) {
        if (!game.isGameOver()) {
          throw ite.getCause();
        }
        throw new GameOverException("Game Over Exception!");
      } catch (final RemoteNotFoundException rnfe) {
        throw new GameOverException("Game Over!");
      }
    }
  }

}
