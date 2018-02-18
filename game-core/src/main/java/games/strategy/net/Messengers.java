package games.strategy.net;

import games.strategy.engine.message.ChannelMessenger;
import games.strategy.engine.message.IChannelMessenger;
import games.strategy.engine.message.IRemoteMessenger;
import games.strategy.engine.message.RemoteMessenger;
import games.strategy.engine.message.unifiedmessenger.UnifiedMessenger;

/**
 * Convenience grouping of a messenger, remote messenger and channel messenger.
 */
public class Messengers {
  private final IMessenger messenger;
  private final IRemoteMessenger remoteMessenger;
  private final IChannelMessenger channelMessenger;

  public Messengers(final IMessenger messenger) {
    this.messenger = messenger;
    final UnifiedMessenger unifiedMessenger = new UnifiedMessenger(messenger);
    channelMessenger = new ChannelMessenger(unifiedMessenger);
    remoteMessenger = new RemoteMessenger(unifiedMessenger);
  }

  public Messengers(final IMessenger messenger, final IRemoteMessenger remoteMessenger,
      final IChannelMessenger channelMessenger) {
    this.messenger = messenger;
    this.remoteMessenger = remoteMessenger;
    this.channelMessenger = channelMessenger;
  }

  public IChannelMessenger getChannelMessenger() {
    return channelMessenger;
  }

  public IMessenger getMessenger() {
    return messenger;
  }

  public IRemoteMessenger getRemoteMessenger() {
    return remoteMessenger;
  }

  @Override
  public String toString() {
    return messenger + "\n" + remoteMessenger + "\n" + channelMessenger;
  }
}
