package games.strategy.debug;

import games.strategy.engine.framework.headlessGameServer.HeadlessGameServer;
import games.strategy.engine.message.MessageContext;
import games.strategy.engine.message.RemoteName;
import games.strategy.net.INode;

public class HeartBeat implements IHeartBeat {
  private final INode m_serverNode;

  public static RemoteName getHeartBeatName(final INode node) {
    return new RemoteName(IHeartBeat.class, "games.strategy.debug.HearBeat:" + node.toString());
  }

  public HeartBeat(final INode serverNode) {
    m_serverNode = serverNode;
  }

  @Override
  public String getDebugInfo() {
    if (MessageContext.getSender().equals(m_serverNode)) {
      if (HeadlessGameServer.headless()) {
        return DebugUtils.getDebugReportHeadless();
      } else {
        return DebugUtils.getDebugReportWithFramesAndWindows();
      }
    } else {
      return "";
    }
  }
}
