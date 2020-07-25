package games.strategy.triplea.ui.menubar.help;

import games.strategy.engine.data.GameData;
import games.strategy.engine.framework.ui.background.BackgroundTaskRunner;
import games.strategy.triplea.ui.UiContext;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JEditorPane;
import javax.swing.JMenuItem;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import lombok.experimental.UtilityClass;
import org.triplea.java.Interruptibles;
import org.triplea.java.Interruptibles.Result;
import org.triplea.swing.SwingAction;

@UtilityClass
class UnitHelpMenu {
  private static final String unitHelpTitle = "Unit Help";

  Action buildMenu(final GameData gameData, final UiContext uiContext) {
            return SwingAction.of(
                unitHelpTitle,
                e -> {
                  final Result<String> result =
                      Interruptibles.awaitResult(
                          () ->
                              BackgroundTaskRunner.runInBackgroundAndReturn(
                                  "Calculating Data",
                                  () -> UnitStatsTable.getUnitStatsTable(gameData, uiContext)));
                  final JEditorPane editorPane =
                      new JEditorPane(
                          "text/html", result.result.orElse("Failed to calculate Data"));
                  editorPane.setEditable(false);
                  editorPane.setCaretPosition(0);
                  final JScrollPane scroll = new JScrollPane(editorPane);
                  scroll.setBorder(BorderFactory.createEmptyBorder());
                  InformationDialog.createDialog(scroll, unitHelpTitle).setVisible(true);
                }));
  }

}
