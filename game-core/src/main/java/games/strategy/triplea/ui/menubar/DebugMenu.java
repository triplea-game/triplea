package games.strategy.triplea.ui.menubar;

import games.strategy.engine.player.Player;
import games.strategy.engine.player.PlayerDebug;
import games.strategy.triplea.ai.pro.AbstractProAi;
import games.strategy.triplea.settings.ClientSetting;
import games.strategy.triplea.ui.TripleAFrame;
import java.awt.event.KeyEvent;
import java.util.Set;
import javax.swing.JMenu;
import org.triplea.swing.SwingAction;

final class DebugMenu extends JMenu {
  private static final long serialVersionUID = -4876915214715298132L;

  DebugMenu(final TripleAFrame frame) {
    super("Debug");

    setMnemonic(KeyEvent.VK_D);

    final Set<Player> players = frame.getLocalPlayers().getLocalPlayers();
    final boolean areThereProAIs = players.stream().anyMatch(AbstractProAi.class::isInstance);
    if (areThereProAIs) {
      AbstractProAi.initialize(frame);
      add(SwingAction.of("Show Hard AI Logs", AbstractProAi::showSettingsWindow))
          .setMnemonic(KeyEvent.VK_X);
    }

    players.stream()
        .filter(PlayerDebug.class::isInstance)
        .forEach(
            player -> {
              final JMenu playerDebugMenu = new JMenu(player.getName());
              add(playerDebugMenu);

              ((PlayerDebug) player).addDebugMenuItems(frame, playerDebugMenu::add);
            });

    add(SwingAction.of("Show Console", () -> ClientSetting.showConsole.setValueAndFlush(true)))
        .setMnemonic(KeyEvent.VK_C);
  }
}
