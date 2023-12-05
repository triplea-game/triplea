package games.strategy.engine.lobby.moderator.toolbox.tabs;

import games.strategy.engine.lobby.moderator.toolbox.tabs.access.log.AccessLogTab;
import games.strategy.engine.lobby.moderator.toolbox.tabs.bad.words.BadWordsTab;
import games.strategy.engine.lobby.moderator.toolbox.tabs.banned.names.BannedUsernamesTab;
import games.strategy.engine.lobby.moderator.toolbox.tabs.banned.users.BannedUsersTab;
import games.strategy.engine.lobby.moderator.toolbox.tabs.event.log.EventLogTab;
import games.strategy.engine.lobby.moderator.toolbox.tabs.maps.MapsTab;
import games.strategy.engine.lobby.moderator.toolbox.tabs.maps.MapsTabModel;
import games.strategy.engine.lobby.moderator.toolbox.tabs.moderators.ModeratorsTab;
import java.awt.Component;
import javax.annotation.Nonnull;
import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import lombok.Builder;
import org.triplea.http.client.lobby.moderator.toolbox.ModeratorToolboxClient;
import org.triplea.http.client.maps.admin.MapTagAdminClient;
import org.triplea.http.client.maps.listing.MapsClient;
import org.triplea.swing.JTabbedPaneBuilder;

/** Factory class to construct the 'tabs' that go in the moderator toolbox tabbed window. */
@Builder
public final class TabFactory {
  @Nonnull private final JFrame frame;
  @Nonnull private final ModeratorToolboxClient moderatorToolboxClient;
  @Nonnull private final MapsClient mapsClient;
  @Nonnull private final MapTagAdminClient mapTagAdminClient;

  public JTabbedPane buildTabs() {
    return JTabbedPaneBuilder.builder()
        .addTab("Access Log", buildAccessLogTab())
        .addTab("Bad Words", buildBadWordsTab())
        .addTab("Banned Names", buildBannedUsernamesTab())
        .addTab("Banned Users", buildBannedUsersTab())
        .addTab("Event Log", buildEventLogTab())
        .addTab("Moderators", buildModeratorsTab())
        .addTab("Maps", buildMapsTab())
        .build();
  }

  private Component buildAccessLogTab() {
    return new AccessLogTab(
            frame,
            moderatorToolboxClient.getToolboxAccessLogClient(),
            moderatorToolboxClient.getToolboxUserBanClient(),
            moderatorToolboxClient.getToolboxUsernameBanClient())
        .get();
  }

  private Component buildBadWordsTab() {
    return new BadWordsTab(frame, moderatorToolboxClient.getToolboxBadWordsClient()).get();
  }

  private Component buildBannedUsernamesTab() {
    return new BannedUsernamesTab(frame, moderatorToolboxClient.getToolboxUsernameBanClient())
        .get();
  }

  private Component buildBannedUsersTab() {
    return new BannedUsersTab(frame, moderatorToolboxClient.getToolboxUserBanClient()).get();
  }

  private Component buildModeratorsTab() {
    return new ModeratorsTab(frame, moderatorToolboxClient.getToolboxModeratorManagementClient())
        .get();
  }

  private Component buildMapsTab() {
    return MapsTab.builder()
        .parentWindowHeight(frame.getWidth())
        .mapsTabModel(
            MapsTabModel.builder()
                .mapsClient(mapsClient)
                .mapTagAdminClient(mapTagAdminClient)
                .build())
        .build()
        .get();
  }

  private Component buildEventLogTab() {
    return new EventLogTab(moderatorToolboxClient.getToolboxEventLogClient()).get();
  }
}
