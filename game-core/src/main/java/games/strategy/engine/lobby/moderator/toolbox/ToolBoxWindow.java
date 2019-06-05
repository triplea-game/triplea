package games.strategy.engine.lobby.moderator.toolbox;

import java.awt.Component;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

import javax.swing.JFrame;
import javax.swing.JTabbedPane;

import org.triplea.http.client.moderator.toolbox.ModeratorToolboxClient;
import org.triplea.swing.JFrameBuilder;
import org.triplea.swing.JPanelBuilder;
import org.triplea.swing.SwingComponents;

import games.strategy.engine.lobby.client.login.LobbyPropertyFetcherConfiguration;
import games.strategy.engine.lobby.client.login.LobbyServerProperties;
import games.strategy.engine.lobby.moderator.toolbox.tabs.ModeratorToolboxTabsFactory;
import games.strategy.triplea.settings.ClientSetting;


public class ToolBoxWindow {

  // TODO: remove this main method, just here for demo purposes.
  public static void main(final String[] args) {
    ClientSetting.initialize();
    ClientSetting.testLobbyHost.setValue("localhost");
    verifyApiKeyAndShowWindow(null);
  }

  /**
   * Fetches the moderator API key from settings, checks if valid, if so we'll show the toolbox window.
   * If the API key is not valid we'll show a simple window where a moderator can enter their API
   * key and repeat the validation process.
   */
  public static void verifyApiKeyAndShowWindow(final JFrame parent) {
    // get the http server URI, if we fail then show an error dialog and abort.
    final Optional<URI> httpServerUri = getHttpServerUri();
    if (!httpServerUri.isPresent()) {
      SwingComponents.showDialog("Error fetching http server URI",
          "Unable to get URI of the http server, please try again or contact TripleA support");
      return;
    }

    final ModeratorToolboxClient toolboxClient = ModeratorToolboxClient.newClient(httpServerUri.get());

    if (!ClientSetting.moderatorApiKey.isSet()
        || ClientSetting.moderatorApiKey.getValueOrThrow().isEmpty()) {
      EnterApiKeyWindow.show(parent, toolboxClient);
      return;
    }

    final String validationResult =
        toolboxClient.validateApiKey(ClientSetting.moderatorApiKey.getValue().orElse(""));

    if (validationResult.equalsIgnoreCase(ModeratorToolboxClient.SUCCESS)) {
      showWindow(parent, toolboxClient);
    } else {
      SwingComponents.showDialog("Key validation error", "Invalid key:\n" + validationResult);
      EnterApiKeyWindow.show(parent, toolboxClient);
    }
  }

  private static Optional<URI> getHttpServerUri() {
    final Optional<URI> uriOverride = ClientSetting.testLobbyHost.getValue()
        .map(host -> ClientSetting.testLobbyHttpsPort.getValue()
            .map(port -> createHttpServerUri(host, port))
            .orElse(null));
    if (uriOverride.isPresent()) {
      return uriOverride;
    }

    return LobbyPropertyFetcherConfiguration.lobbyServerPropertiesFetcher()
        .fetchLobbyServerProperties()
        .map(LobbyServerProperties::getHttpsServerUri);
  }

  private static URI createHttpServerUri(final String host, final int port) {
    try {
      return new URI("http://" + host + ":" + port);
    } catch (final URISyntaxException e) {
      throw new RuntimeException(String.format("Programmer error, illegal URI, host: %s, port: %s", host, port));
    }
  }

  static void showWindow(final Component parent, final ModeratorToolboxClient moderatorToolboxClient) {
    final JFrame frame = JFrameBuilder.builder()
        .title("Moderator Toolbox")
        .locateRelativeTo(parent)
        .size(800, 600)
        .minSize(400, 400)
        .build();

    final JTabbedPane tabs = ModeratorToolboxTabsFactory.buildTabs(frame, moderatorToolboxClient);

    frame.add(
        JPanelBuilder.builder()
            .border(10)
            .addCenter(tabs)
            .build());
    frame.setVisible(true);
  }
}
