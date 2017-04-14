package games.strategy.engine.chat;

import java.util.Map;

import games.strategy.engine.message.IRemote;
import games.strategy.engine.message.RemoteName;
import games.strategy.net.INode;

public interface IStatusController extends IRemote {
  RemoteName STATUS_CONTROLLER =
      new RemoteName("games.strategy.engine.chat.IStatusController.STATUS_CONTROLLER", IStatusController.class);

  /**
   * Set the status for our node.
   */
  void setStatus(String newStatus);

  /**
   * @return The status for all nodes.
   */
  Map<INode, String> getAllStatus();
}
