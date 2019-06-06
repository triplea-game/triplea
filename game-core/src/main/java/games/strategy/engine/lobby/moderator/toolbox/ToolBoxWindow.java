package games.strategy.engine.lobby.moderator.toolbox;

import java.awt.Component;
import java.net.URI;
import java.util.Optional;

import javax.swing.JFrame;

import org.triplea.http.client.moderator.toolbox.ModeratorToolboxClient;
import org.triplea.swing.JFrameBuilder;
import org.triplea.swing.JPanelBuilder;
import org.triplea.swing.SwingComponents;

import games.strategy.engine.lobby.client.login.LobbyPropertyFetcherConfiguration;
import games.strategy.engine.lobby.client.login.LobbyServerProperties;
import games.strategy.engine.lobby.moderator.toolbox.tabs.TabFactory;
import games.strategy.triplea.settings.ClientSetting;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;


/**
 * This window shows a series of tabs that provide CRUD operations to a moderator. Each tab roughly maps
 * to a DB table.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ToolBoxWindow {

  // TODO: remove this main method, just here for demo purposes.
  public static void main(final String[] args) {
    ClientSetting.initialize();
    ClientSetting.testLobbyHost.setValue("localhost");
    verifyApiKeyAndShowWindow(null);
  }

  /**
   * Fetches the moderator API key from settings, checks if valid, if so we'll show the toolbox window.
   * If the API key is not valid we'll show a 'enter-api-key-window' where a moderator can enter their API
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

    // Check for an API key to be present, if none then we'll show teh 'enter-api-key' window.
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
            .map(port -> URI.create("http://" + host + ":" + port))
            .orElse(null));
    if (uriOverride.isPresent()) {
      return uriOverride;
    }

    return LobbyPropertyFetcherConfiguration.lobbyServerPropertiesFetcher()
        .fetchLobbyServerProperties()
        .map(LobbyServerProperties::getHttpsServerUri);
  }

  static void showWindow(final Component parent, final ModeratorToolboxClient moderatorToolboxClient) {
    JFrameBuilder.builder()
        .title("Moderator Toolbox")
        .locateRelativeTo(parent)
        .size(800, 600)
        .minSize(400, 400)
        .add(frame -> JPanelBuilder.builder()
            .border(10)
            .addCenter(TabFactory.buildTabs(frame, moderatorToolboxClient))
            .build())
        .visible(true)
        .build();
  }
}
