package games.strategy.engine.lobby.moderator.toolbox.tabs;

import java.awt.Component;
import java.net.URI;

import javax.annotation.Nonnull;
import javax.swing.JFrame;
import javax.swing.JTabbedPane;

import org.triplea.http.client.moderator.toolbox.ApiKeyPassword;
import org.triplea.http.client.moderator.toolbox.access.log.ToolboxAccessLogClient;
import org.triplea.http.client.moderator.toolbox.api.key.ToolboxApiKeyClient;
import org.triplea.http.client.moderator.toolbox.bad.words.ToolboxBadWordsClient;
import org.triplea.http.client.moderator.toolbox.banned.name.ToolboxUsernameBanClient;
import org.triplea.http.client.moderator.toolbox.banned.user.ToolboxUserBanClient;
import org.triplea.http.client.moderator.toolbox.event.log.ToolboxEventLogClient;
import org.triplea.http.client.moderator.toolbox.moderator.management.ToolboxModeratorManagementClient;
import org.triplea.swing.JTabbedPaneBuilder;

import games.strategy.engine.lobby.moderator.toolbox.tabs.access.log.AccessLogTab;
import games.strategy.engine.lobby.moderator.toolbox.tabs.api.keys.ApiKeysTab;
import games.strategy.engine.lobby.moderator.toolbox.tabs.bad.words.BadWordsTab;
import games.strategy.engine.lobby.moderator.toolbox.tabs.banned.names.BannedUsernamesTab;
import games.strategy.engine.lobby.moderator.toolbox.tabs.banned.users.BannedUsersTab;
import games.strategy.engine.lobby.moderator.toolbox.tabs.event.log.EventLogTab;
import games.strategy.engine.lobby.moderator.toolbox.tabs.moderators.ModeratorsTab;
import lombok.Builder;

/**
 * Factory class to construct the 'tabs' that go in the moderator toolbox tabbed window.
 */
@Builder
public final class TabFactory {

  @Nonnull
  private final JFrame frame;
  @Nonnull
  private final URI uri;

  @Nonnull
  private final ApiKeyPassword apiKeyPassword;

  public JTabbedPane buildTabs() {
    return JTabbedPaneBuilder.builder()
        .addTab("Access Log", buildAccessLogTab())
        .addTab("API Keys", buildApiKeysTab())
        .addTab("Bad Words", buildBadWordsTab())
        .addTab("Banned Names", buildBannedUserNamesTab())
        .addTab("Banned Users", buildBannedUsersTab())
        .addTab("Event Log", buildEventLogTab())
        .addTab("Moderators", buildModeratorsTab())
        .build();
  }

  private Component buildAccessLogTab() {
    return new AccessLogTab(
        frame,
        ToolboxAccessLogClient.newClient(uri, apiKeyPassword),
        ToolboxUserBanClient.newClient(uri, apiKeyPassword),
        ToolboxUsernameBanClient.newClient(uri, apiKeyPassword)).get();
  }

  private Component buildApiKeysTab() {
    return new ApiKeysTab(frame, ToolboxApiKeyClient.newClient(uri, apiKeyPassword)).get();
  }

  private Component buildBadWordsTab() {
    return new BadWordsTab(frame, ToolboxBadWordsClient.newClient(uri, apiKeyPassword)).get();
  }

  private Component buildBannedUserNamesTab() {
    return new BannedUsernamesTab(frame, ToolboxUsernameBanClient.newClient(uri, apiKeyPassword)).get();
  }

  private Component buildBannedUsersTab() {
    return new BannedUsersTab(frame, ToolboxUserBanClient.newClient(uri, apiKeyPassword)).get();
  }

  private Component buildModeratorsTab() {
    return new ModeratorsTab(frame, ToolboxModeratorManagementClient.newClient(uri, apiKeyPassword)).get();
  }

  private Component buildEventLogTab() {
    return new EventLogTab(ToolboxEventLogClient.newClient(uri, apiKeyPassword)).get();
  }
}
