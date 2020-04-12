package games.strategy.triplea.ui.panel.move;

import games.strategy.triplea.ui.AbstractUndoableMovesPanel;
import java.awt.Component;
import javax.annotation.Nonnull;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import lombok.Builder;

@Builder
class DoneMoveAction {

  @Nonnull private final JComponent parentComponent;
  @Nonnull private final AbstractUndoableMovesPanel undoableMovesPanel;
  @Nonnull private final Component unitScrollerPanel;

  boolean doneMoveAction() {
    final boolean performDone =
        (undoableMovesPanel.getCountOfMovesMade() == 0)
            && JOptionPane.showConfirmDialog(
                    JOptionPane.getFrameForComponent(parentComponent),
                    "Are you sure you do not want to move?",
                    "End Move",
                    JOptionPane.YES_NO_OPTION)
                == JOptionPane.YES_OPTION;
    if (performDone) {
      unitScrollerPanel.setVisible(false);
    }
    return performDone;
  }
}
