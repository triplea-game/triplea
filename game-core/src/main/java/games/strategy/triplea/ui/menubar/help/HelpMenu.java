package games.strategy.triplea.ui.menubar.help;

import games.strategy.engine.data.GameData;
import games.strategy.triplea.UrlConstants;
import games.strategy.triplea.ui.UiContext;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.KeyStroke;
import lombok.experimental.UtilityClass;
import org.triplea.swing.SwingAction;
import org.triplea.swing.SwingComponents;

@UtilityClass
public final class HelpMenu {
  private static final Action bugReportMenu =
      SwingAction.of(
          "Send Bug Report",
          e -> SwingComponents.newOpenUrlConfirmationDialog(UrlConstants.GITHUB_ISSUES));

  private static final Action gameLicenseMenu =
      SwingAction.of(
          "License",
          e -> SwingComponents.newOpenUrlConfirmationDialog(UrlConstants.LICENSE_NOTICE));

  public static JMenu buildMenu(final UiContext uiContext, final GameData gameData) {
    final JMenu menu = new JMenu();
    menu.setText("Help");
    menu.setMnemonic('h');

    menu.add(MoveHelpMenu.buildMenu()).setMnemonic(KeyEvent.VK_M);

    final var helpMenu = menu.add(UnitHelpMenu.buildMenu(gameData, uiContext));
    helpMenu.setMnemonic(KeyEvent.VK_U);
    helpMenu.setAccelerator(
        KeyStroke.getKeyStroke(
            KeyEvent.VK_L, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));

    final String gameNotes = gameData.getProperties().get("notes", "").trim();
    if (!gameNotes.isBlank()) {
      menu.add(GameNotesMenu.buildMenu(gameNotes)).setMnemonic(KeyEvent.VK_N);
    }

    menu.addSeparator();

    menu.add(gameLicenseMenu).setMnemonic(KeyEvent.VK_I);

    menu.add(bugReportMenu).setMnemonic(KeyEvent.VK_B);

    return menu;
  }
}
