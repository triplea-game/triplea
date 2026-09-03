package games.strategy.net.websocket.transport;

import games.strategy.net.IClientMessenger;
import games.strategy.net.IMessengerErrorListener;
import games.strategy.net.IObjectStreamFactory;
import java.io.IOException;
import java.net.URI;

/**
 * Websocket {@link IClientMessenger} that runs the unchanged L1 {@code UnifiedMessenger} on top of
 * a relay connection. It is a plain relay client (not the host); the host is a {@link
 * WebsocketServerMessenger}. See {@link RelayMessenger} for the shared transport behaviour.
 */
public class WebsocketClientMessenger extends RelayMessenger implements IClientMessenger {

  public WebsocketClientMessenger(
      final URI relayUri,
      final String gameId,
      final String playerName,
      final IObjectStreamFactory objectStreamFactory)
      throws IOException {
    super(relayUri, gameId, playerName, objectStreamFactory);
  }

  @Override
  public boolean isServer() {
    return false;
  }

  @Override
  public void addErrorListener(final IMessengerErrorListener listener) {
    super.addErrorListener(listener);
  }

  @Override
  public void removeErrorListener(final IMessengerErrorListener listener) {
    super.removeErrorListener(listener);
  }
}
