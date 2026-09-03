package games.strategy.net;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

import games.strategy.engine.framework.GameObjectStreamFactory;
import games.strategy.engine.framework.startup.mc.ClientModel;
import games.strategy.net.websocket.transport.WebsocketClientMessenger;
import games.strategy.net.websocket.transport.WebsocketServerMessenger;
import games.strategy.net.websocket.transport.WebsocketTransport;
import games.strategy.triplea.settings.AbstractClientSettingTestCase;
import games.strategy.triplea.settings.ClientSetting;
import java.net.URI;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.triplea.game.server.GameRelayServer;

/**
 * Verifies the client construction seam ({@link ClientMessengerFactory#newClientMessenger}) honours
 * the {@code useWebsocketTransport} flag: OFF (default) leaves the socket path selected, ON builds
 * a {@link WebsocketClientMessenger} that connects to the host's relay.
 *
 * <p>Driving the full {@code ClientModel}/{@code ServerModel} (lobby/UI heavy) is impractical in a
 * unit test, so the seam is exercised directly. The server seam (relay-start ordering + {@link
 * WebsocketServerMessenger}) is covered end-to-end by {@code WebsocketMessengerIntegrationTest};
 * here a host messenger stands in so the client seam can be proven against a live relay room.
 */
class ClientMessengerFactoryTransportFlagTest extends AbstractClientSettingTestCase {

  private static final Duration TIMEOUT = Duration.ofSeconds(10);

  // A per-run port (avoids clashing with the other websocket relay tests in this JVM, whose relays
  // shut down asynchronously); the factory + host both read it via -Dtriplea.relay.port.
  private final int port = 6100 + ((int) (Math.random() * 800));
  private final URI relayUri = URI.create("ws://localhost:" + port);

  private GameRelayServer relay;
  private WebsocketServerMessenger host;
  private IClientMessenger client;

  @AfterEach
  void tearDown() {
    if (client != null) {
      client.shutDown();
    }
    if (host != null) {
      host.shutDown();
    }
    if (relay != null) {
      relay.stop();
    }
    System.clearProperty("triplea.relay.port");
  }

  @Test
  void flagOffDefaultsToOff() {
    assertThat(ClientSetting.useWebsocketTransport.getValue().orElse(false), is(false));
  }

  @Test
  void flagOnBuildsWebsocketClientMessengerConnectedToHostRelay() throws Exception {
    System.setProperty("triplea.relay.port", String.valueOf(port));
    relay = new GameRelayServer(port);
    relay.start();
    // Host joins first so the relay designates it host, exactly as ServerModel would.
    host =
        new WebsocketServerMessenger(
            relayUri, WebsocketTransport.ROUND1_GAME_ID, "host", new GameObjectStreamFactory(null));

    ClientSetting.useWebsocketTransport.setValue(true);
    final ClientModel.ClientProps props =
        ClientModel.ClientProps.builder().host("localhost").port(1234).name("client").build();

    client =
        ClientMessengerFactory.newClientMessenger(props, new GameObjectStreamFactory(null), null);

    assertThat(client, is(instanceOf(WebsocketClientMessenger.class)));
    await().atMost(TIMEOUT).until(client::isConnected);
    // The client agrees the host is the server node: proves it joined the same relay room.
    assertThat(client.getServerNode(), is(host.getLocalNode()));
    await().atMost(TIMEOUT).until(() -> host.getNodes().size() == 2);
  }
}
