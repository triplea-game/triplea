package games.strategy.triplea.ui.menubar;

import java.awt.event.KeyEvent;
import java.util.Set;

import javax.swing.JMenu;

import games.strategy.engine.player.IGamePlayer;
import games.strategy.triplea.ai.pro.ProAi;
import games.strategy.triplea.settings.ClientSetting;
import games.strategy.triplea.ui.TripleAFrame;
import games.strategy.ui.SwingAction;

final class DebugMenu extends JMenu {
  private static final long serialVersionUID = -4876915214715298132L;

  DebugMenu(final TripleAFrame frame) {
    super("Debug");

    setMnemonic(KeyEvent.VK_D);

    final Set<IGamePlayer> players = frame.getLocalPlayers().getLocalPlayers();
    final boolean areThereProAIs = players.stream().anyMatch(ProAi.class::isInstance);
    if (areThereProAIs) {
      ProAi.initialize(frame);
      add(SwingAction.of("Show Hard AI Logs", e -> ProAi.showSettingsWindow())).setMnemonic(KeyEvent.VK_X);
    }

    add(SwingAction.of("Show Console", e -> ClientSetting.showConsole.saveAndFlush(true)))
        .setMnemonic(KeyEvent.VK_C);
  }
}
