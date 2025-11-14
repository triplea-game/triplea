package games.strategy.triplea.ui.menubar.help;

import games.strategy.engine.data.GameData;
import games.strategy.triplea.UrlConstants;
import games.strategy.triplea.ui.UiContext;
import java.awt.Component;
import java.awt.event.KeyEvent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import lombok.experimental.UtilityClass;
import org.triplea.swing.SwingAction;
import org.triplea.swing.SwingComponents;

@UtilityClass
public final class HelpMenu {

  public static JMenu buildMenu(
      final Component parentComponent, final UiContext uiContext, final GameData gameData) {
    final JMenu menu = new JMenu();
    menu.setText("Help");
    menu.setMnemonic(KeyEvent.VK_H);

    menu.add(MoveHelpMenu.buildMenu()).setMnemonic(KeyEvent.VK_M);

    final var helpMenu = menu.add(UnitHelpMenu.buildMenu(gameData, uiContext));
    helpMenu.setMnemonic(KeyEvent.VK_U);

    final String gameNotes = gameData.loadGameNotes(uiContext.getMapLocation());
    if (!gameNotes.isBlank()) {
      menu.add(GameNotesMenu.buildMenu(gameNotes, uiContext.getMapLocation()))
          .setMnemonic(KeyEvent.VK_N);
    }

    menu.addSeparator();

    menu.add(
            SwingAction.of(
                "License",
                e ->
                    SwingComponents.newOpenUrlConfirmationDialog(
                        parentComponent, UrlConstants.LICENSE_NOTICE)))
        .setMnemonic(KeyEvent.VK_I);

    menu.add(
            SwingAction.of(
                "Send Bug Report",
                e ->
                    SwingComponents.newOpenUrlConfirmationDialog(
                        parentComponent, UrlConstants.GITHUB_ISSUES)))
        .setMnemonic(KeyEvent.VK_B);

    menu.addSeparator();

    final JMenuItem hostingLink = new JMenuItem("User Guide");
    hostingLink.setMnemonic(KeyEvent.VK_H);
    hostingLink.addActionListener(
        e ->
            SwingComponents.newOpenUrlConfirmationDialog(parentComponent, UrlConstants.USER_GUIDE));
    menu.add(hostingLink);

    final JMenuItem warClub = new JMenuItem("TripleA Forum");
    warClub.setMnemonic(KeyEvent.VK_W);
    warClub.addActionListener(
        e ->
            SwingComponents.newOpenUrlConfirmationDialog(
                parentComponent, UrlConstants.TRIPLEA_FORUM));
    menu.add(warClub);

    final JMenuItem donateLink = new JMenuItem("Donate");
    donateLink.setMnemonic(KeyEvent.VK_O);
    donateLink.addActionListener(
        e ->
            SwingComponents.newOpenUrlConfirmationDialog(
                parentComponent, UrlConstants.PAYPAL_DONATE));
    menu.add(donateLink);

    return menu;
  }
}
