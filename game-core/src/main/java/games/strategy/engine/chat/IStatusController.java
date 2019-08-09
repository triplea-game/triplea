package games.strategy.engine.chat;

import games.strategy.engine.message.IRemote;
import games.strategy.engine.message.RemoteName;
import games.strategy.net.INode;
import java.util.Map;

/**
 * Manages the status of all chat participants ensuring changes are broadcast to all participants.
 */
public interface IStatusController extends IRemote {
  RemoteName STATUS_CONTROLLER =
      new RemoteName(
          "games.strategy.engine.chat.IStatusController.STATUS_CONTROLLER",
          IStatusController.class);

  /** Set the status for our node. */
  void setStatus(String newStatus);

  /** Returns the status for all nodes. */
  Map<INode, String> getAllStatus();
}
