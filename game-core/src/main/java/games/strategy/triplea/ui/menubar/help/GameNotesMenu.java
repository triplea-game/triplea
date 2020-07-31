package games.strategy.triplea.ui.menubar.help;

import games.strategy.triplea.ui.NotesPanel;
import javax.swing.Action;
import javax.swing.JDialog;
import javax.swing.SwingUtilities;
import lombok.experimental.UtilityClass;
import org.triplea.swing.SwingAction;

@UtilityClass
class GameNotesMenu {
  private static final String gameNotesTitle = "Game Notes";

  Action buildMenu(final String gameNotes) {
    return SwingAction.of(
        gameNotesTitle,
        e ->
            SwingUtilities.invokeLater(
                () -> {
                  final JDialog dialog =
                      InformationDialog.createDialog(new NotesPanel(gameNotes), gameNotesTitle);
                  if (dialog.getWidth() < 400) {
                    dialog.setSize(400, dialog.getHeight());
                  }
                  if (dialog.getHeight() < 300) {
                    dialog.setSize(dialog.getWidth(), 300);
                  }
                  if (dialog.getWidth() > 800) {
                    dialog.setSize(800, dialog.getHeight());
                  }
                  if (dialog.getHeight() > 600) {
                    dialog.setSize(dialog.getWidth(), 600);
                  }
                  dialog.setVisible(true);
                }));
  }
}
