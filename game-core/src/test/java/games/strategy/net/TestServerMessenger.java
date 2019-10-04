package games.strategy.net;

import java.io.IOException;

/** Implementation of {@link IServerMessenger} suitable for testing. */
public final class TestServerMessenger extends ServerMessenger {
  public static final String ADMINISTRATIVE_MUTE_CHAT_MESSAGE = "You have been muted";
  public static final String CHAT_CHANNEL_NAME = "_TEST_CHAT";

  public TestServerMessenger() throws IOException {
    super("server", 0, new DefaultObjectStreamFactory());
  }
}
