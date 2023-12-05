package games.strategy.engine.lobby.moderator.toolbox;

import games.strategy.engine.lobby.moderator.toolbox.tabs.TabFactory;
import games.strategy.triplea.EngineImageLoader;
import java.awt.Component;
import lombok.experimental.UtilityClass;
import org.triplea.http.client.lobby.moderator.toolbox.ModeratorToolboxClient;
import org.triplea.http.client.maps.admin.MapTagAdminClient;
import org.triplea.http.client.maps.listing.MapsClient;
import org.triplea.swing.JFrameBuilder;
import org.triplea.swing.SwingAction;
import org.triplea.swing.jpanel.JPanelBuilder;

/**
 * This window shows a series of tabs that provide CRUD operations to a moderator. Each tab roughly
 * maps to a DB table.
 */
@UtilityClass
public final class ToolBoxWindow {

  /** Shows the moderator toolbox UI window. */
  public static void showWindow(
      final Component parent,
      final ModeratorToolboxClient moderatorToolboxClient,
      final MapsClient mapsClient,
      final MapTagAdminClient mapTagAdminClient) {
    SwingAction.invokeNowOrLater(
        () ->
            JFrameBuilder.builder()
                .title("Moderator Toolbox")
                .locateRelativeTo(parent)
                .iconImage(EngineImageLoader.loadFrameIcon())
                .size(800, 700)
                .minSize(400, 400)
                .add(
                    frame ->
                        new JPanelBuilder()
                            .border(10)
                            .borderLayout()
                            .addCenter(
                                TabFactory.builder()
                                    .frame(frame)
                                    .moderatorToolboxClient(moderatorToolboxClient)
                                    .mapsClient(mapsClient)
                                    .mapTagAdminClient(mapTagAdminClient)
                                    .build()
                                    .buildTabs())
                            .build())
                .visible(true)
                .build());
  }
}
