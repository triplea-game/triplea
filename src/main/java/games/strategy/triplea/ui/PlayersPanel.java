package games.strategy.triplea.ui;

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import games.strategy.engine.data.PlayerID;
import games.strategy.engine.framework.IGame;
import games.strategy.ui.SwingComponents;

/**
 * Panel to show who is playing which players
 */
public class PlayersPanel {

  public static void showPlayers(final IGame game, final Component parent) {
    JPanel panel = SwingComponents.newJPanelWithVerticalBoxLayout();
    for (final String player : game.getPlayerManager().getPlayers()) {
      final PlayerID playerID = game.getData().getPlayerList().getPlayerID(player);
      if (playerID.isAI()) {
        panel.add(new JLabel(playerID.getWhoAmI().split(":")[1] + " is " + playerID.getName(), JLabel.RIGHT));
      } else {
        panel.add(new JLabel(game.getPlayerManager().getNode(player).getName() + " is " + playerID.getName(), JLabel.RIGHT));
      }
    }

    JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(parent), panel, "Players",
        JOptionPane.PLAIN_MESSAGE);
  }
}
