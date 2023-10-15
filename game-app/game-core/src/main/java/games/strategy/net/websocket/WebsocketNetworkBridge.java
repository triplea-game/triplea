package games.strategy.net.websocket;

import games.strategy.triplea.settings.ClientSetting;
import java.net.URI;
import java.util.Map;
import java.util.function.Consumer;
import lombok.extern.java.Log;
import org.triplea.http.client.web.socket.GenericWebSocketClient;
import org.triplea.http.client.web.socket.messages.MessageType;
import org.triplea.http.client.web.socket.messages.WebSocketMessage;

@Log
public class WebsocketNetworkBridge implements ClientNetworkBridge {
  private GenericWebSocketClient genericWebSocketClient;

  public WebsocketNetworkBridge(final URI serverUri) {
    if (ClientSetting.useWebsocketNetwork.getValue().orElse(false)) {
      genericWebSocketClient =
          GenericWebSocketClient.builder()
              .websocketUri(serverUri)
              .errorHandler(log::warning)
              .headers(Map.of())
              .build();

      log.info("Connecting to game server: " + serverUri);
      genericWebSocketClient.connect();
    }
  }

  @Override
  public void disconnect() {
    genericWebSocketClient.close();
  }

  @Override
  public void sendMessage(final WebSocketMessage webSocketMessage) {
    if (ClientSetting.useWebsocketNetwork.getValue().orElse(false)) {
      genericWebSocketClient.sendMessage(webSocketMessage);
    }
  }

  @Override
  public <T extends WebSocketMessage> void addListener(
      final MessageType<T> messageType, final Consumer<T> messageConsumer) {
    if (ClientSetting.useWebsocketNetwork.getValue().orElse(false)) {
      genericWebSocketClient.addListener(messageType, messageConsumer);
    }
  }
}
