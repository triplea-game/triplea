package games.strategy.engine.gamePlayer;

import static com.google.common.base.Preconditions.checkNotNull;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.logging.Level;

import com.google.common.base.Preconditions;

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
import lombok.Getter;
import lombok.extern.java.Log;

/**
 * Default implementation of PlayerBridge.
 */
@Log
public class DefaultPlayerBridge implements IPlayerBridge {
  private final IGame game;
  @Getter(onMethod_ = {@Override})
  private String stepName;
  private String currentDelegate;

  /** Creates new DefaultPlayerBridge. */
  public DefaultPlayerBridge(final IGame game) {
    this.game = game;
    final GameStepListener gameStepListener = (stepName, delegateName, player, round, displayName) -> {
      checkNotNull(stepName);
      checkNotNull(delegateName);

      this.stepName = stepName;
      currentDelegate = delegateName;
    };
    this.game.addGameStepListener(gameStepListener);
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
        // TODO: before converting this Precondtions check to checkNotNull, make sure we do not depend on the
        // illegal state exception type in a catch block.
        Preconditions.checkState(
            delegate != null,
            "IDelegate in DefaultPlayerBridge.getRemote() cannot be null. CurrentStep: "
                + stepName + ", and CurrentDelegate: " + currentDelegate);
        final RemoteName remoteName;
        try {
          remoteName = ServerGame.getRemoteName(delegate);
        } catch (final Exception e) {
          final String errorMessage =
              "IDelegate IRemote interface class returned null or was not correct interface. CurrentStep: "
                  + stepName + ", and CurrentDelegate: " + currentDelegate;
          log.log(Level.SEVERE, errorMessage, e);
          throw new IllegalStateException(errorMessage, e);
        }
        return getRemoteThatChecksForGameOver(game.getRemoteMessenger().getRemote(remoteName));
      } finally {
        game.getData().releaseReadLock();
      }
    } catch (final MessengerException me) {
      // TODO: this kind of conversion does not seem appropriate. Maybe MessengerException should extend
      // GameOverException? the root cause of the MessenderException is being lost, that is something to fix
      // as well as a potential control-flow-by-exception-handling code smell.
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
