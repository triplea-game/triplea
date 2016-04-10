package games.strategy.common.ui;

import java.awt.Component;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.PlayerManager;
import games.strategy.engine.framework.IGame;
import games.strategy.util.CountDownLatchHandler;
import games.strategy.util.EventThreadJOptionPane;

/**
 * Panel to show who is playing which players
 */
public class PlayersPanel extends JPanel {
  private static final long serialVersionUID = -4283654829822141065L;

  public PlayersPanel(final PlayerManager players, final GameData data) {
    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    for (final String player : players.getPlayers()) {
      final PlayerID playerID = data.getPlayerList().getPlayerID(player);
      if (playerID.isAI()) {
        add(new JLabel(playerID.getWhoAmI().split(":")[1] + " is " + playerID.getName(), JLabel.RIGHT));
      } else {
        add(new JLabel(players.getNode(player).getName() + " is " + playerID.getName(), JLabel.RIGHT));
      }
    }
  }

  public static void showPlayers(final IGame game, final Component parent) {
    final PlayersPanel panel = new PlayersPanel(game.getPlayerManager(), game.getData());
    EventThreadJOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(parent), panel, "Players",
        JOptionPane.PLAIN_MESSAGE, new CountDownLatchHandler(true));
  }
}
