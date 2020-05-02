package games.strategy.engine.lobby.client.ui.action.player.info;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import lombok.experimental.UtilityClass;
import org.triplea.http.client.lobby.moderator.PlayerSummaryForModerator;
import org.triplea.swing.JTabbedPaneBuilder;
import org.triplea.swing.jpanel.JPanelBuilder;

@UtilityClass
class PlayerInformationPopup {

  void showPopup(final JFrame parent, final PlayerSummaryForModerator playerSummaryForModerator) {
    SwingUtilities.invokeLater(
        () -> {
          final JDialog dialog =
              new JDialog(parent, "Player Info: " + playerSummaryForModerator.getName());
          dialog
              .getContentPane()
              .add(PlayerInformationPopup.buildContentPanel(playerSummaryForModerator));
          dialog.pack();
          dialog.setLocationRelativeTo(parent);
          dialog.setVisible(true);
        });
  }

  private JPanel buildContentPanel(final PlayerSummaryForModerator playerSummaryForModerator) {
    final var playerAliasesTab = new PlayerAliasesTab(playerSummaryForModerator);
    final var playerBansTab = new PlayerBansTab(playerSummaryForModerator);

    return new JPanelBuilder()
        .borderLayout()
        .addNorth(PlayerInfoSummaryTextArea.buildPlayerInfoSummary(playerSummaryForModerator))
        .addCenter(
            new JTabbedPaneBuilder()
                .addTab(playerAliasesTab.getTabTitle(), playerAliasesTab.getTabContents())
                .addTab(playerBansTab.getTabTitle(), playerBansTab.getTabContents())
                .build())
        .build();
  }
}
