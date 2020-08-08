package games.strategy.engine.history;

import games.strategy.engine.framework.IGame;
import games.strategy.engine.framework.IGameModifiedChannel;
import games.strategy.engine.message.IChannelMessenger;
import java.util.Optional;
import javax.annotation.Nullable;

/**
 * Has a subset of the historyWriters functionality. Delegates should only have access to these
 * functions. The rest of the history writers functions should only be used by the GameData.
 */
public class DelegateHistoryWriter implements IDelegateHistoryWriter {
  public static final DelegateHistoryWriter NO_OP_INSTANCE = new DelegateHistoryWriter();

  @Nullable private final IGameModifiedChannel channel;

  private DelegateHistoryWriter() {
    this((IGameModifiedChannel) null);
  }

  public DelegateHistoryWriter(final IChannelMessenger messenger) {
    this((IGameModifiedChannel) messenger.getChannelBroadcaster(IGame.GAME_MODIFICATION_CHANNEL));
  }

  public DelegateHistoryWriter(final IGameModifiedChannel channel) {
    this.channel = channel;
  }

  @Override
  public void startEvent(final String eventName, final Object renderingData) {
    Optional.ofNullable(channel)
        .ifPresent(channel -> channel.startHistoryEvent(eventName, renderingData));
  }

  @Override
  public void startEvent(final String eventName) {
    Optional.ofNullable(channel).ifPresent(channel -> channel.startHistoryEvent(eventName));
  }

  @Override
  public void addChildToEvent(final String child) {
    addChildToEvent(child, null);
  }

  @Override
  public void addChildToEvent(final String child, final Object renderingData) {
    Optional.ofNullable(channel)
        .ifPresent(channel -> channel.addChildToEvent(child, renderingData));
  }
}
