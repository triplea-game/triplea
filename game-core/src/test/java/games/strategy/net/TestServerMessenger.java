package games.strategy.net;

import java.io.IOException;

/** Implementation of {@link IServerMessenger} suitable for testing. */
public final class TestServerMessenger extends AbstractServerMessenger {
  public static final String ADMINISTRATIVE_MUTE_CHAT_MESSAGE = "You have been muted";
  public static final String CHAT_CHANNEL_NAME = "_TEST_CHAT";

  public TestServerMessenger(final String name, final int port) throws IOException {
    super(name, port, new DefaultObjectStreamFactory());
  }

  @Override
  protected String getAdministrativeMuteChatMessage() {
    return ADMINISTRATIVE_MUTE_CHAT_MESSAGE;
  }

  @Override
  protected String getChatChannelName() {
    return CHAT_CHANNEL_NAME;
  }
}
