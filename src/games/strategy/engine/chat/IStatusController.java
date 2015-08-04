package games.strategy.engine.chat;

import java.util.Map;

import games.strategy.engine.message.IRemote;
import games.strategy.engine.message.RemoteName;
import games.strategy.net.INode;

public interface IStatusController extends IRemote {
  public static final RemoteName STATUS_CONTROLLER =
      new RemoteName("games.strategy.engine.chat.IStatusController.STATUS_CONTROLLER", IStatusController.class);

  /**
   * Set the status for our node.
   */
  public void setStatus(String newStatus);

  /**
   *
   * @return the status for all nodes.
   */
  public Map<INode, String> getAllStatus();
}
