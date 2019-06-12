package games.strategy.engine.lobby.moderator.toolbox;

import java.net.URI;

import org.triplea.http.client.moderator.toolbox.ModeratorToolboxClient;
import org.triplea.http.client.moderator.toolbox.RegisterApiKeyResult;
import org.triplea.swing.SwingComponents;

import games.strategy.triplea.settings.ClientSetting;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Utility class to extract submit action of sending API key and password for registration.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
class CreateNewApiKeyActions {

  static boolean registerApiKey(
      final URI serverUri,
      final String apiKey,
      final String newPassword) {

    final ModeratorToolboxClient toolboxClient =
        ModeratorToolboxClient.newClient(serverUri, newPassword);

    final RegisterApiKeyResult result = toolboxClient.registerNewKey(apiKey);

    return result.getNewApiKey()
        .map(newKey -> {
          ClientSetting.moderatorApiKey.setValueAndFlush(newKey);
          return true;
        })
        .orElseGet(() -> {
          final String error = result.getErrorMessage()
              .orElseThrow(() -> new IllegalStateException(
                  "Coding bug, both API key and error message results were empty. Expected"
                      + "at least an error message or an api key to be present."));

          SwingComponents.showDialog(
              "Incorrect API key",
              "API key validation failed. Verify and try again.\n"
                  + "Too many failed attempts will result in a lockout.\n"
                  + "Error from server: " + error);
          return false;
        });
  }
}
