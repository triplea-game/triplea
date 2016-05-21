package games.strategy.engine.chat;

import java.util.HashMap;
import java.util.Map;

import games.strategy.engine.message.MessageContext;
import games.strategy.net.IConnectionChangeListener;
import games.strategy.net.INode;
import games.strategy.net.IServerMessenger;
import games.strategy.net.Messengers;

public class StatusController implements IStatusController {
  private final Object m_mutex = new Object();
  private final Map<INode, String> m_status = new HashMap<>();
  private final Messengers m_messengers;

  public StatusController(final Messengers messengers) {
    m_messengers = messengers;
    ((IServerMessenger) m_messengers.getMessenger()).addConnectionChangeListener(new IConnectionChangeListener() {
      @Override
      public void connectionRemoved(final INode to) {
        StatusController.this.connectionRemoved(to);
      }

      @Override
      public void connectionAdded(final INode to) {}
    });
  }

  protected void connectionRemoved(final INode to) {
    synchronized (m_mutex) {
      m_status.remove(to);
    }
    final IStatusChannel channel =
        (IStatusChannel) m_messengers.getChannelMessenger().getChannelBroadcastor(IStatusChannel.STATUS_CHANNEL);
    channel.statusChanged(to, null);
  }

  @Override
  public Map<INode, String> getAllStatus() {
    synchronized (m_mutex) {
      return new HashMap<>(m_status);
    }
  }

  @Override
  public void setStatus(final String newStatus) {
    final INode node = MessageContext.getSender();
    synchronized (m_mutex) {
      m_status.put(node, newStatus);
    }
    final IStatusChannel channel =
        (IStatusChannel) m_messengers.getChannelMessenger().getChannelBroadcastor(IStatusChannel.STATUS_CHANNEL);
    channel.statusChanged(node, newStatus);
  }
}
