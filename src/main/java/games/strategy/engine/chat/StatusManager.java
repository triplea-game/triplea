package games.strategy.engine.chat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import games.strategy.net.INode;
import games.strategy.net.Messengers;

public class StatusManager {
  private final List<IStatusListener> listeners = new CopyOnWriteArrayList<>();
  private final Map<INode, String> status = new HashMap<>();
  private final Messengers messengers;
  private final Object mutex = new Object();
  private final IStatusChannel statusChannelSubscribor;

  public StatusManager(final Messengers messengers) {
    this.messengers = messengers;
    statusChannelSubscribor = (node, status1) -> {
      synchronized (mutex) {
        if (status1 == null) {
          StatusManager.this.status.remove(node);
        } else {
          StatusManager.this.status.put(node, status1);
        }
      }
      notifyStatusChanged(node, status1);
    };
    if (messengers.getMessenger().isServer()
        && !messengers.getRemoteMessenger().hasLocalImplementor(IStatusController.STATUS_CONTROLLER)) {
      final StatusController controller = new StatusController(messengers);
      messengers.getRemoteMessenger().registerRemote(controller, IStatusController.STATUS_CONTROLLER);
    }
    this.messengers.getChannelMessenger().registerChannelSubscriber(statusChannelSubscribor,
        IStatusChannel.STATUS_CHANNEL);
    final IStatusController controller =
        (IStatusController) this.messengers.getRemoteMessenger().getRemote(IStatusController.STATUS_CONTROLLER);
    final Map<INode, String> values = controller.getAllStatus();
    synchronized (mutex) {
      status.putAll(values);
      // at this point we are just being constructed, so we have no
      // listeners
      // and we do not need to notify if anything has changed
    }
  }

  public void shutDown() {
    messengers.getChannelMessenger().unregisterChannelSubscriber(statusChannelSubscribor,
        IStatusChannel.STATUS_CHANNEL);
  }

  /**
   * Get the status for the given node.
   */
  public String getStatus(final INode node) {
    synchronized (mutex) {
      return status.get(node);
    }
  }

  public void setStatus(final String status) {
    final IStatusController controller =
        (IStatusController) messengers.getRemoteMessenger().getRemote(IStatusController.STATUS_CONTROLLER);
    controller.setStatus(status);
  }

  public void addStatusListener(final IStatusListener listener) {
    listeners.add(listener);
  }

  public void removeStatusListener(final IStatusListener listener) {
    listeners.remove(listener);
  }

  private void notifyStatusChanged(final INode node, final String newStatus) {
    for (final IStatusListener listener : listeners) {
      listener.statusChanged(node, newStatus);
    }
  }
}
