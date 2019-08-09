package games.strategy.engine.lobby.moderator.toolbox;

import games.strategy.engine.lobby.moderator.toolbox.tabs.TabFactory;
import java.awt.Component;
import java.net.URI;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.triplea.http.client.moderator.toolbox.ApiKeyPassword;
import org.triplea.swing.JFrameBuilder;
import org.triplea.swing.JPanelBuilder;
import org.triplea.swing.SwingAction;

/**
 * This window shows a series of tabs that provide CRUD operations to a moderator. Each tab roughly
 * maps to a DB table.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class ToolBoxWindow {

  static void showWindow(
      final Component parent, final URI serverUri, final ApiKeyPassword apiKeyPassword) {
    SwingAction.invokeNowOrLater(
        () ->
            JFrameBuilder.builder()
                .title("Moderator Toolbox")
                .locateRelativeTo(parent)
                .size(800, 700)
                .minSize(400, 400)
                .add(
                    frame ->
                        JPanelBuilder.builder()
                            .border(10)
                            .addCenter(
                                TabFactory.builder()
                                    .frame(frame)
                                    .uri(serverUri)
                                    .apiKeyPassword(apiKeyPassword)
                                    .build()
                                    .buildTabs())
                            .build())
                .visible(true)
                .build());
  }
}
