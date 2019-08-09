package games.strategy.engine.lobby.moderator.toolbox;

import games.strategy.engine.lobby.client.login.LobbyPropertyFetcherConfiguration;
import games.strategy.engine.lobby.client.login.LobbyServerProperties;
import games.strategy.triplea.settings.ClientSetting;
import java.net.URI;
import java.util.Optional;
import javax.swing.JFrame;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.triplea.swing.SwingComponents;

/**
 * This class initiates the flow of showing the moderator toolbox. Based on if an API KEY is stored
 * in client settings, we'll show the 'enter password' dialog to 'unlock' that key or we'll show the
 * 'enter api key' window for user to enter a new API key and create password.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ShowToolboxController {
  // TODO: remove this main method, just here for demo purposes.
  public static void main(final String[] args) {
    ClientSetting.initialize();
    ClientSetting.testLobbyHost.setValue("localhost");
    showToolbox(null);
  }

  /**
   * Begins flow to show the toolbox window.
   *
   * <p>Fetches the moderator API key from settings, checks if valid, if so we'll show the toolbox
   * window. If the API key is not valid we'll show a 'enter-api-key-window' where a moderator can
   * enter their API key and repeat the validation process.
   */
  public static void showToolbox(final JFrame parent) {
    // get the http server URI, if we fail then show an error dialog and abort.
    final Optional<URI> httpServerUri = getHttpServerUri();
    if (!httpServerUri.isPresent()) {
      SwingComponents.showDialog(
          "Error fetching http server URI",
          "Unable to get URI of the http server, please try again or contact TripleA support");
      return;
    }

    final boolean hasApiKey = ClientSetting.moderatorApiKey.isSet();
    if (hasApiKey) {
      EnterApiKeyPasswordWindow.show(parent, httpServerUri.get());
    } else {
      CreateNewApiKeyWindow.show(parent, httpServerUri.get());
    }
  }

  private static Optional<URI> getHttpServerUri() {
    final Optional<URI> uriOverride =
        ClientSetting.testLobbyHost
            .getValue()
            .map(
                host ->
                    ClientSetting.testLobbyHttpsPort
                        .getValue()
                        .map(port -> URI.create("http://" + host + ":" + port))
                        .orElse(null));
    if (uriOverride.isPresent()) {
      return uriOverride;
    }

    return LobbyPropertyFetcherConfiguration.lobbyServerPropertiesFetcher()
        .fetchLobbyServerProperties()
        .map(LobbyServerProperties::getHttpsServerUri);
  }
}
