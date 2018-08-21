package games.strategy.net;

import java.io.IOException;

/**
 * Implementation of {@link IServerMessenger} suitable for testing.
 */
public final class TestServerMessenger extends AbstractServerMessenger {
  public TestServerMessenger(final String name, final int port) throws IOException {
    super(name, port, new DefaultObjectStreamFactory());
  }

  @Override
  protected String getAdministrativeMuteChatMessage() {
    return "You have been muted";
  }

  @Override
  protected String getChatChannelName() {
    return "_TEST_CHAT";
  }
}
