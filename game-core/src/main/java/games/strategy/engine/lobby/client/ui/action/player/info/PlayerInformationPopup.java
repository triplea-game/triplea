package games.strategy.engine.lobby.client.ui.action.player.info;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import lombok.experimental.UtilityClass;
import org.triplea.domain.data.UserName;
import org.triplea.http.client.lobby.moderator.PlayerSummary;
import org.triplea.swing.JTabbedPaneBuilder;
import org.triplea.swing.jpanel.JPanelBuilder;

@UtilityClass
class PlayerInformationPopup {

  void showPopup(
      final JFrame parent, final UserName playerName, final PlayerSummary playerSummary) {
    SwingUtilities.invokeLater(
        () -> {
          final JDialog dialog = new JDialog(parent, "Player Info: " + playerName.getValue());
          dialog
              .getContentPane()
              .add(PlayerInformationPopup.buildContentPanel(playerName, playerSummary));
          dialog.pack();
          dialog.setLocationRelativeTo(parent);
          dialog.setVisible(true);
        });
  }

  private JPanel buildContentPanel(final UserName playerName, final PlayerSummary playerSummary) {
    final JTabbedPaneBuilder tabbedPaneBuilder = new JTabbedPaneBuilder();

    final var playerGamesTab = new PlayerGamesTab(playerSummary);
    tabbedPaneBuilder.addTab(playerGamesTab.getTabTitle(), playerGamesTab.getTabContents());

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
        .addNorth(
            // moderators will get IP and system-id information
            playerSummary.getIp() == null
                ? new JPanel()
                : PlayerInfoSummaryTextArea.buildPlayerInfoSummary(playerName, playerSummary))
        .addCenter(tabbedPaneBuilder.build())
        .build();
  }
}
