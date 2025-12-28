package games.strategy.triplea.ui.menubar.help;

import games.strategy.engine.data.GameData;
import games.strategy.triplea.UrlConstants;
import games.strategy.triplea.ui.UiContext;
import java.awt.Component;
import javax.swing.JMenu;
import lombok.experimental.UtilityClass;
import org.triplea.swing.JMenuBuilder;
import org.triplea.swing.JMenuItemBuilder;
import org.triplea.swing.SwingComponents;
import org.triplea.swing.key.binding.KeyCode;

@UtilityClass
public final class HelpMenu {

  public static JMenu buildMenu(
      final Component parentComponent, final UiContext uiContext, final GameData gameData) {
    final String gameNotes = gameData.loadGameNotes(uiContext.getMapLocation());
    return new JMenuBuilder("Help", KeyCode.H)
        .addMenuItem(new JMenuItemBuilder(MoveHelpMenu.buildMenu(), KeyCode.M))
        .addMenuItem(new JMenuItemBuilder(UnitHelpMenu.buildMenu(gameData, uiContext), KeyCode.U))
        .addMenuItemIf(
            !gameNotes.isBlank(),
            new JMenuItemBuilder(
                GameNotesMenu.buildMenu(gameNotes, uiContext.getMapLocation()), KeyCode.N))
        .addSeparator()
        .addMenuItem(
            new JMenuItemBuilder("License", KeyCode.I)
                .actionListener(
                    () ->
                        SwingComponents.newOpenUrlConfirmationDialog(
                            parentComponent, UrlConstants.LICENSE_NOTICE)))
        .addMenuItem(
            new JMenuItemBuilder("Send Bug Report", KeyCode.B)
                .actionListener(
                    () ->
                        SwingComponents.newOpenUrlConfirmationDialog(
                            parentComponent, UrlConstants.GITHUB_ISSUES)))
        .addSeparator()
        .addMenuItem(
            new JMenuItemBuilder("User Guide", KeyCode.H)
                .actionListener(
                    () ->
                        SwingComponents.newOpenUrlConfirmationDialog(
                            parentComponent, UrlConstants.USER_GUIDE)))
        .addMenuItem(
            new JMenuItemBuilder("TripleA Forum", KeyCode.W)
                .actionListener(
                    () ->
                        SwingComponents.newOpenUrlConfirmationDialog(
                            parentComponent, UrlConstants.TRIPLEA_FORUM)))
        .addMenuItem(
            new JMenuItemBuilder("Donate", KeyCode.O)
                .actionListener(
                    () ->
                        SwingComponents.newOpenUrlConfirmationDialog(
                            parentComponent, UrlConstants.PAYPAL_DONATE)))
        .build();
  }
}
