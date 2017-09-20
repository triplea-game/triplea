package games.strategy.triplea.ui;

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import games.strategy.engine.data.PlayerID;
import games.strategy.engine.framework.IGame;
import swinglib.JPanelBuilder;

/**
 * Panel to show who is playing which players.
 */
public class PlayersPanel {

  public static void showPlayers(final IGame game, final Component parent) {
    final JPanel panel = JPanelBuilder.builder()
        .verticalBoxLayout()
        .build();
    for (final String player : game.getPlayerManager().getPlayers()) {
      final PlayerID playerId = game.getData().getPlayerList().getPlayerId(player);
      if (playerId.isAi()) {
        panel.add(new JLabel(playerId.getWhoAmI().split(":")[1] + " is " + playerId.getName(), JLabel.RIGHT));
      } else {
        panel.add(
            new JLabel(game.getPlayerManager().getNode(player).getName() + " is " + playerId.getName(), JLabel.RIGHT));
      }
    }

    JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(parent), panel, "Players",
        JOptionPane.PLAIN_MESSAGE);
  }
}
