package games.strategy.engine.history;

import games.strategy.engine.data.GameData;
import games.strategy.engine.framework.IGame;
import games.strategy.engine.framework.IGameModifiedChannel;
import games.strategy.engine.message.IChannelMessenger;
import games.strategy.triplea.delegate.BaseEditDelegate;
import java.util.Optional;
import javax.annotation.Nullable;

/**
 * Has a subset of the historyWriters functionality. Delegates should only have access to these
 * functions. The rest of the history writers functions should only be used by the GameData.
 */
public class DelegateHistoryWriter implements IDelegateHistoryWriter {

  private static final String COMMENT_PREFIX = "COMMENT: ";
  @Nullable private final IGameModifiedChannel channel;
  private final GameData gameData;

  public DelegateHistoryWriter(final IChannelMessenger messenger, final GameData gameData) {
    this(
        (IGameModifiedChannel) messenger.getChannelBroadcaster(IGame.GAME_MODIFICATION_CHANNEL),
        gameData);
  }

  public DelegateHistoryWriter(
      @Nullable final IGameModifiedChannel channel, final GameData gameData) {
    this.channel = channel;
    this.gameData = gameData;
  }

  private String getEventPrefix() {
    if (BaseEditDelegate.getEditMode(gameData.getProperties())) {
      return "EDIT: ";
    }
    return "";
  }

  private String addPrefixOnEditMode(String eventName) {
    if (eventName.startsWith(COMMENT_PREFIX)) {
      return eventName;
    }
    return getEventPrefix() + eventName;
  }

  @Override
  public void startEvent(final String eventName, final Object renderingData) {
    Optional.ofNullable(channel)
        .ifPresent(
            channel -> channel.startHistoryEvent(addPrefixOnEditMode(eventName), renderingData));
  }

  @Override
  public void startEvent(final String eventName) {
    Optional.ofNullable(channel)
        .ifPresent(channel -> channel.startHistoryEvent(addPrefixOnEditMode(eventName)));
  }

  @Override
  public void addChildToEvent(final String child) {
    addChildToEvent(child, null);
  }

  @Override
  public void addChildToEvent(final String child, final Object renderingData) {
    Optional.ofNullable(channel)
        .ifPresent(channel -> channel.addChildToEvent(addPrefixOnEditMode(child), renderingData));
  }
}
