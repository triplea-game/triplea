package games.strategy.net;

import java.io.IOException;
import org.jetbrains.annotations.NonNls;

/** Implementation of {@link IServerMessenger} suitable for testing. */
public final class TestServerMessenger extends ServerMessenger {
  @NonNls public static final String CHAT_CHANNEL_NAME = "_TEST_CHAT";

  public TestServerMessenger() throws IOException {
    super("server", 0, new DefaultObjectStreamFactory());
  }
}
