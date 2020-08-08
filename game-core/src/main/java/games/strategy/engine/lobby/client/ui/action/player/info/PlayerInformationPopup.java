package games.strategy.engine.lobby.client.ui.action.player.info;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import lombok.experimental.UtilityClass;
import org.triplea.domain.data.UserName;
import org.triplea.http.client.lobby.moderator.PlayerSummary;
import org.triplea.swing.JButtonBuilder;
import org.triplea.swing.JDialogBuilder;
import org.triplea.swing.JTabbedPaneBuilder;
import org.triplea.swing.jpanel.JPanelBuilder;

@UtilityClass
class PlayerInformationPopup {

  void showPopup(
      final JFrame parent, final UserName playerName, final PlayerSummary playerSummary) {
    SwingUtilities.invokeLater(
        () -> {
          new JDialogBuilder()
              .parent(parent)
              .title("Player: " + playerName.getValue())
              .add(dialog -> PlayerInformationPopup.buildContentPanel(dialog, playerSummary))
              .escapeKeyCloses()
              .buildAndShow();
        });
  }

  private JPanel buildContentPanel(final JDialog dialog, final PlayerSummary playerSummary) {
    final JTabbedPaneBuilder tabbedPaneBuilder = new JTabbedPaneBuilder();

    final var playerGamesTab = new PlayerGamesTab(playerSummary);
    tabbedPaneBuilder.addTab(playerGamesTab.getTabTitle(), playerGamesTab.getTabContents(dialog));

    // Only moderators receive aliases and ban information back from the server.
    // If we get that information, build the tabs for it, otherwise skip.
    if (playerSummary.getAliases() != null && playerSummary.getBans() != null) {
      final var playerAliasesTab = new PlayerAliasesTab(playerSummary);
      final var playerBansTab = new PlayerBansTab(playerSummary);

      tabbedPaneBuilder
          .addTab(playerAliasesTab.getTabTitle(), playerAliasesTab.getTabContents())
          .addTab(playerBansTab.getTabTitle(), playerBansTab.getTabContents());
    }

    return new JPanelBuilder()
        .borderLayout()
        .addNorth(PlayerInfoSummaryTextArea.buildPlayerInfoSummary(dialog, playerSummary))
        .addCenter(tabbedPaneBuilder.build())
        .addSouth(new JButtonBuilder("Close").actionListener(dialog::dispose).build())
        .build();
  }
}
