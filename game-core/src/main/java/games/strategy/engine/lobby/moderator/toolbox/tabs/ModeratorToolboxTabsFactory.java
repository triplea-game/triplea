package games.strategy.engine.lobby.moderator.toolbox.tabs;

import javax.swing.JFrame;
import javax.swing.JTabbedPane;

import org.triplea.http.client.moderator.toolbox.ModeratorToolboxClient;
import org.triplea.swing.JTabbedPaneBuilder;

/**
 * Factory class to construct the 'tabs' that go in the moderator toolbox tabbed window.
 */
public class ModeratorToolboxTabsFactory {
  public static JTabbedPane buildTabs(final JFrame frame, final ModeratorToolboxClient moderatorToolboxClient) {
    return JTabbedPaneBuilder.builder()
        // TODO: WIP to add more tabs.
        // .addTab("Users", UsersTab.buildTab())
        // .addTab("Bans", BansTab.buildTab(frame))
        .addTab("Bad Words", BadWordsTab.buildTab(badWordsTabActions(frame, moderatorToolboxClient)))
        // .addTab("Moderators", ModeratorsTab.buildTab(frame, new ModeratorsTabModel()))
        .addTab("Event Log", EventLogTab.buildTab(new EventLogTabModel(moderatorToolboxClient)))
        .build();
  }

  private static BadWordsTabActions badWordsTabActions(
      final JFrame frame, final ModeratorToolboxClient moderatorToolboxClient) {
    return BadWordsTabActions.builder()
        .badWordsTabModel(BadWordsTabModel.builder()
            .moderatorToolboxClient(moderatorToolboxClient)
            .build())
        .parentFrame(frame)
        .build();
  }
}
