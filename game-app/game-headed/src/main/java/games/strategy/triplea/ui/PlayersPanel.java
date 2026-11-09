package games.strategy.triplea.ui;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.framework.IGame;
import java.awt.Component;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import lombok.experimental.UtilityClass;
import org.triplea.swing.jpanel.JPanelBuilder;

/** Panel to show who is playing which players. */
@UtilityClass
public class PlayersPanel {

  /**
   * Displays a dialog that shows which node (user) is controlling each player (nation, power,
   * etc.).
   */
  public static void showPlayers(final IGame game, final Component parent) {
    final JPanel panel = new JPanelBuilder().boxLayoutVertical().build();
    for (final String player : game.getPlayerManager().getPlayers()) {
      final GamePlayer gamePlayer = game.getData().getPlayerList().getPlayerId(player);
      if (gamePlayer.isAi()) {
        panel.add(
            new JLabel(
                gamePlayer.getPlayerType().name + " is " + gamePlayer.getName(), JLabel.RIGHT));
      } else {
        panel.add(
            new JLabel(
                game.getPlayerManager().getNode(player).getName() + " is " + gamePlayer.getName(),
                JLabel.RIGHT));
      }
    }

    JOptionPane.showMessageDialog(
        JOptionPane.getFrameForComponent(parent), panel, "Players", JOptionPane.PLAIN_MESSAGE);
  }
}
