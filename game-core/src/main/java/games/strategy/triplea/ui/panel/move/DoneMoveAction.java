package games.strategy.triplea.ui.panel.move;

import games.strategy.triplea.ui.AbstractUndoableMovesPanel;
import java.awt.Component;
import java.util.function.Function;
import javax.annotation.Nonnull;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Builder
@AllArgsConstructor
class DoneMoveAction {

  @Nonnull private final JComponent parentComponent;
  @Nonnull private final AbstractUndoableMovesPanel undoableMovesPanel;
  @Nonnull private final Component unitScrollerPanel;

  @Builder.Default
  private Function<JComponent, Boolean> confirmNoMovement =
      parentComponent ->
          JOptionPane.showConfirmDialog(
                  JOptionPane.getFrameForComponent(parentComponent),
                  "Are you sure you do not want to move?",
                  "End Move",
                  JOptionPane.YES_NO_OPTION)
              == JOptionPane.YES_OPTION;

  boolean doneMoveAction() {
    final boolean performDone = undoableMovesPanel.noMovesMade() && confirmNoMovement.apply(parentComponent);
    if (performDone) {
      unitScrollerPanel.setVisible(false);
    }
    return performDone;
  }
}
