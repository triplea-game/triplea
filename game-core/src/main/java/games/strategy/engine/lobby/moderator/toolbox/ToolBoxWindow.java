package games.strategy.engine.lobby.moderator.toolbox;

import java.awt.Component;

import org.triplea.http.client.moderator.toolbox.ModeratorToolboxClient;
import org.triplea.swing.JFrameBuilder;
import org.triplea.swing.JPanelBuilder;

import games.strategy.engine.lobby.moderator.toolbox.tabs.TabFactory;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;


/**
 * This window shows a series of tabs that provide CRUD operations to a moderator. Each tab roughly maps
 * to a DB table.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class ToolBoxWindow {

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
