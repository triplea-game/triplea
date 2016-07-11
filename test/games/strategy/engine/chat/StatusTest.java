package games.strategy.engine.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.InetAddress;

import org.junit.jupiter.api.Test;

import games.strategy.net.INode;
import games.strategy.net.IServerMessenger;
import games.strategy.net.Messengers;
import games.strategy.net.Node;
import games.strategy.util.ThreadUtil;

public class StatusTest {

  @Test
  public void testStatus() throws Exception {
    IServerMessenger messenger = mock(IServerMessenger.class);
    INode dummyNode = new Node("dummy", InetAddress.getLocalHost(), 0);
    when(messenger.getLocalNode()).thenReturn(dummyNode);
    when(messenger.getServerNode()).thenReturn(dummyNode);
    when(messenger.isConnected()).thenReturn(true);
    when(messenger.isServer()).thenReturn(true);

    final Messengers messengers = new Messengers(messenger);
    final StatusManager manager = new StatusManager(messengers);
    assertNull(manager.getStatus(messenger.getLocalNode()));
    manager.setStatus("test");
    ThreadUtil.sleep(200);
    assertEquals("test", manager.getStatus(messenger.getLocalNode()));
    assertEquals("test", new StatusManager(messengers).getStatus(messenger.getLocalNode()));
  }
}
