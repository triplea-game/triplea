package games.strategy.engine.framework.startup.ui;

import java.util.function.Consumer;
import lombok.Builder;
import org.triplea.http.client.web.socket.client.connections.GameToLobbyConnection;
import org.triplea.util.ExitStatus;

/**
 * Verifies that the current local server is available from public internet. This is done by
 * checking if the lobby can do a 'reverse' connection back to this server.
 */
@Builder
public class LocalServerAvailabilityCheck {
  private final GameToLobbyConnection gameToLobbyConnection;
  private final int localPort;
  private final Consumer<String> errorHandler;

  /**
   * Starts a thread that requests the server to do a 'reverse' connection back to the local client.
   * This is to ensure that our local IP address is public facing and that other computers on the
   * internet can establish connections to this (the local) host.
   */
  public void run() {
    // if we lose our connection, then shutdown
    new Thread(
            () -> {
              if (!gameToLobbyConnection.checkConnectivity(localPort)) {
                // if the server cannot connect to us, then quit
                errorHandler.accept(
                    "Your computer is not reachable from the internet.\n"
                        + "Please make sure your Firewall allows incoming connections (hosting) "
                        + "for TripleA.\n"
                        + "(The firewall exception must be updated every time a new version of "
                        + "TripleA comes out.)\n"
                        + "And that your Router is configured to send TCP traffic on port "
                        + localPort
                        + " to your local ip address.\n"
                        + "See 'How To Host...' in the help menu, at the top of the lobby "
                        + "screen.");
                ExitStatus.FAILURE.exit();
              }
            })
        .start();
  }
}
