package org.triplea.server.remote.actions;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.triplea.domain.data.ApiKey;
import org.triplea.http.client.IpAddressParser;
import org.triplea.http.client.lobby.moderator.toolbox.banned.user.ToolboxUserBanClient;
import org.triplea.http.client.lobby.moderator.toolbox.banned.user.UserBanParams;
import org.triplea.http.client.remote.actions.RemoteActionsClient;
import org.triplea.http.client.remote.actions.RemoteActionsWebsocketListener;
import org.triplea.server.http.DropwizardTest;

public class RemoteActionsWebsocketIntegrationTest extends DropwizardTest {
  // caution: api-key values must match database (integration.yml)
  private static final ApiKey API_KEY = ApiKey.of("test");

  /**
   * Verify ban remote-action listener end-to-end.
   *
   * <ul>
   *   <li>Connect via websocket, set up ban listener
   *   <li>Connect with toolbox ban client
   *   <li>Issue ban request through client
   *   <li>Verify listener receives ban message
   * </ul>
   *
   * Note, we could/should verify ban through moderator chat client, but that is not done due to
   * prohibitive test setup (just a bit complex setting up all of the listeners). So we rely on the
   * ban through toolbox only to verify we have the full flow set up.
   */
  @Test
  @DisplayName("Verify ban message received when banning through moderator toolbox")
  void receiveBanMessageThroughToolboxBan() {
    final RemoteActionsWebsocketListener listener = new RemoteActionsWebsocketListener(localhost);
    // register listener, add any received messages to collection
    final Collection<InetAddress> banMessages = new ArrayList<>();
    listener.addPlayerBannedListener(banMessages::add);

    // issue the ban
    ToolboxUserBanClient.newClient(localhost, API_KEY)
        .banUser(
            UserBanParams.builder()
                .hoursToBan(1)
                .ip("77.66.55.44")
                .systemId("system-id")
                .username("dummy-data")
                .build());

    // verify we receive the ban message
    Awaitility.await()
        .pollInterval(10, TimeUnit.MILLISECONDS)
        .atMost(5, TimeUnit.SECONDS)
        .until(() -> !banMessages.isEmpty());
  }

  /**
   * Verify shutdown remote-action end-to-end.
   *
   * <ul>
   *   <li>Connect via websocket, set up shutdown listener
   *   <li>Connect with RemoteActionsClient.
   *   <li>Issue shutdown request for localhost
   *   <li>Verify listener receives shutdown message
   * </ul>
   */
  @Test
  @DisplayName("Verify shutdown message can be received")
  void receiveShutdownRequest() {
    final RemoteActionsWebsocketListener listener = new RemoteActionsWebsocketListener(localhost);
    // register shutdown listener and increment message count if we see a message
    final AtomicInteger shutdownMessages = new AtomicInteger();
    listener.addShutdownRequestListener(shutdownMessages::incrementAndGet);

    // send the shutdown request, we should be listening for the localhost IP.
    new RemoteActionsClient(localhost, API_KEY)
        .sendShutdownRequest(IpAddressParser.fromString("127.0.0.1"));

    // verify we receive the shutdown message
    Awaitility.await()
        .pollInterval(10, TimeUnit.MILLISECONDS)
        .atMost(5, TimeUnit.SECONDS)
        .until(() -> shutdownMessages.get() == 1);
  }
}
