package games.strategy.engine.chat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import games.strategy.engine.message.DummyMessenger;
import games.strategy.net.Messengers;
import games.strategy.util.ThreadUtil;

public class StatusTest {

  @Test
  public void testStatus() throws Exception {
    final DummyMessenger messenger = new DummyMessenger();
    final Messengers messengers = new Messengers(messenger);
    final StatusManager manager = new StatusManager(messengers);
    assertNull(manager.getStatus(messenger.getLocalNode()));
    manager.setStatus("test");
    ThreadUtil.sleep(200);
    assertEquals("test", manager.getStatus(messenger.getLocalNode()));
    assertEquals("test", new StatusManager(messengers).getStatus(messenger.getLocalNode()));
  }
}
