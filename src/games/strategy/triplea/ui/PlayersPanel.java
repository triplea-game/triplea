package games.strategy.triplea.ui;

import java.awt.Component;
import java.awt.Image;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.PlayerManager;
import games.strategy.engine.framework.IGame;

/**
 * Panel to show who is playing which players
 */
public class PlayersPanel extends JPanel {
  private static final long serialVersionUID = 9177417134839960231L;

  public PlayersPanel(final PlayerManager players, final IUIContext uiContext, final GameData data) {
    PlayerManager m_players = players;
    IUIContext m_uiContext = uiContext;
    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    for (final String player : m_players.getPlayers()) {
      final PlayerID playerID = data.getPlayerList().getPlayerID(player);
      final Image img = m_uiContext.getFlagImageFactory().getFlag(playerID);
      add(new JLabel(m_players.getNode(player).getName(), new ImageIcon(img), JLabel.RIGHT));
    }
  }

  public static void showPlayers(final IGame game, final IUIContext context, final Component parent) {
    final PlayersPanel panel = new PlayersPanel(game.getPlayerManager(), context, game.getData());
    JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(parent), panel, "Players",
        JOptionPane.PLAIN_MESSAGE);
  }
}
