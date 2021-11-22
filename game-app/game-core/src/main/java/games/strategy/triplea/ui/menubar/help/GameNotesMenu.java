package games.strategy.triplea.ui.menubar.help;

import games.strategy.triplea.ui.UiContext;
import java.awt.Color;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.SwingUtilities;
import lombok.experimental.UtilityClass;
import org.triplea.swing.SwingAction;
import org.triplea.swing.SwingComponents;
import org.triplea.util.LocalizeHtml;

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
                      InformationDialog.createDialog(notesPanel(gameNotes), gameNotesTitle);
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

  private static JComponent notesPanel(final String gameNotes) {
    final String localizedHtml =
        LocalizeHtml.localizeImgLinksInHtml(gameNotes.trim(), UiContext.getMapLocation());

    final JEditorPane gameNotesPane = new JEditorPane("text/html", localizedHtml);
    gameNotesPane.setEditable(false);
    gameNotesPane.setForeground(Color.BLACK);
    gameNotesPane.setCaretPosition(0);
    return SwingComponents.newJScrollPane(gameNotesPane);
  }
}
