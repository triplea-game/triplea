package games.strategy.engine.player;

import com.google.common.base.Preconditions;
import games.strategy.engine.GameOverException;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameDataEvent;
import games.strategy.engine.delegate.IDelegate;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.delegate.IPersistentDelegate;
import games.strategy.engine.framework.IGame;
import games.strategy.engine.framework.ServerGame;
import games.strategy.engine.message.MessengerException;
import games.strategy.engine.message.RemoteName;
import java.util.Optional;
import javax.annotation.Nullable;
import lombok.Getter;
import org.triplea.http.client.web.socket.messages.MessageType;
import org.triplea.http.client.web.socket.messages.WebSocketMessage;

/**
 * Communication with the GamePlayer goes through the PlayerBridge to make the game network
 * transparent.
 */
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
   * Resolves the current delegate to its remote endpoint name so a typed delegate message addresses
   * the delegate endpoint registered by the server.
   */
  public RemoteName getCurrentDelegateRemoteName() {
    if (game.isGameOver()) {
      throw new GameOverException("Game Over");
    }
    try (GameData.Unlocker ignored = game.getData().acquireReadLock()) {
      final Optional<IDelegate> optionalDelegate =
          game.getData().getDelegateOptional(currentDelegate);
      Preconditions.checkState(
          optionalDelegate.isPresent(),
          "IDelegate in PlayerBridge cannot be null. CurrentStep: "
              + stepName
              + ", and CurrentDelegate: "
              + currentDelegate);
      return ServerGame.getRemoteName(optionalDelegate.get());
    }
  }

  /**
   * Returns the live bridge of the current step's delegate. Valid only for a caller that shares the
   * delegate's VM (the AI runs server-co-located); it yields the same {@link IDelegateBridge} the
   * reflective current-delegate proxy's {@code getBridge()} returned, without a network round-trip.
   */
  public IDelegateBridge getCurrentDelegateBridge() {
    if (game.isGameOver()) {
      throw new GameOverException("Game Over");
    }
    try (GameData.Unlocker ignored = game.getData().acquireReadLock()) {
      final Optional<IDelegate> optionalDelegate =
          game.getData().getDelegateOptional(currentDelegate);
      Preconditions.checkState(
          optionalDelegate.isPresent(),
          "IDelegate in PlayerBridge cannot be null. CurrentStep: "
              + stepName
              + ", and CurrentDelegate: "
              + currentDelegate);
      return optionalDelegate.get().getBridge();
    }
  }

  /**
   * Sends a typed request to the current delegate and blocks for its typed reply, converting a lost
   * connection to a game-over. Void business methods use a typed acknowledgement response so the
   * caller still blocks until the delegate has applied the change.
   */
  public <R extends WebSocketMessage> R invokeCurrentDelegate(
      final WebSocketMessage request, final MessageType<R> responseType) {
    try {
      return game.getMessengers()
          .invokeRemoteMessage(getCurrentDelegateRemoteName(), request, responseType);
    } catch (final RuntimeException e) {
      throw convertToGameOverIfNeeded(e);
    }
  }

  /**
   * Sends a typed request to a named persistent delegate and blocks for its typed reply. Returns
   * {@code null} when the named delegate is not a persistent delegate.
   */
  @Nullable
  public <R extends WebSocketMessage> R invokePersistentDelegate(
      final String name, final WebSocketMessage request, final MessageType<R> responseType) {
    final RemoteName remoteName;
    if (game.isGameOver()) {
      throw new GameOverException("Game Over");
    }
    try (GameData.Unlocker ignored = game.getData().acquireReadLock()) {
      final IDelegate delegate = game.getData().getDelegate(name);
      if (!(delegate instanceof IPersistentDelegate)) {
        return null;
      }
      remoteName = ServerGame.getRemoteName(delegate);
    }
    try {
      return game.getMessengers().invokeRemoteMessage(remoteName, request, responseType);
    } catch (final RuntimeException e) {
      throw convertToGameOverIfNeeded(e);
    }
  }

  private RuntimeException convertToGameOverIfNeeded(final RuntimeException e) {
    if (e.getCause() instanceof MessengerException) {
      return new GameOverException("Game Over!");
    }
    return e;
  }
}
