package games.strategy.triplea.ui.menubar.help;

import games.strategy.engine.data.GameData;
import games.strategy.triplea.UrlConstants;
import games.strategy.triplea.ui.UiContext;
import java.awt.Component;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import lombok.experimental.UtilityClass;
import org.triplea.swing.SwingAction;
import org.triplea.swing.SwingComponents;
import org.triplea.swing.key.binding.KeyCode;

@UtilityClass
public final class HelpMenu {

  public static JMenu buildMenu(
      final Component parentComponent, final UiContext uiContext, final GameData gameData) {
    final JMenu menu = new JMenu();
    menu.setText("Help");
    menu.setMnemonic(KeyCode.H.getInputEventCode());

    menu.add(MoveHelpMenu.buildMenu()).setMnemonic(KeyCode.M.getInputEventCode());

    final var helpMenu = menu.add(UnitHelpMenu.buildMenu(gameData, uiContext));
    helpMenu.setMnemonic(KeyCode.U.getInputEventCode());

    final String gameNotes = gameData.loadGameNotes(uiContext.getMapLocation());
    if (!gameNotes.isBlank()) {
      menu.add(GameNotesMenu.buildMenu(gameNotes, uiContext.getMapLocation()))
          .setMnemonic(KeyCode.N.getInputEventCode());
    }

    menu.addSeparator();

    menu.add(
            SwingAction.of(
                "License",
                e ->
                    SwingComponents.newOpenUrlConfirmationDialog(
                        parentComponent, UrlConstants.LICENSE_NOTICE)))
        .setMnemonic(KeyCode.I.getInputEventCode());

    menu.add(
            SwingAction.of(
                "Send Bug Report",
                e ->
                    SwingComponents.newOpenUrlConfirmationDialog(
                        parentComponent, UrlConstants.GITHUB_ISSUES)))
        .setMnemonic(KeyCode.B.getInputEventCode());

    menu.addSeparator();

    final JMenuItem hostingLink = new JMenuItem("User Guide");
    hostingLink.setMnemonic(KeyCode.H.getInputEventCode());
    hostingLink.addActionListener(
        e ->
            SwingComponents.newOpenUrlConfirmationDialog(parentComponent, UrlConstants.USER_GUIDE));
    menu.add(hostingLink);

    final JMenuItem warClub = new JMenuItem("TripleA Forum");
    warClub.setMnemonic(KeyCode.W.getInputEventCode());
    warClub.addActionListener(
        e ->
            SwingComponents.newOpenUrlConfirmationDialog(
                parentComponent, UrlConstants.TRIPLEA_FORUM));
    menu.add(warClub);

    final JMenuItem donateLink = new JMenuItem("Donate");
    donateLink.setMnemonic(KeyCode.O.getInputEventCode());
    donateLink.addActionListener(
        e ->
            SwingComponents.newOpenUrlConfirmationDialog(
                parentComponent, UrlConstants.PAYPAL_DONATE));
    menu.add(donateLink);

    return menu;
  }
}
