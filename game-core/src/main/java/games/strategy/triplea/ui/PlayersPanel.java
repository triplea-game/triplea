package games.strategy.triplea.ui;

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.triplea.swing.JPanelBuilder;

import games.strategy.engine.data.PlayerId;
import games.strategy.engine.framework.IGame;

/**
 * Panel to show who is playing which players.
 */
public final class PlayersPanel {
  private PlayersPanel() {}

  /**
   * Displays a dialog that shows which node (user) is controlling each player (nation, power, etc.).
   */
  public static void showPlayers(final IGame game, final Component parent) {
    final JPanel panel = JPanelBuilder.builder()
        .verticalBoxLayout()
        .build();
    for (final String player : game.getPlayerManager().getPlayers()) {
      final PlayerId playerId = game.getData().getPlayerList().getPlayerId(player);
      if (playerId.isAi()) {
        panel.add(new JLabel(playerId.getPlayerType().name + " is " + playerId.getName(), JLabel.RIGHT));
      } else {
        panel.add(
            new JLabel(game.getPlayerManager().getNode(player).getName() + " is " + playerId.getName(), JLabel.RIGHT));
      }
    }

    JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(parent), panel, "Players",
        JOptionPane.PLAIN_MESSAGE);
  }
}
