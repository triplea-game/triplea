package games.strategy.engine.framework.startup.ui;

import games.strategy.net.INode;
import java.util.function.Consumer;
import lombok.Builder;
import org.triplea.lobby.common.ILobbyGameController;
import org.triplea.util.ExitStatus;

/**
 * Verifies that the current local server is available from public internet. This is done by
 * checking if the lobby can do a 'reverse' connection back to this server.
 */
@Builder
public class LocalServerAvailabilityCheck {
  private final ILobbyGameController controller;
  private final INode localNode;
  private final Consumer<String> errorHandler;

  public void run() {
    // if we lose our connection, then shutdown
    new Thread(
            () -> {
              if (!controller.testGame(localNode)) {
                // if the server cannot connect to us, then quit
                errorHandler.accept(
                    "Your computer is not reachable from the internet.\n"
                        + "Please make sure your Firewall allows incoming connections (hosting) "
                        + "for TripleA.\n"
                        + "(The firewall exception must be updated every time a new version of "
                        + "TripleA comes out.)\n"
                        + "And that your Router is configured to send TCP traffic on port "
                        + localNode.getPort()
                        + " to your local ip address.\n"
                        + "See 'How To Host...' in the help menu, at the top of the lobby "
                        + "screen.");
                ExitStatus.FAILURE.exit();
              }
            })
        .start();
  }
}
