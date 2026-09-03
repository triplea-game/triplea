package games.strategy.net;

import games.strategy.engine.framework.startup.launcher.ServerLauncher;
import games.strategy.engine.framework.startup.login.ClientLogin;
import games.strategy.engine.framework.startup.mc.ClientModel;
import games.strategy.net.websocket.transport.WebsocketClientMessenger;
import games.strategy.net.websocket.transport.WebsocketTransport;
import games.strategy.triplea.settings.ClientSetting;
import java.io.IOException;
import java.net.URI;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.triplea.domain.data.SystemIdLoader;

/** Factory class for implementations of {@link IClientMessenger}. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public final class ClientMessengerFactory {

  /** Creates a client messenger suitable for connecting to a hosted game server. */
  public static IClientMessenger newClientMessenger(
      final ClientModel.ClientProps props,
      final IObjectStreamFactory objectStreamFactory,
      final ClientLogin clientLogin)
      throws IOException {
    if (ClientSetting.useWebsocketTransport.getValue().orElse(false)) {
      // Dial out to the host's in-process relay instead of opening a direct socket. The host runs
      // the relay on RELAY_SERVER_PORT (see ServerModel.createServerMessenger); round 1 uses one
      // shared game-id so host and client land in the same relay room.
      final URI relayUri =
          URI.create("ws://" + props.getHost() + ":" + ServerLauncher.relayServerPort());
      log.info("Connecting to relay: {}", relayUri);
      return new WebsocketClientMessenger(
          relayUri, WebsocketTransport.ROUND1_GAME_ID, props.getName(), objectStreamFactory);
    }
    log.info(String.format("Connecting to bot: %s:%s", props.getHost(), props.getPort()));
    return new ClientMessenger(
        props.getHost(),
        props.getPort(),
        props.getName(),
        SystemIdLoader.load(),
        objectStreamFactory,
        clientLogin);
  }
}
