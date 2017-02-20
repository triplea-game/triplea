package games.strategy.engine.chat;

import java.util.HashMap;
import java.util.Map;

import games.strategy.engine.message.MessageContext;
import games.strategy.net.IConnectionChangeListener;
import games.strategy.net.INode;
import games.strategy.net.IServerMessenger;
import games.strategy.net.Messengers;

public class StatusController implements IStatusController {
  private final Object mutex = new Object();
  private final Map<INode, String> status = new HashMap<>();
  private final Messengers messengers;

  public StatusController(final Messengers messengers) {
    this.messengers = messengers;
    ((IServerMessenger) this.messengers.getMessenger()).addConnectionChangeListener(new IConnectionChangeListener() {
      @Override
      public void connectionRemoved(final INode to) {
        StatusController.this.connectionRemoved(to);
      }

      @Override
      public void connectionAdded(final INode to) {}
    });
  }

  protected void connectionRemoved(final INode to) {
    synchronized (mutex) {
      status.remove(to);
    }
    final IStatusChannel channel =
        (IStatusChannel) messengers.getChannelMessenger().getChannelBroadcastor(IStatusChannel.STATUS_CHANNEL);
    channel.statusChanged(to, null);
  }

  @Override
  public Map<INode, String> getAllStatus() {
    synchronized (mutex) {
      return new HashMap<>(status);
    }
  }

  @Override
  public void setStatus(final String newStatus) {
    final INode node = MessageContext.getSender();
    synchronized (mutex) {
      status.put(node, newStatus);
    }
    final IStatusChannel channel =
        (IStatusChannel) messengers.getChannelMessenger().getChannelBroadcastor(IStatusChannel.STATUS_CHANNEL);
    channel.statusChanged(node, newStatus);
  }
}
