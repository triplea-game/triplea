package games.strategy.engine.lobby.moderator.toolbox.tabs;

import games.strategy.engine.lobby.moderator.toolbox.tabs.access.log.AccessLogTab;
import games.strategy.engine.lobby.moderator.toolbox.tabs.bad.words.BadWordsTab;
import games.strategy.engine.lobby.moderator.toolbox.tabs.banned.names.BannedUsernamesTab;
import games.strategy.engine.lobby.moderator.toolbox.tabs.banned.users.BannedUsersTab;
import games.strategy.engine.lobby.moderator.toolbox.tabs.event.log.EventLogTab;
import games.strategy.engine.lobby.moderator.toolbox.tabs.moderators.ModeratorsTab;
import java.awt.Component;
import javax.annotation.Nonnull;
import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import lombok.Builder;
import org.triplea.http.client.lobby.moderator.toolbox.HttpModeratorToolboxClient;
import org.triplea.swing.JTabbedPaneBuilder;

/** Factory class to construct the 'tabs' that go in the moderator toolbox tabbed window. */
@Builder
public final class TabFactory {
  @Nonnull private final JFrame frame;
  @Nonnull private final HttpModeratorToolboxClient httpModeratorToolboxClient;

  public JTabbedPane buildTabs() {
    return JTabbedPaneBuilder.builder()
        .addTab("Access Log", buildAccessLogTab())
        .addTab("Bad Words", buildBadWordsTab())
        .addTab("Banned Names", buildBannedUsernamesTab())
        .addTab("Banned Users", buildBannedUsersTab())
        .addTab("Event Log", buildEventLogTab())
        .addTab("Moderators", buildModeratorsTab())
        .build();
  }

  private Component buildAccessLogTab() {
    return new AccessLogTab(
            frame,
            httpModeratorToolboxClient.getToolboxAccessLogClient(),
            httpModeratorToolboxClient.getToolboxUserBanClient(),
            httpModeratorToolboxClient.getToolboxUsernameBanClient())
        .get();
  }

  private Component buildBadWordsTab() {
    return new BadWordsTab(frame, httpModeratorToolboxClient.getToolboxBadWordsClient()).get();
  }

  private Component buildBannedUsernamesTab() {
    return new BannedUsernamesTab(frame, httpModeratorToolboxClient.getToolboxUsernameBanClient())
        .get();
  }

  private Component buildBannedUsersTab() {
    return new BannedUsersTab(frame, httpModeratorToolboxClient.getToolboxUserBanClient()).get();
  }

  private Component buildModeratorsTab() {
    return new ModeratorsTab(
            frame, httpModeratorToolboxClient.getToolboxModeratorManagementClient())
        .get();
  }

  private Component buildEventLogTab() {
    return new EventLogTab(httpModeratorToolboxClient.getToolboxEventLogClient()).get();
  }
}
