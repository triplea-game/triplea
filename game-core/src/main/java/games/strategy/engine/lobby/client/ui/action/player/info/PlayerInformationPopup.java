package games.strategy.engine.lobby.client.ui.action.player.info;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import lombok.experimental.UtilityClass;
import org.triplea.http.client.lobby.moderator.PlayerSummary;
import org.triplea.swing.JTabbedPaneBuilder;
import org.triplea.swing.jpanel.JPanelBuilder;

@UtilityClass
class PlayerInformationPopup {

  void showPopup(final JFrame parent, final PlayerSummary playerSummary) {
    SwingUtilities.invokeLater(
        () -> {
          final JDialog dialog =
              new JDialog(parent, "Player Info: " + playerSummary.getName());
          dialog
              .getContentPane()
              .add(PlayerInformationPopup.buildContentPanel(playerSummary));
          dialog.pack();
          dialog.setLocationRelativeTo(parent);
          dialog.setVisible(true);
        });
  }

  private JPanel buildContentPanel(final PlayerSummary playerSummary) {
    final var playerAliasesTab = new PlayerAliasesTab(playerSummary);
    final var playerBansTab = new PlayerBansTab(playerSummary);

    return new JPanelBuilder()
        .borderLayout()
        .addNorth(PlayerInfoSummaryTextArea.buildPlayerInfoSummary(playerSummary))
        .addCenter(
            new JTabbedPaneBuilder()
                .addTab(playerAliasesTab.getTabTitle(), playerAliasesTab.getTabContents())
                .addTab(playerBansTab.getTabTitle(), playerBansTab.getTabContents())
                .build())
        .build();
  }
}
