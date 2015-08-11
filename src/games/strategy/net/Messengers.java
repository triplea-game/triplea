package games.strategy.net;

import games.strategy.engine.message.ChannelMessenger;
import games.strategy.engine.message.IChannelMessenger;
import games.strategy.engine.message.IRemoteMessenger;
import games.strategy.engine.message.RemoteMessenger;
import games.strategy.engine.message.UnifiedMessenger;

/**
 * Convenience grouping of a messenger, remote messenger and channel messenger.
 */
public class Messengers {
  private final IMessenger m_messenger;
  private final IRemoteMessenger m_remoteMessenger;
  private final IChannelMessenger m_channelMessenger;

  public Messengers(final IMessenger messenger) {
    m_messenger = messenger;
    final UnifiedMessenger unifiedMessenger = new UnifiedMessenger(messenger);
    m_channelMessenger = new ChannelMessenger(unifiedMessenger);
    m_remoteMessenger = new RemoteMessenger(unifiedMessenger);
  }

  public Messengers(final IMessenger messenger, final IRemoteMessenger remoteMessenger,
      final IChannelMessenger channelMessenger) {
    m_messenger = messenger;
    m_remoteMessenger = remoteMessenger;
    m_channelMessenger = channelMessenger;
  }

  public IChannelMessenger getChannelMessenger() {
    return m_channelMessenger;
  }

  public IMessenger getMessenger() {
    return m_messenger;
  }

  public IRemoteMessenger getRemoteMessenger() {
    return m_remoteMessenger;
  }

  @Override
  public String toString() {
    return m_messenger + "\n" + m_remoteMessenger + "\n" + m_channelMessenger;
  }
}
