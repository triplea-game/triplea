package games.strategy.engine.player;

import com.google.common.base.Preconditions;
import games.strategy.engine.GameOverException;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameDataEvent;
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
import java.util.Optional;
import javax.annotation.Nullable;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Communication with the GamePlayer goes through the PlayerBridge to make the game network
 * transparent.
 */
@Slf4j
public class PlayerBridge {
  private final IGame game;

  /** The name of the current step being executed. */
  @Getter private String stepName;

  private String currentDelegate;

  /** Creates new PlayerBridge. */
  public PlayerBridge(final IGame game) {
    this.game = game;
    game.getData()
        .addGameDataEventListener(
            GameDataEvent.GAME_STEP_CHANGED,
            () -> {
              this.stepName = game.getData().getSequence().getStep().getName();
              this.currentDelegate = game.getData().getSequence().getStep().getDelegate().getName();
            });
  }

  /** Indicates the game is over. */
  public boolean isGameOver() {
    return game.isGameOver();
  }

  /** Return the game data. */
  public GameData getGameData() {
    return game.getData();
  }

  /**
   * Get a remote reference to the current delegate, the type of the reference is declared by the
   * delegates getRemoteType() method.
   */
  public IRemote getRemoteDelegate() {
    if (game.isGameOver()) {
      throw new GameOverException("Game Over");
    }
    try {
      try (GameData.Unlocker ignored = game.getData().acquireReadLock()) {
        final Optional<IDelegate> optionalDelegate =
            game.getData().getDelegateOptional(currentDelegate);
        // TODO: before converting this Preconditions check to checkNotNull, make sure we do not
        // depend on the illegal state exception type in a catch block.
        Preconditions.checkState(
            optionalDelegate.isPresent(),
            "IDelegate in PlayerBridge.getRemote() cannot be null. CurrentStep: "
                + stepName
                + ", and CurrentDelegate: "
                + currentDelegate);
        final RemoteName remoteName;
        try {
          remoteName = ServerGame.getRemoteName(optionalDelegate.get());
        } catch (final Exception e) {
          final String errorMessage =
              "IDelegate IRemote interface class returned null or was not correct "
                  + "interface. CurrentStep: "
                  + stepName
                  + ", and CurrentDelegate: "
                  + currentDelegate;
          log.error(errorMessage, e);
          throw new IllegalStateException(errorMessage, e);
        }
        return getRemoteThatChecksForGameOver(game.getMessengers().getRemote(remoteName));
      }
    } catch (final RuntimeException e) {
      if (e.getCause() instanceof MessengerException) {
        // TODO: this kind of conversion does not seem appropriate. Maybe MessengerException should
        // extend GameOverException? the root cause
        // of the MessengerException is being lost, that is something to fix
        // as well as a potential control-flow-by-exception-handling code smell.
        throw new GameOverException("Game Over!");
      }
      throw e;
    }
  }

  /**
   * Get a remote reference to the named delegate, the type of the reference is declared by the
   * delegates getRemoteType() method.
   */
  @Nullable
  public IRemote getRemotePersistentDelegate(final String name) {
    if (game.isGameOver()) {
      throw new GameOverException("Game Over");
    }
    try {
      try (GameData.Unlocker ignored = game.getData().acquireReadLock()) {
        final IDelegate delegate = game.getData().getDelegate(name);
        if (!(delegate instanceof IPersistentDelegate)) {
          return null;
        }
        return getRemoteThatChecksForGameOver(
            game.getMessengers().getRemote(ServerGame.getRemoteName(delegate)));
      }
    } catch (final RuntimeException e) {
      if (e.getCause() instanceof MessengerException) {
        throw new GameOverException("Game Over!", e);
      }
      throw e;
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

    GameOverInvocationHandler(final Object delegate, final IGame game) {
      this.delegate = delegate;
      this.game = game;
    }

    @Override
    public Object invoke(final Object proxy, final Method method, final Object[] args)
        throws Throwable {
      try {
        return method.invoke(delegate, args);
      } catch (final InvocationTargetException e) {
        if (e.getCause() instanceof RemoteNotFoundException) {
          throw new GameOverException("Game Over!");
        }
        if (!game.isGameOver()) {
          throw e.getCause();
        }
        throw new GameOverException("Game Over Exception!");
      }
    }
  }
}
